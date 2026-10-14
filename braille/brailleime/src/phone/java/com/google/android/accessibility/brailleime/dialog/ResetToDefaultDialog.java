package com.google.android.accessibility.brailleime.dialog;

import static com.google.android.accessibility.brailleime.Utils.getCombinedGestureDescription;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.brailleime.BrailleImeGestureAction;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;

/** A dialog to reset an action to its default gesture. */
public class ResetToDefaultDialog extends CommonDialog {
  private final Context context;
  private final ButtonClickListener callback;
  private BrailleImeAction action;

  /** A callback to notify its client. */
  public interface ButtonClickListener {
    void onReset();
  }

  public ResetToDefaultDialog(Context context, ButtonClickListener callback) {
    this.context = context;
    this.callback = callback;
  }

  /** Sets the {@link BrailleImeAction}. */
  public void setAction(BrailleImeAction action) {
    this.action = action;
  }

  @Override
  protected Dialog makeDialog() {
    A11yAlertDialogWrapper.Builder dialogBuilder =
        A11yAlertDialogWrapper.materialDialogBuilder(
                new ContextThemeWrapper(context, R.style.A11yAlertDialogCustomViewTheme))
            .setTitle(R.string.reset_to_default_dialog_title)
            .setMessage(
                context.getString(
                    R.string.reset_to_default_dialog_message,
                    getCombinedGestureDescription(
                        context, BrailleImeGestureAction.getDefaultGesture(action))))
            .setPositiveButton(
                R.string.reset_to_default_dialog_positive_button,
                (dialogInterface, i) -> callback.onReset())
            .setNegativeButton(android.R.string.cancel, null);
    return dialogBuilder.create().getDialog();
  }
}
