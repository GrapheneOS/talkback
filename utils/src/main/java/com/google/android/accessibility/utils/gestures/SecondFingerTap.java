/*
 * Copyright (C) 2022 The Android Open Source Project
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

import static android.util.Log.VERBOSE;

import android.content.Context;
import android.graphics.PointF;
import android.os.Build;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.ViewConfiguration;
import androidx.annotation.RequiresApi;
import com.google.android.accessibility.utils.Performance.EventId;

/**
 * This class matches second-finger multi-tap gestures. A second-finger multi-tap gesture is where
 * one finger is held down and a second finger executes the taps. The number of taps for each
 * instance is specified in the constructor.
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class SecondFingerTap extends GestureMatcher {
  private static final int TARGET_FINGER_COUNT = 2;
  protected final int targetTaps;
  private final int doubleTapTimeout;
  protected int currentTaps;
  private final int touchSlop;
  private long firstDownTime;
  // Store initial down points for slop checking and update when next down if is inside slop.
  private final PointF[] bases;
  protected boolean pendingRestart;

  SecondFingerTap(
      Context context,
      int taps,
      int gesture,
      GestureMatcher.StateChangeListener listener,
      GestureMatcher.AnalyticsEventLogger logger) {
    super(gesture, new Handler(context.getMainLooper()), listener, logger);
    targetTaps = taps;
    doubleTapTimeout = ViewConfiguration.getDoubleTapTimeout();
    touchSlop = ViewConfiguration.get(context).getScaledTouchSlop() * TARGET_FINGER_COUNT;
    bases = new PointF[TARGET_FINGER_COUNT];
    for (int i = 0; i < TARGET_FINGER_COUNT; i++) {
      bases[i] = new PointF();
    }
    clear();
  }

  @Override
  public void clear() {
    currentTaps = 0;
    firstDownTime = Long.MAX_VALUE;
    pendingRestart = false;
    super.clear();
  }

  // Instead of clear the detector, this method restore the state variables to detect the next tap
  // event.
  @Override
  public void restart(boolean pending) {
    super.restart(pending);
    pendingRestart = pending;
    currentTaps = 0;
  }

  @Override
  public boolean bypassCancelByTapUpToTouchExplore() {
    return true;
  }

  @Override
  protected void onDown(EventId eventId, MotionEvent event) {
    firstDownTime = event.getEventTime();
  }

  @Override
  protected void onPointerDown(EventId eventId, MotionEvent event) {
    long timeDelta = event.getEventTime() - firstDownTime;
    if (timeDelta < doubleTapTimeout) {
      cancelGesture(event);
      return;
    }

    if (event.getPointerCount() > TARGET_FINGER_COUNT) {
      gestureMotionEventLog(VERBOSE, "onPointerDown/getPointerCount=%d", event.getPointerCount());
      cancelGesture(event);
      return;
    }
    bases[0].x = event.getX(0);
    bases[0].y = event.getY(0);
    bases[1].x = event.getX(1);
    bases[1].y = event.getY(1);
  }

  @Override
  protected void onPointerUp(EventId eventId, MotionEvent event) {
    gestureMotionEventLog(VERBOSE, "onPointerUp");
    if (event.getPointerId(event.getActionIndex()) != 1) {
      // Invalid finger of split-tap
      gestureMotionEventLog(VERBOSE, "Invalid finger of split-tap");
      cancelGesture(event);
      return;
    }
    if (event.getPointerCount() > TARGET_FINGER_COUNT) {
      gestureMotionEventLog(VERBOSE, "onPointerUp/getPointerCount=%d", event.getPointerCount());
      cancelGesture(event);
      return;
    }
    if (!validatePositions(event)) {
      gestureMotionEventLog(VERBOSE, "onPointerUp/validatePositions=false");
      cancelGesture(event);
      return;
    }
    if (pendingRestart) {
      pendingRestart = false;
      return;
    }
    if (getState() == STATE_GESTURE_STARTED || getState() == STATE_CLEAR) {
      currentTaps++;
      gestureMotionEventLog(VERBOSE, "onPointerUp/getState=%d", getState());
      if (currentTaps == targetTaps) {
        gestureMotionEventLog(VERBOSE, "onPointerUp/currentTaps=%d", currentTaps);
        // Done.
        completeGesture(eventId, event);
        restart(false);
        startGesture(event);
      }
    } else {
      gestureMotionEventLog(VERBOSE, "onPointerUp/currentTaps=%d", currentTaps);
      // Nonsensical event stream.
      cancelGesture(event);
    }
  }

  @Override
  protected void onUp(EventId eventId, MotionEvent event) {
    gestureMotionEventLog(VERBOSE, "onUp");
    // Cancel early when possible, or it will take precedence over two-finger double tap.
    cancelGesture(event);
  }

  /**
   * Ensures the touched points when performing split-tap are not moving too far (within the
   * touch-slop).
   *
   * @param event the latest MotionEvent received.
   * @return {@code true} if successful, {@code false} otherwise.
   */
  private boolean validatePositions(MotionEvent event) {
    int eventCount = event.getPointerCount();
    if (eventCount != TARGET_FINGER_COUNT) {
      return true;
    }
    for (int index = 0; index < eventCount; index++) {
      PointF base = bases[index];
      final float dX = base.x - event.getX(index);
      final float dY = base.y - event.getY(index);
      final float delta = (float) Math.hypot(dX, dY);
      if (delta > touchSlop) {
        return false;
      }
    }
    return true;
  }

  @Override
  public String getGestureName() {
    return switch (targetTaps) {
      case 1 -> "Second Finger Tap";
      default -> "Second Finger " + targetTaps + " Taps";
    };
  }

  @Override
  public String toString() {
    return super.toString() + ", Taps:" + currentTaps + ", Bases:" + bases[0] + "," + bases[1];
  }
}
