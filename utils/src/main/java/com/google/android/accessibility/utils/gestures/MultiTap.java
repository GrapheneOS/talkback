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

import static com.google.android.accessibility.utils.gestures.GestureAnalyticsEvent.EVENT_DOUBLE_TAP_SLOP_OVER_RANGE;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.R;
import com.google.android.accessibility.utils.gestures.GestureManifold.GestureConfigProvider;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * This class matches multi-tap gestures. The number of taps for each instance is specified in the
 * constructor.
 *
 * @hide
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class MultiTap extends GestureMatcher {
  private static final String TAG = "MultiTap";
  final int targetTaps;
  private final GestureConfigProvider configProvider;
  // The acceptable distance between two taps
  int doubleTapSlop;
  // The acceptable distance the pointer can move and still count as a tap.
  int touchSlop;
  int tapTimeout;
  int doubleTapTimeout;
  int currentTaps;
  float baseX;
  float baseY;
  long lastDownTime;
  long lastUpTime;

  public MultiTap(
      Context context,
      int taps,
      int gesture,
      GestureMatcher.StateChangeListener listener,
      GestureConfigProvider configProvider,
      GestureMatcher.AnalyticsEventLogger logger) {
    super(gesture, new Handler(context.getMainLooper()), listener, logger);
    this.configProvider = configProvider;
    targetTaps = taps;
    initializeViewConfigurationParameters(context);
    clear();
  }

  @Override
  public void onConfigurationChanged(Context context) {
    initializeViewConfigurationParameters(context);
  }

  private void initializeViewConfigurationParameters(Context context) {
    doubleTapSlop =
        (int)
            (ViewConfiguration.get(context).getScaledDoubleTapSlop()
                * configProvider.getDoubleTapSlopMultiplier());
    LogUtils.v(TAG, "Double-Tap slop is: %d", doubleTapSlop);
    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    int deltaTapTimeout = context.getResources().getInteger(R.integer.config_tap_timeout_delta);
    tapTimeout = ViewConfiguration.getTapTimeout() + deltaTapTimeout;
    doubleTapTimeout = GestureConfiguration.DOUBLE_TAP_TIMEOUT_MS;
  }

  @Override
  public void clear() {
    currentTaps = 0;
    baseX = Float.NaN;
    baseY = Float.NaN;
    lastDownTime = Long.MAX_VALUE;
    lastUpTime = Long.MAX_VALUE;
    super.clear();
  }

  @Override
  protected void onDown(EventId eventId, MotionEvent event) {
    long time = event.getEventTime();
    long timeDelta = time - lastUpTime;
    if (timeDelta > doubleTapTimeout) {
      debugMotionEvent(TAG, "onDown/doubleTapTimeout's over. Gesture:%d", getGestureId());
      cancelGesture(event);
      return;
    }
    lastDownTime = time;
    if (Float.isNaN(baseX) && Float.isNaN(baseY)) {
      baseX = event.getX();
      baseY = event.getY();
    }
    if (!isInsideSlop(event, doubleTapSlop, /* isTouchSlop= */ false)) {
      debugMotionEvent(TAG, "onDown/doubleTapSlop's over. Gesture:%d", getGestureId());
      cancelGesture(event);
      return;
    }
    baseX = event.getX();
    baseY = event.getY();
    if (currentTaps + 1 == targetTaps) {
      // Start gesture detecting on down of final tap.
      // Note that if this instance is matching double tap,
      // and the service is not requesting to handle double tap, GestureManifold will
      // ignore this.
      startGesture(event);
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
        // Done.
        completeGesture(eventId, event);
      } else {
        processGesture(eventId, event);
      }
      // Needs more taps.
    } else {
      // Either too many taps or nonsensical event stream.
      cancelGesture(event);
      debugMotionEvent(TAG, "onUp/Too many taps. Gesture:%d", getGestureId());
    }
  }

  @Override
  protected void onMove(EventId eventId, MotionEvent event) {
    if (!isInsideSlop(event, touchSlop, /* isTouchSlop= */ true)) {
      cancelGesture(event);
      debugMotionEvent(TAG, "onMove/!isInsideSlop. Gesture:%d", getGestureId());
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
    return switch (targetTaps) {
      case 2 -> "Double Tap";
      case 3 -> "Triple Tap";
      default -> Integer.toString(targetTaps) + " Taps";
    };
  }

  /**
   * This class helps to collect data (double-tap slop over), in addition to the fundamental Gesture
   * analytic event.
   */
  public static class MultiTapAnalyticsEvent extends GestureAnalyticsEvent {
    public int doubleTapSlopOverRange;

    MultiTapAnalyticsEvent(int event, int gestureId) {
      super(event, gestureId);
    }

    @CanIgnoreReturnValue
    MultiTapAnalyticsEvent setDoubleTapSlopOverRange(int doubleTapSlopOverRange) {
      this.doubleTapSlopOverRange = doubleTapSlopOverRange;
      return this;
    }
  }

  private void logExceededSlop(double deviation) {
    int extraData;

    if (deviation < 0.1) {
      extraData = MultiTapAnalyticsEvent.EXTRA_DEBUG_DOUBLE_TAP_SLOP_OVER_IN_10_PERCENT;
    } else if (deviation < 0.2) {
      extraData = MultiTapAnalyticsEvent.EXTRA_DEBUG_DOUBLE_TAP_SLOP_OVER_IN_20_PERCENT;
    } else if (deviation < 0.5) {
      extraData = MultiTapAnalyticsEvent.EXTRA_DEBUG_DOUBLE_TAP_SLOP_OVER_IN_50_PERCENT;
    } else if (deviation < 1.0) {
      extraData = MultiTapAnalyticsEvent.EXTRA_DEBUG_DOUBLE_TAP_SLOP_OVER_IN_100_PERCENT;
    } else {
      extraData = MultiTapAnalyticsEvent.EXTRA_DEBUG_DOUBLE_TAP_SLOP_OVER_MORE_THAN_100_PERCENT;
    }
    debugMotionEvent(TAG, "logExceededSlop. Gesture:%d, range:%d", getGestureId(), extraData);
    MultiTapAnalyticsEvent event =
        new MultiTapAnalyticsEvent(EVENT_DOUBLE_TAP_SLOP_OVER_RANGE, getGestureId())
            .setDoubleTapSlopOverRange(extraData);
    analyticsEvent(event);
  }

  private boolean isInsideSlop(MotionEvent event, int slop, boolean isTouchSlop) {
    final float deltaX = baseX - event.getX();
    final float deltaY = baseY - event.getY();
    if (deltaX == 0 && deltaY == 0) {
      return true;
    }
    final double moveDelta = Math.hypot(deltaX, deltaY);
    if (!isTouchSlop && moveDelta > slop) {
      logExceededSlop((moveDelta - slop) / slop);
    }
    return moveDelta <= slop;
  }

  protected boolean isValidUpEvent(MotionEvent upEvent) {
    long time = upEvent.getEventTime();
    long timeDelta = time - lastDownTime;
    if (timeDelta > tapTimeout) {
      debugMotionEvent(TAG, "isValidUpEvent/tapTimeout's over. Gesture:%d", getGestureId());
      return false;
    }
    lastUpTime = time;
    if (!isInsideSlop(upEvent, touchSlop, /* isTouchSlop= */ true)) {
      debugMotionEvent(TAG, "isValidUpEvent/!isInsideSlop. Gesture:%d", getGestureId());
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return super.toString()
        + ", Taps:"
        + currentTaps
        + ", mBaseX: "
        + Float.toString(baseX)
        + ", mBaseY: "
        + Float.toString(baseY);
  }
}
