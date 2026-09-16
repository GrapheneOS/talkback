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

package com.google.android.accessibility.braille.brailledisplay.controller;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.FeatureFlagReader;
import com.google.android.accessibility.braille.brailledisplay.SupportedCommand;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorIme;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.BrailleKeyBindingUtils;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.braille.common.FeedbackManager;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;

/** An event consumer interacts with the input method service. */
class EditorConsumer implements EventConsumer {
  private static final String TAG = "EditorConsumer";

  /** Accessibility event types that associated with the editor. */
  private static final int UPDATE_EVENT_MASK =
      AccessibilityEvent.TYPE_VIEW_FOCUSED
          | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
          | AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
          | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED;

  private final Context context;
  private final BehaviorIme behaviorIme;
  private final FeedbackManager feedbackManager;

  /** Public constructor for general use. */
  public EditorConsumer(Context context, FeedbackManager feedbackManager, BehaviorIme behaviorIme) {
    this.context = context;
    this.feedbackManager = feedbackManager;
    this.behaviorIme = behaviorIme;
  }

  @Override
  public void onActivate() {}

  @Override
  public void onDeactivate() {
    behaviorIme.commitHoldings();
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    BrailleDisplayLog.v(TAG, "onAccessibilityEvent");
    // To maintain the position of the panning page, filter out irrelevant Windows updates,
    // such as clock updates, that could potentially disrupt it.
    if ((event.getEventType() & UPDATE_EVENT_MASK) != 0) {
      behaviorIme.triggerUpdateDisplay();
    }
  }

  @Override
  public boolean onMappedInputEvent(BrailleInputEvent event) {
    BrailleDisplayLog.v(TAG, "onMappedInputEvent: " + event.getCommand());
    boolean success = false;
    switch (event.getCommand()) {
      case BrailleInputEvent.CMD_KEY_DEL ->
          success =
              behaviorIme.performImeAction(BrailleImeAction.DELETE_CHARACTER_OR_PREVIOUS_ITEM);
      case BrailleInputEvent.CMD_DEL_WORD ->
          success = behaviorIme.performImeAction(BrailleImeAction.DELETE_WORD);
      case BrailleInputEvent.CMD_BRAILLE_KEY ->
          success = behaviorIme.sendBrailleDots(event.getArgument());
      case BrailleInputEvent.CMD_NAV_TOP_OR_KEY_ACTIVATE, BrailleInputEvent.CMD_NAV_TOP ->
          success = behaviorIme.performImeAction(BrailleImeAction.BEGINNING_OF_PAGE);
      case BrailleInputEvent.CMD_NAV_BOTTOM_OR_KEY_ACTIVATE, BrailleInputEvent.CMD_NAV_BOTTOM ->
          success = behaviorIme.performImeAction(BrailleImeAction.END_OF_PAGE);
      case BrailleInputEvent.CMD_NAV_LINE_PREVIOUS ->
          // Line navigation moves by paragraph since there's no way
          // of knowing the line extents in the edit text.
          success = behaviorIme.performImeAction(BrailleImeAction.PREVIOUS_LINE);
      case BrailleInputEvent.CMD_NAV_LINE_NEXT ->
          success = behaviorIme.performImeAction(BrailleImeAction.NEXT_LINE);
      case BrailleInputEvent.CMD_NAV_CHARACTER_PREVIOUS ->
          success = behaviorIme.performImeAction(BrailleImeAction.PREVIOUS_CHARACTER);
      case BrailleInputEvent.CMD_NAV_CHARACTER_NEXT ->
          success = behaviorIme.performImeAction(BrailleImeAction.NEXT_CHARACTER);
      case BrailleInputEvent.CMD_NAV_WORD_PREVIOUS ->
          success = behaviorIme.performImeAction(BrailleImeAction.PREVIOUS_WORD);
      case BrailleInputEvent.CMD_NAV_WORD_NEXT ->
          success = behaviorIme.performImeAction(BrailleImeAction.NEXT_WORD);
      case BrailleInputEvent.CMD_KEY_ENTER, BrailleInputEvent.CMD_ACTIVATE_CURRENT ->
          success = behaviorIme.performEnterKeyAction();
      case BrailleInputEvent.CMD_ROUTE -> success = behaviorIme.moveCursor(event.getArgument());
      case BrailleInputEvent.CMD_SELECTION_CUT ->
          success = behaviorIme.performImeAction(BrailleImeAction.CUT);
      case BrailleInputEvent.CMD_SELECTION_COPY ->
          success = behaviorIme.performImeAction(BrailleImeAction.COPY);
      case BrailleInputEvent.CMD_SELECTION_PASTE ->
          success = behaviorIme.performImeAction(BrailleImeAction.PASTE);
      case BrailleInputEvent.CMD_SELECTION_SELECT_ALL ->
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_ALL);
      case BrailleInputEvent.CMD_SELECTION_SELECT_CURRENT_TO_START -> {
        if (FeatureFlagReader.useSelectCurrentToStartOrEnd(context)) {
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_CURRENT_TO_START);
        }
      }
      case BrailleInputEvent.CMD_SELECTION_SELECT_CURRENT_TO_END -> {
        if (FeatureFlagReader.useSelectCurrentToStartOrEnd(context)) {
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_CURRENT_TO_END);
        }
      }
      case BrailleInputEvent.CMD_SELECT_PREVIOUS_CHARACTER ->
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_PREVIOUS_CHARACTER);
      case BrailleInputEvent.CMD_SELECT_NEXT_CHARACTER ->
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_NEXT_CHARACTER);
      case BrailleInputEvent.CMD_SELECT_PREVIOUS_WORD ->
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_PREVIOUS_WORD);
      case BrailleInputEvent.CMD_SELECT_NEXT_WORD ->
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_NEXT_WORD);
      case BrailleInputEvent.CMD_SELECT_PREVIOUS_LINE ->
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_PREVIOUS_LINE);
      case BrailleInputEvent.CMD_SELECT_NEXT_LINE ->
          success = behaviorIme.performImeAction(BrailleImeAction.SELECT_NEXT_LINE);
      default -> {
        // Forbid navigation commands while editing. Instead, we extract the dots and act as
        // typing.
        SupportedCommand command = BrailleKeyBindingUtils.convertToCommand(context, event);
        if (command != null) {
          BrailleCharacter character = command.getPressDot();
          if (character != null
              && !character.equals(BrailleCharacter.EMPTY_CELL)
              && command.hasSpace()) {
            // Consumed event since they are available while editing.
            return false;
          } else {
            success = behaviorIme.sendBrailleDots(character.toInt());
          }
        } else {
          return false;
        }
      }
    }
    if (!success) {
      feedbackManager.emitFeedback(FeedbackManager.Type.COMMAND_FAILED);
    }
    // Always return true because we own these actions.
    return true;
  }

  @Override
  public void onReadingControlValueChanged() {
    behaviorIme.commitHoldings();
  }
}
