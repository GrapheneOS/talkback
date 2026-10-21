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

import static com.google.android.accessibility.talkback.compositor.Compositor.EVENT_TYPE_VIEW_HOVER_ENTER;
import static com.google.android.accessibility.talkback.compositor.CompositorConfigs.shouldEarlyAnnounceForLiftToType;
import static com.google.android.accessibility.talkback.compositor.CompositorUtils.PRUNE_EMPTY;

import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.AccessibilityEventFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.Compositor.HandleEventOptions;
import com.google.android.accessibility.talkback.compositor.CompositorUtils;
import com.google.android.accessibility.talkback.compositor.EventFeedback;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.compositor.TalkBackFeedbackProvider;
import com.google.android.accessibility.talkback.compositor.roledescription.TreeNodesDescription;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorPhoneticLetters;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Event feedback rules for {@link EVENT_TYPE_VIEW_HOVER_ENTER} event.
 *
 * <p>These rules will provide the event feedback output function by inputting the {@link
 * HandleEventOptions} and outputting {@link EventFeedback}.
 */
public class EventTypeHoverEnterFeedbackRule {

  private static final String TAG = "EventTypeHoverEnterFeedbackRule";

  /**
   * Adds the feedback rules to the provided event feedback rules map. So {@link
   * TalkBackFeedbackProvider} can provide the event feedback by the rules. If the event is related
   * to keyboard key, the TTS output will replaced with key's description and its phonetic.
   *
   * @param eventFeedbackRules the event feedback rules
   * @param globalVariables the global compositor variables
   */
  public static void addFeedbackRule(
      Context context,
      Map<Integer, Function<HandleEventOptions, EventFeedback>> eventFeedbackRules,
      ProcessorPhoneticLetters processorPhoneticLetters,
      TreeNodesDescription treeNodesDescription,
      GlobalVariables globalVariables) {
    eventFeedbackRules.put(
        EVENT_TYPE_VIEW_HOVER_ENTER,
        (eventOptions) -> {
          CharSequence ttsOutput =
              computeTtsOutput(
                  context,
                  eventOptions,
                  processorPhoneticLetters,
                  treeNodesDescription,
                  globalVariables);
          boolean sourceNodeIsNull = (eventOptions.sourceNode == null);
          int earcon = sourceNodeIsNull ? R.raw.focus : -1;
          int haptic = sourceNodeIsNull ? R.array.view_hovered_pattern : -1;

          LogUtils.v(
              TAG,
              StringBuilderUtils.joinFields(
                  " ttsOutputRule= eventContentDescriptionOrEventAggregateText, ",
                  StringBuilderUtils.optionalTag("sourceNodeIsNull", sourceNodeIsNull),
                  StringBuilderUtils.optionalText("ttsOutput", ttsOutput)));

          return EventFeedback.builder()
              .setTtsOutput(Optional.of(ttsOutput))
              .setTtsAddToHistory(true)
              .setForceFeedbackEvenIfAudioPlaybackActive(true)
              .setForceFeedbackEvenIfMicrophoneActive(true)
              .setForceFeedbackEvenIfSsbActive(true)
              .setEarcon(earcon)
              .setHaptic(haptic)
              .build();
        });
  }

  private static CharSequence computeTtsOutput(
      Context context,
      HandleEventOptions eventOptions,
      ProcessorPhoneticLetters processorPhoneticLetters,
      TreeNodesDescription treeNodesDescription,
      GlobalVariables globalVariables) {
    AccessibilityEvent event = eventOptions.eventObject;
    if (shouldEarlyAnnounceForLiftToType(context)
        && AccessibilityEventFeedbackUtils.isEventRelatedToKeyboardKey(event)) {
      // If the event is related to keyboard key, the TTS output will replaced with key's
      // description and its phonetic.
      List<CharSequence> outputJoinList = new ArrayList<>();
      AccessibilityNodeInfoCompat node = eventOptions.sourceNode;
      CharSequence nodeTreeDescription =
          treeNodesDescription.aggregateNodeTreeDescription(node, event);
      if (!TextUtils.isEmpty(nodeTreeDescription)) {
        outputJoinList.add(nodeTreeDescription);
      } else {
        Locale preferredLocale = globalVariables.getPreferredLocaleByNode(node);
        CharSequence eventDescription =
            AccessibilityEventFeedbackUtils.getEventContentDescriptionOrEventAggregateText(
                event, preferredLocale);
        outputJoinList.add(eventDescription);
      }
      Optional<CharSequence> phoneticExample =
          processorPhoneticLetters.getPhoneticLetterForKeyboardFocusEvent(event);
      phoneticExample.ifPresent(outputJoinList::add);
      if (!outputJoinList.isEmpty()) {
        LogUtils.v(TAG, "TtsOutput replaced with key's description and its phonetic.");
        return CompositorUtils.joinCharSequences(
            outputJoinList, CompositorUtils.getSeparator(), PRUNE_EMPTY);
      }
    }
    return AccessibilityEventFeedbackUtils.getEventContentDescriptionOrEventAggregateText(
        event, globalVariables.getUserPreferredLocale());
  }
}
