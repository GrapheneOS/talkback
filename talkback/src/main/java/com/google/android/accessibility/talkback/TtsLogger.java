/*
 * Copyright (C) 2024 Google Inc.
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

package com.google.android.accessibility.talkback;

import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech.FailoverTtsListener;

/**
 * Logger handles the Text-to-Speech events for logging. In general, the result will pass through to
 * {@link TalkBackAnalytics} and then encapsulate and upload target analytics.
 */
public class TtsLogger implements FailoverTtsListener {

  public TtsLogger(
      TalkBackAnalytics analytics, SpeechRateAndPitchActor.RateState speechRateState) {}

  @Override
  public void onTtsInitialized(boolean wasSwitchingEngines, String enginePackageName) {
    // Empty.
  }

  @Override
  public void onUtteranceStarted(String utteranceId, long delay) {
    // Empty.
  }

  @Override
  public void onUtteranceRangeStarted(String utteranceId, int start, int end) {
    // Empty.
  }

  @Override
  public void onUtteranceCompleted(String utteranceId, boolean success) {
    // Empty.
  }
}
