/*
 * Copyright (C) 2020 The Android Open Source Project
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

import static com.google.android.accessibility.utils.SettingsUtils.isTouchExplorationEnabled;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.format.DateFormat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.monitor.RingerModeAndScreenMonitor;
import com.google.android.accessibility.talkback.preference.base.PreferenceActionHelper.WebPage;
import com.google.android.accessibility.talkback.utils.DateTimeUtils;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Fragment to display advanced settings. */
public class AdvancedSettingFragment extends TalkbackBaseFragment {
  private static final String TAG = "AdvancedSettingFragment";

  private Context context;

  private SharedPreferences prefs;

  public AdvancedSettingFragment() {
    super(R.xml.advanced_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_advanced_settings);
  }

  @Override
  public void onResume() {
    super.onResume();
    updateTouchExplorationState();
    updateTimeFeedbackFormatPreference();
  }

  private void updateTimeFeedbackFormatPreference() {
    final ListPreference timeFeedbackFormatPref =
        findPreference(getString(R.string.pref_time_feedback_format_key));
    if (timeFeedbackFormatPref != null) {
      timeFeedbackFormatPref.setSummaryProvider(preference -> getSummaryForTimeFeedbackFormat());
    }
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
  }

  private String getSummaryForTimeFeedbackFormat() {
    final Resources resources = getResources();
    final String timeFeedbackFormat =
        SharedPreferencesUtils.getStringPref(
            prefs,
            resources,
            R.string.pref_time_feedback_format_key,
            R.string.pref_time_feedback_format_default);
    int timeFeedbackFormatType =
        RingerModeAndScreenMonitor.prefValueToTimeFeedbackFormat(resources, timeFeedbackFormat);

    return switch (timeFeedbackFormatType) {
      case DateTimeUtils.TIME_FEEDBACK_FORMAT_12_HOURS ->
          getString(R.string.pref_time_feedback_format_entries_12_hour);
      case DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS ->
          getString(R.string.pref_time_feedback_format_entries_24_hour);
      case DateTimeUtils.TIME_FEEDBACK_FORMAT_UNDEFINED ->
          getString(R.string.pref_time_feedback_format_entries_default);
      default -> {
        LogUtils.w(TAG, "Unexpected time format: %d", timeFeedbackFormat);
        yield DateFormat.is24HourFormat(getContext())
            ? getString(R.string.pref_time_feedback_format_entries_24_hour)
            : getString(R.string.pref_time_feedback_format_entries_12_hour);
      }
    };
  }

  /**
   * Updates the preferences state to match the actual state of touch exploration. This is called
   * once when the preferences activity launches and again whenever the actual state of touch
   * exploration changes.
   */
  private void updateTouchExplorationState() {
    final ContentResolver resolver = context.getContentResolver();
    final Resources res = getResources();
    final boolean requestedState =
        SharedPreferencesUtils.getBooleanPref(
            prefs, res, R.string.pref_explore_by_touch_key, R.bool.pref_explore_by_touch_default);
    final boolean actualState;

    // If accessibility is disabled then touch exploration is always
    // disabled, so the "actual" state should just be the requested state.
    if (TalkBackService.isServiceActive()) {
      actualState = isTouchExplorationEnabled(resolver);
    } else {
      actualState = requestedState;
    }

    // TODO: Use SharedViewModel mechanism to handle this across TalkBack settings fragments.
    // Enable/disable preferences that depend on explore-by-touch.
    // Cannot use "dependency" attribute in preferences XML file, because touch-explore-preference
    // is in a different preference-activity (developer preferences).
    Preference singleTapPref = findPreference(getString(R.string.pref_single_tap_key));
    if (singleTapPref != null) {
      singleTapPref.setEnabled(actualState);
    }
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    context = getContext();
    prefs = SharedPreferencesUtils.getSharedPreferences(context);

    // During setup, do not allow user access below settings.
    if (!SettingsUtils.allowLinksOutOfSettings(context)) {
      PreferenceGroup controlCategory =
          (PreferenceGroup) findPreferenceByResId(R.string.pref_category_controls_key);
      if (controlCategory != null) {
        PreferenceSettingsUtils.hidePreference(
            context, controlCategory, R.string.pref_manage_labels_key);
      }
      // Legal and privacy settings that use WebView.
      PreferenceGroup otherCategory =
          (PreferenceGroup) findPreferenceByResId(R.string.pref_category_others_key);
      if (otherCategory != null) {
        PreferenceSettingsUtils.hidePreference(context, otherCategory, R.string.pref_policy_key);
        PreferenceSettingsUtils.hidePreference(context, otherCategory, R.string.pref_show_tos_key);
      }
    }

    // Link preferences to web-viewer. The behavior depends on the type of form factors.
    if (findPreference(getString(R.string.pref_policy_key)) != null) {
      PreferenceActionHelper.assignWebIntentToPreference(
          this,
          findPreference(getString(R.string.pref_policy_key)),
          WebPage.WEB_PAGE_PRIVACY_POLICY);
    }
    if (findPreference(getString(R.string.pref_show_tos_key)) != null) {
      PreferenceActionHelper.assignWebIntentToPreference(
          this,
          findPreference(getString(R.string.pref_show_tos_key)),
          WebPage.WEB_PAGE_TERMS_OF_SERVICE);
    }

    // Hiding the smart browse mode toggle if the feature is not enabled.
    if (!FeatureFlagReader.enableSmartBrowseMode(context)) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_smart_browse_mode_key);
    }

    // TODO hide preference until the feature stable.
    if (!TalkbackFeatureSupport.supportTalkBackExitBanner(context)) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_show_exit_watermark_key);
    }
  }
}
