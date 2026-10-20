package com.kooo.evcam.update;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.MainActivity;
import com.kooo.evcam.R;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;

/** Real release-55 notes, native layout/scroll behavior and local callbacks; no update network call. */
public class UpdateOfferAndroidTest extends MainPageControlsAndroidTest {
    private void shell(String command) throws Exception {
        try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand(command);
             java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) {
            while (in.read() != -1) { }
        }
    }
    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        // Let emulator-only unavailable-camera toasts expire before the visual capture.
        SystemClock.sleep(3500);
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        File target = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir(null), name + ".png");
        try (FileOutputStream out = new FileOutputStream(target)) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { image.recycle(); }
    }
    private String allText(View view) {
        StringBuilder out = new StringBuilder();
        if (view instanceof TextView) out.append(((TextView) view).getText()).append('\n');
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            out.append(allText(((ViewGroup) view).getChildAt(i)));
        return out.toString();
    }
    private void assertFits(AlertDialog dialog) {
        View button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Rect visible = new Rect();
        dialog.getWindow().getDecorView().getWindowVisibleDisplayFrame(visible);
        Rect action = new Rect();
        assertTrue(button.getGlobalVisibleRect(action));
        assertEquals("Whole install button remains visible", button.getHeight(), action.height());
        int[] at = new int[2]; button.getLocationOnScreen(at);
        assertTrue("Install fits within display", visible.contains(at[0], at[1])
                && visible.contains(at[0] + button.getWidth() - 1, at[1] + button.getHeight() - 1));
        ScrollView scroll = dialog.findViewById(R.id.update_offer_notes);
        int[] notesAt = new int[2]; scroll.getLocationOnScreen(notesAt);
        assertTrue("Scrolling notes end before fixed action bar", notesAt[1] + scroll.getHeight() <= at[1]);
    }
    @Test public void notesScrollWithoutMovingActionsInBothThemesAndCompactLargeText() throws Exception {
        String markdown;
        try (java.io.InputStream input = InstrumentationRegistry.getInstrumentation().getContext()
                .getAssets().open("update-offer-55.md")) {
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096]; int n;
            while ((n = input.read(buffer)) != -1) bytes.write(buffer, 0, n);
            markdown = bytes.toString(StandardCharsets.UTF_8.name());
        }
        String notes = ReleaseNotes.summarise(markdown);
        GithubReleases.Release release = new GithubReleases.Release("v2.11.55-alpha.1",
                "update.apk", "https://example.invalid/update.apk", 24167846, "", "", markdown, notes);
        boolean[] installed = {false};
        AlertDialog[] shown = {null};
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            for (int mode : new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES}) {
                InstrumentationRegistry.getInstrumentation().runOnMainSync(
                        () -> AppCompatDelegate.setDefaultNightMode(mode));
                SystemClock.sleep(600);
                scenario.onActivity(a -> shown[0] = UpdateOfferDialog.show(a, release,
                        "2.11.54-alpha.1", () -> installed[0] = true));
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                capture(mode == AppCompatDelegate.MODE_NIGHT_NO ? "update-offer-light" : "update-offer-dark");
                scenario.onActivity(a -> {
                    assertFits(shown[0]);
                    ScrollView scroll = shown[0].findViewById(R.id.update_offer_notes);
                    assertTrue("Long notes scroll", scroll.canScrollVertically(1));
                    String rendered = allText(scroll);
                    for (String line : notes.split("\n")) if (!line.trim().isEmpty())
                        assertTrue("Every change is retained", rendered.contains(line.replaceFirst("^• ", "")));
                    scroll.fullScroll(View.FOCUS_DOWN);
                });
                InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                scenario.onActivity(a -> {
                    assertFits(shown[0]);
                    shown[0].getButton(AlertDialog.BUTTON_NEGATIVE).performClick();
                });
                assertFalse("Later never downloads", installed[0]);
            }
            shell("wm size 1920x1080"); shell("settings put system font_scale 1.3");
            SystemClock.sleep(1000);
            scenario.onActivity(a -> shown[0] = UpdateOfferDialog.show(a, release,
                    "2.11.54-alpha.1", () -> installed[0] = true));
            capture("update-offer-compact-large");
            scenario.onActivity(a -> {
                assertFits(shown[0]); shown[0].getButton(AlertDialog.BUTTON_POSITIVE).performClick();
            });
            // AlertDialog posts button listeners to its Handler after performClick returns.
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            assertTrue("Download still invokes the existing install-guide callback", installed[0]);
            scenario.onActivity(a -> shown[0] = UpdateOfferDialog.show(a, release.withNotes(""),
                    "2.11.54-alpha.1", () -> { }));
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            scenario.onActivity(a -> {
                assertFits(shown[0]);
                assertTrue(allText(shown[0].findViewById(R.id.update_offer_notes))
                        .contains(a.getString(R.string.upd_offer_no_notes)));
                shown[0].dismiss();
            });
        } finally { shell("wm size reset"); shell("settings put system font_scale 1.0"); }
    }
}
