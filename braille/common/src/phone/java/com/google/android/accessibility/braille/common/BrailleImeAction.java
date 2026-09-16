/*
 * Copyright 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.common;

import android.content.res.Resources;
import androidx.annotation.StringRes;
import com.google.android.accessibility.braille.interfaces.ImeAction;

/** Braille keyboard actions. */
public enum BrailleImeAction implements ImeAction {
  ADD_SPACE_OR_NEXT_ITEM(R.string.bk_gesture_add_space),
  DELETE_CHARACTER_OR_PREVIOUS_ITEM(R.string.bk_gesture_delete),
  ADD_NEWLINE(R.string.bk_gesture_new_line),
  DELETE_WORD(R.string.bk_gesture_delete_word),
  HIDE_KEYBOARD(R.string.bk_gesture_hide_the_keyboard), // not used.
  SWITCH_KEYBOARD(R.string.bk_gesture_switch_to_next_keyboard),
  SUBMIT_TEXT(R.string.bk_gesture_submit_text),
  HELP_AND_OTHER_ACTIONS(R.string.bk_gesture_help_and_other_options),
  PREVIOUS_CHARACTER(R.string.bk_gesture_move_to_previous_character),
  NEXT_CHARACTER(R.string.bk_gesture_move_to_next_character),
  PREVIOUS_WORD(R.string.bk_gesture_move_to_previous_word),
  NEXT_WORD(R.string.bk_gesture_move_to_next_word),
  PREVIOUS_LINE(R.string.bk_gesture_move_to_previous_line),
  NEXT_LINE(R.string.bk_gesture_move_to_next_line),
  BEGINNING_OF_PAGE(R.string.bk_gesture_move_to_beginning),
  END_OF_PAGE(R.string.bk_gesture_move_to_end),
  PREVIOUS_GRANULARITY(R.string.bk_gesture_switch_to_previous_granularity),
  NEXT_GRANULARITY(R.string.bk_gesture_switch_to_next_granularity),
  MOVE_CURSOR_BACKWARD(R.string.bk_gesture_move_cursor_backward),
  MOVE_CURSOR_FORWARD(R.string.bk_gesture_move_cursor_forward),
  SELECT_PREVIOUS_CHARACTER(R.string.bk_gesture_select_previous_character),
  SELECT_NEXT_CHARACTER(R.string.bk_gesture_select_next_character),
  SELECT_PREVIOUS_WORD(R.string.bk_gesture_select_previous_word),
  SELECT_NEXT_WORD(R.string.bk_gesture_select_next_word),
  SELECT_PREVIOUS_LINE(R.string.bk_gesture_select_previous_line),
  SELECT_NEXT_LINE(R.string.bk_gesture_select_next_line),
  SELECT_ALL(R.string.bk_gesture_select_all),
  SELECT_CURRENT_TO_START(R.string.bk_gesture_select_current_to_start),
  SELECT_CURRENT_TO_END(R.string.bk_gesture_select_current_to_end),
  COPY(R.string.bk_gesture_copy),
  CUT(R.string.bk_gesture_cut),
  PASTE(R.string.bk_gesture_paste),
  PREVIOUS_MISSPELLED_WORD(R.string.bk_gesture_previous_misspelled_word, MOVE_CURSOR_BACKWARD),
  NEXT_MISSPELLED_WORD(R.string.bk_gesture_next_misspelled_word, MOVE_CURSOR_FORWARD),
  HEAR_PREVIOUS_SPELLING_SUGGESTION(
      R.string.bk_gesture_previous_suggestion, DELETE_CHARACTER_OR_PREVIOUS_ITEM),
  HEAR_NEXT_SPELLING_SUGGESTION(R.string.bk_gesture_next_suggestion, ADD_SPACE_OR_NEXT_ITEM),
  CONFIRM_SPELLING_SUGGESTION(R.string.bk_gesture_confirm_spelling_suggestion, ADD_NEWLINE),
  UNDO_SPELLING_SUGGESTION(R.string.bk_gesture_undo_spelling_suggestion, DELETE_WORD),
  NEXT_KEYBOARD(R.string.bk_gesture_switch_to_next_keyboard);

  @StringRes private final int descriptionRes;
  private final BrailleImeAction rootAction;

  BrailleImeAction(@StringRes int descriptionRes, BrailleImeAction rootAction) {
    this.rootAction = rootAction;
    this.descriptionRes = descriptionRes;
  }

  BrailleImeAction(@StringRes int descriptionRes) {
    this.rootAction = this;
    this.descriptionRes = descriptionRes;
  }

  /** Gets the description res of the {@link BrailleImeAction}. */
  @Override
  public String getDescriptionRes(Resources resources) {
    return resources.getString(descriptionRes);
  }

  /**
   * Gets the root of {@link BrailleImeAction}. If a root action is present, the gesture of this
   * action is the same as {@link #rootAction}. If the root action is absent, it returns itself.
   */
  public BrailleImeAction getRootAction() {
    return rootAction;
  }
}
