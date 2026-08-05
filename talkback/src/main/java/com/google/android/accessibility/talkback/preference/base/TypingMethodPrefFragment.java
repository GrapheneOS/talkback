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

import static com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForTapAndTouchExploration.DOUBLE_TAP;
import static com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForTapAndTouchExploration.FORCE_LIFT_TO_TYPE_ON_IME;
import static com.google.android.accessibility.talkback.focusmanagement.FocusProcessorForTapAndTouchExploration.LIFT_TO_TYPE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.annotation.StringRes;
import android.text.TextUtils;
import androidx.preference.Preference;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory;
import com.google.android.accessibility.material.preference.AccessibilitySuiteRadioButtonPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.utils.TalkBackSettingDialogUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;

/** Fragment to customize typing method. */
public class TypingMethodPrefFragment extends TalkbackBaseFragment {
  private static final String TAG = "TypingMethodPrefFragment";
  private AccessibilitySuitePreferenceCategory typingMethodPrefCategory;
  private Context context;
  private SharedPreferences prefs;

  /** Preference items for typing method. */
  public enum TypingMethodPref {
    METHOD_DOUBLE_TAP(R.string.value_type_confirmation_double_tap, DOUBLE_TAP),
    METHOD_LIFT_TO_TYPE_MOST(R.string.value_type_confirmation_lift_to_type, LIFT_TO_TYPE),
    METHOD_LIFT_TO_TYPE_ALL(
        R.string.value_type_confirmation_lift_to_type_for_any_key, FORCE_LIFT_TO_TYPE_ON_IME);

    @StringRes private final int titleId;
    private final int method;

    TypingMethodPref(int titleId, int method) {
      this.titleId = titleId;
      this.method = method;
    }

    @StringRes
    public int getTitleId() {
      return titleId;
    }

    public int getMethod() {
      return method;
    }
  }

  public TypingMethodPrefFragment() {
    super(R.xml.typing_method_preferences);
  }

  @Override
  public CharSequence getTitle() {
    return getText(R.string.title_pref_typing_confirmation);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    context = getContext();

    if (context == null) {
      return;
    }
    prefs = SharedPreferencesUtils.getSharedPreferences(context);

    initTypingMethodPref();
  }

  @Override
  public boolean onPreferenceTreeClick(Preference preference) {
    if (preference.getParent() != null && preference.getParent().equals(typingMethodPrefCategory)) {
      CharSequence prefTitle = preference.getTitle();

      int oldValue =
          SharedPreferencesUtils.getIntFromStringPref(
              prefs,
              context.getResources(),
              R.string.pref_typing_confirmation_key,
              R.string.pref_typing_confirmation_default);
      boolean itemChanged = false;
      int method = LIFT_TO_TYPE;
      for (TypingMethodPref source : TypingMethodPref.values()) {
        if (TextUtils.equals(prefTitle, getString(source.getTitleId()))) {
          method = source.getMethod();
          itemChanged = oldValue != method;
          break;
        }
      }

      if (itemChanged) {
        if (method == FORCE_LIFT_TO_TYPE_ON_IME) {
          // Inform user a reading control item is added to adjust the latency
          // dynamically.
          A11yAlertDialogWrapper dialog =
              TalkBackSettingDialogUtils.createDialogForAddReadingControlItem(this, prefs, context);
          if (dialog != null) {
            dialog.show();
          }
        }
        setTypingMethod(prefTitle);
      }
    }

    return super.onPreferenceTreeClick(preference);
  }

  private void setTypingMethod(CharSequence value) {
    for (TypingMethodPref source : TypingMethodPref.values()) {
      if (TextUtils.equals(value, getString(source.getTitleId()))) {
        SharedPreferencesUtils.putStringPref(
            prefs,
            context.getResources(),
            R.string.pref_typing_confirmation_key,
            String.valueOf(source.getMethod()));
        updateCheckedState(value);
        break;
      }
    }
  }

  private void initTypingMethodPref() {
    typingMethodPrefCategory =
        (AccessibilitySuitePreferenceCategory)
            findPreference(getString(R.string.pref_typing_confirmation_key));
    int typingMethod =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            context.getResources(),
            R.string.pref_typing_confirmation_key,
            R.string.pref_typing_confirmation_default);
    for (TypingMethodPref source : TypingMethodPref.values()) {
      if (typingMethod == source.getMethod()) {
        updateCheckedState(getString(source.getTitleId()));
        break;
      }
    }
  }

  private void updateCheckedState(CharSequence selectedPref) {
    int count = typingMethodPrefCategory.getPreferenceCount();
    for (int i = 0; i < count; i++) {
      AccessibilitySuiteRadioButtonPreference pref =
          (AccessibilitySuiteRadioButtonPreference) typingMethodPrefCategory.getPreference(i);
      boolean shouldCheck = TextUtils.equals(selectedPref, pref.getTitle());
      pref.setChecked(shouldCheck);
    }
  }
}
