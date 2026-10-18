package com.kooo.evcam.zeekr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.ParcelFileDescriptor;
import android.view.ContextThemeWrapper;
import android.view.ViewGroup;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.kooo.evcam.AppConfig;
import com.kooo.evcam.AutoFitTextureView;
import com.kooo.evcam.R;
import com.kooo.evcam.settings.AppTheme;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AppThemePaletteAndroidTest {
    private void main(Runnable r) { InstrumentationRegistry.getInstrumentation().runOnMainSync(r); }
    @Test public void serviceOwnedButtonsChangePaletteWithoutReplacingTextureOrWindow() throws Exception {
        Context target = InstrumentationRegistry.getInstrumentation().getTargetContext();
        try (ParcelFileDescriptor fd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                .executeShellCommand("appops set " + target.getPackageName() + " SYSTEM_ALERT_WINDOW allow");
             java.io.InputStream in = new ParcelFileDescriptor.AutoCloseInputStream(fd)) { while (in.read() != -1) {} }
        RearViewMirrorView[] view = {null};
        int old = AppCompatDelegate.getDefaultNightMode();
        try {
            main(() -> { view[0] = new RearViewMirrorView(new ContextThemeWrapper(target, R.style.Theme_Cam),
                    new AppConfig(target)); view[0].show(); });
            InstrumentationRegistry.getInstrumentation().waitForIdleSync();
            AutoFitTextureView texture = view[0].getTextureView();
            android.graphics.SurfaceTexture surface = texture.getSurfaceTexture();
            assertNotNull(surface);
            for (String mode : new String[]{AppTheme.LIGHT, AppTheme.DARK, AppTheme.SYSTEM}) {
                main(() -> {
                    AppTheme.apply(mode);
                    Bitmap bitmap = Bitmap.createBitmap(view[0].getWidth(), view[0].getHeight(), Bitmap.Config.ARGB_8888);
                    try { view[0].draw(new Canvas(bitmap)); } finally { bitmap.recycle(); }
                    SideViewControls controls = (SideViewControls) view[0].getChildAt(1);
                    ImageButton button = (ImageButton) ((ViewGroup) controls).getChildAt(0);
                    assertEquals(AppTheme.palette(target).getResources().getColor(R.color.side_control_icon, null),
                            button.getImageTintList().getDefaultColor());
                    assertSame(texture, view[0].getTextureView()); assertSame(surface, texture.getSurfaceTexture());
                    assertTrue(view[0].isShowing());
                });
            }
        } finally { main(() -> { if (view[0] != null) view[0].hide();
            AppTheme.apply(AppTheme.SYSTEM); AppCompatDelegate.setDefaultNightMode(old); }); }
    }
}
