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

package com.google.android.accessibility.brailleime;

import static androidx.core.content.res.ResourcesCompat.ID_NULL;
import static com.google.android.accessibility.braille.common.BrailleImeAction.ADD_NEWLINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.ADD_SPACE_OR_NEXT_ITEM;
import static com.google.android.accessibility.braille.common.BrailleImeAction.BEGINNING_OF_PAGE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.CONFIRM_SPELLING_SUGGESTION;
import static com.google.android.accessibility.braille.common.BrailleImeAction.COPY;
import static com.google.android.accessibility.braille.common.BrailleImeAction.CUT;
import static com.google.android.accessibility.braille.common.BrailleImeAction.DELETE_CHARACTER_OR_PREVIOUS_ITEM;
import static com.google.android.accessibility.braille.common.BrailleImeAction.DELETE_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.END_OF_PAGE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.HEAR_NEXT_SPELLING_SUGGESTION;
import static com.google.android.accessibility.braille.common.BrailleImeAction.HEAR_PREVIOUS_SPELLING_SUGGESTION;
import static com.google.android.accessibility.braille.common.BrailleImeAction.HELP_AND_OTHER_ACTIONS;
import static com.google.android.accessibility.braille.common.BrailleImeAction.HIDE_KEYBOARD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.MOVE_CURSOR_BACKWARD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.MOVE_CURSOR_FORWARD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_GRANULARITY;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_MISSPELLED_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PASTE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_GRANULARITY;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_MISSPELLED_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_ALL;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_CURRENT_TO_END;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_CURRENT_TO_START;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_NEXT_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_NEXT_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_NEXT_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_PREVIOUS_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_PREVIOUS_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_PREVIOUS_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SUBMIT_TEXT;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SWITCH_KEYBOARD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.UNDO_SPELLING_SUGGESTION;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.BASIC;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.CURSOR_MOVEMENT;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.SPELL_CHECK;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.TEXT_SELECTION_AND_EDITING;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.CHARACTER;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.EDITING;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.GRANULARITY;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.KEYBOARD;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.LINE;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.NONE;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.PLACE_ON_PAGE;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.TYPING;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.WORD;
import static com.google.android.accessibility.brailleime.Utils.getCombinedGestureDescription;
import static com.google.common.collect.ImmutableList.toImmutableList;

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.StringRes;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.braille.interfaces.ImeAction;
import com.google.android.accessibility.brailleime.input.Gesture;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Commands supported by Braille keyboard. */
public class SupportedCommand {
  private static final ImmutableList<SupportedCommand> SUPPORTED_COMMANDS =
      ImmutableList.<SupportedCommand>builder()
          .add(new SupportedCommand(ADD_SPACE_OR_NEXT_ITEM, BASIC, TYPING))
          .add(new SupportedCommand(DELETE_CHARACTER_OR_PREVIOUS_ITEM, BASIC, TYPING))
          .add(new SupportedCommand(ADD_NEWLINE, BASIC, TYPING))
          .add(new SupportedCommand(DELETE_WORD, BASIC, TYPING))
          .add(new SupportedCommand(HIDE_KEYBOARD, BASIC, KEYBOARD, /* editable= */ false))
          .add(new SupportedCommand(SWITCH_KEYBOARD, BASIC, KEYBOARD, /* editable= */ false))
          .add(new SupportedCommand(SUBMIT_TEXT, BASIC, TYPING))
          .add(new SupportedCommand(HELP_AND_OTHER_ACTIONS, BASIC, KEYBOARD, /* editable= */ false))
          .add(new SupportedCommand(PREVIOUS_CHARACTER, CURSOR_MOVEMENT, CHARACTER))
          .add(new SupportedCommand(NEXT_CHARACTER, CURSOR_MOVEMENT, CHARACTER))
          .add(new SupportedCommand(PREVIOUS_WORD, CURSOR_MOVEMENT, WORD))
          .add(new SupportedCommand(NEXT_WORD, CURSOR_MOVEMENT, WORD))
          .add(new SupportedCommand(PREVIOUS_LINE, CURSOR_MOVEMENT, LINE))
          .add(new SupportedCommand(NEXT_LINE, CURSOR_MOVEMENT, LINE))
          .add(new SupportedCommand(BEGINNING_OF_PAGE, CURSOR_MOVEMENT, PLACE_ON_PAGE))
          .add(new SupportedCommand(END_OF_PAGE, CURSOR_MOVEMENT, PLACE_ON_PAGE))
          .add(new SupportedCommand(PREVIOUS_GRANULARITY, CURSOR_MOVEMENT, GRANULARITY))
          .add(new SupportedCommand(NEXT_GRANULARITY, CURSOR_MOVEMENT, GRANULARITY))
          .add(new SupportedCommand(MOVE_CURSOR_BACKWARD, CURSOR_MOVEMENT, GRANULARITY))
          .add(new SupportedCommand(MOVE_CURSOR_FORWARD, CURSOR_MOVEMENT, GRANULARITY))
          .add(
              new SupportedCommand(
                  SELECT_PREVIOUS_CHARACTER, TEXT_SELECTION_AND_EDITING, CHARACTER))
          .add(new SupportedCommand(SELECT_NEXT_CHARACTER, TEXT_SELECTION_AND_EDITING, CHARACTER))
          .add(new SupportedCommand(SELECT_PREVIOUS_WORD, TEXT_SELECTION_AND_EDITING, WORD))
          .add(new SupportedCommand(SELECT_NEXT_WORD, TEXT_SELECTION_AND_EDITING, WORD))
          .add(new SupportedCommand(SELECT_PREVIOUS_LINE, TEXT_SELECTION_AND_EDITING, LINE))
          .add(new SupportedCommand(SELECT_NEXT_LINE, TEXT_SELECTION_AND_EDITING, LINE))
          .add(new SupportedCommand(SELECT_ALL, TEXT_SELECTION_AND_EDITING, EDITING))
          .add(new SupportedCommand(SELECT_CURRENT_TO_START, TEXT_SELECTION_AND_EDITING, EDITING))
          .add(new SupportedCommand(SELECT_CURRENT_TO_END, TEXT_SELECTION_AND_EDITING, EDITING))
          .add(new SupportedCommand(COPY, TEXT_SELECTION_AND_EDITING, EDITING))
          .add(new SupportedCommand(CUT, TEXT_SELECTION_AND_EDITING, EDITING))
          .add(new SupportedCommand(PASTE, TEXT_SELECTION_AND_EDITING, EDITING))
          .add(new SupportedCommand(PREVIOUS_MISSPELLED_WORD, SPELL_CHECK))
          .add(new SupportedCommand(NEXT_MISSPELLED_WORD, SPELL_CHECK))
          .add(new SupportedCommand(HEAR_PREVIOUS_SPELLING_SUGGESTION, SPELL_CHECK))
          .add(new SupportedCommand(HEAR_NEXT_SPELLING_SUGGESTION, SPELL_CHECK))
          .add(new SupportedCommand(CONFIRM_SPELLING_SUGGESTION, SPELL_CHECK))
          .add(new SupportedCommand(UNDO_SPELLING_SUGGESTION, SPELL_CHECK))
          .build();
  private final BrailleImeAction action;
  private final Category category;
  private final SubCategory subCategory;
  private final boolean editable;

