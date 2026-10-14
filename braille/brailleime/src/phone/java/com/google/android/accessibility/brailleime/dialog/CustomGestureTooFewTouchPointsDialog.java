package com.google.android.accessibility.brailleime.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;

/** Dialog for showing the number of touch points is too few for custom gesture. */
public class CustomGestureTooFewTouchPointsDialog extends CommonDialog {
  private final Context context;

  public CustomGestureTooFewTouchPointsDialog(Context context) {
    this.context = context;
  }

  @Override
  protected Dialog makeDialog() {
    A11yAlertDialogWrapper.Builder dialogBuilder =
        A11yAlertDialogWrapper.materialDialogBuilder(
            new ContextThemeWrapper(context, R.style.A11yAlertDialogCustomViewTheme));
    return dialogBuilder
        .setTitle(context.getString(R.string.not_enough_touch_points_dialog_title))
        .setMessage(context.getString(R.string.not_enough_touch_points_dialog_message))
        .setPositiveButton(context.getString(R.string.not_enough_touch_points_dialog_button), null)
        .create()
        .getDialog();
  }
}
