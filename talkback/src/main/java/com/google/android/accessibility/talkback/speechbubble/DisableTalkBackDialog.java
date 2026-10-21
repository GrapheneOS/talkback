/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.talkback.speechbubble;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.dialog.BaseDialog;

/** Dialog to confirm turning off TalkBack. */
public class DisableTalkBackDialog extends BaseDialog {

  /** Callback interface to handle the action of turning off TalkBack. */
  public interface OnTalkBackDisableListener {
    /** Called when the user confirms turning off TalkBack. */
    void onTalkBackDisableConfirmed();
  }

  @Nullable private OnTalkBackDisableListener onTalkBackDisableListener;

  /**
   * Constructs a {@code DisableTalkBackDialog}.
   *
   * @param context The context.
   */
  public DisableTalkBackDialog(Context context) {
    super(context, R.string.talkback_turn_off_title, null);
    setPositiveButtonStringRes(R.string.talkback_turn_off_button);
    setNegativeButtonStringRes(R.string.talkback_keep_on_button);
  }

  /**
   * Sets the listener to be called when the user confirms turning off TalkBack.
   *
   * @param listener The listener.
   */
  public void setOnTalkBackDisableListener(@Nullable OnTalkBackDisableListener listener) {
    this.onTalkBackDisableListener = listener;
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    if (buttonClicked == DialogInterface.BUTTON_POSITIVE) {
      if (onTalkBackDisableListener != null) {
        onTalkBackDisableListener.onTalkBackDisableConfirmed();
      }
    }
  }

  @Override
  public void handleDialogDismiss() {
    // No specific action needed on dismiss
  }

  @Override
  @Nullable
  public String getMessageString() {
    return null;
  }

  @Override
  @Nullable
  public View getCustomizedView(LayoutInflater inflater) {
    // No custom view needed for this dialog
    return null;
  }
}
