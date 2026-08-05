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

package com.google.android.accessibility.braille.interfaces;

/** Allows BrailleDisplay to signal to BrailleIme. */
public interface BrailleImeForBrailleDisplay {
  /** The result of actions. */
  enum Result {
    SUCCESS,
    REACH_EDGE,
    INVALID_INPUT_CONNECTION,
  }

  /** When a physical braille display is connected, informs BrailleIme. */
  void onBrailleDisplayConnected();

  /** When a physical braille display is disconnected, informs BrailleIme. */
  void onBrailleDisplayDisconnected();

  /** Performs an action on the BrailleIme. */
  boolean performImeAction(ImeAction action);

  /** Sends the input from a physical braille display to BrailleIme. */
  boolean sendBrailleDots(BrailleCharacter dots);

  /** Tells BrailleIme to move the cursor in the text field to a specified position. */
  boolean moveTextFieldCursor(int toIndex);

  /** Tells BrailleIme to move the cursor of holdings to a specified position. */
  boolean moveHoldingsCursor(int toIndex);

  /** Tells BrailleIme commit holdings to the editor. */
  void commitHoldings();

  /** Tells BrailleIme commit holdings to the editor and performs enter key action. */
  boolean commitHoldingsAndPerformEnterKeyAction();

  /** Tells BrailleIme to update result. */
  void updateResultForDisplay();

  /** Returns whether braille keyboard is activated. */
  boolean isBrailleKeyboardActivated();

  /** Handles keycode entered from braille display for BARD Mobile. */
  boolean handleBrailleKeyForBARDMobile(int keyCode);
}
