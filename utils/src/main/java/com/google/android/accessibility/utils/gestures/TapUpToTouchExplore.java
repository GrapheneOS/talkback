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

package com.google.android.accessibility.utils.gestures;

import static android.util.Log.VERBOSE;
import static com.google.android.accessibility.utils.gestures.GestureUtils.MM_PER_CM;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.MotionEvent;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.R;

/**
 * This class is a pseudo gesture matcher which can early determine to enter the Touch Explore
 * state. To help entering Touch Explore state earlier, it has to predict that all gesture detector
 * would no longer possible to match with additional MotionEvent. For 1-finger case, when the 1st
 * action_down is held for more than the time the swipe gestures expect the finger should move, itʼs
 * no conflict to directly enter the touch explore state.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class TapUpToTouchExplore extends GestureMatcher {
  private static final String TAG = "TapUpToTouchExplore";
  private float baseX;
  private float baseY;
  private long firstDownTime;
  // This is the calculated movement threshold used track if the user is still
  // moving their finger.
  private float gestureDetectionThresholdPixels;
  // Time threshold in millisecond to determine if an interaction is a gesture or not.
  private int maxStartThreshold;

  TapUpToTouchExplore(
      Context context,
      int gesture,
      GestureMatcher.StateChangeListener listener,
      GestureMatcher.AnalyticsEventLogger logger) {
    super(gesture, new Handler(context.getMainLooper()), listener, logger);
    initializeViewConfigurationParameters(context);
    clear();
  }

  @Override
  public void onConfigurationChanged(Context context) {
    initializeViewConfigurationParameters(context);
  }

  private void initializeViewConfigurationParameters(Context context) {
    float gestureConfirmDistanceCm =
        context.getResources().getFloat(R.dimen.config_gesture_confirm_distance_cm);
    DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
    gestureDetectionThresholdPixels =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_MM, MM_PER_CM, displayMetrics)
            * gestureConfirmDistanceCm;
    int maxTimeToStartSwipeMsPerCm =
        context.getResources().getInteger(R.integer.config_max_time_to_start_swipe_ms_per_cm);
    maxStartThreshold = (int) (maxTimeToStartSwipeMsPerCm * gestureConfirmDistanceCm);
  }

  @Override
  public void clear() {
    baseX = Float.NaN;
    baseY = Float.NaN;
    firstDownTime = Long.MAX_VALUE;
    super.clear();
  }

  @Override
  protected void onDown(EventId eventId, MotionEvent event) {
    baseX = event.getX();
    baseY = event.getY();
    firstDownTime = event.getEventTime();
  }

  @Override
  protected void onMove(EventId eventId, MotionEvent event) {
    if (event.getPointerCount() > 1) {
      return;
    }
    final float x = event.getX();
    final float y = event.getY();
    final double moveDelta = Math.hypot(Math.abs(x - baseX), Math.abs(y - baseY));
    if (moveDelta > gestureDetectionThresholdPixels) {
      // No need to monitor to Touch Explore
      cancelGesture(event);
      return;
    }
    final long timeDelta = event.getEventTime() - firstDownTime;
    if (timeDelta > maxStartThreshold) {
      debugMotionEvent(TAG, "onMove/timeDelta is over. Gesture:%d", getGestureId());
      completeGesture(eventId, event);
      return;
    }
  }

  @Override
  protected void onPointerDown(EventId eventId, MotionEvent event) {
    cancelGesture(event);
  }

  @Override
  protected void onPointerUp(EventId eventId, MotionEvent event) {
    cancelGesture(event);
  }

  @Override
  protected void onUp(EventId eventId, MotionEvent event) {
    gestureMotionEventLog(VERBOSE, "onUp");
    cancelGesture(event);
  }

  @Override
  public String getGestureName() {
    return "TapUpToTouchExplore";
  }

  @Override
  public String toString() {
    return super.toString() + "BaseX: " + baseX + ", BaseY: " + baseY;
  }
}
