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

package com.google.android.accessibility.braille.brailledisplay.controller;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_FOCUSED;
import static android.view.accessibility.AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.google.android.accessibility.braille.brltty.BrailleInputEvent.CMD_TOGGLE_BRAILLE_GRADE;

import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.FeatureFlagReader;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.SupportedCommand;
import com.google.android.accessibility.braille.brailledisplay.analytics.BrailleDisplayAnalytics;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorDisplayer;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorFocus;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorIme;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorNavigation;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorNodeText;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorScreenReader;
import com.google.android.accessibility.braille.brailledisplay.controller.CellsContentConsumer.Reason;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.PopUpHistory;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.SnackbarParser;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.BrailleKeyBindingUtils;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BrailleCommonTalkBackSpeaker;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.FeedbackManager;
import com.google.android.accessibility.braille.common.TalkBackSpeaker.AnnounceType;
import com.google.android.accessibility.braille.interfaces.ScreenReaderActionPerformer.ScreenReaderAction;
import com.google.android.accessibility.braille.interfaces.TalkBackForBrailleDisplay.CustomLabelAction;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.ClassLoadingCache;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/** A class that transfers events to responding event consumers. */
public class EventManager implements EventConsumer {
  private static final String TAG = "EventManager";

  /** Accessibility event types that are related to announcements. */
  private static final int ANNOUNCEMENT_EVENT_MASK =
      TYPE_NOTIFICATION_STATE_CHANGED | TYPE_ANNOUNCEMENT | TYPE_WINDOW_CONTENT_CHANGED;

  /** Accessibility event types that warrant rechecking the current state. */
  private static final int UPDATE_STATE_EVENT_MASK =
      TYPE_VIEW_FOCUSED
          | TYPE_WINDOWS_CHANGED
          | TYPE_WINDOW_STATE_CHANGED
          | TYPE_WINDOW_CONTENT_CHANGED
          | TYPE_VIEW_ACCESSIBILITY_FOCUSED
          | TYPE_VIEW_TEXT_SELECTION_CHANGED
          | TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED
          | ANNOUNCEMENT_EVENT_MASK;

  private final Context context;
  private final DefaultConsumer defaultConsumer;
  private final EditorConsumer editorConsumer;
  private final SystemEventConsumer systemEventConsumer;
  private final CaptionConsumer captionConsumer;
  private final BehaviorIme behaviorIme;
  private final BehaviorFocus behaviorFocus;
  private final BehaviorNodeText behaviorNodeText;
  private final BehaviorDisplayer behaviorDisplayer;
  private final BehaviorNavigation behaviorNavigation;
  private final BehaviorScreenReader behaviorScreenReader;
  private final CellsContentConsumer cellsContentConsumer;
  private final PowerManager powerManager;
  private final AutoScrollManager autoScrollManager;
  private final FeedbackManager feedbackManager;
  private final PopUpHistory popUpHistory;
  private EventConsumer currentConsumer;
  private boolean windowActive;
  private boolean learningModeActive;
  private static final String LEARNING_MODE_CLASS_NAME =
      "com.google.android.accessibility.braille.brailledisplay.settings.BrailleLearningModeActivity";

  public EventManager(
      Context context,
      CellsContentConsumer cellsContentConsumer,
      FeedbackManager feedbackManager,
      BehaviorIme behaviorIme,
      BehaviorFocus behaviorFocus,
      BehaviorScreenReader behaviorScreenReader,
      BehaviorNodeText behaviorNodeText,
      BehaviorDisplayer behaviorDisplayer,
      BehaviorNavigation behaviorNavigation) {
    this.context = context;
    this.cellsContentConsumer = cellsContentConsumer;
    this.autoScrollManager =
        new AutoScrollManager(context, behaviorNavigation, feedbackManager, behaviorDisplayer);
    this.popUpHistory = new PopUpHistory(context, behaviorDisplayer);
    this.behaviorIme = behaviorIme;
    this.behaviorScreenReader = behaviorScreenReader;
    this.behaviorFocus = behaviorFocus;
    this.behaviorNodeText = behaviorNodeText;
    this.behaviorDisplayer = behaviorDisplayer;
    this.behaviorNavigation = behaviorNavigation;
    this.feedbackManager = feedbackManager;
    defaultConsumer =
        new DefaultConsumer(
            context,
            cellsContentConsumer,
            feedbackManager,
            behaviorNodeText,
            behaviorFocus,
            behaviorScreenReader,
            behaviorDisplayer,
            behaviorIme);
    editorConsumer = new EditorConsumer(context, feedbackManager, behaviorIme);
    systemEventConsumer =
        new SystemEventConsumer(context, behaviorDisplayer, cellsContentConsumer, popUpHistory);
    captionConsumer = new CaptionConsumer(context, cellsContentConsumer);
    currentConsumer = defaultConsumer;
    powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    windowActive = behaviorIme.isOnscreenKeyboardActive();
  }

