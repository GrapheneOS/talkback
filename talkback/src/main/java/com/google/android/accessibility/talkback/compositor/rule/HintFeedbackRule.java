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
package com.google.android.accessibility.talkback.compositor.rule;

import static com.google.android.accessibility.talkback.compositor.Compositor.EVENT_SPEAK_HINT;
import static com.google.android.accessibility.talkback.compositor.HintEventInterpretation.HINT_TYPE_ACCESSIBILITY_FOCUS;
import static com.google.android.accessibility.talkback.compositor.HintEventInterpretation.HINT_TYPE_INPUT_FOCUS;
import static com.google.android.accessibility.talkback.compositor.HintEventInterpretation.HINT_TYPE_LINK;
import static com.google.android.accessibility.talkback.compositor.HintEventInterpretation.HINT_TYPE_SCREEN;
import static com.google.android.accessibility.talkback.compositor.HintEventInterpretation.HINT_TYPE_SELECTOR;
import static com.google.android.accessibility.talkback.compositor.HintEventInterpretation.HINT_TYPE_TEXT_SUGGESTION;
import static com.google.android.accessibility.talkback.compositor.TalkBackFeedbackProvider.EMPTY_FEEDBACK;
import static com.google.android.accessibility.talkback.selector.SelectorController.Setting.ACTIONS;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_QUEUE;

import android.content.Context;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.Compositor.HandleEventOptions;
import com.google.android.accessibility.talkback.compositor.EventFeedback;
import com.google.android.accessibility.talkback.compositor.EventInterpretation;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.compositor.HintEventInterpretation;
import com.google.android.accessibility.talkback.compositor.hint.AccessibilityFocusHint;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Event feedback rules for {@link EVENT_SPEAK_HINT} event. These rules will provide the event
 * feedback output function by inputting the {@link HandleEventOptions} and outputting {@link
 * EventFeedback}.
 */
public final class HintFeedbackRule {

  private static final String TAG = "HintFeedbackRule";

  /**
   * Adds the feedback rules to the provided event feedback rules map. So {@link
   * TalkBackFeedbackProvider} can populate the event feedback with the given event.
   *
   * @param eventFeedbackRules the event feedback rules
   * @param context the parent context
   * @param globalVariables the global compositor variables
   */
  public static void addFeedbackRule(
      Map<Integer, Function<HandleEventOptions, EventFeedback>> eventFeedbackRules,
      Context context,
      AccessibilityFocusHint accessibilityFocusHint,
      GlobalVariables globalVariables) {
    eventFeedbackRules.put(
        EVENT_SPEAK_HINT,
        (eventOptions) ->
            speakHintFeedback(
                eventOptions.eventInterpretation,
                eventOptions.sourceNode,
                context,
                accessibilityFocusHint,
                globalVariables));
  }

  private static EventFeedback speakHintFeedback(
      EventInterpretation eventInterpretation,
      AccessibilityNodeInfoCompat node,
      Context context,
      AccessibilityFocusHint accessibilityFocusHint,
      GlobalVariables globalVariables) {
    if (eventInterpretation == null || accessibilityFocusHint == null) {
      LogUtils.e(TAG, "speakHintFeedback: error: eventOptions null.");
      return EMPTY_FEEDBACK;
    }
    HintEventInterpretation hintEventInterpretation = eventInterpretation.getHint();
    if (hintEventInterpretation == null) {
      LogUtils.e(TAG, "speakHintFeedback: error: hintEventInterpretation is null.");
      return EMPTY_FEEDBACK;
    }

    CharSequence ttsOutput =
        speakHintText(
            hintEventInterpretation, node, context, accessibilityFocusHint, globalVariables);

    return EventFeedback.builder()
        .setTtsOutput(Optional.of(ttsOutput))
        .setQueueMode(QUEUE_MODE_QUEUE)
        .setRefreshSourceNode(true)
        .setForceFeedbackEvenIfAudioPlaybackActive(
            forceFeedbackEvenIfAudioPlaybackActive(eventInterpretation))
        .setForceFeedbackEvenIfMicrophoneActive(false)
        .setForceFeedbackEvenIfSsbActive(false)
        .build();
  }

