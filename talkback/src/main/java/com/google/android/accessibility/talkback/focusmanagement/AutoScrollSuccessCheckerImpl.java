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
package com.google.android.accessibility.talkback.focusmanagement;

import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build.VERSION_CODES;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.RequiresApi;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.TreeDebug;
import com.google.android.accessibility.utils.output.ScrollActionRecord.AutoScrollSuccessChecker;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Objects;

/**
 * Determines whether auto-scroll success for the default granularity. We assume the single item
 * won't exceed 50% of the scrollable node so we set {@code autoScrollSuccessThreshold} 0.6f so that
 * the next item is focusable. In the future, we may adjust this threshold based on largest item
 * size in the scrollable view
 */
@RequiresApi(VERSION_CODES.P)
public class AutoScrollSuccessCheckerImpl implements AutoScrollSuccessChecker {

  private static final String TAG = "AutoScrollSuccessCheckerImpl";
  // This is determined by the minimum target size, 48dp and hdpi resolution.
  public static final int REACH_EDGE_MIN_TOLERANCE_DISTANCE_PX = 72;

  private final int scrollAction;
  private final Point threshold;

  private long totalScrollDeltaX = 0;
  private long totalScrollDeltaY = 0;

  public AutoScrollSuccessCheckerImpl(
      int scrollAction, Rect scrollableNodeBounds, float autoScrollSuccessThreshold) {
    this.scrollAction = scrollAction;
    threshold =
        new Point(
            (int) (scrollableNodeBounds.width() * autoScrollSuccessThreshold),
            (int) (scrollableNodeBounds.height() * autoScrollSuccessThreshold));
  }

  /**
   * Determines if the auto scroll is successful.
   *
   * <p>The auto scroll is considered successful if the scrollable node is scrolled to the edge or
   * the total scroll delta exceeds the threshold.
   */
  @Override
  public boolean isAutoScrollSuccess(
      AccessibilityNodeInfoCompat scrolledNodeInRecord, AccessibilityEvent scrolledEvent) {
    if (scrolledEvent.getSource() == null) {
      return false;
    }

    AccessibilityNodeInfoCompat scrolledNode =
        AccessibilityNodeInfoCompat.wrap(scrolledEvent.getSource());
    if (!scrolledNode.equals(scrolledNodeInRecord)) {
      return false;
    }
    LogUtils.d(
        TAG,
        "isAutoScrollSuccess - record.getScrolledNode() = %s , event node = %s",
        TreeDebug.nodeDebugDescription(scrolledNodeInRecord),
        TreeDebug.nodeDebugDescription(scrolledNode));

    long scrollDeltaX = AccessibilityEventUtils.getScrollDeltaX(scrolledEvent);
    long scrollDeltaY = AccessibilityEventUtils.getScrollDeltaY(scrolledEvent);
    if (reachEdge(scrollDeltaX, scrollDeltaY, scrolledEvent)) {
      return true;
    }

    totalScrollDeltaX += scrollDeltaX;
    totalScrollDeltaY += scrollDeltaY;

    LogUtils.d(
        TAG,
        "isAutoScrollSuccess - threshold = %s, totalScrollDeltaX = %s, totalScrollDeltaY = %s."
            + " ",
        threshold,
        totalScrollDeltaX,
        totalScrollDeltaY);
    if (Math.abs(totalScrollDeltaX) < threshold.x && Math.abs(totalScrollDeltaY) < threshold.y) {
      return false;
    }
    if (scrollAction == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) {
      if ((totalScrollDeltaX > 0 || totalScrollDeltaY > 0)) {
        return true;
      }
    } else if (scrollAction == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
      if ((scrollDeltaX < 0 || scrollDeltaY < 0)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines if the scrollable node is scrolled to the edge.
   *
   * <p>The scrollable node is considered scrolled to the edge if the scroll delta between the edge
   * and the scroll position is less than {@link #REACH_EDGE_MIN_TOLERANCE_DISTANCE_PX}.
   */
  private boolean reachEdge(
      long scrollDeltaX, long scrollDeltaY, AccessibilityEvent scrolledEvent) {
    int maxScrollX = scrolledEvent.getMaxScrollX();
    int maxScrollY = scrolledEvent.getMaxScrollY();
    int scrollX = scrolledEvent.getScrollX();
    int scrollY = scrolledEvent.getScrollY();

    LogUtils.d(
        TAG,
        "reachEdge - deltaX=%s, deltaY=%s, scrollX=%s, scrollY=%s, maxScrollX = %s, "
            + "maxScrollY = %s.",
        scrollDeltaX,
        scrollDeltaY,
        scrollX,
        scrollY,
        maxScrollX,
        maxScrollY);
    if (maxScrollX == 0 && maxScrollY == 0) {
      LogUtils.w(TAG, "invalid maxScroll");
      return false;
    }
    if (scrollAction == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD) {
      if (scrollDeltaY > 0) {
        return maxScrollY - scrollY < REACH_EDGE_MIN_TOLERANCE_DISTANCE_PX;
      }
      if (scrollDeltaX > 0) {
        return maxScrollX - scrollX < REACH_EDGE_MIN_TOLERANCE_DISTANCE_PX;
      }
    }
    if (scrollAction == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD) {
      if (scrollDeltaY < 0) {
        return scrollY < REACH_EDGE_MIN_TOLERANCE_DISTANCE_PX;
      }
      if (scrollDeltaX < 0) {
        return scrollX < REACH_EDGE_MIN_TOLERANCE_DISTANCE_PX;
      }
    }
    return false;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AutoScrollSuccessCheckerImpl)) {
      return false;
    }
    AutoScrollSuccessCheckerImpl that = (AutoScrollSuccessCheckerImpl) o;
    return scrollAction == that.scrollAction
        && Objects.equals(threshold, that.threshold)
        && totalScrollDeltaX == that.totalScrollDeltaX
        && totalScrollDeltaY == that.totalScrollDeltaY;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scrollAction, threshold, totalScrollDeltaX, totalScrollDeltaY);
  }
}
