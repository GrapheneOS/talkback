package com.google.android.accessibility.brailleime.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;

/** Custom braille keyboard when TalkBack is off dialog. */
public class CustomGestureTalkBackOffDialog extends CommonDialog {

  /** A callback to notify its client. */
  public interface ButtonClickListener {
    void onLaunchSettings();
  }

  private final Context context;
  private final ButtonClickListener callback;

  public CustomGestureTalkBackOffDialog(Context context, ButtonClickListener callback) {
    this.context = context;
    this.callback = callback;
  }

  @Override
  protected Dialog makeDialog() {
    A11yAlertDialogWrapper.Builder dialogBuilder =
        A11yAlertDialogWrapper.materialDialogBuilder(
                new ContextThemeWrapper(context, R.style.A11yAlertDialogCustomViewTheme))
            .setTitle(R.string.talkback_off_custom_gestures_dialog_title)
            .setMessage(R.string.talkback_off_custom_gestures_dialog_message)
            .setPositiveButton(
                R.string.talkback_off_custom_gestures_dialog_positive_button,
                (dialogInterface, i) -> callback.onLaunchSettings())
            .setNegativeButton(android.R.string.cancel, null);
    return dialogBuilder.create().getDialog();
  }
}
