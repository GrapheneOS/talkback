/*
 * Copyright (C) 2012 Google Inc.
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

import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_ACTIVATE_CURRENT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_ARIA_LANDMARK_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_ARIA_LANDMARK_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_ARIA_LANDMARK_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_BRAILLE_DISPLAY_SETTINGS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_BRAILLE_KEY;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_BUTTON_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_BUTTON_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_BUTTON_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_CHECKBOX_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_CHECKBOX_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_CHECKBOX_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_COMBO_BOX_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_COMBO_BOX_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_COMBO_BOX_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_CONTROL_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_CONTROL_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_CONTROL_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_EDIT_FIELD_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_EDIT_FIELD_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_EDIT_FIELD_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_GRAPHIC_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_GRAPHIC_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_GRAPHIC_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_1_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_1_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_1_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_2_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_2_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_2_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_3_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_3_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_3_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_4_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_4_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_4_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_5_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_5_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_5_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_6_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_6_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_6_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HEADING_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_HELP;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_KEY_ENTER;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LINK_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LINK_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LINK_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LIST_ITEM_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LIST_ITEM_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LIST_ITEM_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LIST_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LIST_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_LIST_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_BOTTOM;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_BOTTOM_OR_KEY_ACTIVATE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_ITEM_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_ITEM_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_LINE_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_LINE_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_TOP;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_TOP_OR_KEY_ACTIVATE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_WINDOW_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NAV_WINDOW_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_NEXT_READING_CONTROL;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_PREVIOUS_READING_CONTROL;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_RADIO_BUTTON_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_RADIO_BUTTON_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_RADIO_BUTTON_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_ROUTE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SCROLL_BACKWARD;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SCROLL_FORWARD;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_STOP_READING;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SWITCH_TO_NEXT_INPUT_LANGUAGE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_SWITCH_TO_NEXT_OUTPUT_LANGUAGE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TABLE_COL_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TABLE_COL_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TABLE_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TABLE_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TABLE_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TABLE_ROW_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TABLE_ROW_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TALKBACK_SETTINGS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_BROWSE_MODE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_VOICE_FEEDBACK;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TURN_OFF_BRAILLE_DISPLAY;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_UNVISITED_LINK_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_UNVISITED_LINK_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_UNVISITED_LINK_PREVIOUS;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_VISITED_LINK_NEXT;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_VISITED_LINK_NEXT_IN_BROWSE;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_VISITED_LINK_PREVIOUS;
import static com.google.android.accessibility.braille.common.translate.EditBufferUtils.NO_CURSOR;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayImeUnavailableActivity;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.FeatureFlagReader;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.analytics.BrailleDisplayAnalytics;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorDisplayer;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorFocus;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorIme;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorNodeText;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorScreenReader;
import com.google.android.accessibility.braille.brailledisplay.controller.CellsContentConsumer.Reason;
import com.google.android.accessibility.braille.brailledisplay.platform.PersistentStorage;
import com.google.android.accessibility.braille.brailledisplay.settings.BrailleDisplaySettingsActivity;
import com.google.android.accessibility.braille.brailledisplay.settings.KeyBindingsActivity;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BrailleCommonTalkBackSpeaker;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.FeedbackManager;
import com.google.android.accessibility.braille.common.TalkBackSpeaker.AnnounceType;
import com.google.android.accessibility.braille.common.translate.BrailleLanguages.Code;
import com.google.android.accessibility.braille.interfaces.ScreenReaderActionPerformer.ScreenReaderAction;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoRef;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils.Constants;
import com.google.android.accessibility.utils.PerformActionUtils;
import com.google.android.accessibility.utils.material.MaterialComponentUtils;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategyUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Optional;

/** An event consumer digests calls not handled by the hosted IME. */
class DefaultConsumer implements EventConsumer {
  private static final String TAG = "DefaultNavigationMode";
  private static final int LOG_LANGUAGE_CHANGE_DELAY_MS =
      10000; // After the lowest TTS speed announcement ends.
  private static final int INPUT = 1;
  private static final int OUTPUT = 2;
  private final Context context;
  private final CellsContentConsumer cellsContentConsumer;
  private final NodeBrailler nodeBrailler;
  private final FeedbackManager feedbackManager;
  private final BehaviorScreenReader behaviorScreenReaderAction;
  private final BehaviorFocus behaviorFocus;
  private final BehaviorDisplayer behaviorDisplayer;
  private final BehaviorIme behaviorIme;
  private final AccessibilityNodeInfoRef lastFocusedNode = new AccessibilityNodeInfoRef();
  private final Handler loggingHandler;
  private AlertDialog turnOffBdDialog;
  private AlertDialog browseModeDialog;

