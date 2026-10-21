/*
 * Copyright 2025 Google Inc.
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

package com.google.android.accessibility.talkback.actor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.dialog.BaseDialog;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Dialog to show when voice input is not supported in the current Gboard version. */
public class VoiceInputDownloadDialog extends BaseDialog {
  private static final int SHOW_DIALOG_RES_ID = R.string.pref_show_voice_input_download_dialog;
  private static final String GBOARD_PLAYSTORE_URL =
      "https://play.google.com/store/apps/details?id=com.google.android.inputmethod.latin";
  private final SharedPreferences prefs;
  private boolean showDownloadButton = false;

  public VoiceInputDownloadDialog(Context context, boolean hasPlayStore) {
    super(context, R.string.dialog_title_voice_input_download, /* pipeline= */ null);
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    this.showDownloadButton = hasPlayStore;
    if (showDownloadButton) {
      setPositiveButtonStringRes(R.string.label_update_gboard);
    } else {
      setIncludeNegativeButton(false);
    }
  }

  private static void openGboardPlayStorePage(Context context) {
    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(GBOARD_PLAYSTORE_URL));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  public boolean getShouldShowDialogPref() {
    return prefs.getBoolean(context.getString(SHOW_DIALOG_RES_ID), true);
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    if (buttonClicked == DialogInterface.BUTTON_POSITIVE && showDownloadButton) {
      openGboardPlayStorePage(context);
    }
  }

  @Override
  public void handleDialogDismiss() {
    SharedPreferencesUtils.putBooleanPref(prefs, context.getResources(), SHOW_DIALOG_RES_ID, false);
  }

  @Override
  public String getMessageString() {
    return context.getString(R.string.dialog_message_voice_input_download);
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    return null;
  }
}
