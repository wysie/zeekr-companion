package com.kooo.evcam.ui;
import android.app.Activity;import android.content.*;import com.google.android.material.dialog.MaterialAlertDialogBuilder;import com.kooo.evcam.R;
public final class ExternalLinks {
 private ExternalLinks(){}
 public static void showCopyLink(Activity activity,String url){
  androidx.appcompat.app.AlertDialog dialog=CamDialogs.show(new MaterialAlertDialogBuilder(activity,R.style.Theme_Cam_MaterialAlertDialog)
   .setTitle(R.string.ui_browser_unavailable).setMessage(url).setNegativeButton(R.string.action_close,null)
   .setPositiveButton(R.string.ui_copy_link,(d,w)->{ClipboardManager clipboard=(ClipboardManager)activity.getSystemService(Context.CLIPBOARD_SERVICE);
    if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.ui_copy_link),url));}));
  android.widget.TextView message=dialog.findViewById(android.R.id.message);if(message!=null)message.setTextIsSelectable(true);
 }
}
