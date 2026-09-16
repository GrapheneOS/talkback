package com.google.android.accessibility.brailleime.dialog;

import android.app.Dialog;
import android.content.Context;
import android.view.ContextThemeWrapper;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.brailleime.BrailleImeGestureAction;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.brailleime.input.Gesture;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.util.Optional;

/** A dialog to notify the user that the gesture is mirrored. */
public class MirroredGestureDialog extends CommonDialog {
  /** A callback to notify its client. */
  public interface ButtonClickListener {
    void onMirrored(Gesture gesture);
  }

  private Dialog dialog;
  private final Context context;
  private final ButtonClickListener callback;
  private Gesture gesture;
  private BrailleImeAction action;

  public MirroredGestureDialog(Context context, ButtonClickListener callback) {
    this.context = context;
    this.callback = callback;
  }

  /** Sets the gesture and action. */
  public void setGestureAction(BrailleImeAction action, Gesture gesture) {
    this.gesture = gesture;
    this.action = action;
  }

  @Override
  protected Dialog makeDialog() {
    A11yAlertDialogWrapper.Builder dialogBuilder =
        A11yAlertDialogWrapper.materialDialogBuilder(
                new ContextThemeWrapper(context, R.style.A11yAlertDialogCustomViewTheme))
            .setTitle(R.string.mirrored_gesture_dialog_title)
            .setMessage(getMessage())
            .setPositiveButton(
                R.string.mirrored_gesture_dialog_positive_button,
                (dialogInterface, i) -> callback.onMirrored(gesture))
            .setNegativeButton(
                hasConflict()
                    ? R.string.mirrored_gesture_dialog_conflict_negative_button
                    : R.string.mirrored_gesture_dialog_negative_button,
                (dialog, which) -> dismiss());
    dialog = dialogBuilder.create().getDialog();
    dialog.setCanceledOnTouchOutside(false);
    return dialog;
  }

  private String getMessage() {
    String mirroredGestureStr = gesture.mirrorDots().getDescription(context.getResources());
    String actionStr = action.getDescriptionRes(context.getResources());
    String message =
        context.getString(R.string.mirrored_gesture_dialog_message, actionStr, mirroredGestureStr);
    if (hasConflict()) {
      Optional<BrailleImeAction> optionalBrailleImeAction =
          BrailleImeGestureAction.getAction(context, gesture.mirrorDots()).stream()
              .filter(brailleImeAction -> !action.equals(brailleImeAction))
              .findFirst();
      String oldAction =
          optionalBrailleImeAction
              .map(brailleImeAction -> brailleImeAction.getDescriptionRes(context.getResources()))
              .orElse("");
      message =
          context.getString(
              R.string.mirrored_gesture_dialog_conflict_message,
              mirroredGestureStr,
              actionStr,
              oldAction);
    }
    return message;
  }

  private boolean hasConflict() {
    return !gesture.equals(gesture.mirrorDots())
        && !BrailleImeGestureAction.getAction(context, gesture.mirrorDots()).isEmpty();
  }
}
