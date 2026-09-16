/*
 * Copyright (C) 2023 Google Inc.
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

package com.google.android.accessibility.braille.interfaces;

/**
 * An interface that defines screen reader executable actions and provides a method for performing
 * the actions.
 */
public interface ScreenReaderActionPerformer {

  /** Actions forwarded to screen reader to perform. */
  enum ScreenReaderAction {
    NEXT_ITEM,
    PREVIOUS_ITEM,
    NEXT_WINDOW,
    PREVIOUS_WINDOW,
    SCROLL_FORWARD,
    SCROLL_BACKWARD,
    NAVIGATE_TO_TOP,
    NAVIGATE_TO_BOTTOM,
    NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD,
    NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD,
    CLICK_CURRENT,
    CLICK_NODE,
    LONG_CLICK_CURRENT,
    LONG_CLICK_NODE,
    PREVIOUS_READING_CONTROL,
    NEXT_READING_CONTROL,
    SCREEN_SEARCH,
    OPEN_TALKBACK_MENU,
    TOGGLE_VOICE_FEEDBACK,
    GLOBAL_HOME,
    GLOBAL_BACK,
    GLOBAL_RECENTS,
    GLOBAL_NOTIFICATIONS,
    GLOBAL_QUICK_SETTINGS,
    GLOBAL_ALL_APPS,
    WEB_BROWSE_MODE,
    WEB_NEXT_HEADING_1,
    WEB_PREVIOUS_HEADING_1,
    WEB_NEXT_HEADING_2,
    WEB_PREVIOUS_HEADING_2,
    WEB_NEXT_HEADING_3,
    WEB_PREVIOUS_HEADING_3,
    WEB_NEXT_HEADING_4,
    WEB_PREVIOUS_HEADING_4,
    WEB_NEXT_HEADING_5,
    WEB_PREVIOUS_HEADING_5,
    WEB_NEXT_HEADING_6,
    WEB_PREVIOUS_HEADING_6,
    WEB_NEXT_VISITED_LINK,
    WEB_PREVIOUS_VISITED_LINK,
    WEB_NEXT_UNVISITED_LINK,
    WEB_PREVIOUS_UNVISITED_LINK,
    WEB_NEXT_LANDMARK,
    WEB_PREVIOUS_LANDMARK,
    WEB_NEXT_BUTTON,
    WEB_PREVIOUS_BUTTON,
    WEB_NEXT_CHECKBOX,
    WEB_PREVIOUS_CHECKBOX,
    WEB_NEXT_COMBOBOX,
    WEB_PREVIOUS_COMBOBOX,
    WEB_NEXT_EDITFIELD,
    WEB_PREVIOUS_EDITFIELD,
    WEB_NEXT_GRAPHIC,
    WEB_PREVIOUS_GRAPHIC,
    WEB_NEXT_LIST,
    WEB_PREVIOUS_LIST,
    WEB_NEXT_LIST_ITEM,
    WEB_PREVIOUS_LIST_ITEM,
    WEB_NEXT_RADIO_BUTTON,
    WEB_PREVIOUS_RADIO_BUTTON,
    WEB_NEXT_TABLE,
    WEB_PREVIOUS_TABLE,
    NEXT_HEADING,
    PREVIOUS_HEADING,
    NEXT_CONTROL,
    PREVIOUS_CONTROL,
    NEXT_LINK,
    PREVIOUS_LINK,
    ACCESSIBILITY_FOCUS,
    FOCUS_NEXT_CHARACTER,
    FOCUS_PREVIOUS_CHARACTER,
    FOCUS_NEXT_WORD,
    FOCUS_PREVIOUS_WORD,
    FOCUS_NEXT_LINE,
    FOCUS_PREVIOUS_LINE,
    STOP_READING,
    CUT,
    COPY,
    PASTE,
    SELECT_PREVIOUS_CHARACTER,
    SELECT_NEXT_CHARACTER,
    SELECT_PREVIOUS_WORD,
    SELECT_NEXT_WORD,
    SELECT_PREVIOUS_LINE,
    SELECT_NEXT_LINE,
    SELECT_CURRENT_TO_START,
    SELECT_CURRENT_TO_END,
    SELECT_ALL,
    TYPO_CORRECT,
    PLAY_PAUSE_MEDIA,
    CURSOR_TO_BEGINNING,
    CURSOR_TO_END,
    TABLE_PREVIOUS_ROW,
    TABLE_NEXT_ROW,
    TABLE_PREVIOUS_COL,
    TABLE_NEXT_COL
  }

  /** Performs the action associated with Talkback. */
  boolean performAction(ScreenReaderAction action, int inputMode, Object... args);
}
