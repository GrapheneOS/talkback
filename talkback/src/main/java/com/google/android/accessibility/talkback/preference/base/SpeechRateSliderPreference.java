/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.accessibility.talkback.preference.base;

import android.content.Context;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.util.AttributeSet;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.preference.PreferenceViewHolder;
import com.google.android.accessibility.material.preference.AccessibilitySliderPreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Preference for adjusting talkback speech rate. The state description and increase/decrease value
 * actions are customized to match the SpeechRateAndPitchActor used in selector.
 */
@RequiresApi(api = VERSION_CODES.S)
public class SpeechRateSliderPreference extends AccessibilitySliderPreference {

  private SeekBar seekBar = null;

  public SpeechRateSliderPreference(@NonNull Context context) {
    super(context);
  }

  public SpeechRateSliderPreference(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
    super.onBindViewHolder(holder);
    seekBar = (SeekBar) holder.findViewById(R.id.seekbar);

    if (seekBar == null) {
      return;
    }

    ViewCompat.setStateDescription(
        seekBar, getContext().getString(R.string.tb_template_percent, String.valueOf(getValue())));

    ViewCompat.replaceAccessibilityAction(
        seekBar,
        AccessibilityActionCompat.ACTION_SCROLL_BACKWARD,
        null,
        (view, arguments) -> {
          int newRate =
              (int)
                  Math.max(
                      getValue() / SpeechRateAndPitchActor.RATE_STEP,
                      SpeechRateAndPitchActor.RATE_MINIMUM * 100);
          setSeekBarValue(newRate);
          return true;
        });

    ViewCompat.replaceAccessibilityAction(
        seekBar,
        AccessibilityActionCompat.ACTION_SCROLL_FORWARD,
        null,
        (view, arguments) -> {
          int newRate =
              (int)
                  Math.min(
                      getValue() * SpeechRateAndPitchActor.RATE_STEP,
                      SpeechRateAndPitchActor.RATE_MAXIMUM * 100);
          setSeekBarValue(newRate);
          return true;
        });
  }

  public @Nullable SeekBar getSeekBar() {
    return seekBar;
  }

  private void setSeekBarValue(int newValue) {
    if (seekBar == null) {
      return;
    }

    int forcedValue =
        (int)
            (SpeechRateAndPitchActor.forceRateToDefaultWhenClose((float) newValue / 100.0f) * 100);

    // We can't call seekBar.setProgress() because it calls setProgressInternal(int progress,
    // boolean fromUser, boolean animate) with fromUser being false. Use set progress accessibility
    // action instead so that fromUser will be true when calling setProgressInternal(int progress,
    // boolean fromUser, boolean animate). SeekBarPreference only calls OnPreferenceChangeListener
    // when fromUser is true.
    Bundle arg = new Bundle();
    arg.putFloat(
        AccessibilityNodeInfoCompat.ACTION_ARGUMENT_PROGRESS_VALUE, forcedValue - getMin());
    seekBar.performAccessibilityAction(android.R.id.accessibilityActionSetProgress, arg);
  }
}
