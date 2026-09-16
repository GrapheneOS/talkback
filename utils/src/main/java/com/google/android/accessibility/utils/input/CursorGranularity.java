/*
 * Copyright (C) 2011 Google Inc.
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

package com.google.android.accessibility.utils.input;

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.R;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import java.util.Arrays;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** List of different Granularities for node and cursor movement in TalkBack. */
public enum CursorGranularity {
  // 0 is the default Android value when you want something outside the bit mask
  // TODO: If rewriting this as a class, use a constant for 0.
  DEFAULT(R.string.granularity_default, 1, 0),
  CHARACTER(
      R.string.granularity_character,
      2,
      AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_CHARACTER),
  WORD(R.string.granularity_word, 3, AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_WORD),
  LINE(R.string.granularity_line, 4, AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_LINE),
  PARAGRAPH(
      R.string.granularity_paragraph,
      5,
      AccessibilityNodeInfoCompat.MOVEMENT_GRANULARITY_PARAGRAPH),
  WEB_LIST(R.string.granularity_web_list, 8, 0),
  HEADING(R.string.granularity_native_heading, 10, 0),
  CONTROL(R.string.granularity_native_control, 11, 0),
  LINK(R.string.granularity_native_link, 12, 0),
  WEB_LANDMARK(R.string.granularity_web_landmark, 13, 0),
  WEB_BUTTON(R.string.granularity_web_button, 14, 0),
  WEB_CHECKBOX(R.string.granularity_web_checkbox, 15, 0),
  WEB_EDITFIELD(R.string.granularity_web_editfield, 16, 0),
  WEB_FOCUSABLE(R.string.granularity_web_focusable, 17, 0),
  WEB_H1(R.string.granularity_web_h1, 18, 0),
  WEB_H2(R.string.granularity_web_h2, 19, 0),
  WEB_H3(R.string.granularity_web_h3, 20, 0),
  WEB_H4(R.string.granularity_web_h4, 21, 0),
  WEB_H5(R.string.granularity_web_h5, 22, 0),
  WEB_H6(R.string.granularity_web_h6, 23, 0),
  WEB_GRAPHIC(R.string.granularity_web_graphic, 24, 0),
  WEB_LISTITEM(R.string.granularity_web_listitem, 25, 0),
  WEB_TABLE(R.string.granularity_web_table, 26, 0),
  WEB_COMBOBOX(R.string.granularity_web_combobox, 27, 0),
  WEB_VISITED_LINK(R.string.granularity_web_visited_link, 28, 0),
  WINDOWS(R.string.granularity_window, 29, 0),
  CONTAINER(R.string.granularity_container, 30, 0),
  SEARCH(R.string.granularity_search, 31, 0),
  ROW_COLUMN(R.string.granularity_row_column, 32, 0),
  ROW(R.string.granularity_row, 33, 0),
  COLUMN(R.string.granularity_column, 34, 0),
  WEB_UNVISITED_LINK(R.string.granularity_web_unvisited_link, 35, 0),
  WEB_RADIO(R.string.granularity_web_radio, 36, 0);

  /**
   * Whether to check the supported HTML elements when adding web granularities.
   *
   * <p>TODO: This is a workaround not to check the supported HTML elements from extras
   * due to the value might be empty. The way should be safe after checking with Chrome team.
   */
  private static final boolean CHECK_SUPPORTED_HTML_ELEMENTS = false;

  /** Used to represent a granularity with no framework value. */
  private static final int NO_VALUE = 0;

  /** The resource identifier for this granularity's user-visible name. */
  public final int resourceId;

  public final int id;

  /**
   * The framework value for this granularity, passed as an argument to {@link
   * AccessibilityNodeInfoCompat#ACTION_NEXT_AT_MOVEMENT_GRANULARITY}.
   */
  public final int value;

  /**
   * Constructs a new granularity with the specified system identifier.
   *
   * @param value The system identifier. See the GRANULARITY_ constants in {@link
   *     AccessibilityNodeInfoCompat} for a complete list.
   */
  private CursorGranularity(int resourceId, int id, int value) {
    this.resourceId = resourceId;
    this.id = id;
    this.value = value;
  }

  /**
   * Returns the granularity associated with a particular key.
   *
   * @param resourceId The key associated with a granularity.
   * @return The granularity associated with the key, or {@code null} if the key is invalid.
   */
  public static @Nullable CursorGranularity fromResourceId(int resourceId) {
    for (CursorGranularity value : values()) {
      if (value.resourceId == resourceId) {
        return value;
      }
    }

    return null;
  }

  public static @Nullable CursorGranularity fromId(int id) {
    for (CursorGranularity value : values()) {
      if (value.id == id) {
        return value;
      }
    }

    return null;
  }

