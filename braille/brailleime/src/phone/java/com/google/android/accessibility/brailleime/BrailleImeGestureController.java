/*
 * Copyright 2023 Google Inc.
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

package com.google.android.accessibility.brailleime;

import android.content.Context;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.brailleime.BrailleImeVibrator.VibrationType;
import com.google.android.accessibility.brailleime.analytics.BrailleImeAnalytics;
import com.google.android.accessibility.brailleime.input.DotHoldSwipe;
import com.google.android.accessibility.brailleime.input.Gesture;
import com.google.android.accessibility.brailleime.input.Swipe;
import com.google.common.annotations.VisibleForTesting;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.List;
import java.util.Optional;

/** Controller to handle incoming gestures to BrailleIme. */
public class BrailleImeGestureController {
  private static final String TAG = "BrailleImeGestureController";
  private final Context context;
  private final BrailleImeActor brailleImeActor;
  private BrailleImeAnalytics brailleImeAnalytics;

  public BrailleImeGestureController(Context context, BrailleImeActor brailleImeActor) {
    this.context = context;
    this.brailleImeActor = brailleImeActor;
    brailleImeAnalytics = BrailleImeAnalytics.getInstance(context);
  }

  /** Receives gesture swipe to perform action. */
  @CanIgnoreReturnValue
  public boolean performSwipeAction(Swipe swipe) {
    return performAction(swipe);
  }

  /** Receives gesture dot hold and swipe to perform action. */
  @CanIgnoreReturnValue
  public boolean performDotHoldAndSwipeAction(DotHoldSwipe dotHoldSwipe) {
    return performAction(dotHoldSwipe);
  }

  /** Receives gesture hold to perform action. */
  @CanIgnoreReturnValue
  public boolean performDotHoldAction(int pointersHeldCount) {
    if (pointersHeldCount == 1 || pointersHeldCount == 2 || pointersHeldCount == 3) {
      BrailleImeVibrator.getInstance(context).vibrate(VibrationType.HOLD);
      return true;
    }
    return false;
  }

  public String performTyping(BrailleCharacter brailleChar) {
    String result = brailleImeActor.performTyping(brailleChar);
    brailleImeAnalytics.logTotalBrailleCharCount(1);
    BrailleImeVibrator.getInstance(context).vibrate(VibrationType.BRAILLE_COMMISSION);
    return result;
  }

  private boolean performAction(Gesture gesture) {
    List<BrailleImeAction> currentActions = BrailleImeGestureAction.getAction(context, gesture);
    List<BrailleImeAction> defaultActions = BrailleImeGestureAction.getDefaultAction(gesture);
    // Use the root one.
    Optional<BrailleImeAction> optionalBrailleImeAction =
        currentActions.stream().filter(action -> action.equals(action.getRootAction())).findFirst();
    if (optionalBrailleImeAction.isEmpty()) {
      BrailleImeLog.d(TAG, "No BrailleImeAction");
      return false;
    }
    BrailleImeAction action = optionalBrailleImeAction.get();
    boolean result = brailleImeActor.performAction(action);
    performFeedback(action);
    performLogging(action, currentActions.equals(defaultActions));
    return result;
  }

  private void performFeedback(BrailleImeAction action) {
    switch (action) {
      case HIDE_KEYBOARD, SWITCH_KEYBOARD, HELP_AND_OTHER_ACTIONS, SUBMIT_TEXT ->
          BrailleImeVibrator.getInstance(context).vibrate(VibrationType.OTHER_GESTURES);
      case NEXT_GRANULARITY,
          PREVIOUS_GRANULARITY,
          ADD_SPACE_OR_NEXT_ITEM,
          DELETE_CHARACTER_OR_PREVIOUS_ITEM,
          MOVE_CURSOR_FORWARD,
          MOVE_CURSOR_BACKWARD ->
          BrailleImeVibrator.getInstance(context)
              .vibrate(VibrationType.SPACE_DELETE_OR_MOVE_CURSOR_OR_GRANULARITY);
      case ADD_NEWLINE, DELETE_WORD ->
          BrailleImeVibrator.getInstance(context).vibrate(VibrationType.NEWLINE_OR_DELETE_WORD);
      default -> {
        // do nothing.
      }
    }
  }

