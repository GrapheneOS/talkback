/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.android.accessibility.utils;

import android.content.Context;
import android.os.Bundle;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirectionOrUnknown;
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utility class for sending commands to Chrome. */
public class WebInterfaceUtils {

  private static final String KEY_WEB_IMAGE = "AccessibilityNodeInfo.hasImage";
  private static final String VALUE_HAS_WEB_IMAGE = "true";

  private static final String ACTION_ARGUMENT_HTML_ELEMENT_STRING_VALUES =
      "ACTION_ARGUMENT_HTML_ELEMENT_STRING_VALUES";

  /** Direction constant for forward movement within a page. */
  public static final int DIRECTION_FORWARD = 1;

  /** Direction constant for backward movement within a page. */
  public static final int DIRECTION_BACKWARD = -1;

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page section.
   */
  public static final String HTML_ELEMENT_MOVE_BY_SECTION = "SECTION";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page heading.
   */
  public static final String HTML_ELEMENT_MOVE_BY_HEADING = "HEADING";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page section.
   */
  public static final String HTML_ELEMENT_MOVE_BY_LANDMARK = "LANDMARK";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous link.
   */
  public static final String HTML_ELEMENT_MOVE_BY_LINK = "LINK";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous list.
   */
  public static final String HTML_ELEMENT_MOVE_BY_LIST = "LIST";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous control.
   */
  public static final String HTML_ELEMENT_MOVE_BY_CONTROL = "CONTROL";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous button.
   */
  public static final String HTML_ELEMENT_MOVE_BY_BUTTON = "BUTTON";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous checkbox.
   */
  public static final String HTML_ELEMENT_MOVE_BY_CHECKBOX = "CHECKBOX";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous radio.
   */
  public static final String HTML_ELEMENT_MOVE_BY_RADIO = "RADIO";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous edit field.
   */
  public static final String HTML_ELEMENT_MOVE_BY_EDIT_FIELD = "TEXT_FIELD";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous focusable item.
   */
  public static final String HTML_ELEMENT_MOVE_BY_FOCUSABLE_ITEM = "FOCUSABLE";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page heading 1.
   */
  public static final String HTML_ELEMENT_MOVE_BY_HEADING_1 = "H1";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page heading 2.
   */
  public static final String HTML_ELEMENT_MOVE_BY_HEADING_2 = "H2";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page heading 3.
   */
  public static final String HTML_ELEMENT_MOVE_BY_HEADING_3 = "H3";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page heading 4.
   */
  public static final String HTML_ELEMENT_MOVE_BY_HEADING_4 = "H4";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page heading 5.
   */
  public static final String HTML_ELEMENT_MOVE_BY_HEADING_5 = "H5";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous page heading 6.
   */
  public static final String HTML_ELEMENT_MOVE_BY_HEADING_6 = "H6";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous image.
   */
  public static final String HTML_ELEMENT_MOVE_BY_GRAPHIC = "GRAPHIC";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous list item.
   */
  public static final String HTML_ELEMENT_MOVE_BY_LIST_ITEM = "LIST_ITEM";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous table.
   */
  public static final String HTML_ELEMENT_MOVE_BY_TABLE = "TABLE";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous combo box.
   */
  public static final String HTML_ELEMENT_MOVE_BY_COMBOBOX = "COMBOBOX";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous visited link.
   */
  public static final String HTML_ELEMENT_MOVE_BY_VISITED_LINK = "VISITED_LINK";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous unvisited link.
   */
  public static final String HTML_ELEMENT_MOVE_BY_UNVISITED_LINK = "UNVISITED_LINK";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous column.
   */
  public static final String HTML_ELEMENT_MOVE_BY_COLUMN = "COLUMN";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous row.
   */
  public static final String HTML_ELEMENT_MOVE_BY_ROW = "ROW";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous column bounds.
   */
  public static final String HTML_ELEMENT_MOVE_BY_COLUMN_BOUNDS = "COLUMN_BOUNDS";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous row bounds.
   */
  public static final String HTML_ELEMENT_MOVE_BY_ROW_BOUNDS = "ROW_BOUNDS";

  /**
   * HTML element argument to use with {@link AccessibilityNodeInfoCompat#performAction()} to
   * instruct Chrome to move to the next or previous table bounds.
   */
  public static final String HTML_ELEMENT_MOVE_BY_TABLE_BOUNDS = "TABLE_BOUNDS";

