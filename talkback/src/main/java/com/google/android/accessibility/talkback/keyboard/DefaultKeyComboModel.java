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
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.Map;
import java.util.TreeMap;

/** Default key combo model. */
public class DefaultKeyComboModel implements KeyComboModel {
  public static final String PREF_KEY_PREFIX = "default_key_combo_model";

  private final Context context;
  // TODO: b/407061139 - Consider refactoring the key combo map to use a more efficient data
  // structure; e.g., a static ImmutableMap.
  private final Map<String, KeyCombo> keyComboMap = new TreeMap<>();
  private final KeyComboPersister persister;

  private int triggerModifier = KeyEvent.META_ALT_ON;

  public DefaultKeyComboModel(Context context) {
    this.context = context;
    persister = new KeyComboPersister(context, PREF_KEY_PREFIX);

    loadTriggerModifierFromPreferences();
    addKeyCombos();
  }

  private void loadTriggerModifierFromPreferences() {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);

    if (!prefs.contains(getPreferenceKeyForTriggerModifier())) {
      // Store default value in preferences to show it in preferences UI.
      prefs
          .edit()
          .putString(getPreferenceKeyForTriggerModifier(), getDefaultTriggerModifier())
          .apply();
    }

    String triggerModifier =
        prefs.getString(getPreferenceKeyForTriggerModifier(), getDefaultTriggerModifier());
    if (triggerModifier.equals(context.getString(R.string.trigger_modifier_alt_entry_value))) {
      this.triggerModifier = KeyEvent.META_ALT_ON;
    } else if (triggerModifier.equals(
        context.getString(R.string.trigger_modifier_meta_entry_value))) {
      this.triggerModifier = KeyEvent.META_META_ON;
    }
  }

  @Override
  public int getTriggerModifier() {
    return triggerModifier;
  }

  @Override
  public void notifyTriggerModifierChanged() {
    loadTriggerModifierFromPreferences();
  }

  @Override
  public String getPreferenceKeyForTriggerModifier() {
    return context.getString(R.string.pref_default_keymap_trigger_modifier_key);
  }

  @Override
  public Map<String, KeyCombo> getKeyComboMap() {
    return keyComboMap;
  }

  @Nullable
  @Override
  public String getKeyForKeyCombo(KeyCombo keyCombo) {
    if (keyCombo.getKeyComboCode() == KEY_COMBO_CODE_UNASSIGNED) {
      return null;
    }

    for (Map.Entry<String, KeyCombo> entry : keyComboMap.entrySet()) {
      if (entry.getValue().equals(keyCombo)) {
        return entry.getKey();
      }
    }

    return null;
  }

  @Override
  public KeyCombo getKeyComboForKey(@Nullable String key) {
    if (key != null && keyComboMap.containsKey(key)) {
      return keyComboMap.get(key);
    } else {
      return new KeyCombo();
    }
  }

  @Override
  public KeyCombo getDefaultKeyCombo(@Nullable String key) {
    if (key == null) {
      return new KeyCombo();
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_perform_click))) {
      return new KeyCombo(
          NO_MODIFIER, NO_PREFIX_KEY_CODE, KeyEvent.KEYCODE_ENTER, /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_perform_long_click))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_ENTER,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_read_from_top))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_ENTER,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_read_from_cursor_item))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_ENTER,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_talkback_context_menu))) {
      return new KeyCombo(
          NO_MODIFIER, NO_PREFIX_KEY_CODE, KeyEvent.KEYCODE_SPACE, /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_custom_actions))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_SPACE,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_language_options))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_L,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_toggle_search))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_SLASH,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_global_home))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_H,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_global_recents))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_R,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_global_back))) {
      return new KeyCombo(
          NO_MODIFIER, NO_PREFIX_KEY_CODE, KeyEvent.KEYCODE_DEL, /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_global_notifications))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_N,
          /* triggerModifierUsed= */ true);
    }

    if (FeatureSupport.supportMediaControls()
        && key.equals(context.getString(R.string.keycombo_shortcut_global_play_pause_media))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_SPACE,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(
        context.getString(R.string.keycombo_shortcut_global_scroll_forward_reading_menu))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_DOWN,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(
        context.getString(R.string.keycombo_shortcut_global_scroll_backward_reading_menu))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_UP,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(
        context.getString(R.string.keycombo_shortcut_global_adjust_reading_setting_next))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_DOWN,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(
        context.getString(R.string.keycombo_shortcut_global_adjust_reading_settings_previous))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_UP,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_default))) {
      return new KeyCombo(
          NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_RIGHT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_default))) {
      return new KeyCombo(
          NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_LEFT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_up))) {
      return new KeyCombo(
          NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_UP,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_down))) {
      return new KeyCombo(
          NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_DOWN,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_first))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_LEFT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_last))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_RIGHT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_window))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_DOWN,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_window))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_UP,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_character))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_RIGHT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_character))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_LEFT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_word))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_RIGHT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_word))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_LEFT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_aria_landmark))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_D,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_aria_landmark))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_D,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_button))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_B,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_button))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_B,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_checkbox))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_X,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_checkbox))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_X,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_combobox))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_Z,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_combobox))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_Z,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_control))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_C,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_control))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_C,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_edit_field))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_E,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_edit_field))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_E,
          /* triggerModifierUsed= */ true);
    }

    if (true) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_focusable_item))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_F,
            /* triggerModifierUsed= */ true);
      }

      if (key.equals(
          context.getString(R.string.keycombo_shortcut_navigate_previous_focusable_item))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_F,
            /* triggerModifierUsed= */ true);
      }
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_graphic))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_G,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_graphic))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_G,
          /* triggerModifierUsed= */ true);
    }
    
    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_H,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_H,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_1))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_1,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_1))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_1,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_2))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_2,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_2))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_2,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_3))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_3,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_3))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_3,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_4))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_4,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_4))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_4,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_5))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_5,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_5))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_5,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_6))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_6,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_6))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_6,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_link))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_L,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_link))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_L,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_list))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_O,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_list))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_O,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_list_item))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_I,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_list_item))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_I,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_table))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_T,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_table))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_T,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_visited_link))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_V,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_visited_link))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_V,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_unvisited_link))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_U,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(
        context.getString(R.string.keycombo_shortcut_navigate_previous_unvisited_link))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_U,
          /* triggerModifierUsed= */ true);
    }

    return new KeyCombo();
  }

  @Override
  public void saveKeyCombo(String key, KeyCombo keyCombo) {
    persister.saveKeyCombo(key, keyCombo);
    keyComboMap.put(key, keyCombo);
  }

  @Override
  public void clearKeyCombo(String key) {
    saveKeyCombo(key, new KeyCombo());
  }

  @Override
  public boolean isEligibleKeyCombo(KeyCombo keyCombo) {
    if (keyCombo.getKeyComboCode() == KEY_COMBO_CODE_UNASSIGNED) {
      return true;
    }

    // Do not allow a key combo which consists of only modifiers.
    int keyCode = keyCombo.getKeyCode();
    if (KeyEvent.isModifierKey(keyCode) || keyCode == KeyEvent.KEYCODE_UNKNOWN) {
      return false;
    }

    // It's not allowed to include the trigger modifier as part of KeyCombo's modifiers.
    if ((keyCombo.getModifiers() & getTriggerModifier()) != 0) {
      return false;
    }

    // In the default keymap, a key combo must contain the trigger modifier.
    return keyCombo.isTriggerModifierUsed();
  }

  @Override
  public String getDescriptionOfEligibleKeyCombo() {
    return context.getString(
        R.string.keycombo_assign_dialog_default_keymap_instruction, getTriggerModifierName());
  }

  @Override
  public void updateVersion(int previousVersion) {
    if (previousVersion < 50200001) {
      // From version 50200001, we've renamed keycombo_shortcut_navigate_next and
      // keycombo_shortcut_navigate_previous to keycombo_shortcut_navigate_next_default and
      // keycombo_shortcut_navigate_previous_default respectively.
      moveKeyComboPreferenceValue(
          context.getString(R.string.keycombo_shortcut_navigate_next),
          context.getString(R.string.keycombo_shortcut_navigate_next_default));
      moveKeyComboPreferenceValue(
          context.getString(R.string.keycombo_shortcut_navigate_previous),
          context.getString(R.string.keycombo_shortcut_navigate_previous_default));
    }
  }

  private String getTriggerModifierName() {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String triggerModifier =
        prefs.getString(getPreferenceKeyForTriggerModifier(), getDefaultTriggerModifier());
    String triggerModifierName = "";

    if (triggerModifier.equals(context.getString(R.string.trigger_modifier_alt_entry_value))) {
      triggerModifierName = context.getString(R.string.keycombo_key_modifier_alt);
    } else if (triggerModifier.equals(
        context.getString(R.string.trigger_modifier_meta_entry_value))) {
      triggerModifierName = context.getString(R.string.keycombo_key_modifier_meta);
    }

    return triggerModifierName;
  }

  private String getDefaultTriggerModifier() {
    if (FormFactorUtils.isAndroidPc()) {
      return context.getString(R.string.trigger_modifier_meta_entry_value);
    } else {
      return context.getString(R.string.trigger_modifier_alt_entry_value);
    }
  }

  private void addKeyCombos() {
    addKeyCombo(context.getString(R.string.keycombo_shortcut_perform_click));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_perform_long_click));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_other_read_from_top));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_other_read_from_cursor_item));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_other_talkback_context_menu));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_other_custom_actions));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_other_language_options));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_other_toggle_search));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_back));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_rate_increase));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_rate_decrease));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_default));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_default));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_up));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_down));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_first));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_last));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_word));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_word));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_character));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_character));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_button));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_button));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_control));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_control));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_checkbox));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_checkbox));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_aria_landmark));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_aria_landmark));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_edit_field));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_edit_field));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_graphic));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_graphic));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_heading));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_heading));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_heading_1));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_1));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_heading_2));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_2));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_heading_3));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_3));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_heading_4));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_4));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_heading_5));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_5));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_heading_6));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_6));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_list_item));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_list_item));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_link));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_link));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_list));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_list));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_table));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_table));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_combobox));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_combobox));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_visited_link));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_visited_link));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_unvisited_link));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_unvisited_link));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_focusable_item));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_focusable_item));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_window));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_window));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_home));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_recents));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_notifications));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_play_pause_media));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_scroll_forward_reading_menu));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_scroll_backward_reading_menu));
    addKeyCombo(
        context.getString(R.string.keycombo_shortcut_global_adjust_reading_settings_previous));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_adjust_reading_setting_next));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_other_copy_last_spoken_phrase));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_global_hide_or_show_screen));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_row));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_row));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_column));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_column));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_row_bounds));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_row_bounds));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_column_bounds));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_column_bounds));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_table_bounds));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_table_bounds));
  }

  private void addKeyCombo(String key) {
    if (!persister.contains(key)) {
      KeyCombo defaultKeyCombo = getDefaultKeyCombo(key);
      persister.saveKeyCombo(key, defaultKeyCombo);
    }

    KeyCombo keyCombo = persister.getKeyCombo(key);
    keyComboMap.put(key, keyCombo);
  }

  /**
   * Move key combo preference value from fromKey to toKey. Original value in fromKey is deleted.
   */
  private void moveKeyComboPreferenceValue(String fromKey, String toKey) {
    if (!persister.contains(fromKey)) {
      return;
    }

    KeyCombo keyCombo = persister.getKeyCombo(fromKey);
    saveKeyCombo(toKey, keyCombo);
    persister.remove(fromKey);
  }
}
