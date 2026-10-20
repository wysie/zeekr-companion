# Blind-camera quality comparison

The owner reports that bilinear/bicubic choices do not noticeably improve the
blind popup. This diagnostic records evidence rather than changing picture tuning.
Open Blind spot camera → Picture, retain a live Surround source in the applied main
preview, select Left/Right, and tap Capture quality comparison.

The ZIP contains `native-side.png`, `popup-render.png` and `comparison.json`.
The first image is the decoded side lane at native size, unmirrored and unrotated;
it is not sensor RAW. The second is a same-frame diagnostic render using current
popup settings and the current same-side popup extent, or saved normal popup extent
when that window is absent. It is not an acquired live-window screenshot. UI labels,
arrows, controls, opacity and HUD-specific trim/tone are excluded to isolate camera
pixels. Metadata records this provenance explicitly, including whether an actual
popup was present and its requested/rendered GPU buffer state when available.

## Frame and renderer ownership

For a live shared GPU source, one task on its existing GL thread samples the acquired
OES texture twice without calling updateTexImage between images. A temporary FBO
holds only diagnostic pixels; it is never a camera declaration or live-output tap.
The original lane uses nearest texels with the producer's texture/crop matrix, then
restores the previous OES min/mag filters before rendering the processed picture.
GPU processing uses SideGpuRenderer and SideGpuPicture, the actual popup shader and
parameters. Compatibility processing uses its mesh/framing/rotation policy on the
frozen native lane. Source timestamp and observed progression time describe the
source frame; they do not establish camera exposure age or optical resolution.
FBO/viewport/filter state is restored on success or failure. Capture does not
publish a live output timestamp, renew source freshness or change projection
requests, camera producers, session ownership, recording intent or configuration.

Without a GPU source, one native-sized TextureView bitmap is acquired on the main
thread, with stable texture identity/timestamp checked across that copy. Both
images then derive from that same immutable acquisition. A source already corrected
by a GPU route is never passed off as an original uncorrected bitmap. The popup
settings and geometry are snapshotted before asynchronous work; later UI changes
cannot alter the exported render. Native rows, RGBA channels and orientation are
retained in the reference; adjacent-lane separators are excluded, and no diagnostic
sharpening is applied to it.

Only one request can be active. Missing/stale sources are refused, and the existing
Picture-page factory/reverse/screen guards apply. Hide, stop, detach and side changes
cancel pending work. A bounded timeout releases a lost request. Completed exports
are retained. The source/render budget is 8,388,608 combined pixels with 4096px
individual edges; direct full-frame readback additionally limits the source strip
to 8,388,608 pixels/8192px. Oversized images fail instead of being reduced without
notice. Bitmap/GL memory failures are handled without choosing a new live route.

## Export and UI

PNG encoding and ZIP writing run on a single file worker. The native ZIP is written
to a unique `.partial` file, synced, and renamed only on completion. Failed/cancelled
partial files are removed; existing exports are never overwritten. The result uses
the existing diagnostics export directory and its fallback policy, normally
ZeekrCompanion/Logs. A persistent last-completed file location enables retrieval
after leaving the page. Phone sharing is explicit through the existing QR flow;
there is no automatic upload. Export state is separate from settings backup stores.
A new attempt hides previous-file sharing controls so an old capture is not mistaken
for the result of a failed new capture.

The footer follows native Picture typography and actions, caps its scroll height
at 180dp, and keeps status/path text separate from live-camera availability. No new
permission prompt, gear/speed gate, popup, HUD window or recording-stop flow is added.
The capture adds one-off GPU readback, PNG compression and storage load; smoothness
or recording cadence on a real vehicle is not established by emulator tests.

## Validation (2026-10-20)

The debug JVM suite passes 1,535 tests, lint reports zero errors, and debug, signed
release and instrumentation APKs build successfully. A focused emulator run passes
31 native tests covering GPU lane/readback orientation and channels, same-frame
shader equivalence, frozen/cancelled capture recovery, unchanged live producer
ownership, ZIP integrity, the real Picture-page capture action, preserved settings,
and recording isolation. This is not a claim that the entire UI suite passes; the
previously documented Preview-dialog visibility assertion remains outside this run.

Light and dark native Picture-page captures were inspected at 2560×1600. The
preview, bounded footer, wrapped saved path and share/copy controls remain visible.
The synthetic UI export contains exactly the three documented files: a 1280×1280
native lane, a 1791×814 Canvas popup render, and metadata retaining aim back 40°,
the selected backend and same-frame provenance. Separate GL tests exercise the
GPU path against the actual live side shader. Synthetic images establish capture
correctness, not vehicle optical quality; owner capture and on-car cadence checks
remain pending. This feature is unreleased.
