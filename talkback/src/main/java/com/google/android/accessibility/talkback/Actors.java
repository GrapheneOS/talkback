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

package com.google.android.accessibility.talkback;

import static android.content.Intent.ACTION_ASSIST;
import static android.content.Intent.ACTION_VOICE_COMMAND;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.google.android.accessibility.talkback.Feedback.AdjustValue.Action.DECREASE_VALUE;
import static com.google.android.accessibility.talkback.Feedback.AdjustVolume.Action.DECREASE_VOLUME;
import static com.google.android.accessibility.talkback.Feedback.SpeechRate.Action.INCREASE_RATE;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CONTEXT;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.output.SpeechController.UTTERANCE_GROUP_CONTENT_HINTS;
import static com.google.android.accessibility.utils.preference.PreferencesActivity.FRAGMENT_NAME;
import static com.google.android.accessibility.utils.traversal.TraversalStrategy.SEARCH_FOCUS_FORWARD;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.View.OnClickListener;
import android.widget.Toast;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.android.talkback.TalkBackPreferencesActivity;
import com.google.android.accessibility.talkback.Feedback.AdjustValue;
import com.google.android.accessibility.talkback.Feedback.AdjustVolume;
import com.google.android.accessibility.talkback.Feedback.BrailleDisplay;
import com.google.android.accessibility.talkback.Feedback.ContinuousRead;
import com.google.android.accessibility.talkback.Feedback.DeviceInfo;
import com.google.android.accessibility.talkback.Feedback.DimScreen;
import com.google.android.accessibility.talkback.Feedback.EditText;
import com.google.android.accessibility.talkback.Feedback.Focus;
import com.google.android.accessibility.talkback.Feedback.FocusDirection;
import com.google.android.accessibility.talkback.Feedback.GeminiRequest;
import com.google.android.accessibility.talkback.Feedback.GeminiResultDialog;
import com.google.android.accessibility.talkback.Feedback.Gesture;
import com.google.android.accessibility.talkback.Feedback.ImageCaption;
import com.google.android.accessibility.talkback.Feedback.ImageCaptionResult;
import com.google.android.accessibility.talkback.Feedback.InterruptGroup;
import com.google.android.accessibility.talkback.Feedback.InterruptLevel;
import com.google.android.accessibility.talkback.Feedback.Keyboard;
import com.google.android.accessibility.talkback.Feedback.Label;
import com.google.android.accessibility.talkback.Feedback.Language;
import com.google.android.accessibility.talkback.Feedback.NodeAction;
import com.google.android.accessibility.talkback.Feedback.PassThroughMode;
import com.google.android.accessibility.talkback.Feedback.ScreenOverviewResult;
import com.google.android.accessibility.talkback.Feedback.Scroll;
import com.google.android.accessibility.talkback.Feedback.ServiceFlag;
import com.google.android.accessibility.talkback.Feedback.ShowToast;
import com.google.android.accessibility.talkback.Feedback.Sound;
import com.google.android.accessibility.talkback.Feedback.Speech;
import com.google.android.accessibility.talkback.Feedback.SpeechRate;
import com.google.android.accessibility.talkback.Feedback.SystemAction;
import com.google.android.accessibility.talkback.Feedback.TalkBackUI;
import com.google.android.accessibility.talkback.Feedback.TouchLatency;
import com.google.android.accessibility.talkback.Feedback.TriggerIntent;
import com.google.android.accessibility.talkback.Feedback.UiChange;
import com.google.android.accessibility.talkback.Feedback.UniversalSearch;
import com.google.android.accessibility.talkback.Feedback.UpdateSpeechOverlayLayout;
import com.google.android.accessibility.talkback.Feedback.Vibration;
import com.google.android.accessibility.talkback.Feedback.VoiceRecognition;
import com.google.android.accessibility.talkback.Feedback.WebAction;
import com.google.android.accessibility.talkback.TalkBackService.ServiceFlagRequester;
import com.google.android.accessibility.talkback.actor.AutoScrollActor;
import com.google.android.accessibility.talkback.actor.BrailleDisplayActor;
import com.google.android.accessibility.talkback.actor.DimScreenActor;
import com.google.android.accessibility.talkback.actor.DirectionNavigationActor;
import com.google.android.accessibility.talkback.actor.FocusActor;
import com.google.android.accessibility.talkback.actor.FocusActorForScreenStateChange;
import com.google.android.accessibility.talkback.actor.FocusActorForTapAndTouchExploration;
import com.google.android.accessibility.talkback.actor.FullScreenReadActor;
import com.google.android.accessibility.talkback.actor.GestureReporter;
import com.google.android.accessibility.talkback.actor.ImageCaptioner;
import com.google.android.accessibility.talkback.actor.KeyboardActor;
import com.google.android.accessibility.talkback.actor.LanguageActor;
import com.google.android.accessibility.talkback.actor.NodeActionPerformer;
import com.google.android.accessibility.talkback.actor.NumberAdjustor;
import com.google.android.accessibility.talkback.actor.PassThroughModeActor;
import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import com.google.android.accessibility.talkback.actor.SystemActionPerformer;
import com.google.android.accessibility.talkback.actor.TalkBackUIActor;
import com.google.android.accessibility.talkback.actor.TextEditActor;
import com.google.android.accessibility.talkback.actor.TouchLatencyAdjustor;
import com.google.android.accessibility.talkback.actor.TypoNavigator;
import com.google.android.accessibility.talkback.actor.VolumeAdjustor;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.Action;
import com.google.android.accessibility.talkback.actor.search.SearchScreenNodeStrategy;
import com.google.android.accessibility.talkback.actor.search.UniversalSearchActor;
import com.google.android.accessibility.talkback.actor.voicecommands.VoiceCommandActor;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.compositor.WindowContentChangeAnnouncementFilter;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.action.NavigationAction;
import com.google.android.accessibility.talkback.labeling.TalkBackLabelManager;
import com.google.android.accessibility.talkback.preference.base.AutomaticDescriptionsFragment;
import com.google.android.accessibility.talkback.training.TutorialInitiator;
import com.google.android.accessibility.utils.AccessibilityNode;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils.Constants;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;
import com.google.android.accessibility.utils.output.FeedbackController;
import com.google.android.accessibility.utils.output.SpeechCleanupUtils.PunctuationVerbosity;
import com.google.android.accessibility.utils.output.SpeechControllerImpl;
import com.google.android.accessibility.utils.output.TextToSpeechBubbleOverlay;
import com.google.android.accessibility.utils.output.TextToSpeechOverlay;
import com.google.android.accessibility.utils.output.TextToSpeechSimpleOverlay;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Objects;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Pipeline stage for feedback execution. REFERTO */
class Actors {