  private static final ImmutableMap<String, ImmutableList<String>> URL_BAR_IDS =
      ImmutableMap.ofEntries(
          Map.entry("com.android.chrome", ImmutableList.of("com.android.chrome:id/url_bar")),
          Map.entry("com.chrome.beta", ImmutableList.of("com.chrome.beta:id/url_bar")),
          Map.entry("com.chrome.dev", ImmutableList.of("com.chrome.dev:id/url_bar")),
          Map.entry(
              "org.mozilla.firefox",
              ImmutableList.of(
                  "org.mozilla.firefox:id/url",
                  "org.mozilla.firefox:id/url_bar_title",
                  "org.mozilla.firefox:id/url_edit_text",
                  "org.mozilla.firefox:id/mozac_browser_toolbar_url_view",
                  "org.mozilla.firefox:id/mozac_browser_toolbar_edit_url_view")),
          Map.entry(
              "org.mozilla.firefox_beta",
              ImmutableList.of(
                  "org.mozilla.firefox_beta:id/url",
                  "org.mozilla.firefox_beta:id/url_bar_title",
                  "org.mozilla.firefox_beta:id/url_edit_text",
                  "org.mozilla.firefox_beta:id/mozac_browser_toolbar_url_view",
                  "org.mozilla.firefox_beta:id/mozac_browser_toolbar_edit_url_view")),
          Map.entry(
              "com.sec.android.app.sbrowser",
              ImmutableList.of("com.sec.android.app.sbrowser:id/location_bar_edit_text")),
          Map.entry("com.android.browser", ImmutableList.of("com.android.browser:id/url")),
          Map.entry("com.opera.android", ImmutableList.of("com.opera.android:id/url_field")),
          Map.entry("com.opera.browser", ImmutableList.of("com.opera.browser:id/url_field")),
          Map.entry(
              "com.hsv.freeadblockerbrowser", ImmutableList.of("com.opera.browser:id/url_field")),
          Map.entry("com.microsoft.emmx", ImmutableList.of("com.microsoft.emmx:id/url_bar")));

  /**
   * Filter for WebView container node. See {@link
   * #ascendToWebViewContainer(AccessibilityNodeInfoCompat)}.
   */
  private static final Filter<AccessibilityNodeInfoCompat> FILTER_WEB_VIEW_CONTAINER =
      new Filter<AccessibilityNodeInfoCompat>() {
        @Override
        public boolean accept(AccessibilityNodeInfoCompat node) {
          if (node == null) {
            return false;
          }

          return Role.getRole(node) == Role.ROLE_WEB_VIEW
              && Role.getRole(node.getParent()) != Role.ROLE_WEB_VIEW;
        }
      };

  /** Filter for WebView node. See {@link #ascendToWebView(AccessibilityNodeInfoCompat)}. */
  private static final Filter<AccessibilityNodeInfoCompat> FILTER_WEB_VIEW =
      new Filter<AccessibilityNodeInfoCompat>() {
        @Override
        public boolean accept(AccessibilityNodeInfoCompat node) {
          return node != null && Role.getRole(node) == Role.ROLE_WEB_VIEW;
        }
      };

  public static int searchDirectionToWebNavigationDirection(
      Context context, @SearchDirectionOrUnknown int searchDirection) {
    if (searchDirection == TraversalStrategy.SEARCH_FOCUS_UNKNOWN) {
      return 0;
    }
    @SearchDirectionOrUnknown
    int logicalDirection =
        TraversalStrategyUtils.getLogicalDirection(
            searchDirection, WindowUtils.isScreenLayoutRTL(context));
    return logicalDirection == TraversalStrategy.SEARCH_FOCUS_FORWARD
        ? WebInterfaceUtils.DIRECTION_FORWARD
        : WebInterfaceUtils.DIRECTION_BACKWARD;
  }

  /**
   * Gets supported html elements, such as HEADING, LANDMARK, LINK and LIST, by
   * AccessibilityNodeInfoCompat.
   *
   * @param node The node containing supported html elements
   * @return supported html elements
   */
  public static String @Nullable [] getSupportedHtmlElements(
      @Nullable AccessibilityNodeInfoCompat node) {
    SupportedHtmlNodeCollector supportedHtmlNodeCollector = new SupportedHtmlNodeCollector();
    AccessibilityNodeInfoUtils.isOrHasMatchingAncestor(node, supportedHtmlNodeCollector);
    if ((supportedHtmlNodeCollector.getSupportedTypes() == null)
        || supportedHtmlNodeCollector.getSupportedTypes().isEmpty()) {
      return null;
    }
    return supportedHtmlNodeCollector.getSupportedTypes().toArray(new String[] {});
  }

