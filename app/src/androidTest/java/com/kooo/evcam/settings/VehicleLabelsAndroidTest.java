package com.kooo.evcam.settings;

import android.content.Context;
import android.content.res.Configuration;
import android.view.ContextThemeWrapper;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.R;
import com.kooo.evcam.ui.VehicleLabels;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.Locale;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class VehicleLabelsAndroidTest {
    private Context context(String language) {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        Configuration config = new Configuration(target.getResources().getConfiguration());
        config.setLocale(Locale.forLanguageTag(language));
        return new ContextThemeWrapper(target.createConfigurationContext(config), R.style.Theme_Cam);
    }

    @Test public void namesFollowSelectionInEachLanguageWithoutChangingCameraOrHudSettings() {
        Context english = context("en");
        AppConfig config = new AppConfig(english);
        String original = config.getVehicleNameMode();
        String stream = config.getCarModel();
        boolean hud = config.isZeekr009HudEnabled();
        try {
            for (String language : new String[]{"en", "zh", "ms"}) {
                Context localized = context(language);
                config.setVehicleNameMode("009");
                assertTrue(VehicleLabels.name(localized).contains("009"));
                assertTrue(VehicleLabels.profile(localized, false).contains("009"));
                assertTrue(VehicleLabels.profile(localized, true).contains("009"));
                assertFalse(VehicleLabels.profile(localized, false).contains("7X"));
                config.setVehicleNameMode("7x");
                assertTrue(VehicleLabels.profile(localized, false).contains("7X"));
                assertEquals(stream, config.getCarModel());
                assertEquals(hud, config.isZeekr009HudEnabled());
            }
            config.setVehicleNameMode("invalid");
            assertEquals("7x", config.getVehicleNameMode()); // Invalid writes preserve the valid setting.
            config.setVehicleNameMode("auto");
            assertEquals("auto", config.getVehicleNameMode());
            assertEquals("Zeekr", VehicleLabels.name(english)); // Disposable emulator reports no Zeekr model.
        } finally { config.setVehicleNameMode(original); }
    }
}
