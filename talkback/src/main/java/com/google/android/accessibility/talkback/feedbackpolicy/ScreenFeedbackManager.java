/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.android.accessibility.talkback.feedbackpolicy;

import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static com.google.android.accessibility.talkback.Feedback.HINT;
import static com.google.android.accessibility.utils.AccessibilityWindowInfoUtils.WINDOW_ID_NONE;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.LocaleSpan;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.Compositor;
import com.google.android.accessibility.talkback.eventprocessor.EventState;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorAccessibilityHints;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.PureFunction;
import com.google.android.accessibility.utils.ReadOnly;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.Role.RoleName;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.input.WindowEventInterpreter;
import com.google.android.accessibility.utils.input.WindowEventInterpreter.Announcement;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.FormatMethod;
import com.google.errorprone.annotations.FormatString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Generates speech for window events, functioning as both feedback-policy-mapper & speech-actor.
 *
 * <p>The overall design is to have 3 stages, similar to Compositor:
 *
 * <ol>
 *   <li>Event interpretation, which outputs a complete description of the event that can be logged
 *       to tell us all we need to know about what happened.
 *   <li>Feedback rules, which are stateless (aka static) and independent of the android operating
 *       system version. The feedback can be logged to tell us all we need to know about what
 *       talkback is trying to do in response to the event. This happens in composeFeedback().
 *   <li>Feedback methods, which provide a simple interface for speaking and acting on the
 *       user-interface.
 * </ol>
 */
