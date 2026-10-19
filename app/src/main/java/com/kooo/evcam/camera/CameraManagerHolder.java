package com.kooo.evcam.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;

import com.kooo.evcam.AppConfig;
import com.kooo.evcam.AppLog;
import com.kooo.evcam.blackbox.BlackBox;
import com.kooo.evcam.profile.CameraProfile;
import com.kooo.evcam.profile.Profile;
import com.kooo.evcam.profile.ProfileResolution;
import com.kooo.evcam.profile.ProfileSizes;
import com.kooo.evcam.profile.ProfileStore;
import com.kooo.evcam.zeekr.StreamLayoutTable;
import com.kooo.evcam.zeekr.ZeekrCameraLocator;
import com.kooo.evcam.zeekr.ZeekrMultiPlan;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The process-owned camera pipeline. UI and services reuse the applied snapshot.
 * Saved configuration changes never replace a live pipeline implicitly: Apply first
 * drains the recorder, waits for scheduler-confirmed native closure, then rebuilds.
 * Failed camera enumeration retains the existing bounded retry policy and applied setup.
 * Successful explicit Apply increments the UI revision so the Activity can recreate its
 * source views while Android restores the Settings page and nested editor state.
 *
 * <h3>怎么建</h3>
 *
 * <ul>
 *   <li>对象当场建好交出去（登记表的监听、调度、录像宿主都在它的构造里接上）；相机列表、合成流探测、照配置定尺寸
 *       都要进相机服务，在 {@link #setup} 这条线程上做 —— 主线程不进相机服务（lifecycle-spec §3.5：相机服务忙的时候一问要等
 *       几秒到十几秒）。做完回主线程 {@link MultiCameraManager#initCameras}，各路的画面尺寸由 SingleCamera 在它自己的相机线程上
 *       不开设备先算（{@code resolveSize}）。</li>
 *   <li>槽位照存下的映射建（{@link ZeekrMultiPlan#choose}）：相机列表完整时照列表分，值得存才存下来（{@code worthStoring}：
 *       环视是探测到的合成流、或者还没存过能用的；只在变了时写）；不完整时照存下的建，
 *       列表里少的那一路照样建出来，交给通道调度判「不在」（被别的程序拿着）—— 车机拿着后座舱时它就不在列表里，以前照列表分，
 *       后座舱这一格就没了，以后既认不出、也开不了。没存过（第一次装）照列表建（大纲 §6 风险 13）。</li>
 *   <li>用几路、座舱哪两格要建，照当前配置（{@link Profile}：开了几路、座舱那一路开没开、有没有格子）—— 和主界面的布局同一个判断；
 *       环视钉合成流尺寸（照配置解，「最大」也解得出来），座舱照配置定尺寸、auto 不钉。</li>
 * </ul>
 */
public class CameraManagerHolder {
    private static final String TAG = "CameraManagerHolder";
    private static CameraManagerHolder instance;
    private MultiCameraManager cameraManager;
    /** Native session topology used for the current pipeline. */
    private String builtFor;
    private Shape builtShape;
    private volatile Shape displayShape;
    private volatile boolean replacing, applyFailed;
    private volatile int configurationRevision;
    private int applyGeneration;

    public synchronized int configurationRevision() { return configurationRevision; }
    public synchronized Profile appliedProfile(Context context) {
        Shape shown = displayShape != null ? displayShape : builtShape;
        return shown == null ? new ProfileStore(context).current() : Profile.fromMap(shown.profile.toMap());
    }
    public synchronized String appliedPreviewMode(Context context) {
        Shape shown = displayShape != null ? displayShape : builtShape;
        return shown == null ? new AppConfig(context).getPreviewSourceMode() : shown.previewMode;
    }

    private com.kooo.evcam.profile.RecordingSetup builtRecordingSetup;
    /** 这一份建好之后的样子（主线程上写）；null = 还在建。 */
    private Built built;
    /** 等这一份建好的（{@link #whenBuilt}，主线程）。 */
    private final List<Waiter> waiters = new ArrayList<>();
    /** 放掉重建了要告诉的（{@link #addReplacedListener}）：在主线程上叫。 */
    private final List<Runnable> replacedListeners = new java.util.concurrent.CopyOnWriteArrayList<>();
    private final Handler main = new Handler(Looper.getMainLooper());

    /** 列相机出错之后，至少隔这么久才再放掉重建一次（见类说明）。 */
    static final long FAILED_REBUILD_GAP_MS = 10_000L;
    /** 上一次建出错（列相机出错）的时刻（elapsedRealtime）；建好了清成 0。 */
    private long failedAtMs;
    /** 连着出错了几次（建好了清零）：只有第一次进黑匣子，接着的进日志，好了再记一行一共几次。 */
    private int failedStreak;
    /**
     * 建管线的那一条线程（相机列表、合成流探测、照配置定尺寸都要进相机服务）：一次只建一份，空闲几秒线程就退。
     */
    private final java.util.concurrent.ThreadPoolExecutor setup = new java.util.concurrent.ThreadPoolExecutor(
            0, 1, 5, java.util.concurrent.TimeUnit.SECONDS, new java.util.concurrent.LinkedBlockingQueue<>(),
            runnable -> new Thread(runnable, "CameraSetup"));

    private CameraManagerHolder() {}

    public static synchronized CameraManagerHolder getInstance() {
        if (instance == null) {
            instance = new CameraManagerHolder();
        }
        return instance;
    }

    /** 一份管线建好之后的样子：主界面画四宫格几何、状态条那一格、座舱格子、出错的提示用。 */
    public static final class Built {
        final java.util.Map<String, Size> extraSizes = new java.util.LinkedHashMap<>();
        /** 槽位怎么分的（照存下的、或者照列表）；列相机出错时是 null。 */
        public final ZeekrMultiPlan plan;
        /** 合成流的尺寸：探测到的，或者存下的；null = 不知道（环视不是合成流、探测不到也没存过）。 */
        public final Size compositeSize;
        /** 照配置钉在环视上的尺寸；null = 不钉（不是合成流，或者配置解不出来）。 */
        public final Size compositeUse;
        /** 合成流探测器的说明（日志、主界面状态条那一格的来历）。 */
        public final String diagnostics;
        /** 相机服务没答上来（列相机出错）：这一份一路相机都没有，隔够 {@link CameraManagerHolder#FAILED_REBUILD_GAP_MS} 之后有人要管线时重建。 */
        public final boolean failed;

        Built(ZeekrMultiPlan plan, Size compositeSize, Size compositeUse, String diagnostics, boolean failed) {
            this.plan = plan;
            this.compositeSize = compositeSize;
            this.compositeUse = compositeUse;
            this.diagnostics = diagnostics;
            this.failed = failed;
        }
    }

    /** 等某一份管线建好的一位。 */
    private static final class Waiter {
        final MultiCameraManager manager;
        final Consumer<Built> then;

        Waiter(MultiCameraManager manager, Consumer<Built> then) {
            this.manager = manager;
            this.then = then;
        }
    }

    /**
     * 这一份照什么建：车型（存下的映射按它存）、配置里开了几路（主界面的布局也照它）、座舱两格要不要建。
     * 和主界面使用同一个 ProfileChannels：看启用的通道身份，不能把单独座舱误当作环视。
     */
    private static final class Shape {
        final String carModel;
        String previewMode;
        boolean gpuPreview, gpu360, jpeg;
        final Profile profile;
        final boolean mapping009;
        final com.kooo.evcam.profile.ProfileChannels channels;
        final boolean cabinFront;
        final boolean cabinRear;
        final int count;
        final java.util.Map<String, String> sources = new java.util.LinkedHashMap<>();

        private Shape(String carModel, Profile profile, boolean mapping009, String previewMode, boolean overlayNeedsSurround) {
            this.previewMode = previewMode;
            this.mapping009 = mapping009;
            this.carModel = carModel;
            this.profile = profile;
            channels = com.kooo.evcam.profile.ProfileChannels.of(profile, previewMode, overlayNeedsSurround);
            cabinFront = channels.keys.contains("back");
            cabinRear = channels.keys.contains("left");
            count = Math.max(1, channels.count);
        }

        static Shape of(Context app) {
            AppConfig config = new AppConfig(app);
            Shape shape = new Shape(config.getCarModel(), new ProfileStore(app).current(), config.is009CameraMappingEnabled(), config.getPreviewSourceMode(),
                    config.isSidePopupEnabled() || config.isRearViewEnabled() || new com.kooo.evcam.zeekr.BirdEyeWindowConfig(app).popup());
            shape.gpuPreview = config.isGpuFisheyePreview();
            shape.gpu360 = config.isGpuBirdEye();
            shape.jpeg = config.isPhotoViaJpegEnabled();
            for (String role : com.kooo.evcam.profile.RecordingSources.ALL_ROLES)
                shape.sources.put(role, config.getSourceCameraId(role));
            return shape;
        }

        com.kooo.evcam.profile.RecordingSetup recordingSetup() {
            return com.kooo.evcam.profile.RecordingSetup.of(carModel, mapping009, profile, sources);
        }

        String key() {
            return com.kooo.evcam.profile.CameraSetup.key(carModel, mapping009, profile, previewMode,
                    channels.keys.contains("front"), sources, gpuPreview, gpu360, jpeg);
        }
    }

    /** Reuse the applied pipeline; create it initially or retry failed enumeration after backoff. Main thread. */
    public synchronized MultiCameraManager getOrInit(Context context) {
        return obtain(context);
    }

    /** UI callers use the same applied snapshot as recording, photos and overlays. */
    public synchronized MultiCameraManager getForMainScreen(Context context) {
        return obtain(context);
    }

    /** Channel toggles are saved immediately but need a new main-screen layout/pipeline. */
    public synchronized boolean hasPendingChannelChange(Context context) {
        return replacing || applyFailed || (cameraManager != null
                && !Shape.of(context.getApplicationContext()).key().equals(builtFor));
    }

    /** Only actual recording-role/model/manual-mapping edits can block recording startup.
     * Pending preview/overlay choices still use the wider main-screen shape above. */
    public synchronized boolean hasPendingRecordingChange(Context context) {
        return applyFailed || (cameraManager != null && !cameraManager.isReleased() && builtRecordingSetup != null
                && !Shape.of(context.getApplicationContext()).recordingSetup().equals(builtRecordingSetup));
    }

    /** Read-only evidence separating a pending preview from a blocked recording configuration. */
    public synchronized String configurationSummary(Context context) {
        Shape requested = Shape.of(context.getApplicationContext());
        boolean active = cameraManager != null && !cameraManager.isReleased();
        return "applying=" + replacing + ", applyFailed=" + applyFailed + ", pipelineActive=" + active + ", mainPreviewPending=" + (active && !requested.key().equals(builtFor))
                + ", recordingPending=" + (active && builtRecordingSetup != null && !requested.recordingSetup().equals(builtRecordingSetup))
                + "\n  built recording: " + builtRecordingSetup + "\n  requested recording: " + requested.recordingSetup()
                + "\n  requested preview: " + new AppConfig(context).getPreviewSourceMode();
    }

    private MultiCameraManager obtain(Context context) {
        Context app = context.getApplicationContext();
        if ((replacing || applyFailed) && cameraManager != null) return cameraManager;
        Shape shape = builtShape != null && cameraManager != null ? builtShape : Shape.of(app);
        if (cameraManager != null && !cameraManager.isReleased()) {
            Rebuild why = rebuildReason();
            if (why == Rebuild.NO) {
                return cameraManager;
            }
            if (why == Rebuild.LISTING_FAILED && failedStreak > 1) {
                AppLog.w(TAG, "管线：上一次列相机出错，放掉重建（连着第 " + failedStreak + " 次出错）");
            } else {
                BlackBox.noteImportant("Camera enumeration failed; rebuilding the applied setup after "
                        + FAILED_REBUILD_GAP_MS / 1000 + " seconds");
            }
            cameraManager.release();
            cameraManager = null;
            for (Runnable listener : replacedListeners) {
                main.post(listener);
            }
        }
        return create(app, shape);
    }

    private synchronized MultiCameraManager create(Context app, Shape shape) {
        MultiCameraManager manager = new MultiCameraManager(app);
        manager.setSessionOptions(new CameraSessionOptions(shape.gpuPreview, shape.gpu360, shape.jpeg, shape.profile));
        manager.setMaxOpenCameras(shape.count);
        // Recording remains MediaCodec; the saved experimental pipeline preference has no effect.
        manager.setCodecRecordingMode(new AppConfig(app).shouldUseCodecRecording());
        cameraManager = manager;
        builtShape = shape;
        builtFor = shape.key();
        builtRecordingSetup = shape.recordingSetup();
        built = null;
        AppLog.i(TAG, "建相机管线（" + builtFor + "，最多 " + shape.count + " 路）：相机列表、合成流探测在相机线程上做");
        setup.execute(() -> build(app, manager, shape));
        return manager;
    }

    private enum Rebuild { NO, LISTING_FAILED }

    private Rebuild rebuildReason() {
        if (built != null && built.failed) {
            return android.os.SystemClock.elapsedRealtime() - failedAtMs < FAILED_REBUILD_GAP_MS
                    ? Rebuild.NO : Rebuild.LISTING_FAILED;
        }
        return Rebuild.NO; // Saved edits are applied only by the explicit action.
    }

    /**
     * 这一份还是进程里在用的那一份：没放、没被换掉、建的时候列相机没出错。主界面回到前台、收到「放掉重建了」时据此判断要不要
     * 接上新的那一份（列相机出错的那一份也当作要换：接的时候隔够了就重建）。
     */
    public synchronized boolean isCurrent(MultiCameraManager manager) {
        return manager != null && manager == cameraManager && !manager.isReleased()
                && !(built != null && built.failed);
    }

    /**
     * 放掉重建了就叫 {@code listener}（主线程上叫，可能在当时持有的那一份已经放了之后）：主界面挂上，据此当场接上新的那一份 ——
     * 它握着放掉了的那一份时，预览黑着、回调挂在死的那份上。退出（{@link #release}）不叫。挂了就要在销毁时摘
     * （{@link #removeReplacedListener}）。
     */
    public void addReplacedListener(Runnable listener) {
        if (listener != null && !replacedListeners.contains(listener)) {
            replacedListeners.add(listener);
        }
    }

    public void removeReplacedListener(Runnable listener) {
        replacedListeners.remove(listener);
    }

    /** 已有的那一份（不建）；没有是 null。只读它的事实（诊断、可用性分类、熄屏录制）用这个。 */
    public synchronized MultiCameraManager getCameraManager() {
        return cameraManager;
    }

    /**
     * 这一份管线建好了就叫 {@code then}（主线程上调、主线程上叫）：已经建好当场叫，否则等建好时叫。
     * 管线已经不是这一份了（放了、换了一份）就不叫 —— 等的一方再去拿新的那一份。
     */
    public void whenBuilt(MultiCameraManager manager, Consumer<Built> then) {
        Built done;
        synchronized (this) {
            if (manager == null || manager != cameraManager) {
                return;
            }
            done = built;
            if (done == null) {
                waiters.add(new Waiter(manager, then));
                return;
            }
        }
        then.accept(done);
    }

    /**
     * 放掉这一份（退出时）：MultiCameraManager.release 停录收尾、让调度按次序关相机，关完调度自己停、放掉交给它的录制器。
     * 管线只在退出和换车型时才放。
     */
    public synchronized void release() {
        if (cameraManager != null) {
            cameraManager.release();
            cameraManager = null;
        }
        applyGeneration++;
        builtShape = displayShape = null;
        replacing = applyFailed = false;
        builtFor = null;
        builtRecordingSetup = null;
        built = null;
        failedAtMs = 0;
        failedStreak = 0;
        waiters.clear();
    }

    /** Capture at the action, before draining the recorder. Later edits stay pending. */
    public synchronized Runnable prepareSavedChanges(Context context, Consumer<Boolean> done) {
        Context app = context.getApplicationContext();
        Shape requested = Shape.of(app);
        return () -> applySavedChanges(app, requested, done);
    }

    /** Main thread. Called only after the recorder has drained its current clip. */
    private synchronized void applySavedChanges(Context context, Shape requested, Consumer<Boolean> done) {
        if (replacing) { done.accept(false); return; }
        Context app = context.getApplicationContext();
        final int generation = ++applyGeneration;
        replacing = true;
        applyFailed = false;
        MultiCameraManager previous = cameraManager;
        if (previous != null) previous.release();
        final Runnable[] closed = new Runnable[1];
        final Runnable[] timeout = new Runnable[1];
        timeout[0] = () -> {
            if (!replacing || generation != applyGeneration) return;
            ChannelScheduler.cancelClosedWaiter(closed[0]);
            replacing = false;
            applyFailed = true;
            BlackBox.noteImportant("Camera Apply failed: native camera shutdown/setup timed out");
            done.accept(false);
        };
        closed[0] = () -> {
            if (!replacing || generation != applyGeneration) return;
            // All old sessions have surrendered their surfaces before GL producers are removed.
            setup.execute(() -> {
                com.kooo.evcam.zeekr.PreviewDewarp.releaseAll();
                main.post(() -> {
                    if (!replacing || generation != applyGeneration) return;
                    MultiCameraManager next = create(app, requested);
                    whenBuilt(next, result -> {
                        if (!replacing || generation != applyGeneration) return;
                        main.removeCallbacks(timeout[0]);
                        replacing = false;
                        applyFailed = result.failed;
                        if (!result.failed) {
                            displayShape = requested;
                            configurationRevision++;
                            for (Runnable listener : replacedListeners) main.post(listener);
                        }
                        BlackBox.noteImportant("Camera Apply completed: success=" + !result.failed);
                        done.accept(!result.failed);
                    });
                });
            });
        };
        main.postDelayed(timeout[0], 60_000);
        ChannelScheduler.whenClosedEverywhere(closed[0]);
    }

    // ================================================================= 建（相机线程）

    /**
     * 在 {@link #setup} 线程上：列相机、探测合成流、分槽位（照存下的映射或者照列表）、照配置定尺寸，做完回主线程交给管线。
     * 这里会进相机服务，所以不在主线程上。
     */
    private void build(Context app, MultiCameraManager manager, Shape shape) {
        AppLog.i(TAG, "本次建管线使用的配置:\n" + shape.profile);
        AppConfig config = new AppConfig(app);
        CameraManager service = (CameraManager) app.getSystemService(Context.CAMERA_SERVICE);
        String[] ids;
        try {
            if (service == null) {
                throw new IllegalStateException("camera service not available");
            }
            ids = service.getCameraIdList();
        } catch (CameraAccessException | RuntimeException e) {
            AppLog.e(TAG, "管线：列相机失败，这一份没有相机，隔够 " + FAILED_REBUILD_GAP_MS / 1000 + " 秒之后有人要管线时重建: " + e);
            Built failed = new Built(null, null, null, String.valueOf(e), true);
            main.post(() -> finish(manager, shape, failed, null, null));
            return;
        }
        ZeekrCameraLocator.Result located = ZeekrCameraLocator.locate(service);
        AppLog.i(TAG, "极氪合成流探测结果（相机列表 " + java.util.Arrays.toString(ids) + "）:\n" + located.diagnostics);

        if (located.found()) config.setDetectedSurroundId(located.cameraId);
        String overrideComposite = shape.sources.get(CameraProfile.ROLE_COMPOSITE);
        String overrideCabin1 = shape.sources.get(CameraProfile.ROLE_CABIN_1);
        String overrideCabin2 = shape.sources.get(CameraProfile.ROLE_CABIN_2);
        String overrides = ZeekrMultiPlan.Stored.overridesKey(overrideComposite, overrideCabin1, overrideCabin2);
        String storedText = config.getStoredSlotMap(shape.carModel);
        ZeekrMultiPlan.Stored stored = ZeekrMultiPlan.Stored.decode(storedText);
        ZeekrMultiPlan plan = shape.mapping009
                ? ZeekrMultiPlan.observed009(ids, located.found() ? located.cameraId : config.getDetectedSurroundId(),
                        overrideComposite, overrideCabin1, overrideCabin2)
                : ZeekrMultiPlan.choose(ids, located.found() ? located.cameraId : null,
                        overrideComposite, overrideCabin1, overrideCabin2, stored);
        plan = plan.withSourceChoices(ids, located.found() ? located.cameraId : config.getDetectedSurroundId(),
                overrideComposite, overrideCabin1, overrideCabin2);
        AppLog.i(TAG, "槽位分配:\n" + plan.explanation + "-> " + plan);

        // 合成流尺寸：探测到的就是环视这一路，用探测的；环视不在列表里（探测不到它）用存下的
        Size compositeSize = null;
        if (located.found() && located.cameraId.equals(plan.compositeId)) {
            compositeSize = located.size;
        } else if (plan.fromStored && stored != null && stored.compositeWidth > 0 && stored.compositeHeight > 0) {
            compositeSize = new Size(stored.compositeWidth, stored.compositeHeight);
        }

        if (plan.fromStored) {
            // 列表里没有、又是这一次要建的那几路：照样建出来，调度开之前看相机服务说它「不在」，判成被别的程序拿着
            List<String> absent = new ArrayList<>();
            for (String id : plan.missing) {
                if (id.equals(plan.compositeId) || shape.cabinFront && id.equals(plan.cabin1Id)
                        || shape.cabinRear && id.equals(plan.cabin2Id)) {
                    absent.add(id);
                }
            }
            BlackBox.noteImportant("管线：照存下的映射建 环视=" + plan.compositeId + "、后座舱=" + plan.cabin2Id
                    + "、前座舱=" + plan.cabin1Id + "（车型 " + shape.carModel + "）"
                    + (absent.isEmpty() ? "" : "；相机列表里现在没有 " + String.join("、", absent) + " → 交给调度判"));
        } else if (!shape.mapping009 && plan.worthStoring(stored, overrides)) {
            // 列表完整、值得存（环视是探测到的合成流，或者还没存过能用的）才更新：合成流这一次没探测到时，照列表分出来的是
            // 「普通相机顶替环视、座舱顺次挪位」，不能拿它盖掉存着的那一份好的
            ZeekrMultiPlan.Stored now = ZeekrMultiPlan.Stored.of(plan,
                    compositeSize == null ? 0 : compositeSize.getWidth(),
                    compositeSize == null ? 0 : compositeSize.getHeight(), overrides);
            String encoded = now.encode();
            if (!encoded.equals(storedText)) {
                config.setStoredSlotMap(shape.carModel, encoded);
                BlackBox.noteImportant("管线：相机列表完整，更新存下的映射 " + now + "（车型 " + shape.carModel + "）");
            }
        }

        // 「哪一路是合成流」（四宫格拆不拆、照片和录像的几何都看它）：照分好的槽位定，探测器探到的不一定是环视那一路（手动指定）
        StreamLayoutTable.setCompositeCameraId(plan.compositeIsReal ? plan.compositeId : null);

        // 照配置定尺寸（「最大」要问相机服务这一路声明了哪些尺寸，所以也在这条线程上算）：环视只在真是合成流时钉
        Size compositeUse = plan.compositeIsReal && compositeSize != null
                ? profileSize(app, shape.profile, CameraProfile.ROLE_COMPOSITE, compositeSize) : null;
        Size cabinFrontUse = shape.cabinFront ? profileSize(app, shape.profile, CameraProfile.ROLE_CABIN_1, null) : null;
        Size cabinRearUse = shape.cabinRear ? profileSize(app, shape.profile, CameraProfile.ROLE_CABIN_2, null) : null;
        Built result = new Built(plan, compositeSize, compositeUse, located.diagnostics, false);
        for (String role : com.kooo.evcam.profile.RecordingSources.EXTRA_ROLES)
            if (shape.channels.keys.contains(role)) result.extraSizes.put(role, profileSize(app, shape.profile, role, null));
        main.post(() -> finish(manager, shape, result, cabinFrontUse, cabinRearUse));
    }

    /**
     * 回到主线程：照分好的槽位建各路相机、交给调度，紧接着钉尺寸（同一个主线程任务里：各路的画面尺寸排在这之后才算，
     * 先算就算成别的尺寸了），然后告诉等着的。这期间管线放了、换了一份，就什么都不做。
     */
    private void finish(MultiCameraManager manager, Shape shape, Built result, Size cabinFrontUse, Size cabinRearUse) {
        List<Waiter> due = new ArrayList<>();
        synchronized (this) {
            if (manager != cameraManager || manager.isReleased()) {
                return;
            }
            ZeekrMultiPlan plan = result.plan;
            if (plan == null) {
                manager.initCameras(null, null, null, null, null, null, null, null);
            } else {
                java.util.Map<String, String> ids = new java.util.LinkedHashMap<>();
                for (String key : shape.channels.keys) {
                    String role = com.kooo.evcam.profile.RecordingSources.role(key);
                    String explicit = shape.sources.get(role);
                    String id = "front".equals(key) ? plan.compositeId : "back".equals(key) ? plan.cabin1Id
                            : "left".equals(key) ? plan.cabin2Id
                            : com.kooo.evcam.profile.RecordingSources.defaultId(role, shape.mapping009);
                    if (explicit != null) id = explicit.isEmpty() ? null : explicit;
                    ids.put(key, id);
                }
                manager.initCameras(ids, java.util.Collections.emptyMap());
                pin(manager, CameraSlots.KEY_SURROUND, result.compositeUse);
                pin(manager, CameraSlots.KEY_CABIN_FRONT, cabinFrontUse);
                pin(manager, CameraSlots.KEY_CABIN_REAR, cabinRearUse);
                for (java.util.Map.Entry<String, Size> extra : result.extraSizes.entrySet()) pin(manager, extra.getKey(), extra.getValue());
                AppLog.i(TAG, "相机管线建好了（" + builtFor + "）: " + plan + "，环视尺寸 " + result.compositeUse
                        + "（合成流 " + result.compositeSize + "），前座舱 " + cabinFrontUse + "，后座舱 " + cabinRearUse);
            }
            if (result.failed) {
                failedAtMs = android.os.SystemClock.elapsedRealtime();
                failedStreak++;
            } else {
                if (failedStreak > 1) {
                    BlackBox.noteImportant("管线：列相机连着出错 " + failedStreak + " 次之后建好了");
                }
                failedAtMs = 0;
                failedStreak = 0;
            }
            built = result;
            if (!result.failed && !replacing && !applyFailed) displayShape = shape;
            for (java.util.Iterator<Waiter> it = waiters.iterator(); it.hasNext(); ) {
                Waiter waiter = it.next();
                if (waiter.manager == manager) {
                    due.add(waiter);
                }
                it.remove();   // 别的那几份（放了、换了）的等待一起作废
            }
        }
        for (Waiter waiter : due) {
            waiter.then.accept(result);
        }
    }

    private static void pin(MultiCameraManager manager, String key, Size size) {
        SingleCamera camera = manager.getCamera(key);
        if (camera != null && size != null) {
            camera.setPreferredSize(size);
        }
    }

    /**
     * 这一路在配置里要用的预览尺寸。
     *
     * @param probed 这一路探测到的尺寸；没有传 null
     * @return null 表示不指定，交给 {@code chooseOptimalSize} 按老规则挑
     */
    private static Size profileSize(Context app, Profile profile, String role, Size probed) {
        CameraProfile camera = profile.camera(role);
        if (camera == null) {
            return probed;
        }
        int[] probedPair = probed == null ? null : new int[]{probed.getWidth(), probed.getHeight()};
        // 「最大」也要能解出来：不给声明的最大尺寸的话，选了 max 等于没选 —— 解成「不指定」，相机就按老规则挑一个 1280x800 附近的
        ProfileResolution.Size size = ProfileResolution.resolve(camera.preview.resolution,
                probedPair, ProfileSizes.declaredMax(app, role));
        return size.specified() ? new Size(size.width, size.height) : null;
    }
}
