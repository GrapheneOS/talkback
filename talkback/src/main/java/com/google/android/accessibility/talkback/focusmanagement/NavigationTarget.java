/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static com.google.android.accessibility.utils.input.CursorGranularity.COLUMN;
import static com.google.android.accessibility.utils.input.CursorGranularity.CONTAINER;
import static com.google.android.accessibility.utils.input.CursorGranularity.CONTROL;
import static com.google.android.accessibility.utils.input.CursorGranularity.DEFAULT;
import static com.google.android.accessibility.utils.input.CursorGranularity.HEADING;
import static com.google.android.accessibility.utils.input.CursorGranularity.LINK;
import static com.google.android.accessibility.utils.input.CursorGranularity.ROW;
import static com.google.android.accessibility.utils.input.CursorGranularity.SEARCH;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_BUTTON;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_CHECKBOX;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_COMBOBOX;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_EDITFIELD;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_FOCUSABLE;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_GRAPHIC;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_H1;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_H2;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_H3;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_H4;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_H5;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_H6;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_LANDMARK;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_LIST;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_LISTITEM;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_RADIO;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_TABLE;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_UNVISITED_LINK;
import static com.google.android.accessibility.utils.input.CursorGranularity.WEB_VISITED_LINK;
import static com.google.android.accessibility.utils.input.CursorGranularity.WINDOWS;

import android.content.Context;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.input.CursorGranularity;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Map;

/**
 * Defines different types of target nodes for navigation action. Provides some utils towards {@link
 * TargetType}.
 */
public final class NavigationTarget {

  private static final String TAG = "NavigationTarget";

  private NavigationTarget() {}

  private static final int MASK_TARGET_WEB_GRANULARITY_ELEMENT = 1 << 16;
  private static final int MASK_TARGET_NATIVE_AND_WEB_GRANULARITY_ELEMENT = 1 << 18;
  public static final int TARGET_DEFAULT = 0;

  // Targets for native and web shared granularity.
  public static final int TARGET_HEADING = MASK_TARGET_NATIVE_AND_WEB_GRANULARITY_ELEMENT + 1;
  public static final int TARGET_CONTROL = MASK_TARGET_NATIVE_AND_WEB_GRANULARITY_ELEMENT + 2;
  public static final int TARGET_LINK = MASK_TARGET_NATIVE_AND_WEB_GRANULARITY_ELEMENT + 3;
  public static final int TARGET_ROW = MASK_TARGET_NATIVE_AND_WEB_GRANULARITY_ELEMENT + 4;
  public static final int TARGET_COLUMN = MASK_TARGET_NATIVE_AND_WEB_GRANULARITY_ELEMENT + 5;

  public static final int TARGET_CONTAINER = 200;
  public static final int TARGET_WINDOW = 201;
  public static final int TARGET_SEARCH = 203;

  // Targets for web-only granularity navigation.
  public static final int TARGET_HTML_ELEMENT_LIST = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 105;
  public static final int TARGET_HTML_ELEMENT_BUTTON = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 106;
  public static final int TARGET_HTML_ELEMENT_CHECKBOX = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 107;
  public static final int TARGET_HTML_ELEMENT_ARIA_LANDMARK =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 108;
  public static final int TARGET_HTML_ELEMENT_EDIT_FIELD =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 109;
  public static final int TARGET_HTML_ELEMENT_FOCUSABLE_ITEM =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 110;
  public static final int TARGET_HTML_ELEMENT_HEADING_1 = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 111;
  public static final int TARGET_HTML_ELEMENT_HEADING_2 = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 112;
  public static final int TARGET_HTML_ELEMENT_HEADING_3 = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 113;
  public static final int TARGET_HTML_ELEMENT_HEADING_4 = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 114;
  public static final int TARGET_HTML_ELEMENT_HEADING_5 = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 115;
  public static final int TARGET_HTML_ELEMENT_HEADING_6 = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 116;
  public static final int TARGET_HTML_ELEMENT_GRAPHIC = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 117;
  public static final int TARGET_HTML_ELEMENT_LIST_ITEM = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 118;
  public static final int TARGET_HTML_ELEMENT_TABLE = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 119;
  public static final int TARGET_HTML_ELEMENT_COMBOBOX = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 120;
  public static final int TARGET_HTML_ELEMENT_VISITED_LINK =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 121;
  public static final int TARGET_HTML_ELEMENT_UNVISITED_LINK =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 122;
  public static final int TARGET_HTML_ELEMENT_COLUMN_BOUNDS =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 124;
  public static final int TARGET_HTML_ELEMENT_ROW_BOUNDS =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 125;
  public static final int TARGET_HTML_ELEMENT_TABLE_BOUNDS =
      MASK_TARGET_WEB_GRANULARITY_ELEMENT + 126;
  public static final int TARGET_HTML_ELEMENT_RADIO = MASK_TARGET_WEB_GRANULARITY_ELEMENT + 127;

