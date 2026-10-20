package com.kooo.evcam.zeekr;

import android.graphics.Bitmap;
import org.json.JSONObject;

/** Sole ownership of the explicit capture's pixels transfers from renderer to file writer. */
final class SideQualityPair implements AutoCloseable {
    final Bitmap original;
    Bitmap rendered;
    final JSONObject metadata;
    SideQualityPair(Bitmap original, Bitmap rendered, JSONObject metadata) {
        this.original=original;this.rendered=rendered;this.metadata=metadata;
    }
    @Override public void close() { original.recycle();if(rendered!=null)rendered.recycle(); }
}
