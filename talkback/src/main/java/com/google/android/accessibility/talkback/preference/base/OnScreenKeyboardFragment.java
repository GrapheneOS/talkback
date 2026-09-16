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

package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.ListPreference;
import androidx.preference.TwoStatePreference;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.utils.VerbosityPreferences;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Fragment holding a set of onscreen keyboard preferences. */
public class OnScreenKeyboardFragment extends TalkbackBaseFragment {
  private static final String TAG = "OnScreenKeyboardFragment";
  private Context context;
  private SharedPreferences prefs;

  /** Listener for verbosity preset preference changes. */
  private final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
      (prefs, key) -> {
        if (!isAdded() || (getActivity() == null)) {
          return;
        }
        if (TextUtils.equals(key, getString(R.string.pref_verbosity_preset_key))) {
          String verbosityValue =
              SharedPreferencesUtils.getStringPref(
                  prefs,
                  getResources(),
                  R.string.pref_verbosity_preset_key,
                  R.string.pref_verbosity_preset_value_default);
          updateSpeakPhoneticLetters(verbosityValue);
          updateOnScreenKeyboardEchoPref(verbosityValue);
        }
      };

  public OnScreenKeyboardFragment() {
    super(R.xml.on_screen_keyboard_preferences);
  }

  @Override
  protected CharSequence getTitle() {
    return getText(R.string.title_pref_on_screen_keyboard);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    context = getContext();

    if (context == null) {
      return;
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
  }

  @Override
  public void onResume() {
    super.onResume();

    AccessibilitySuitePreference typingMethodPreference =
        (AccessibilitySuitePreference)
            findPreference(getString(R.string.pref_typing_confirmation_key));
    if (typingMethodPreference != null) {
      typingMethodPreference.setSummaryProvider(
          preference -> {
            int method =
                SharedPreferencesUtils.getIntFromStringPref(
                    prefs,
                    context.getResources(),
                    R.string.pref_typing_confirmation_key,
                    R.string.pref_typing_confirmation_default);

            String[] typingValues =
                context.getResources().getStringArray(R.array.pref_typing_types_talkback_values);
            int position = Integer.parseInt(typingValues[typingValues.length - 1]);
            for (int index = 0; index < typingValues.length; index++) {
              if (method == Integer.parseInt(typingValues[index])) {
                position = index;
                break;
              }
            }
            return context.getResources()
                .getStringArray(R.array.pref_typing_types_talkback_entries)[position];
          });
    }

    // Use verbosity prefix to keep settings consistent with Verbosity settings.
    String verbosityValue =
        SharedPreferencesUtils.getStringPref(
            prefs,
            getResources(),
            R.string.pref_verbosity_preset_key,
            R.string.pref_verbosity_preset_value_default);
    updateSpeakPhoneticLetters(verbosityValue);
    updateOnScreenKeyboardEchoPref(verbosityValue);
    prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  private void updateSpeakPhoneticLetters(String verbosityValue) {
    // Use verbosity prefix to keep settings consistent with Verbosity settings.
    String verbosityPrefKey =
        VerbosityPreferences.toVerbosityPrefKey(
            verbosityValue, getString(R.string.pref_phonetic_letters_key));
    TwoStatePreference preference = findPreference(verbosityPrefKey);
    if (preference == null) {
      preference = findPreference(getString(R.string.pref_phonetic_letters_key));
      if (preference == null) {
        return;
      }
      preference.setKey(verbosityPrefKey);
    }

    boolean speakPhoneticPref =
        VerbosityPreferences.getPreferenceVerbosityBool(
            prefs,
            getResources(),
            verbosityValue,
            getResources().getString(R.string.pref_phonetic_letters_key),
            getResources().getBoolean(R.bool.pref_phonetic_letters_default));
    preference.setChecked(speakPhoneticPref);
    // Disable default verbosity preference details if it is high or low.
    preference.setEnabled(!VerbosityPreferences.isVerbosityValueHighOrLow(verbosityValue, context));
  }

  private void updateOnScreenKeyboardEchoPref(String verbosityValue) {
    // Use verbosity prefix to keep settings consistent with Verbosity settings.
    String verbosityPrefKey =
        VerbosityPreferences.toVerbosityPrefKey(
            verbosityValue, getString(R.string.pref_keyboard_echo_on_screen_key));
    ListPreference preference = findPreference(verbosityPrefKey);
    if (preference == null) {
      preference = findPreference(getString(R.string.pref_keyboard_echo_on_screen_key));
      if (preference == null) {
        return;
      }
      preference.setKey(verbosityPrefKey);
    }

    int onScreenKeyboardPref = VerbosityPreferences.readOnScreenKeyboardEcho(prefs, getResources());
    preference.setValue(String.valueOf(onScreenKeyboardPref));
    // Disable default verbosity preference details if it is high or low.
    preference.setEnabled(!VerbosityPreferences.isVerbosityValueHighOrLow(verbosityValue, context));
  }
}