  public SupportedCommand(
      BrailleImeAction action, Category category, SubCategory subCategory, boolean editable) {
    if (!category.subCategoryList.contains(subCategory)) {
      throw new IllegalArgumentException(
          "Category does not have compatible SubCategory: " + subCategory);
    }
    this.action = action;
    this.category = category;
    this.subCategory = subCategory;
    this.editable = editable;
  }

  public SupportedCommand(BrailleImeAction action, Category category, SubCategory subCategory) {
    this(action, category, subCategory, /* editable= */ true);
  }

  public SupportedCommand(BrailleImeAction action, Category category, boolean editable) {
    this(action, category, NONE, editable);
  }

  public SupportedCommand(BrailleImeAction action, Category category) {
    this(action, category, NONE, /* editable= */ true);
  }

  /** Returns an immutable and order-sensitive {@link SupportedCommand} list. */
  public static ImmutableList<SupportedCommand> getSupportedCommands(Context context) {
    return SUPPORTED_COMMANDS.stream()
        .filter((SupportedCommand supportedCommand) -> supportedCommand.isAvailable(context))
        .collect(toImmutableList());
  }

  /** {@link SupportedCommand} category. */
  public enum Category {
    BASIC(R.string.braille_keyboard_basic_controls, TYPING, KEYBOARD),
    CURSOR_MOVEMENT(
        R.string.braille_keyboard_cursor_movement,
        R.string.braille_keyboard_cursor_movement_description,
        GRANULARITY,
        CHARACTER,
        WORD,
        LINE,
        PLACE_ON_PAGE),
    TEXT_SELECTION_AND_EDITING(
        R.string.braille_keyboard_text_selection_and_editing, CHARACTER, WORD, LINE, EDITING),
    SPELL_CHECK(
        R.string.braille_keyboard_spell_check,
        R.string.braille_keyboard_spell_check_description,
        NONE),
    ;
    @StringRes private final int titleRes;
    @StringRes private final int descriptionRes;
    private final ImmutableList<SubCategory> subCategoryList;

