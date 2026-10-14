/*
 * Copyright (C) 2019 The Android Open Source Project
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

import android.app.Application;
import android.os.SystemClock;
import androidx.annotation.NonNull;
import com.google.android.accessibility.talkback.logging.LatencyExtension;
import com.google.android.accessibility.talkback.logging.LatencyExtensionWriter;

/** Initialize and configures Primes to collect performance metrics. */
public class PrimesController implements LatencyExtensionWriter {

  /** Timer for measuring latency. */
  public enum TimerAction {
    START_UP,
    GESTURE_EVENT,
    KEY_EVENT,
    DPAD_NAVIGATION,
    TTS_DELAY,
    INITIAL_FOCUS_RESTORE,
    INITIAL_FOCUS_FOLLOW_INPUT,
    INITIAL_FOCUS_FIRST_CONTENT,
    IMAGE_CAPTION_OCR_SUCCEED,
    IMAGE_CAPTION_OCR_FAILED,
    IMAGE_CAPTION_ICON_LABEL_SUCCEED,
    IMAGE_CAPTION_ICON_LABEL_FAILED,
    IMAGE_CAPTION_IMAGE_DESCRIPTION_SUCCEED,
    IMAGE_CAPTION_IMAGE_DESCRIPTION_FAILED,
    IMAGE_CAPTION_IMAGE_PROCESS_BLOCK_OVERLAY,
    GEMINI_RESPONSE_LATENCY,
    // This is only logged when the duration between the two requests is below the framework delay.
    LATENCY_BETWEEN_SCREENSHOT_CAPTURE_REQUEST,

    EVENT_BASED_HEARING_FEEDBACK,
    EVENT_BASED_PERFORMING_ACTION,
    GESTURE_EVENT_LATENCY,
    TOUCH_CONTROLLER_STATE_CHANGE_LATENCY,
    GEMINI_ON_DEVICE_RESPONSE_LATENCY,
    END_TO_END_LATENCY,
    TOUCH_EXPLORE_DELAY_TYPING_100,
    TOUCH_EXPLORE_DELAY_TYPING_150,
    TOUCH_EXPLORE_DELAY_TYPING_200,
    TOUCH_EXPLORE_DELAY_TYPING_250,
    TOUCH_EXPLORE_DELAY_100,
    TOUCH_EXPLORE_DELAY_150,
    TOUCH_EXPLORE_DELAY_200,
    TOUCH_EXPLORE_DELAY_250
  }

  public void initialize(Application app) {}

  public void startTimer(TimerAction timerAction) {}

  @Override
  public void startTimer(TimerAction timerAction, String id) {}

  public void stopTimer(TimerAction timerAction) {}

  @Override
  public void stopTimer(TimerAction timerAction, String id, LatencyExtension latencyExtension) {}

  public void recordDuration(TimerAction timerAction, long startMs, long endMs) {}

  public void recordDuration(TimerAction timerAction, long duration) {}

  public long getTime() {
    return SystemClock.uptimeMillis();
  }

  @Override
  public void recordDuration(
      @NonNull TimerAction timerAction,
      long startMs,
      long endMs,
      LatencyExtension latencyExtension) {}
}