  private static final String LOG_TAG = "Actors";

  //////////////////////////////////////////////////////////////////////////
  // Member data
  // TODO: Add more actors for braille, UI-actions.

  private final Context context;
  private final TalkBackAnalytics analytics;
  private final DimScreenActor dimmer;
  private final SpeechControllerImpl speaker;
  private final FullScreenReadActor continuousReader;
  private final FeedbackController soundAndVibration;
  private final AutoScrollActor scroller;
  private final FocusActor focuser;
  private final FocusActorForScreenStateChange focuserWindowChange;
  private final FocusActorForTapAndTouchExploration focuserTouch;
  private final DirectionNavigationActor directionNavigator;
  private final TextEditActor editor;
  private final TalkBackLabelManager labeler;
  private final NodeActionPerformer nodeActionPerformer;
  private final SystemActionPerformer systemActionPerformer;
  private final PassThroughModeActor passThroughModeActor;
  private final LanguageActor languageSwitcher;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;
  private final TalkBackUIActor talkBackUIActor;
  private final SpeechRateAndPitchActor speechRateAndPitchActor;
  private final NumberAdjustor numberAdjustor;
  private final TypoNavigator typoNavigator;
  private final VolumeAdjustor volumeAdjustor;
  private final ActorStateWritable actorState;
  private final VoiceCommandActor voiceCommandActor;
  private final GestureReporter gestureReporter;
  private final KeyboardActor keyboardActor;
  private final ImageCaptioner imageCaptioner;
  private final UniversalSearchActor universalSearchActor;
  private final GeminiActor geminiActor;
  private final TouchLatencyAdjustor touchLatencyAdjustor;
  private final ServiceFlagRequester serviceFlagRequester;
  private final BrailleDisplayActor brailleDisplayActor;
  private final InputDeviceMonitor inputDeviceMonitor;

  //////////////////////////////////////////////////////////////////////////
  // Construction methods

  public Actors(
      Context context,
      TalkBackAnalytics analytics,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      DimScreenActor dimmer,
      SpeechControllerImpl speaker,
      FullScreenReadActor continuousReader,
      FeedbackController soundAndVibration,
      AutoScrollActor scroller,
      FocusActor focuser,
      FocusActorForScreenStateChange focuserWindowChange,
      FocusActorForTapAndTouchExploration focuserTouch,
      DirectionNavigationActor directionNavigator,
      TextEditActor editor,
      TalkBackLabelManager labeler,
      NodeActionPerformer nodeActionPerformer,
      SystemActionPerformer systemActionPerformer,
      LanguageActor languageSwitcher,
      @Nullable PassThroughModeActor passThroughModeActor,
      TalkBackUIActor talkBackUIActor,
      SpeechRateAndPitchActor speechRateAndPitchActor,
      NumberAdjustor numberAdjustor,
      TypoNavigator typoNavigator,
      VolumeAdjustor volumeAdjustor,
      VoiceCommandActor voiceCommandActor,
      GestureReporter gestureReporter,
      KeyboardActor keyboardActor,
      ImageCaptioner imageCaptioner,
      UniversalSearchActor universalSearchActor,
      GeminiActor geminiActor,
      TouchLatencyAdjustor touchLatencyAdjustor,
      ServiceFlagRequester serviceFlagRequester,
      BrailleDisplayActor brailleDisplayActor,
      InputDeviceMonitor inputDeviceMonitor) {
    this.context = context;
    this.analytics = analytics;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.dimmer = dimmer;
    this.speaker = speaker;
    this.continuousReader = continuousReader;
    this.soundAndVibration = soundAndVibration;
    this.scroller = scroller;
    this.focuser = focuser;
    this.focuserWindowChange = focuserWindowChange;
    this.focuserTouch = focuserTouch;
    this.directionNavigator = directionNavigator;
    this.editor = editor;
    this.labeler = labeler;
    this.nodeActionPerformer = nodeActionPerformer;
    this.systemActionPerformer = systemActionPerformer;
    this.languageSwitcher = languageSwitcher;
    this.passThroughModeActor = passThroughModeActor;
    this.talkBackUIActor = talkBackUIActor;
    this.speechRateAndPitchActor = speechRateAndPitchActor;
    this.numberAdjustor = numberAdjustor;
    this.typoNavigator = typoNavigator;
    this.volumeAdjustor = volumeAdjustor;
    this.voiceCommandActor = voiceCommandActor;
    this.gestureReporter = gestureReporter;
    this.keyboardActor = keyboardActor;
    this.imageCaptioner = imageCaptioner;
    this.universalSearchActor = universalSearchActor;
    this.geminiActor = geminiActor;
    this.serviceFlagRequester = serviceFlagRequester;
    this.touchLatencyAdjustor = touchLatencyAdjustor;
    this.brailleDisplayActor = brailleDisplayActor;
    this.inputDeviceMonitor = inputDeviceMonitor;

    actorState =
        new ActorStateWritable(
            dimmer.state,
            speaker.state,
            continuousReader.state,
            scroller.stateReader,
            focuser.getHistory(),
            directionNavigator.state,
            nodeActionPerformer.stateReader,
            languageSwitcher.state,
            speechRateAndPitchActor.rateState,
            passThroughModeActor.state,
            labeler.stateReader(),
            geminiActor.state,
            universalSearchActor.state,
            editor.state);
    // Focuser stores some actor-state in ActorState, because focuser does not use that state
    // internally, only for communication to interpreters.
    this.focuser.setActorState(actorState);
    this.systemActionPerformer.setActorState(actorState);
    ActorState actorStateReadOnly = new ActorState(actorState);
    this.directionNavigator.setActorState(actorStateReadOnly);
    this.focuserWindowChange.setActorState(actorStateReadOnly);
    this.languageSwitcher.setActorState(actorStateReadOnly);
    this.focuserTouch.setActorState(actorStateReadOnly);
    this.imageCaptioner.setActorState(actorStateReadOnly);
    this.analytics.setActorState(actorStateReadOnly);
  }