  /**
   * Logs the gesture action.
   *
   * @param action the gesture action.
   * @param customized whether the gesture action is customized.
   */
  private void performLogging(BrailleImeAction action, boolean customized) {
    switch (action) {
      case HIDE_KEYBOARD -> {
        brailleImeAnalytics.logGestureActionCloseKeyboard();
        brailleImeAnalytics.sendAllLogs();
      }
      case SWITCH_KEYBOARD -> brailleImeAnalytics.logGestureActionSwitchKeyboard();
      case NEXT_GRANULARITY ->
          brailleImeAnalytics.logGestureActionSwitchToNextEditingGranularity(customized);
      case PREVIOUS_GRANULARITY ->
          brailleImeAnalytics.logGestureActionSwitchToPreviousEditingGranularity(customized);
      case PASTE -> brailleImeAnalytics.logGestureActionPaste(customized);
      case CUT -> brailleImeAnalytics.logGestureActionCut(customized);
      case COPY -> brailleImeAnalytics.logGestureActionCopy(customized);
      case SUBMIT_TEXT -> {
        brailleImeAnalytics.logGestureActionSubmitText(customized);
        brailleImeAnalytics.collectSessionEvents();
      }
      case ADD_SPACE_OR_NEXT_ITEM -> brailleImeAnalytics.logGestureActionKeySpace(customized);
      case ADD_NEWLINE -> brailleImeAnalytics.logGestureActionKeyNewline(customized);
      case DELETE_CHARACTER_OR_PREVIOUS_ITEM ->
          brailleImeAnalytics.logGestureActionKeyDeleteCharacter(customized);
      case DELETE_WORD -> brailleImeAnalytics.logGestureActionKeyDeleteWord(customized);
      case MOVE_CURSOR_FORWARD -> brailleImeAnalytics.logGestureActionMoveCursorForward(customized);
      case MOVE_CURSOR_BACKWARD ->
          brailleImeAnalytics.logGestureActionMoveCursorBackward(customized);
      case NEXT_CHARACTER ->
          brailleImeAnalytics.logGestureActionMoveCursorForwardByCharacter(customized);
      case PREVIOUS_CHARACTER ->
          brailleImeAnalytics.logGestureActionMoveCursorBackwardByCharacter(customized);
      case NEXT_WORD -> brailleImeAnalytics.logGestureActionMoveCursorForwardByWord(customized);
      case PREVIOUS_WORD ->
          brailleImeAnalytics.logGestureActionMoveCursorBackwardByWord(customized);
      case NEXT_LINE -> brailleImeAnalytics.logGestureActionMoveCursorForwardByLine(customized);
      case PREVIOUS_LINE ->
          brailleImeAnalytics.logGestureActionMoveCursorBackwardByLine(customized);
      case BEGINNING_OF_PAGE ->
          brailleImeAnalytics.logGestureActionMoveCursorToBeginning(customized);
      case END_OF_PAGE -> brailleImeAnalytics.logGestureActionMoveCursorToEnd(customized);
      case SELECT_NEXT_CHARACTER ->
          brailleImeAnalytics.logGestureActionSelectNextCharacter(customized);
      case SELECT_PREVIOUS_CHARACTER ->
          brailleImeAnalytics.logGestureActionSelectPreviousCharacter(customized);
      case SELECT_NEXT_WORD -> brailleImeAnalytics.logGestureActionSelectNextWord(customized);
      case SELECT_PREVIOUS_WORD ->
          brailleImeAnalytics.logGestureActionSelectPreviousWord(customized);
      case SELECT_NEXT_LINE -> {
        // TODO: As the text selection for line granularity movement does not work, we
        // mask off the action of selecting text by line.
        // brailleImeAnalytics.logGestureActionSelectNextLine();
      }
      case SELECT_PREVIOUS_LINE -> {
        // TODO: As the text selection for line granularity movement does not work, we
        // mask off the action of selecting text by line.
        // brailleImeAnalytics.logGestureActionSelectPreviousLine();
      }
      case SELECT_ALL -> brailleImeAnalytics.logGestureActionSelectAllText(customized);
      case SELECT_CURRENT_TO_START ->
          brailleImeAnalytics.logGestureActionSelectCursorToStart(customized);
      case SELECT_CURRENT_TO_END ->
          brailleImeAnalytics.logGestureActionSelectCursorToEnd(customized);
      default -> {
        // do nothing.
      }
    }
  }

  @VisibleForTesting
  public void testing_setBrailleImeAnalytics(BrailleImeAnalytics brailleImeAnalytics) {
    this.brailleImeAnalytics = brailleImeAnalytics;
  }
}
