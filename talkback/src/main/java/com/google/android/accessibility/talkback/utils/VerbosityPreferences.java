/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.google.android.accessibility.talkback.utils;

import static com.google.android.accessibility.talkback.actor.TalkBackUIActor.Type.GESTURE_OR_KEYBOARD_ACTION_OVERLAY;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.TextUtils;
import androidx.annotation.NonNull;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.input.TextEventFilter.KeyboardEchoType;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Utility functions for verbosity preferences. Verbosity preferences should be read through
 * getPreferenceValue() to use preference verbosity rules.
 */
public class VerbosityPreferences {

  private static final String TAG = "VerbosityPreferences";

  private VerbosityPreferences() {} // Do not instantiate.

  public static boolean getPreferenceValueBool(
      SharedPreferences preferences, Resources resources, String key, boolean defaultValue) {

    // If no verbosity selected... use old preference. Otherwise use verbosity rule or custom value.
    String verbosityValue =
        SharedPreferencesUtils.getStringPref(
            preferences, resources, R.string.pref_verbosity_preset_key, 0); // Default to null.
    if (verbosityValue == null) {
      return preferences.getBoolean(key, defaultValue);
    }
    return getPreferenceVerbosityBool(preferences, resources, verbosityValue, key, defaultValue);
  }

  public static String getPreferenceValueString(
      SharedPreferences preferences, Resources resources, String key, String defaultValue) {

    // If no verbosity selected... use old preference. Otherwise use verbosity rule or custom value.
    String verbosityValue =
        SharedPreferencesUtils.getStringPref(
            preferences, resources, R.string.pref_verbosity_preset_key, 0); // Default to null.
    if (verbosityValue == null) {
      return preferences.getString(key, defaultValue);
    }
    return getPreferenceVerbosityString(preferences, resources, verbosityValue, key, defaultValue);
  }

  public static boolean getPreferenceVerbosityBool(
      SharedPreferences preferences,
      Resources resources,
      @NonNull String verbosityValue,
      String key,
      boolean defaultValue) {
    if (verbosityValue.equals(resources.getString(R.string.pref_verbosity_preset_value_high))) {
      return true;
    } else if (verbosityValue.equals(
        resources.getString(R.string.pref_verbosity_preset_value_low))) {
      return false;
    } else {
      String keyForVerbosity = toVerbosityPrefKey(verbosityValue, key);
      return preferences.getBoolean(keyForVerbosity, defaultValue);
    }
  }

  /** Reads echo type of physical keyboard. */
  @KeyboardEchoType
  public static int readPhysicalKeyboardEcho(SharedPreferences prefs, Resources resources) {
    return Integer.parseInt(
        VerbosityPreferences.getPreferenceValueString(
            prefs,
            resources,
            resources.getString(R.string.pref_keyboard_echo_physical_key),
            resources.getString(R.string.pref_keyboard_echo_default)));
  }

  /** Reads echo type of on-screen keyboard. */
  @KeyboardEchoType
  public static int readOnScreenKeyboardEcho(SharedPreferences prefs, Resources resources) {
    return Integer.parseInt(
        VerbosityPreferences.getPreferenceValueString(
            prefs,
            resources,
            resources.getString(R.string.pref_keyboard_echo_on_screen_key),
            resources.getString(R.string.pref_keyboard_echo_default)));
  }

  public static String getPreferenceVerbosityString(
      SharedPreferences preferences,
      Resources resources,
      @NonNull String verbosityValue,
      String key,
      String defaultValue) {
    if (verbosityValue.equals(resources.getString(R.string.pref_verbosity_preset_value_high))) {
      // If verbosity is high... use rule to select list preference value.
      return getVerbosityValueHighFromListPreference(key, resources);
    } else if (verbosityValue.equals(
        resources.getString(R.string.pref_verbosity_preset_value_low))) {
      // If verbosity is low... use rule to select list preference value.
      return getVerbosityValueLowFromListPreference(key, resources);
    } else {
      // If verbosity is custom... retrieve preference value.
      String keyForVerbosity = toVerbosityPrefKey(verbosityValue, key);
      return preferences.getString(keyForVerbosity, defaultValue);
    }
  }

