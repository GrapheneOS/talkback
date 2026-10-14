/*
 * Copyright (C) 2020 Google Inc.
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

import android.content.Context;
import android.content.SharedPreferences;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** This class supports changing speech rate and pitch. */
public class SpeechRateAndPitchActor {

  /** Read-only interface for actor-state data. */
  public class RateState {
    public int getSpeechRatePercentage() {
      return SpeechRateAndPitchActor.this.getSpeechRatePercentage();
    }
  }

  private final Context context;
  private final SharedPreferences prefs;
  public final SpeechRateAndPitchActor.RateState rateState =
      new SpeechRateAndPitchActor.RateState();
  // Speech rate is a multiplier to the TTS_DEFAULT_RATE. Here defines the range from 10% to 600%.
  // Each step is increase/decrease 10%.
  public static final float RATE_MINIMUM = 0.1f;
  public static final float RATE_MAXIMUM = 6.0f;
  public static final float RATE_STEP = 1.1f;
  // Speech pitch
  public static final float PITCH_MINIMUM = 0.2f;
  public static final float PITCH_MAXIMUM = 2.0f;
  public static final float PITCH_STEP = 0.1f;
  private int speechRatePercent;
  private float speechPitch;

  public SpeechRateAndPitchActor(Context context) {
    this.context = context;
    prefs = SharedPreferencesUtils.getSharedPreferences(context);
    speechRatePercent = (int) (getCurrentOrDefaultSpeechRate(/* isCurrent= */ true) * 100);
    speechPitch = getCurrentOrDefaultSpeechPitch(/* isCurrent= */ true);
  }

  /**
   * getCurrentOrDefaultSpeechRate: utility to return the current speech rate or the default speech
   * rate.
   *
   * @param isCurrent whether the current speech rate is requested.
   */
  private float getCurrentOrDefaultSpeechRate(boolean isCurrent) {
    return SharedPreferencesUtils.getFloatFromStringPref(
        prefs,
        context.getResources(),
        isCurrent ? R.string.pref_speech_rate_key : R.string.pref_speech_rate_default,
        R.string.pref_speech_rate_default);
  }

  /**
   * getCurrentOrDefaultSpeechPitch: utility to return the current speech pitch or the default
   * speech pitch.
   *
   * @param isCurrent whether the current speech pitch is requested.
   */
  private float getCurrentOrDefaultSpeechPitch(boolean isCurrent) {
    return SharedPreferencesUtils.getFloatFromStringPref(
        prefs,
        context.getResources(),
        isCurrent ? R.string.pref_speech_pitch_key : R.string.pref_speech_pitch_default,
        R.string.pref_speech_pitch_default);
  }

  /**
   * changeSpeechRate: utility to change speech rate based on current settings.
   *
   * @param isIncrease to specify speech rate increase (true) or decrease (false).
   * @return true always.
   */
  public boolean changeSpeechRate(boolean isIncrease) {
    float currentRate = getCurrentOrDefaultSpeechRate(/* isCurrent= */ true);
    float newRate =
        isIncrease
            ? Math.min(currentRate * RATE_STEP, RATE_MAXIMUM)
            : Math.max(currentRate / RATE_STEP, RATE_MINIMUM);

    newRate = forceRateToDefaultWhenClose(newRate);

    speechRatePercent = (int) (newRate * 100);
    applySpeechRateToPrefs(newRate, speechRatePercent);

    return true;
  }

  /** resetSpeechRate: utility to reset speech rate to default. */
  public void resetSpeechRate() {
    float defaultRate = getCurrentOrDefaultSpeechRate(/* isCurrent= */ false);

    speechRatePercent = (int) (defaultRate * 100);
    applySpeechRateToPrefs(defaultRate, speechRatePercent);
  }

  /**
   * applySpeechRateToPrefs: applies speech rate to SharedPreferences.
   *
   * @param speechRate the speech rate to be applied.
   * @param speechRatePercent the speech rate percent to be applied.
   */
  private void applySpeechRateToPrefs(float speechRate, int speechRatePercent) {
    prefs
        .edit()
        .putString(context.getString(R.string.pref_speech_rate_key), Float.toString(speechRate))
        .putInt(context.getString(R.string.pref_speech_rate_seekbar_key_int), speechRatePercent)
        .apply();
  }

  public int getSpeechRatePercentage() {
    return speechRatePercent;
  }

  /**
   * changeSpeechPitch: utility to change speech pitch based on current settings.
   *
   * @param isIncrease to specify speech pitch increase (true) or decrease (false).
   * @return true if the pitch changed; otehrwise, false.
   */
  public boolean changeSpeechPitch(boolean isIncrease) {
    float currentPitch = getCurrentOrDefaultSpeechPitch(/* isCurrent= */ true);
    float newPitch =
        isIncrease
            ? Math.min(currentPitch + PITCH_STEP, PITCH_MAXIMUM)
            : Math.max(currentPitch - PITCH_STEP, PITCH_MINIMUM);

    if (currentPitch == newPitch) {
      // Return false to indicate that the pitch didn't change. This is because it reached either
      // the minimum or maximum pitch.
      return false;
    }

    speechPitch = newPitch;
    applySpeechPitchToPrefs(newPitch);

    return true;
  }

  /** resetSpeechPitch: utility to reset speech pitch to default. */
  public void resetSpeechPitch() {
    float defaultPitch = getCurrentOrDefaultSpeechPitch(/* isCurrent= */ false);

    speechPitch = defaultPitch;
    applySpeechPitchToPrefs(defaultPitch);
  }

  /**
   * applySpeechPitchToPrefs: applies speech pitch to SharedPreferences.
   *
   * @param speechPitch the speech pitch to be applied.
   */
  private void applySpeechPitchToPrefs(float speechPitch) {
    prefs
        .edit()
        .putString(context.getString(R.string.pref_speech_pitch_key), Float.toString(speechPitch))
        .apply();
  }

  public float getSpeechPitch() {
    return speechPitch;
  }

  /**
   * Since the speech rate will no longer be a multiple of RATE_STEP after reaching RATE_MAXIMUM or
   * RATE_MINIMUM, the rate cannot get back to TTS_DEFAULT_RATE with further calculation through
   * RATE_STEP. Therefore, forcing the new rate to be TTS_DEFAULT_RATE when the calculated result is
   * close to 1. The boundary for resetting to TTS_DEFAULT_RATE should consider the value of
   * RATE_STEP to avoid falling into a trap that the new rate could never escape.
   *
   * @param rate the input rate to be forced
   * @return the output rate after forcing.
   */
  public static float forceRateToDefaultWhenClose(float rate) {
    if (rate > 0.95f && rate < 1.05f) {
      return 1.0f;
    }
    return rate;
  }
}
