package com.google.android.accessibility.brailleime.dialog;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.accessibility.brailleime.BrailleImeGestureAction.getActionWithSameRoot;
import static com.google.android.accessibility.brailleime.Utils.getCombinedGestureDescription;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.brailleime.BrailleImeGestureAction;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.util.List;

/** A dialog for showing custom gesture commands. */
public class CustomGestureDialog extends CommonDialog {

  /** A callback to notify its client. */
  public interface ButtonClickListener {
    void onChangeGesture();

    void onResetToDefault();

    void onRemove();
  }

  private final Context context;
  private final ButtonClickListener callback;
  private BrailleImeAction action;

  public CustomGestureDialog(Context context, ButtonClickListener callback) {
    this.context = context;
    this.callback = callback;
  }

  /** Sets the {@link BrailleImeAction}. */
  public void setAction(BrailleImeAction action) {
    this.action = action;
  }

  @Override
  protected Dialog makeDialog() {
    final View dialogView =
        LayoutInflater.from(context).inflate(R.layout.custom_gesture_dialog, /* root= */ null);
    TextView assignedAction = dialogView.findViewById(R.id.assigned_action);
    assignedAction.setText(
        getCombinedGestureDescription(
            context, BrailleImeGestureAction.getGesture(context, action)));
    TextView shareText = dialogView.findViewById(R.id.share_text);
    List<BrailleImeAction> sameAction = getActionWithSameRoot(action);
    if (!sameAction.isEmpty()) {
      shareText.setVisibility(VISIBLE);
      shareText.setText(
          context.getString(
              R.string.custom_gestures_dialog_share_description,
              sameAction.get(0).getDescriptionRes(context.getResources())));
    } else {
      shareText.setVisibility(GONE);
    }
    Button changeGesture = dialogView.findViewById(R.id.custom_gesture_button_change_gesture);
    changeGesture.setOnClickListener(
        v -> {
          dismiss();
          callback.onChangeGesture();
        });
    Button restToDefault = dialogView.findViewById(R.id.custom_gestures_button_reset_to_default);
    restToDefault.setOnClickListener(
        v -> {
          dismiss();
          callback.onResetToDefault();
        });
    Button remove = dialogView.findViewById(R.id.custom_gesture_button_remove);
    remove.setOnClickListener(
        v -> {
          dismiss();
          callback.onRemove();
        });
    Button cancel = dialogView.findViewById(R.id.custom_gesture_button_cancel);
    cancel.setOnClickListener(v -> dismiss());
    A11yAlertDialogWrapper.Builder dialogBuilder =
        A11yAlertDialogWrapper.materialDialogBuilder(context)
            .setTitle(action.getDescriptionRes(context.getResources()))
            .setView(dialogView);
    return dialogBuilder.create().getDialog();
  }
}