  private static String getVerbosityValueHighFromListPreference(String key, Resources resources) {
    if (key.equals(resources.getString(R.string.pref_keyboard_echo_on_screen_key))
        || key.equals(resources.getString(R.string.pref_keyboard_echo_physical_key))) {
      String[] keyboardEchoValues = resources.getStringArray(R.array.pref_keyboard_echo_values);
      return (keyboardEchoValues == null || keyboardEchoValues.length == 0)
          ? null
          : keyboardEchoValues[keyboardEchoValues.length - 1];
    } else if (key.equals(resources.getString(R.string.pref_capital_letters_key))) {
      String[] capitalLetterValues = resources.getStringArray(R.array.pref_capital_letters_values);
      return (capitalLetterValues == null || capitalLetterValues.length == 0)
          ? null
          : capitalLetterValues[1]; // Say "cap"
    } else {
      LogUtils.e(TAG, "Unhandled key \"%s\"", key);
      return null;
    }
  }

  private static String getVerbosityValueLowFromListPreference(String key, Resources resources) {
    if (key.equals(resources.getString(R.string.pref_keyboard_echo_on_screen_key))
        || key.equals(resources.getString(R.string.pref_keyboard_echo_physical_key))) {
      String[] keyboardEchoValues = resources.getStringArray(R.array.pref_keyboard_echo_values);
      return (keyboardEchoValues == null || keyboardEchoValues.length == 0)
          ? null
          : keyboardEchoValues[0];
    } else if (key.equals(resources.getString(R.string.pref_capital_letters_key))) {
      String[] capitalLetterValues = resources.getStringArray(R.array.pref_capital_letters_values);
      return (capitalLetterValues == null || capitalLetterValues.length == 0)
          ? null
          : capitalLetterValues[0]; // Do nothing
    } else {
      LogUtils.e(TAG, "Unhandled key \"%s\"", key);
      return null;
    }
  }

  /**
   * Creates new preference key by appending the verbosityName. For example, pref_a11y_hints becomes
   * pref_verbosity_preset_value_high_pref_a11y_hints
   *
   * @param verbosityName verbosity name, such as pref_verbosity_preset_value_high.
   * @param preferenceKey preference key.
   */
  public static String toVerbosityPrefKey(String verbosityName, String preferenceKey) {
    return verbosityName + "_" + preferenceKey;
  }

  /**
   * Restores the preference key to the custom verbosity pref key. For example,
   * pref_verbosity_preset_value_high_pref_a11y_hints restores to pref_a11y_hints
   *
   * @param resources the resources that is used to get the string.
   * @param preferenceKey preference key.
   */
  public static String restoreToCustomVerbosityPrefKey(Resources resources, String preferenceKey) {
    if (preferenceKey == null) {
      return null;
    }

    String substring = preferenceKey;
    if (preferenceKey.contains(resources.getString(R.string.pref_verbosity_preset_value_custom))) {
      substring =
          TextUtils.substring(
              preferenceKey,
              resources.getString(R.string.pref_verbosity_preset_value_custom).length() + 1,
              preferenceKey.length());
    } else if (preferenceKey.contains(
        resources.getString(R.string.pref_verbosity_preset_value_high))) {
      substring =
          TextUtils.substring(
              preferenceKey,
              resources.getString(R.string.pref_verbosity_preset_value_high).length() + 1,
              preferenceKey.length());
    } else if (preferenceKey.contains(
        resources.getString(R.string.pref_verbosity_preset_value_low))) {
      substring =
          TextUtils.substring(
              preferenceKey,
              resources.getString(R.string.pref_verbosity_preset_value_low).length() + 1,
              preferenceKey.length());
    }

    return substring;
  }

  /**
   * Maps verbosity value key to verbosity name.
   *
   * @param verbosityValueKey Preset verbosity key
   * @param context Context used to evaluate strings.
   * @return preset verbosity name
   */
  public static @Nullable String verbosityValueToName(String verbosityValueKey, Context context) {
    if (verbosityValueKey.equals(context.getString(R.string.pref_verbosity_preset_value_high))) {
      return context.getString(R.string.pref_verbosity_preset_entry_high);
    } else if (verbosityValueKey.equals(
        context.getString(R.string.pref_verbosity_preset_value_custom))) {
      return context.getString(R.string.pref_verbosity_preset_entry_custom);
    } else if (verbosityValueKey.equals(
        context.getString(R.string.pref_verbosity_preset_value_low))) {
      return context.getString(R.string.pref_verbosity_preset_entry_low);
    } else {
      return null;
    }
  }

  /**
   * Returns {@code true} if the current verbosity preset value is high or low. Some verbosity
   * preset settings need to be disabled when it is true.
   *
   * @param verbosityValue Preset verbosity
   * @param context Context used to evaluate strings
   */
  public static boolean isVerbosityValueHighOrLow(String verbosityValue, Context context) {
    return TextUtils.equals(
            verbosityValue, context.getString(R.string.pref_verbosity_preset_value_high))
        || TextUtils.equals(
            verbosityValue, context.getString(R.string.pref_verbosity_preset_value_low));
  }