  @Override
  public void onActivate() {
    currentConsumer.onActivate();
  }

  @Override
  public void onDeactivate() {
    currentConsumer.onDeactivate();
    autoScrollManager.stop();
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    BrailleDisplayLog.v(TAG, "isInteractive: " + powerManager.isInteractive());
    if (powerManager.isInteractive()) {
      if ((event.getEventType() & UPDATE_STATE_EVENT_MASK) == 0) {
        return;
      }
      updateConsumer(event);
      currentConsumer.onAccessibilityEvent(event);
    } else {
      // Clear the cell bar.
      cellsContentConsumer.setContent(new CellsContent(""), Reason.SCREEN_OFF);
      autoScrollManager.stop();
      return;
    }
    // Detect keyboard visibility.
    if (event.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
      boolean currentWindowActive = behaviorIme.isOnscreenKeyboardActive();
      if (windowActive != currentWindowActive) {
        windowActive = currentWindowActive;
        displayKeyboardVisibilityChangedTimedMessage(windowActive);
      }
    }
    // Update learning mode state based on focused activity.
    if (FeatureFlagReader.enableBrailleDisplayLearnMode(context)
        && event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      boolean previouslyActive = learningModeActive;
      learningModeActive = event.getClassName() == LEARNING_MODE_CLASS_NAME;
      maybeAnnounceLearningModeState(previouslyActive, learningModeActive);
    }
  }

  @Override
  @CanIgnoreReturnValue
  public boolean onMappedInputEvent(BrailleInputEvent event) {
    BrailleDisplayLog.v(TAG, "onMappedInputEvent: " + event.getCommand());
    int command = event.getCommand();
    logBrailleCommands(command);
    if (shouldOverrideCommandForLearningMode() && handleLearningModeOverrides(command)) {
      return true;
    }
    if (shouldPanUp(command)) {
      if (behaviorNavigation.panUp()) {
        return true;
      }
      feedbackManager.emitFeedback(FeedbackManager.Type.NAVIGATE_OUT_OF_BOUNDS);
    } else if (shouldPanDown(command)) {
      if (behaviorNavigation.panDownWhenAutoScrollDisabled()) {
        return true;
      }
      feedbackManager.emitFeedback(FeedbackManager.Type.NAVIGATE_OUT_OF_BOUNDS);
    } else {
      if (handleHigherPriorityCommands(event)) {
        return true;
      }
      // Global commands can't be overridden.
      if (handleGlobalCommands(event)) {
        return true;
      }
      if (handleTalkBackCommands(event)) {
        return true;
      }
      if (handleGeneralCommands(event)) {
        return true;
      }
      if (handleOtherCommands(event)) {
        return true;
      }
      feedbackManager.emitFeedback(FeedbackManager.Type.UNKNOWN_COMMAND);
    }
    return false;
  }

  /** Called when the reading control settings is changed. */
  public void onReadingControlSettingsChanged(String readingControlDescription) {
    behaviorDisplayer.displayTimedMessage(readingControlDescription);
  }

  /** Called when the reading control value is changed. */
  @Override
  public void onReadingControlValueChanged() {
    currentConsumer.onReadingControlValueChanged();
  }

  private void updateConsumer(@Nullable AccessibilityEvent event) {
    if (FeatureFlagReader.usePopupMessage(context)
        && BrailleUserPreferences.readPopupMessageEnabled(context)
        && event != null
        && isAnnouncement(event)) {
      setCurrentConsumer(systemEventConsumer);
    } else if (behaviorIme.acceptInput()) {
      setCurrentConsumer(editorConsumer);
    } else if (BrailleUserPreferences.readCaptionEnabled(context)
        && event != null
        && isCaption(event)) {
      setCurrentConsumer(captionConsumer);
    } else {
      setCurrentConsumer(defaultConsumer);
    }
  }