  /** navigation target types. */
  @IntDef({
    TARGET_DEFAULT,
    TARGET_HEADING,
    TARGET_CONTROL,
    TARGET_LINK,
    TARGET_HTML_ELEMENT_LIST,
    TARGET_HTML_ELEMENT_BUTTON,
    TARGET_HTML_ELEMENT_CHECKBOX,
    TARGET_HTML_ELEMENT_RADIO,
    TARGET_HTML_ELEMENT_ARIA_LANDMARK,
    TARGET_HTML_ELEMENT_EDIT_FIELD,
    TARGET_HTML_ELEMENT_FOCUSABLE_ITEM,
    TARGET_HTML_ELEMENT_HEADING_1,
    TARGET_HTML_ELEMENT_HEADING_2,
    TARGET_HTML_ELEMENT_HEADING_3,
    TARGET_HTML_ELEMENT_HEADING_4,
    TARGET_HTML_ELEMENT_HEADING_5,
    TARGET_HTML_ELEMENT_HEADING_6,
    TARGET_HTML_ELEMENT_GRAPHIC,
    TARGET_HTML_ELEMENT_LIST_ITEM,
    TARGET_HTML_ELEMENT_TABLE,
    TARGET_HTML_ELEMENT_COMBOBOX,
    TARGET_HTML_ELEMENT_VISITED_LINK,
    TARGET_HTML_ELEMENT_UNVISITED_LINK,
    TARGET_HTML_ELEMENT_COLUMN_BOUNDS,
    TARGET_HTML_ELEMENT_ROW_BOUNDS,
    TARGET_HTML_ELEMENT_TABLE_BOUNDS,
    TARGET_COLUMN,
    TARGET_ROW,
    TARGET_CONTAINER,
    TARGET_WINDOW,
    TARGET_SEARCH,
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface TargetType {}

  /**
   * Returns whether the target supports web navigation, including the web-only targets and
   * native-web-shared targets.
   */
  public static boolean supportsWebNavigation(@TargetType int type) {
    return isWebOnlyTarget(type) || isNativeAndWebSharedTarget(type);
  }

  /** Returns whether the target is native and web shared. */
  private static boolean isNativeAndWebSharedTarget(@TargetType int type) {
    return ((type & MASK_TARGET_NATIVE_AND_WEB_GRANULARITY_ELEMENT) != 0);
  }

  /** Returns whether the target is web only. */
  public static boolean isWebOnlyTarget(@TargetType int type) {
    return ((type & MASK_TARGET_WEB_GRANULARITY_ELEMENT) != 0);
  }

  /** Returns whether the target is macro granularity. */
  public static boolean isMacroGranularity(@TargetType int type) {
    return type == TARGET_HEADING
        || type == TARGET_CONTROL
        || type == TARGET_LINK
        || type == TARGET_CONTAINER;
  }

  /** Returns whether the target supports table navigation. */
  public static boolean supportsTableNavigation(@TargetType int type) {
    return (type == NavigationTarget.TARGET_COLUMN
        || type == NavigationTarget.TARGET_ROW
        || type == NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS
        || type == NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS
        || type == NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS);
  }

  /** Gets display name of HTML {@link TargetType}. Used to compose speaking feedback. */
  public static String htmlTargetToDisplayName(Context context, @TargetType int type) {
    return switch (type) {
      case TARGET_DEFAULT -> context.getString(R.string.granularity_default);
      case TARGET_LINK -> context.getString(R.string.display_name_link);
      case TARGET_HTML_ELEMENT_LIST -> context.getString(R.string.display_name_list);
      case TARGET_CONTROL -> context.getString(R.string.display_name_control);
      case TARGET_HEADING -> context.getString(R.string.display_name_heading);
      case TARGET_HTML_ELEMENT_BUTTON -> context.getString(R.string.display_name_button);
      case TARGET_HTML_ELEMENT_CHECKBOX -> context.getString(R.string.display_name_checkbox);
      case TARGET_HTML_ELEMENT_RADIO -> context.getString(R.string.display_name_radio);
      case TARGET_HTML_ELEMENT_ARIA_LANDMARK ->
          context.getString(R.string.display_name_aria_landmark);
      case TARGET_HTML_ELEMENT_EDIT_FIELD -> context.getString(R.string.display_name_edit_field);
      case TARGET_HTML_ELEMENT_FOCUSABLE_ITEM ->
          context.getString(R.string.display_name_focusable_item);
      case TARGET_HTML_ELEMENT_HEADING_1 -> context.getString(R.string.display_name_heading_1);
      case TARGET_HTML_ELEMENT_HEADING_2 -> context.getString(R.string.display_name_heading_2);
      case TARGET_HTML_ELEMENT_HEADING_3 -> context.getString(R.string.display_name_heading_3);
      case TARGET_HTML_ELEMENT_HEADING_4 -> context.getString(R.string.display_name_heading_4);
      case TARGET_HTML_ELEMENT_HEADING_5 -> context.getString(R.string.display_name_heading_5);
      case TARGET_HTML_ELEMENT_HEADING_6 -> context.getString(R.string.display_name_heading_6);
      case TARGET_HTML_ELEMENT_GRAPHIC -> context.getString(R.string.display_name_graphic);
      case TARGET_HTML_ELEMENT_LIST_ITEM -> context.getString(R.string.display_name_list_item);
      case TARGET_HTML_ELEMENT_TABLE -> context.getString(R.string.display_name_table);
      case TARGET_HTML_ELEMENT_COMBOBOX -> context.getString(R.string.display_name_combobox);
      case TARGET_HTML_ELEMENT_VISITED_LINK ->
          context.getString(R.string.display_name_visited_link);
      case TARGET_HTML_ELEMENT_UNVISITED_LINK ->
          context.getString(R.string.display_name_unvisited_link);
      case TARGET_COLUMN -> context.getString(R.string.display_name_column);
      case TARGET_ROW -> context.getString(R.string.display_name_row);
      case TARGET_HTML_ELEMENT_COLUMN_BOUNDS ->
          context.getString(R.string.display_name_column_bounds);
      case TARGET_HTML_ELEMENT_ROW_BOUNDS -> context.getString(R.string.display_name_row_bounds);
      case TARGET_HTML_ELEMENT_TABLE_BOUNDS ->
          context.getString(R.string.display_name_table_bounds);
      default -> {
        LogUtils.e(TAG, "htmlTargetToDisplayName() unhandled target type=" + type);
        yield "(unknown)";
      }
    };
  }

  /** Gets display name of Native {@link TargetType}. Used to compose speaking feedback. */
  @SuppressWarnings("SwitchIntDef") // Only some values handled.
  public static String nativeTargetToDisplayName(Context context, @TargetType int type) {
    return switch (type) {
      case TARGET_HEADING -> context.getString(R.string.display_name_heading);
      case TARGET_CONTROL -> context.getString(R.string.display_name_control);
      case TARGET_LINK -> context.getString(R.string.display_name_link);
      case TARGET_CONTAINER -> context.getString(R.string.display_name_container);
      case TARGET_WINDOW -> context.getString(R.string.display_name_window);
      case TARGET_SEARCH -> context.getString(R.string.display_name_search);
      case TARGET_ROW -> context.getString(R.string.display_name_row);
      case TARGET_COLUMN -> context.getString(R.string.display_name_column);
      default -> {
        LogUtils.e(TAG, "nativeTargetToDisplayName() unhandled target type=" + type);
        yield "";
      }
    };
  }

  /**
   * Gets HTML element name of {@link TargetType}. Used as parameter to perform html navigation
   * action.
   */
  @SuppressWarnings("SwitchIntDef") // Only some values handled.
  @Nullable
  public static String targetTypeToHtmlElement(@TargetType int targetType) {
    return switch (targetType) {
      case TARGET_LINK -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_LINK;
      case TARGET_HTML_ELEMENT_LIST -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_LIST;
      case TARGET_CONTROL -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_CONTROL;
      case TARGET_HEADING -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING;
      case TARGET_HTML_ELEMENT_BUTTON -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_BUTTON;
      case TARGET_HTML_ELEMENT_CHECKBOX -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_CHECKBOX;
      case TARGET_HTML_ELEMENT_RADIO -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_RADIO;
      case TARGET_HTML_ELEMENT_ARIA_LANDMARK -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_LANDMARK;
      case TARGET_HTML_ELEMENT_EDIT_FIELD -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_EDIT_FIELD;
      case TARGET_HTML_ELEMENT_FOCUSABLE_ITEM ->
          WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_FOCUSABLE_ITEM;
      case TARGET_HTML_ELEMENT_HEADING_1 -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_1;
      case TARGET_HTML_ELEMENT_HEADING_2 -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_2;
      case TARGET_HTML_ELEMENT_HEADING_3 -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_3;
      case TARGET_HTML_ELEMENT_HEADING_4 -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_4;
      case TARGET_HTML_ELEMENT_HEADING_5 -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_5;
      case TARGET_HTML_ELEMENT_HEADING_6 -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_6;
      case TARGET_HTML_ELEMENT_GRAPHIC -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_GRAPHIC;
      case TARGET_HTML_ELEMENT_LIST_ITEM -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_LIST_ITEM;
      case TARGET_HTML_ELEMENT_TABLE -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_TABLE;
      case TARGET_HTML_ELEMENT_COMBOBOX -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_COMBOBOX;
      case TARGET_HTML_ELEMENT_VISITED_LINK -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_VISITED_LINK;
      case TARGET_HTML_ELEMENT_UNVISITED_LINK ->
          WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_UNVISITED_LINK;
      case TARGET_COLUMN -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_COLUMN;
      case TARGET_ROW -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_ROW;
      case TARGET_HTML_ELEMENT_COLUMN_BOUNDS ->
          WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_COLUMN_BOUNDS;
      case TARGET_HTML_ELEMENT_ROW_BOUNDS -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_ROW_BOUNDS;
      case TARGET_HTML_ELEMENT_TABLE_BOUNDS -> WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_TABLE_BOUNDS;
      case TARGET_DEFAULT -> "";
      default -> null;
    };
  }

  /** Gets node filter for non-html {@link TargetType}. */
  @SuppressWarnings("SwitchIntDef") // Only some values handled.
  @Nullable
  public static Filter<AccessibilityNodeInfoCompat> createNodeFilter(
      @TargetType int target,
      @Nullable final Map<AccessibilityNodeInfoCompat, Boolean> speakingNodesCache) {
    if (NavigationTarget.isWebOnlyTarget(target)) {
      LogUtils.w(TAG, "Cannot define node filter for web only target.");
      return null;
    }
    Filter<AccessibilityNodeInfoCompat> nodeFilter =
        new Filter<AccessibilityNodeInfoCompat>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            return (node != null)
                && AccessibilityNodeInfoUtils.shouldFocusNode(node, speakingNodesCache);
          }
        };
    Filter<AccessibilityNodeInfoCompat> additionalCheckFilter = null;
    switch (target) {
      case TARGET_HEADING ->
          additionalCheckFilter =
              AccessibilityNodeInfoUtils.FILTER_HEADING.or(
                  AccessibilityNodeInfoUtils.FILTER_CONTAINER_WITH_UNFOCUSABLE_HEADING);
      case TARGET_CONTROL ->
          additionalCheckFilter =
              AccessibilityNodeInfoUtils.getFilterIncludingChildren(
                  AccessibilityNodeInfoUtils.FILTER_CONTROL);
      case TARGET_LINK -> additionalCheckFilter = AccessibilityNodeInfoUtils.FILTER_LINK;
      case TARGET_CONTAINER -> additionalCheckFilter = AccessibilityNodeInfoUtils.FILTER_CONTAINER;
      default -> {
        // TARGET_DEFAULT:
      }
    }
    if (additionalCheckFilter != null) {
      nodeFilter = nodeFilter.and(additionalCheckFilter);
    }
    return nodeFilter;
  }