  private static class SupportedHtmlNodeCollector extends Filter<AccessibilityNodeInfoCompat> {
    private final ArrayList<String> supportedTypes = new ArrayList<>();

    @Override
    public boolean accept(AccessibilityNodeInfoCompat node) {
      if (node == null) {
        return false;
      }
      Bundle bundle = node.getExtras();
      CharSequence supportedHtmlElements =
          bundle.getCharSequence(ACTION_ARGUMENT_HTML_ELEMENT_STRING_VALUES);

      if (supportedHtmlElements != null) {
        Collections.addAll(supportedTypes, supportedHtmlElements.toString().split(","));
        return true;
      }
      return false;
    }

    public ArrayList<String> getSupportedTypes() {
      return supportedTypes;
    }
  }

  /**
   * Returns the WebView container node if the {@code node} is a web element. <strong>Note:</strong>
   * A web content node tree is always constructed with a WebView root node, a second level WebView
   * node, and all other nodes attached beneath the second level WebView node. When referring to the
   * WebView container, we prefer the root node instead of the second level node, because attributes
   * like isVisibleToUser() sometimes are not correctly exposed at second level WebView node.
   */
  public static @Nullable AccessibilityNodeInfoCompat ascendToWebViewContainer(
      AccessibilityNodeInfoCompat node) {
    if (!WebInterfaceUtils.supportsWebActions(node)) {
      return null;
    }
    return AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(node, FILTER_WEB_VIEW_CONTAINER);
  }

  /** Returns the closest ancestor(inclusive) WebView node if the {@code node} is a web element. */
  public static @Nullable AccessibilityNodeInfoCompat ascendToWebView(
      AccessibilityNodeInfoCompat node) {
    if (!WebInterfaceUtils.supportsWebActions(node)) {
      return null;
    }
    return AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(node, FILTER_WEB_VIEW);
  }

  /**
   * Determines whether or not the given node contains web content.
   *
   * @param node The node to evaluate
   * @return {@code true} if the node contains web content, {@code false} otherwise
   */
  public static boolean supportsWebActions(@Nullable AccessibilityNodeInfoCompat node) {
    return AccessibilityNodeInfoUtils.supportsAnyAction(
        node,
        AccessibilityNodeInfoCompat.ACTION_NEXT_HTML_ELEMENT,
        AccessibilityNodeInfoCompat.ACTION_PREVIOUS_HTML_ELEMENT);
  }

  /**
   * Determines whether or not the given node contains native web content (and not Chrome).
   *
   * @param node The node to evaluate
   * @return {@code true} if the node contains native web content, {@code false} otherwise
   */
  public static boolean hasNativeWebContent(@Nullable AccessibilityNodeInfoCompat node) {
    return supportsWebActions(node);
  }

  /**
   * Returns whether the given node has navigable web content, either legacy (Chrome) or native web
   * content.
   *
   * @param node The node to check for web content.
   * @return Whether the given node has navigable web content.
   */
  public static boolean hasNavigableWebContent(@Nullable AccessibilityNodeInfoCompat node) {
    return supportsWebActions(node);
  }

  /** Check if node is web container */
  public static boolean isWebContainer(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    return hasNativeWebContent(node) || isNodeFromFirefox(node);
  }

  /** Returns {@code true} if the {@code node} or its descendant contains image. */
  public static boolean containsImage(AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }
    Bundle extras = node.getExtras();
    return (extras != null) && VALUE_HAS_WEB_IMAGE.equals(extras.getString(KEY_WEB_IMAGE));
  }

  public static @Nullable AccessibilityNodeInfoCompat findUrlBar(AccessibilityNodeInfoCompat root) {
    return AccessibilityNodeInfoUtils.searchFromBfs(
        root,
        new Filter<AccessibilityNodeInfoCompat>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            return URL_BAR_IDS
                .getOrDefault(node.getPackageName().toString(), ImmutableList.of())
                .contains(node.getViewIdResourceName());
          }
        });
  }

  private static boolean isNodeFromFirefox(AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }

    final String packageName =
        node.getPackageName() != null ? node.getPackageName().toString() : "";
    return packageName.startsWith("org.mozilla.");
  }
}
