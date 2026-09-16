/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.android.accessibility.braille.brailledisplay;

import static androidx.core.content.res.ResourcesCompat.ID_NULL;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.getDotsDescription;
import static com.google.common.collect.ImmutableList.toImmutableList;

import android.content.Context;
import android.content.res.Resources;
import android.icu.text.NumberFormat;
import android.text.TextUtils;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.Locale;

/** Commands supported by Braille display. */
public class SupportedCommand {
  private static ImmutableList<SupportedCommand> supportedCommands;
  private static ImmutableList<SupportedCommand> allCommands;

  /** Returns an immutable and order-sensitive {@link SupportedCommand} list. */
  public static ImmutableList<SupportedCommand> getAllCommands(Context context) {
    if (allCommands == null) {
      ImmutableList.Builder<SupportedCommand> builder = ImmutableList.builder();
      builder
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_PAN_DOWN,
                  R.string.bd_cmd_nav_pan_down,
                  Category.BASIC,
                  KeyDescriptor.builder().setKeyNameRes(R.string.bd_key_pan_down).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_PAN_UP,
                  R.string.bd_cmd_nav_pan_up,
                  Category.BASIC,
                  KeyDescriptor.builder().setKeyNameRes(R.string.bd_key_pan_up).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_ROUTE,
                  R.string.bd_cmd_route,
                  Category.BASIC,
                  KeyDescriptor.builder().setKeyNameRes(R.string.bd_key_route).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_KEY_ENTER,
                  R.string.bd_cmd_activate_current,
                  Category.BASIC,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LONG_PRESS_CURRENT,
                  R.string.bd_cmd_touch_and_hold_current,
                  Category.BASIC,
                  KeyDescriptor.builder().setSpace(true).setDots(new BrailleCharacter(8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_ITEM_PREVIOUS,
                  R.string.bd_cmd_nav_item_previous,
                  Category.NAVIGATION,
                  Subcategory.BASIC,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(1, 7)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_ITEM_NEXT,
                  R.string.bd_cmd_nav_item_next,
                  Category.NAVIGATION,
                  Subcategory.BASIC,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(4, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SCROLL_BACKWARD,
                  R.string.bd_cmd_scroll_backward,
                  Category.NAVIGATION,
                  Subcategory.BASIC,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(2, 4, 6, 7)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SCROLL_FORWARD,
                  R.string.bd_cmd_scroll_forward,
                  Category.NAVIGATION,
                  Subcategory.BASIC,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(1, 3, 5, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent
                      .CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD,
                  R.string.bd_cmd_move_by_reading_granularity_or_adjust_reading_control_backward,
                  Category.NAVIGATION,
                  Subcategory.BASIC,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(3, 7)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent
                      .CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD,
                  R.string.bd_cmd_move_by_reading_granularity_or_adjust_reading_control_forward,
                  Category.NAVIGATION,
                  Subcategory.BASIC,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(6, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TOGGLE_AUTO_SCROLL,
                  R.string.bd_cmd_toggle_auto_scroll,
                  Category.NAVIGATION,
                  Subcategory.AUTO_SCROLL,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 4, 5, 6))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_INCREASE_AUTO_SCROLL_DURATION,
                  R.string.bd_cmd_increase_auto_scroll_duration,
                  Category.NAVIGATION,
                  Subcategory.AUTO_SCROLL,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(4)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_DECREASE_AUTO_SCROLL_DURATION,
                  R.string.bd_cmd_decrease_auto_scroll_duration,
                  Category.NAVIGATION,
                  Subcategory.AUTO_SCROLL,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(1)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_WINDOW_PREVIOUS,
                  R.string.bd_cmd_nav_window_previous,
                  Category.NAVIGATION,
                  Subcategory.WINDOW,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(2, 4, 5, 6, 7)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_WINDOW_NEXT,
                  R.string.bd_cmd_nav_window_next,
                  Category.NAVIGATION,
                  Subcategory.WINDOW,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(2, 4, 5, 6, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_TOP,
                  R.string.bd_cmd_nav_top,
                  Category.NAVIGATION,
                  Subcategory.PLACE_ON_PAGE,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 3))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_BOTTOM,
                  R.string.bd_cmd_nav_bottom,
                  Category.NAVIGATION,
                  Subcategory.PLACE_ON_PAGE,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(4, 5, 6))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_PREVIOUS_READING_CONTROL,
                  R.string.bd_cmd_previous_reading_control,
                  Category.NAVIGATION,
                  Subcategory.READING_CONTROLS,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(2, 3, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NEXT_READING_CONTROL,
                  R.string.bd_cmd_next_reading_control,
                  Category.NAVIGATION,
                  Subcategory.READING_CONTROLS,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(5, 6, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_CAPTION_ENTER_OR_EXIT,
                  R.string.bd_cmd_captions_show_or_hide,
                  Category.NAVIGATION,
                  Subcategory.CAPTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 4, 7, 8))
                      .build(),
                  FeatureFlagReader.useShowCaptions(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_BOTTOM,
                  R.string.bd_cmd_captions_end,
                  Category.NAVIGATION,
                  Subcategory.CAPTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(4, 5, 6))
                      .build(),
                  FeatureFlagReader.useShowCaptions(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_GLOBAL_BACK,
                  R.string.bd_cmd_global_back,
                  Category.SYSTEM_ACTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_GLOBAL_HOME,
                  R.string.bd_cmd_global_home,
                  Category.SYSTEM_ACTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 5))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_GLOBAL_NOTIFICATIONS,
                  R.string.bd_cmd_global_notifications,
                  Category.SYSTEM_ACTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 3, 4, 5))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_GLOBAL_RECENTS,
                  R.string.bd_cmd_global_recents,
                  Category.SYSTEM_ACTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 3, 5))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_QUICK_SETTINGS,
                  R.string.bd_cmd_quick_settings,
                  Category.SYSTEM_ACTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 3, 4, 5))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_ALL_APPS,
                  R.string.bd_cmd_global_all_apps,
                  Category.SYSTEM_ACTIONS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 3, 4))
                      .build(),
                  FeatureSupport.supportGetSystemActions(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TOGGLE_SCREEN_SEARCH,
                  R.string.bd_cmd_toggle_screen_search,
                  Category.TALKBACK_FEATURES,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(3, 4))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_EDIT_CUSTOM_LABEL,
                  R.string.bd_cmd_edit_custom_label,
                  Category.TALKBACK_FEATURES,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 3, 4, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_OPEN_TALKBACK_MENU,
                  R.string.bd_cmd_open_talkback_menu,
                  Category.TALKBACK_FEATURES,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 3, 4))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TOGGLE_VOICE_FEEDBACK,
                  R.string.bd_cmd_toggle_voice_feedback,
                  Category.TALKBACK_FEATURES,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(1, 3, 4, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_STOP_READING,
                  R.string.bd_cmd_read_stop,
                  Category.TALKBACK_FEATURES,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TALKBACK_SETTINGS,
                  R.string.bd_cmd_talkback_settings,
                  Category.TALKBACK_FEATURES,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(2, 3, 4, 5, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_PLAY_PAUSE_MEDIA,
                  R.string.bd_cmd_play_pause_media,
                  Category.TALKBACK_FEATURES,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(7, 8))
                      .build(),
                  FeatureSupport.supportGetSystemActions(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SWITCH_TO_NEXT_INPUT_LANGUAGE,
                  R.string.bd_cmd_switch_to_next_input_language,
                  Category.BRAILLE_SETTINGS,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(2, 4, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SWITCH_TO_NEXT_OUTPUT_LANGUAGE,
                  R.string.bd_cmd_switch_to_next_output_language,
                  Category.BRAILLE_SETTINGS,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(1, 3, 5, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TOGGLE_BRAILLE_GRADE,
                  R.string.bd_cmd_toggle_contracted_mode,
                  Category.BRAILLE_SETTINGS,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 4, 5))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HELP,
                  R.string.bd_cmd_help,
                  Category.BRAILLE_SETTINGS,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(1, 3, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_BRAILLE_DISPLAY_SETTINGS,
                  R.string.bd_cmd_braille_display_settings,
                  Category.BRAILLE_SETTINGS,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(1, 2, 7, 8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TURN_OFF_BRAILLE_DISPLAY,
                  R.string.bd_cmd_turn_off_braille_display,
                  Category.BRAILLE_SETTINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 4, 5, 6, 7, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NEXT_INPUT_METHOD,
                  R.string.bd_cmd_switch_to_next_input_method,
                  Category.EDITING,
                  Subcategory.SWITCH_KEYBOARD,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 3, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_CHARACTER_PREVIOUS,
                  R.string.bd_cmd_nav_character_previous,
                  Category.EDITING,
                  Subcategory.MOVE_CURSOR,
                  KeyDescriptor.builder().setSpace(true).setDots(new BrailleCharacter(3)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_CHARACTER_NEXT,
                  R.string.bd_cmd_nav_character_next,
                  Category.EDITING,
                  Subcategory.MOVE_CURSOR,
                  KeyDescriptor.builder().setSpace(true).setDots(new BrailleCharacter(6)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_WORD_PREVIOUS,
                  R.string.bd_cmd_nav_word_previous,
                  Category.EDITING,
                  Subcategory.MOVE_CURSOR,
                  KeyDescriptor.builder().setSpace(true).setDots(new BrailleCharacter(2)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_WORD_NEXT,
                  R.string.bd_cmd_nav_word_next,
                  Category.EDITING,
                  Subcategory.MOVE_CURSOR,
                  KeyDescriptor.builder().setSpace(true).setDots(new BrailleCharacter(5)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_LINE_PREVIOUS,
                  R.string.bd_cmd_nav_line_previous,
                  Category.EDITING,
                  Subcategory.MOVE_CURSOR,
                  KeyDescriptor.builder().setSpace(true).setDots(new BrailleCharacter(1)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_NAV_LINE_NEXT,
                  R.string.bd_cmd_nav_line_next,
                  Category.EDITING,
                  Subcategory.MOVE_CURSOR,
                  KeyDescriptor.builder().setSpace(true).setDots(new BrailleCharacter(4)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECTION_SELECT_ALL,
                  R.string.bd_cmd_select_all,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 3, 4, 5, 6, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECTION_SELECT_CURRENT_TO_START,
                  R.string.bd_cmd_select_cursor_to_start,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 3, 7, 8))
                      .build(),
                  FeatureFlagReader.useSelectCurrentToStartOrEnd(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECTION_SELECT_CURRENT_TO_END,
                  R.string.bd_cmd_select_cursor_to_end,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(4, 5, 6, 7, 8))
                      .build(),
                  FeatureFlagReader.useSelectCurrentToStartOrEnd(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECT_PREVIOUS_CHARACTER,
                  R.string.bd_cmd_select_previous_character,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(3, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECT_NEXT_CHARACTER,
                  R.string.bd_cmd_select_next_character,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(6, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECT_PREVIOUS_WORD,
                  R.string.bd_cmd_select_previous_word,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(2, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECT_NEXT_WORD,
                  R.string.bd_cmd_select_next_word,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(5, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECT_PREVIOUS_LINE,
                  R.string.bd_cmd_select_previous_line,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 8))
                      .build(),
                  // TODO: As the text selection for line granularity movement does not
                  // work, we mask off the action of selecting text by line.
                  /* available= */ false))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECT_NEXT_LINE,
                  R.string.bd_cmd_select_next_line,
                  Category.EDITING,
                  Subcategory.SELECT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(4, 8))
                      .build(),
                  // TODO: As the text selection for line granularity movement does not
                  // work, we mask off the action of selecting text by line.
                  /* available= */ false))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECTION_COPY,
                  R.string.bd_cmd_copy,
                  Category.EDITING,
                  Subcategory.EDIT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 4, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECTION_CUT,
                  R.string.bd_cmd_cut,
                  Category.EDITING,
                  Subcategory.EDIT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 3, 4, 6, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SELECTION_PASTE,
                  R.string.bd_cmd_paste,
                  Category.EDITING,
                  Subcategory.EDIT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 2, 3, 6, 8))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_KEY_DEL,
                  R.string.bd_cmd_key_del,
                  Category.EDITING,
                  Subcategory.EDIT,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(7)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_DEL_WORD,
                  R.string.bd_cmd_del_word,
                  Category.EDITING,
                  Subcategory.EDIT,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(2, 7))
                      .build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_KEY_ENTER,
                  R.string.bd_cmd_key_enter,
                  Category.EDITING,
                  Subcategory.EDIT,
                  KeyDescriptor.builder().setDots(new BrailleCharacter(8)).build()))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_SHOW_POPUP_MESSAGE_HISTORY,
                  R.string.bd_cmd_show_popup_message_history,
                  Category.BASIC,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 3, 4, 5, 7, 8))
                      .build(),
                  FeatureFlagReader.usePopupMessage(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TOGGLE_BROWSE_MODE,
                  R.string.bd_cmd_browse_mode,
                  Category.WEB_NAVIGATION,
                  Subcategory.UNDEFINED,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 3, 4, 6))
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_ARIA_LANDMARK_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 4, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_landmark_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_ARIA_LANDMARK_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_landmark,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 4, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_landmark_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_ARIA_LANDMARK_PREVIOUS,
                  R.string.bd_cmd_previous_landmark,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 4, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_landmark_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_GRAPHIC_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 4, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_graphic_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_GRAPHIC_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_graphic,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 4, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_graphic_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_GRAPHIC_PREVIOUS,
                  R.string.bd_cmd_previous_graphic,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 4, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_graphic_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LIST_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_list_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LIST_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_list,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3))
                      .setKeyNameRes(R.string.bd_cmd_next_list_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LIST_PREVIOUS,
                  R.string.bd_cmd_previous_list,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_list_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LIST_ITEM_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 4, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_list_item_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LIST_ITEM_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_list_item,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 4))
                      .setKeyNameRes(R.string.bd_cmd_next_list_item_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LIST_ITEM_PREVIOUS,
                  R.string.bd_cmd_previous_list_item,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 4, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_list_item_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TABLE_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 4, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_table_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TABLE_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_table,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 4, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_table_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TABLE_PREVIOUS,
                  R.string.bd_cmd_previous_table,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTENT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 4, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_table_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_NEXT_IN_BROWSE,
                  R.string.bd_cmd_heading_next,
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_PREVIOUS,
                  R.string.bd_cmd_heading_previous,
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_heading_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_1_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_1_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_1_NEXT_IN_BROWSE,
                  new Description(
                      R.string.bd_cmd_next_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(1)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_1_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_1_PREVIOUS,
                  new Description(
                      R.string.bd_cmd_previous_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(1)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_heading_1_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_2_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_2_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_2_NEXT_IN_BROWSE,
                  new Description(
                      R.string.bd_cmd_next_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(2)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_2_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_2_PREVIOUS,
                  new Description(
                      R.string.bd_cmd_previous_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(2)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_heading_2_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_3_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_3_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_3_NEXT_IN_BROWSE,
                  new Description(
                      R.string.bd_cmd_next_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(3)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_3_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_3_PREVIOUS,
                  new Description(
                      R.string.bd_cmd_previous_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(3)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_heading_3_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_4_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 5, 6, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_4_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_4_NEXT_IN_BROWSE,
                  new Description(
                      R.string.bd_cmd_next_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(4)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 5, 6))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_4_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_4_PREVIOUS,
                  new Description(
                      R.string.bd_cmd_previous_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(4)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 5, 6, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_heading_4_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_5_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 6, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_5_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_5_NEXT_IN_BROWSE,
                  new Description(
                      R.string.bd_cmd_next_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(5)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 6))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_5_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_5_PREVIOUS,
                  new Description(
                      R.string.bd_cmd_previous_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(5)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 6, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_heading_5_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_6_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_6_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_6_NEXT_IN_BROWSE,
                  new Description(
                      R.string.bd_cmd_next_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(6)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_heading_6_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_HEADING_6_PREVIOUS,
                  new Description(
                      R.string.bd_cmd_previous_heading_123456,
                      NumberFormat.getNumberInstance(Locale.getDefault()).format(6)),
                  Category.WEB_NAVIGATION,
                  Subcategory.HEADINGS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(2, 3, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_heading_6_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_CONTROL_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 4, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_control_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_CONTROL_NEXT_IN_BROWSE,
                  R.string.bd_cmd_control_next,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 4))
                      .setKeyNameRes(R.string.bd_cmd_next_control_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_CONTROL_PREVIOUS,
                  R.string.bd_cmd_control_previous,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 4, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_control_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_BUTTON_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_button_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_BUTTON_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_button,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2))
                      .setKeyNameRes(R.string.bd_cmd_next_button_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_BUTTON_PREVIOUS,
                  R.string.bd_cmd_previous_button,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_button_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_CHECKBOX_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 4, 6, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_checkbox_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_CHECKBOX_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_checkbox,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 4, 6))
                      .setKeyNameRes(R.string.bd_cmd_next_checkbox_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_CHECKBOX_PREVIOUS,
                  R.string.bd_cmd_previous_checkbox,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 4, 6, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_checkbox_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_RADIO_BUTTON_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_radio_button_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_RADIO_BUTTON_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_radio_button,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_radio_button_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_RADIO_BUTTON_PREVIOUS,
                  R.string.bd_cmd_previous_radio_button,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_radio_button_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_COMBO_BOX_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 4, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_combo_box_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_COMBO_BOX_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_combo_box,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 4))
                      .setKeyNameRes(R.string.bd_cmd_next_combo_box_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_COMBO_BOX_PREVIOUS,
                  R.string.bd_cmd_previous_combo_box,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 4, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_combo_box_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_EDIT_FIELD_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 5, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_edit_field_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_EDIT_FIELD_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_edit_field,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 5))
                      .setKeyNameRes(R.string.bd_cmd_next_edit_field_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_EDIT_FIELD_PREVIOUS,
                  R.string.bd_cmd_previous_edit_field,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 5, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_edit_field_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LINK_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LINK_NEXT_IN_BROWSE,
                  R.string.bd_cmd_list_next,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3))
                      .setKeyNameRes(R.string.bd_cmd_next_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_LINK_PREVIOUS,
                  R.string.bd_cmd_list_previous,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_VISITED_LINK_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 6, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_visited_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_VISITED_LINK_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_visited_link,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 6))
                      .setKeyNameRes(R.string.bd_cmd_next_visited_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_VISITED_LINK_PREVIOUS,
                  R.string.bd_cmd_previous_visited_link,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 2, 3, 6, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_visited_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_UNVISITED_LINK_NEXT,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 6, 8))
                      .setKeyNameRes(R.string.bd_cmd_next_unvisited_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_UNVISITED_LINK_NEXT_IN_BROWSE,
                  R.string.bd_cmd_next_unvisited_link,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 6))
                      .setKeyNameRes(R.string.bd_cmd_next_unvisited_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_UNVISITED_LINK_PREVIOUS,
                  R.string.bd_cmd_previous_unvisited_link,
                  Category.WEB_NAVIGATION,
                  Subcategory.CONTROLS,
                  KeyDescriptor.builder()
                      .setDots(new BrailleCharacter(1, 3, 6, 7))
                      .setKeyNameRes(R.string.bd_cmd_previous_unvisited_link_key)
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TABLE_COL_NEXT,
                  R.string.bd_cmd_next_column,
                  Category.WEB_NAVIGATION,
                  Subcategory.TABLE_NAVIGATION,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(5, 7, 8))
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TABLE_COL_PREVIOUS,
                  R.string.bd_cmd_previous_column,
                  Category.WEB_NAVIGATION,
                  Subcategory.TABLE_NAVIGATION,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(2, 7, 8))
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TABLE_ROW_NEXT,
                  R.string.bd_cmd_next_row,
                  Category.WEB_NAVIGATION,
                  Subcategory.TABLE_NAVIGATION,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(4, 7, 8))
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)))
          .add(
              new SupportedCommand(
                  BrailleInputEvent.CMD_TABLE_ROW_PREVIOUS,
                  R.string.bd_cmd_previous_row,
                  Category.WEB_NAVIGATION,
                  Subcategory.TABLE_NAVIGATION,
                  KeyDescriptor.builder()
                      .setSpace(true)
                      .setDots(new BrailleCharacter(1, 7, 8))
                      .build(),
                  FeatureFlagReader.useBrowseMode(context)));
      allCommands = builder.build();
    }
    return allCommands;
  }

  /** Returns available supported commands. */
  public static ImmutableList<SupportedCommand> getAvailableSupportedCommands(Context context) {
    if (supportedCommands == null) {
      supportedCommands =
          getAllCommands(context).stream()
              .filter(SupportedCommand::isAvailable)
              .collect(toImmutableList());
    }
    return supportedCommands;
  }

  @VisibleForTesting
  public static void reset() {
    supportedCommands = null;
    allCommands = null;
  }

  /** Category of command. */
  public enum Category {
    UNDEFINED,
    BASIC,
    NAVIGATION,
    SYSTEM_ACTIONS,
    TALKBACK_FEATURES,
    BRAILLE_SETTINGS,
    EDITING(R.string.bd_cmd_subcategory_editing_description),
    WEB_NAVIGATION(R.string.bd_cmd_subcategory_web_navigation_description);

    @StringRes private final int descriptionRes;

    Category() {
      this(ID_NULL);
    }

    Category(@StringRes int descriptionRes) {
      this.descriptionRes = descriptionRes;
    }

    /** Gets the description of the {@link Category}. */
    public String getDescription(Resources resources) {
      return descriptionRes == ID_NULL ? "" : resources.getString(descriptionRes);
    }
  }

  /** Subcategory of command. */
  public enum Subcategory {
    UNDEFINED(ID_NULL),
    BASIC(R.string.bd_cmd_subcategory_title_basic),
    WINDOW(R.string.bd_cmd_subcategory_title_window),
    PLACE_ON_PAGE(R.string.bd_cmd_subcategory_title_place_on_page),
    WEB_CONTENT(R.string.bd_cmd_subcategory_title_web_content),
    READING_CONTROLS(R.string.bd_cmd_subcategory_title_reading_controls),
    AUTO_SCROLL(R.string.bd_cmd_subcategory_title_auto_scroll),
    MOVE_CURSOR(R.string.bd_cmd_subcategory_title_move_cursor),
    SELECT(R.string.bd_cmd_subcategory_title_select),
    EDIT(R.string.bd_cmd_subcategory_title_edit),
    SWITCH_KEYBOARD(R.string.bd_cmd_subcategory_title_switch_keyboard),
    CAPTIONS(R.string.bd_cmd_subcategory_title_captions),
    CONTENT(R.string.bd_cmd_subcategory_title_content),
    HEADINGS(R.string.bd_cmd_subcategory_title_headings),
    CONTROLS(R.string.bd_cmd_subcategory_title_controls),
    TABLE_NAVIGATION(R.string.bd_cmd_subcategory_title_web_navigation);

    @StringRes private final int titleRes;

    Subcategory(@StringRes int titleRes) {
      this.titleRes = titleRes;
    }

    /** Gets the title of the {@link Subcategory}. */
    public String getTitle(Resources resources) {
      return titleRes == ID_NULL ? "" : resources.getString(titleRes);
    }
  }

  private final int command;
  private final Description commandDescription;
  private final Category category;
  private final Subcategory subcategory;
  private final KeyDescriptor keyDescriptor;
  private final boolean available;

  /** Description of the supported command. */
  private static class Description {
    @StringRes private final int descriptionId;
    private final Object[] arguments;

    private Description(@StringRes int descriptionId, Object... arguments) {
      this.descriptionId = descriptionId;
      this.arguments = arguments;
    }

    private String getDescription(Resources resources) {
      return resources.getString(descriptionId, arguments);
    }
  }

  private SupportedCommand(int command, KeyDescriptor keyDescriptor, boolean available) {
    this(command, ID_NULL, Category.UNDEFINED, Subcategory.UNDEFINED, keyDescriptor, available);
  }

  private SupportedCommand(
      int command,
      @StringRes int commandDescriptionRes,
      Category category,
      KeyDescriptor keyDescriptor) {
    this(
        command,
        commandDescriptionRes,
        category,
        Subcategory.UNDEFINED,
        keyDescriptor,
        /* available= */ true);
  }

  private SupportedCommand(
      int command,
      @StringRes int commandDescriptionRes,
      Category category,
      KeyDescriptor keyDescriptor,
      boolean available) {
    this(command, commandDescriptionRes, category, Subcategory.UNDEFINED, keyDescriptor, available);
  }

  private SupportedCommand(
      int command,
      @StringRes int commandDescriptionRes,
      Category category,
      Subcategory subcategory,
      KeyDescriptor keyDescriptor) {
    this(
        command,
        commandDescriptionRes,
        category,
        subcategory,
        keyDescriptor,
        /* available= */ true);
  }

  private SupportedCommand(
      int command,
      @StringRes int commandDescriptionRes,
      Category category,
      Subcategory subcategory,
      KeyDescriptor keyDescriptor,
      boolean available) {
    this(
        command,
        new Description(commandDescriptionRes),
        category,
        subcategory,
        keyDescriptor,
        available);
  }

  private SupportedCommand(
      int command,
      Description commandDescription,
      Category category,
      Subcategory subcategory,
      KeyDescriptor keyDescriptor,
      boolean available) {
    this.command = command;
    this.commandDescription = commandDescription;
    this.category = category;
    this.subcategory = subcategory;
    this.keyDescriptor = keyDescriptor;
    this.available = available;
  }

  /** Gets the corresponding braille input event command. */
  public int getCommand() {
    return command;
  }

  /** Whether the command is available. */
  public boolean isAvailable() {
    return available;
  }

  /** Gets the unified braille command key description. */
  public String getKeyDescription(Resources resources) {
    return keyDescriptor.getDescription(resources);
  }

  /** Gets the category of this braille command. */
  public Category getCategory() {
    return category;
  }

  /** Gets the subcategory of this braille command. */
  public Subcategory getSubcategory() {
    return subcategory;
  }

  /** Gets dots pressed for the command. */
  public BrailleCharacter getPressDot() {
    return keyDescriptor.dots();
  }

  /** Whether command includes space key. */
  public boolean hasSpace() {
    return keyDescriptor.space();
  }

  /** Gets the corresponding braille input event command description. */
  public String getCommandDescription(Resources resources) {
    if (commandDescription == null) {
      return "";
    }
    return commandDescription.getDescription(resources);
  }

  /** The descriptor describe a braille command. */
  @AutoValue
  abstract static class KeyDescriptor {
    abstract boolean space();

    abstract BrailleCharacter dots();

    abstract boolean longPress();

    @StringRes
    abstract int keyNameRes();

    static Builder builder() {
      return new AutoValue_SupportedCommand_KeyDescriptor.Builder()
          .setSpace(false)
          .setDots(BrailleCharacter.EMPTY_CELL)
          .setLongPress(false)
          .setKeyNameRes(0);
    }

    private String getDescription(Resources resources) {
      String result = "";
      String tmp = "";
      if (space()) {
        tmp = resources.getString(R.string.bd_key_space);
      }
      if (!dots().equals(BrailleCharacter.EMPTY_CELL)) {
        String r = getDotsDescription(resources, dots());
        if (TextUtils.isEmpty(tmp)) {
          tmp = r;
        } else {
          tmp = resources.getString(R.string.bd_commands_delimiter, tmp, r);
        }
      }
      if (TextUtils.isEmpty(tmp)) {
        if (keyNameRes() != 0) {
          tmp = resources.getString(keyNameRes());
          result =
              longPress()
                  ? resources.getString(R.string.bd_commands_touch_and_hold_template, tmp)
                  : resources.getString(R.string.bd_commands_press_template, tmp);
        }
      } else {
        result =
            longPress()
                ? resources.getString(R.string.bd_commands_touch_and_hold_template, tmp)
                : resources.getString(R.string.bd_commands_press_template, tmp);
        if (keyNameRes() != 0) {
          result =
              resources.getString(
                  R.string.bd_commands_web_key_template, resources.getString(keyNameRes()), result);
        }
      }
      return result;
    }

    /** KeyDescriptor builder. */
    @AutoValue.Builder
    abstract static class Builder {
      abstract Builder setSpace(boolean space);

      abstract Builder setDots(BrailleCharacter brailleCharacter);

      abstract Builder setLongPress(boolean longPress);

      abstract Builder setKeyNameRes(@StringRes int keyNameRes);

      abstract KeyDescriptor build();
    }
  }
}
