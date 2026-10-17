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

    @Test public void modelNamesFollowSelectionWithoutChangingStreamProfileOrHudLayout() {
        Context english = context("en");
        AppConfig config = new AppConfig(english);
        android.content.SharedPreferences prefs = english.getSharedPreferences("app_config", 0);
        String original = prefs.getString(com.kooo.evcam.zeekr.VehicleProfile.KEY, null);
        String stream = config.getCarModel();
        boolean hud = config.is009HudLayout();
        try {
            for (String language : new String[]{"en", "zh", "ms"}) {
                Context localized = context(language);
                config.setVehicleProfileMode("009");
                assertTrue(VehicleLabels.name(localized).contains("009"));
                assertTrue(VehicleLabels.profile(localized, false).contains("009"));
                assertTrue(VehicleLabels.profile(localized, true).contains("009"));
                assertFalse(VehicleLabels.profile(localized, false).contains("7X"));
                config.setVehicleProfileMode("7x");
                assertTrue(VehicleLabels.profile(localized, false).contains("7X"));
                assertEquals(stream, config.getCarModel());
                assertEquals(hud, config.is009HudLayout());
            }
            config.setVehicleProfileMode("invalid");
            assertEquals("7x", config.getVehicleProfileMode()); // Invalid writes preserve the valid setting.
            config.setVehicleProfileMode("auto");
            assertEquals("auto", config.getVehicleProfileMode());
            assertEquals("Zeekr", VehicleLabels.name(english)); // Disposable emulator reports no Zeekr model.
        } finally {
            android.content.SharedPreferences.Editor e = prefs.edit();
            if (original == null) e.remove(com.kooo.evcam.zeekr.VehicleProfile.KEY);
            else e.putString(com.kooo.evcam.zeekr.VehicleProfile.KEY, original);
            e.commit();
        }
    }
}