  public void setPipelineEventReceiver(Pipeline.EventReceiver pipelineEventReceiver) {
    scroller.setPipelineEventReceiver(pipelineEventReceiver);
    directionNavigator.setPipelineEventReceiver(pipelineEventReceiver);
  }

  public void setPipelineFeedbackReturner(Pipeline.FeedbackReturner pipelineFeedbackReturner) {
    scroller.setPipeline(pipelineFeedbackReturner);
    dimmer.setPipeline(pipelineFeedbackReturner);
    continuousReader.setPipeline(pipelineFeedbackReturner);
    directionNavigator.setPipeline(pipelineFeedbackReturner);
    editor.setPipeline(pipelineFeedbackReturner);
    focuser.setPipeline(pipelineFeedbackReturner);
    focuserWindowChange.setPipeline(pipelineFeedbackReturner);
    languageSwitcher.setPipeline(pipelineFeedbackReturner);
    if (passThroughModeActor != null) {
      passThroughModeActor.setPipeline(pipelineFeedbackReturner);
    }
    focuserTouch.setPipeline(pipelineFeedbackReturner);
    numberAdjustor.setPipeline(pipelineFeedbackReturner);
    typoNavigator.setPipeline(pipelineFeedbackReturner);
    voiceCommandActor.setPipeline(pipelineFeedbackReturner);
    imageCaptioner.setPipeline(pipelineFeedbackReturner);
    universalSearchActor.setPipeline(pipelineFeedbackReturner);
    geminiActor.setPipeline(pipelineFeedbackReturner);
    touchLatencyAdjustor.setPipeline(pipelineFeedbackReturner);
  }

  public void setUserInterface(UserInterface userInterface) {
    directionNavigator.setUserInterface(userInterface);
  }

  //////////////////////////////////////////////////////////////////////////
  // Pipeline methods

  /** Returns a read-only actor state data structure. */
  public ActorState getState() {
    return new ActorState(actorState);
  }

