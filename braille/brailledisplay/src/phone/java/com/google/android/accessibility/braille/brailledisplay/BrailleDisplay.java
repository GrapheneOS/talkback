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

package com.google.android.accessibility.braille.brailledisplay;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController;
import com.google.android.accessibility.braille.brailledisplay.platform.BrailleDisplayManager;
import com.google.android.accessibility.braille.common.BrailleCommonTalkBackSpeaker;
import com.google.android.accessibility.braille.interfaces.BrailleDisplayForBrailleIme;
import com.google.android.accessibility.braille.interfaces.BrailleDisplayForTalkBack;
import com.google.android.accessibility.braille.interfaces.BrailleImeForBrailleDisplay;
import com.google.android.accessibility.braille.interfaces.TalkBackForBrailleCommon;
import com.google.android.accessibility.braille.interfaces.TalkBackForBrailleDisplay;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/** Entry point between TalkBack and the braille display feature. */
public class BrailleDisplay implements BrailleDisplayForTalkBack, BrailleDisplayForBrailleIme {
  private static final String TAG = "BrailleDisplay";
  private boolean isRunning;
  private BrailleDisplayManager brailleDisplayManager;
  private final BdController controller;
  private final AccessibilityService accessibilityService;

  /** Provides BrailleIme callbacks for BrailleDisplay. */
  public interface BrailleImeProvider {
    BrailleImeForBrailleDisplay getBrailleImeForBrailleDisplay();
  }

  public BrailleDisplay(
      AccessibilityService accessibilityService,
      TalkBackForBrailleDisplay talkBackForBrailleDisplay,
      TalkBackForBrailleCommon talkBackForBrailleCommon,
      BrailleImeProvider brailleImeProvider) {
    this.controller =
        new BdController(
            accessibilityService,
            talkBackForBrailleDisplay,
            talkBackForBrailleCommon,
            brailleImeProvider);
    this.accessibilityService = accessibilityService;
    this.brailleDisplayManager = new BrailleDisplayManager(accessibilityService, controller);
    BrailleCommonTalkBackSpeaker.getInstance().initialize(talkBackForBrailleCommon);
  }

  /** Starts braille display. */
  @Override
  public void start() {
    BrailleDisplayLog.d(TAG, "start");
    brailleDisplayManager.setAccessibilityServiceContextProvider(() -> accessibilityService);
    brailleDisplayManager.onServiceStarted();
    isRunning = true;
  }

  /** Stops braille display. */
  @Override
  public void stop() {
    BrailleDisplayLog.d(TAG, "stop");
    brailleDisplayManager.onServiceStopped();
    brailleDisplayManager.setAccessibilityServiceContextProvider(() -> null);
    brailleDisplayManager = null;
    isRunning = false;
  }

  /** Notifies receiving accessibility event. */
  @Override
  public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    if (isRunning) {
      brailleDisplayManager.onAccessibilityEvent(accessibilityEvent);
    }
  }

  @CanIgnoreReturnValue
  @Override
  public boolean onKeyEvent(KeyEvent keyEvent) {
    if (isRunning) {
      return brailleDisplayManager.onKeyEvent(keyEvent);
    }
    return false;
  }

  @Override
  public void onReadingControlSettingsChanged(CharSequence readingControlDescription) {
    if (isRunning) {
      controller.onReadingControlSettingsChanged(readingControlDescription);
    }
  }

  @Override
  public void onReadingControlValueChanged() {
    if (isRunning) {
      controller.onReadingControlValueChanged();
    }
  }

  @Override
  public void switchBrailleDisplayOnOrOff() {
    if (isRunning) {
      controller.switchBrailleDisplayOnOrOff();
    }
  }

  @Override
  public void toggleBrailleContractedMode() {
    if (isRunning) {
      controller.toggleBrailleContractedMode();
    }
  }

  @Override
  public void toggleBrailleOnScreenOverlay() {
    if (isRunning) {
      controller.toggleBrailleOnScreenOverlay();
    }
  }

  @Override
  public void onImeVisibilityChanged(boolean visible) {
    if (isRunning) {
      controller.getBrailleDisplayForBrailleIme().onImeVisibilityChanged(visible);
    }
  }

  @Override
  public void showOnDisplay(ResultForDisplay result) {
    if (isRunning) {
      controller.getBrailleDisplayForBrailleIme().showOnDisplay(result);
    }
  }

  @Override
  public boolean isBrailleDisplayConnectedAndNotSuspended() {
    if (isRunning) {
      return controller.getBrailleDisplayForBrailleIme().isBrailleDisplayConnectedAndNotSuspended();
    }
    return false;
  }

  @Override
  public void suspendInFavorOfBrailleKeyboard() {
    if (isRunning) {
      controller.getBrailleDisplayForBrailleIme().suspendInFavorOfBrailleKeyboard();
    }
  }
}
