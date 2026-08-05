/*
 * Copyright 2010 Google Inc.
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

import static com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_ARGS;
import static com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_NAME;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDialogFragmentCompat;
import androidx.preference.PreferenceGroup;
import com.android.talkback.TalkBackPreferencesActivity.TalkBackSubSettings;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.talkback.training.TutorialInitiator;
import com.google.android.accessibility.utils.Consumer;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Panel holding a set of shortcut preferences. Recreated when preset value changes. */
public class TalkBackGestureShortcutPreferenceFragment extends TalkbackBaseFragment {
  private static final String TAG = "TalkBackGestureShortcutPreferenceFragment";

  /** Preferences managed by this activity. */
  private SharedPreferences prefs;

  private boolean isVibrationWatchEnabled = false;

  private boolean shouldUpdatePreferenceOnResume = false;

  public TalkBackGestureShortcutPreferenceFragment() {
    super(R.xml.gesture_preferences);
  }

  private final DialogInterface.OnClickListener resetGestureConfirmDialogPositive =
      (dialogInterface, i) -> {
        PreferencesActivityUtils.announceText(
            getString(R.string.gestures_announce_reset_gesture_settings), getContext());
        // Reset logic here
        resetGestureShortcut();
        dialogInterface.dismiss();
      };

  private final Preference.OnPreferenceClickListener resetGesturePreferenceClickListener =
      (preference) -> {
        boolean gestureSetEnabled =
            GestureShortcutMapping.isGestureSetEnabled(
                getContext(),
                prefs,
                R.string.pref_multiple_gesture_set_key,
                R.bool.pref_multiple_gesture_set_default);

        // Show confirm dialog.
        A11yAlertDialogWrapper.materialDialogBuilder(
                getContext(), getActivity().getSupportFragmentManager())
            .setTitle(
                getString(
                    gestureSetEnabled
                        ? R.string.Reset_gesture_settings_set_dialog_title
                        : R.string.Reset_gesture_settings_dialog_title))
            .setMessage(getString(R.string.message_reset_gesture_settings_confirm_dialog))
            .setPositiveButton(
                R.string.reset_button_in_reset_gesture_settings_confirm_dialog,
                resetGestureConfirmDialogPositive)
            .setNegativeButton(android.R.string.cancel, (dialog, i) -> dialog.cancel())
            .create()
            .show();

        return true;
      };

  @Override
  public void onDisplayPreferenceDialog(Preference preference) {
    if (preference instanceof GestureListPreference) {
      // In createDialogFragment, we create PreferenceDialogFragmentCompat for Handset & Auto and
      // null for Wear.
      Fragment fragment = ((GestureListPreference) preference).createDialogFragment();
      if (fragment instanceof PreferenceDialogFragmentCompat) {
        PreferenceDialogFragmentCompat dialogFragment = (PreferenceDialogFragmentCompat) fragment;
        dialogFragment.setTargetFragment(this, 0);
        dialogFragment.show(getParentFragmentManager(), preference.getKey());
      } else {
        shouldUpdatePreferenceOnResume = true;
        // For wear and phone devices, instead of dialog, we use a new fragment to show the
        // available action items.
        Intent intent = new Intent(getContext(), TalkBackSubSettings.class);
        intent.putExtra(FRAGMENT_NAME, GesturePreferenceFragmentCompat.class.getCanonicalName());
        Bundle bundle =
            GesturePreferenceFragmentCompat.createBundleForFragmentArguments(
                (GestureListPreference) preference);
        intent.putExtra(FRAGMENT_ARGS, bundle);
        startActivity(intent);
      }
    } else {
      super.onDisplayPreferenceDialog(preference);
    }
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_category_manage_gestures);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    Context context = getContext();

    prefs = SharedPreferencesUtils.getSharedPreferences(context);

    // Launches Practice gestures page.
    @Nullable
    Preference practiceGesturesPreference =
        findPreference(getString(R.string.pref_practice_gestures_entry_point_key));
    if (practiceGesturesPreference != null) {
      practiceGesturesPreference.setIntent(TutorialInitiator.createPracticeGesturesIntent(context));
    }

    boolean gestureSetEnabled =
        GestureShortcutMapping.isGestureSetEnabled(
            context,
            prefs,
            R.string.pref_multiple_gesture_set_key,
            R.bool.pref_multiple_gesture_set_default);

    @Nullable
    Preference gestureSetPreference = findPreference(getString(R.string.pref_gesture_set_key));
    if (gestureSetEnabled) {
      if (gestureSetPreference != null) {
        int gestureSet =
            SharedPreferencesUtils.getIntFromStringPref(
                prefs,
                getResources(),
                R.string.pref_gesture_set_key,
                R.string.pref_gesture_set_value_default);
        updatePreferenceKey(gestureSet);
      }
    } else if (gestureSetPreference != null) {
      getPreferenceScreen().removePreference(gestureSetPreference);
    }

    Preference resetGesturePreferenceScreen =
        (Preference) findPreference(getString(R.string.pref_reset_gesture_settings_key));
    resetGesturePreferenceScreen.setOnPreferenceClickListener(resetGesturePreferenceClickListener);