  /** Executes feedback and modifies actorState. Returns success flag. */
  public boolean act(@Nullable EventId eventId, Feedback.Part part) {
    LogUtils.d(LOG_TAG, "act() eventId=%s part=%s", eventId, part);

    boolean success = true;

    // Custom labels
    @Nullable Label label = part.label();
    if (label != null && label.node() != null) {
      success &=
          switch (label.action()) {
            case SET ->
                labeler.stateReader().supportsLabel(label.node())
                    && labeler.setLabel(label.node(), label.text());
          };
    }

    // Continuous reading
    @Nullable ContinuousRead continuousRead = part.continuousRead();
    if (continuousRead != null && continuousRead.action() != null) {
      switch (continuousRead.action()) {
        case START_AT_TOP -> continuousReader.startReadingFromBeginning(eventId);
        case START_AT_CURSOR -> continuousReader.startReadingFromFocusedNode(eventId);
        case READ_FOCUSED_CONTENT -> continuousReader.readFocusedContent(eventId);
        case INTERRUPT -> continuousReader.interrupt();
        case IGNORE -> continuousReader.ignore();
      }
    }

    @Nullable DimScreen dim = part.dimScreen();
    if (dim != null && dim.action() != null) {
      switch (dim.action()) {
        case BRIGHTEN -> dimmer.disableDimming();
        case DIM -> dimmer.enableDimmingAndShowConfirmDialog();
      }
    }

    // Speech
    @Nullable Speech speech = part.speech();
    if (speech != null && speech.action() != null) {
      var unused = false;
      switch (speech.action()) {
        case SPEAK -> {
          if ((speech.hint() != null)
              && (speech.hintSpeakOptions() != null)
              && (speech.hintSpeakOptions().mCompletedAction != null)) {
            speaker.addUtteranceCompleteAction(
                speaker.peekNextUtteranceId(),
                UTTERANCE_GROUP_CONTENT_HINTS,
                speech.hintSpeakOptions().mCompletedAction);
          }
          if (speech.text() != null) {
            speaker.speak(speech.text(), eventId, speech.options());
          }
        }
        case SAVE_LAST -> speaker.saveLastUtterance();
        case COPY_SAVED -> speaker.copySavedUtteranceToClipboard(eventId);
        case COPY_LAST -> speaker.copyLastUtteranceToClipboard(eventId);
        case REPEAT_SAVED -> speaker.repeatSavedUtterance();
        case REPEAT_LAST -> unused = speaker.repeatLastUtterance();
        case SPELL_SAVED -> speaker.spellSavedUtterance();
        case SPELL_LAST -> unused = speaker.spellLastUtterance();
        case PAUSE_OR_RESUME -> {
          boolean doPause = speaker.readyToPause();
          continuousReader.pauseOrResumeContinuousReadingState(doPause);
          speaker.pauseOrResumeUtterance(doPause);
        }
        case TOGGLE_VOICE_FEEDBACK -> speaker.toggleVoiceFeedback();
        case SILENCE -> speaker.setSilenceSpeech(true);
        case UNSILENCE -> speaker.setSilenceSpeech(false);
        case INVALIDATE_FREQUENT_CONTENT_CHANGE_CACHE ->
            WindowContentChangeAnnouncementFilter.invalidateRecordNode();
        case SYNTHESIZE -> {
          Bundle speechParams = null;
          if (speech.options() != null) {
            speechParams = speech.options().mSpeechParams;
          }
          speaker.addSpeech(
              speech.text(), speech.synthesizeStatusNotifier(), speechParams, eventId);
        }
        case RESET_FORMATTING_HISTORY -> speaker.clearInlineFormattingHistory();
      }
    }

    @Nullable VoiceRecognition voiceRecognition = part.voiceRecognition();
    if (voiceRecognition != null && voiceRecognition.action() != null) {
      switch (voiceRecognition.action()) {
        case START_LISTENING ->
            voiceCommandActor.getSpeechPermissionAndListen(voiceRecognition.checkDialog());
        case START_LISTENING_IF_SCREEN_NOT_LOCKED ->
            voiceCommandActor.startListeningIfScreenNotLocked(
                voiceRecognition.checkDialog(), voiceRecognition.nodeMenuShortcut());
        case STOP_LISTENING -> voiceCommandActor.stopListening();
        case SHOW_COMMAND_LIST -> voiceCommandActor.showCommandsHelpPage();
      }
    }

    // Sound effects
    @Nullable Sound sound = part.sound();
    if (sound != null) {
      soundAndVibration.playAuditory(
          sound.resourceId(),
          sound.rate(),
          sound.volume(),
          sound.ignoreVolumeAdjustment(),
          eventId,
          sound.separationMillisec());
    }

    // Vibration
    @Nullable Vibration vibration = part.vibration();
    if (vibration != null) {
      soundAndVibration.playHaptic(vibration.resourceId(), eventId);
    }

    // TriggerIntent
    @Nullable TriggerIntent triggerIntent = part.triggerIntent();
    if (triggerIntent != null) {
      Intent intent = null;
      switch (triggerIntent.action()) {
        case TRIGGER_TUTORIAL -> intent = TutorialInitiator.createTutorialIntent(context);
        case TRIGGER_PRACTICE_GESTURE ->
            intent = TutorialInitiator.createPracticeGesturesIntent(context);
        case TRIGGER_ASSISTANT -> {
          // The intent to invoke assistant for watch is different from for phone.
          intent =
              new Intent(FormFactorUtils.isAndroidWear() ? ACTION_ASSIST : ACTION_VOICE_COMMAND);
          intent.setFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_CLEAR_TOP);
        }
        case TRIGGER_BRAILLE_DISPLAY_SETTINGS -> {
          if (FeatureSupport.supportBrailleDisplay(context)) {
            intent = new Intent().setComponent(Constants.BRAILLE_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
          }
        }
        case TRIGGER_IMAGE_DESCRIPTIONS_SETTINGS -> {
          intent = new Intent(context, TalkBackPreferencesActivity.class);
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
          intent.putExtra(FRAGMENT_NAME, AutomaticDescriptionsFragment.class.getName());
        }
      }
      try {
        if (intent != null) {
          context.startActivity(intent);
        }
      } catch (ActivityNotFoundException exception) {
        LogUtils.d(LOG_TAG, intent + " can not be served by any activity.");
      }
    }

    // Language
    @Nullable Language language = part.language();
    if (language != null && language.action() != null) {
      switch (language.action()) {
        case PREVIOUS_LANGUAGE ->
            languageSwitcher.selectPreviousOrNextLanguage(/* isNext= */ false);
        case NEXT_LANGUAGE -> languageSwitcher.selectPreviousOrNextLanguage(/* isNext= */ true);
        case SET_LANGUAGE -> languageSwitcher.setLanguage(language.currentLanguage());
      }
    }

    // Touch latency
    @Nullable TouchLatency touchLatency = part.touchLatency();
    if (touchLatency != null) {
      boolean result = touchLatencyAdjustor.adjustLatency(touchLatency);
      success &= result;
    }

    // System action
    @Nullable SystemAction systemAction = part.systemAction();
    if (systemAction != null) {
      success &= systemActionPerformer.performAction(systemAction.systemActionId());
    }

    // Text editing
    @Nullable EditText edit = part.edit();
    if (edit != null) {
      success &=
          switch (edit.action()) {
            case SELECT_ALL -> editor.selectAll(edit.node(), eventId);
            case SELECT_SEGMENT -> editor.selectSegment(edit.node(), eventId);
            case START_SELECT -> editor.startSelect(edit.node(), eventId);
            case END_SELECT -> editor.endSelect(edit.node(), eventId);
            case COPY -> editor.copy(edit.node(), eventId);
            case CUT -> editor.cut(edit.node(), eventId);
            case PASTE -> editor.paste(edit.node(), eventId);
            case DELETE -> editor.delete(edit.node(), eventId);
            case CURSOR_TO_BEGINNING ->
                editor.cursorToBeginning(edit.node(), edit.stopSelecting(), eventId);
            case CURSOR_TO_END -> editor.cursorToEnd(edit.node(), edit.stopSelecting(), eventId);
            case INSERT -> editor.insert(edit.node(), edit.text(), eventId);
            case TYPO_CORRECTION ->
                editor.correctTypo(edit.node(), edit.text(), edit.spellingSuggestion(), eventId);
            case MOVE_CURSOR -> editor.moveCursor(edit.node(), edit.cursorIndex(), eventId);
            case TOGGLE_VOICE_DICTATION -> editor.toggleVoiceDictation(eventId);
          };
    }

    // Node action
    @Nullable NodeAction nodeAction = part.nodeAction();
    AccessibilityNode nodeActionTarget = (nodeAction == null) ? null : nodeAction.target();
    if (nodeAction != null && nodeActionTarget != null) {
      success &= nodeActionPerformer.performAction(nodeAction, eventId);
    }

    // Scrolling
    @Nullable Scroll scroll = part.scroll();
    if (scroll != null) {
      switch (scroll.action()) {
        case SCROLL ->
            success &=
                scroller.scroll(
                    scroll.userAction(),
                    scroll.node(),
                    scroll.nodeCompat(),
                    scroll.nodeAction(),
                    scroll.source(),
                    scroll.timeout(),
                    scroll.autoScrollAttempt(),
                    scroll.args(),
                    scroll.autoScrollChecker(),
                    eventId);
        case CANCEL_TIMEOUT -> scroller.cancelTimeout();
        case RESET_SCROLL_RECORDS -> scroller.resetScrollActionRecords();
        case ENSURE_ON_SCREEN ->
            success &=
                scroller.ensureOnScreen(
                    scroll.userAction(),
                    scroll.nodeCompat(),
                    scroll.nodeToMoveOnScreen(),
                    scroll.source(),
                    scroll.timeout(),
                    eventId);
        default -> {
          // Do nothing.
        }
      }
    }

    // Focus
    @Nullable Focus focus = part.focus();
    if (focus != null && focus.action() != null) {
      switch (focus.action()) {
        case FOCUS -> {
          if (focus.target() != null) {
            success &=
                focuser.setAccessibilityFocus(
                    focus.target(),
                    focus.forceRefocus(),
                    Objects.requireNonNull(focus.focusActionInfo()),
                    eventId);
          }
        }
        case CLEAR -> focuser.clearAccessibilityFocus(eventId);
        case CACHE -> success &= focuser.cacheNodeToRestoreFocus(focus.target());
        case MUTE_NEXT_FOCUS -> focuser.setMuteNextFocus();
        case RENEW_ENSURE_FOCUS -> focuser.renewEnsureFocus();
        case RESTORE_ON_NEXT_WINDOW -> focuser.overrideNextFocusRestorationForWindowTransition();
        case RESTORE_TO_CACHE -> success &= focuser.restoreFocus(eventId);
        case CLEAR_CACHED -> success &= focuser.popCachedNodeToRestoreFocus();
        case INITIAL_FOCUS_RESTORE ->
            success &= focuserWindowChange.restoreLastFocusedNode(focus.screenState(), eventId);
        case INITIAL_FOCUS_FOLLOW_INPUT ->
            success &=
                focuserWindowChange.syncAccessibilityFocusAndInputFocus(
                    focus.screenState(), eventId);
        case INITIAL_FOCUS_FIRST_CONTENT ->
            success &=
                focuserWindowChange.focusOnRequestInitialNodeOrFirstFocusableNonTitleNode(
                    focus.screenState(), eventId);
        case FOCUS_FOR_TOUCH ->
            success &=
                focuserTouch.setAccessibilityFocus(focus.target(), focus.forceRefocus(), eventId);
        case CLICK_NODE -> success &= focuser.clickNode(focus.target(), eventId);
        case DOUBLE_CLICK_NODE -> success &= focuser.doubleClickNode(focus.target(), eventId);
        case LONG_CLICK_NODE -> success &= focuser.longClickNode(focus.target(), eventId);
        case CLICK_CURRENT -> success &= focuser.clickCurrentFocus(eventId);
        case DOUBLE_CLICK_CURRENT -> success &= focuser.doubleClickCurrentFocus(eventId);
        case LONG_CLICK_CURRENT -> success &= focuser.longClickCurrentFocus(eventId);
        case CLICK_ANCESTOR -> success &= focuser.clickCurrentHierarchical(eventId);
        case SEARCH_FROM_TOP -> {
          if (focus.searchKeyword() != null) {
            success &=
                universalSearchActor.searchAndFocus(
                    /* startAtRoot= */ true, focus.searchKeyword(), directionNavigator);
          }
        }
        case SEARCH_AGAIN ->
            success &=
                universalSearchActor.searchAndFocus(
                    /* startAtRoot= */ false,
                    universalSearchActor.state.getLastKeyword(),
                    directionNavigator);
        case ENSURE_ACCESSIBILITY_FOCUS_ON_SCREEN ->
            success &= focuser.ensureAccessibilityFocusOnScreen(eventId);
        case STEAL_NEXT_WINDOW_NAVIGATION ->
            success &=
                directionNavigator.updateStealNextWindowNavigation(
                    focus.stealNextWindowTarget(), focus.stealNextWindowTargetDirection());
        case READ_NODE_LINK_URL -> success &= focuser.readFocusedContentLinkUrl(eventId);
      }
    }

    // PassThroughMode
    @Nullable PassThroughMode passThroughMode = part.passThroughMode();
    if (passThroughMode != null && passThroughModeActor != null) {
      switch (passThroughMode.action()) {
        case ENABLE_PASSTHROUGH ->
            passThroughModeActor.setTouchExplorePassThrough(/* enable= */ true);
        case DISABLE_PASSTHROUGH ->
            passThroughModeActor.setTouchExplorePassThrough(/* enable= */ false);
        case PASSTHROUGH_CONFIRM_DIALOG -> passThroughModeActor.showEducationDialog();
        case STOP_TIMER -> passThroughModeActor.cancelPassThroughGuardTimer();
        case LOCK_PASS_THROUGH ->
            passThroughModeActor.lockTouchExplorePassThrough(passThroughMode.region());
        default -> {}
      }
    }

    // SpeechRate
    @Nullable SpeechRate speechRate = part.speechRate();
    if (speechRate != null && speechRateAndPitchActor != null) {
      switch (speechRate.action()) {
        case INCREASE_RATE, DECREASE_RATE ->
            success &=
                speechRateAndPitchActor.changeSpeechRate(
                    /* isIncrease= */ speechRate.action() == INCREASE_RATE);
        default -> {}
      }
    }

    // AdjustValue
    @Nullable AdjustValue adjustValue = part.adjustValue();
    if (adjustValue != null) {
      switch (adjustValue.action()) {
        case INCREASE_VALUE, DECREASE_VALUE ->
            success &= numberAdjustor.adjustValue(adjustValue.action() == DECREASE_VALUE);
        default -> {}
      }
    }

    // NavigateTypo
    Feedback.NavigateTypo navigateTypo = part.navigateTypo();
    if (navigateTypo != null) {
      success &=
          typoNavigator.navigate(
              eventId, navigateTypo.isNext(), navigateTypo.useInputFocusIfEmpty());
    }

    // VolumeValue
    @Nullable AdjustVolume adjustVolume = part.adjustVolume();
    if (adjustVolume != null) {
      success &=
          switch (adjustVolume.action()) {
            case INCREASE_VOLUME, DECREASE_VOLUME ->
                volumeAdjustor.adjustVolume(
                    adjustVolume.action() == DECREASE_VOLUME, adjustVolume.streamType());
          };
    }

    // FocusDirection
    @Nullable FocusDirection direction = part.focusDirection();
    if (direction != null) {
      switch (direction.action()) {
        case NEXT -> {
          var unused =
              directionNavigator.navigateWithSpecifiedGranularity(
                  SEARCH_FOCUS_FORWARD,
                  direction.granularity(),
                  direction.wrap(),
                  direction.inputMode(),
                  direction.skipEdgeCheck(),
                  eventId);
        }
        case FOLLOW ->
            directionNavigator.followTo(
                direction.targetNode(), direction.granularity(), direction.direction(), eventId);
        case NEXT_PAGE -> directionNavigator.more(eventId);
        case PREVIOUS_PAGE -> directionNavigator.less(eventId);
        case SCROLL_UP -> directionNavigator.scrollDirection(eventId, NavigationAction.SCROLL_UP);
        case SCROLL_DOWN ->
            directionNavigator.scrollDirection(eventId, NavigationAction.SCROLL_DOWN);
        case SCROLL_LEFT ->
            directionNavigator.scrollDirection(eventId, NavigationAction.SCROLL_LEFT);
        case SCROLL_RIGHT ->
            directionNavigator.scrollDirection(eventId, NavigationAction.SCROLL_RIGHT);
        case TOP -> directionNavigator.jumpToTop(direction.inputMode(), eventId);
        case BOTTOM -> directionNavigator.jumpToBottom(direction.inputMode(), eventId);
        case SET_GRANULARITY ->
            directionNavigator.setGranularity(
                direction.granularity(), direction.targetNode(), direction.fromUser(), eventId);
        case NEXT_GRANULARITY -> directionNavigator.nextGranularity(eventId);
        case PREVIOUS_GRANULARITY -> directionNavigator.previousGranularity(eventId);
        case SELECTION_MODE_ON ->
            directionNavigator.setSelectionModeActive(direction.targetNode(), eventId);
        case SELECTION_MODE_OFF -> directionNavigator.setSelectionModeInactive();
        case NAVIGATE -> {
          // In case when the user changes granularity linearly with gesture, or change setting with
          // selector, we cannot confirm the change until the user performs a navigation action.
          analytics.logPendingChanges();
          if (direction.toContainer()) {
            success &=
                directionNavigator.nextContainer(
                    direction.direction(), direction.inputMode(), eventId);
          } else if (direction.toWindow()) {
            success &=
                directionNavigator.navigateToNextOrPreviousWindow(
                    direction.direction(),
                    direction.defaultToInputFocus(),
                    direction.inputMode(),
                    eventId);
          } else if (direction.hasHtmlTargetType()) {
            success &=
                directionNavigator.navigateToHtmlElement(
                    direction.htmlTargetType(),
                    direction.direction(),
                    direction.inputMode(),
                    eventId);
          } else if (direction.hasTableTargetType()) {
            success &=
                directionNavigator.tableNavigation(
                    eventId, direction.direction(), direction.tableTargetType());
          } else if (direction.granularity() != null) {
            success &=
                directionNavigator.navigateWithSpecifiedGranularity(
                    direction.direction(),
                    direction.granularity(),
                    direction.wrap(),
                    direction.inputMode(),
                    direction.skipEdgeCheck(),
                    eventId);
          } else {
            success &=
                directionNavigator.navigate(
                    direction.direction(),
                    direction.wrap(),
                    direction.scroll(),
                    direction.defaultToInputFocus(),
                    direction.inputMode(),
                    direction.skipEdgeCheck(),
                    eventId);
          }
        }
      }
    }

    // Web action
    @Nullable WebAction webAction = part.webAction();
    if (webAction != null) {
      success &=
          switch (webAction.action()) {
            case PERFORM_ACTION -> focuser.getWebActor().performAction(webAction, eventId);
            case HTML_DIRECTION ->
                focuser
                    .getWebActor()
                    .navigateToHtmlElement(
                        webAction.target(), webAction.navigationAction(), eventId);
          };
    }

    // TalkBack UI
    @Nullable TalkBackUI talkBackUI = part.talkBackUI();
    if (talkBackUI != null) {
      success &=
          switch (talkBackUI.action()) {
            case SHOW_GESTURE_OR_KEYBOARD_ACTION_UI, SHOW_SELECTOR_UI ->
                talkBackUIActor.showQuickMenu(talkBackUI);
            case HIDE -> talkBackUIActor.hide(talkBackUI.type());
            case SUPPORT -> talkBackUIActor.setSupported(talkBackUI.type(), true);
            case NOT_SUPPORT -> talkBackUIActor.setSupported(talkBackUI.type(), false);
          };
    }

    // Show Toast
    @Nullable ShowToast showToast = part.showToast();
    if (showToast != null) {
      switch (showToast.action()) {
        case SHOW ->
            Toast.makeText(
                    context,
                    showToast.message(),
                    showToast.durationIsLong() ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT)
                .show();
      }
    }

    // Gesture
    @Nullable Gesture gesture = part.gesture();
    if (gesture != null) {
      success &=
          switch (gesture.action()) {
            case SAVE -> gestureReporter.record(gesture.currentGesture());
            case REPORT -> gestureReporter.report();
          };
    }

    // Keyboard events
    @Nullable Keyboard keyboard = part.keyboard();
    if (keyboard != null) {
      switch (keyboard.action()) {
        case SHOW_KEYBOARD_SHORTCUTS_DIALOG -> keyboardActor.showKeyboardShortcutsDialog();
      }
    }

    // Image caption
    @Nullable ImageCaption imageCaption = part.imageCaption();
    if (imageCaption != null) {
      success &=
          switch (imageCaption.action()) {
            case PERFORM_CAPTIONS ->
                imageCaptioner.caption(
                    imageCaption.target(), /* isUserRequested= */ imageCaption.userRequested());
            case PERFORM_CAPTION_WITH_GEMINI ->
                imageCaptioner.captionWithGemini(imageCaption.target());
            case CONFIRM_DOWNLOAD_AND_PERFORM_CAPTIONS ->
                imageCaptioner.confirmDownloadAndPerformCaption(imageCaption.target());
            case INITIALIZE_ICON_DETECTION -> imageCaptioner.initIconDetection();
            case INITIALIZE_IMAGE_DESCRIPTION -> imageCaptioner.initImageDescription();
            case ONLINE_GEMINI_FEATURE_OPT_IN ->
                imageCaptioner.geminiOptInForManualTrigger(
                    imageCaption.target(), imageCaption.featureType());
            case PERFORM_CAPTION_WITH_ON_DEVICE_GEMINI ->
                imageCaptioner.captionWithOnDeviceGemini(imageCaption.target());
            case ON_DEVICE_DETAILED_DESCRIPTION_OPT_IN ->
                imageCaptioner.geminiOnDeviceOptInForManualTrigger(imageCaption.target());
            case CONFIG_DETAILED_IMAGE_DESCRIPTIONS_SETTINGS ->
                imageCaptioner.geminiConfigDetailedImageDescriptionTrigger(imageCaption.target());
            case PERFORM_SCREEN_OVERVIEW ->
                imageCaptioner.geminiScreenOverview(imageCaption.target());
          };
    }

    // Image Caption Result
    @Nullable ImageCaptionResult imageCaptionResult = part.imageCaptionResult();
    if (imageCaptionResult != null) {
      success &=
          imageCaptioner.handleResultFromGemini(
              imageCaptionResult.requestId(),
              imageCaptionResult.text(),
              imageCaptionResult.isSuccess(),
              imageCaptionResult.finishReason(),
              imageCaptionResult.errorReason(),
              imageCaptionResult.userRequested());
    }

    // Device info
    @Nullable DeviceInfo deviceInfo = part.deviceInfo();
    if (deviceInfo != null) {
      success &=
          switch (deviceInfo.action()) {
            case CONFIG_CHANGED ->
                talkBackUIActor.onConfigurationChanged(deviceInfo.configuration());
          };
    }

    // UI change events
    @Nullable UiChange uiChange = part.uiChange();
    if (uiChange != null) {
      @Nullable Rect sourceBounds = uiChange.sourceBoundsInScreen();
      switch (uiChange.action()) {
        case CLEAR_SCREEN_CACHE -> {
          if (sourceBounds == null) {
            success &= imageCaptioner.clearWholeScreenCache();
          } else {
            success &= imageCaptioner.clearPartialScreenCache(sourceBounds);
          }
        }
        case CLEAR_CACHE_FOR_VIEW -> success &= imageCaptioner.clearCacheForView(sourceBounds);
      }
    }

    // UniversalSearch events
    @Nullable UniversalSearch universalSearch = part.universalSearch();
    if (universalSearch != null) {
      switch (universalSearch.action()) {
        case TOGGLE_SEARCH -> universalSearchActor.toggleSearch(eventId);
        case CANCEL_SEARCH -> universalSearchActor.cancelSearch(eventId);
        case HANDLE_SCREEN_STATE -> universalSearchActor.handleScreenState(eventId);
        case RENEW_OVERLAY -> universalSearchActor.renewOverlay(universalSearch.config());
        case SHOW_HEADING_LIST ->
            universalSearchActor.showVariousList(
                eventId, SearchScreenNodeStrategy.ListType.HEADING);
        case SHOW_LANDMARK_LIST ->
            universalSearchActor.showVariousList(
                eventId, SearchScreenNodeStrategy.ListType.LANDMARK);
        case SHOW_LINK_LIST ->
            universalSearchActor.showVariousList(eventId, SearchScreenNodeStrategy.ListType.LINK);
        case SHOW_CONTROL_LIST ->
            universalSearchActor.showVariousList(
                eventId, SearchScreenNodeStrategy.ListType.CONTROL);
        case SHOW_TABLE_LIST ->
            universalSearchActor.showVariousList(eventId, SearchScreenNodeStrategy.ListType.TABLE);
      }
    }

    @Nullable UpdateSpeechOverlayLayout updateSpeechOverlay = part.updateSpeechOverlayLayout();
    if (updateSpeechOverlay != null) {
      TextToSpeechOverlay overlay = speaker.getTtsOverlay();
      if (overlay != null) {
        overlay.onConfigurationChanged();
      }
      return true;
    }

    // Gemini request
    @Nullable GeminiRequest geminiRequest = part.geminiRequest();
    if (geminiRequest != null) {
      switch (geminiRequest.action()) {
        case REQUEST ->
            geminiActor.requestOnlineGeminiCommand(
                geminiRequest.requestId(),
                Action.IMAGE_DESCRIPTION,
                geminiRequest.text(),
                geminiRequest.image());
        case REQUEST_ON_DEVICE_IMAGE_CAPTIONING ->
            geminiActor.requestAiCoreImageCaptioning(
                geminiRequest.requestId(), geminiRequest.image(), geminiRequest.manualTrigger());
        case MUTE_GEMINI_SOUND -> geminiActor.stopProgressTones();
        case REQUEST_SCREEN_OVERVIEW -> geminiActor.requestScreenOverview(geminiRequest);
        case REQUEST_IMAGE_QNA ->
            geminiActor.requestImageQna(
                Action.IMAGE_QNA, geminiRequest.text(), geminiRequest.imageBytes());
        case REQUEST_SCREEN_QNA ->
            geminiActor.requestImageQna(
                Action.SCREEN_QNA, geminiRequest.text(), geminiRequest.imageBytes());
        case DISMISS_BOTTOM_SHEET -> geminiActor.requestDismissDialog();
      }
    }

    // Screen Overview Result
    @Nullable ScreenOverviewResult screenOverviewResult = part.screenOverviewResult();
    if (screenOverviewResult != null) {
      geminiActor.displayScreenOverviewResultDialog(screenOverviewResult);
    }

    // Dialog result UI for Gemini
    @Nullable GeminiResultDialog geminiResultDialog = part.geminiResultDialog();
    if (geminiResultDialog != null) {
      switch (geminiResultDialog.action()) {
        case IMAGE_CAPTION_RESULT ->
            geminiActor.displayImageCaptioningResultDialog(
                geminiResultDialog.requestId(),
                geminiResultDialog.imageDescriptionResult(),
                geminiResultDialog.iconLabelResult(),
                geminiResultDialog.ocrTextResult(),
                geminiResultDialog.isScreenDescription());
      }
    }

    // Change service flags
    @Nullable ServiceFlag serviceFlag = part.serviceFlag();
    if (serviceFlag != null) {
      switch (serviceFlag.action()) {
        case ENABLE_FLAG ->
            serviceFlagRequester.requestFlag(serviceFlag.flag(), /* requestedState= */ true);
        case DISABLE_FLAG ->
            serviceFlagRequester.requestFlag(serviceFlag.flag(), /* requestedState= */ false);
      }
    }

    // Perform braille display actions
    BrailleDisplay brailleDisplay = part.brailleDisplay();
    if (brailleDisplay != null) {
      switch (brailleDisplay.action()) {
        case TOGGLE_BRAILLE_DISPLAY_ON_OR_OFF -> brailleDisplayActor.switchBrailleDisplayOnOrOff();
        case TOGGLE_BRAILLE_CONTRACTED_MODE -> brailleDisplayActor.toggleBrailleContractedMode();
        case TOGGLE_BRAILLE_ON_SCREEN_OVERLAY -> brailleDisplayActor.toggleBrailleOnScreenOverlay();
        default -> {}
      }
    }

    return success;
  }