  public static String targetTypeToString(@TargetType int targetType) {
    return switch (targetType) {
      case TARGET_DEFAULT -> "TARGET_DEFAULT";
      case TARGET_HEADING -> "TARGET_HEADING";
      case TARGET_CONTROL -> "TARGET_CONTROL";
      case TARGET_LINK -> "TARGET_LINK";
      case TARGET_HTML_ELEMENT_LIST -> "TARGET_HTML_ELEMENT_LIST";
      case TARGET_HTML_ELEMENT_BUTTON -> "TARGET_HTML_ELEMENT_BUTTON";
      case TARGET_HTML_ELEMENT_CHECKBOX -> "TARGET_HTML_ELEMENT_CHECKBOX";
      case TARGET_HTML_ELEMENT_RADIO -> "TARGET_HTML_ELEMENT_RADIO";
      case TARGET_HTML_ELEMENT_ARIA_LANDMARK -> "TARGET_HTML_ELEMENT_ARIA_LANDMARK";
      case TARGET_HTML_ELEMENT_EDIT_FIELD -> "TARGET_HTML_ELEMENT_EDIT_FIELD";
      case TARGET_HTML_ELEMENT_FOCUSABLE_ITEM -> "TARGET_HTML_ELEMENT_FOCUSABLE_ITEM";
      case TARGET_HTML_ELEMENT_HEADING_1 -> "TARGET_HTML_ELEMENT_HEADING_1";
      case TARGET_HTML_ELEMENT_HEADING_2 -> "TARGET_HTML_ELEMENT_HEADING_2";
      case TARGET_HTML_ELEMENT_HEADING_3 -> "TARGET_HTML_ELEMENT_HEADING_3";
      case TARGET_HTML_ELEMENT_HEADING_4 -> "TARGET_HTML_ELEMENT_HEADING_4";
      case TARGET_HTML_ELEMENT_HEADING_5 -> "TARGET_HTML_ELEMENT_HEADING_5";
      case TARGET_HTML_ELEMENT_HEADING_6 -> "TARGET_HTML_ELEMENT_HEADING_6";
      case TARGET_HTML_ELEMENT_GRAPHIC -> "TARGET_HTML_ELEMENT_GRAPHIC";
      case TARGET_HTML_ELEMENT_LIST_ITEM -> "TARGET_HTML_ELEMENT_LIST_ITEM";
      case TARGET_HTML_ELEMENT_TABLE -> "TARGET_HTML_ELEMENT_TABLE";
      case TARGET_HTML_ELEMENT_COMBOBOX -> "TARGET_HTML_ELEMENT_COMBOBOX";
      case TARGET_HTML_ELEMENT_VISITED_LINK -> "TARGET_HTML_ELEMENT_VISITED_LINK";
      case TARGET_HTML_ELEMENT_UNVISITED_LINK -> "TARGET_HTML_ELEMENT_UNVISITED_LINK";
      case TARGET_HTML_ELEMENT_COLUMN_BOUNDS -> "TARGET_HTML_ELEMENT_COLUMN_BOUNDS";
      case TARGET_HTML_ELEMENT_ROW_BOUNDS -> "TARGET_HTML_ELEMENT_ROW_BOUNDS";
      case TARGET_HTML_ELEMENT_TABLE_BOUNDS -> "TARGET_HTML_ELEMENT_TABLE_BOUNDS";
      case TARGET_COLUMN -> "TARGET_COLUMN";
      case TARGET_ROW -> "TARGET_ROW";
      case TARGET_CONTAINER -> "TARGET_CONTAINER";
      case TARGET_WINDOW -> "TARGET_WINDOW";
      case TARGET_SEARCH -> "TARGET_SEARCH";
      default -> "UNKNOWN";
    };
  }

