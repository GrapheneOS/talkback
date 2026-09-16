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

package com.google.android.accessibility.talkback.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.util.Map;
import java.util.TreeMap;

/** New key combo model. */
public class NewKeyComboModel implements KeyComboModel {
  public static final String PREF_KEY_PREFIX = "new_key_combo_model";

  private final Context context;
  // TODO: b/407061139 - Consider refactoring the key combo map to use a more efficient data
  // structure; e.g., a static ImmutableMap.
  private final Map<String, KeyCombo> keyComboMap = new TreeMap<>();
  private final KeyComboPersister persister;
  private final int triggerModifier = KeyEvent.META_META_ON;

  public NewKeyComboModel(Context context) {
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
    return context.getString(R.string.pref_new_keymap_trigger_modifier_key);
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
          NO_MODIFIER, NO_PREFIX_KEY_CODE, KeyEvent.KEYCODE_SPACE, /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_perform_long_click))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_SPACE,
          /* triggerModifierUsed= */ true);
    }

    if (FeatureFlagReader.enableDoubleClickKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_perform_double_click))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_SPACE,
            /* triggerModifierUsed= */ true);
      }
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_read_from_top))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_R,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_read_from_cursor_item))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_R,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_talkback_context_menu))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_M,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_custom_actions))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_A,
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

    if (FeatureSupport.supportMediaControls()
        && key.equals(context.getString(R.string.keycombo_shortcut_global_play_pause_media))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_ENTER,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(
        context.getString(R.string.keycombo_shortcut_global_scroll_forward_reading_menu))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_RIGHT,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(
        context.getString(R.string.keycombo_shortcut_global_scroll_backward_reading_menu))) {
      return new KeyCombo(
          KeyEvent.META_SHIFT_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_LEFT,
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
          KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_DOWN,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_window))) {
      return new KeyCombo(
          KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_DPAD_UP,
          /* triggerModifierUsed= */ true);
    }

    if (FeatureFlagReader.enableContainerNavigationKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_container))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            /* triggerModifierUsed= */ true);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_container))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_LEFT,
            /* triggerModifierUsed= */ true);
      }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Beginning of the Browse Mode only commands.
    if (FeatureFlagReader.enableBrowseMode(context)) {
      // Note that the default key combo for the browse mode toggle is added only to display it in
      // the keyboard shortcut settings page. `KeyComboManager.matchAndPerformKeyCombo()` should
      // not handle this key combo as `KeyComboManager.onKeyDown()` handles it separately. Also,
      // `KeyboardShortcutDialogPreference` does not use this `KeyCombo` to show its key shortcut;
      // instead, it references the android:summary attribute defined in the xml directly.
      // TODO: b/440094156 - Dynamically update the key combo for the browse mode toggle based on
      // the chosen trigger modifier.
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode))) {
        return new KeyCombo(
            NO_MODIFIER,
            KeyEvent.KEYCODE_META_RIGHT,
            KEY_COMBO_CODE_UNASSIGNED,
            /* triggerModifierUsed= */ true);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_character))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_character))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_LEFT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_word))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_word))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_LEFT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_line))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_UP,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_line))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_DOWN,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_aria_landmark))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_D,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(
          context.getString(R.string.keycombo_shortcut_navigate_previous_aria_landmark))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_D,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_button))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_B,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_button))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_B,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_checkbox))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_X,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_checkbox))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_X,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_combobox))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_C,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_combobox))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_C,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_control))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_F,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_control))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_F,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_edit_field))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_E,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_edit_field))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_E,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_graphic))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_G,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_graphic))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_G,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_H,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_H,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_1))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_1,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_1))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_1,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_2))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_2,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_2))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_2,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_3))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_3,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_3))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_3,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_4))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_4,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_4))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_4,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_5))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_5,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_5))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_5,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_heading_6))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_6,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_heading_6))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_6,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_link))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_K,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_link))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_K,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_list))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_L,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_list))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_L,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_list_item))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_I,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_list_item))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_I,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_radio))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_R,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_radio))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_R,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_table))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_T,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_table))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_T,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_visited_link))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_V,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(
          context.getString(R.string.keycombo_shortcut_navigate_previous_visited_link))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_V,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_unvisited_link))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_U,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(
          context.getString(R.string.keycombo_shortcut_navigate_previous_unvisited_link))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_U,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_column))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_column))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_LEFT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_column_bounds))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_DOWN,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(
          context.getString(R.string.keycombo_shortcut_navigate_previous_column_bounds))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_UP,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_row))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_DOWN,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_row))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_UP,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_row_bounds))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_row_bounds))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_DPAD_LEFT,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_table_bounds))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_MOVE_END,
            /* triggerModifierUsed= */ false);
      }

      if (key.equals(
          context.getString(R.string.keycombo_shortcut_navigate_previous_table_bounds))) {
        return new KeyCombo(
            KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_MOVE_HOME,
            /* triggerModifierUsed= */ false);
      }

      if (FeatureFlagReader.enableAnnounceTableHeader(context)) {
        if (key.equals(
            context.getString(R.string.keycombo_shortcut_other_announce_column_header))) {
          return new KeyCombo(
              KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
              NO_PREFIX_KEY_CODE,
              KeyEvent.KEYCODE_C,
              /* triggerModifierUsed= */ false);
        }
        if (key.equals(context.getString(R.string.keycombo_shortcut_other_announce_row_header))) {
          return new KeyCombo(
              KeyEvent.META_ALT_ON | KeyEvent.META_CTRL_ON,
              NO_PREFIX_KEY_CODE,
              KeyEvent.KEYCODE_R,
              /* triggerModifierUsed= */ false);
        }
      }

      if (FeatureFlagReader.enableSequenceKeyInfra(context)
          && FeatureFlagReader.enableReadCurrentUrl(context)
          && key.equals(context.getString(R.string.keycombo_shortcut_other_read_current_url))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            KeyEvent.KEYCODE_R,
            KeyEvent.KEYCODE_U,
            /* triggerModifierUsed= */ true);
      }
    }
    // End of the Browse Mode only commands.
    ////////////////////////////////////////////////////////////////////////////////////////////////

    if (FeatureFlagReader.enableSpeechVolumeKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_global_speech_volume_increase))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_VOLUME_UP,
            /* triggerModifierUsed= */ true);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_global_speech_volume_decrease))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableSpeechPitchKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_global_speech_pitch_increase))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_VOLUME_UP,
            /* triggerModifierUsed= */ true);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_global_speech_pitch_decrease))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableSpeechRateKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_global_speech_rate_increase))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_VOLUME_UP,
            /* triggerModifierUsed= */ true);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_global_speech_rate_decrease))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableResetSpeechSettingsKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_global_reset_speech_settings))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_BACKSLASH,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableToggleVoiceFeedback(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_global_toggle_voice_feedback))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_VOLUME_MUTE,
            /* triggerModifierUsed= */ true);
      }
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_other_copy_last_spoken_phrase))) {
      return new KeyCombo(
          KeyEvent.META_CTRL_ON,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_C,
          /* triggerModifierUsed= */ true);
    }

    if (key.equals(context.getString(R.string.keycombo_shortcut_global_hide_or_show_screen))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_BRIGHTNESS_UP,
          /* triggerModifierUsed= */ true);
    }

    if (FeatureFlagReader.enableShowTutorialKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_show_tutorial))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_0,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableToggleBrailleOnScreenOverlay(context)) {
      if (key.equals(
          context.getString(R.string.keycombo_shortcut_other_toggle_braille_on_screen_overlay))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_O,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableToggleBrailleContractedMode(context)) {
      if (key.equals(
          context.getString(R.string.keycombo_shortcut_other_toggle_braille_contracted_mode))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON | KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_G,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableToggleSelection(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_toggle_selection))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_S,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableToggleSoundFeedback(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_global_toggle_sound_feedback))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_E,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableCyclePunctuationVerbosityKeyboard(context)) {
      if (key.equals(
          context.getString(R.string.keycombo_shortcut_global_cycle_punctuation_verbosity))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_P,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableShowKeyboardShortcutsDialog(context)) {
      if (key.equals(
          context.getString(R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_SLASH,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableShowLearnModePageKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_global_show_learn_mode_page))) {
        return new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_0,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableShowVariousListOnScreenSearch(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_show_landmark_list))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_D,
            /* triggerModifierUsed= */ true);
      }
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_show_link_list))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_K,
            /* triggerModifierUsed= */ true);
      }
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_show_control_list))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_F,
            /* triggerModifierUsed= */ true);
      }
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_show_table_list))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_T,
            /* triggerModifierUsed= */ true);
      }

      if (key.equals(context.getString(R.string.keycombo_shortcut_other_show_heading_list))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_H,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableSequenceKeyInfra(context)) {
      if (FeatureFlagReader.enableAnnounceBatteryState(context)) {
        if (key.equals(
            context.getString(R.string.keycombo_shortcut_global_announce_battery_state))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_B,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableAnnounceCurrentTimeAndDate(context)) {
        if (key.equals(
            context.getString(R.string.keycombo_shortcut_global_announce_current_time_and_date))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_D,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableAnnounceCurrentTitle(context)) {
        if (key.equals(
            context.getString(R.string.keycombo_shortcut_global_announce_current_title))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_T,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableAnnouncePhoneticPronunciation(context)) {
        if (key.equals(
            context.getString(R.string.keycombo_shortcut_global_announce_phonetic_pronunciation))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_C,
              /* triggerModifierUsed= */ true);
        }
      }

      if (FeatureFlagReader.enableAnnounceRichTextDescription(context)) {
        if (key.equals(
            context.getString(R.string.keycombo_shortcut_global_announce_rich_text_description))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_F,
              /* triggerModifierUsed= */ true);
        }
      }

      if (isScreenOverviewFeatureEnabled()) {
        if (key.equals(context.getString(R.string.keycombo_shortcut_global_screen_overview))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_S,
              /* triggerModifierUsed= */ true);
        }
      }
      if (isDescribeImageFeatureEnabled()) {
        if (key.equals(context.getString(R.string.keycombo_shortcut_global_describe_image))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_I,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableRefocusCurrentNodeKeyboard(context)) {
        if (key.equals(context.getString(R.string.keycombo_shortcut_global_refocus_current_node))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_R,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableReadLinkUrl(context)) {
        if (key.equals(context.getString(R.string.keycombo_shortcut_other_read_link_url))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_R,
              KeyEvent.KEYCODE_L,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableOpenTalkbackSettingsKeyboard(context)) {
        if (key.equals(
            context.getString(R.string.keycombo_shortcut_other_open_talkback_settings))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_O,
              KeyEvent.KEYCODE_S,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableOpenTtsSettingsKeyboard(context)) {
        if (key.equals(context.getString(R.string.keycombo_shortcut_other_open_tts_settings))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_O,
              KeyEvent.KEYCODE_T,
              /* triggerModifierUsed= */ true);
        }
      }
      if (FeatureFlagReader.enableReportIssueKeyboard(context)) {
        if (key.equals(context.getString(R.string.keycombo_shortcut_other_report_issue))) {
          return new KeyCombo(
              KeyComboModel.NO_MODIFIER,
              KeyEvent.KEYCODE_O,
              KeyEvent.KEYCODE_I,
              /* triggerModifierUsed= */ true);
        }
      }
    }

    if (FeatureFlagReader.enableToggleKeyComboPassThroughOnce(context)) {
      if (key.equals(
          context.getString(
              R.string.keycombo_shortcut_global_toggle_key_combo_pass_through_once))) {
        return new KeyCombo(
            KeyEvent.META_SHIFT_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_ESCAPE,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableCycleTypingEchoKeyboard(context)) {
      if (key.equals(context.getString(R.string.keycombo_shortcut_other_cycle_typing_echo))) {
        return new KeyCombo(
            KeyEvent.META_CTRL_ON,
            NO_PREFIX_KEY_CODE,
            KeyEvent.KEYCODE_2,
            /* triggerModifierUsed= */ true);
      }
    }

    if (FeatureFlagReader.enableOpenVoiceCommands(context)
        && TalkbackFeatureSupport.supportSpeechRecognize()
        && key.equals(context.getString(R.string.keycombo_shortcut_other_open_voice_commands))) {
      return new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          NO_PREFIX_KEY_CODE,
          KeyEvent.KEYCODE_BACKSLASH,
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

    if (FeatureFlagReader.enableSequenceKeyInfra(context)) {
      // Check whether the key combo contains an eligible prefix key if it is a sequence key combo.
      int prefixKeyCode = keyCombo.getPrefixKeyCode();
      if (prefixKeyCode != NO_PREFIX_KEY_CODE
          && (!KeyCombo.SEQUENCE_KEY_COMBO_CODE_PREFIX_KEYS.contains(prefixKeyCode)
              || keyCombo.getModifiers() != NO_MODIFIER
              || !keyCombo.isTriggerModifierUsed())) {
        return false;
      }
    }

    int keyCode = keyCombo.getKeyCode();
    // Do not allow a key combo which consists of only modifiers.
    if (KeyEvent.isModifierKey(keyCode) || keyCode == KeyEvent.KEYCODE_UNKNOWN) {
      return false;
    }

    // It's not allowed to include the trigger modifier as part of KeyCombo's modifiers.
    if ((keyCombo.getModifiers() & getTriggerModifier()) != 0) {
      return false;
    }

    // In the new keymap, a key combo must contain the trigger modifier.
    return keyCombo.isTriggerModifierUsed();
  }

  @Override
  public String getDescriptionOfEligibleKeyCombo() {
    return context.getString(
        R.string.keycombo_assign_dialog_default_keymap_instruction, getTriggerModifierName());
  }

  @Override
  public void updateVersion(int previousVersion) {
    // No action needed for the new keymap as it's added after version 50200001.
  }

  private String getTriggerModifierName() {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String triggerModifier =
        prefs.getString(getPreferenceKeyForTriggerModifier(), getDefaultTriggerModifier());
    String triggerModifierName = "";

    if (triggerModifier.equals(context.getString(R.string.trigger_modifier_meta_entry_value))) {
      triggerModifierName = context.getString(R.string.keycombo_key_modifier_meta);
    }

    return triggerModifierName;
  }

  private String getDefaultTriggerModifier() {
    return context.getString(R.string.trigger_modifier_meta_entry_value);
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
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_default));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_default));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_up));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_down));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_first));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_last));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_window));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_window));

    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_character));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_character));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_word));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_word));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_line));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_line));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_button));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_button));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_control));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_control));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_checkbox));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_radio));
    addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_radio));
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

    if (FeatureFlagReader.enableBrowseMode(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_toggle_browse_mode));
    }

    if (FeatureFlagReader.enableAnnounceCurrentTimeAndDate(context)) {
      addKeyCombo(
          context.getString(R.string.keycombo_shortcut_global_announce_current_time_and_date));
    }
    if (FeatureFlagReader.enableAnnounceBatteryState(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_announce_battery_state));
    }
    if (FeatureFlagReader.enableAnnounceCurrentTitle(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_announce_current_title));
    }

    if (FeatureFlagReader.enableAnnouncePhoneticPronunciation(context)) {
      addKeyCombo(
          context.getString(R.string.keycombo_shortcut_global_announce_phonetic_pronunciation));
    }

    if (FeatureFlagReader.enableAnnounceRichTextDescription(context)) {
      addKeyCombo(
          context.getString(R.string.keycombo_shortcut_global_announce_rich_text_description));
    }

    if (FeatureFlagReader.enableAnnounceTableHeader(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_announce_column_header));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_announce_row_header));
    }
    if (FeatureFlagReader.enableContainerNavigationKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_next_container));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_navigate_previous_container));
    }
    if (isScreenOverviewFeatureEnabled()) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_screen_overview));
    }
    if (isDescribeImageFeatureEnabled()) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_describe_image));
    }
    if (FeatureFlagReader.enableSpeechRateKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_rate_increase));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_rate_decrease));
    }
    if (FeatureFlagReader.enableSpeechVolumeKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_volume_increase));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_volume_decrease));
    }
    if (FeatureFlagReader.enableSpeechPitchKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_pitch_increase));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_speech_pitch_decrease));
    }
    if (FeatureFlagReader.enableToggleSelection(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_toggle_selection));
    }
    if (FeatureFlagReader.enableToggleSoundFeedback(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_toggle_sound_feedback));
    }
    if (FeatureFlagReader.enableOpenTtsSettingsKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_open_tts_settings));
    }
    if (FeatureFlagReader.enableResetSpeechSettingsKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_reset_speech_settings));
    }
    if (FeatureFlagReader.enableOpenTalkbackSettingsKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_open_talkback_settings));
    }
    if (FeatureFlagReader.enableShowKeyboardShortcutsDialog(context)) {
      addKeyCombo(
          context.getString(R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog));
    }
    if (FeatureFlagReader.enableShowTutorialKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_show_tutorial));
    }
    if (FeatureFlagReader.enableShowLearnModePageKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_show_learn_mode_page));
    }
    if (FeatureFlagReader.enableToggleVoiceFeedback(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_toggle_voice_feedback));
    }
    if (FeatureFlagReader.enableToggleBrailleContractedMode(context)) {
      addKeyCombo(
          context.getString(R.string.keycombo_shortcut_other_toggle_braille_contracted_mode));
    }
    if (FeatureFlagReader.enableToggleBrailleOnScreenOverlay(context)) {
      addKeyCombo(
          context.getString(R.string.keycombo_shortcut_other_toggle_braille_on_screen_overlay));
    }
    if (FeatureFlagReader.enableReportIssueKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_report_issue));
    }
    if (FeatureFlagReader.enableCyclePunctuationVerbosityKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_cycle_punctuation_verbosity));
    }
    if (FeatureFlagReader.enableShowVariousListOnScreenSearch(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_show_table_list));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_show_heading_list));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_show_landmark_list));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_show_link_list));
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_show_control_list));
    }
    if (FeatureFlagReader.enableReadCurrentUrl(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_read_current_url));
    }
    if (FeatureFlagReader.enableReadLinkUrl(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_read_link_url));
    }
    if (FeatureFlagReader.enableToggleKeyComboPassThroughOnce(context)) {
      addKeyCombo(
          context.getString(R.string.keycombo_shortcut_global_toggle_key_combo_pass_through_once));
    }
    if (FeatureFlagReader.enableDoubleClickKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_perform_double_click));
    }
    if (FeatureFlagReader.enableCycleTypingEchoKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_cycle_typing_echo));
    }
    if (FeatureFlagReader.enableRefocusCurrentNodeKeyboard(context)) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_global_refocus_current_node));
    }
    if (FeatureFlagReader.enableOpenVoiceCommands(context)
        && TalkbackFeatureSupport.supportSpeechRecognize()) {
      addKeyCombo(context.getString(R.string.keycombo_shortcut_other_open_voice_commands));
    }
  }

  private void addKeyCombo(String key) {
    if (!persister.contains(key)) {
      KeyCombo defaultKeyCombo = getDefaultKeyCombo(key);
      persister.saveKeyCombo(key, defaultKeyCombo);
    }

    KeyCombo keyCombo = persister.getKeyCombo(key);
    keyComboMap.put(key, keyCombo);
  }

  private boolean isScreenOverviewFeatureEnabled() {
    return GeminiConfiguration.screenOverviewEnabled(context)
        && FeatureFlagReader.enableScreenOverview(context)
        && FeatureSupport.supportTakeScreenshotByWindow();
  }

  private boolean isDescribeImageFeatureEnabled() {
    return GeminiConfiguration.isServerSideGeminiImageCaptioningEnabled(context)
        && FeatureFlagReader.enableDescribeImage(context)
        && FeatureSupport.supportTakeScreenshotByWindow();
  }
}