  /** Set the current consumer to the given consumer. */
  private void setCurrentConsumer(EventConsumer consumer) {
    if (currentConsumer != consumer) {
      currentConsumer.onDeactivate();
      currentConsumer = consumer;
      currentConsumer.onActivate();
    }
  }

  /** Logs triggered command. */
  private void logBrailleCommands(int command) {
    for (SupportedCommand cmd : SupportedCommand.getAvailableSupportedCommands(context)) {
      if (cmd.getCommand() == command) {
        BrailleDisplayAnalytics.getInstance(context).logBrailleCommand(command);
        break;
      }
    }
  }

  private boolean shouldPanUp(int command) {
    boolean reversePanningButtons = BrailleUserPreferences.readReversePanningButtons(context);
    return (command == BrailleInputEvent.CMD_NAV_PAN_UP && !reversePanningButtons)
        || (command == BrailleInputEvent.CMD_NAV_PAN_DOWN && reversePanningButtons);
  }

  private boolean shouldPanDown(int command) {
    boolean reversePanningButtons = BrailleUserPreferences.readReversePanningButtons(context);
    return (command == BrailleInputEvent.CMD_NAV_PAN_DOWN && !reversePanningButtons)
        || (command == BrailleInputEvent.CMD_NAV_PAN_UP && reversePanningButtons);
  }

  private void displayKeyboardVisibilityChangedTimedMessage(boolean visible) {
    String timedMessage;
    if (visible) {
      CharSequence keyboardName = behaviorIme.getOnScreenKeyboardName();
      keyboardName =
          TextUtils.isEmpty(keyboardName) ? context.getString(R.string.bd_keyboard) : keyboardName;
      timedMessage = context.getString(R.string.bd_keyboard_showing, keyboardName);
    } else {
      timedMessage = context.getString(R.string.bd_keyboard_hidden);
    }
    behaviorDisplayer.displayTimedMessage(timedMessage);
  }

  /** Handles the auto-scroll commands. */
  private boolean handleAutoscrollCommands(BrailleInputEvent event) {
    if (autoScrollManager.isActive()) {
      if (event.getCommand() == BrailleInputEvent.CMD_BRAILLE_KEY) {
        SupportedCommand supportedCommand =
            BrailleKeyBindingUtils.convertToCommand(
                context, /* hasSpace= */ false, event.getArgument());
        if (shouldDecreaseAutoScrollDuration(supportedCommand)) {
          autoScrollManager.decreaseDuration();
          return true;
        } else if (shouldIncreaseAutoScrollDuration(supportedCommand)) {
          autoScrollManager.increaseDuration();
          return true;
        }
      }
      autoScrollManager.stop();
      return true;
    } else if (event.getCommand() == BrailleInputEvent.CMD_TOGGLE_AUTO_SCROLL) {
      if (autoScrollManager.isActive()) {
        autoScrollManager.stop();
      } else {
        autoScrollManager.start();
      }
      return true;
    }
    return false;
  }

  /** Returns whether to increase auto scroll duration. */
  private boolean shouldIncreaseAutoScrollDuration(SupportedCommand supportedCommand) {
    return supportedCommand != null
        && supportedCommand.getCommand() == BrailleInputEvent.CMD_INCREASE_AUTO_SCROLL_DURATION;
  }

  /** Returns whether to decrease auto scroll duration. */
  private boolean shouldDecreaseAutoScrollDuration(SupportedCommand supportedCommand) {
    return supportedCommand != null
        && supportedCommand.getCommand() == BrailleInputEvent.CMD_DECREASE_AUTO_SCROLL_DURATION;
  }

  /** Return whether the event is with type of announcements */
  private boolean isAnnouncement(AccessibilityEvent event) {
    if ((event.getEventType() & ANNOUNCEMENT_EVENT_MASK) != 0) {
      if ((event.getEventType() & TYPE_WINDOW_CONTENT_CHANGED) != 0) {
        return SnackbarParser.isAlert(AccessibilityEventUtils.sourceCompat(event));
      }
      return true;
    }
    return false;
  }

