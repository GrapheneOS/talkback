/*
 * Copyright (C) 2022 Google Inc.
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
package com.google.android.accessibility.talkback.keyboard;

import static com.google.android.accessibility.talkback.Feedback.ContinuousRead.Action.START_AT_CURSOR;
import static com.google.android.accessibility.talkback.Feedback.ContinuousRead.Action.START_AT_TOP;
import static com.google.android.accessibility.talkback.Feedback.DimScreen.Action.BRIGHTEN;
import static com.google.android.accessibility.talkback.Feedback.DimScreen.Action.DIM;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.DOUBLE_CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.LONG_CLICK_CURRENT;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.READ_NODE_LINK_URL;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.NEXT_GRANULARITY;
import static com.google.android.accessibility.talkback.Feedback.FocusDirection.Action.PREVIOUS_GRANULARITY;
import static com.google.android.accessibility.talkback.Feedback.Keyboard.Action.SHOW_KEYBOARD_SHORTCUTS_DIALOG;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.COPY_LAST;
import static com.google.android.accessibility.talkback.Feedback.Speech.Action.TOGGLE_VOICE_FEEDBACK;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.SHOW_CONTROL_LIST;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.SHOW_HEADING_LIST;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.SHOW_LANDMARK_LIST;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.SHOW_LINK_LIST;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.SHOW_TABLE_LIST;
import static com.google.android.accessibility.talkback.Feedback.UniversalSearch.Action.TOGGLE_SEARCH;
import static com.google.android.accessibility.talkback.Feedback.VoiceRecognition.Action.START_LISTENING_IF_SCREEN_NOT_LOCKED;
import static com.google.android.accessibility.talkback.actor.TalkBackUIActor.Type.SELECTOR_ITEM_ACTION_OVERLAY;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CONTEXT;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CUSTOM_ACTION;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.LANGUAGE;
import static com.google.android.accessibility.talkback.selector.SelectorController.Setting.ACTIONS;
import static com.google.android.accessibility.talkback.trainingcommon.TrainingConfig.TrainingId.TRAINING_ID_TUTORIAL_KEYBOARD;
import static com.google.android.accessibility.utils.input.CursorGranularity.CHARACTER;
import static com.google.android.accessibility.utils.input.CursorGranularity.DEFAULT;
import static com.google.android.accessibility.utils.input.CursorGranularity.LINE;
import static com.google.android.accessibility.utils.input.CursorGranularity.WORD;
import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_KEYBOARD;
import static com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_NAME;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_BACKWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_DOWN;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_UP;

import android.accessibilityservice.AccessibilityService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Patterns;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.android.talkback.TalkBackPreferencesActivity;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.FocusDirection;
import com.google.android.accessibility.talkback.HelpAndFeedbackUtils;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.DirectionNavigationActor;
import com.google.android.accessibility.talkback.actor.FullScreenReadActor;
import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import com.google.android.accessibility.talkback.actor.SystemActionPerformer;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.compositor.CollectionStateFeedbackUtils;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorPhoneticLetters;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget;
import com.google.android.accessibility.talkback.focusmanagement.NavigationTarget.TargetType;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.monitor.BatteryMonitor;
import com.google.android.accessibility.talkback.preference.base.TalkBackKeyboardShortcutPreferenceFragment;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.talkback.selector.SelectorController.AnnounceType;
import com.google.android.accessibility.talkback.training.TutorialInitiator;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivity;
import com.google.android.accessibility.talkback.utils.DateTimeUtils;
import com.google.android.accessibility.talkback.utils.GeminiCommandUtils;
import com.google.android.accessibility.talkback.utils.VerbosityPreferences;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.SpannableUtils.SpannableWithOffset;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.input.CursorGranularity;
import com.google.android.accessibility.utils.monitor.CollectionState;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.output.TextFormattingUtils;
import com.google.android.accessibility.utils.traversal.SpannableTraversalUtils;
import com.google.android.accessibility.utils.traversal.TraversalStrategy;
import com.google.android.accessibility.utils.traversal.TraversalStrategy.SearchDirection;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Feedback-mapper for keyboard shortcuts to actions. This class reacts to navigation actions,
 * global actions, and either traverses with intra-node granularity or changes accessibility focus.
 */
public class KeyComboMapper {
  private static final String TAG = "KeyComboMapper";

