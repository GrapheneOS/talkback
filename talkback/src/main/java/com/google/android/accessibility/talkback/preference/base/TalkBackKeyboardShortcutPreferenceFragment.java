/*
 * Copyright 2015 Google Inc.
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

package com.google.android.accessibility.talkback.preference.base;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.TalkBackService.TalkbackServiceStateNotifier.TalkBackServiceStateChangeListener;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.keyboard.KeyCombo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.preference.TalkBackPreferenceFilter;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Panel holding a set of keyboard shortcut preferences. */
public class TalkBackKeyboardShortcutPreferenceFragment extends TalkbackBaseFragment {
  private static final String TAG = "TalkBackKeyboardShortcutPreferenceFragment";
  // android.content.res.Resources.ID_NULL was introduced in API 29, but the min SDK version of
  // TalkBack is API 26. For compatibility, define a constant here instead of using ID_NULL.
  private static final int RESOURCE_ID_NULL = 0;

  private String keymap;
  private SharedPreferences prefs;
  private @Nullable String triggerModifierToBeSet;
  private PreferenceScreen resetKeymapPreference;

  public TalkBackKeyboardShortcutPreferenceFragment() {
    super(R.xml.default_key_combo_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_manage_keyboard_shortcuts);
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    TalkBackService.TalkbackServiceStateNotifier.getInstance()
        .registerTalkBackServiceStateChangeListener(serviceStateChangeListener);
  }

  /** Updates fragment whenever their values change. */
  private final OnSharedPreferenceChangeListener sharedPreferenceChangeListener =
      (prefs, key) -> {
        if (TextUtils.equals(key, getString(R.string.pref_select_keymap_key))) {
          // Refreshes key combo model after keymap changes.
          KeyComboManager keyComboManager = getKeyComboManager();
          keyComboManager.refreshKeyComboModel();
          keymap = getKeymap();
          updateFragment();
        } else if (TextUtils.equals(
            key, getString(R.string.pref_default_keymap_trigger_modifier_key))) {
          updateFragment();
        }
      };

  private final DialogInterface.OnClickListener chooseTriggerModifierConfirmDialogPositive =
      (dialogInterface, i) -> {
        resetKeymap();

        KeyComboModel keyComboModel = getKeyComboManager().getKeyComboModel();

        // Update preference.
        String preferenceKeyForTriggerModifier = keyComboModel.getPreferenceKeyForTriggerModifier();
        ListPreference listPreference =
            (ListPreference) findPreference(preferenceKeyForTriggerModifier);
        listPreference.setValue(triggerModifierToBeSet);

        // Update KeyComboModel.
        keyComboModel.notifyTriggerModifierChanged();

        // Update UI.
        setUpDialogPreference(KeyboardShortcutDialogPreference::onTriggerModifierChanged);

        // Announce that trigger modifier has changed.
        CharSequence[] entries = listPreference.getEntries();
        CharSequence newTriggerModifier =
            entries[listPreference.findIndexOfValue(triggerModifierToBeSet)];
        PreferencesActivityUtils.announceText(
            getString(R.string.keycombo_menu_announce_new_trigger_modifier, newTriggerModifier),
            getActivity());

        triggerModifierToBeSet = null;
      };

  private final DialogInterface.OnClickListener resetKeymapConfirmDialogPositive =
      (dialogInterface, i) -> {
        resetKeymap();
        dialogInterface.dismiss();

        PreferencesActivityUtils.announceText(
            getString(R.string.keycombo_menu_announce_reset_keymap), getActivity());
      };

  private final Preference.OnPreferenceClickListener resetKeymapPreferenceClickListener =
      (preference) -> {
        // Show confirm dialog.
        A11yAlertDialogWrapper.materialDialogBuilder(getActivity())
            .setTitle(getString(R.string.keycombo_menu_reset_keymap))
            .setMessage(getString(R.string.message_in_reset_keymap_confirm_dialog))
            .setPositiveButton(
                R.string.reset_button_in_reset_keymap_confirm_dialog,
                resetKeymapConfirmDialogPositive)
            .setNegativeButton(
                android.R.string.cancel,
                (DialogInterface dialogInterface, int i) -> dialogInterface.cancel())
            .create()
            .show();
        return true;
      };

