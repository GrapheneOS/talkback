/*
 * Copyright (C) 2025 Google Inc.
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

package com.google.android.accessibility.talkback.actor;

import static com.google.android.accessibility.talkback.Feedback.TouchLatency.LatencyAction.TOUCH_FOCUS_LATENCY_ACTION;
import static com.google.android.accessibility.talkback.Feedback.TouchLatency.LatencyAction.TYPING_FOCUS_LATENCY_ACTION;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import com.google.android.accessibility.talkback.Feedback.TouchLatency;
import com.google.android.accessibility.talkback.Feedback.TouchLatency.LatencyAction;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.preference.base.FocusDelayPrefFragment;
import com.google.android.accessibility.talkback.preference.base.TypingFocusDelayPrefFragment;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;

/**
 * This class supports to adjust the time latency when deciding to enter touch explore. When the
 * current value is already maximum and user tries to increase it, or the value is minimum and user
 * tries to decrease it, user will get notify about the status.
 */
public class TouchLatencyAdjustor {
  private static final String TAG = "TouchLatencyAdjustor";
  private static final int MAX_LATENCY = 300;
  private static final int MIN_LATENCY = 150;
  private final Context context;
  private final SharedPreferences prefs;
  private Pipeline.FeedbackReturner pipeline;

  public TouchLatencyAdjustor(@NonNull Context context) {
    this.context = context;
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  /**
   * Adjust the time latency value.
   *
   * @param touchLatency To specify the touch latency type and direction to adjust.
   * @return false if value reaches its maximum/minimum according to the adjusting direction.
   */
  public boolean adjustLatency(TouchLatency touchLatency) {
    if ((touchLatency.action() == TOUCH_FOCUS_LATENCY_ACTION)
            && !FeatureFlagReader.touchFocusTimeout(context)
        || (touchLatency.action() == TYPING_FOCUS_LATENCY_ACTION)
            && !FeatureFlagReader.typingFocusTimeout(context)) {
      return false;
    }
    LatencyAction action = touchLatency.action();
    boolean upward = touchLatency.increaseLatency();
    int imeUserIntentTimeout =
        SharedPreferencesUtils.getIntFromStringPref(
            prefs,
            context.getResources(),
            action == TOUCH_FOCUS_LATENCY_ACTION
                ? R.string.pref_touch_focus_time_out_key
                : R.string.pref_typing_focus_time_out_key,
            action == TOUCH_FOCUS_LATENCY_ACTION
                ? R.string.pref_touch_focus_time_out_default
                : R.string.pref_touch_explore_time_out_default);
    LogUtils.d(TAG, "Adjust latency value:%s", touchLatency);

    if ((upward && imeUserIntentTimeout >= MAX_LATENCY)
        || (!upward && imeUserIntentTimeout <= MIN_LATENCY)) {
      return false;
    }
    imeUserIntentTimeout += upward ? 50 : -50;
    if (imeUserIntentTimeout > MAX_LATENCY) {
      imeUserIntentTimeout = MAX_LATENCY;
    } else if (imeUserIntentTimeout < MIN_LATENCY) {
      imeUserIntentTimeout = MIN_LATENCY;
    }
    if (action == TOUCH_FOCUS_LATENCY_ACTION) {
      updateFocusDelayPreference(imeUserIntentTimeout);
    } else {
      updateTypingFocusDelayPreference(imeUserIntentTimeout);
    }

    return true;
  }

  private void updateFocusDelayPreference(int timeout) {
    for (FocusDelayPrefFragment.FocusDelayPref source :
        FocusDelayPrefFragment.FocusDelayPref.values()) {
      if (timeout == source.getDelay()) {
        SharedPreferencesUtils.putStringPref(
            prefs,
            context.getResources(),
            R.string.pref_touch_focus_time_out_key,
            Integer.toString(timeout));
        break;
      }
    }
  }

  private void updateTypingFocusDelayPreference(int timeout) {
    for (TypingFocusDelayPrefFragment.TypingFocusDelayPref source :
        TypingFocusDelayPrefFragment.TypingFocusDelayPref.values()) {
      if (timeout == source.getDelay()) {
        SharedPreferencesUtils.putStringPref(
            prefs,
            context.getResources(),
            R.string.pref_typing_focus_time_out_key,
            Integer.toString(timeout));
        break;
      }
    }
  }
}