  private final Pipeline.FeedbackReturner pipeline;
  private final ActorState actorState;
  private final SelectorController selectorController;
  private final SpeechRateAndPitchActor speechRateAndPitchActor;
  private final ListMenuManager menuManager;
  private final FullScreenReadActor fullScreenReadActor;
  private final DirectionNavigationActor.StateReader stateReader;
  private final BatteryMonitor batteryMonitor;
  private final ScreenStateMonitor.State screenState;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;
  private final ProcessorPhoneticLetters processorPhoneticLetters;
  private final CollectionState collectionState;
  private final AccessibilityService accessibilityService;
  private final GestureShortcutMapping gestureShortcutMapping;

  public KeyComboMapper(
      AccessibilityService accessibilityService,
      Pipeline.FeedbackReturner pipeline,
      ActorState actorState,
      SelectorController selectorController,
      SpeechRateAndPitchActor speechRateAndPitchActor,
      ListMenuManager menuManager,
      FullScreenReadActor fullScreenReadActor,
      DirectionNavigationActor.StateReader stateReader,
      BatteryMonitor batteryMonitor,
      ScreenStateMonitor.State screenState,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      ProcessorPhoneticLetters processorPhoneticLetters,
      CollectionState collectionState,
      GestureShortcutMapping gestureShortcutMapping) {
    this.pipeline = pipeline;
    this.actorState = actorState;
    this.selectorController = selectorController;
    this.speechRateAndPitchActor = speechRateAndPitchActor;
    this.menuManager = menuManager;
    this.fullScreenReadActor = fullScreenReadActor;
    this.stateReader = stateReader;
    this.batteryMonitor = batteryMonitor;
    this.screenState = screenState;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.processorPhoneticLetters = processorPhoneticLetters;
    this.collectionState = collectionState;
    this.accessibilityService = accessibilityService;
    this.gestureShortcutMapping = gestureShortcutMapping;
  }

