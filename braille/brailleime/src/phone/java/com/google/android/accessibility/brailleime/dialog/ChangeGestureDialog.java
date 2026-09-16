package com.google.android.accessibility.brailleime.dialog;

import android.app.Dialog;
import android.content.Context;
import android.text.SpannableString;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.brailleime.Utils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;

/** A dialog to change the gesture. */
public class ChangeGestureDialog extends CommonDialog {
  private static final String BRAILLE_KEYBOARD_HELP_URI =
      "https://support.google.com/accessibility/android/answer/9728765";

  /** A callback to notify its client. */
  public interface ButtonClickListener {
    void onContinue();

    void onNeverShown();
  }

  private Dialog dialog;
  private final Context context;
  private final ButtonClickListener callback;

  public ChangeGestureDialog(Context context, ButtonClickListener callback) {
    this.context = context;
    this.callback = callback;
  }

  @Override
  protected Dialog makeDialog() {
    A11yAlertDialogWrapper.Builder builder = A11yAlertDialogWrapper.materialDialogBuilder(context);
    View view =
        LayoutInflater.from(context)
            .inflate(R.layout.braille_common_dialog_with_checkbox, /* root= */ null);
    CheckBox dontShowAgainCheckBox = view.findViewById(R.id.check_box);
    dontShowAgainCheckBox.setText(R.string.change_gestures_dialog_dont_show_checkbox);
    TextView messageTextView = view.findViewById(R.id.text_view);
    SpannableString spannableMessageString =
        SpannableString.valueOf(context.getString(R.string.change_gestures_dialog_message));
    String subString = context.getString(R.string.change_gestures_dialog_message_link);
    Utils.formatSubstringAsUrl(spannableMessageString, subString, BRAILLE_KEYBOARD_HELP_URI);
    messageTextView.setText(spannableMessageString);
    builder =
        builder
            .setTitle(R.string.change_gestures_dialog_message_title)
            .setView(view)
            .setPositiveButton(
                R.string.change_gestures_dialog_positive_button,
                (dialog, which) -> {
                  dismiss();
                  callback.onContinue();
                  if (dontShowAgainCheckBox.isChecked()) {
                    callback.onNeverShown();
                  }
                })
            .setNegativeButton(android.R.string.cancel, (dialog, which) -> dismiss());
    dialog = builder.create().getDialog();
    dialog.setCanceledOnTouchOutside(false);
    return dialog;
  }
}
