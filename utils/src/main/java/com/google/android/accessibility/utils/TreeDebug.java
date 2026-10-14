/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.google.android.accessibility.utils;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.text.ParcelableSpan;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import com.google.android.accessibility.utils.traversal.OrderedTraversalStrategy;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Util class to help debug Node trees. */
public class TreeDebug {

  public static final String TAG = "TreeDebug";
  private static final Logger defaultLogger =
      (format, args) -> LogUtils.v(TreeDebug.TAG, format, args);

  /** Logs the layout hierarchy of node trees for given list of windows. */
  public static void logNodeTrees(List<AccessibilityWindowInfo> windows) {
    logNodeTrees(windows, defaultLogger);
  }

  public static void logNodeTrees(
      List<AccessibilityWindowInfo> windows, @NonNull Logger treeDebugLogger) {
    if (windows == null || windows.isEmpty()) {
      return;
    }
    treeDebugLogger.log("------------Node tree------------");
    for (AccessibilityWindowInfo window : windows) {
      if (window == null) {
        continue;
      }
      // TODO: Filter and print useful window information.
      treeDebugLogger.log("Window: %s", window);
      AccessibilityNodeInfoCompat root =
          AccessibilityNodeInfoUtils.toCompat(AccessibilityWindowInfoUtils.getRoot(window));
      logNodeTree(root, treeDebugLogger);
    }
  }

  /** Logs the layout hierarchy of node tree for using the input node as the root. */
  public static void logNodeTree(@Nullable AccessibilityNodeInfoCompat node) {
    logNodeTree(node, defaultLogger);
  }

  public static void logNodeTree(
      @Nullable AccessibilityNodeInfoCompat node, @NonNull Logger treeDebugLogger) {
    if (node == null) {
      return;
    }

    HashSet<AccessibilityNodeInfoCompat> seen = new HashSet<>();
    logNodeTree(node, "", seen, treeDebugLogger);
  }

  private static void logNodeTree(
      AccessibilityNodeInfoCompat node,
      String indent,
      HashSet<AccessibilityNodeInfoCompat> seen,
      @NonNull Logger looger) {
    if (!seen.add(node)) {
      looger.log("Cycle: %d", node.hashCode());
      return;
    }

    // Include the hash code as a "poor man's" id, knowing that it
    // might not always be unique.
    looger.log("%s(%d)%s", indent, node.hashCode(), nodeDebugDescription(node));

    indent += "  ";
    int childCount = node.getChildCount();
    for (int i = 0; i < childCount; ++i) {
      AccessibilityNodeInfoCompat child = node.getChild(i);
      if (child == null) {
        looger.log("%sCouldn't get child %d", indent, i);
        continue;
      }

      logNodeTree(child, indent, seen, looger);
    }
  }

  private static void appendSimpleName(StringBuilder sb, CharSequence fullName) {
    int dotIndex = TextUtils.lastIndexOf(fullName, '.');
    if (dotIndex < 0) {
      dotIndex = 0;
    }

    sb.append(fullName, dotIndex, fullName.length());
  }

