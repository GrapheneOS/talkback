/*
 * Copyright (C) 2025 The Android Open Source Project
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

import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.FILTER_TABLE_CELL;

import android.graphics.Rect;
import android.util.Pair;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionItemInfoCompat;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TargetType;
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.HashMap;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/** A helper class to navigate within a table. */
public final class TableNavigation {

  private static final String TAG = "TableNavigation";
  private static final int ATTEMPT_TIMES = 3;

  private NavigationAction navigationAction;
  private AccessibilityNodeInfoCompat tableRoot;
  private FocusFinder focusFinder;
  private EventId eventId;
  private Table table;

  /** Currently it only supports row and column navigation in native table. */
  public static boolean isNativeTableGranularity(@TargetType int type) {
    return (type == NavigationTarget.TARGET_COLUMN || type == NavigationTarget.TARGET_ROW);
  }

  public TableNavigation(
      @NonNull NavigationAction navigationAction,
      @NonNull AccessibilityNodeInfoCompat tableRoot,
      FocusFinder focusFinder,
      EventId eventId) {
    this.navigationAction = navigationAction;
    this.tableRoot = tableRoot;
    this.focusFinder = focusFinder;
    this.eventId = eventId;
    if (navigationAction.pivotIndex == null) {
      throw new IllegalArgumentException("navigationAction.pivotIndex should not be null.");
    }
    table = buildTableAttributes();
  }

  @Nullable
  private Table buildTableAttributes() {
    Rect pivotCellBounds = new Rect();
    AccessibilityNodeInfoCompat pivotCell =
        getCellFromTargetIndex(tableRoot, navigationAction.pivotIndex);
    if (pivotCell == null) {
      LogUtils.w(TAG, "buildTableAttributes but pivotCell is invalid.");
      return null;
    }
    pivotCell.getBoundsInScreen(pivotCellBounds);
    if (pivotCellBounds.isEmpty()) {
      LogUtils.w(TAG, "buildTableAttributes but pivotCellBounds is empty.");
      return null;
    }
    return new Table(tableRoot, navigationAction.pivotIndex, pivotCellBounds);
  }

  // Navigates to a table cell and returns the focusable node in the cell.
  public AccessibilityNodeInfoCompat navigateToTableCell() {
    return getFocusFromNearestIndex(ATTEMPT_TIMES);
  }

  // Returns the first focusable node from the pivot.
  @CanIgnoreReturnValue
  public AccessibilityNodeInfoCompat getFirstFocusableByDirection(
      AccessibilityNodeInfoCompat pivot, int direction) {
    TraversalStrategy traversalStrategy =
        TraversalStrategyUtils.getTraversalStrategy(pivot, focusFinder, direction);
    // Use TARGET_DEFAULT regardless of the current granularity.
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        NavigationTarget.createNodeFilter(
            NavigationTarget.TARGET_DEFAULT, traversalStrategy.getSpeakingNodesCache());
    return TraversalStrategyUtils.findFirstFocusInNodeTree(
        traversalStrategy, pivot, direction, nodeFilter);
  }

  /** Returns whether the table is eligible to scroll. */
  public boolean canScroll(int scrollAction) {
    Pair<Integer, Integer> targetPosition = getDefaultTargetIndex();
    return navigationAction.shouldScroll
        // Sets strict to true to avoid doing the unnecessary scroll.
        && !outOfBoundary(targetPosition, /* strict= */ true)
        && AccessibilityNodeInfoUtils.supportsAction(tableRoot, scrollAction);
  }

  /**
   * Returns the scroll direction based on the table attributes.
   *
   * @return the scroll direction. It is either {@link TraversalStrategy#SEARCH_FOCUS_FORWARD} or
   *     {@link TraversalStrategy#SEARCH_FOCUS_BACKWARD}.
   */
  @TraversalStrategy.SearchDirection
  public int getScrollDirection() {
    if (table == null) {
      LogUtils.w(TAG, "getScrollDirection but table is invalid. Return the original direction.");
      return navigationAction.searchDirection;
    }
    boolean reversed =
        (navigationAction.targetType == NavigationTarget.TARGET_ROW)
            ? table.isVerticalReversed
            : table.isHorizontalReversed;
    if (navigationAction.searchDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD) {
      return reversed
          ? TraversalStrategy.SEARCH_FOCUS_BACKWARD
          : TraversalStrategy.SEARCH_FOCUS_FORWARD;
    } else {
      return reversed
          ? TraversalStrategy.SEARCH_FOCUS_FORWARD
          : TraversalStrategy.SEARCH_FOCUS_BACKWARD;
    }
  }

  /** Returns the navigation action. */
  public NavigationAction getNavigationAction() {
    return navigationAction;
  }

  /** Returns the event id. */
  public EventId getEventId() {
    return eventId;
  }