  ///////////////////////////////////////////////////////////////////////////////
  // Start and stop methods

  public void onBoot(boolean quiet) {
    speaker.updateTtsEngine(quiet);
  }

  public void prepareForOnUnbind(float finalAnnouncementVolume) {
    // Main thread will be waiting during the TTS announcement, thus in this special case we should
    // not handle TTS callback in main thread.
    speaker.setHandleTtsCallbackInHandlerThread(false);
    // TalkBack is not allowed to display overlay at this state.
    speaker.setOverlayEnabled(false);
    speaker.setSpeechVolume(finalAnnouncementVolume);
  }

  public void onUnbind() {
    speaker.setMute(true);
    soundAndVibration.shutdown();
    geminiActor.onUnbind();
  }

  public void interruptAllFeedback(boolean stopTtsSpeechCompletely) {
    speaker.interrupt(stopTtsSpeechCompletely);
    soundAndVibration.interrupt();
  }

  public void interruptSoundAndVibration() {
    soundAndVibration.interrupt();
  }

  public void clearHintUtteranceCompleteAction(
      @InterruptGroup int group, @InterruptLevel int level) {
    // interrupt-level=2: hints for the focus event
    // interrupt-level=1: hints for the other (non-focus event) hints.
    if (group == Feedback.HINT && level >= 2) {
      speaker.clearHintUtteranceCompleteAction();
    }
  }