  private final Preference.OnPreferenceClickListener
      viewSystemKeyboardShortcutsPreferenceClickListener =
          (preference) -> {
            if (getActivity() != null) {
              getActivity().requestShowKeyboardShortcuts();
            } else {
              LogUtils.w(TAG, "Could not resolve activity to call requestShowKeyboardShortcuts");
            }
            return true;
          };

  private final OnPreferenceChangeListener preferenceChangeListener =
      (preference, newValue) -> {
        String preferenceKeyForTriggerModifier =
            getKeyComboManager().getKeyComboModel().getPreferenceKeyForTriggerModifier();
        if (preference instanceof KeyboardShortcutDialogPreference keyboardPreference
            && newValue instanceof KeyCombo customizedKeyCombo) {
          keyboardPreference.setKeyCombo(customizedKeyCombo);
          keyboardPreference.notifyChanged();
        } else if (preference.getKey() != null
            && preference.getKey().equals(getString(R.string.pref_select_keymap_key))
            && newValue instanceof String chosenKeymap) {
          // Do nothing if keymap is the same.
          if (keymap.equals(chosenKeymap)) {
            return false;
          }

          // Announce new keymap.
          PreferencesActivityUtils.announceText(
              String.format(
                  getString(R.string.keycombo_menu_announce_active_keymap),
                  getKeymapName(chosenKeymap)),
              getActivity());
        } else if (preference.getKey() != null
            && preference.getKey().equals(preferenceKeyForTriggerModifier)
            && newValue instanceof String chosenTriggerModifier) {
          triggerModifierToBeSet = chosenTriggerModifier;

          ListPreference listPreference = (ListPreference) preference;
          if (listPreference.getValue().equals(triggerModifierToBeSet)) {
            return false;
          }

          CharSequence[] entries = listPreference.getEntries();
          CharSequence newTriggerModifier =
              entries[listPreference.findIndexOfValue(triggerModifierToBeSet)];
          CharSequence currentTriggerModifier =
              entries[listPreference.findIndexOfValue(listPreference.getValue())];

          // Show alert dialog.
          A11yAlertDialogWrapper.materialDialogBuilder(getContext())
              .setTitle(
                  getString(
                      R.string.keycombo_menu_alert_title_trigger_modifier, newTriggerModifier))
              .setMessage(
                  getString(
                      R.string.keycombo_menu_alert_message_trigger_modifier,
                      currentTriggerModifier))
              .setPositiveButton(
                  getString(
                      R.string.keycombo_menu_alert_button_trigger_modifier, newTriggerModifier),
                  chooseTriggerModifierConfirmDialogPositive)
              .setNegativeButton(
                  android.R.string.cancel,
                  (DialogInterface dialogInterface, int i) -> triggerModifierToBeSet = null)
              .create()
              .show();
          return false;
        }
        return true;
      };

  private void resetKeymap() {
    KeyComboModel keyComboModel = getKeyComboManager().getKeyComboModel();
    for (String key : keyComboModel.getKeyComboMap().keySet()) {
      KeyCombo defaultKeyCombo = keyComboModel.getDefaultKeyCombo(key);
      // Do nothing if key combo code is not changed from default one.
      if (defaultKeyCombo.equals(keyComboModel.getKeyComboForKey(key))) {
        continue;
      }
      // Save with default key combo.
      keyComboModel.saveKeyCombo(key, defaultKeyCombo);

      // Update UI.
      KeyboardShortcutDialogPreference keyboardShortcutDialogPreference =
          (KeyboardShortcutDialogPreference) findPreference(key);
      keyboardShortcutDialogPreference.setKeyCombo(defaultKeyCombo);
      keyboardShortcutDialogPreference.notifyChanged();
    }
  }

  private KeyComboManager getKeyComboManager() {
    TalkBackService talkBackService = TalkBackService.getInstance();

    return talkBackService == null
        ? KeyComboManager.create(getActivity())
        : talkBackService.getKeyComboManager();
  }

  private @Nullable String getKeymapName(String keymap) {
    if (keymap.equals(getString(R.string.default_keymap_entry_value))) {
      return getString(R.string.value_default_keymap);
    } else if (keymap.equals(getString(R.string.new_keymap_entry_value))) {
      return getString(R.string.value_new_keymap);
    }
    return null;
  }

  private String getKeymap() {
    KeyComboManager keyComboManager = getKeyComboManager();
    return keyComboManager.getKeymap();
  }