  /** Transforms target type to cursor granularity */
  public static CursorGranularity targetTypeToGranularity(@TargetType int targetType) {
    switch (targetType) {
      case TARGET_HEADING:
        return HEADING;
      case TARGET_CONTROL:
        return CONTROL;
      case TARGET_LINK:
        return LINK;
      case TARGET_CONTAINER:
        return CONTAINER;
      case TARGET_WINDOW:
        return WINDOWS;
      case TARGET_SEARCH:
        return SEARCH;
      case TARGET_HTML_ELEMENT_LIST:
        return WEB_LIST;
      case TARGET_HTML_ELEMENT_HEADING_1:
        return WEB_H1;
      case TARGET_HTML_ELEMENT_HEADING_2:
        return WEB_H2;
      case TARGET_HTML_ELEMENT_HEADING_3:
        return WEB_H3;
      case TARGET_HTML_ELEMENT_HEADING_4:
        return WEB_H4;
      case TARGET_HTML_ELEMENT_HEADING_5:
        return WEB_H5;
      case TARGET_HTML_ELEMENT_HEADING_6:
        return WEB_H6;
      case TARGET_HTML_ELEMENT_ARIA_LANDMARK:
        return WEB_LANDMARK;
      case TARGET_HTML_ELEMENT_LIST_ITEM:
        return WEB_LISTITEM;
      case TARGET_HTML_ELEMENT_EDIT_FIELD:
        return WEB_EDITFIELD;
      case TARGET_HTML_ELEMENT_FOCUSABLE_ITEM:
        return WEB_FOCUSABLE;
      case TARGET_HTML_ELEMENT_BUTTON:
        return WEB_BUTTON;
      case TARGET_HTML_ELEMENT_CHECKBOX:
        return WEB_CHECKBOX;
      case TARGET_HTML_ELEMENT_RADIO:
        return WEB_RADIO;
      case TARGET_HTML_ELEMENT_GRAPHIC:
        return WEB_GRAPHIC;
      case TARGET_HTML_ELEMENT_TABLE:
        return WEB_TABLE;
      case TARGET_HTML_ELEMENT_COMBOBOX:
        return WEB_COMBOBOX;
      case TARGET_HTML_ELEMENT_VISITED_LINK:
        return WEB_VISITED_LINK;
      case TARGET_HTML_ELEMENT_UNVISITED_LINK:
        return WEB_UNVISITED_LINK;
      case TARGET_ROW:
        return ROW;
      case TARGET_COLUMN:
        return COLUMN;
      case TARGET_HTML_ELEMENT_COLUMN_BOUNDS:
      case TARGET_HTML_ELEMENT_ROW_BOUNDS:
      case TARGET_HTML_ELEMENT_TABLE_BOUNDS:
      case TARGET_DEFAULT:
      default:
        return DEFAULT;
    }
  }
}