  public DefaultConsumer(
      Context context,
      CellsContentConsumer cellsContentConsumer,
      FeedbackManager feedbackManager,
      BehaviorNodeText behaviorNodeText,
      BehaviorFocus behaviorFocus,
      BehaviorScreenReader behaviorScreenReaderAction,
      BehaviorDisplayer behaviorDisplayer,
      BehaviorIme behaviorIme) {
    this.context = context;
    this.behaviorScreenReaderAction = behaviorScreenReaderAction;
    this.behaviorFocus = behaviorFocus;
    this.behaviorDisplayer = behaviorDisplayer;
    this.behaviorIme = behaviorIme;
    this.cellsContentConsumer = cellsContentConsumer;
    this.nodeBrailler = new NodeBrailler(context, behaviorNodeText);
    this.feedbackManager = feedbackManager;
    loggingHandler = new LoggingHandler(context);
  }

  private boolean itemPrevious() {
    return feedbackManager.emitOnFailure(
        behaviorScreenReaderAction.performAction(ScreenReaderAction.PREVIOUS_ITEM),
        FeedbackManager.Type.NAVIGATE_OUT_OF_BOUNDS);
  }

  private boolean itemNext() {
    return feedbackManager.emitOnFailure(
        behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_ITEM),
        FeedbackManager.Type.NAVIGATE_OUT_OF_BOUNDS);
  }

  @CanIgnoreReturnValue
  @Override
  public boolean onMappedInputEvent(BrailleInputEvent event) {
    BrailleDisplayLog.v(TAG, "onMappedInputEvent: " + event.getCommand());
    // Switch to the braille display keyboard when typing on the braille display and braille
    // keyboard is not default anytime.
    if (isTriggerImeSwitchCommands(event)) {
      trySwitchIme();
    }
    boolean success = false;
    if (FeatureFlagReader.useBrowseMode(context) || !isBrowseModeCommand(event.getCommand())) {
      switch (event.getCommand()) {
        case CMD_BRAILLE_KEY ->
            success = behaviorFocus.handleBrailleKeyWithoutKeyboardOpen(event.getArgument());
        case CMD_NAV_ITEM_PREVIOUS -> success = itemPrevious();
        case CMD_NAV_ITEM_NEXT -> success = itemNext();
        case CMD_NAV_LINE_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.FOCUS_PREVIOUS_LINE);
        case CMD_NAV_LINE_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.FOCUS_NEXT_LINE);
        case CMD_TABLE_ROW_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.TABLE_NEXT_ROW);
        case CMD_TABLE_ROW_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.TABLE_PREVIOUS_ROW);
        case CMD_TABLE_COL_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.TABLE_NEXT_COL);
        case CMD_TABLE_COL_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.TABLE_PREVIOUS_COL);
        case CMD_NAV_TOP_OR_KEY_ACTIVATE,
            CMD_NAV_BOTTOM_OR_KEY_ACTIVATE,
            CMD_KEY_ENTER,
            CMD_ACTIVATE_CURRENT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.CLICK_CURRENT);
        case CMD_ROUTE -> {
          Optional<ClickableSpan[]> clickableSpans =
              cellsContentConsumer.getClickableSpans(event.getArgument());
          if (clickableSpans.isPresent() && clickableSpans.get().length > 0) {
            success = activateClickableSpan(context, clickableSpans.get()[0]);
          }
          if (!success) {
            AccessibilityNodeInfoCompat node =
                cellsContentConsumer.getAccessibilityNode(event.getArgument());
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.CLICK_NODE, node);
            int index = cellsContentConsumer.getTextIndexInWhole(event.getArgument());
            if (AccessibilityNodeInfoUtils.isTextSelectable(node) && index != NO_CURSOR) {
              // TODO: handle selectable text too.
              final Bundle args = new Bundle();
              args.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_START_INT, index);
              args.putInt(AccessibilityNodeInfoCompat.ACTION_ARGUMENT_SELECTION_END_INT, index);
              success =
                  PerformActionUtils.performAction(
                      node,
                      AccessibilityNodeInfoCompat.ACTION_SET_SELECTION,
                      args,
                      /* eventId= */ null);
            }
          }
        }
        case CMD_NEXT_READING_CONTROL ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_READING_CONTROL);
        case CMD_PREVIOUS_READING_CONTROL ->
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction.PREVIOUS_READING_CONTROL);
        case CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD ->
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction
                        .NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD);
        case CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD ->
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction
                        .NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD);
        case CMD_SCROLL_FORWARD ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.SCROLL_FORWARD);
        case CMD_SCROLL_BACKWARD ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.SCROLL_BACKWARD);
        case CMD_NAV_TOP ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NAVIGATE_TO_TOP);
        case CMD_NAV_BOTTOM ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.NAVIGATE_TO_BOTTOM);
        case CMD_NAV_WINDOW_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_WINDOW);
        case CMD_NAV_WINDOW_PREVIOUS ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.PREVIOUS_WINDOW);
        case CMD_SWITCH_TO_NEXT_OUTPUT_LANGUAGE -> {
          if (BrailleUserPreferences.readAvailablePreferredCodes(context).size() > 1) {
            Code nextCode = BrailleUserPreferences.getNextOutputCode(context);
            String userFacingName = nextCode.getUserFacingName(context);
            BrailleUserPreferences.writeCurrentActiveOutputCode(context, nextCode);
            displayTimedMessage(userFacingName);
            // Delay logging because user might go through a long list of languages before reaching
            // his desired language.
            loggingHandler.removeMessages(OUTPUT);
            loggingHandler.sendMessageDelayed(
                loggingHandler.obtainMessage(OUTPUT), LOG_LANGUAGE_CHANGE_DELAY_MS);
            BrailleUserPreferences.writeSwitchContactedCount(context);
            BrailleCommonTalkBackSpeaker.getInstance()
                .speak(
                    getSwitchLanguageAnnounceText(
                        context.getString(
                            R.string.bd_switch_reading_language_announcement, userFacingName)),
                    AnnounceType.INTERRUPT);
            success = true;
          }
        }
        case CMD_SWITCH_TO_NEXT_INPUT_LANGUAGE -> {
          if (BrailleUserPreferences.readAvailablePreferredCodes(context).size() > 1) {
            Code nextCode = BrailleUserPreferences.getNextInputCode(context);
            String userFacingName = nextCode.getUserFacingName(context);
            BrailleUserPreferences.writeCurrentActiveInputCode(context, nextCode);
            displayTimedMessage(userFacingName);
            // Delay logging because user might go through a long list of languages before reaching
            // his desired language.
            loggingHandler.removeMessages(INPUT);
            loggingHandler.sendMessageDelayed(
                loggingHandler.obtainMessage(INPUT), LOG_LANGUAGE_CHANGE_DELAY_MS);
            BrailleUserPreferences.writeSwitchContactedCount(context);
            BrailleCommonTalkBackSpeaker.getInstance()
                .speak(
                    getSwitchLanguageAnnounceText(
                        context.getString(
                            R.string.bd_switch_typing_language_announcement, userFacingName)),
                    AnnounceType.INTERRUPT);
            success = true;
          }
        }
        case CMD_HELP -> success = startHelpActivity();
        case CMD_BRAILLE_DISPLAY_SETTINGS -> success = startBrailleDisplayActivity();
        case CMD_TURN_OFF_BRAILLE_DISPLAY -> {
          showTurnOffBdDialog();
          success = true;
        }
        case CMD_TOGGLE_VOICE_FEEDBACK -> {
          success =
              behaviorScreenReaderAction.performAction(ScreenReaderAction.TOGGLE_VOICE_FEEDBACK);
          displayTimedMessage(
              context.getString(
                  behaviorScreenReaderAction.getVoiceFeedbackEnabled()
                      ? R.string.bd_voice_feedback_unmute
                      : R.string.bd_voice_feedback_mute));
        }
        case CMD_TALKBACK_SETTINGS -> success = startTalkBackSettingsActivity();
        case CMD_STOP_READING ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.STOP_READING);
        case CMD_TOGGLE_BROWSE_MODE -> {
          success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_BROWSE_MODE);
          if (success) {
            String message =
                context.getString(
                    isBrowseMode() ? R.string.bd_browse_mode_on : R.string.bd_browse_mode_off);
            displayTimedMessage(message);
          } else if (behaviorFocus.isBrowseModeFlagEnabled()) {
            showEnhancedKeymapDialog();
          }
        }
        case CMD_ARIA_LANDMARK_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_LANDMARK);
        case CMD_ARIA_LANDMARK_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_LANDMARK);
          }
        }
        case CMD_ARIA_LANDMARK_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_LANDMARK);
        case CMD_GRAPHIC_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_GRAPHIC);
        case CMD_GRAPHIC_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_GRAPHIC);
          }
        }
        case CMD_GRAPHIC_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_GRAPHIC);
        case CMD_LIST_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_LIST);
        case CMD_LIST_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_LIST);
          }
        }
        case CMD_LIST_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_LIST);
        case CMD_LIST_ITEM_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_LIST_ITEM);
        case CMD_LIST_ITEM_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_LIST_ITEM);
          }
        }
        case CMD_LIST_ITEM_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_LIST_ITEM);
        case CMD_TABLE_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_TABLE);
        case CMD_TABLE_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_TABLE);
          }
        }
        case CMD_TABLE_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_TABLE);
        case CMD_HEADING_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_HEADING);
        case CMD_HEADING_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_HEADING);
          }
        }
        case CMD_HEADING_PREVIOUS ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.PREVIOUS_HEADING);
        case CMD_HEADING_1_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_1);
        case CMD_HEADING_1_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_1);
          }
        }
        case CMD_HEADING_1_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_HEADING_1);
        case CMD_HEADING_2_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_2);
        case CMD_HEADING_2_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_2);
          }
        }
        case CMD_HEADING_2_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_HEADING_2);
        case CMD_HEADING_3_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_3);
        case CMD_HEADING_3_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_3);
          }
        }
        case CMD_HEADING_3_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_HEADING_3);
        case CMD_HEADING_4_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_4);
        case CMD_HEADING_4_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_4);
          }
        }
        case CMD_HEADING_4_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_HEADING_4);
        case CMD_HEADING_5_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_5);
        case CMD_HEADING_5_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_5);
          }
        }
        case CMD_HEADING_5_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_HEADING_5);
        case CMD_HEADING_6_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_6);
        case CMD_HEADING_6_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_HEADING_6);
          }
        }
        case CMD_HEADING_6_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_HEADING_6);
        case CMD_CONTROL_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_CONTROL);
        case CMD_CONTROL_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_CONTROL);
          }
        }
        case CMD_CONTROL_PREVIOUS ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.PREVIOUS_CONTROL);
        case CMD_BUTTON_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_BUTTON);
        case CMD_BUTTON_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_BUTTON);
          }
        }
        case CMD_BUTTON_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_BUTTON);
        case CMD_CHECKBOX_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_CHECKBOX);
        case CMD_CHECKBOX_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_CHECKBOX);
          }
        }
        case CMD_CHECKBOX_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_CHECKBOX);
        case CMD_RADIO_BUTTON_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_RADIO_BUTTON);
        case CMD_RADIO_BUTTON_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_RADIO_BUTTON);
          }
        }
        case CMD_RADIO_BUTTON_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction.WEB_PREVIOUS_RADIO_BUTTON);
        case CMD_COMBO_BOX_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_COMBOBOX);
        case CMD_COMBO_BOX_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_COMBOBOX);
          }
        }
        case CMD_COMBO_BOX_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_COMBOBOX);
        case CMD_EDIT_FIELD_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_EDITFIELD);
        case CMD_EDIT_FIELD_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_EDITFIELD);
          }
        }
        case CMD_EDIT_FIELD_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_PREVIOUS_EDITFIELD);
        case CMD_LINK_NEXT ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_LINK);
        case CMD_LINK_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.NEXT_LINK);
          }
        }
        case CMD_LINK_PREVIOUS ->
            success = behaviorScreenReaderAction.performAction(ScreenReaderAction.PREVIOUS_LINK);
        case CMD_VISITED_LINK_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_VISITED_LINK);
        case CMD_VISITED_LINK_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(ScreenReaderAction.WEB_NEXT_VISITED_LINK);
          }
        }
        case CMD_VISITED_LINK_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction.WEB_PREVIOUS_VISITED_LINK);
        case CMD_UNVISITED_LINK_NEXT ->
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction.WEB_NEXT_UNVISITED_LINK);
        case CMD_UNVISITED_LINK_NEXT_IN_BROWSE -> {
          if (isBrowseMode()) {
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction.WEB_NEXT_UNVISITED_LINK);
          }
        }
        case CMD_UNVISITED_LINK_PREVIOUS ->
            success =
                behaviorScreenReaderAction.performAction(
                    ScreenReaderAction.WEB_PREVIOUS_UNVISITED_LINK);
        default -> {
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
  public void onActivate() {
    lastFocusedNode.clear();
    // Braille the focused node, or if that fails, braille
    // the first focusable node.
    if (!brailleFocusedNodeAndEvent(Reason.START_UP, /* event= */ null)) {
      brailleFirstFocusableNode(Reason.START_UP, /* event= */ null);
    }
  }

  @Override
  public void onDeactivate() {
    if (turnOffBdDialog != null) {
      turnOffBdDialog.dismiss();
    }
    if (browseModeDialog != null) {
      browseModeDialog.dismiss();
    }
  }

  @Override
  @SuppressLint("SwitchIntDef") // pre-existing logic
  public void onAccessibilityEvent(AccessibilityEvent event) {
    BrailleDisplayLog.v(TAG, "onAccessibilityEvent");
    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
          brailleFocusedNodeAndEvent(Reason.NAVIGATE_TO_NEW_NODE, event);
      case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
        // Update content state changes that only related to the focused node.
        brailleFocusedEventTimedMessage(event);
        // Update content changes that only related to the focused node.
        brailleFocusedNodeAndEvent(Reason.WINDOW_CHANGED, event);
      }
      case AccessibilityEvent.TYPE_WINDOWS_CHANGED,
          AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
        if (!brailleFocusedNodeAndEvent(Reason.WINDOW_CHANGED, event)) {
          // Since focus is typically not set in a newly opened
          // window, so braille the window as-if the first focusable
          // node had focus.  We don't update the focus because that
          // will make other services (e.g. talkback) reflect this
          // change, which is not desired.
          brailleFirstFocusableNode(Reason.WINDOW_CHANGED, event);
        }
      }
      default -> {}
    }
  }

  private boolean isBrowseModeCommand(int command) {
    return switch (command) {
      case CMD_TOGGLE_BROWSE_MODE,
          CMD_ARIA_LANDMARK_NEXT,
          CMD_ARIA_LANDMARK_NEXT_IN_BROWSE,
          CMD_ARIA_LANDMARK_PREVIOUS,
          CMD_BUTTON_NEXT,
          CMD_BUTTON_NEXT_IN_BROWSE,
          CMD_BUTTON_PREVIOUS,
          CMD_CHECKBOX_NEXT,
          CMD_CHECKBOX_NEXT_IN_BROWSE,
          CMD_CHECKBOX_PREVIOUS,
          CMD_COMBO_BOX_NEXT,
          CMD_COMBO_BOX_NEXT_IN_BROWSE,
          CMD_COMBO_BOX_PREVIOUS,
          CMD_CONTROL_NEXT,
          CMD_CONTROL_NEXT_IN_BROWSE,
          CMD_CONTROL_PREVIOUS,
          CMD_EDIT_FIELD_NEXT,
          CMD_EDIT_FIELD_NEXT_IN_BROWSE,
          CMD_EDIT_FIELD_PREVIOUS,
          CMD_GRAPHIC_NEXT,
          CMD_GRAPHIC_NEXT_IN_BROWSE,
          CMD_GRAPHIC_PREVIOUS,
          CMD_HEADING_1_NEXT,
          CMD_HEADING_1_NEXT_IN_BROWSE,
          CMD_HEADING_1_PREVIOUS,
          CMD_HEADING_2_NEXT,
          CMD_HEADING_2_NEXT_IN_BROWSE,
          CMD_HEADING_2_PREVIOUS,
          CMD_HEADING_3_NEXT,
          CMD_HEADING_3_NEXT_IN_BROWSE,
          CMD_HEADING_3_PREVIOUS,
          CMD_HEADING_4_NEXT,
          CMD_HEADING_4_NEXT_IN_BROWSE,
          CMD_HEADING_4_PREVIOUS,
          CMD_HEADING_5_NEXT,
          CMD_HEADING_5_NEXT_IN_BROWSE,
          CMD_HEADING_5_PREVIOUS,
          CMD_HEADING_6_NEXT,
          CMD_HEADING_6_NEXT_IN_BROWSE,
          CMD_HEADING_6_PREVIOUS,
          CMD_HEADING_NEXT_IN_BROWSE,
          CMD_LINK_NEXT_IN_BROWSE,
          CMD_LIST_NEXT,
          CMD_LIST_NEXT_IN_BROWSE,
          CMD_LIST_PREVIOUS,
          CMD_LIST_ITEM_NEXT,
          CMD_LIST_ITEM_NEXT_IN_BROWSE,
          CMD_LIST_ITEM_PREVIOUS,
          CMD_RADIO_BUTTON_NEXT,
          CMD_RADIO_BUTTON_NEXT_IN_BROWSE,
          CMD_RADIO_BUTTON_PREVIOUS,
          CMD_TABLE_COL_NEXT,
          CMD_TABLE_COL_PREVIOUS,
          CMD_TABLE_NEXT,
          CMD_TABLE_NEXT_IN_BROWSE,
          CMD_TABLE_PREVIOUS,
          CMD_VISITED_LINK_NEXT,
          CMD_VISITED_LINK_NEXT_IN_BROWSE,
          CMD_VISITED_LINK_PREVIOUS,
          CMD_UNVISITED_LINK_NEXT,
          CMD_UNVISITED_LINK_NEXT_IN_BROWSE,
          CMD_UNVISITED_LINK_PREVIOUS ->
          true;
      default -> false;
    };
  }

  private boolean activateClickableSpan(Context context, ClickableSpan clickableSpan) {
    if (clickableSpan instanceof URLSpan) {
      final Intent intent =
          new Intent(Intent.ACTION_VIEW, Uri.parse(((URLSpan) clickableSpan).getURL()));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        context.startActivity(intent);
      } catch (ActivityNotFoundException exception) {
        BrailleDisplayLog.e(TAG, "Failed to start activity", exception);
        return false;
      }
    }
    try {
      clickableSpan.onClick(null);
    } catch (RuntimeException exception) {
      BrailleDisplayLog.e(TAG, "Failed to invoke ClickableSpan", exception);
      return false;
    }
    return true;
  }

  private AccessibilityNodeInfoCompat getFocusedNode(boolean fallbackOnRoot) {
    return behaviorFocus.getAccessibilityFocusNode(fallbackOnRoot);
  }

  /**
   * Formats some braille content from an {@link AccessibilityEvent}.
   *
   * @param event The event from which to format an utterance.
   * @return The formatted utterance.
   */
  private CellsContent formatEventToBraille(AccessibilityEvent event) {
    AccessibilityNodeInfoCompat eventNode = AccessibilityEventUtils.sourceCompat(event);
    if (eventNode != null) {
      CellsContent ret = nodeBrailler.brailleEvent(event);
      ret.setPanStrategy(CellsContent.PAN_CURSOR);
      lastFocusedNode.reset(eventNode);
      if (!TextUtils.isEmpty(ret.getText())) {
        return ret;
      }
    }
    // Fall back on putting the event text on the display.
    // TODO: This can interfere with what's on the display and should be
    // done in a more disciplined manner.
    BrailleDisplayLog.v(TAG, "No node on event, falling back on event text");
    lastFocusedNode.clear();
    return new CellsContent(AccessibilityEventUtils.getEventTextOrDescription(event));
  }

  @CanIgnoreReturnValue
  private boolean brailleFocusedNodeAndEvent(Reason reason, AccessibilityEvent event) {
    AccessibilityNodeInfoCompat focused = getFocusedNode(/* fallbackOnRoot= */ false);
    if (focused != null) {
      CellsContent content = nodeBrailler.brailleNodeAndEvent(focused, event);
      if (focused.equals(lastFocusedNode.get())
          && (content.getPanStrategy() == CellsContent.PAN_RESET)) {
        content.setPanStrategy(CellsContent.PAN_KEEP);
      }
      cellsContentConsumer.setContent(content, reason);
      lastFocusedNode.reset(focused);
      return true;
    }
    return false;
  }

  private void brailleFirstFocusableNode(Reason reason, AccessibilityEvent event) {
    AccessibilityNodeInfoCompat root = getFocusedNode(/* fallbackOnRoot= */ true);
    if (root != null) {
      AccessibilityNodeInfoCompat toBraille;
      if (AccessibilityNodeInfoUtils.shouldFocusNode(root)) {
        toBraille = root;
      } else {
        TraversalStrategy traversalStrategy =
            TraversalStrategyUtils.getTraversalStrategy(
                root, behaviorFocus.createFocusFinder(), TraversalStrategy.SEARCH_FOCUS_FORWARD);
        toBraille = traversalStrategy.findFocus(root, TraversalStrategy.SEARCH_FOCUS_FORWARD);
        if (toBraille == null) {
          // Fall back on root as a last resort.
          toBraille = root;
        }
      }
      CellsContent content = nodeBrailler.brailleNodeAndEvent(toBraille, event);
      if (AccessibilityNodeInfoRef.isNull(lastFocusedNode)
          && (content.getPanStrategy() == CellsContent.PAN_RESET)) {
        content.setPanStrategy(CellsContent.PAN_KEEP);
      }
      lastFocusedNode.clear();
      cellsContentConsumer.setContent(content, reason);
    }
  }

  private void brailleFocusedEventTimedMessage(AccessibilityEvent event) {
    AccessibilityNodeInfoCompat node = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (node != null && node.equals(lastFocusedNode.get())) {
      String state = formatEventToBraille(event).getText().toString();
      if (!TextUtils.isEmpty(state)) {
        displayTimedMessage(state);
      }
    }
  }

  private boolean isBrowseMode() {
    return behaviorFocus.isBrowseMode();
  }

  private boolean startHelpActivity() {
    Intent intent = new Intent(context, KeyBindingsActivity.class);
    intent.addFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TOP
            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    intent.putExtra(KeyBindingsActivity.PROPERTY_KEY, behaviorDisplayer.getDeviceProperties());
    context.startActivity(intent);
    return true;
  }

  private boolean startTalkBackSettingsActivity() {
    Intent intent = new Intent();
    intent.setComponent(Constants.SETTINGS_ACTIVITY);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
    return true;
  }

  private boolean startBrailleDisplayActivity() {
    Intent intent = new Intent(context, BrailleDisplaySettingsActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
    return true;
  }

  private String getSwitchLanguageAnnounceText(String text) {
    StringBuilder sb = new StringBuilder(text);
    if (BrailleUserPreferences.readAnnounceSwitchContracted(context)) {
      sb.append("\n");
      sb.append(context.getString(R.string.bd_switch_contracted_mode_announcement));
    }
    return sb.toString();
  }

  private void displayTimedMessage(String timedMessage) {
    if (!behaviorDisplayer.isBrailleDisplayConnected()) {
      return;
    }
    cellsContentConsumer.setTimedContent(
        TimedMessager.Type.POPUP,
        new CellsContent(timedMessage),
        BrailleUserPreferences.getTimedMessageDurationInMillisecond(
            context, timedMessage.length()));
  }

  private void trySwitchIme() {
    if (!behaviorIme.switchInputMethodToBrailleKeyboard()) {
      showSwitchInputMethodDialog();
    }
  }

  private boolean isTriggerImeSwitchCommands(BrailleInputEvent event) {
    return event.getCommand() == BrailleInputEvent.CMD_BRAILLE_KEY
        || event.getCommand() == BrailleInputEvent.CMD_KEY_DEL;
  }

  private void showSwitchInputMethodDialog() {
    BrailleDisplayImeUnavailableActivity.initialize(
        () ->
            MaterialComponentUtils.alertDialogBuilder(
                behaviorScreenReaderAction.getAccessibilityService()));
    if (BrailleDisplayImeUnavailableActivity.necessaryToStart(context)) {
      Intent intent = new Intent(context, BrailleDisplayImeUnavailableActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
      context.startActivity(intent);
    }
  }

  private void showTurnOffBdDialog() {
    if (turnOffBdDialog == null) {
      turnOffBdDialog =
          MaterialComponentUtils.alertDialogBuilder(
                  behaviorScreenReaderAction.getAccessibilityService())
              .setTitle(R.string.bd_turn_off_bd_confirm_dialog_title)
              .setMessage(R.string.bd_turn_off_bd_confirm_dialog_message)
              .setPositiveButton(
                  R.string.bd_turn_off_bd_confirm_dialog_positive_button,
                  (dialog1, which) -> PersistentStorage.setConnectionEnabled(context, false))
              .setNegativeButton(android.R.string.cancel, null)
              .create();
      turnOffBdDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
    }
    if (!turnOffBdDialog.isShowing()) {
      turnOffBdDialog.show();
    }
  }

  private void showEnhancedKeymapDialog() {
    if (browseModeDialog != null && browseModeDialog.isShowing()) {
      return;
    }
    browseModeDialog =
        MaterialComponentUtils.alertDialogBuilder(context)
            .setTitle(R.string.bd_enhanced_keymap_dialog_title)
            .setMessage(R.string.bd_enhanced_keymap_dialog_message)
            .setPositiveButton(
                R.string.bd_enhanced_keymap_dialog_positive_button,
                (dialog, which) -> behaviorFocus.launchTalkBackKeyboardSettings())
            .setNegativeButton(R.string.bd_bt_cancel_button, null)
            .create();
    browseModeDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
    browseModeDialog.show();
  }

  private static class LoggingHandler extends Handler {
    private final Context context;

    private LoggingHandler(Context context) {
      super();
      this.context = context;
    }

    @Override
    public void handleMessage(Message msg) {
      if (msg.what == INPUT) {
        BrailleDisplayAnalytics.getInstance(context)
            .logBrailleInputCodeSetting(
                BrailleUserPreferences.readCurrentActiveInputCodeAndCorrect(context),
                BrailleUserPreferences.readContractedMode(context));
      } else if (msg.what == OUTPUT) {
        BrailleDisplayAnalytics.getInstance(context)
            .logBrailleOutputCodeSetting(
                BrailleUserPreferences.readCurrentActiveOutputCodeAndCorrect(context),
                BrailleUserPreferences.readContractedMode(context));
      }
    }
  }
}
