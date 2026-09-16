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

import static com.google.android.accessibility.talkback.trainingcommon.TrainingUtils.GUP_SUPPORT_PORTAL_URL;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceGroup;
import androidx.preference.TwoStatePreference;
import com.android.talkback.TalkBackPreferencesActivity.HatsRequesterViewModel;
import com.google.android.accessibility.talkback.HatsSurveyRequester;
import com.google.android.accessibility.talkback.HelpAndFeedbackUtils;
import com.google.android.accessibility.talkback.NotificationActivity;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackExitController;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.ImageCaptioner;
import com.google.android.accessibility.talkback.preference.base.PreferenceActionHelper.WebPage;
import com.google.android.accessibility.talkback.training.OnboardingInitiator;
import com.google.android.accessibility.talkback.training.TutorialInitiator;
import com.google.android.accessibility.talkback.trainingcommon.tv.TvTutorialInitiator;
import com.google.android.accessibility.talkback.trainingcommon.tv.VendorConfigReader;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.NetworkUtils;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;
import java.util.Optional;

/** Fragment that holds the preference of Talkback settings. */
public class TalkBackPreferenceFragment extends TalkbackBaseFragment {
  private Context context;
  private SharedPreferences prefs;
  private SettingsMetricStore settingsMetricStore;

  private Optional<HatsSurveyRequester> hatsSurveyRequester;

  public TalkBackPreferenceFragment() {
    super(R.xml.preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.talkback_preferences_title);
  }

  /**
   * Loads the preferences from the XML preference definition and defines an
   * onPreferenceChangeListener
   */
  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    context = getContext();
    if (context == null) {
      return;
    }

    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    settingsMetricStore = new SettingsMetricStore(context);

    fixListSummaries(getPreferenceScreen());

    HatsRequesterViewModel viewModel =
        new ViewModelProvider(getActivity()).get(HatsRequesterViewModel.class);
    hatsSurveyRequester = Optional.ofNullable(viewModel.getHatsSurveyRequester());
    hatsSurveyRequester.ifPresent(
        listener -> listener.setOnSurveyAvailableListener(() -> updateSurveyOption()));

    assignNewFeaturesIntent();

    // Hiding Speech Rate Settings for all surfaces except Android TV.
    if (!FormFactorUtils.isAndroidTv()) {
      PreferenceSettingsUtils.hidePreference(
          context, getPreferenceScreen(), R.string.pref_speech_rate_key);
    }

    if (SettingsUtils.allowLinksOutOfSettings(context) || FormFactorUtils.isAndroidTv()) {
      assignTtsSettingsIntent();
    } else {
      // During setup, do not allow access to main settings via text-to-speech settings.
      removePreference(R.string.pref_category_audio_key, R.string.pref_tts_settings_key);
    }

    // Changes title from Sound and Vibration to Sound if this device doesn't support vibration.
    if (!FeatureSupport.isVibratorSupported(context)) {
      Preference preference = findPreferenceByResId(R.string.pref_sound_and_vibration_key);
      if (preference != null) {
        preference.setTitle(R.string.title_pref_sound);
      }
    }

    updatePhysicalKeyboardPreference();

