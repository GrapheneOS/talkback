/*
 * Copyright (C) 2024 Google Inc.
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

import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_ADDED;
import static com.google.android.accessibility.talkback.compositor.Compositor.EVENT_ACTIONABLE_NONMODAL_ALERT_APPEARED;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.Compositor.HandleEventOptions;
import com.google.android.accessibility.talkback.compositor.EventFeedback;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Event feedback rules for {@link EVENT_ACTIONABLE_NONMODAL_ALERT_APPEARED} event. These rules will
 * provide the event feedback output function by inputting the {@link HandleEventOptions} and
 * outputting {@link EventFeedback}.
 */
public final class ActionableNonmodalAlertAppearedFeedbackRule {
  private static final String TAG = "ActionableNonmodalAlertAppearedFeedbackRule";

  private ActionableNonmodalAlertAppearedFeedbackRule() {}

  /**
   * Adds the feedback rules to the provided event feedback rules map. So {@link
   * TalkBackFeedbackProvider} can provide the event feedback by the rules.
   *
   * @param eventFeedbackRules the event feedback rules
   * @param context the parent context
   * @param globalVariables the global compositor variables
   */
  @SuppressLint("NewApi")
  public static void addFeedbackRule(
      Map<Integer, Function<HandleEventOptions, EventFeedback>> eventFeedbackRules,
      Context context,
      GlobalVariables globalVariables) {
    eventFeedbackRules.put(
        EVENT_ACTIONABLE_NONMODAL_ALERT_APPEARED,
        eventOptions -> {
          if (!globalVariables.getUsageHintEnabled()) {
            return EventFeedback.builder().build();
          }
          CharSequence gesture = globalVariables.getGestureForNextWindowShortcut();
          String gestureOutput =
              (gesture != null)
                  ? context.getString(R.string.nonmodal_alert_available_with_gesture, gesture)
                  : "";
          if (TextUtils.isEmpty(gestureOutput)) {
            return EventFeedback.builder().build();
          }
          String ttsOutput = "";
          if (eventOptions.eventObject.getEventType()
                  == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
              && (eventOptions.eventObject.getContentChangeTypes()
                      & CONTENT_CHANGE_TYPE_PANE_APPEARED)
                  != 0) {
            ttsOutput = gestureOutput;
          } else if (eventOptions.eventObject.getEventType()
                  == AccessibilityEvent.TYPE_WINDOWS_CHANGED
              && (eventOptions.eventObject.getWindowChanges() & WINDOWS_CHANGE_ADDED) != 0) {
            ttsOutput = gestureOutput;
          }
          LogUtils.v(
              TAG,
              "actionableNonModalAlertFeedback %s",
              new StringBuilder().append(String.format(", ttsOutput= {%s}", ttsOutput)).toString());
          return EventFeedback.builder()
              .setTtsOutput(Optional.of(ttsOutput))
              .setQueueMode(QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
              .setTtsAddToHistory(true)
              .setForceFeedbackEvenIfAudioPlaybackActive(true)
              .setForceFeedbackEvenIfMicrophoneActive(true)
              .setForceFeedbackEvenIfSsbActive(false)
              .setForceFeedbackEvenIfPhoneCallActive(true)
              .build();
        });
  }
}