  private int getPreferenceResourceId() {
    if (TextUtils.equals(keymap, getContext().getString(R.string.default_keymap_entry_value))) {
      return R.xml.default_key_combo_preferences;
    } else if (TextUtils.equals(keymap, getContext().getString(R.string.new_keymap_entry_value))) {
      return R.xml.new_key_combo_preferences;
    }
    return RESOURCE_ID_NULL;
  }

  private void maybeUpdateKeyComboModel() {
    KeyComboManager keyComboManager = getKeyComboManager();
    if (keyComboManager.getKeyComboModel() == null) {
      // After the classic keymap is removed, key combo model can be null here for users who used
      // the classic keymap. In this case, switch to the default keymap and show the notification
      // dialog if needed.
      keyComboManager.maybeSwitchKeymapAndShowNewKeymapNotificationDialog();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    prefs.unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
    TalkBackService.TalkbackServiceStateNotifier.getInstance()
        .unregisterTalkBackServiceStateChangeListener(serviceStateChangeListener);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    // Call `maybeUpdateKeyComboModel()` before `super.onCreatePreferences()` because the key combo
    // model will be null for classic keymap users. If it's null, it will switch to the default key
    // combo model (i.e. default keymap). Otherwise, `super.onCreatePreferences()` will crash.
    maybeUpdateKeyComboModel();
    super.onCreatePreferences(savedInstanceState, rootKey);

    keymap = getKeymap();
    int preferenceResourceId = getPreferenceResourceId();
    if (preferenceResourceId == RESOURCE_ID_NULL) {
      LogUtils.w(TAG, "Preference resource id is null, can't create preferences.");
      return;
    }
    setPreferencesFromResource(preferenceResourceId, rootKey);

    prefs = SharedPreferencesUtils.getSharedPreferences(getContext());
    prefs.registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);

    updatePreference();
  }

  /** Updates Fragment when KeyComboModel changes or resets keymap. */
  private void updateFragment() {
    PreferenceSettingsUtils.setPreferencesFromResource(this, getPreferenceResourceId(), null);
    updatePreference();
  }

  /** Updates preference, including UI, by KeyComboModel. */
  private void updatePreference() {
    // By default, the preference filter is applied in TalkbackBaseFragment which is only performed
    // 1st time the fragment created. Keyboard shortcut preference group can change between default
    // key map and new key map, so need to redo the filter when the preference changed.
    TalkBackPreferenceFilter talkBackPreferenceFilter =
        new TalkBackPreferenceFilter(getActivity().getApplicationContext());
    talkBackPreferenceFilter.filterPreferences(getPreferenceScreen());

    updateSelectKeymapPreferenceWithAvailableKeymaps();

    resetKeymapPreference = findPreference(getString(R.string.pref_reset_keymap_key));
    resetKeymapPreference.setOnPreferenceClickListener(resetKeymapPreferenceClickListener);
    Preference viewSystemKeyboardShortcutsPreference =
        findPreference(getString(R.string.pref_view_system_keyboard_shortcuts_key));
    if (viewSystemKeyboardShortcutsPreference != null) {
      viewSystemKeyboardShortcutsPreference.setOnPreferenceClickListener(
          viewSystemKeyboardShortcutsPreferenceClickListener);
    }

    updateDialogAndResetKeymapPreference();

    initPreferenceUIs(getPreferenceScreen());
    if (FeatureFlagReader.enableNewKeymap(getContext())
        && keymap.equals(getString(R.string.new_keymap_entry_value))) {
      updateDialogPreferencesTitle();
    }
  }

  private void updateDialogPreferencesTitle() {
    Set<String> keySet = getKeyComboManager().getKeyComboModel().getKeyComboMap().keySet();
    String browseModeCommandTitleTemplate =
        getString(R.string.template_keycombo_menu_browse_mode_command);
    for (String key : keySet) {
      KeyboardShortcutDialogPreference preference = findPreference(key);
      if (preference != null
          && preference.getTitle().toString().equals(browseModeCommandTitleTemplate)) {
        TalkBackPhysicalKeyboardShortcut action =
            TalkBackPhysicalKeyboardShortcut.getActionFromKey(getContext().getResources(), key);
        if (action == null || action.getKeyStrRes() == -1) {
          continue;
        }
        String keyCommandDescription = action.getDescription(getContext().getResources());
        preference.setTitle(String.format(browseModeCommandTitleTemplate, keyCommandDescription));
      }
    }
  }

