package com.google.android.accessibility.talkback.keyboard;

import android.content.res.Resources;
import android.view.KeyEvent;
import androidx.annotation.StringRes;
import com.google.android.accessibility.talkback.R;
import java.util.HashMap;
import java.util.Map;

/** Keyboard shortcut wrapper wraps the key and KeyboardShortcut ordinal together. */
public enum TalkBackPhysicalKeyboardShortcut {
  ACTION_UNKNOWN(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_UNKNOWN */ 75, -1, R.string.gesture_name_unknown),
  NAVIGATE_NEXT(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_ITEM */ 0,
      R.string.keycombo_shortcut_navigate_next,
      R.string.keycombo_menu_navigate_next),
  NAVIGATE_PREVIOUS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_ITEM */ 1,
      R.string.keycombo_shortcut_navigate_previous,
      R.string.keycombo_menu_navigate_previous),
  NAVIGATE_ABOVE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_ABOVE_ITEM */ 2,
      R.string.keycombo_shortcut_navigate_up,
      R.string.keycombo_menu_navigate_up),
  NAVIGATE_BELOW(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_BELOW_ITEM*/ 3,
      R.string.keycombo_shortcut_navigate_down,
      R.string.keycombo_menu_navigate_down),
  NAVIGATE_FIRST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_FIRST_ITEM */ 4,
      R.string.keycombo_shortcut_navigate_first,
      R.string.keycombo_menu_navigate_first),
  NAVIGATE_LAST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_LAST_ITEM */ 5,
      R.string.keycombo_shortcut_navigate_last,
      R.string.keycombo_menu_navigate_last),
  NAVIGATE_NEXT_WINDOW(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_WINDOW */ 6,
      R.string.keycombo_shortcut_navigate_next_window,
      R.string.keycombo_menu_navigate_next_window),
  NAVIGATE_PREVIOUS_WINDOW(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_WINDOW */ 7,
      R.string.keycombo_shortcut_navigate_previous_window,
      R.string.keycombo_menu_navigate_previous_window),
  NAVIGATE_NEXT_WORD(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_WORD */ 8,
      R.string.keycombo_shortcut_navigate_next_word,
      R.string.keycombo_menu_navigate_next_word),
  NAVIGATE_PREVIOUS_WORD(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_WORD */ 9,
      R.string.keycombo_shortcut_navigate_previous_word,
      R.string.keycombo_menu_navigate_previous_word),
  NAVIGATE_NEXT_CHARACTER(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_CHARACTER */ 10,
      R.string.keycombo_shortcut_navigate_next_character,
      R.string.keycombo_menu_navigate_next_character),
  NAVIGATE_PREVIOUS_CHARACTER(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_CHARACTER */ 11,
      R.string.keycombo_shortcut_navigate_previous_character,
      R.string.keycombo_menu_navigate_previous_character),
  PERFORM_CLICK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_PERFORM_CLICK */ 12,
      R.string.keycombo_shortcut_perform_click,
      R.string.keycombo_menu_perform_click),
  PERFORM_LONG_CLICK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_PERFORM_LONG_CLICK */ 13,
      R.string.keycombo_shortcut_perform_long_click,
      R.string.keycombo_menu_perform_long_click),
  // Global actions
  BACK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_BACK */ 14,
      R.string.keycombo_shortcut_global_back,
      R.string.keycombo_menu_global_back),
  HOME(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_HOME */ 15,
      R.string.keycombo_shortcut_global_home,
      R.string.keycombo_menu_global_home),
  RECENT_APPS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_RECENT_APPS */ 16,
      R.string.keycombo_shortcut_global_recents,
      R.string.keycombo_menu_global_recent),
  NOTIFICATIONS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NOTIFICATIONS */ 17,
      R.string.keycombo_shortcut_global_notifications,
      R.string.keycombo_menu_global_notifications),
  PAUSE_OR_RESUME_TALKBACK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_PAUSE_OR_RESUME_TALKBACK */ 18,
      -1 /* ?? */,
      R.string.shortcut_pause_or_resume_feedback),
  // Other actions
  READ_FROM_TOP(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_READ_FROM_TOP */ 19,
      R.string.keycombo_shortcut_other_read_from_top,
      R.string.shortcut_read_from_top),
  READ_FROM_CURSOR_ITEM(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_READ_FROM_NEXT_ITEM */ 20,
      R.string.keycombo_shortcut_other_read_from_cursor_item,
      R.string.keycombo_menu_other_read_from_cursor_item),
  SHOW_GLOBAL_CONTEXT_MENU(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_GLOBAL_CONTEXT_MENU */ 21,
      R.string.keycombo_shortcut_other_talkback_context_menu,
      R.string.keycombo_menu_other_talkback_context_menu),
  SHOW_LOCAL_CONTEXT_MENU(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_LOCAL_CONTEXT_MENU */ 22,
      -1 /* ?? */,
      R.string.context_menu_category_contextual_actions),
  SHOW_ACTIONS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_ACTIONS */ 23,
      R.string.keycombo_shortcut_other_custom_actions,
      R.string.keycombo_menu_other_custom_actions),
  SHOW_LANGUAGES_AVAILABLE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_LANGUAGES_AVAILABLE */ 24,
      R.string.keycombo_shortcut_other_language_options,
      R.string.keycombo_menu_other_language_options),
  SEARCH_SCREEN_FOR_ITEM(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SEARCH_SCREEN_FOR_ITEM */ 25,
      R.string.keycombo_shortcut_other_toggle_search,
      R.string.keycombo_menu_other_toggle_search),
  // Web navigation actions
  NAVIGATE_NEXT_BUTTON(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_BUTTON */ 26,
      R.string.keycombo_shortcut_navigate_next_button,
      R.string.keycombo_menu_web_navigate_to_next_button),
  NAVIGATE_PREVIOUS_BUTTON(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_BUTTON */ 27,
      R.string.keycombo_shortcut_navigate_previous_button,
      R.string.keycombo_menu_web_navigate_to_previous_button),
  NAVIGATE_NEXT_CONTROL(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_CONTROL */ 28,
      R.string.keycombo_shortcut_navigate_next_control,
      R.string.keycombo_menu_web_navigate_to_next_control),
  NAVIGATE_PREVIOUS_CONTROL(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_CONTROL */ 29,
      R.string.keycombo_shortcut_navigate_previous_control,
      R.string.keycombo_menu_web_navigate_to_previous_control),
  NAVIGATE_NEXT_ARIA_LANDMARK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_ARIA_LANDMARK */ 30,
      R.string.keycombo_shortcut_navigate_next_aria_landmark,
      R.string.keycombo_menu_web_navigate_to_next_aria_landmark),
  NAVIGATE_PREVIOUS_ARIA_LANDMARK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_ARIA_LANDMARK */ 31,
      R.string.keycombo_shortcut_navigate_previous_aria_landmark,
      R.string.keycombo_menu_web_navigate_to_previous_aria_landmark),
  NAVIGATE_NEXT_EDIT_FIELD(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_EDIT_FIELD */ 32,
      R.string.keycombo_shortcut_navigate_next_edit_field,
      R.string.keycombo_menu_web_navigate_to_next_edit_field),
  NAVIGATE_PREVIOUS_EDIT_FIELD(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_EDIT_FIELD */ 33,
      R.string.keycombo_shortcut_navigate_previous_edit_field,
      R.string.keycombo_menu_web_navigate_to_previous_edit_field),
  NAVIGATE_NEXT_FOCUSABLE_ITEM(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_FOCUSABLE_ITEM */ 34,
      R.string.keycombo_shortcut_navigate_next_focusable_item,
      R.string.keycombo_menu_web_navigate_to_next_focusable_item),
  NAVIGATE_PREVIOUS_FOCUSABLE_ITEM(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_FOCUSABLE_ITEM */ 35,
      R.string.keycombo_shortcut_navigate_previous_focusable_item,
      R.string.keycombo_menu_web_navigate_to_previous_focusable_item),
  NAVIGATE_NEXT_GRAPHIC(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_GRAPHIC */ 36,
      R.string.keycombo_shortcut_navigate_next_graphic,
      R.string.keycombo_menu_web_navigate_to_next_graphic),
  NAVIGATE_PREVIOUS_GRAPHIC(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_GRAPHIC */ 37,
      R.string.keycombo_shortcut_navigate_previous_graphic,
      R.string.keycombo_menu_web_navigate_to_previous_graphic),
  NAVIGATE_NEXT_HEADING(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_HEADING */ 38,
      R.string.keycombo_shortcut_navigate_next_heading,
      R.string.keycombo_menu_web_navigate_to_next_heading),
  NAVIGATE_PREVIOUS_HEADING(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_HEADING */ 39,
      R.string.keycombo_shortcut_navigate_previous_heading,
      R.string.keycombo_menu_web_navigate_to_previous_heading),
  NAVIGATE_NEXT_HEADING_1(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_HEADING_1 */ 40,
      R.string.keycombo_shortcut_navigate_next_heading_1,
      R.string.keycombo_menu_web_navigate_to_next_heading_1),
  NAVIGATE_PREVIOUS_HEADING_1(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_HEADING_1 */ 41,
      R.string.keycombo_shortcut_navigate_previous_heading_1,
      R.string.keycombo_menu_web_navigate_to_previous_heading_1),
  NAVIGATE_NEXT_HEADING_2(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_HEADING_2 */ 42,
      R.string.keycombo_shortcut_navigate_next_heading_2,
      R.string.keycombo_menu_web_navigate_to_next_heading_2),
  NAVIGATE_PREVIOUS_HEADING_2(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_HEADING_2 */ 43,
      R.string.keycombo_shortcut_navigate_previous_heading_2,
      R.string.keycombo_menu_web_navigate_to_previous_heading_2),
  NAVIGATE_NEXT_HEADING_3(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_HEADING_3 */ 44,
      R.string.keycombo_shortcut_navigate_next_heading_3,
      R.string.keycombo_menu_web_navigate_to_next_heading_3),
  NAVIGATE_PREVIOUS_HEADING_3(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_HEADING_3 */ 45,
      R.string.keycombo_shortcut_navigate_previous_heading_3,
      R.string.keycombo_menu_web_navigate_to_previous_heading_3),
  NAVIGATE_NEXT_HEADING_4(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_HEADING_4 */ 46,
      R.string.keycombo_shortcut_navigate_next_heading_4,
      R.string.keycombo_menu_web_navigate_to_next_heading_4),
  NAVIGATE_PREVIOUS_HEADING_4(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_HEADING_4 */ 47,
      R.string.keycombo_shortcut_navigate_previous_heading_4,
      R.string.keycombo_menu_web_navigate_to_previous_heading_4),
  NAVIGATE_NEXT_HEADING_5(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_HEADING_5 */ 48,
      R.string.keycombo_shortcut_navigate_next_heading_5,
      R.string.keycombo_menu_web_navigate_to_next_heading_5),
  NAVIGATE_PREVIOUS_HEADING_5(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_HEADING_5 */ 49,
      R.string.keycombo_shortcut_navigate_previous_heading_5,
      R.string.keycombo_menu_web_navigate_to_previous_heading_5),
  NAVIGATE_NEXT_HEADING_6(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_HEADING_6 */ 50,
      R.string.keycombo_shortcut_navigate_next_heading_6,
      R.string.keycombo_menu_web_navigate_to_next_heading_6),
  NAVIGATE_PREVIOUS_HEADING_6(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_HEADING_6 */ 51,
      R.string.keycombo_shortcut_navigate_previous_heading_6,
      R.string.keycombo_menu_web_navigate_to_previous_heading_6),
  NAVIGATE_NEXT_LIST_ITEM(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_LIST_ITEM */ 52,
      R.string.keycombo_shortcut_navigate_next_list_item,
      R.string.keycombo_menu_web_navigate_to_next_list_item),
  NAVIGATE_PREVIOUS_LIST_ITEM(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_LIST_ITEM */ 53,
      R.string.keycombo_shortcut_navigate_previous_list_item,
      R.string.keycombo_menu_web_navigate_to_previous_list_item),
  NAVIGATE_NEXT_LINK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_LINK */ 54,
      R.string.keycombo_shortcut_navigate_next_link,
      R.string.keycombo_menu_web_navigate_to_next_link),
  NAVIGATE_PREVIOUS_LINK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_LINK */ 55,
      R.string.keycombo_shortcut_navigate_previous_link,
      R.string.keycombo_menu_web_navigate_to_previous_link),
  NAVIGATE_NEXT_LIST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_LIST */ 56,
      R.string.keycombo_shortcut_navigate_next_list,
      R.string.keycombo_menu_web_navigate_to_next_list),
  NAVIGATE_PREVIOUS_LIST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_LIST */ 57,
      R.string.keycombo_shortcut_navigate_previous_list,
      R.string.keycombo_menu_web_navigate_to_previous_list),
  NAVIGATE_NEXT_TABLE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_TABLE */ 58,
      R.string.keycombo_shortcut_navigate_next_table,
      R.string.keycombo_menu_web_navigate_to_next_table),
  NAVIGATE_PREVIOUS_TABLE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_TABLE */ 59,
      R.string.keycombo_shortcut_navigate_previous_table,
      R.string.keycombo_menu_web_navigate_to_previous_table),
  NAVIGATE_NEXT_COMBOBOX(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_COMBOBOX */ 60,
      R.string.keycombo_shortcut_navigate_next_combobox,
      R.string.keycombo_menu_web_navigate_to_next_combobox),
  NAVIGATE_PREVIOUS_COMBOBOX(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_COMBOBOX */ 61,
      R.string.keycombo_shortcut_navigate_previous_combobox,
      R.string.keycombo_menu_web_navigate_to_previous_combobox),
  NAVIGATE_NEXT_CHECKBOX(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_CHECKBOX */ 62,
      R.string.keycombo_shortcut_navigate_next_checkbox,
      R.string.keycombo_menu_web_navigate_to_next_checkbox),
  NAVIGATE_PREVIOUS_CHECKBOX(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_CHECKBOX */ 63,
      R.string.keycombo_shortcut_navigate_previous_checkbox,
      R.string.keycombo_menu_web_navigate_to_previous_checkbox),
  NEXT_NAVIGATION_SETTING(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NEXT_NAVIGATION_SETTING */ 64,
      R.string.keycombo_shortcut_granularity_increase,
      R.string.keycombo_menu_granularity_increase),
  PREVIOUS_NAVIGATION_SETTING(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_PREVIOUS_NAVIGATION_SETTING */ 65,
      R.string.keycombo_shortcut_granularity_decrease,
      R.string.keycombo_menu_granularity_decrease),
  OPEN_MANAGE_KEYBOARD_SHORTCUTS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_OPEN_MANAGE_KEYBOARD_SHORTCUTS */ 66,
      -1 /* ?? */,
      R.string.gesture_name_unknown), // Deprecated
  OPEN_TALKBACK_SETTINGS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_OPEN_TALKBACK_SETTINGS */ 67,
      R.string.keycombo_shortcut_other_open_talkback_settings,
      R.string.keycombo_menu_other_open_talkback_settings),
  // Navigation in default keymap.
  NAVIGATE_NEXT_DEFAULT(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_ITEM_DEFAULT */ 68,
      R.string.keycombo_shortcut_navigate_next_default,
      R.string.shortcut_next),
  NAVIGATE_PREVIOUS_DEFAULT(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_ITEM_DEFAULT */ 69,
      R.string.keycombo_shortcut_navigate_previous_default,
      R.string.shortcut_previous),
  // Global shortcut
  PLAY_PAUSE_MEDIA(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_PLAY_PAUSE_MEDIA */ 70,
      R.string.keycombo_shortcut_global_play_pause_media,
      R.string.keycombo_menu_global_play_pause_media),
  SELECT_NEXT_READING_MENU(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SELECT_NEXT_READING_MENU */ 71,
      R.string.keycombo_shortcut_global_scroll_forward_reading_menu,
      R.string.keycombo_menu_global_scroll_forward_reading_menu),
  SELECT_PREVIOUS_READING_MENU(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SELECT_PREVIOUS_READING_MENU */ 72,
      R.string.keycombo_shortcut_global_scroll_backward_reading_menu,
      R.string.keycombo_menu_global_scroll_backward_reading_menu),
  ADJUST_READING_MENU_UP(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ADJUST_READING_MENU_UP */ 73,
      R.string.keycombo_shortcut_global_adjust_reading_settings_previous,
      R.string.keycombo_menu_global_adjust_reading_settings_previous),
  ADJUST_READING_MENU_DOWN(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ADJUST_READING_MENU_DOWN */ 74,
      R.string.keycombo_shortcut_global_adjust_reading_setting_next,
      R.string.keycombo_menu_global_adjust_reading_setting_next),
  COPY_LAST_SPOKEN_PHRASE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_COPY_LAST_SPOKEN_PHRASE */ 76,
      R.string.keycombo_shortcut_other_copy_last_spoken_phrase,
      R.string.keycombo_menu_other_copy_last_spoken_phrase),
  HIDE_OR_SHOW_SCREEN(
      /* KEYBOARD_SHORTCUT_HIDE_OR_SHOW_SCREEN */ 77,
      R.string.keycombo_shortcut_global_hide_or_show_screen,
      R.string.keycombo_menu_global_hide_or_show_screen),
  NAVIGATE_NEXT_ROW(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_ROW */ 78,
      R.string.keycombo_shortcut_navigate_next_row,
      R.string.keycombo_menu_web_navigate_to_next_row),
  NAVIGATE_PREVIOUS_ROW(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_ROW */ 79,
      R.string.keycombo_shortcut_navigate_previous_row,
      R.string.keycombo_menu_web_navigate_to_previous_row),
  NAVIGATE_NEXT_COLUMN(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_COLUMN */ 80,
      R.string.keycombo_shortcut_navigate_next_column,
      R.string.keycombo_menu_web_navigate_to_next_column),
  NAVIGATE_PREVIOUS_COLUMN(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_COLUMN */ 81,
      R.string.keycombo_shortcut_navigate_previous_column,
      R.string.keycombo_menu_web_navigate_to_previous_column),
  NAVIGATE_NEXT_ROW_BOUNDS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_ROW_BOUNDS */ 82,
      R.string.keycombo_shortcut_navigate_next_row_bounds,
      R.string.keycombo_menu_web_navigate_to_next_row_bounds),
  NAVIGATE_PREVIOUS_ROW_BOUNDS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_ROW_BOUNDS */ 83,
      R.string.keycombo_shortcut_navigate_previous_row_bounds,
      R.string.keycombo_menu_web_navigate_to_previous_row_bounds),
  NAVIGATE_NEXT_COLUMN_BOUNDS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_COLUMN_BOUNDS */ 84,
      R.string.keycombo_shortcut_navigate_next_column_bounds,
      R.string.keycombo_menu_web_navigate_to_next_column_bounds),
  NAVIGATE_PREVIOUS_COLUMN_BOUNDS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_COLUMN_BOUNDS */ 85,
      R.string.keycombo_shortcut_navigate_previous_column_bounds,
      R.string.keycombo_menu_web_navigate_to_previous_column_bounds),
  NAVIGATE_NEXT_TABLE_BOUNDS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_TABLE_BOUNDS */ 86,
      R.string.keycombo_shortcut_navigate_next_table_bounds,
      R.string.keycombo_menu_web_navigate_to_next_table_bounds),
  NAVIGATE_PREVIOUS_TABLE_BOUNDS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_TABLE_BOUNDS */ 87,
      R.string.keycombo_shortcut_navigate_previous_table_bounds,
      R.string.keycombo_menu_web_navigate_to_previous_table_bounds),
  NAVIGATE_NEXT_VISITED_LINK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_VISITED_LINK */ 88,
      R.string.keycombo_shortcut_navigate_next_visited_link,
      R.string.keycombo_menu_web_navigate_to_next_visited_link),
  NAVIGATE_PREVIOUS_VISITED_LINK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_VISITED_LINK */ 89,
      R.string.keycombo_shortcut_navigate_previous_visited_link,
      R.string.keycombo_menu_web_navigate_to_previous_visited_link),
  INPUT_FRAMEWORK_NAVIGATE_UP(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_INPUT_FRAMEWORK_NAVIGATE_UP */ 90,
      R.string.keycombo_shortcut_input_framework_navigate_up,
      R.string.keycombo_menu_navigate_up),
  INPUT_FRAMEWORK_NAVIGATE_DOWN(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_INPUT_FRAMEWORK_NAVIGATE_DOWN */ 91,
      R.string.keycombo_shortcut_input_framework_navigate_down,
      R.string.keycombo_menu_navigate_down),
  INPUT_FRAMEWORK_NAVIGATE_LEFT(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_INPUT_FRAMEWORK_NAVIGATE_LEFT */ 92,
      R.string.keycombo_shortcut_input_framework_navigate_left,
      R.string.keycombo_key_arrow_left),
  INPUT_FRAMEWORK_NAVIGATE_RIGHT(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_INPUT_FRAMEWORK_NAVIGATE_RIGHT */ 93,
      R.string.keycombo_shortcut_input_framework_navigate_right,
      R.string.keycombo_key_arrow_right),
  INPUT_FRAMEWORK_ENTER(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_INPUT_FRAMEWORK_NAVIGATE_ENTER */ 94,
      R.string.keycombo_shortcut_input_framework_enter,
      R.string.keycombo_shortcut_input_framework_enter),
  SPEECH_RATE_INCREASE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SPEECH_RATE_INCREASE */ 95,
      R.string.keycombo_shortcut_global_speech_rate_increase,
      R.string.shortcut_speech_rate_increase),
  SPEECH_RATE_DECREASE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SPEECH_RATE_DECREASE */ 96,
      R.string.keycombo_shortcut_global_speech_rate_decrease,
      R.string.shortcut_speech_rate_decrease),
  ANNOUNCE_CURRENT_TIME_AND_DATE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ANNOUNCE_CURRENT_TIME_AND_DATE */ 97,
      R.string.keycombo_shortcut_global_announce_current_time_and_date,
      R.string.shortcut_announce_current_time_and_date),
  ANNOUNCE_BATTERY_STATE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ANNOUNCE_BATTERY_STATE */ 98,
      R.string.keycombo_shortcut_global_announce_battery_state,
      R.string.shortcut_announce_battery_state),
  SPEECH_VOLUME_INCREASE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SPEECH_VOLUME_INCREASE */ 99,
      R.string.keycombo_shortcut_global_speech_volume_increase,
      R.string.keycombo_menu_global_speech_volume_increase),
  SPEECH_VOLUME_DECREASE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SPEECH_VOLUME_DECREASE */ 100,
      R.string.keycombo_shortcut_global_speech_volume_decrease,
      R.string.keycombo_menu_global_speech_volume_decrease),
  SPEECH_PITCH_INCREASE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SPEECH_PITCH_INCREASE */ 101,
      R.string.keycombo_shortcut_global_speech_pitch_increase,
      R.string.shortcut_speech_pitch_increase),
  SPEECH_PITCH_DECREASE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SPEECH_PITCH_DECREASE */ 102,
      R.string.keycombo_shortcut_global_speech_pitch_decrease,
      R.string.shortcut_speech_pitch_decrease),
  TOGGLE_SELECTION(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_TOGGLE_SELECTION*/ 103,
      R.string.keycombo_shortcut_other_toggle_selection,
      R.string.shortcut_toggle_selection),
  OPEN_TTS_SETTINGS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_OPEN_TTS_SETTINGS */ 104,
      R.string.keycombo_shortcut_other_open_tts_settings,
      R.string.keycombo_menu_other_open_tts_settings),
  RESET_SPEECH_SETTINGS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_RESET_SPEECH_SETTINGS */ 105,
      R.string.keycombo_shortcut_global_reset_speech_settings,
      R.string.reset_speech_rate_and_pitch),
  SHOW_TUTORIAL(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_TUTORIAL */ 106,
      R.string.keycombo_shortcut_other_show_tutorial,
      R.string.shortcut_show_tutorial),
  TOGGLE_SOUND_FEEDBACK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_TOGGLE_SOUND_FEEDBACK */ 107,
      R.string.keycombo_shortcut_global_toggle_sound_feedback,
      R.string.shortcut_toggle_sound_feedback),
  SHOW_LEARN_MODE_PAGE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_LEARN_MODE_PAGE*/ 108,
      R.string.keycombo_shortcut_global_show_learn_mode_page,
      R.string.shortcut_show_learn_mode_page),
  TOGGLE_VOICE_FEEDBACK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_TOGGLE_VOICE_FEEDBACK */ 109,
      R.string.keycombo_shortcut_global_toggle_voice_feedback,
      R.string.shortcut_toggle_voice_feedback),
  NAVIGATE_NEXT_LINE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_NEXT_LINE */ 110,
      R.string.keycombo_shortcut_navigate_next_line,
      R.string.keycombo_menu_navigate_next_line),
  NAVIGATE_PREVIOUS_LINE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_PREVIOUS_LINE */ 111,
      R.string.keycombo_shortcut_navigate_previous_line,
      R.string.keycombo_menu_navigate_previous_line),
  TOGGLE_BRAILLE_CONTRACTED_MODE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_TOGGLE_BRAILLE_CONTRACTED_MODE */ 112,
      R.string.keycombo_shortcut_other_toggle_braille_contracted_mode,
      R.string.shortcut_toggle_braille_contracted_mode),
  TOGGLE_BRAILLE_ON_SCREEN_OVERLAY(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_TOGGLE_BRAILLE_ON_SCREEN_OVERLAY */ 113,
      R.string.keycombo_shortcut_other_toggle_braille_on_screen_overlay,
      R.string.shortcut_toggle_braille_on_screen_overlay),
  NAVIGATE_NEXT_UNVISITED_LINK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_UNVISITED_LINK */ 114,
      R.string.keycombo_shortcut_navigate_next_unvisited_link,
      R.string.keycombo_menu_web_navigate_to_next_unvisited_link),
  NAVIGATE_PREVIOUS_UNVISITED_LINK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_UNVISITED_LINK */ 115,
      R.string.keycombo_shortcut_navigate_previous_unvisited_link,
      R.string.keycombo_menu_web_navigate_to_previous_unvisited_link),
  REPORT_ISSUE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_REPORT_ISSUE */ 116,
      R.string.keycombo_shortcut_other_report_issue,
      R.string.keycombo_menu_other_report_issue),
  NAVIGATE_NEXT_RADIO(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_RADIO */ 117,
      R.string.keycombo_shortcut_navigate_next_radio,
      R.string.keycombo_menu_web_navigate_to_next_radio),
  NAVIGATE_PREVIOUS_RADIO(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_RADIO */ 118,
      R.string.keycombo_shortcut_navigate_previous_radio,
      R.string.keycombo_menu_web_navigate_to_previous_radio),
  ANNOUNCE_CURRENT_TITLE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ANNOUNCE_CURRENT_TITLE */ 119,
      R.string.keycombo_shortcut_global_announce_current_title,
      R.string.shortcut_announce_current_title),
  ANNOUNCE_PHONETIC_PRONUNCIATION(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ANNOUNCE_PHONETIC_PRONUNCIATION */ 120,
      R.string.keycombo_shortcut_global_announce_phonetic_pronunciation,
      R.string.shortcut_announce_phonetic_pronunciation),
  CYCLE_PUNCTUATION_VERBOSITY(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_CYCLE_PUNCTUATION_VERBOSITY */ 121,
      R.string.keycombo_shortcut_global_cycle_punctuation_verbosity,
      R.string.shortcut_cycle_punctuation_verbosity),
  SHOW_HEADING_LIST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_HEADING_LIST */ 122,
      R.string.keycombo_shortcut_other_show_heading_list,
      R.string.keycombo_menu_other_show_heading_list),
  SHOW_LANDMARK_LIST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_LANDMARK_LIST */ 123,
      R.string.keycombo_shortcut_other_show_landmark_list,
      R.string.keycombo_menu_other_show_landmark_list),
  SHOW_LINK_LIST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_LINK_LIST */ 124,
      R.string.keycombo_shortcut_other_show_link_list,
      R.string.keycombo_menu_other_show_link_list),
  SHOW_CONTROL_LIST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_CONTROL_LIST */ 125,
      R.string.keycombo_shortcut_other_show_control_list,
      R.string.keycombo_menu_other_show_control_list),
  SHOW_TABLE_LIST(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_TABLE_LIST */ 126,
      R.string.keycombo_shortcut_other_show_table_list,
      R.string.keycombo_menu_other_show_table_list),
  READ_LINK_URL(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_READ_LINK_URL */ 127,
      R.string.keycombo_shortcut_other_read_link_url,
      R.string.keycombo_menu_other_read_link_url),
  DESCRIBE_IMAGE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_DESCRIBE_IMAGE */ 128,
      R.string.keycombo_shortcut_global_describe_image,
      R.string.keycombo_menu_global_describe_image),
  SCREEN_OVERVIEW(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SCREEN_OVERVIEW */ 129,
      R.string.keycombo_shortcut_global_screen_overview,
      R.string.keycombo_menu_global_screen_overview),
  TOGGLE_KEY_COMBO_PASS_THROUGH_ONCE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_KEY_COMBO_PASS_THROUGH_ONCE */ 130,
      R.string.keycombo_shortcut_global_toggle_key_combo_pass_through_once,
      R.string.keycombo_menu_global_toggle_key_combo_pass_through_once),
  PERFORM_DOUBLE_CLICK(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_PERFORM_DOUBLE_CLICK */ 131,
      R.string.keycombo_shortcut_perform_double_click,
      R.string.keycombo_menu_perform_double_click),
  ANNOUNCE_COLUMN_HEADER(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ANNOUNCE_COLUMN_HEADER */ 132,
      R.string.keycombo_shortcut_other_announce_column_header,
      R.string.keycombo_menu_other_announce_column_header),
  ANNOUNCE_ROW_HEADER(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ANNOUNCE_ROW_HEADER */ 133,
      R.string.keycombo_shortcut_other_announce_row_header,
      R.string.keycombo_menu_other_announce_row_header),
  NAVIGATE_NEXT_CONTAINER(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_NEXT_CONTAINER */ 134,
      R.string.keycombo_shortcut_navigate_next_container,
      R.string.shortcut_next_container),
  NAVIGATE_PREVIOUS_CONTAINER(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_NAVIGATE_TO_PREVIOUS_CONTAINER */ 135,
      R.string.keycombo_shortcut_navigate_previous_container,
      R.string.shortcut_prev_container),
  CYCLE_TYPING_ECHO(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_CYCLE_TYPING_ECHO */ 136,
      R.string.keycombo_shortcut_other_cycle_typing_echo,
      R.string.keycombo_menu_other_cycle_typing_echo),
  REFOCUS_CURRENT_NODE(
      /* KeyboardShortcut.REFOCUS_CURRENT_NODE */ 137,
      R.string.keycombo_shortcut_global_refocus_current_node,
      R.string.keycombo_menu_global_refocus_current_node),
  ANNOUNCE_RICH_TEXT_DESCRIPTION(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_ANNOUNCE_RICH_TEXT_DESCRIPTION */ 138,
      R.string.keycombo_shortcut_global_announce_rich_text_description,
      R.string.title_text_formatting_menu),
  READ_CURRENT_URL(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_READ_CURRENT_URL */ 139,
      R.string.keycombo_shortcut_other_read_current_url,
      R.string.keycombo_menu_other_read_current_url),
  SHOW_KEYBOARD_SHORTCUTS_DIALOG(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_SHOW_KEYBOARD_SHORTCUTS_DIALOG */ 140,
      R.string.keycombo_shortcut_other_show_keyboard_shortcuts_dialog,
      R.string.keycombo_menu_other_show_keyboard_shortcuts_dialog),
  OPEN_VOICE_COMMANDS(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_OPEN_VOICE_COMMANDS */ 141,
      R.string.keycombo_shortcut_other_open_voice_commands,
      R.string.keycombo_menu_other_open_voice_commands),
  TOGGLE_BROWSE_MODE(
      /* KeyboardShortcut.KEYBOARD_SHORTCUT_TOGGLE_BROWSE_MODE */ 142,
      R.string.keycombo_shortcut_other_toggle_browse_mode,
      R.string.keycombo_menu_other_toggle_browse_mode);

  private static Map<String, TalkBackPhysicalKeyboardShortcut> keyShortcutMap;
  private static Map<Integer, TalkBackPhysicalKeyboardShortcut> ordinalShortcutMap;
  private final int keyboardShortcutOrdinal;
  @StringRes private final int keyStrRes;
  @StringRes private final int descriptionStrRes;

  TalkBackPhysicalKeyboardShortcut(
      int keyboardShortcutOrdinal, @StringRes int keyStrRes, @StringRes int descriptionStrRes) {
    this.keyboardShortcutOrdinal = keyboardShortcutOrdinal;
    this.keyStrRes = keyStrRes;
    this.descriptionStrRes = descriptionStrRes;
  }

  public int getKeyboardShortcutOrdinal() {
    return keyboardShortcutOrdinal;
  }

  public int getKeyStrRes() {
    return keyStrRes;
  }

  public String getKey(Resources resources) {
    if (keyStrRes == -1) {
      return "";
    }
    return resources.getString(keyStrRes);
  }

  public String getDescription(Resources resources) {
    if (descriptionStrRes == -1) {
      return "";
    }
    return resources.getString(descriptionStrRes);
  }

  /** Obtains {@link TalkBackPhysicalKeyboardShortcut} from key. */
  public static TalkBackPhysicalKeyboardShortcut getActionFromKey(Resources resources, String key) {
    if (keyShortcutMap == null) {
      keyShortcutMap = new HashMap<>();
      for (TalkBackPhysicalKeyboardShortcut talkBackPhysicalKeyboardShortcut :
          TalkBackPhysicalKeyboardShortcut.values()) {
        keyShortcutMap.put(
            talkBackPhysicalKeyboardShortcut.getKey(resources), talkBackPhysicalKeyboardShortcut);
      }
    }
    return nullToUnknown(keyShortcutMap.get(key));
  }

  /** Obtains {@link TalkBackPhysicalKeyboardShortcut} from keyboard shortcut ordinal. */
  public static TalkBackPhysicalKeyboardShortcut getActionFromKeyboardShortcutOrdinal(
      int keyboardShortcutOrdinal) {
    if (ordinalShortcutMap == null) {
      ordinalShortcutMap = new HashMap<>();
      for (TalkBackPhysicalKeyboardShortcut talkBackPhysicalKeyboardShortcut :
          TalkBackPhysicalKeyboardShortcut.values()) {
        ordinalShortcutMap.put(
            talkBackPhysicalKeyboardShortcut.keyboardShortcutOrdinal,
            talkBackPhysicalKeyboardShortcut);
      }
    }
    return nullToUnknown(ordinalShortcutMap.get(keyboardShortcutOrdinal));
  }

  /**
   * Frameworks actions such as navigate the input focus or enter are handled by frameworks side
   * rather than handled by any key maps. This is a method handles the mapping since these actions
   * would not go through into any {@link KeyComboModel}.
   */
  public static TalkBackPhysicalKeyboardShortcut getActionFromInputFrameworkAction(int keyCode) {
    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_UP -> {
        return INPUT_FRAMEWORK_NAVIGATE_UP;
      }
      case KeyEvent.KEYCODE_DPAD_DOWN -> {
        return INPUT_FRAMEWORK_NAVIGATE_DOWN;
      }
      case KeyEvent.KEYCODE_DPAD_LEFT -> {
        return INPUT_FRAMEWORK_NAVIGATE_LEFT;
      }
      case KeyEvent.KEYCODE_DPAD_RIGHT -> {
        return INPUT_FRAMEWORK_NAVIGATE_RIGHT;
      }
      case KeyEvent.KEYCODE_ENTER -> {
        return INPUT_FRAMEWORK_ENTER;
      }
      default -> {}
    }
    return ACTION_UNKNOWN;
  }

  private static TalkBackPhysicalKeyboardShortcut nullToUnknown(
      TalkBackPhysicalKeyboardShortcut shortcut) {
    return shortcut == null ? ACTION_UNKNOWN : shortcut;
  }
}