public class ScreenFeedbackManager
    implements AccessibilityEventListener, WindowEventInterpreter.WindowEventHandler {

  // TODO: Move this to Mappers, using talkback.Feedback

  private static final String TAG = "ScreenFeedbackManager";

  /** Event types that are handled by ScreenFeedbackManager. */
  private static final int MASK_EVENTS_HANDLED_BY_SCREEN_FEEDBACK_MANAGER =
      AccessibilityEvent.TYPE_WINDOWS_CHANGED | TYPE_WINDOW_STATE_CHANGED;

  private final WindowEventInterpreter interpreter;
  private boolean listeningToInterpreter = false;
  protected FeedbackComposer feedbackComposer;

  // Context used by this class.
  protected final AccessibilityService service;
  private final Compositor.@NonNull TextComposer compositor;

  private final Pipeline.FeedbackReturner pipeline;

  public ScreenFeedbackManager(
      AccessibilityService service,
      @NonNull WindowEventInterpreter windowEventInterpreter,
      Compositor.@NonNull TextComposer compositor,
      FocusFinder focusFinder,
      GestureShortcutMapping gestureShortcutMapping,
      Pipeline.FeedbackReturner pipeline) {
    this.interpreter = windowEventInterpreter;
    feedbackComposer = new FeedbackComposer(service, focusFinder, gestureShortcutMapping);

    this.service = service;
    this.compositor = compositor;
    this.pipeline = pipeline;
  }

  @Override
  public int getEventTypes() {
    return MASK_EVENTS_HANDLED_BY_SCREEN_FEEDBACK_MANAGER;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    // Skip the delayed interpret if doesn't allow the announcement.
    getInterpreter().interpret(event, eventId, allowAnnounce(event));
  }

  public WindowEventInterpreter getInterpreter() {
    // Interpreter requires an initialized listener, so add listener on-demand.
    if (!listeningToInterpreter) {
      interpreter.addPriorityListener(this);
      listeningToInterpreter = true;
    }
    return interpreter;
  }

  private void checkSpeaker() {
    if (pipeline == null) {
      throw new IllegalStateException();
    }
  }

  protected void speak(
      CharSequence utterance,
      @Nullable CharSequence hint,
      EventId eventId,
      boolean forceFeedbackEvenIfAudioPlaybackActive,
      boolean forceFeedbackEvenIfMicrophoneActive,
      boolean forceFeedbackEvenIfSsbActive,
      boolean sourceIsVolumeControl) {
    if ((hint != null)) {
      pipeline.returnFeedback(
          eventId, ProcessorAccessibilityHints.screenEventToHint(hint, service, compositor));
    }

    int flags =
        (forceFeedbackEvenIfAudioPlaybackActive
                ? FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                : 0)
            | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
            | (forceFeedbackEvenIfMicrophoneActive
                ? FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                : 0)
            | (forceFeedbackEvenIfSsbActive
                ? FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE
                : 0)
            | (sourceIsVolumeControl ? FeedbackItem.FLAG_SOURCE_IS_VOLUME_CONTROL : 0);
    ;

    SpeechController.SpeakOptions speakOptions =
        SpeechController.SpeakOptions.create()
            .setQueueMode(SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
            .setFlags(flags);

    pipeline.returnFeedback(
        eventId,
        com.google.android.accessibility.talkback.Feedback.speech(utterance, speakOptions)
            .sound(com.google.android.accessibility.utils.R.raw.window_state)
            .vibration(com.google.android.accessibility.utils.R.array.window_state_pattern));
  }

  @Override
  public void handle(
      WindowEventInterpreter.EventInterpretation interpretation, @Nullable EventId eventId) {
    if (interpretation == null) {
      return;
    }

    boolean doFeedback = customHandle(interpretation, eventId);
    if (!doFeedback) {
      return;
    }

    // Generate feedback from interpreted event.
    Feedback feedback = feedbackComposer.composeFeedback(interpretation, /* logDepth= */ 0);
    LogUtils.v(TAG, "feedback=%s", feedback);

    if (!feedback.isEmpty()) {
      pipeline.returnFeedback(
          eventId,
          com.google.android.accessibility.talkback.Feedback.interrupt(HINT, /* level= */ 2));
    }

    // This will throw exception if has no any speaker.
    checkSpeaker();

    // Speak each feedback part.
    @Nullable Announcement announcement = interpretation.getAnnouncement();
    boolean sourceIsVolumeControl =
        (announcement != null) && announcement.isFromVolumeControlPanel();
    for (FeedbackPart feedbackPart : feedback.getParts()) {
      speak(
          feedbackPart.getSpeech(),
          feedbackPart.getHint(),
          eventId,
          feedbackPart.getForceFeedbackEvenIfAudioPlaybackActive(),
          feedbackPart.getForceFeedbackEvenIfMicrophoneActive(),
          feedbackPart.getForceFeedbackEvenIfSsbActive(),
          sourceIsVolumeControl);
    }
  }

  private boolean allowAnnounce(AccessibilityEvent event) {
    // If the user performs a cursor control(copy, paste, start selection mode, etc) in the
    // talkback context menu and lands back to the edit text, a TYPE_WINDOWS_CHANGED and a
    // TYPE_WINDOW_STATE_CHANGED events will be fired. We should skip these two events to
    // avoid announcing the window title.
    boolean allowAnnounce = true;
    if (event.getEventType() == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        && EventState.getInstance()
            .checkAndClearRecentFlag(
                EventState.EVENT_SKIP_WINDOWS_CHANGED_PROCESSING_AFTER_CURSOR_CONTROL)) {
      allowAnnounce = false;
    }
    if (event.getEventType() == TYPE_WINDOW_STATE_CHANGED
        && EventState.getInstance()
            .checkAndClearRecentFlag(
                EventState.EVENT_SKIP_WINDOW_STATE_CHANGED_PROCESSING_AFTER_CURSOR_CONTROL)) {
      allowAnnounce = false;
    }

    return allowAnnounce;
  }

  /** Handle of interpreted event, and return whether to compose speech. */
  protected boolean customHandle(
      WindowEventInterpreter.EventInterpretation interpretation, @Nullable EventId eventId) {
    if (interpretation == null) {
      return false;
    }

    // Dialog opened events should be read regardless of whether the windows are stable.
    if (FeatureFlagReader.enableSpeakDialogContent(service)
        && interpretation.isWebDialogOpenedEvent()) {
      return interpretation.isAllowAnnounce();
    }

    // For original event, perform some state & UI actions, even if windows are unstable.
    if (interpretation.isOriginalEvent()) {
      if (!interpretation.isAllowAnnounce()) {
        interpretation.setMainWindowsChanged(false);
      }
    }

    // Only speak if windows are stable and the event allows announcement.
    return interpretation.areWindowsStable() && interpretation.isAllowAnnounce();
  }

  private static Resources getLocalizedResources(Context context, Locale desiredLocale) {
    Configuration conf = context.getResources().getConfiguration();
    conf = new Configuration(conf);
    conf.setLocale(desiredLocale);
    Context localizedContext = context.createConfigurationContext(conf);
    return localizedContext.getResources();
  }

  /** Inner class used for speech feedback generation. */
  @PureFunction
  @VisibleForTesting
  static class FeedbackComposer {

    private final AccessibilityService service;
    private final @Nullable FocusFinder focusFinder;
    private final @Nullable GestureShortcutMapping gestureShortcutMapping;

    public FeedbackComposer(
        AccessibilityService service,
        @Nullable FocusFinder focusFinder,
        @Nullable GestureShortcutMapping gestureShortcutMapping) {
      this.service = service;
      this.focusFinder = focusFinder;
      this.gestureShortcutMapping = gestureShortcutMapping;
    }

    /** Compose speech feedback for fully interpreted window event, statelessly. */
    public Feedback composeFeedback(
        WindowEventInterpreter.EventInterpretation interpretation, final int logDepth) {

      logCompose(logDepth, "composeFeedback", "interpretation=%s", interpretation);

      Feedback feedback = new Feedback();

      if (isWakeUpForWear(interpretation)) {
        // Only announce unread notification feedback on wake up experience for wear and have early
        // return to avoid unwanted window title announcement.
        FeedbackPart part = getFeedbackPartUnreadNotificationOnWakeUpIfNecessary();
        if (part != null) {
          feedback.addPart(part);
        }
        return feedback;
      }

      // Compose feedback for announcement.
      Announcement announcement = interpretation.getAnnouncement();
      if (announcement != null) {
        logCompose(logDepth, "composeFeedback", "announcement");
        feedback.addPart(
            new FeedbackPart(announcement.text())
                .forceFeedbackEvenIfAudioPlaybackActive(!announcement.isFromVolumeControlPanel())
                .forceFeedbackEvenIfMicrophoneActive(!announcement.isFromVolumeControlPanel())
                .forceFeedbackEvenIfSsbActive(announcement.isFromInputMethodEditor()));
      }

      // Compose feedback for IME window
      if (interpretation.getShouldAnnounceInputMethodChange()) {
        logCompose(logDepth, "composeFeedback", "input method");
        CharSequence inputMethodFeedback;
        if (interpretation.getInputMethod().getId() == WINDOW_ID_NONE) {
          inputMethodFeedback = service.getString(R.string.hide_keyboard_window);
        } else {
          // Keyboard title may contain LocaleSpan. Try to get the whole string in the same language
          // and set the LocaleSpan back to the whole string, so TTS can speak the whole string
          // without chunking by the specified language.
          CharSequence title = interpretation.getInputMethod().getTitleForFeedback();
          SpannableString titleSpannable = new SpannableString(title);
          LocaleSpan[] localeSpans =
              titleSpannable.getSpans(0, titleSpannable.length(), LocaleSpan.class);
          if (localeSpans.length > 0) {
            Resources resources = getLocalizedResources(service, localeSpans[0].getLocale());
            inputMethodFeedback = resources.getString(R.string.show_keyboard_window, title);
            SpannableString feedbackSpannable = new SpannableString(inputMethodFeedback);
            feedbackSpannable.setSpan(localeSpans[0], 0, feedbackSpannable.length(), 0);
            inputMethodFeedback = feedbackSpannable;
          } else {
            inputMethodFeedback = service.getString(R.string.show_keyboard_window, title);
          }
        }
        feedback.addPart(
            new FeedbackPart(inputMethodFeedback)
                .forceFeedbackEvenIfAudioPlaybackActive(true)
                .forceFeedbackEvenIfMicrophoneActive(true));
      }

      // Generate spoken feedback for main window changes.
      CharSequence utterance = "";
      CharSequence hint = null;

      if (FeatureFlagReader.enableSpeakDialogContent(service)
          && interpretation.isWebDialogOpenedEvent()) {
        logCompose(logDepth, "composeFeedback", "dialog opened");
        // Generate spoken feedback for dialog window appearance. Note that this logic is in a
        // separate if-statement block to ensure that the dialog title is not announced again if the
        // dialog content is already being announced.
        utterance = getPaneDialogUtterance(interpretation);
      } else if (interpretation.getMainWindowsChanged()) {
        if (interpretation.getOtherActiveWindow().getId() != WINDOW_ID_NONE) {
          logCompose(logDepth, "composeFeedback", "other active window");
          utterance = interpretation.getOtherActiveWindow().getTitleForFeedback();
        } else if (interpretation.getWindowA().getId() != WINDOW_ID_NONE) {
          if (interpretation.getWindowB().getId() == WINDOW_ID_NONE) {
            // Single window mode.
            logCompose(logDepth, "composeFeedback", "single window mode");
            utterance = interpretation.getWindowA().getTitleForFeedback();
          } else {
            // Either Split screen mode or multi-panel.
            logCompose(logDepth, "composeFeedback", "split-screen/multi-panel mode");
            int feedbackTemplate;
            if (interpretation.getHorizontalPlacement()) {
              if (WindowUtils.isScreenLayoutRTL(service)) {
                feedbackTemplate = R.string.template_split_screen_mode_landscape_rtl;
              } else {
                feedbackTemplate = R.string.template_split_screen_mode_landscape_ltr;
              }
            } else {
              feedbackTemplate = R.string.template_split_screen_mode_portrait;
            }

            utterance =
                service.getString(
                    feedbackTemplate,
                    interpretation.getWindowA().getTitleForFeedback(),
                    interpretation.getWindowB().getTitleForFeedback());
          }
        }
      }

      // Append picture-in-picture window description.
      if ((interpretation.getMainWindowsChanged() || interpretation.getPicInPicChanged())
          && interpretation.getPicInPic().getId() != WINDOW_ID_NONE
          && interpretation.getOtherActiveWindow().getId() == WINDOW_ID_NONE) {
        logCompose(logDepth, "composeFeedback", "picture-in-picture");
        CharSequence picInPicWindowTitle = interpretation.getPicInPic().getTitleForFeedback();
        if (picInPicWindowTitle == null) {
          picInPicWindowTitle = ""; // Notify that pic-in-pic exists, even if title unavailable.
        }
        utterance =
            appendTemplate(
                service,
                utterance,
                R.string.template_overlay_window,
                picInPicWindowTitle,
                logDepth + 1);
      }

      // In case no announcement is generated, checks the source window of the received event.
      if (TextUtils.isEmpty(utterance)) {
        logCompose(logDepth, "composeFeedback", "Event source window");
        int fromWindowId = interpretation.getEventSourceWindow().getId();

        // For TYPE_WINDOW_STATE_CHANGED events, only announce the accessibility pane title.
        if (interpretation.getEventType() == TYPE_WINDOW_STATE_CHANGED) {
          if (interpretation.getContentChangeTypes() == CONTENT_CHANGE_TYPE_PANE_APPEARED) {
            logCompose(logDepth, "composeFeedback", "Pane appeared");
            utterance = interpretation.getEventSourceWindow().getTitleForFeedback();
          }
        } else {
          if (interpretation.getWindowChangeTypes() != AccessibilityEvent.WINDOWS_CHANGE_REMOVED
              && fromWindowId != WINDOW_ID_NONE
              && interpretation.getEventSourceWindow().idOrTitleChanged()) {
            logCompose(logDepth, "composeFeedback", "Window changed");
            // TODO: For window changed events, announce the title from the new window.
            // utterance = interpretation.getEventSourceWindow().getTitleForFeedback();
          }
        }
      }

      // Custom the feedback if the composer needs.
      feedback = customizeFeedback(service, feedback, interpretation, logDepth);

      // Return feedback.
      if (!TextUtils.isEmpty(utterance)) {
        feedback.addPart(
            new FeedbackPart(utterance)
                .hint(hint)
                .forceFeedbackEvenIfAudioPlaybackActive(true)
                .forceFeedbackEvenIfMicrophoneActive(true));
      }
      feedback.setReadOnly();
      return feedback;
    }

    private boolean isWakeUpForWear(WindowEventInterpreter.EventInterpretation interpretation) {
      return FormFactorUtils.isAndroidWear() && interpretation.isInterpretFirstTimeWhenWakeUp();
    }

    private @Nullable FeedbackPart getFeedbackPartUnreadNotificationOnWakeUpIfNecessary() {
      for (AccessibilityWindowInfo window : AccessibilityServiceCompatUtils.getWindows(service)) {
        AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.toCompat(window.getRoot());
        AccessibilityNodeInfoCompat unreadNode =
            AccessibilityNodeInfoUtils.getSelfOrMatchingDescendant(
                root,
                new Filter<AccessibilityNodeInfoCompat>() {
                  @Override
                  public boolean accept(AccessibilityNodeInfoCompat node) {
                    return AccessibilityNodeInfoUtils.isWearUnreadNotificationDot(node);
                  }
                });

        if (unreadNode != null) {
          return new FeedbackPart(unreadNode.getContentDescription())
              .forceFeedbackEvenIfAudioPlaybackActive(true)
              .forceFeedbackEvenIfMicrophoneActive(true);
        }
      }
      return null;
    }

    private CharSequence appendTemplate(
        Context context,
        @Nullable CharSequence text,
        int templateResId,
        CharSequence templateArg,
        final int logDepth) {
      logCompose(logDepth, "appendTemplate", "templateArg=%s", templateArg);
      CharSequence templatedText = context.getString(templateResId, templateArg);
      SpannableStringBuilder builder = new SpannableStringBuilder((text == null) ? "" : text);
      StringBuilderUtils.appendWithSeparator(builder, templatedText);
      return builder;
    }

    private Feedback customizeFeedback(
        Context context,
        Feedback feedback,
        WindowEventInterpreter.EventInterpretation interpretation,
        final int logDepth) {
      // Compose feedback for the popup window, such as auto-complete suggestions window.
      // To navigate to access the suggestions window when the suggestions window popups,
      // the user can
      // 1. perform a previous-window gesture when a11y focus is on IME window.
      // 2. perform a next-item gesture when a11y focus is on the auto-complete textView.
      if (focusFinder == null || gestureShortcutMapping == null) {
        return feedback;
      }
      if (interpretation.getAnchorNodeRole() == Role.ROLE_EDIT_TEXT) {
        logCompose(logDepth, "customComposeFeedback", "auto-complete suggestions");
        AccessibilityNodeInfoCompat focus =
            focusFinder.findFocusCompat(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
        if (focus == null) {
          return feedback;
        }
        final String gesture;
        if ((Role.getRole(focus) == Role.ROLE_EDIT_TEXT
            && AccessibilityWindowInfoUtils.getAnchoredWindow(focus) != null)) {
          gesture =
              gestureShortcutMapping.getGestureFromActionKey(
                  context.getString(R.string.shortcut_value_next));
        } else if (focus.getWindow().getType() == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
          gesture =
              gestureShortcutMapping.getGestureFromActionKey(
                  context.getString(R.string.shortcut_value_previous_window));
        } else {
          return feedback;
        }
        String utterance =
            (FeatureSupport.isMultiFingerGestureSupported() && gesture != null)
                ? context.getString(R.string.suggestions_window_available_with_gesture, gesture)
                : context.getString(R.string.suggestions_window_available);
        feedback.addPart(
            new FeedbackPart(utterance)
                .forceFeedbackEvenIfAudioPlaybackActive(true)
                .forceFeedbackEvenIfMicrophoneActive(true));
      }
      return feedback;
    }

    /**
     * Returns the utterance for pane dialog window appearance, reading some or all of the dialog's
     * content.
     *
     * @param interpretation The event interpretation.
     * @return The utterance for dialog window appearance. If the dialog is an alert dialog, the
     *     utterance will include all nodes with text in the dialog. Otherwise, the utterance will
     *     include nodes with text up until the focused node. Returns an empty string if the dialog
     *     does not have any nodes with text, or if the dialog is not an alert dialog and there is
     *     no focused node.
     */
    private String getPaneDialogUtterance(
        WindowEventInterpreter.EventInterpretation interpretation) {
      if (!FeatureFlagReader.enableSpeakDialogContent(service)
          || !interpretation.isWebDialogOpenedEvent()) {
        return "";
      }

      @Nullable AccessibilityNodeInfo eventSourceNode = interpretation.getSourceNode();
      if (eventSourceNode == null) {
        return "";
      }

      Filter<AccessibilityNodeInfoCompat> hasTextFilter =
          new Filter<AccessibilityNodeInfoCompat>() {
            @Override
            public boolean accept(AccessibilityNodeInfoCompat info) {
              // Skip the dialog node itself to avoid duplicate announcements.
              @RoleName int role = Role.getRole(info);
              if (role == Role.ROLE_DIALOG || role == Role.ROLE_ALERT_DIALOG) {
                return false;
              }
              // Return only nodes with text.
              return !TextUtils.isEmpty(info.getText());
            }
          };

      AccessibilityNodeInfoCompat dialogNode = AccessibilityNodeInfoUtils.toCompat(eventSourceNode);
      @RoleName int dialogRole = Role.getRole(dialogNode);
      List<AccessibilityNodeInfoCompat> childrenWithText = new ArrayList<>();
      switch (dialogRole) {
        case Role.ROLE_DIALOG -> {
          // For non-alert dialogs, announce content up until the focused node.
          if (focusFinder == null) {
            return "";
          }
          @Nullable AccessibilityNodeInfoCompat focusedNode =
              focusFinder.findFocusCompat(AccessibilityNodeInfo.FOCUS_ACCESSIBILITY);
          childrenWithText =
              AccessibilityNodeInfoUtils.getMatchingDescendantsOrRootUntilNode(
                  dialogNode, hasTextFilter, /* stopNode= */ focusedNode);
        }
        case Role.ROLE_ALERT_DIALOG ->
            // For alert dialogs, announce all content in the dialog.
            childrenWithText =
                AccessibilityNodeInfoUtils.getMatchingDescendantsOrRoot(dialogNode, hasTextFilter);
        default ->
            // Checking for isDialogOpenedEvent() should prevent this from happening.
            throw new IllegalStateException("Unexpected dialog role: " + dialogRole);
      }

      if (childrenWithText == null) {
        return "";
      }

      SpannableStringBuilder dialogContent = new SpannableStringBuilder();
      for (AccessibilityNodeInfoCompat child : childrenWithText) {
        StringBuilderUtils.appendWithSeparator(dialogContent, child.getText());
      }

      if (dialogContent.length() > 0) {
        return dialogContent.toString();
      }

      return "";
    }
  }

  // /////////////////////////////////////////////////////////////////////////////////////
  // Inner class: speech output

  /** Data container specifying speech, feedback timing, etc. */
  protected static class Feedback extends ReadOnly {
    private final List<FeedbackPart> parts = new ArrayList<>();

    public void addPart(FeedbackPart part) {
      checkIsWritable();
      parts.add(part);
    }

    public List<FeedbackPart> getParts() {
      return isWritable() ? parts : Collections.unmodifiableList(parts);
    }

    public boolean isEmpty() {
      return parts.isEmpty();
    }

    @Override
    public String toString() {
      StringBuilder strings = new StringBuilder();
      for (FeedbackPart part : parts) {
        strings.append("[").append(part).append("] ");
      }
      return strings.toString();
    }
  }

  /** Data container used by Feedback, with a builder-style interface. */
  protected static class FeedbackPart {
    private final CharSequence speech;
    private @Nullable CharSequence hint;

    // Follows REFERTO.
    private boolean forceFeedbackEvenIfAudioPlaybackActive = false;
    private boolean forceFeedbackEvenIfMicrophoneActive = false;
    private boolean forceFeedbackEvenIfSsbActive = false;

    public FeedbackPart(CharSequence speech) {
      this.speech = speech;
    }

    @CanIgnoreReturnValue
    public FeedbackPart hint(@Nullable CharSequence hint) {
      this.hint = hint;
      return this;
    }

    @CanIgnoreReturnValue
    public FeedbackPart forceFeedbackEvenIfAudioPlaybackActive(boolean force) {
      forceFeedbackEvenIfAudioPlaybackActive = force;
      return this;
    }

    @CanIgnoreReturnValue
    public FeedbackPart forceFeedbackEvenIfMicrophoneActive(boolean force) {
      forceFeedbackEvenIfMicrophoneActive = force;
      return this;
    }

    @CanIgnoreReturnValue
    public FeedbackPart forceFeedbackEvenIfSsbActive(boolean force) {
      forceFeedbackEvenIfSsbActive = force;
      return this;
    }

    public CharSequence getSpeech() {
      return speech;
    }

    public @Nullable CharSequence getHint() {
      return hint;
    }

    public boolean getForceFeedbackEvenIfAudioPlaybackActive() {
      return forceFeedbackEvenIfAudioPlaybackActive;
    }

    public boolean getForceFeedbackEvenIfMicrophoneActive() {
      return forceFeedbackEvenIfMicrophoneActive;
    }

    public boolean getForceFeedbackEvenIfSsbActive() {
      return forceFeedbackEvenIfSsbActive;
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          formatString(speech).toString(),
          (hint == null ? "" : " hint:" + formatString(hint)),
          StringBuilderUtils.optionalTag(
              "forceFeedbackEvenIfAudioPlaybackActive", forceFeedbackEvenIfAudioPlaybackActive),
          StringBuilderUtils.optionalTag(
              " forceFeedbackEvenIfMicrophoneActive", forceFeedbackEvenIfMicrophoneActive),
          StringBuilderUtils.optionalTag(
              " forceFeedbackEvenIfSsbActive", forceFeedbackEvenIfSsbActive));
    }
  }

  // /////////////////////////////////////////////////////////////////////////////////////
  // Logging functions

  private static CharSequence formatString(CharSequence text) {
    return (text == null) ? "null" : String.format("\"%s\"", text);
  }

  @FormatMethod
  protected static void logCompose(
      final int depth, String methodName, @FormatString String format, Object... args) {

    // Compute indentation.
    char[] indentChars = new char[depth * 2];
    Arrays.fill(indentChars, ' ');
    String indent = new String(indentChars);

    // Log message.
    LogUtils.v(TAG, "%s%s() %s", indent, methodName, String.format(format, args));
  }
}