  /** Updates select keymap preference with available keymaps. */
  private void updateSelectKeymapPreferenceWithAvailableKeymaps() {
    ListPreference keymapPreference = findPreference(getString(R.string.pref_select_keymap_key));
    List<CharSequence> entries = new ArrayList<>();
    List<CharSequence> entryValues = new ArrayList<>();
    entries.add(getString(R.string.value_default_keymap));
    entryValues.add(getString(R.string.default_keymap_entry_value));
    if (FeatureFlagReader.enableNewKeymap(getContext())) {
      entries.add(getString(R.string.value_new_keymap));
      entryValues.add(getString(R.string.new_keymap_entry_value));
    }
    keymapPreference.setEntries(entries.toArray(new CharSequence[entries.size()]));
    keymapPreference.setEntryValues(entryValues.toArray(new CharSequence[entryValues.size()]));
  }

  /**
   * Initialize preference UIs.
   *
   * @param root Root element of preference UIs.
   */
  private void initPreferenceUIs(PreferenceGroup root) {
    if (root == null) {
      return;
    }

    final KeyComboModel keyComboModel = getKeyComboManager().getKeyComboModel();
    final String preferenceKeyForTriggerModifier =
        keyComboModel.getPreferenceKeyForTriggerModifier();
    final Map<String, KeyCombo> keyComboMap = keyComboModel.getKeyComboMap();
    for (int i = root.getPreferenceCount() - 1; i >= 0; i--) {
      final Preference preference = root.getPreference(i);
      final String key = preference.getKey();
      if (key != null && preference instanceof KeyboardShortcutDialogPreference) {
        if (!keyComboMap.containsKey(key)) {
          // Hide preference of unavailable key combo on this device.
          root.removePreference(preference);
          if (root.getPreferenceCount() == 0) {
            getPreferenceScreen().removePreference(root);
          }
        } else if (key.equals(getString(R.string.keycombo_shortcut_other_toggle_browse_mode))) {
          // Disable the preference to make it not customizable.
          KeyboardShortcutDialogPreference keyShortcutPreference =
              (KeyboardShortcutDialogPreference) preference;
          keyShortcutPreference.setEnabled(false);
          keyShortcutPreference.setSummary(
              getString(
                  R.string.shortcut_not_customizable,
                  keyShortcutPreference.getSummary().toString()));
        } else {
          // Set onPreferenceChangeListener.
          preference.setOnPreferenceChangeListener(preferenceChangeListener);
        }
      } else if ((key != null && key.equals(getString(R.string.pref_select_keymap_key)))
          || (key != null && key.equals(preferenceKeyForTriggerModifier))) {
        // Set onPreferenceChangeListener.
        preference.setOnPreferenceChangeListener(preferenceChangeListener);
      } else if (preference instanceof PreferenceGroup) {
        initPreferenceUIs((PreferenceGroup) preference);
      }
    }
  }

  private void setUpDialogPreference(Consumer<KeyboardShortcutDialogPreference> consumer) {
    final Set<String> keySet = getKeyComboManager().getKeyComboModel().getKeyComboMap().keySet();
    for (String key : keySet) {
      KeyboardShortcutDialogPreference preference = findPreference(key);
      if (preference != null) {
        consumer.accept(preference);
      }
    }
  }

  private void updateDialogAndResetKeymapPreference() {
    resetKeymapPreference.setEnabled(isServiceActive());
    setUpDialogPreference(
        preference -> {
          preference.setEnabled(isServiceActive());
        });
  }

  private boolean isServiceActive() {
    return TalkBackService.isServiceActive();
  }

  /** Enabled or disabled the dialog preference if the state of TalkbackService is changed. */
  TalkBackService.TalkbackServiceStateNotifier.TalkBackServiceStateChangeListener
      serviceStateChangeListener =
          new TalkBackServiceStateChangeListener() {
            @Override
            public void onServiceStateChange(boolean isServiceActive) {
              resetKeymapPreference.setEnabled(isServiceActive);
              setUpDialogPreference(
                  preference -> {
                    preference.setEnabled(isServiceActive);
                    if (isServiceActive) {
                      preference.updateKeyComboManager();
                      preference.onTriggerModifierChanged();
                    }
                  });
            }
          };
}