  /**
   * Populates {@code result} with the {@link CursorGranularity}s represented by the {@code bitmask}
   * of granularity framework values. The {@link #DEFAULT} granularity is always returned as the
   * first item in the list.
   *
   * @param bitmask A bit mask of granularity framework values.
   * @param hasWebContent Whether the view has web content.
   * @param result The list to populate with supported granularities.
   */
  public static void extractFromMask(
      int bitmask,
      boolean hasWebContent,
      String @Nullable [] supportedHtmlElements,
      List<CursorGranularity> result) {
    result.clear();

    for (CursorGranularity value : values()) {
      if (value.value == NO_VALUE) {
        continue;
      }

      if ((bitmask & value.value) == value.value) {
        result.add(value);
      }
    }

    if (hasWebContent) {
      addWebGranularities(supportedHtmlElements, result);
    }
    result.add(HEADING);
    result.add(CONTROL);
    result.add(LINK);
    result.add(WINDOWS);
    result.add(CONTAINER);
    result.add(SEARCH);
    result.add(ROW_COLUMN);
    result.add(ROW);
    result.add(COLUMN);
    result.add(DEFAULT);
  }

  private static void addWebGranularities(
      String @Nullable [] supportedHtmlElements, List<CursorGranularity> result) {
    if (CHECK_SUPPORTED_HTML_ELEMENTS && supportedHtmlElements != null) {
      List<String> elements = Arrays.asList(supportedHtmlElements);
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_LANDMARK)) {
        result.add(WEB_LANDMARK);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_LIST)) {
        result.add(WEB_LIST);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_BUTTON)) {
        result.add(WEB_BUTTON);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_CHECKBOX)) {
        result.add(WEB_CHECKBOX);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_EDIT_FIELD)) {
        result.add(WEB_EDITFIELD);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_FOCUSABLE_ITEM)) {
        result.add(WEB_FOCUSABLE);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_1)) {
        result.add(WEB_H1);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_2)) {
        result.add(WEB_H2);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_3)) {
        result.add(WEB_H3);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_4)) {
        result.add(WEB_H4);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_5)) {
        result.add(WEB_H5);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_HEADING_6)) {
        result.add(WEB_H6);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_GRAPHIC)) {
        result.add(WEB_GRAPHIC);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_LIST_ITEM)) {
        result.add(WEB_LISTITEM);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_TABLE)) {
        result.add(WEB_TABLE);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_COMBOBOX)) {
        result.add(WEB_COMBOBOX);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_VISITED_LINK)) {
        result.add(WEB_VISITED_LINK);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_UNVISITED_LINK)) {
        result.add(WEB_UNVISITED_LINK);
      }
      if (elements.contains(WebInterfaceUtils.HTML_ELEMENT_MOVE_BY_RADIO)) {
        result.add(WEB_RADIO);
      }
    } else {
      result.add(WEB_LANDMARK);
      result.add(WEB_LIST);
      result.add(WEB_BUTTON);
      result.add(WEB_CHECKBOX);
      result.add(WEB_RADIO);
      result.add(WEB_EDITFIELD);
      result.add(WEB_FOCUSABLE);
      result.add(WEB_H1);
      result.add(WEB_H2);
      result.add(WEB_H3);
      result.add(WEB_H4);
      result.add(WEB_H5);
      result.add(WEB_H6);
      result.add(WEB_GRAPHIC);
      result.add(WEB_LISTITEM);
      result.add(WEB_TABLE);
      result.add(WEB_COMBOBOX);
      result.add(WEB_VISITED_LINK);
      result.add(WEB_UNVISITED_LINK);
    }
  }

  /** Returns whether {@code granularity} is a web-only granularity. */
  public boolean isWebOnlyGranularity() {
    // For some reason R.string cannot be used in a switch statement
    return resourceId == R.string.granularity_web_list
        || resourceId == R.string.granularity_web_landmark
        || resourceId == R.string.granularity_web_button
        || resourceId == R.string.granularity_web_editfield
        || resourceId == R.string.granularity_web_checkbox
        || resourceId == R.string.granularity_web_radio
        || resourceId == R.string.granularity_web_focusable
        || resourceId == R.string.granularity_web_h1
        || resourceId == R.string.granularity_web_h2
        || resourceId == R.string.granularity_web_h3
        || resourceId == R.string.granularity_web_h4
        || resourceId == R.string.granularity_web_h5
        || resourceId == R.string.granularity_web_h6
        || resourceId == R.string.granularity_web_graphic
        || resourceId == R.string.granularity_web_listitem
        || resourceId == R.string.granularity_web_table
        || resourceId == R.string.granularity_web_combobox
        || resourceId == R.string.granularity_web_visited_link
        || resourceId == R.string.granularity_web_unvisited_link;
  }

  /** Returns whether {@code granularity} supports web granularity. */
  public boolean supportsWebGranularity() {
    return isNativeMacroGranularity() || isWebOnlyGranularity() || isTableNavigationGranularity();
  }

  /**
   * Returns whether {@code granularity} is a native macro granularity. Macro granularity refers to
   * granularity which helps to navigate across multiple nodes in oppose to micro granularity
   * (Characters, words, etc) which is used to navigate within a node.
   */
  public boolean isNativeMacroGranularity() {
    return resourceId == R.string.granularity_native_heading
        || resourceId == R.string.granularity_native_control
        || resourceId == R.string.granularity_native_link;
  }

  public boolean isMicroGranularity() {
    return resourceId == R.string.granularity_character
        || resourceId == R.string.granularity_word
        || resourceId == R.string.granularity_line
        || resourceId == R.string.granularity_paragraph;
  }

  public boolean isTableNavigationGranularity() {
    return resourceId == R.string.granularity_row_column
        || resourceId == R.string.granularity_row
        || resourceId == R.string.granularity_column;
  }
}
