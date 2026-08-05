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
import android.os.Bundle;
import android.text.TextUtils;
import androidx.preference.Preference;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory;
import com.google.android.accessibility.material.preference.AccessibilitySuiteRadioButtonPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Panel holding a set of text formatting announcement mode preferences. */
public final class TextFormattingAnnouncementModeFragment extends TalkbackBaseFragment {
  private AccessibilitySuitePreferenceCategory announcementModePrefCategory;
  private SharedPreferences prefs;

  /** Preference items for anouncement mode. */
  public enum AnouncementModePref {
    SPEECH_AND_SOUND(
        R.string.pref_text_formatting_announcement_mode_speech_and_sound,
        R.string.pref_text_formatting_announcement_mode_value_speech_sound),
    SPEECH(
        R.string.pref_text_formatting_announcement_mode_speech,
        R.string.pref_text_formatting_announcement_mode_value_speech),
    SOUND(
        R.string.pref_text_formatting_announcement_mode_sound,
        R.string.pref_text_formatting_announcement_mode_value_sound);

    private final int titleId;
    private final int modeId;

    AnouncementModePref(int titleId, int modeId) {
      this.titleId = titleId;
      this.modeId = modeId;
    }

    public int getTitleId() {
      return titleId;
    }

    public int getModeId() {
      return modeId;
    }
  }

  public TextFormattingAnnouncementModeFragment() {
    super(R.xml.text_formatting_announcement_mode_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_text_formatting_announcement_mode);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);

    if (getContext() == null) {
      return;
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(getContext());

    initAnouncementModePref();
  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    if (preference.getParent().equals(announcementModePrefCategory)) {
      String prefTitle = preference.getTitle().toString();
      announcementModeSelected(prefTitle);

      for (AnouncementModePref source : AnouncementModePref.values()) {
        if (TextUtils.equals(prefTitle, getString(source.getTitleId()))) {
          SharedPreferencesUtils.putStringPref(
              prefs,
              getResources(),
              R.string.pref_text_formatting_announcement_mode_key,
              getString(source.getModeId()));
          break;
        }
      }
    }

    return super.onPreferenceTreeClick(preference);
  }

  private void initAnouncementModePref() {
    announcementModePrefCategory =
        (AccessibilitySuitePreferenceCategory)
            findPreference(getString(R.string.pref_text_formatting_announcement_mode_category_key));
    String announcementMode =
        SharedPreferencesUtils.getStringPref(
            prefs,
            getResources(),
            R.string.pref_text_formatting_announcement_mode_key,
            R.string.pref_text_formatting_announcement_mode_value_default);
    for (AnouncementModePref source : AnouncementModePref.values()) {
      if (announcementMode.equals(getString(source.getModeId()))) {
        announcementModeSelected(getString(source.getTitleId()));
        break;
      }
    }
  }

  private void announcementModeSelected(String title) {
    int count = announcementModePrefCategory.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      AccessibilitySuiteRadioButtonPreference pref =
          (AccessibilitySuiteRadioButtonPreference) announcementModePrefCategory.getPreference(i);
      boolean shouldCheck = TextUtils.equals(title, pref.getTitle());
      pref.setChecked(shouldCheck);
    }
  }

  /**
   * Returns the title of the current announcement mode.
   *
   * @param context The context of the current announcement mode.
   * @return The title of the current announcement mode.
   */
  public static String getAnnouncementModeTitle(Context context) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    String announcementMode =
        SharedPreferencesUtils.getStringPref(
            prefs,
            context.getResources(),
            R.string.pref_text_formatting_announcement_mode_key,
            R.string.pref_text_formatting_announcement_mode_value_default);
    for (AnouncementModePref source : AnouncementModePref.values()) {
      if (announcementMode.equals(context.getString(source.getModeId()))) {
        return context.getString(source.getTitleId());
      }
    }
    return context.getString(AnouncementModePref.SPEECH_AND_SOUND.getTitleId());
  }
}
