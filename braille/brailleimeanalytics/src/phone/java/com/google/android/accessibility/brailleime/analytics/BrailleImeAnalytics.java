/*
 * Copyright (C) 2019 Google Inc.
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
package com.google.android.accessibility.brailleime.analytics;

import android.content.Context;

/** Stub implementation of analytics used by the open source variant. */
public class BrailleImeAnalytics {
  /** Context menu selections. */
  public enum ContextMenuSelections {
    UNSPECIFIED_OPTION,
    SWITCH_CONTRACTED_STATUS,
    SEE_ALL_ACTIONS,
    CALIBRATION,
    TUTORIAL_OPEN,
    TUTORIAL_FINISH,
    GO_TO_SETTINGS,
    TYPING_LANGUAGE
  }

  /** Calibration triggered type. */
  public enum CalibrationTriggeredType {
    UNSPECIFIED_FINGER,
    FIVE_FINGER,
    SIX_FINGER,
    SEVEN_FINGER,
    EIGHT_FINGER,
    MANUAL
  }

  private static BrailleImeAnalytics instance;

  public static BrailleImeAnalytics getInstance(Context context) {
    if (instance == null) {
      instance = new BrailleImeAnalytics(context.getApplicationContext());
    }
    return instance;
  }

  private BrailleImeAnalytics(Context context) {}

  /** Stub implementation does nothing. */
  public void sendAllLogs() {}

  /** Stub implementation does nothing. */
  public void startSession() {}

  /** Stub implementation does nothing. */
  public void collectSessionEvents() {}

  /** Stub implementation does nothing. */
  public void logGestureActionKeySpace(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionKeyDeleteCharacter(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionKeyDeleteWord(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionKeyNewline(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSubmitText(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionCloseKeyboard() {}

  /** Stub implementation does nothing. */
  public void logGestureActionSwitchKeyboard() {}

  /** Stub implementation does nothing. */
  public void logGestureActionOpenOptionsMenu() {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorForward(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorBackward(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logContractedToggle(boolean contractedModeOn) {}

  /** Stub implementation does nothing. */
  public void logTalkBackOffDialogDisplay() {}

  /** Stub implementation does nothing. */
  public void logFewTouchPointsDialogDisplay() {}

  /** Stub implementation does nothing. */
  public void logTotalBrailleCharCount(int numOfChar) {}

  /** Stub implementation does nothing. */
  public void logTutorialFinishedByTutorialCompleted() {}

  /** Stub implementation does nothing. */
  public void logTutorialFinishedByTalkbackStop() {}

  /** Stub implementation does nothing. */
  public void logTutorialFinishedByLaunchSettings() {}

  /** Stub implementation does nothing. */
  public void logTutorialFinishedBySwitchToNextInputMethod() {}

  /** Stub implementation does nothing. */
  public void logContextMenuOptionCount(ContextMenuSelections contextMenuSelections) {}

  /** Logs for finishing calibration. */
  public void logCalibrationFinish(
      CalibrationTriggeredType calibrationType, boolean tabletop, boolean isEight) {}

  /** Logs for calibration failure. */
  public void logCalibrationFailed(
      CalibrationTriggeredType calibrationType, boolean tabletop, boolean isEight) {}

  /** Logs for starting calibration. */
  public void logCalibrationStarted(
      CalibrationTriggeredType calibrationType, boolean tabletop, boolean isEight) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorForwardByCharacter(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorBackwardByCharacter(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorForwardByWord(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorBackwardByWord(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorForwardByLine(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorBackwardByLine(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorToBeginning(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionMoveCursorToEnd(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectNextCharacter(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectPreviousCharacter(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectNextWord(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectPreviousWord(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectNextLine(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectPreviousLine(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectAllText(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectCursorToStart(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSelectCursorToEnd(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionCut(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionCopy(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionPaste(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSwitchToNextEditingGranularity(boolean customized) {}

  /** Stub implementation does nothing. */
  public void logGestureActionSwitchToPreviousEditingGranularity(boolean customized) {}
}