  /** Return whether the event is with the class name of {@code SubtitleView}. */
  private boolean isCaption(AccessibilityEvent event) {
    AccessibilityNodeInfoCompat sourceNode = AccessibilityEventUtils.sourceCompat(event);
    if (sourceNode == null) {
      return false;
    }
    return ClassLoadingCache.checkInstanceOf(
        sourceNode.getClassName(), "androidx.media3.ui.SubtitleView");
  }

  /** Handles higher priority because it's in a special mode. */
  private boolean handleHigherPriorityCommands(BrailleInputEvent event) {
    return handleAutoscrollCommands(event) || handleTimedMessage(event);
  }

  /**
   * Handles global commands.
   *
   * @param event input event fromm braille display
   * @return return true when we own these actions return value is not related to talkback
   *     operation.
   */
  private boolean handleGlobalCommands(BrailleInputEvent event) {
    boolean success;
    switch (event.getCommand()) {
      case BrailleInputEvent.CMD_GLOBAL_HOME ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.GLOBAL_HOME);
      case BrailleInputEvent.CMD_GLOBAL_BACK ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.GLOBAL_BACK);
      case BrailleInputEvent.CMD_GLOBAL_RECENTS ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.GLOBAL_RECENTS);
      case BrailleInputEvent.CMD_GLOBAL_NOTIFICATIONS ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.GLOBAL_NOTIFICATIONS);
      case BrailleInputEvent.CMD_QUICK_SETTINGS ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.GLOBAL_QUICK_SETTINGS);
      case BrailleInputEvent.CMD_ALL_APPS ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.GLOBAL_ALL_APPS);
      default -> {
        return false;
      }
    }
    handleCommandCompletion(success);
    // Always return true because we own these actions.
    return true;
  }

  /** Handles the commands related to TalkBack. */
  private boolean handleTalkBackCommands(BrailleInputEvent event) {
    boolean success = false;
    AccessibilityNodeInfoCompat node;
    switch (event.getCommand()) {
      case BrailleInputEvent.CMD_TOGGLE_SCREEN_SEARCH ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.SCREEN_SEARCH);
      case BrailleInputEvent.CMD_EDIT_CUSTOM_LABEL -> {
        node = behaviorFocus.getAccessibilityFocusNode(false);
        if (node != null && behaviorNodeText.supportsLabel(node)) {
          CharSequence viewLabel = behaviorNodeText.getCustomLabelText(node);
          // If no custom label, only have "add" option. If there is already a
          // label we have the "edit" and "remove" options.
          return behaviorNodeText.showLabelDialog(
              TextUtils.isEmpty(viewLabel)
                  ? CustomLabelAction.ADD_LABEL
                  : CustomLabelAction.EDIT_LABEL,
              node);
        }
      }
      case BrailleInputEvent.CMD_OPEN_TALKBACK_MENU ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.OPEN_TALKBACK_MENU);
      case BrailleInputEvent.CMD_PLAY_PAUSE_MEDIA ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.PLAY_PAUSE_MEDIA);
      default -> {
        return false;
      }
    }
    handleCommandCompletion(success);
    // Always return true because we own these actions.
    return true;
  }

  /** Handles the commands not restricted in any situation. */
  private boolean handleGeneralCommands(BrailleInputEvent event) {
    boolean success;
    switch (event.getCommand()) {
      case BrailleInputEvent.CMD_LONG_PRESS_CURRENT ->
          success = behaviorScreenReader.performAction(ScreenReaderAction.LONG_CLICK_CURRENT);
      case BrailleInputEvent.CMD_LONG_PRESS_ROUTE -> {
        AccessibilityNodeInfoCompat node =
            cellsContentConsumer.getAccessibilityNode(event.getArgument());
        if (node == null) {
          success = behaviorScreenReader.performAction(ScreenReaderAction.LONG_CLICK_CURRENT);
          break;
        }
        success = behaviorScreenReader.performAction(ScreenReaderAction.LONG_CLICK_NODE, node);
      }
      case BrailleInputEvent.CMD_NEXT_INPUT_METHOD ->
          success = behaviorIme.switchToNextInputMethod();
      case CMD_TOGGLE_BRAILLE_GRADE -> {
        behaviorDisplayer.toggleBrailleContractedMode();
        success = true;
      }
      case BrailleInputEvent.CMD_SHOW_POPUP_MESSAGE_HISTORY -> {
        if (!FeatureFlagReader.usePopupMessage(context)) {
          return false;
        }
        popUpHistory.showHistory();
        success = true;
      }
      default -> {
        return false;
      }
    }
    handleCommandCompletion(success);
    // Always return true because we own these actions.
    return true;
  }

  /** Handles the commands related to captions. */
  private boolean handleCaptionCommands(BrailleInputEvent event) {
    if (!FeatureFlagReader.useShowCaptions(context)) {
      return false;
    }
    boolean result = false;
    switch (event.getCommand()) {
      case BrailleInputEvent.CMD_CAPTION_ENTER_OR_EXIT ->
          BrailleUserPreferences.writeCaptionEnabled(
              context, !BrailleUserPreferences.readCaptionEnabled(context));
      case BrailleInputEvent.CMD_NAV_BOTTOM -> {
        // If captions are not enabled or not displaying, the event will be passed to the consumer
        // for processing.
        if (!cellsContentConsumer.isCaptionShowing()) {
          BrailleDisplayLog.v(TAG, "Not showing caption.");
          return false;
        }
        result = cellsContentConsumer.showLatestCaption();
      }
      default -> {
        return false;
      }
    }
    if (!result) {
      feedbackManager.emitFeedback(FeedbackManager.Type.COMMAND_FAILED);
    }
    // Always return true because we own these actions.
    return true;
  }

  private boolean handleTimedMessage(BrailleInputEvent event) {
    if (handleCaptionCommands(event)) {
      return true;
    }
    if (cellsContentConsumer.isTimedMessageDisplaying()) {
      cellsContentConsumer.dismissTimedMessage();
      if (cellsContentConsumer.isTimedMessageDisplaying()) {
        // Need to dismiss queued message. No action is taken on this event.
        return true;
      } else {
        // Skip route key because they should do nothing about taking action on the timed message.
        return event.getCommand() == BrailleInputEvent.CMD_ROUTE;
      }
    }
    return false;
  }

  private boolean handleOtherCommands(BrailleInputEvent event) {
    updateConsumer(/* event= */ null);
    return currentConsumer.onMappedInputEvent(event);
  }

  private void handleCommandCompletion(boolean success) {
    if (success) {
      if (behaviorIme.isBrailleKeyboardActivated()) {
        behaviorIme.commitHoldings();
      }
    } else {
      feedbackManager.emitFeedback(FeedbackManager.Type.COMMAND_FAILED);
    }
  }

  /** Returns whether learning mode is enabled. */
  private boolean shouldOverrideCommandForLearningMode() {
    return FeatureFlagReader.enableBrailleDisplayLearnMode(context) && learningModeActive;
  }

  /**
   * Changes command behavior for learning mode. Supported command will announce the description
   * defined in {@link BrailleKeyBindingUtils}. Returns false if command is not a supported command.
   *
   * @param command Maps to the keyboard commands defined in {@link BrailleInputEvent}.
   */
  private boolean handleLearningModeOverrides(int command) {
    SupportedCommand supportedCommand =
        SupportedCommand.getAvailableSupportedCommands(context).stream()
            .filter(c -> c.getCommand() == command)
            .findFirst()
            .orElse(null);
    if (supportedCommand == null) {
      return false;
    }
    String commandDescription = supportedCommand.getCommandDescription(context.getResources());
    BrailleCommonTalkBackSpeaker.getInstance().speak(commandDescription, AnnounceType.INTERRUPT);
    behaviorDisplayer.displayTimedMessage(commandDescription);
    return true;
  }

  /**
   * Announces learning mode intro or outro message if active state has changed.
   *
   * @param previouslyActive Whether learning mode was active before the current event.
   * @param currentlyActive Whether learning mode is active after the current event.
   */
  private void maybeAnnounceLearningModeState(boolean previouslyActive, boolean currentlyActive) {
    if (previouslyActive == currentlyActive) {
      return;
    }
    if (!behaviorDisplayer.isBrailleDisplayConnected()) {
      return;
    }
    if (currentlyActive) {
      BrailleCommonTalkBackSpeaker.getInstance()
          .speak(context.getString(R.string.bd_learning_mode_instructions), AnnounceType.INTERRUPT);
      return;
    }
    BrailleCommonTalkBackSpeaker.getInstance()
        .speak(context.getString(R.string.bd_learning_mode_outro_message), AnnounceType.INTERRUPT);
  }
}