    // Remove braille category if none of braille feature supported.
    if (!FeatureSupport.supportBrailleDisplay(context)
        && !FeatureSupport.supportBrailleKeyboard(context)) {
      removeCategory(R.string.pref_category_braille_key);
    } else {
      boolean isMultiTouchSupported =
          context
              .getPackageManager()
              .hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH_JAZZHAND);
      if (!isMultiTouchSupported) {
        final Preference brailleKeyboardCategory =
            findPreferenceByResId(R.string.pref_brailleime_key);
        if (brailleKeyboardCategory != null) {
          brailleKeyboardCategory.setEnabled(false);
          brailleKeyboardCategory.setSummary(R.string.summary_pref_brailleime_disabled);
        }
      }
    }

    if (FormFactorUtils.isAndroidTv()) {
      Preference preference = findPreferenceByResId(R.string.pref_tutorial_and_help_key);
      if (preference != null) {
        preference.setTitle(
            TvTutorialInitiator.shouldShowTraining(VendorConfigReader.retrieveConfig(context))
                ? R.string.title_pref_category_tutorial
                : R.string.title_pref_category_help_no_tutorial);
        preference.setFragment(TutorialAndHelpFragment.class.getName());
      }
    } else if (FormFactorUtils.isAndroidWear()) {
      Preference prefTutorial = findPreferenceByResId(R.string.pref_tutorial_key);
      if (prefTutorial != null) {
        prefTutorial.setIntent(TutorialInitiator.createTutorialIntent(getActivity()));
      }
      Preference prefHelp = findPreferenceByResId(R.string.pref_help_key);
      if (prefHelp != null) {
        // Only Wear has this preference in this fragment.
        PreferenceActionHelper.assignWebIntentToPreference(this, prefHelp, WebPage.WEB_PAGE_HELP);
      }
    } else {
      updateTutorialAndHelpPreferencesForPhoneOrTablet();
    }

    if (!ImageCaptioner.supportsImageCaption(context)) {
      removePreference(R.string.pref_category_audio_key, R.string.pref_auto_image_captioning_key);
    }
    updateGeminiPreferenceState();
  }

  private void updateGeminiPreferenceState() {
    Preference geminiSupport = findPreferenceByResId(R.string.pref_gemini_settings_key);
    if (geminiSupport != null) {
      boolean hasOptIn =
          SharedPreferencesUtils.getBooleanPref(
              prefs,
              context.getResources(),
              R.string.pref_gemini_enabled_key,
              R.bool.pref_gemini_opt_in_default);
      geminiSupport.setSummary(
          hasOptIn
              ? R.string.summary_pref_gemini_support_enabled
              : R.string.summary_pref_gemini_support_disabled);
    } else {
      removePreference(R.string.pref_category_controls_key, R.string.pref_gemini_settings_key);
    }
  }

  private void updateTutorialAndHelpPreferencesForPhoneOrTablet() {
    Preference preference = findPreferenceByResId(R.string.pref_tutorial_and_help_key);
    if (preference != null) {
      preference.setIntent(TutorialInitiator.createTutorialIntent(getActivity()));
    }
    preference = findPreferenceByResId(R.string.pref_help_and_feedback_key);
    if (preference != null) {
      if (HelpAndFeedbackUtils.supportsHelpAndFeedback(context)) {
        preference.setOnPreferenceClickListener(
            preference1 -> {
              HelpAndFeedbackUtils.launchHelpAndFeedback(getActivity());
              return true;
            });
      } else {
        preference.setTitle(R.string.title_pref_help);
        PreferenceActionHelper.assignWebIntentToPreference(this, preference, WebPage.WEB_PAGE_HELP);
      }
    }
    preference = findPreferenceByResId(R.string.pref_gup_key);
    if (preference != null) {
      preference.setOnPreferenceClickListener(
          gUpPreference -> {
            if (NetworkUtils.isNetworkConnected(context)) {
              settingsMetricStore.onGupPreferenceClicked();
            }
            return false;
          });
      Intent intent = new Intent(Intent.ACTION_VIEW);
      intent.setData(Uri.parse(GUP_SUPPORT_PORTAL_URL));
      preference.setIntent(intent);
    }
  }

  private void updatePhysicalKeyboardPreference() {
    Preference preference = findPreferenceByResId(R.string.pref_physical_keyboard_key);
    if (preference == null) {
      return;
    }

    // Assign physical keyboard settings if it is available.
    InputDeviceMonitor inputDeviceMonitor = new InputDeviceMonitor(context);
    if (inputDeviceMonitor.hasPhysicalKeyboard()) {
      Intent intent = new Intent(Settings.ACTION_HARD_KEYBOARD_SETTINGS);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
      if (PreferenceSettingsUtils.canHandleIntent(context, intent)) {
        preference.setIntent(intent);
      }
    } else {
      removePreference(R.string.pref_category_typing_key, R.string.pref_physical_keyboard_key);
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    updateSurveyOption();
    updateGeminiPreferenceState();
    updatePhysicalKeyboardPreference();
    updateTtsOverlay();

    // Disable watermark when user enters TalkBack settings.
    disableTalkBackExitWatermark();
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    hatsSurveyRequester.ifPresent(requester -> requester.setOnSurveyAvailableListener(null));
  }

  private void removePreference(int categoryKeyId, int preferenceKeyId) {
    final PreferenceGroup category = (PreferenceGroup) findPreferenceByResId(categoryKeyId);
    if (category != null) {
      PreferenceSettingsUtils.hidePreference(context, category, preferenceKeyId);
    }
  }

  private void removeCategory(int categoryKeyId) {
    final PreferenceCategory category = (PreferenceCategory) findPreferenceByResId(categoryKeyId);
    if (category != null) {
      getPreferenceScreen().removePreference(category);
    }
  }

  /** Assigns the intent to open text-to-speech settings. */
  private void assignTtsSettingsIntent() {
    PreferenceGroup category =
        (PreferenceGroup) findPreferenceByResId(R.string.pref_category_audio_key);
    Preference ttsSettingsPreference = findPreferenceByResId(R.string.pref_tts_settings_key);

    if (category == null || ttsSettingsPreference == null) {
      return;
    }

    // Removing Text-to-Speech Settings for TV.
    if (FormFactorUtils.isAndroidTv()) {
      category.removePreference(ttsSettingsPreference);
      return;
    }

    Intent ttsSettingsIntent = new Intent(TalkBackService.INTENT_TTS_SETTINGS);
    if (!PreferenceSettingsUtils.canHandleIntent(context, ttsSettingsIntent)) {
      // Need to remove preference item if no TTS Settings intent filter in settings app.
      category.removePreference(ttsSettingsPreference);
    }

    ttsSettingsPreference.setIntent(ttsSettingsIntent);
  }

  private void assignNewFeaturesIntent() {
    final Preference prefNewFeatures =
        findPreferenceByResId(R.string.pref_new_feature_in_talkback_entry_point_key);

    if (prefNewFeatures == null) {
      return;
    }

    if (FormFactorUtils.isAndroidTv() || FormFactorUtils.isAndroidXr()) {
      return;
    }

    Intent newFeatureIntent;
    if (FormFactorUtils.isAndroidWear()) {
      newFeatureIntent =
          NotificationActivity.createStartIntent(
              context,
              R.string.wear_new_feature_page_title,
              R.string.wear_new_feature_page_content,
              Integer.MIN_VALUE,
              R.string.wear_new_feature_page_button_content_description,
              /* url= */ null);
    } else {
      newFeatureIntent = OnboardingInitiator.createOnboardingIntentForSettings(context);
    }
    prefNewFeatures.setIntent(newFeatureIntent);
  }

  private void updateSurveyOption() {
    final Preference prefSurvey =
        findPreferenceByResId(R.string.pref_survey_setting_entry_point_key);

    if (prefSurvey == null) {
      return;
    }

    if (hatsSurveyRequester.isEmpty()) {
      prefSurvey.setVisible(false);
      return;
    }

    if (!hatsSurveyRequester.get().isSurveyAvailable()) {
      prefSurvey.setVisible(false);
      return;
    }

    prefSurvey.setVisible(true);
    prefSurvey.setOnPreferenceClickListener(
        preference -> {
          hatsSurveyRequester.ifPresent(
              requester -> {
                boolean unused = requester.presentCachedSurvey();
              });
          prefSurvey.setVisible(false);
          return true;
        });
  }

  private void updateTtsOverlay() {
    final TwoStatePreference preference =
        (TwoStatePreference) findPreferenceByResId(R.string.pref_tts_overlay_key);
    if (preference == null) {
      return;
    }
    boolean prefValue =
        SharedPreferencesUtils.getBooleanPref(
            prefs,
            context.getResources(),
            R.string.pref_tts_overlay_key,
            context.getResources().getBoolean(R.bool.pref_tts_overlay_default));
    if (prefValue == preference.isChecked()) {
      return;
    }
    preference.setChecked(prefValue);
  }

  /**
   * Since the "%s" summary is currently broken, this sets the preference change listener for all
   * {@link ListPreference} views to fill in the summary with the current entry value.
   */
  private void fixListSummaries(PreferenceGroup group) {
    if (group == null) {
      return;
    }

    final int count = group.getPreferenceCount();

    for (int i = 0; i < count; i++) {
      final Preference preference = group.getPreference(i);

      if (preference instanceof PreferenceGroup) {
        fixListSummaries((PreferenceGroup) preference);
      } else if (preference instanceof ListPreference) {
        // First make sure the current summary is correct, then set the
        // listener. This is necessary for summaries to show correctly
        // on SDKs < 14.
        preferenceChangeListener.onPreferenceChange(
            preference, ((ListPreference) preference).getValue());

        preference.setOnPreferenceChangeListener(preferenceChangeListener);
      }
    }
  }

  private void disableTalkBackExitWatermark() {
    TalkBackService service = TalkBackService.getInstance();
    if (service == null) {
      return;
    }
    TalkBackExitController talkBackExitController = service.getTalkBackExitController();
    if (talkBackExitController != null && talkBackExitController.isTalkBackExitWatermarkShown()) {
      talkBackExitController.disableTalkBackExitWatermark(service);
    }
  }

  /**
   * Listens for preference changes and updates the summary to reflect the current setting. This
   * shouldn't be necessary, since preferences are supposed to automatically do this when the
   * summary is set to "%s".
   */
  private final OnPreferenceChangeListener preferenceChangeListener =
      new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
          if (preference instanceof ListPreference && newValue instanceof String) {
            final ListPreference listPreference = (ListPreference) preference;
            final int index = listPreference.findIndexOfValue((String) newValue);
            final CharSequence[] entries = listPreference.getEntries();

            // Ignore Setting speech rate summary.
            if (listPreference
                .getKey()
                .equals(getContext().getString(R.string.pref_speech_rate_key))) {
              return true;
            }

            if (index >= 0 && index < entries.length) {
              preference.setSummary(entries[index].toString().replaceAll("%", "%%"));
            } else {
              preference.setSummary("");
            }
          }

          return true;
        }
      };
}
