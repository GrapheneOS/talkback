package com.google.android.accessibility.brailleime.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;

/** A dialog to reset all gestures. */
public class ResetAllGestureDialog extends CommonDialog {
  /** A callback to notify its client. */
  public interface ButtonClickListener {
    void onReset();
  }

  private Dialog dialog;
  private final Context context;
  private final ButtonClickListener callback;

  public ResetAllGestureDialog(Context context, ButtonClickListener callback) {
    this.context = context;
    this.callback = callback;
  }

  @Override
  protected Dialog makeDialog() {
    A11yAlertDialogWrapper.Builder dialogBuilder =
        A11yAlertDialogWrapper.materialDialogBuilder(
                new ContextThemeWrapper(context, R.style.A11yAlertDialogCustomViewTheme))
            .setTitle(R.string.reset_gestures_dialog_title)
            .setMessage(R.string.reset_gestures_dialog_message)
            .setPositiveButton(
                R.string.reset_gestures_dialog_positive_button,
                (dialogInterface, i) -> callback.onReset())
            .setNegativeButton(android.R.string.cancel, null);
    dialog = dialogBuilder.create().getDialog();
    dialog.setCanceledOnTouchOutside(false);
    return dialog;
  }
}
