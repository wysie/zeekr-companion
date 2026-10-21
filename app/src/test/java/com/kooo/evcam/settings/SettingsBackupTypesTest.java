package com.kooo.evcam.settings;

import com.kooo.evcam.AppConfig;
import org.junit.Test;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public class SettingsBackupTypesTest {
    @Test public void checksTypesEvenWhenNoPreferenceHasEverBeenSaved() {
        assertFalse(SettingsBackupTypes.accepts("app_config", "interface_theme", 1));
        assertFalse(SettingsBackupTypes.accepts("app_config", "video_storage_limit_gb", "10"));
        assertFalse(SettingsBackupTypes.accepts("app_config", "009_hud_side_size", 50L));
        assertFalse(SettingsBackupTypes.accepts("app_config", "source_id_surround", true));
        assertFalse(SettingsBackupTypes.accepts("profiles", "current", 1));
        assertFalse(SettingsBackupTypes.accepts("bird_eye_calibration", "camera_2_mirror", 1));
        assertFalse(SettingsBackupTypes.accepts("bird_eye_calibration", "camera_0_pitch", 40L));
        assertFalse(SettingsBackupTypes.accepts("bird_eye_window", "size", "45"));
        assertFalse(SettingsBackupTypes.accepts("bird_eye_appearance", "vehicle_colour", 1));
        assertTrue(SettingsBackupTypes.accepts("app_config", "interface_theme", "dark"));
        assertTrue("Unknown extension settings keep their encoded types", SettingsBackupTypes.accepts("app_config", "future_extension", 42L));
    }

    @Test public void everyDirectAppConfigReadHasATypeIndependentOfCurrentSavedValues() throws Exception {
        String source = new String(Files.readAllBytes(Paths.get("src/main/java/com/kooo/evcam/AppConfig.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        Pattern calls = Pattern.compile("(?:prefs\\.get|\\bread)(Boolean|Int|Long|Float|StringSet|String)\\(\\s*(KEY_[A-Z_0-9]+|\\\"[^\\\"]+\\\")\\s*,");
        Matcher call = calls.matcher(source); int checked = 0;
        while (call.find()) {
            String expression = call.group(2), key;
            if (expression.startsWith("\"")) key = expression.substring(1, expression.length()-1);
            else { Field field = AppConfig.class.getDeclaredField(expression); field.setAccessible(true); key = (String) field.get(null); }
            Class<?> type;
            switch (call.group(1)) {
                case "Boolean": type = Boolean.class; break;
                case "Int": type = Integer.class; break;
                case "Long": type = Long.class; break;
                case "Float": type = Float.class; break;
                case "StringSet": type = java.util.Set.class; break;
                default: type = String.class;
            }
            assertEquals("Imported key must match its actual consumer: " + key, type, SettingsBackupTypes.expected("app_config", key));
            checked++;
        }
        assertTrue("Consumer coverage must actually inspect the configuration source", checked > 100);
        for (SettingSpec spec : SettingsRegistry.ALL)
            assertEquals(spec.key, String.class, SettingsBackupTypes.expected("app_config", spec.key));
    }
}
