/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.google.android.accessibility.utils.gestures;

import android.content.Context;
import android.os.Build;
import android.view.MotionEvent;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider;

/**
 * This class matches gestures of the form multi-tap and hold. The number of taps for each instance
 * is specified in the constructor.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class MultiTapAndHold extends MultiTap {
  private static final String TAG = "MultiTapAndHold";

  public MultiTapAndHold(
      Context context,
      int taps,
      int gesture,
      GestureMatcher.StateChangeListener listener,
      GestureConfigProvider configProvider,
      GestureMatcher.AnalyticsEventLogger logger) {
    super(context, taps, gesture, listener, configProvider, logger);
  }

  @Override
  protected void onDown(EventId eventId, MotionEvent event) {
    super.onDown(eventId, event);
    if (currentTaps + 1 == targetTaps && getState() != STATE_GESTURE_CANCELED) {
      // We should check the detector state in advance because it may enter Cancel state in base
      // class (MultiTap).
      completeAfterLongPressTimeout(eventId, event);
    }
  }

  @Override
  protected void onUp(EventId eventId, MotionEvent event) {
    if (!isValidUpEvent(event)) {
      debugMotionEvent(TAG, "onUp/!isValidUpEvent. Gesture:%d", getGestureId());
      cancelGesture(event);
      return;
    }
    if (getState() == STATE_GESTURE_STARTED || getState() == STATE_CLEAR) {
      currentTaps++;
      if (currentTaps == targetTaps) {
        debugMotionEvent(TAG, "onUp/Not a HOLD gesture. Gesture:%d", getGestureId());
        cancelGesture(event);
        return;
      }
      // Needs more taps.
    } else {
      debugMotionEvent(TAG, "onUp/State mismatch. Gesture:%d", getGestureId());
      // Either too many taps or nonsensical event stream.
      cancelGesture(event);
      return;
    }
    cancelAfterDoubleTapTimeout(event);
  }

  @Override
  public String getGestureName() {
    return switch (targetTaps) {
      case 2 -> "Double Tap and Hold";
      case 3 -> "Triple Tap and Hold";
      default -> Integer.toString(targetTaps) + " Taps and Hold";
    };
  }
}
