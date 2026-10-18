package com.kooo.evcam.ui;
import android.content.Context;import com.google.android.material.dialog.MaterialAlertDialogBuilder;import com.kooo.evcam.R;
/** Destructive actions run only after the native dialog's explicit confirmation. */
public final class ResetConfirmation {
 private ResetConfirmation(){}
 public static void show(Context context,CharSequence title,int scope,Runnable confirmed) {
  CamDialogs.showDestructive(new MaterialAlertDialogBuilder(context,R.style.Theme_Cam_MaterialAlertDialog)
    .setTitle(title).setMessage(scope).setNegativeButton(R.string.action_cancel,null)
    .setPositiveButton(R.string.ui_reset_confirm,(dialog,which)->confirmed.run()));
 }
}