  /**
   * Interrupts speech, with some exceptions. Does not interrupt:
   *
   * <ul>
   *   <li>When the WebView is active, because the IME is unintentionally dismissed by WebView's
   *       performAction implementation.
   * </ul>
   */
  public void interruptGentle(EventId eventId) {
    @Nullable AccessibilityNodeInfoCompat currentFocus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (Role.getRole(currentFocus) == Role.ROLE_WEB_VIEW) {
      return;
    }

    if (actorState.continuousRead.isActive()) {
      interruptSoundAndVibration();
    } else {
      interruptAllFeedback(/* stopTtsSpeechCompletely= */ false);
    }
  }

  public void shutdown() {
    speaker.shutdown();
  }

  /////////////////////////////////////////////////////////////////////////////////
  // Parameter setting pass-through methods
  // Keeping preference logic outside actors, in specific accessibility-service code.

  public void setOverlayEnabled(boolean enabled) {
    if (enabled) {
      boolean showBubbleOverlay = false;
      if (FeatureFlagReader.enableShowSpeechBubbleOverlay(context)) {
        boolean hasKeyboard = inputDeviceMonitor.hasPhysicalKeyboard();
        boolean hasPointer = inputDeviceMonitor.hasPointingDevice();

        if (hasKeyboard || hasPointer) {
          showBubbleOverlay = true;
        }
      }

      if (showBubbleOverlay) {
        speaker.setOverlay(
            new TextToSpeechBubbleOverlay(
                context,
                R.string.talkback_speech_bubble_label,
                R.string.talkback_menu_title,
                getCloseButtonOnClickListener(),
                getMenuButtonOnClickListener()));
      } else {
        speaker.setOverlay(new TextToSpeechSimpleOverlay(context));
      }
    }

    speaker.setOverlayEnabled(enabled);
  }