    Category(@StringRes int titleRes, @StringRes int descriptionRes, SubCategory... subCategories) {
      subCategoryList = ImmutableList.copyOf(Arrays.asList(subCategories));
      this.titleRes = titleRes;
      this.descriptionRes = descriptionRes;
    }

    Category(@StringRes int titleRes, SubCategory... subCategories) {
      this(titleRes, ID_NULL, subCategories);
    }

    /** Gets the {@link SubCategory} list belong to the {@link Category}. */
    public List<SubCategory> getSubCategories() {
      return new ArrayList<>(subCategoryList);
    }

    /** Gets the title of the {@link Category}. */
    public String getTitle(Resources resources) {
      return resources.getString(titleRes);
    }

    /** Gets the description of the {@link Category}. */
    public String getDescription(Resources resources) {
      return descriptionRes == ID_NULL ? "" : resources.getString(descriptionRes);
    }
  }

  /** {@link SupportedCommand} sub-category. */
  public enum SubCategory {
    NONE(),
    CHARACTER(R.string.bk_pref_category_title_character),
    WORD(R.string.bk_pref_category_title_word),
    LINE(R.string.bk_pref_category_title_line),
    PLACE_ON_PAGE(R.string.bk_pref_category_title_place_on_page),
    EDITING(R.string.bk_pref_category_title_editing),
    GRANULARITY(R.string.bk_pref_category_title_granularity),
    TYPING(R.string.bk_pref_category_title_typing),
    KEYBOARD(R.string.bk_pref_category_title_keyboard);

    @StringRes private final int nameRes;

    SubCategory() {
      this(ID_NULL);
    }

    SubCategory(@StringRes int nameRes) {
      this.nameRes = nameRes;
    }

    /** Gets the key of the {@link SubCategory}. */
    public String getName(Resources resources) {
      if (nameRes == ID_NULL) {
        return "";
      }
      return resources.getString(nameRes);
    }
  }

  /** Gets the {@link ImeAction}. */
  public BrailleImeAction getBrailleImeAction() {
    return action;
  }

  /** Gets the {@link Gesture} for the {@link BrailleImeAction}. */
  public List<Gesture> getBrailleImeGesture(Context context) {
    return BrailleImeGestureAction.getGesture(context, getBrailleImeAction());
  }

  /** Gets the category of the action. */
  public Category getCategory() {
    return category;
  }

  /** Gets the subcategory of the action. */
  public SubCategory getSubCategory() {
    return subCategory;
  }

  /** Gets the description of the action. */
  public String getActionDescription(Context context) {
    return getBrailleImeAction().getDescriptionRes(context.getResources());
  }

  /** Gets the description of the gesture. */
  public String getGestureDescription(Context context) {
    List<Gesture> gestures = getBrailleImeGesture(context);
    if (gestures.isEmpty()) {
      return context.getResources().getString(R.string.bk_gesture_unassigned);
    }
    return getCombinedGestureDescription(context, gestures);
  }

  /** Returns if the command is editable. */
  public boolean isEditable() {
    return editable;
  }

  private boolean isAvailable(Context context) {
    if (action == SELECT_CURRENT_TO_START || action == SELECT_CURRENT_TO_END) {
      return FeatureFlagReader.useSelectCurrentToStartOrEnd(context);
    }
    return true;
  }
}
