/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.utils.TalkBackSharedPreferencesUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Key value store to make key combo code persistent. */
public class KeyComboPersister {
  private final Context context;
  private final SharedPreferences prefs;
  private final String keyPrefix;

  public KeyComboPersister(Context context, String keyPrefix) {
    this.context = context;
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    this.keyPrefix = keyPrefix;
  }

  public void saveKeyCombo(String key, KeyCombo keyCombo) {
    prefs
        .edit()
        .putLong(
            TalkBackSharedPreferencesUtils.getKeyForKeyComboCode(this.keyPrefix, key),
            keyCombo.getKeyComboCode())
        .apply();
    prefs
        .edit()
        .putBoolean(
            TalkBackSharedPreferencesUtils.getKeyForTriggerModifierUsed(
                this.keyPrefix, key, FeatureFlagReader.enableBrowseMode(context)),
            keyCombo.isTriggerModifierUsed())
        .apply();
  }

  public void remove(String key) {
    prefs
        .edit()
        .remove(TalkBackSharedPreferencesUtils.getKeyForKeyComboCode(this.keyPrefix, key))
        .apply();
    prefs
        .edit()
        .remove(
            TalkBackSharedPreferencesUtils.getKeyForTriggerModifierUsed(
                this.keyPrefix, key, FeatureFlagReader.enableBrowseMode(context)))
        .apply();
  }

  public boolean contains(String key) {
    return prefs.contains(
        TalkBackSharedPreferencesUtils.getKeyForKeyComboCode(this.keyPrefix, key));
  }

  public KeyCombo getKeyCombo(String key) {
    long keyComboCode = getKeyComboCode(key);
    boolean triggerModifierUsed = getTriggerModifierUsed(key);
    return new KeyCombo(keyComboCode, triggerModifierUsed);
  }

  public Long getKeyComboCode(String key) {
    return prefs.getLong(
        TalkBackSharedPreferencesUtils.getKeyForKeyComboCode(this.keyPrefix, key),
        KeyComboModel.KEY_COMBO_CODE_UNASSIGNED);
  }

  private boolean getTriggerModifierUsed(String key) {
    return prefs.getBoolean(
        TalkBackSharedPreferencesUtils.getKeyForTriggerModifierUsed(
            this.keyPrefix, key, FeatureFlagReader.enableBrowseMode(context)),
        true);
  }
}
