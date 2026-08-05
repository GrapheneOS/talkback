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

import android.view.KeyEvent;
import java.util.Map;

/**
 * Manages key combo code and key. KeyComboModel is responsible for persisting preferences of key
 * combo code.
 */
public interface KeyComboModel {
  int KEY_COMBO_CODE_UNASSIGNED = KeyEvent.KEYCODE_UNKNOWN;
  int KEY_COMBO_CODE_INVALID = -1;
  int NO_MODIFIER = 0;
  int NO_PREFIX_KEY_CODE = 0;

  // TODO: Migrating currently using (null) shared preference key to
  // classic_key_combo_model.
  String KEYMAP_SHORTCUT_START_KEY = "keycombo_shortcut";

  /**
   * Returns modifier of this model. If this model doesn't have modifier,
   * KEY_COMBO_MODEL_NO_MODIFIER will be returned.
   */
  int getTriggerModifier();

  /**
   * Notifies the model that preference of trigger modifier has changed. You must call this method
   * when you change preference of trigger modifier since the model might cache the value in it.
   */
  void notifyTriggerModifierChanged();

  /**
   * Returns preference key to be used for storing trigger modifier of this mode. Returns null if
   * this model doesn't support trigger modifier.
   */
  String getPreferenceKeyForTriggerModifier();

  /**
   * Returns map of key and KeyCombo. KeyCombo contains a key combo code that doesn't container
   * trigger modifier and a boolean that indicates whether the trigger modifier is used.
   */
  Map<String, KeyCombo> getKeyComboMap();

  /**
   * Gets key for preference that is assigned for keyCombo if keyCombo is not empty. If no
   * preference is assigned or keyCombo was empty, returns null.
   *
   * @param keyCombo KeyCombo that contains a key combo code, which doesn't contain the trigger
   *     modifier if model has it, and a boolean, which indicates whether the trigger modifier is
   *     used if model has it.
   */
  String getKeyForKeyCombo(KeyCombo keyCombo);

  /** Gets KeyCombo for key. An empty KeyCombo will be returned if key is invalid. */
  KeyCombo getKeyComboForKey(String key);

  /**
   * Gets default KeyCombo for key. An empty KeyCombo will be returned if no key combo is assigned
   * to the key or it's invalid.
   */
  KeyCombo getDefaultKeyCombo(String key);

  /**
   * Assigns keyCombo for preference.
   *
   * @param key key of key combo.
   * @param keyCombo key combo.
   */
  void saveKeyCombo(String key, KeyCombo keyCombo);

  /** Clears key combo assigned for preference key. */
  void clearKeyCombo(String key);

  /**
   * Returns true if keyCombo is eligible combination for this model. This method doesn't check
   * consistency with other key combo codes in this model. e.g. duplicated key combos.
   *
   * @param keyCombo KeyCombo object.
   */
  boolean isEligibleKeyCombo(KeyCombo keyCombo);

  /** Returns description of eligible key combination. This will be shown in the UI. */
  String getDescriptionOfEligibleKeyCombo();

  /** Updates key combo model. This method will be called when TalkBack is updated. */
  void updateVersion(int previousVersion);
}
