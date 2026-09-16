/*
 * Copyright (C) 2023 Google Inc.
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

package com.google.android.accessibility.brailleime.settings;

import static com.google.android.accessibility.braille.common.BrailleUserPreferences.BRAILLE_SHARED_PREFS_FILENAME;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.BASIC;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.CURSOR_MOVEMENT;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.SPELL_CHECK;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.TEXT_SELECTION_AND_EDITING;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextThemeWrapper;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.brailleime.FeatureFlagReader;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.brailleime.SupportedCommand.Category;
import com.google.android.accessibility.brailleime.dialog.ResetAllGestureDialog;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.preference.PreferencesActivity;
import com.google.android.material.snackbar.Snackbar;

/** An activity for showing a BrailleIme gesture. */
public class BrailleImeGestureActivity extends PreferencesActivity {
  public static final String CATEGORY = "category";

  @Override
  protected PreferenceFragmentCompat createPreferenceFragment() {
    return new BrailleImeGestureFragment();
  }

  /** Fragment that holds the braille keyboard gesture preference. */
    public static class BrailleImeGestureFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      getPreferenceManager().setSharedPreferencesName(BRAILLE_SHARED_PREFS_FILENAME);
      PreferenceSettingsUtils.addPreferencesFromResource(this, R.xml.brailleime_gesture_category);
      setIntentForClickEvent(R.string.pref_key_brailleime_basic_controls, BASIC);
      setIntentForClickEvent(R.string.pref_key_brailleime_cursor_movement, CURSOR_MOVEMENT);
      setIntentForClickEvent(
          R.string.pref_key_brailleime_text_selection_and_editing, TEXT_SELECTION_AND_EDITING);
      setIntentForClickEvent(R.string.pref_key_brailleime_spell_check, SPELL_CHECK);
      Preference preference =
          findPreference(getString(R.string.pref_key_brailleime_reset_all_gestures));
      preference.setVisible(FeatureFlagReader.useGestureCustomization(getContext()));
      preference.setOnPreferenceClickListener(
          clickedPreference -> {
            ResetAllGestureDialog dialog =
                new ResetAllGestureDialog(
                    getContext(),
                    () -> {
                      BrailleUserPreferences.resetAllGestureActionMap(
                          BrailleImeGestureFragment.this.getContext());
                      showSnackBar(
                          R.string.braille_keyboard_snackbar_reset_all, Snackbar.LENGTH_LONG);
                    });
            dialog.show();
            return true;
          });
    }

    private void setIntentForClickEvent(int prefKeyId, Category category) {
      Preference preference = findPreference(getString(prefKeyId));
      Intent intent = new Intent(getContext(), BrailleImeGestureCommandActivity.class);
      intent.putExtra(CATEGORY, category);
      preference.setIntent(intent);
    }

    private void showSnackBar(@StringRes int resId, int duration) {
      Context themedContext = getContext();
      Snackbar.make(themedContext, getView(), themedContext.getString(resId), duration).show();
    }
  }
}