  /** Returns the table root. */
  public AccessibilityNodeInfoCompat getTableRoot() {
    return tableRoot;
  }

  // Returns the default target position for navigation.
  private Pair<Integer, Integer> getDefaultTargetIndex() {
    return getTargetIndexIncrements(1);
  }

  /**
   * Returns whether the {@code targetPosition} is out of the table boundary.
   *
   * @param strict whether the boundary is strict. Sets to false to tolerate the wrong row count
   *     reported from the native GridView.
   */
  private boolean outOfBoundary(Pair<Integer, Integer> targetPosition, boolean strict) {
    if (targetPosition == null) {
      return true;
    }

    int rowCount = tableRoot.getCollectionInfo().getRowCount();
    int colCount = tableRoot.getCollectionInfo().getColumnCount();

    return targetPosition.first < 0
        || (strict ? targetPosition.first >= rowCount : targetPosition.first > rowCount)
        || targetPosition.second < 0
        || targetPosition.second >= colCount;
  }

  @Nullable
  private Pair<Integer, Integer> getTargetIndexIncrements(int increment) {
    int rowIndex = navigationAction.pivotIndex.first;
    int columnIndex = navigationAction.pivotIndex.second;
    if (navigationAction.targetType == NavigationTarget.TARGET_ROW) {
      return Pair.create(
          navigationAction.searchDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
              ? rowIndex + increment
              : rowIndex - increment,
          columnIndex);
    } else if (navigationAction.targetType == NavigationTarget.TARGET_COLUMN) {
      return Pair.create(
          rowIndex,
          navigationAction.searchDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
              ? columnIndex + increment
              : columnIndex - increment);
    }
    return null;
  }

  // Finds the first focusable node in the visible cell in the target position.
  @Nullable
  private AccessibilityNodeInfoCompat getFocusFromTargetIndex(
      Pair<Integer, Integer> targetPosition) {
    AccessibilityNodeInfoCompat cell = getCellFromTargetIndex(tableRoot, targetPosition);
    if (cell == null || !cell.isVisibleToUser()) {
      LogUtils.w(TAG, "getFocusFromTargetIndex but cell is invalid, cell=%s", cell);
      return null;
    }
    return getFirstFocusableByDirection(cell, TraversalStrategy.SEARCH_FOCUS_FORWARD);
  }

  // Finds the cell in the target position but exclude the nested cells.
  @Nullable
  public static AccessibilityNodeInfoCompat getCellFromTargetIndex(
      AccessibilityNodeInfoCompat tableRoot, Pair<Integer, Integer> targetPosition) {
    if (targetPosition == null) {
      return null;
    }
    return AccessibilityNodeInfoUtils.getMatchingDescendant(
        tableRoot,
        Filter.node(
            (node) -> {
              CollectionItemInfoCompat collectionItemInfo = node.getCollectionItemInfo();
              return collectionItemInfo != null
                  && collectionItemInfo.getRowIndex() == targetPosition.first
                  && collectionItemInfo.getColumnIndex() == targetPosition.second;
            }),
        Filter.node(
            (node) -> {
              return node.getCollectionItemInfo() != null;
            }));
  }

  /**
   * Returns all cells with CollectionItemInfo in the Table but skips the nested cells.
   *
   * <p>This method traverses the entire table, only calls it when necessary.
   */
  @Nullable
  private static List<AccessibilityNodeInfoCompat> getCellsFromTable(
      AccessibilityNodeInfoCompat tableRoot) {
    return AccessibilityNodeInfoUtils.getMatchingDescendantsNotNested(tableRoot, FILTER_TABLE_CELL);
  }

  /**
   * Returns the first focusable node in the visible cell in the target position.
   *
   * @param attemptTimes the maximum times to try to find the focus.
   *     <p>The method tries to find the focus from the nearest index first, then falls back to the
   *     approximate location.
   */
  private AccessibilityNodeInfoCompat getFocusFromNearestIndex(int attemptTimes) {
    AccessibilityNodeInfoCompat target = null;
    int i = 1;
    Pair<Integer, Integer> nearestIndex = getTargetIndexIncrements(i);
    // Sets strict to false to allow searching focus out of the row count because the row count
    // might be incorrect from native GridView. This should be fine to do one more search.
    while (i <= attemptTimes && !outOfBoundary(nearestIndex, /* strict= */ false)) {
      target = getFocusFromTargetIndex(nearestIndex);
      if (target == null) {
        target = getFocusFromApproximateLocation(i - 1);
      }
      if (target != null) {
        LogUtils.d(TAG, "getFocusFromNearestIndex and match focus in the attempt=%s.", i);
        break;
      }
      nearestIndex = getTargetIndexIncrements(i++);
    }
    return target;
  }

