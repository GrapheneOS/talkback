/*
 * Copyright (C) 2025 Google Inc.
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

package com.google.android.accessibility.talkback.keyboard;

import static androidx.core.content.res.ResourcesCompat.ID_NULL;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.StringRes;
import com.android.talkback.TalkBackPreferencesActivity;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.dialog.BaseDialog;
import com.google.android.accessibility.talkback.preference.base.TalkBackKeyboardShortcutPreferenceFragment;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.preference.PreferencesActivity;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/** A dialog to notify users of the new keymap. */
public class NewKeymapNotificationDialog extends BaseDialog {
  private static final String TAG = "NewKeymapNotificationDialog";

  /** The URL to open when the user clicks the neutral button. */
  // TODO: b/410892855 - Update the URL to the final one.
  public static final Uri NEW_KEYMAP_HELP_PAGE_URI =
      Uri.parse("https://support.google.com/accessibility/android/answer/6110948");

  @StringRes private int messageResId = ID_NULL;

  public NewKeymapNotificationDialog(
      Context context, @StringRes int titleResId, @StringRes int messageResId) {
    super(context, titleResId, /* pipeline= */ null);
    this.messageResId = messageResId;
    setPositiveButtonStringRes(R.string.keycombo_new_keymap_notification_dialog_positive_button);
    setNegativeButtonStringRes(R.string.keycombo_new_keymap_notification_dialog_negative_button);
    setNeutralButtonStringRes(R.string.keycombo_new_keymap_notification_dialog_neutral_button);
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    Intent intent;
    switch (buttonClicked) {
      case DialogInterface.BUTTON_NEGATIVE -> {
        // Open the keyboard shortcuts settings page.
        intent = new Intent(context, TalkBackPreferencesActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(
            PreferencesActivity.FRAGMENT_NAME,
            TalkBackKeyboardShortcutPreferenceFragment.class.getName());
        context.startActivity(intent);
      }
      case DialogInterface.BUTTON_NEUTRAL -> {
        // Open the help center page for the new keymap.
        intent = new Intent(Intent.ACTION_VIEW, NEW_KEYMAP_HELP_PAGE_URI);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
      }
      default -> {
        // Do nothing.
      }
    }

    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    if (prefs == null) {
      LogUtils.w(TAG, "handleDialogClick: prefs is null");
      return;
    }

    SharedPreferencesUtils.putIntPref(
        prefs, context.getResources(), R.string.pref_notify_new_keymap_key, buttonClicked);
  }

  @Override
  public void handleDialogDismiss() {}

  @Override
  public String getMessageString() {
    return context.getString(messageResId);
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    return null;
  }
}