  private static CharSequence speakHintText(
      HintEventInterpretation hintEventInterpretation,
      AccessibilityNodeInfoCompat node,
      Context context,
      AccessibilityFocusHint accessibilityFocusHint,
      GlobalVariables globalVariables) {
    StringBuilder logString = new StringBuilder();
    int hintType = hintEventInterpretation.getHintType();
    CharSequence hint =
        switch (hintType) {
          case HINT_TYPE_ACCESSIBILITY_FOCUS -> accessibilityFocusHint.getHint(node);
          case HINT_TYPE_INPUT_FOCUS -> getInputFocusHint();
          case HINT_TYPE_SCREEN, HINT_TYPE_SELECTOR ->
              getHintInterpretationText(hintEventInterpretation, globalVariables);
          case HINT_TYPE_TEXT_SUGGESTION -> getTextSuggestionHint(context, globalVariables);
          case HINT_TYPE_LINK ->
              accessibilityFocusHint.getClickableHint().getOpenLinkHint(context, globalVariables);
          default -> "";
        };

    int nodeId = node == null ? 0 : node.hashCode();
    LogUtils.v(
        TAG,
        "  speakHintText: %s,",
        logString
            .append(String.format("(%s) ", nodeId))
            .append(String.format(", hint={%s}", hint))
            .append(String.format(", hintType=%s", hintType))
            .toString());

    return hint;
  }

  private static CharSequence getInputFocusHint() {
    // TODO: Has removed Speak password settings.
    return "";
  }

  private static CharSequence getHintInterpretationText(
      HintEventInterpretation hintEventInterpretation, GlobalVariables globalVariables) {
    boolean enableUsageHint = globalVariables.getUsageHintEnabled();
    LogUtils.v(
        TAG,
        "    hintInterpretationText: %s",
        String.format(" enableUsageHint=%s", enableUsageHint));
    return enableUsageHint ? hintEventInterpretation.getText() : "";
  }

  private static CharSequence getTextSuggestionHint(
      Context context, GlobalVariables globalVariables) {
    StringBuilder logString = new StringBuilder();
    boolean enableUsageHint = globalVariables.getUsageHintEnabled();
    boolean hasReadingMenuActionSettings = globalVariables.hasReadingMenuActionSettings();
    int currentReadingMenu = globalVariables.getCurrentReadingMenuOrdinal();
    LogUtils.v(
        TAG,
        "    textSuggestionHint: %s",
        logString
            .append(String.format("enableUsageHint=%s", enableUsageHint))
            .append(
                String.format(", hasReadingMenuActionSettings=%s", hasReadingMenuActionSettings))
            .append(String.format(", currentReadingMenu=%s", currentReadingMenu))
            .toString());
    if (enableUsageHint && hasReadingMenuActionSettings) {
      return currentReadingMenu == ACTIONS.ordinal()
          ? context.getString(
              R.string.template_hint_reading_menu_actions_for_spelling_suggestion,
              globalVariables.getGestureStringForReadingMenuSelectedSettingNextAction())
          : context.getString(
              R.string.template_hint_reading_menu_spelling_suggestion,
              globalVariables.getGestureStringForReadingMenuNextSetting());
    }
    return context.getString(R.string.hint_suggestion);
  }

  private static boolean forceFeedbackEvenIfAudioPlaybackActive(
      EventInterpretation eventInterpretation) {
    HintEventInterpretation hintEventInterpretation =
        eventInterpretation == null ? null : eventInterpretation.getHint();
    return hintEventInterpretation != null
        && hintEventInterpretation.getForceFeedbackEvenIfAudioPlaybackActive();
  }

  private HintFeedbackRule() {}
}
