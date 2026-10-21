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
import com.google.android.accessibility.talkback.utils.TalkBackSettingDialogUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;

/** Fragment to customize typing focus latency. */
public class TypingFocusDelayPrefFragment extends TalkbackBaseFragment {
  private static final String TAG = "TypingFocusDelayPrefFragment";
  private AccessibilitySuitePreferenceCategory focusDelayPrefCategory;
  private Context context;
  private SharedPreferences prefs;

  /** Preference items for Typing focus delay. */
  public enum TypingFocusDelayPref {
    DELAY_150_MS(R.string.value_touch_explore_time_out_150ms, 150),
    DELAY_200_MS(R.string.value_touch_explore_time_out_200ms, 200),
    DELAY_250_MS(R.string.value_touch_explore_time_out_250ms, 250),
    DELAY_300_MS(R.string.value_touch_explore_time_out_300ms, 300);

    private final int titleId;
    private final int delay;

    TypingFocusDelayPref(int titleId, int delay) {
      this.titleId = titleId;
      this.delay = delay;
    }

    public int getTitleId() {
      return titleId;
    }

    public int getDelay() {
      return delay;
    }
  }

  public TypingFocusDelayPrefFragment() {
    super(R.xml.typing_focus_delay_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_touch_explore_timeout);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    context = getContext();

    if (context == null || !FeatureSupport.supportCustomizingFocusIndicator()) {
      return;
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(context);

    initFocusDelayPref();
  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    if (preference.getParent().equals(focusDelayPrefCategory)) {
      CharSequence prefTitle = preference.getTitle();
      int oldValue =
          SharedPreferencesUtils.getIntFromStringPref(
              prefs,
              context.getResources(),
              R.string.pref_typing_focus_time_out_key,
              R.string.pref_touch_focus_time_out_default);
      boolean itemChanged = false;
      for (TypingFocusDelayPrefFragment.TypingFocusDelayPref source :
          TypingFocusDelayPrefFragment.TypingFocusDelayPref.values()) {
        if (TextUtils.equals(prefTitle, getString(source.getTitleId()))) {
          itemChanged = oldValue != source.getDelay();
          break;
        }
      }

      if (itemChanged) {
        // Inform user a reading control item is added to adjust the latency
        // dynamically.
        A11yAlertDialogWrapper dialog =
            TalkBackSettingDialogUtils.createDialogForAddReadingControlItem(this, prefs, context);
        if (dialog != null) {
          dialog.show();
        }
        setFocusDelay(prefTitle);
      }
    }

    return super.onPreferenceTreeClick(preference);
  }

  private void setFocusDelay(CharSequence value) {
    for (TypingFocusDelayPrefFragment.TypingFocusDelayPref source :
        TypingFocusDelayPrefFragment.TypingFocusDelayPref.values()) {
      if (TextUtils.equals(value, getString(source.getTitleId()))) {
        SharedPreferencesUtils.putStringPref(
            prefs,
            context.getResources(),
            R.string.pref_typing_focus_time_out_key,
            String.valueOf(source.getDelay()));
        updateCheckedState(value);
        break;
      }
    }
  }

  private void initFocusDelayPref() {
    focusDelayPrefCategory =
        (AccessibilitySuitePreferenceCategory)
            findPreference(getString(R.string.pref_typing_focus_time_out_key));
    int focusDelay =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            context.getResources(),
            R.string.pref_typing_focus_time_out_key,
            R.string.pref_touch_focus_time_out_default);
    for (TypingFocusDelayPrefFragment.TypingFocusDelayPref source :
        TypingFocusDelayPrefFragment.TypingFocusDelayPref.values()) {
      if (focusDelay == source.getDelay()) {
        updateCheckedState(getString(source.getTitleId()));
        break;
      }
    }
  }

  private void updateCheckedState(CharSequence selectedPref) {
    int count = focusDelayPrefCategory.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      AccessibilitySuiteRadioButtonPreference pref =
          (AccessibilitySuiteRadioButtonPreference) focusDelayPrefCategory.getPreference(i);
      boolean shouldCheck = TextUtils.equals(selectedPref, pref.getTitle().toString());
      pref.setChecked(shouldCheck);
    }
  }
}