  /** Gets a description of the properties of a node. */
  public static CharSequence nodeDebugDescription(AccessibilityNodeInfoCompat node) {
    StringBuilder sb = new StringBuilder();
    sb.append(node.getWindowId());

    if (node.getClassName() != null) {
      appendSimpleName(sb, node.getClassName());
    } else {
      sb.append("unknownClassName");
    }

    String uniqueId = node.getUniqueId();
    if (uniqueId != null) {
      sb.append(":uniqueId(");
      sb.append(uniqueId);
      sb.append(")");
    }

    if (AccessibilityNodeInfoUtils.hasRequestInitialAccessibilityFocus(node)) {
      sb.append(":hasRequestInitialAccessibilityFocus");
    }

    // TODO: Will fix when the G3 tool chain supports the
    // AccessibilityNodeInfoCompat#getMinMillisBetweenContentChanges API.
    long refreshTime = AccessibilityNodeInfoUtils.getMinDurationBetweenContentChangesMillis(node);
    // node.getMinMillisBetweenContentChanges();
    if (refreshTime != 0) {
      sb.append(":rate-update(");
      sb.append(refreshTime);
      sb.append(")");
    }

    if (!node.isVisibleToUser()) {
      sb.append(":invisible");
    }

    Rect rect = new Rect();
    node.getBoundsInScreen(rect);
    sb.append(":");
    sb.append("(")
        .append(rect.left)
        .append(", ")
        .append(rect.top)
        .append(" - ")
        .append(rect.right)
        .append(", ")
        .append(rect.bottom)
        .append(")");

    if (!TextUtils.isEmpty(node.getPaneTitle())) {
      sb.append(":PANE{");
      sb.append(node.getPaneTitle());
      sb.append("}");
    }

    @Nullable CharSequence nodeText = AccessibilityNodeInfoUtils.getText(node);
    @Nullable String textWithSpansInfo =
        SpannableUtils.spansToStringForLogging(nodeText, ParcelableSpan.class);
    if (nodeText != null) {
      sb.append(":TEXT{");
      sb.append(nodeText.toString().trim());
      if (!TextUtils.isEmpty(textWithSpansInfo)) {
        sb.append(textWithSpansInfo);
      }
      sb.append("}");
    }

    if (node.getContentDescription() != null) {
      sb.append(":CONTENT{");
      sb.append(node.getContentDescription().toString().trim());
      sb.append("}");
    }

    if (!TextUtils.isEmpty(node.getRoleDescription())) {
      sb.append(":ROLE{");
      sb.append(node.getRoleDescription().toString().trim());
      sb.append("}");
    }

    if (AccessibilityNodeInfoUtils.getState(node) != null) {
      sb.append(":STATE{");
      sb.append(AccessibilityNodeInfoUtils.getState(node).toString().trim());
      sb.append("}");
    }

    sb.append(":GRANULARITY{");
    sb.append(AccessibilityNodeInfoUtils.getMovementGranularity(node));
    sb.append("}");

    // Views that inherit Checkable can have its own state description and the log already covered
    // by above SD, but for some views that are not Checkable but have checked status, like
    // overriding by AccessibilityDelegate, we should also log it.
    if (node.isCheckable()) {
      sb.append(":");
      if (node.isChecked()) {
        sb.append("checked");
      } else {
        sb.append("not checked");
      }
    }

    List<AccessibilityActionCompat> actions = node.getActionList();
    if (!actions.isEmpty()) {
      sb.append("(action:");
      if (actions.contains(AccessibilityActionCompat.ACTION_FOCUS)) {
        sb.append("FOCUS/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS)) {
        sb.append("A11Y_FOCUS/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_CLEAR_ACCESSIBILITY_FOCUS)) {
        sb.append("CLEAR_A11Y_FOCUS/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_BACKWARD)) {
        sb.append("SCROLL_BACKWARD/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_FORWARD)) {
        sb.append("SCROLL_FORWARD/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_CLICK)) {
        sb.append("CLICK/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_LONG_CLICK)) {
        sb.append("LONG_CLICK/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_EXPAND)) {
        sb.append("EXPAND/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_COLLAPSE)) {
        sb.append("COLLAPSE/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_UP)) {
        sb.append("SCROLL_UP/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_DOWN)) {
        sb.append("SCROLL_DOWN/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_LEFT)) {
        sb.append("SCROLL_LEFT/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_RIGHT)) {
        sb.append("SCROLL_RIGHT/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_TO_POSITION)) {
        sb.append("SCROLL_TO_POSITION/");
      }
      if (actions.contains(AccessibilityActionCompat.ACTION_SCROLL_IN_DIRECTION)) {
        sb.append("SCROLL_IN_DIRECTION/");
      }
      sb.setLength(sb.length() - 1);
      sb.append(")");
    }

    boolean hasCustomAction = false;
    StringBuilder customActionSb = new StringBuilder();
    customActionSb.append("(custom action:");
    for (AccessibilityActionCompat action : actions) {
      CharSequence label = action.getLabel();
      if (label != null) {
        customActionSb.append("LABEL:").append(label).append("/");
        hasCustomAction = true;
      }
    }
    customActionSb.setLength(customActionSb.length() - 1);
    customActionSb.append(")");
    if (hasCustomAction) {
      sb.append(customActionSb);
    }

    if (node.isFocusable()) {
      sb.append(":focusable");
    }
    if (node.isScreenReaderFocusable()) {
      sb.append(":screenReaderfocusable");
    }

    if (node.isFocused()) {
      sb.append(":focused");
    }

    if (node.isSelected()) {
      sb.append(":selected");
    }

    if (node.isScrollable()) {
      sb.append(":scrollable");
    }

    if (node.isClickable()) {
      sb.append(":clickable");
    }

    if (node.isLongClickable()) {
      sb.append(":longClickable");
    }

    if (node.isAccessibilityFocused()) {
      sb.append(":accessibilityFocused");
    }
    if (AccessibilityNodeInfoUtils.supportsTextLocation(node)) {
      sb.append(":supportsTextLocation");
    }
    if (!node.isEnabled()) {
      sb.append(":disabled");
    }

    if (node.isEditable()) {
      sb.append(":editable");
    }

    if (AccessibilityNodeInfoUtils.isTextSelectable(node)) {
      sb.append(":textSelectable");
    }

    if (node.isFieldRequired()) {
      sb.append(":fieldRequired");
    }

    int liveRegion = node.getLiveRegion();
    if (liveRegion == ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE) {
      sb.append(":politeLiveRegion");
    } else if (liveRegion == ViewCompat.ACCESSIBILITY_LIVE_REGION_ASSERTIVE) {
      sb.append(":assertiveLiveRegion");
    }

    if (node.getCollectionInfo() != null) {
      sb.append(":collection");
      sb.append("#ROWS=");
      sb.append(node.getCollectionInfo().getRowCount());
      sb.append("#COLS=");
      sb.append(node.getCollectionInfo().getColumnCount());
    }

    if (AccessibilityNodeInfoUtils.isHeading(node)) {
      sb.append(":heading");
    } else if (node.getCollectionItemInfo() != null) {
      sb.append(":item");
    }
    if (node.getCollectionItemInfo() != null) {
      sb.append("#rowIndex=");
      sb.append(node.getCollectionItemInfo().getRowIndex());
      sb.append(",colIndex=");
      sb.append(node.getCollectionItemInfo().getColumnIndex());
      if (FeatureSupport.supportGridTitle()) {
        if (node.unwrap() != null && node.unwrap().getCollectionItemInfo() != null) {
          CollectionItemInfo itemInfo = node.unwrap().getCollectionItemInfo();
          if (itemInfo.getRowTitle() != null) {
            sb.append(",RowTitle=");
            sb.append(itemInfo.getRowTitle());
          }
          if (itemInfo.getColumnTitle() != null) {
            sb.append(",ColumnTitle=");
            sb.append(itemInfo.getColumnTitle());
          }
        }
      }
    }
    if (node.getRangeInfo() != null) {
      sb.append(":range");
      sb.append("#max=");
      sb.append(node.getRangeInfo().getMax());
      sb.append(",min=");
      sb.append(node.getRangeInfo().getMin());
      sb.append(",current=");
      sb.append(node.getRangeInfo().getCurrent());
      sb.append(",type=");
      sb.append(node.getRangeInfo().getType());
    }

    if (AccessibilityNodeInfoUtils.getViewIdText(node) != null) {
      sb.append(":resourceId=");
      sb.append(AccessibilityNodeInfoUtils.getViewIdText(node));
    }

    return sb.toString();
  }

  /** Logs the traversal order of node trees for given list of windows. */
  public static void logOrderedTraversalTree(
      List<AccessibilityWindowInfo> windows,
      Logger logger,
      @Nullable List<AccessibilityNodeInfoCompat> nodeCollector) {
    logger.log("------------Node tree traversal order----------");
    for (AccessibilityWindowInfo window : windows) {
      logger.log("Window: %s", window);
      AccessibilityNodeInfoCompat root =
          AccessibilityNodeInfoUtils.toCompat(AccessibilityWindowInfoUtils.getRoot(window));
      if (root != null) {
        OrderedTraversalStrategy orderTraversalStrategy = new OrderedTraversalStrategy(root);
        List<AccessibilityNodeInfoCompat> nodes = orderTraversalStrategy.dumpTree();
        for (AccessibilityNodeInfoCompat node : nodes) {
          if (nodeCollector != null) {
            if (AccessibilityNodeInfoUtils.shouldFocusNode(node)) {
              nodeCollector.add(node);
            }
          }
          logger.log(
              " (%d)%s%s",
              node.hashCode(),
              TreeDebug.nodeDebugDescription(node),
              getCustomizedTraversalNodeString(node));
        }
      }
    }
  }

  /**
   * Returns the string contains the attribute {@link AccessibilityNodeInfo#getTraversalBefore()}
   * and {@link AccessibilityNodeInfo#getTraversalAfter()} of the target node.
   */
  private static String getCustomizedTraversalNodeString(AccessibilityNodeInfoCompat node) {
    StringBuilder builder = new StringBuilder();
    AccessibilityNodeInfoCompat beforeNode = node.getTraversalBefore();
    AccessibilityNodeInfoCompat afterNode = node.getTraversalAfter();
    if (beforeNode != null) {
      builder.append(" before:").append(beforeNode.hashCode());
    }
    if (afterNode != null) {
      builder.append(" after:").append(afterNode.hashCode());
    }
    return builder.toString();
  }

  /**
   * Logs the layout hierarchy of node trees and the traversal order of node tree of all the
   * displays.
   *
   * @param service The parent service
   * @param focusIndicatorSpec Focus indicator info, in case a screenshot is desired
   */
  public static void logNodeTreesOnAllDisplaysAndWriteScreenshot(
      AccessibilityService service, TreeDebugRenderer.FocusIndicatorSpec focusIndicatorSpec) {
    List<AccessibilityNodeInfoCompat> nodesForScreenshot = new ArrayList<>();
    logNodeTreesOnAllDisplays(service, defaultLogger, nodesForScreenshot);
    if (focusIndicatorSpec != null) {
      if (FeatureSupport.canTakeScreenShotByAccessibilityService()) {
        new TreeDebugRenderer(service, nodesForScreenshot, focusIndicatorSpec)
            .startRenderOperation();
      }
    }
  }

  /**
   * Logs the layout hierarchy of node trees and the traversal order of node tree of all the
   * displays.
   *
   * @param service The parent service
   * @param treeDebugLogger The functional interface for logging
   */
  public static void logNodeTreesOnAllDisplays(
      AccessibilityService service, Logger treeDebugLogger) {
    logNodeTreesOnAllDisplays(service, treeDebugLogger, null);
  }

  private static void logNodeTreesOnAllDisplays(
      AccessibilityService service,
      Logger logger,
      List<AccessibilityNodeInfoCompat> nodesForScreenshot) {
    AccessibilityServiceCompatUtils.forEachWindowInfoListOnAllDisplays(
        service,
        windowInfoList -> {
          TreeDebug.logNodeTrees(windowInfoList, logger);
          TreeDebug.logOrderedTraversalTree(windowInfoList, logger, nodesForScreenshot);
        });
  }
}
