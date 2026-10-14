/*
 * Copyright (C) 2025 Google Inc.
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

package com.google.android.accessibility.talkback.utils;

import android.content.Context;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.gemini.GeminiFunctionUtils;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;

/**
 * Utility class for Gemini functions called by {@link GestureController} and {@link
 * KeyComboMapper}.
 */
public final class GeminiCommandUtils {
  private static final String TAG = "GeminiCommandUtils";

  private static final SpeakOptions SPEAK_OPTIONS =
      SpeakOptions.create()
          .setFlags(
              FeedbackItem.FLAG_NO_HISTORY
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE);

  public static Feedback.Part.Builder feedbackForDescribeImage(
      Context context, AccessibilityNodeInfoCompat node, ActorState actorState) {
    if (node == null) {
      return Feedback.speech(context.getString(R.string.image_caption_no_result), SPEAK_OPTIONS);
    }

    return GeminiFunctionUtils.getPreferredImageDescriptionFeedback(context, actorState, node);
  }

  public static Feedback.Part.Builder feedbackForScreenOverview(
      Context context, AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return Feedback.speech(context.getString(R.string.screen_overview_no_result), SPEAK_OPTIONS);
    }

    return GeminiFunctionUtils.getPreferredScreenOverviewFeedback(context, node);
  }

  private GeminiCommandUtils() {}
}
