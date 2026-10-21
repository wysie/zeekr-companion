package com.kooo.evcam.update;

import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.R;
import com.kooo.evcam.ui.MainPageControlsAndroidTest;
import com.kooo.evcam.ui.RearViewGuide;
import com.kooo.evcam.zeekr.AboutActivity;
import org.junit.Test;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;

/** Actual translated About/help views, resource selection, readable scrolling and install consent. */
public class LocalizedHelpAndroidTest extends MainPageControlsAndroidTest {
    private String text(View view) {
        StringBuilder result = new StringBuilder();
        if (view instanceof TextView) result.append(((TextView) view).getText()).append('\n');
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
            result.append(text(((ViewGroup) view).getChildAt(i)));
        return result.toString();
    }
    private void capture(String name) throws Exception {
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        Bitmap image = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();
        File target = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir(null), "localization-" + name + ".png");
        try (FileOutputStream out = new FileOutputStream(target)) {
            assertTrue(image.compress(Bitmap.CompressFormat.PNG, 100, out));
        } finally { image.recycle(); }
    }
    private void assertButtonFits(Dialog dialog) {
        View button = dialog.findViewById(R.id.guide_done);
        Rect visible = new Rect();
        assertTrue(button.getGlobalVisibleRect(visible));
        assertEquals("Entire action remains visible", button.getHeight(), visible.height());
    }

    @Test public void aboutAndAllGuidePagesUseChosenLanguageInBothThemes() throws Exception {
        LocaleListCompat previous = AppCompatDelegate.getApplicationLocales();
        try {
            for (String language : new String[]{"en", "ms", "zh-CN"}) {
                for (int night : new int[]{AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES}) {
                    InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language));
                        AppCompatDelegate.setDefaultNightMode(night);
                    });
                    try (ActivityScenario<AboutActivity> scenario = ActivityScenario.launch(AboutActivity.class)) {
                        Dialog[] shown = {null};
                        scenario.onActivity(a -> {
                            String about = text(a.getWindow().getDecorView());
                            String expected = language.equals("ms") ? "Saya wysie" : language.equals("en") ? "I’m wysie" : "我是 wysie";
                            assertTrue("About is localized: " + language, about.contains(expected));
                            assertTrue(about.contains("9271 6786"));
                            shown[0] = RearViewGuide.show(a);
                        });
                        int[] titles = {R.string.guide_controls_title, R.string.guide_cameras_title,
                                R.string.guide_framing_title, R.string.guide_docking_title, R.string.guide_resize_title};
                        for (int page = 0; page < titles.length; page++) {
                            final int position = page;
                            scenario.onActivity(a -> ((RecyclerView) shown[0].findViewById(R.id.guide_pages))
                                    .scrollToPosition(position));
                            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                            scenario.onActivity(a -> {
                                assertTrue(text(shown[0].getWindow().getDecorView()).contains(a.getString(titles[position])));
                                RecyclerView list = shown[0].findViewById(R.id.guide_pages);
                                assertNotNull(list.findViewHolderForAdapterPosition(position));
                                assertTrue(list.findViewHolderForAdapterPosition(position).itemView instanceof ScrollView);
                            });
                            if (page == 0) capture(language + "-mirror-" + night);
                        }
                        boolean[] proceeded = {false};
                        scenario.onActivity(a -> {
                            shown[0].dismiss();
                            shown[0] = UpdateInstallGuide.show(a, () -> proceeded[0] = true);
                        });
                        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
                        scenario.onActivity(a -> {
                            assertTrue(text(shown[0].getWindow().getDecorView()).contains(a.getString(R.string.guide_install_body)));
                            assertButtonFits(shown[0]);
                            shown[0].cancel();
                        });
                        assertFalse("Cancellation must not download", proceeded[0]);
                        scenario.onActivity(a -> shown[0] = UpdateInstallGuide.show(a, () -> proceeded[0] = true));
                        capture(language + "-installer-" + night);
                        scenario.onActivity(a -> shown[0].findViewById(R.id.guide_done).performClick());
                        assertTrue("Only explicit confirmation proceeds", proceeded[0]);
                    }
                }
            }
        } finally {
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> AppCompatDelegate.setApplicationLocales(previous));
        }
    }
}