    // Disable 2-finger swipe gestures which are reserved as 2-finger pass-through.
    if (FeatureSupport.isMultiFingerGestureSupported()) {
      String[] twoFingerPassThroughGestureSet =
          context.getResources().getStringArray(R.array.pref_2finger_pass_through_shortcut_keys);
      String[] summaries =
          context.getResources().getStringArray(R.array.pref_2finger_pass_through_shortcut_summary);
      if (twoFingerPassThroughGestureSet.length == summaries.length) {
        for (int i = 0; i < twoFingerPassThroughGestureSet.length; i++) {
          GestureListPreference twoFingerGesturePreference =
              findPreference(twoFingerPassThroughGestureSet[i]);
          if (twoFingerGesturePreference != null) {
            twoFingerGesturePreference.setEnabled(false);
            twoFingerGesturePreference.setSummaryWhenDisabled(
                context.getResources().getString(R.string.shortcut_not_customizable, summaries[i]));
          }
        }
      }
    }
    getCustomizationMap().forEach(this::runPreferencesCustomization);
  }

  private HashMap<List<Integer>, Consumer<Preference>> getCustomizationMap() {
    HashMap<List<Integer>, Consumer<Preference>> customizations = new HashMap<>();
    if (FormFactorUtils.isAndroidWear()) {
      List<Integer> keys = new ArrayList<>();
      keys.add(R.string.pref_shortcut_2finger_1tap_key);
      keys.add(R.string.pref_shortcut_2finger_2tap_key);
      customizations.put(
          keys,
          preference -> {
            GestureListPreference gestureListPreference = (GestureListPreference) preference;
            gestureListPreference.setSummaryWhenDisabled(
                requireContext().getString(R.string.shortcut_disabled_due_to_vibration_watch));
          });
    }
    return customizations;
  }

  private void runPreferencesCustomization(
      List<Integer> keys, Consumer<Preference> preferenceModifier) {
    for (int keyId : keys) {
      Preference preference = findPreferenceByResId(keyId);
      if (preference != null) {
        preferenceModifier.accept(preference);
      }
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    updatePreferencesOnResume();
    updatePreferencesForVibrationWatchIfNeeded();
  }

  private void updatePreferencesOnResume() {
    if (!shouldUpdatePreferenceOnResume) {
      return;
    }
    int gestureSet =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            getResources(),
            R.string.pref_gesture_set_key,
            R.string.pref_gesture_set_value_default);
    updatePreferenceKey(gestureSet);
    shouldUpdatePreferenceOnResume = false;
  }

  private void updatePreferencesForVibrationWatchIfNeeded() {

    if (!FormFactorUtils.isAndroidWear()) {
      return;
    }

    boolean isVibrationWatchEnabled = SettingsUtils.isVibrationWatchEnabled(requireContext());
    if (isVibrationWatchEnabled != this.isVibrationWatchEnabled) {
      this.isVibrationWatchEnabled = isVibrationWatchEnabled;
      List<Integer> keys = new ArrayList<>();
      keys.add(R.string.pref_shortcut_2finger_1tap_key);
      keys.add(R.string.pref_shortcut_2finger_2tap_key);
      runPreferencesCustomization(
          keys, preference -> preference.setEnabled(!isVibrationWatchEnabled));
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  /** Listener for preference changes. */
  private final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
      (prefs, key) -> {
        if (getActivity() == null) {
          LogUtils.w(
              TAG, "Fragment is not attached to activity, do not update verbosity setting page.");
          return;
        }
        if (TextUtils.equals(key, getString(R.string.pref_gesture_set_key))) {
          ListPreference preference =
              (ListPreference) findPreference(getString(R.string.pref_gesture_set_key));

          String newValueString =
              prefs.getString(
                  getString(R.string.pref_gesture_set_key),
                  getString(R.string.pref_gesture_set_value_default));
          if (preference != null) {
            preference.setValue(newValueString);
          }

          updatePreferenceKey(Integer.parseInt(newValueString));
        }
      };

  private void updatePreferenceKeyForGesture(
      AccessibilitySuitePreferenceCategory category, int gestureSet) {
    int count = category.getPreferenceCount();

    for (int i = 0; i < count; i++) {
      Preference preference = category.getPreference(i);
      if (preference instanceof GestureListPreference) {
        GestureListPreference gesture = (GestureListPreference) preference;
        String key =
            GestureShortcutMapping.getPrefKeyWithGestureSet(preference.getKey(), gestureSet);
        gesture.setKey(key);
        gesture.setSummary(
            GestureShortcutMapping.getActionString(
                getContext(), prefs.getString(key, gesture.getDefaultValue())));
      }
    }
  }

  private void updatePreferenceKey(int gestureSet) {
    PreferenceGroup root = getPreferenceScreen();
    int count = root.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      Preference preference = root.getPreference(i);
      if (preference instanceof AccessibilitySuitePreferenceCategory) {
        updatePreferenceKeyForGesture(
            (AccessibilitySuitePreferenceCategory) preference, gestureSet);
      }
    }
  }

  private void resetGestureShortcut() {
    // Reset preference to default
    final SharedPreferences.Editor prefEditor = prefs.edit();
    final String[] gesturePrefKeys = getResources().getStringArray(R.array.pref_shortcut_keys);
    int gestureSet =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            getResources(),
            R.string.pref_gesture_set_key,
            R.string.pref_gesture_set_value_default);

    PreferenceGroup root = getPreferenceScreen();

    for (String defaultKey : gesturePrefKeys) {
      String prefKey = GestureShortcutMapping.getPrefKeyWithGestureSet(defaultKey, gestureSet);
      if (prefs.contains(prefKey)) {
        GestureListPreference preference = (GestureListPreference) root.findPreference(prefKey);
        if (preference != null) {
          // for multi-finger gestures which are not supported before R, the preference may not be
          // found. We don't need to reset the relative preferences.
          preference.updateSummaryToDefaultValue();
        }
        prefEditor.remove(prefKey);
      }
    }
    prefEditor.apply();
  }
}