  /**
   * Returns announcement for the change of verbosity.
   *
   * @param verbosityValueKey Preset verbosity key
   * @param context Context used to evaluate strings.
   * @return Announcement text
   */
  public static @Nullable String getVerbosityChangeAnnouncement(
      String verbosityValueKey, Context context) {
    String name = verbosityValueToName(verbosityValueKey, context);
    return TextUtils.isEmpty(name)
        ? null
        : String.format(
            context.getString(R.string.pref_verbosity_preset_change),
            verbosityValueToName(verbosityValueKey, context));
  }

  /**
   * Changes the physical keyboard typing echo setting to the next value, wrapping around to the
   * first value if the last value is currently selected. Note by design, the physical keyboard
   * typing echo setting is only modifiable when the preset verbosity level is set to custom. Per
   * UX, if the preset verbosity level is not custom (high or low), the preset verbosity level
   * should automatically change to custom, in addition to cycling the typing echo.
   *
   * @param context Context used to evaluate strings.
   * @param pipeline Used to play announcement.
   * @param EventId Event id
   * @return true if the typing echo was successfully cycled, false if an error occurred, in which
   *     case all settings are left untouched.
   */
  public static boolean cycleKeyboardTypingEcho(
      Context context, Pipeline.FeedbackReturner pipeline, EventId eventId) {
    if (!FeatureFlagReader.enableCycleTypingEchoKeyboard(context)) {
      return false;
    }
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    SharedPreferences.Editor prefsEditor = prefs.edit();

    String currentVerbosityValue =
        SharedPreferencesUtils.getStringPref(
            prefs,
            context.getResources(),
            R.string.pref_verbosity_preset_key,
            R.string.pref_verbosity_preset_value_default);
    String customVerbosityValue = context.getString(R.string.pref_verbosity_preset_value_custom);
    boolean changeVerbosityLevel = !currentVerbosityValue.equals(customVerbosityValue);
    if (changeVerbosityLevel) {
      prefsEditor.putString(
          context.getString(R.string.pref_verbosity_preset_key), customVerbosityValue);
    }

    List<String> keyboardEchoValues =
        Arrays.asList(context.getResources().getStringArray(R.array.pref_keyboard_echo_values));
    List<String> keyboardEchoEntries =
        Arrays.asList(context.getResources().getStringArray(R.array.pref_keyboard_echo_entries));
    String keyboardEchoKey =
        VerbosityPreferences.toVerbosityPrefKey(
            customVerbosityValue, context.getString(R.string.pref_keyboard_echo_physical_key));
    if (keyboardEchoEntries.size() != keyboardEchoValues.size()) {
      LogUtils.e(TAG, "Keyboard echo values and entries have different sizes");
      return false;
    }

    String currentKeyboardEcho =
        prefs.getString(keyboardEchoKey, context.getString(R.string.pref_keyboard_echo_default));
    int currentIndex = keyboardEchoValues.indexOf(currentKeyboardEcho);
    if (currentIndex == -1) {
      LogUtils.e(TAG, " Current keyboard echo setting not present in possible values");
      return false;
    }

    int newIndex = (currentIndex + 1) % keyboardEchoValues.size();
    String newKeyboardEcho = keyboardEchoValues.get(newIndex);
    prefsEditor.putString(keyboardEchoKey, newKeyboardEcho).apply();
    pipeline.returnFeedback(
        eventId,
        Feedback.speech(
            context.getString(R.string.typing_echo_change, keyboardEchoEntries.get(newIndex))));
    if (changeVerbosityLevel) {
      // If the VerbosityPrefFragment is visible while the user takes this shortcut, the
      // VerbosityPrefFragment will detect this method's change to custom verbosity and try to make
      // the exact same announcement that's being made here. In this specific case though, the
      // speech pipeline is able to deduplicate the announcement, ultimately only announcing this
      // text once.
      pipeline.returnFeedback(
          eventId, Feedback.speech(getVerbosityChangeAnnouncement(customVerbosityValue, context)));
    }
    pipeline.returnFeedback(
        eventId,
        Feedback.talkBackUI(
            Feedback.TalkBackUI.Action.SHOW_GESTURE_OR_KEYBOARD_ACTION_UI,
            GESTURE_OR_KEYBOARD_ACTION_OVERLAY,
            context.getString(
                R.string.typing_echo_change_visual_feedback, keyboardEchoEntries.get(newIndex)),
            /* showIcon= */ false,
            Feedback.TalkBackUI.Item.ITEM_UNSPECIFIED));
    return true;
  }
}