  /**
   * Performs the KeyCombo action by given {@code actionId}.
   *
   * @param action the keyboard shortcut generating from key combos.
   */
  @CanIgnoreReturnValue
  boolean performKeyComboAction(TalkBackPhysicalKeyboardShortcut action, EventId eventId) {
    boolean result = true;
    switch (action) {
      case NAVIGATE_NEXT ->
          // Direction navigation actions.
          result = pipeline.returnFeedback(eventId, createFocusDirectionBuilder(true));
      case NAVIGATE_PREVIOUS ->
          result = pipeline.returnFeedback(eventId, createFocusDirectionBuilder(false));
      case NAVIGATE_NEXT_DEFAULT ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_FORWARD)
                      .setGranularity(DEFAULT)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setDefaultToInputFocus(true)
                      .setScroll(true)
                      .setWrap(true));
      case NAVIGATE_PREVIOUS_DEFAULT ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_BACKWARD)
                      .setGranularity(DEFAULT)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setDefaultToInputFocus(true)
                      .setScroll(true)
                      .setWrap(true));
      case NAVIGATE_ABOVE ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_UP)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setWrap(true)
                      .setScroll(true)
                      .setDefaultToInputFocus(true));
      case NAVIGATE_BELOW ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_DOWN)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setWrap(true)
                      .setScroll(true)
                      .setDefaultToInputFocus(true));
      case NAVIGATE_FIRST ->
          result = pipeline.returnFeedback(eventId, Feedback.focusTop(INPUT_MODE_KEYBOARD));
      case NAVIGATE_LAST ->
          result = pipeline.returnFeedback(eventId, Feedback.focusBottom(INPUT_MODE_KEYBOARD));
      case NAVIGATE_NEXT_CONTAINER ->
          result = pipeline.returnFeedback(eventId, Feedback.nextContainer(INPUT_MODE_KEYBOARD));
      case NAVIGATE_PREVIOUS_CONTAINER ->
          result = pipeline.returnFeedback(eventId, Feedback.prevContainer(INPUT_MODE_KEYBOARD));
      // Tap
      case PERFORM_CLICK -> {
        if (SelectorController.getCurrentSetting(accessibilityService).equals(ACTIONS)) {
          selectorController.activateCurrentAction(eventId);
          break;
        }
        result = pipeline.returnFeedback(eventId, Feedback.focus(CLICK_CURRENT));
      }
      case PERFORM_DOUBLE_CLICK ->
          result = pipeline.returnFeedback(eventId, Feedback.focus(DOUBLE_CLICK_CURRENT));
      case PERFORM_LONG_CLICK ->
          result = pipeline.returnFeedback(eventId, Feedback.focus(LONG_CLICK_CURRENT));
      // Micro Granularity
      case NAVIGATE_NEXT_WINDOW ->
          result =
              pipeline.returnFeedback(
                  eventId, Feedback.nextWindow(INPUT_MODE_KEYBOARD).setDefaultToInputFocus(true));
      case NAVIGATE_PREVIOUS_WINDOW ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.previousWindow(INPUT_MODE_KEYBOARD).setDefaultToInputFocus(true));
      case NAVIGATE_NEXT_WORD ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_FORWARD)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setGranularity(WORD));
      case NAVIGATE_PREVIOUS_WORD ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_BACKWARD)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setGranularity(WORD));
      case NAVIGATE_NEXT_CHARACTER ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_FORWARD)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setGranularity(CHARACTER));
      case NAVIGATE_PREVIOUS_CHARACTER ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_BACKWARD)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setGranularity(CHARACTER));
      case NAVIGATE_NEXT_LINE ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_FORWARD)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setGranularity(LINE)
                      .setSkipEdgeCheck(true));
      case NAVIGATE_PREVIOUS_LINE ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.focusDirection(SEARCH_FOCUS_BACKWARD)
                      .setInputMode(INPUT_MODE_KEYBOARD)
                      .setGranularity(LINE)
                      .setSkipEdgeCheck(true));
      // Native or Web navigation actions.
      case NAVIGATE_NEXT_HEADING ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.HEADING,
                  NavigationTarget.TARGET_HEADING,
                  /* forward= */ true,
                  eventId);
      case NAVIGATE_PREVIOUS_HEADING ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.HEADING,
                  NavigationTarget.TARGET_HEADING,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_LINK ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.LINK,
                  NavigationTarget.TARGET_LINK,
                  /* forward= */ true,
                  eventId);
      case NAVIGATE_PREVIOUS_LINK ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.LINK,
                  NavigationTarget.TARGET_LINK,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_CONTROL ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.CONTROL,
                  NavigationTarget.TARGET_CONTROL,
                  /* forward= */ true,
                  eventId);
      case NAVIGATE_PREVIOUS_CONTROL ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.CONTROL,
                  NavigationTarget.TARGET_CONTROL,
                  /* forward= */ false,
                  eventId);
      // Web navigation actions.
      case NAVIGATE_NEXT_BUTTON ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_BUTTON, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_BUTTON ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_BUTTON, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_CHECKBOX ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_CHECKBOX, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_CHECKBOX ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_CHECKBOX, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_ARIA_LANDMARK ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_ARIA_LANDMARK, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_ARIA_LANDMARK ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_ARIA_LANDMARK,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_EDIT_FIELD ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_EDIT_FIELD, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_EDIT_FIELD ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_EDIT_FIELD, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_FOCUSABLE_ITEM ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_FOCUSABLE_ITEM,
                  /* forward= */ true,
                  eventId);
      case NAVIGATE_PREVIOUS_FOCUSABLE_ITEM ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_FOCUSABLE_ITEM,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_HEADING_1 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_1, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_HEADING_1 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_1, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_HEADING_2 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_2, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_HEADING_2 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_2, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_HEADING_3 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_3, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_HEADING_3 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_3, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_HEADING_4 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_4, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_HEADING_4 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_4, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_HEADING_5 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_5, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_HEADING_5 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_5, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_HEADING_6 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_6, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_HEADING_6 ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_HEADING_6, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_GRAPHIC ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_GRAPHIC, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_GRAPHIC ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_GRAPHIC, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_LIST_ITEM ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_LIST_ITEM, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_LIST_ITEM ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_LIST_ITEM, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_LIST ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_LIST, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_LIST ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_LIST, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_TABLE ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_TABLE, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_TABLE ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_TABLE, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_COMBOBOX ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_COMBOBOX, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_COMBOBOX ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_COMBOBOX, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_VISITED_LINK ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_VISITED_LINK, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_VISITED_LINK ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_VISITED_LINK, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_UNVISITED_LINK ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_UNVISITED_LINK,
                  /* forward= */ true,
                  eventId);
      case NAVIGATE_PREVIOUS_UNVISITED_LINK ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_UNVISITED_LINK,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_RADIO ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_RADIO, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_RADIO ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_RADIO, /* forward= */ false, eventId);
      // Table commands.
      case NAVIGATE_NEXT_ROW ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.ROW, NavigationTarget.TARGET_ROW, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_ROW ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.ROW,
                  NavigationTarget.TARGET_ROW,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_COLUMN ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.COLUMN,
                  NavigationTarget.TARGET_COLUMN,
                  /* forward= */ true,
                  eventId);
      case NAVIGATE_PREVIOUS_COLUMN ->
          result =
              performNativeOrWebNavigationKeyCombo(
                  CursorGranularity.COLUMN,
                  NavigationTarget.TARGET_COLUMN,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_ROW_BOUNDS ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_ROW_BOUNDS ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_ROW_BOUNDS, /* forward= */ false, eventId);
      case NAVIGATE_NEXT_COLUMN_BOUNDS ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_COLUMN_BOUNDS ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_COLUMN_BOUNDS,
                  /* forward= */ false,
                  eventId);
      case NAVIGATE_NEXT_TABLE_BOUNDS ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS, /* forward= */ true, eventId);
      case NAVIGATE_PREVIOUS_TABLE_BOUNDS ->
          result =
              performWebNavigationKeyCombo(
                  NavigationTarget.TARGET_HTML_ELEMENT_TABLE_BOUNDS, /* forward= */ false, eventId);
      // Global actions
      case BACK ->
          result =
              pipeline.returnFeedback(
                  eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_BACK));
      case HOME ->
          result =
              pipeline.returnFeedback(
                  eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_HOME));
      case NOTIFICATIONS ->
          result =
              pipeline.returnFeedback(
                  eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS));
      case RECENT_APPS ->
          result =
              pipeline.returnFeedback(
                  eventId, Feedback.systemAction(AccessibilityService.GLOBAL_ACTION_RECENTS));
      case PLAY_PAUSE_MEDIA ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.systemAction(SystemActionPerformer.GLOBAL_ACTION_KEYCODE_HEADSETHOOK));
      // Other actions.
      case SEARCH_SCREEN_FOR_ITEM ->
          result = pipeline.returnFeedback(eventId, Feedback.universalSearch(TOGGLE_SEARCH));
      case SHOW_HEADING_LIST ->
          result = pipeline.returnFeedback(eventId, Feedback.universalSearch(SHOW_HEADING_LIST));
      case SHOW_LANDMARK_LIST ->
          result = pipeline.returnFeedback(eventId, Feedback.universalSearch(SHOW_LANDMARK_LIST));
      case SHOW_LINK_LIST ->
          result = pipeline.returnFeedback(eventId, Feedback.universalSearch(SHOW_LINK_LIST));
      case SHOW_CONTROL_LIST ->
          result = pipeline.returnFeedback(eventId, Feedback.universalSearch(SHOW_CONTROL_LIST));
      case SHOW_TABLE_LIST ->
          result = pipeline.returnFeedback(eventId, Feedback.universalSearch(SHOW_TABLE_LIST));
      case SELECT_NEXT_READING_MENU ->
          selectorController.selectPreviousOrNextSetting(eventId, AnnounceType.DESCRIPTION, true);
      case SELECT_PREVIOUS_READING_MENU ->
          selectorController.selectPreviousOrNextSetting(eventId, AnnounceType.DESCRIPTION, false);
      case ADJUST_READING_MENU_DOWN -> selectorController.adjustSelectedSetting(eventId, true);
      case ADJUST_READING_MENU_UP -> selectorController.adjustSelectedSetting(eventId, false);
      case SPEECH_RATE_INCREASE -> selectorController.changeSpeechRate(eventId, true);
      case SPEECH_RATE_DECREASE -> selectorController.changeSpeechRate(eventId, false);
      case SPEECH_VOLUME_INCREASE ->
          selectorController.changeAccessibilityVolume(eventId, /* decreaseVolume= */ false);
      case SPEECH_VOLUME_DECREASE ->
          selectorController.changeAccessibilityVolume(eventId, /* decreaseVolume= */ true);
      case SPEECH_PITCH_INCREASE -> changeSpeechPitch(eventId, /* isIncrease= */ true);
      case SPEECH_PITCH_DECREASE -> changeSpeechPitch(eventId, /* isIncrease= */ false);
      case NEXT_NAVIGATION_SETTING ->
          result = pipeline.returnFeedback(eventId, Feedback.focusDirection(NEXT_GRANULARITY));
      case PREVIOUS_NAVIGATION_SETTING ->
          result = pipeline.returnFeedback(eventId, Feedback.focusDirection(PREVIOUS_GRANULARITY));
      case READ_FROM_TOP ->
          result = pipeline.returnFeedback(eventId, Feedback.continuousRead(START_AT_TOP));
      case READ_FROM_CURSOR_ITEM ->
          result = pipeline.returnFeedback(eventId, Feedback.continuousRead(START_AT_CURSOR));
      case READ_CURRENT_URL -> result = readCurrentUrl(eventId);
      case READ_LINK_URL ->
          result = pipeline.returnFeedback(eventId, Feedback.focus(READ_NODE_LINK_URL));
      case SHOW_GLOBAL_CONTEXT_MENU -> result = menuManager.showMenu(CONTEXT, eventId);
      case SHOW_ACTIONS -> result = menuManager.showMenu(CUSTOM_ACTION, eventId);
      case SHOW_LANGUAGES_AVAILABLE -> result = menuManager.showMenu(LANGUAGE, eventId);
      case OPEN_MANAGE_KEYBOARD_SHORTCUTS -> {
        if (SettingsUtils.allowLinksOutOfSettings(accessibilityService.getApplicationContext())) {
          openManageKeyboardShortcuts();
        }
      }
      case OPEN_TALKBACK_SETTINGS -> {
        if (SettingsUtils.allowLinksOutOfSettings(accessibilityService.getApplicationContext())) {
          openTalkBackSettings();
        }
      }
      case COPY_LAST_SPOKEN_PHRASE ->
          result =
              pipeline.returnFeedback(
                  eventId, Feedback.part().setSpeech(Feedback.Speech.create(COPY_LAST)));
      case HIDE_OR_SHOW_SCREEN -> {
        if (actorState.getDimScreen().isDimmingEnabled()) {
          result = pipeline.returnFeedback(eventId, Feedback.dimScreen(BRIGHTEN));
        } else {
          result = pipeline.returnFeedback(eventId, Feedback.dimScreen(DIM));
        }
      }
      case SCREEN_OVERVIEW ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  GeminiCommandUtils.feedbackForScreenOverview(
                      accessibilityService,
                      accessibilityFocusMonitor.getAccessibilityFocus(
                          /* useInputFocusIfEmpty= */ false)));
      case DESCRIBE_IMAGE ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  GeminiCommandUtils.feedbackForDescribeImage(
                      accessibilityService,
                      accessibilityFocusMonitor.getAccessibilityFocus(
                          /* useInputFocusIfEmpty= */ false),
                      actorState));
      case REFOCUS_CURRENT_NODE -> {
        AccessibilityNodeInfoCompat currentFocusedNode =
            accessibilityFocusMonitor.getAccessibilityFocus(
                /* useInputFocusIfEmpty= */ false, /* requireEditable= */ false);
        if (currentFocusedNode == null) {
          break;
        }
        result =
            pipeline.returnFeedback(
                eventId,
                Feedback.focus(
                        currentFocusedNode,
                        FocusActionInfo.builder()
                            .setSourceAction(FocusActionInfo.KEYBOARD_SHORTCUT_REFOCUS)
                            .setIsFromRefocusAction(true)
                            .build())
                    .setForceRefocus(true));
      }
      case ANNOUNCE_CURRENT_TIME_AND_DATE ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.speech(DateTimeUtils.getCurrentTimeAndDate(accessibilityService)));
      case ANNOUNCE_BATTERY_STATE ->
          result =
              pipeline.returnFeedback(
                  eventId, Feedback.speech(batteryMonitor.getBatteryStateDescription()));
      case ANNOUNCE_CURRENT_TITLE ->
          result =
              pipeline.returnFeedback(eventId, Feedback.speech(screenState.getActiveWindowTitle()));
      case ANNOUNCE_PHONETIC_PRONUNCIATION ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.speech(
                      processorPhoneticLetters.getPhoneticText(
                          AccessibilityNodeInfoUtils.getNodeText(
                              accessibilityFocusMonitor.getAccessibilityFocus(
                                  /* useInputFocusIfEmpty= */ false)))));
      case ANNOUNCE_COLUMN_HEADER -> {
        CharSequence columnHeader =
            CollectionStateFeedbackUtils.getCurrentTableItemColumnHeader(
                collectionState, accessibilityService);
        result =
            pipeline.returnFeedback(
                eventId,
                Feedback.speech(
                    (columnHeader == null
                        ? accessibilityService.getString(R.string.announce_no_column_header)
                        : columnHeader)));
      }
      case ANNOUNCE_RICH_TEXT_DESCRIPTION -> {
        final List<SpannableWithOffset> spannableStrings = new ArrayList<>();
        SpannableTraversalUtils.getSpannableStringsWithTargetCharacterStyleInNodeTree(
            accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ true),
            TextFormattingUtils.getFormattingSpanClasses(),
            spannableStrings);
        CharSequence textFormattingString =
            TextFormattingUtils.getTextFormattingSummary(accessibilityService, spannableStrings);

        if (!TextUtils.isEmpty(textFormattingString)) {
          result = pipeline.returnFeedback(eventId, Feedback.speech(textFormattingString));
        }
      }
      case ANNOUNCE_ROW_HEADER -> {
        CharSequence rowHeader =
            CollectionStateFeedbackUtils.getCurrentTableItemRowHeader(
                collectionState, accessibilityService);
        result =
            pipeline.returnFeedback(
                eventId,
                Feedback.speech(
                    (rowHeader == null
                        ? accessibilityService.getString(R.string.announce_no_row_header)
                        : rowHeader)));
      }
      case TOGGLE_SELECTION -> {
        boolean selectionModeEnabled = actorState.getDirectionNavigation().isSelectionModeActive();
        if (selectionModeEnabled) {
          fullScreenReadActor.setSelectionModeDisabled(eventId);
        } else {
          fullScreenReadActor.setSelectionModeEnabled(eventId);
        }
      }
      case TOGGLE_SOUND_FEEDBACK ->
          result =
              pipeline.returnFeedback(eventId, Feedback.speech(toggleSoundFeedbackAnnouncement()));
      case OPEN_TTS_SETTINGS -> {
        if (SettingsUtils.allowLinksOutOfSettings(accessibilityService.getApplicationContext())) {
          openTtsSettings();
        }
      }
      case RESET_SPEECH_SETTINGS -> {
        resetSpeechSettings();
        result =
            pipeline.returnFeedback(
                eventId,
                Feedback.speech(
                    accessibilityService.getString(R.string.reset_speech_rate_and_pitch)));
      }
      case SHOW_KEYBOARD_SHORTCUTS_DIALOG ->
          result =
              pipeline.returnFeedback(eventId, Feedback.keyboard(SHOW_KEYBOARD_SHORTCUTS_DIALOG));
      case SHOW_TUTORIAL -> showTutorial();
      case SHOW_LEARN_MODE_PAGE -> openLearnModePage();
      case TOGGLE_VOICE_FEEDBACK ->
          result = pipeline.returnFeedback(eventId, Feedback.speech(TOGGLE_VOICE_FEEDBACK));
      case TOGGLE_BRAILLE_CONTRACTED_MODE ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.performBrailleDisplayAction(
                      Feedback.BrailleDisplay.Action.TOGGLE_BRAILLE_CONTRACTED_MODE));
      case TOGGLE_BRAILLE_ON_SCREEN_OVERLAY ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.performBrailleDisplayAction(
                      Feedback.BrailleDisplay.Action.TOGGLE_BRAILLE_ON_SCREEN_OVERLAY));
      case REPORT_ISSUE -> reportIssue();
      case CYCLE_PUNCTUATION_VERBOSITY ->
          selectorController.cycleSpeakPunctuationVerbosity(
              eventId, TalkBackAnalytics.TYPE_KEYBOARD);
      case CYCLE_TYPING_ECHO ->
          result =
              VerbosityPreferences.cycleKeyboardTypingEcho(accessibilityService, pipeline, eventId);
      case OPEN_VOICE_COMMANDS ->
          result =
              pipeline.returnFeedback(
                  eventId,
                  Feedback.voiceRecognition(
                      START_LISTENING_IF_SCREEN_NOT_LOCKED,
                      /* checkDialog= */ true,
                      gestureShortcutMapping.nodeMenuShortcut() != null
                          ? gestureShortcutMapping.nodeMenuShortcut().toString()
                          : ""));
      default -> {}
    }
    if (!result) {
      pipeline.returnFeedback(eventId, Feedback.sound(R.raw.complete));
    }
    return result;
  }

  /**
   * Interrupts the actions if FullScreenReadActor is activated and the action is not a navigation.
   *
   * @param performedAction the action generating from key combos.
   */
  void interruptByKeyCombo(TalkBackPhysicalKeyboardShortcut performedAction) {
    if (performedAction
            == TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT_DEFAULT /* next in default keymap */
        || performedAction == TalkBackPhysicalKeyboardShortcut.NAVIGATE_PREVIOUS_DEFAULT
        /* previous in default keymap */
        || performedAction
            == TalkBackPhysicalKeyboardShortcut.NAVIGATE_NEXT /* next in classic keymap */
        || performedAction
            == TalkBackPhysicalKeyboardShortcut
                .NAVIGATE_PREVIOUS /* previous in classic keymap */) {
      return;
    }
    if (fullScreenReadActor.isActive()) {
      fullScreenReadActor.interrupt();
    }
  }

  private boolean performNativeOrWebNavigationKeyCombo(
      CursorGranularity granularity, @TargetType int targetType, boolean forward, EventId eventId) {
    if ((granularity.isNativeMacroGranularity()
            && !FeatureFlagReader.enableHeadingControlLinkNativeNavigation(accessibilityService))
        || (granularity.isTableNavigationGranularity()
            && !FeatureFlagReader.enableTableNavigation(accessibilityService))
        || actorState.getDirectionNavigation().hasNavigableWebContent()) {
      return performWebNavigationKeyCombo(targetType, forward, eventId);
    } else {
      return pipeline.returnFeedback(
          eventId,
          Feedback.focusDirection(
                  forward
                      ? TraversalStrategy.SEARCH_FOCUS_FORWARD
                      : TraversalStrategy.SEARCH_FOCUS_BACKWARD)
              .setInputMode(INPUT_MODE_KEYBOARD)
              .setGranularity(granularity));
    }
  }

  private String toggleSoundFeedbackAnnouncement() {
    SharedPreferences sharedPreferences =
        SharedPreferencesUtils.getSharedPreferences(accessibilityService);
    boolean updatedSoundFeedbackValue =
        !sharedPreferences.getBoolean(
            accessibilityService.getString(R.string.pref_soundback_key),
            accessibilityService.getResources().getBoolean(R.bool.pref_soundback_default));
    sharedPreferences
        .edit()
        .putBoolean(
            accessibilityService.getString(R.string.pref_soundback_key), updatedSoundFeedbackValue)
        .apply();
    return accessibilityService.getString(
        R.string.sound_feedback_state,
        updatedSoundFeedbackValue
            ? accessibilityService.getString(R.string.value_on)
            : accessibilityService.getString(R.string.value_off));
  }

  private boolean performWebNavigationKeyCombo(
      @TargetType int targetType, boolean forward, EventId eventId) {
    @SearchDirection
    int direction =
        forward ? TraversalStrategy.SEARCH_FOCUS_FORWARD : TraversalStrategy.SEARCH_FOCUS_BACKWARD;
    return pipeline.returnFeedback(
        eventId,
        Feedback.focusDirection(direction)
            .setInputMode(INPUT_MODE_KEYBOARD)
            .setHtmlTargetType(targetType));
  }

  private void openManageKeyboardShortcuts() {
    Intent intent = new Intent(accessibilityService, TalkBackPreferencesActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.putExtra(FRAGMENT_NAME, TalkBackKeyboardShortcutPreferenceFragment.class.getName());
    accessibilityService.startActivity(intent);
  }

  private void openTalkBackSettings() {
    Intent intent = new Intent(accessibilityService, TalkBackPreferencesActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    accessibilityService.startActivity(intent);
  }

  private void openTtsSettings() {
    Intent intent = new Intent(TalkBackService.INTENT_TTS_SETTINGS);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    accessibilityService.startActivity(intent);
  }

  private void showTutorial() {
    Intent intent =
        TrainingActivity.createTrainingIntent(accessibilityService, TRAINING_ID_TUTORIAL_KEYBOARD);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    accessibilityService.startActivity(intent);
  }

  private void openLearnModePage() {
    Intent intent = TutorialInitiator.createPracticeKeyboardShortcutsIntent(accessibilityService);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    accessibilityService.startActivity(intent);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
  public void reportIssue() {
    HelpAndFeedbackUtils.launchFeedbackViaKeyboard(accessibilityService);
  }

  private void resetSpeechSettings() {
    speechRateAndPitchActor.resetSpeechRate();
    speechRateAndPitchActor.resetSpeechPitch();
  }

  /**
   * Creates focus direction builder for focus navigation in Container and Window granularity by
   * using classic keymap actions.
   */
  private FocusDirection.Builder createFocusDirectionBuilder(boolean navigateToNext) {
    int direction = navigateToNext ? SEARCH_FOCUS_FORWARD : SEARCH_FOCUS_BACKWARD;
    FocusDirection.Builder defaultBuilder =
        Feedback.focusDirection(direction)
            .setInputMode(INPUT_MODE_KEYBOARD)
            .setWrap(true)
            .setScroll(true)
            .setDefaultToInputFocus(true);
    CursorGranularity granularity = stateReader.getCurrentGranularity();
    if (granularity == null) {
      return defaultBuilder;
    }
    return switch (granularity) {
      case CONTAINER -> defaultBuilder.setToContainer(true);
      case WINDOWS -> defaultBuilder.setToWindow(true);
      default -> defaultBuilder;
    };
  }

  private void changeSpeechPitch(EventId eventId, boolean isIncrease) {
    String feedbackString;
    boolean changed = speechRateAndPitchActor.changeSpeechPitch(isIncrease);
    if (changed) {
      feedbackString =
          accessibilityService.getString(
              isIncrease ? R.string.speech_pitch_increase : R.string.speech_pitch_decrease);
    } else {
      feedbackString =
          accessibilityService.getString(
              isIncrease ? R.string.speech_pitch_maximum : R.string.speech_pitch_minimum);
    }

    pipeline.returnFeedback(
        eventId,
        Feedback.speech(
            feedbackString,
            SpeakOptions.create()
                .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
                .setFlags(
                    FeedbackItem.FLAG_NO_HISTORY
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE)));

    if (actorState.getDimScreen().isDimmingEnabled()) {
      // No need to show the UI when the screen is dimming.
      return;
    }

    // Shows a quick menu overlay for visual notification.
    pipeline.returnFeedback(
        eventId,
        Feedback.showSelectorUI(
            SELECTOR_ITEM_ACTION_OVERLAY, feedbackString, /* showIcon= */ false));
  }

  private boolean readCurrentUrl(EventId eventId) {
    BooleanSupplier generateNoUrlFeedback =
        () ->
            pipeline.returnFeedback(
                eventId,
                Feedback.speech(
                    accessibilityService.getString(R.string.read_current_url_no_url_available)));
    AccessibilityNodeInfoCompat rootNode =
        AccessibilityNodeInfoUtils.toCompat(accessibilityService.getRootInActiveWindow());
    if (rootNode == null) {
      LogUtils.v(TAG, "READ_CURRENT_URL: no root node");
      return generateNoUrlFeedback.getAsBoolean();
    }
    AccessibilityNodeInfoCompat urlBarNode = WebInterfaceUtils.findUrlBar(rootNode);
    if (urlBarNode == null) {
      LogUtils.v(TAG, "READ_CURRENT_URL: Current window does not have a url bar");
      return generateNoUrlFeedback.getAsBoolean();
    }
    // This can legitimately happen for a new/blank tab. The text in the url bar may be empty or
    // may not be a valid url (ex: "Search or type a URL").
    if (TextUtils.isEmpty(urlBarNode.getText())
        || !Patterns.WEB_URL.matcher(urlBarNode.getText()).matches()) {
      return generateNoUrlFeedback.getAsBoolean();
    }
    return pipeline.returnFeedback(eventId, Feedback.speech(urlBarNode.getText()));
  }
}