  // Finds the first focusable node in the visible cell in the approximate location.
  @Nullable
  private AccessibilityNodeInfoCompat getFocusFromApproximateLocation(int increment) {
    Pair<Integer, Integer> approximateIndex = getTargetIndexInApproximateLocation(increment);
    return getFocusFromTargetIndex(approximateIndex);
  }

  @Nullable
  private Pair<Integer, Integer> getTargetIndexInApproximateLocation(int increment) {
    if (table == null) {
      LogUtils.w(TAG, "getTargetIndexInApproximateLocation but table is invalid.");
      return null;
    }
    Pair<Integer, Integer> approximateLocation = null;
    if (navigationAction.targetType == NavigationTarget.TARGET_ROW) {
      approximateLocation =
          (navigationAction.searchDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
                  ^ table.isVerticalReversed)
              ? Pair.create(
                  table.pivotCellBounds.centerX(),
                  table.pivotCellBounds.bottom + table.avgHeight / 2 + increment * table.avgHeight)
              : Pair.create(
                  table.pivotCellBounds.centerX(),
                  table.pivotCellBounds.top - table.avgHeight / 2 - increment * table.avgHeight);
    } else if (navigationAction.targetType == NavigationTarget.TARGET_COLUMN) {
      approximateLocation =
          (navigationAction.searchDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
                  ^ table.isHorizontalReversed)
              ? Pair.create(
                  table.pivotCellBounds.right + table.avgWidth / 2 + increment * table.avgWidth,
                  table.pivotCellBounds.centerY())
              : Pair.create(
                  table.pivotCellBounds.left - table.avgWidth / 2 - increment * table.avgWidth,
                  table.pivotCellBounds.centerY());
    }
    if (approximateLocation == null) {
      return null;
    }
    for (Pair<Integer, Integer> key : table.cellBoundsMap.keySet()) {
      Rect rect = table.cellBoundsMap.get(key);
      if (rect.contains(approximateLocation.first, approximateLocation.second)) {
        LogUtils.d(TAG, "match focus from the approximate location key=%s.", key);
        return key;
      }
    }
    return null;
  }

  /**
   * A class calculating a table's approximate attributes.
   *
   * <p>The class calculates the average height and width of the table cells and determines whether
   * the table is reversed horizontally or vertically.
   */
  public static class Table {
    int avgHeight = 0;
    int avgWidth = 0;
    // Grids are typically in vertical and not reversed.
    boolean isVerticalReversed = false;
    boolean isHorizontalReversed = false;
    HashMap<Pair<Integer, Integer>, Rect> cellBoundsMap = new HashMap<>();
    Rect pivotCellBounds = new Rect();

    public Table(
        AccessibilityNodeInfoCompat tableRoot,
        Pair<Integer, Integer> pivotIndex,
        Rect pivotCellBounds) {
      this.pivotCellBounds = pivotCellBounds;
      calculateTableAttributes(tableRoot, pivotIndex);
    }

    private void calculateTableAttributes(
        AccessibilityNodeInfoCompat tableRoot, Pair<Integer, Integer> pivotIndex) {
      List<AccessibilityNodeInfoCompat> cells = getCellsFromTable(tableRoot);
      Rect tempRect = new Rect();
      for (AccessibilityNodeInfoCompat cell : cells) {
        CollectionItemInfoCompat collectionItemInfo = cell.getCollectionItemInfo();
        cell.getBoundsInScreen(tempRect);
        cellBoundsMap.put(
            Pair.create(collectionItemInfo.getRowIndex(), collectionItemInfo.getColumnIndex()),
            new Rect(tempRect));
        avgHeight += tempRect.height();
        avgWidth += tempRect.width();
        if (collectionItemInfo.getColumnIndex() < pivotIndex.second) {
          isHorizontalReversed = tempRect.left > pivotCellBounds.left;
        } else if (collectionItemInfo.getColumnIndex() > pivotIndex.second) {
          isHorizontalReversed = tempRect.left < pivotCellBounds.left;
        } else if (collectionItemInfo.getRowIndex() < pivotIndex.first) {
          isVerticalReversed = tempRect.top > pivotCellBounds.top;
        } else if (collectionItemInfo.getRowIndex() > pivotIndex.first) {
          isVerticalReversed = tempRect.top < pivotCellBounds.top;
        }
      }
      avgHeight /= cellBoundsMap.size();
      avgWidth /= cellBoundsMap.size();
      LogUtils.d(
          TAG,
          "Build table, pivotIndex=%s, pivotCellBounds=%s, avgHeight=%s, avgWidth=%s,"
              + " isVerticalReversed=%s, isHorizontalReversed=%s",
          pivotIndex,
          pivotCellBounds,
          avgHeight,
          avgWidth,
          isVerticalReversed,
          isHorizontalReversed);
    }
  }
}
