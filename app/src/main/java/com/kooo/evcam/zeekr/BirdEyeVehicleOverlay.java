package com.kooo.evcam.zeekr;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import com.kooo.evcam.AppLog;
import com.kooo.evcam.R;

/** Decorative vehicle only: never changes camera mapping or supplies underbody imagery. */
final class BirdEyeVehicleOverlay {
    // Original asset pixels: body x=180..792; mirrors extend to x=122..850.
    // Ignore transparent padding/fringe rather than scaling it into the footprint.
    private static final Rect SOURCE = new Rect(122, 50, 850, 1572);
    private static final float MIRROR_EXTENSION = 58f / 612f;
    private static Bitmap sharedArtwork;
    private static String sharedColour;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final RectF destination = new RectF();
    private Bitmap artwork;

    static boolean use009(boolean imageFitted, boolean mapping009) {
        return imageFitted || mapping009;
    }

    void prepare(Resources resources, boolean enabled, String colour) {
        artwork = enabled ? artwork(resources, BirdEyeAppearance.normalize(colour)) : null;
    }

    private static synchronized Bitmap artwork(Resources resources, String colour) {
        if (!colour.equals(sharedColour)) {
            sharedColour = colour;
            // Retain only the current shared colour. Old display lists own their bitmap
            // until Android releases it; never recycle it underneath another view.
            sharedArtwork = null;
            try {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inScaled = false;
                int resource = BirdEyeAppearance.GREEN.equals(colour) ? R.drawable.bird_eye_car_009_green
                        : BirdEyeAppearance.BLACK.equals(colour) ? R.drawable.bird_eye_car_009_black : R.drawable.bird_eye_car_009;
                sharedArtwork = BitmapFactory.decodeResource(resources, resource, options);
            } catch (RuntimeException | OutOfMemoryError error) {
                AppLog.w("BirdEyeVehicleOverlay", "Vehicle artwork unavailable; using outline");
            }
        }
        return sharedArtwork;
    }

    boolean draw(Canvas canvas, RectF footprint) {
        if (artwork == null || footprint.isEmpty()) return false;
        // The whole original footprint stays opaque, including gaps at the asset's corners.
        // A transparent sprite background must not imply that we can see under the vehicle.
        paint.setColor(0xff000000);
        canvas.drawRect(footprint, paint);
        float extension = footprint.width() * MIRROR_EXTENSION;
        destination.set(footprint.left - extension, footprint.top,
                footprint.right + extension, footprint.bottom);
        paint.setColor(0xffffffff);
        canvas.drawBitmap(artwork, SOURCE, destination, paint);
        return true;
    }
}