  public OnClickListener getCloseButtonOnClickListener() {
    return v -> {
      TalkBackService service = TalkBackService.getInstance();
      if (service != null) {
        service.showDisableTalkBackDialog();
      }
    };
  }

  public OnClickListener getMenuButtonOnClickListener() {
    return v -> {
      TalkBackService service = TalkBackService.getInstance();
      if (service != null) {
        ListMenuManager menuManager = service.getListMenuManager();
        if (menuManager != null) {
          menuManager.showMenu(CONTEXT, EVENT_ID_UNTRACKED);
        }
      }
    };
  }

  public void setUseIntonation(boolean use) {
    speaker.setUseIntonation(use);
  }

  public void setUsePunctuation(boolean use) {
    speaker.setUsePunctuation(use);
  }

  public void setPunctuationVerbosity(@PunctuationVerbosity int verbosity) {
    speaker.setPunctuationVerbosity(verbosity);
  }

  public void setFormattingOptions(int options) {
    speaker.setFormattingOptions(options);
  }

  public void setFormattingFeedbackMode(int mode) {
    speaker.setFormattingFeedbackMode(mode);
  }

  public void clearInlineFormattingHistory() {
    speaker.clearInlineFormattingHistory();
  }

  public void setSpeechPitch(float pitch) {
    speaker.setSpeechPitch(pitch);
  }

  public void setSpeechRate(float rate) {
    speaker.setSpeechRate(rate);
  }

  public void setUseAudioFocus(boolean use) {
    speaker.setUseAudioFocus(use);
  }

  public void setSpeechVolume(float volume) {
    speaker.setSpeechVolume(volume);
  }
}
