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

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.R;
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider;

/**
 * This class is a pseudo gesture matcher which can early determine to enter the Touch Explore
 * state. To help entering Touch Explore state earlier, it has to predict that all gesture detector
 * would no longer possible to match with additional MotionEvent. For 1-finger case, when the delta
 * time of action_down & action_up is over the TapTimeout value, we can determine all gesture
 * detectors are fail. To report the faked gesture complete event would cancel the all detectors and
 * inform TouchInteractionMonitor to directly transit to Touch Explore state.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class TapToTouchExplore extends GestureMatcher {
  private static final String TAG = "TapToTouchExplore";
  // The acceptable distance the pointer can move and still count as a tap.
  int touchSlop;
  int tapTimeout;
  float baseX;
  float baseY;
  long lastDownTime;

  public TapToTouchExplore(
      Context context,
      int gesture,
      GestureMatcher.StateChangeListener listener,
      GestureConfigProvider configProvider,
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
    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    int deltaTapTimeout = context.getResources().getInteger(R.integer.config_tap_timeout_delta);
    tapTimeout = ViewConfiguration.getTapTimeout() + deltaTapTimeout;
  }

  @Override
  public void clear() {
    baseX = Float.NaN;
    baseY = Float.NaN;
    lastDownTime = Long.MAX_VALUE;
    super.clear();
  }

  @Override
  protected void onDown(EventId eventId, MotionEvent event) {
    baseX = event.getX();
    baseY = event.getY();
    lastDownTime = event.getEventTime();
  }

  @Override
  protected void onUp(EventId eventId, MotionEvent event) {
    if (isOutsideSlop(event, touchSlop, /* isTouchSlop= */ true)) {
      debugMotionEvent(TAG, "onUp/isOutsideSlop. Gesture:%d", getGestureId());
      cancelGesture(event);
      return;
    }
    if (isInvalidUpEvent(event)) {
      debugMotionEvent(TAG, "onUp/isInvalidUpEvent. Gesture:%d", getGestureId());
      completeGesture(eventId, event);
      return;
    }
    cancelGesture(event);
  }

  @Override
  protected void onMove(EventId eventId, MotionEvent event) {
    if (isOutsideSlop(event, touchSlop, /* isTouchSlop= */ true)) {
      cancelGesture(event);
      debugMotionEvent(TAG, "onMove/isOutsideSlop. Gesture:%d", getGestureId());
    }
  }

  @Override
  protected void onPointerDown(EventId eventId, MotionEvent event) {
    cancelGesture(event);
    debugMotionEvent(TAG, "onPointerDown. Gesture:%d", getGestureId());
  }

  @Override
  protected void onPointerUp(EventId eventId, MotionEvent event) {
    cancelGesture(event);
    debugMotionEvent(TAG, "onPointerUp. Gesture:%d", getGestureId());
  }

  @Override
  public String getGestureName() {
    return "Touch Explore";
  }

  protected boolean isOutsideSlop(MotionEvent event, int slop, boolean isTouchSlop) {
    final float deltaX = baseX - event.getX();
    final float deltaY = baseY - event.getY();
    if (deltaX == 0 && deltaY == 0) {
      return false;
    }
    final double moveDelta = Math.hypot(deltaX, deltaY);
    return moveDelta > slop;
  }

  protected boolean isInvalidUpEvent(MotionEvent upEvent) {
    long time = upEvent.getEventTime();
    long timeDelta = time - lastDownTime;
    if (timeDelta > tapTimeout) {
      debugMotionEvent(TAG, "isInvalidUpEvent/tapTimeout's over. Gesture:%d", getGestureId());
      return true;
    }
    return false;
  }

  @Override
  public String toString() {
    return super.toString() + "mBaseX: " + baseX + ", mBaseY: " + baseY;
  }
}
