package com.kooo.evcam.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.preference.Preference;
import androidx.preference.PreferenceViewHolder;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Locale;
import static org.junit.Assert.*;

/** Measures the real preference binding, including long translations and large fonts. */
@RunWith(AndroidJUnit4.class)
public class SettingsValueRowAndroidTest {
    @Test public void longValuesLeaveBothTitleAndValueReadable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
            String[][] labels = {{"en", "Storage location", "USB drive (D719-0D20) · 237.2 GB free"},
                    {"ms", "Lokasi storan", "Pemacu USB (D719-0D20) · 237.2 GB tersedia"},
                    {"zh", "存储位置", "USB 存储设备 (D719-0D20) · 可用 237.2 GB"}};
            for (String[] sample : labels) for (float font : new float[]{1f, 1.3f})
                for (int night : new int[]{Configuration.UI_MODE_NIGHT_NO, Configuration.UI_MODE_NIGHT_YES})
                    for (int widthDp : new int[]{280, 500, 800}) {
                        Configuration cfg = new Configuration(target.getResources().getConfiguration());
                        cfg.fontScale = font; cfg.setLocale(Locale.forLanguageTag(sample[0]));
                        cfg.uiMode = (cfg.uiMode & ~Configuration.UI_MODE_NIGHT_MASK) | night;
                        Context c = new ContextThemeWrapper(target.createConfigurationContext(cfg), R.style.Theme_Cam);
                        View row = LayoutInflater.from(c).inflate(R.layout.pref_row_value, null);
                        Preference p = new Preference(c); p.setTitle(sample[1]); p.setSummary(sample[2]);
                        p.onBindViewHolder(PreferenceViewHolder.createInstanceForTests(row));
                        int width = Math.round(widthDp * c.getResources().getDisplayMetrics().density);
                        row.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                        row.layout(0, 0, row.getMeasuredWidth(), row.getMeasuredHeight());
                        for (int id : new int[]{android.R.id.title, android.R.id.summary}) {
                            TextView text = row.findViewById(id);
                            String label = sample[0] + " " + font + " " + widthDp + " " + text.getText();
                            assertTrue(label, text.getWidth() > 0);
                            assertNotNull(label, text.getLayout());
                            int last = text.getLayout().getLineCount() - 1;
                            assertEquals(label, 0, text.getLayout().getEllipsisCount(last));
                            assertEquals(label, text.length(), text.getLayout().getLineEnd(last));
                            assertTrue(label, text.getBottom() <= row.getHeight());
                        }
                        if ("true".equals(InstrumentationRegistry.getArguments().getString("captureValueRows"))
                                && font == 1.3f && widthDp == 280) {
                            android.graphics.Bitmap image = android.graphics.Bitmap.createBitmap(row.getWidth(),
                                    row.getHeight(), android.graphics.Bitmap.Config.ARGB_8888);
                            row.draw(new android.graphics.Canvas(image));
                            java.io.File file = new java.io.File(target.getExternalFilesDir(null),
                                    "value-row-" + sample[0] + "-" + night + ".png");
                            try (java.io.FileOutputStream out = new java.io.FileOutputStream(file)) {
                                assertTrue(image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out));
                            } catch (java.io.IOException e) { throw new AssertionError(e); }
                            finally { image.recycle(); }
                        }
                    }
        });
    }
}
