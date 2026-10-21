/*
 * Copyright (C) 2023 Google Inc.
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
package com.google.android.accessibility.talkback.compositor;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.keyboard.KeyCombo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;

/** Utils class that provides common methods for keyComboManager. */
public final class KeyComboManagerUtils {

  /** Returns user friendly string representations of key combo code */
  public static String getKeyComboStringRepresentation(
      Context context, @Nullable KeyComboManager keyComboManager, @StringRes int keyStringResId) {
    if (keyComboManager == null) {
      return "";
    }
    // Browse mode toggle shortcut is currently hardcoded in the summary string of its
    // `KeyboardShortcutDialogPreference` entry in new_key_combo_preferences.xml.
    if (keyStringResId == R.string.keycombo_shortcut_other_toggle_browse_mode) {
      return context.getString(R.string.keycombo_summary_other_toggle_browse_mode);
    }

    KeyCombo keyCombo = getKeyComboForKey(context, keyComboManager, keyStringResId);

    return keyComboManager.getKeyComboStringRepresentation(keyCombo);
  }

  /** Gets key combo code for key. KEY_COMBO_CODE_UNASSIGNED will be returned if key is invalid. */
  public static long getKeyComboCodeForKey(
      Context context, @Nullable KeyComboManager keyComboManager, @StringRes int keyStringResId) {
    KeyCombo keyCombo = getKeyComboForKey(context, keyComboManager, keyStringResId);
    if (keyCombo != null) {
      return keyCombo.getKeyComboCode();
    }
    return KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
  }

  @Nullable
  private static KeyCombo getKeyComboForKey(
      Context context, @Nullable KeyComboManager keyComboManager, @StringRes int keyStringResId) {
    if (keyComboManager != null) {
      return keyComboManager
          .getKeyComboModel()
          .getKeyComboForKey(context.getString(keyStringResId));
    }
    return null;
  }

  private KeyComboManagerUtils() {}
}
