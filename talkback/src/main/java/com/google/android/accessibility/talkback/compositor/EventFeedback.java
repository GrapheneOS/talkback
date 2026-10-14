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

package com.google.android.accessibility.talkback.compositor;

import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.auto.value.AutoValue;
import java.util.Optional;

/**
 * Data-structure that holds compositor event feedback output results for compositor event feedback.
 */
@AutoValue
public abstract class EventFeedback {

  public abstract Optional<CharSequence> ttsOutput();

  public abstract int queueMode();

  public abstract boolean forceFeedbackEvenIfAudioPlaybackActive();

  public abstract boolean forceFeedbackEvenIfMicrophoneActive();

  public abstract boolean forceFeedbackEvenIfSsbActive();

  public abstract boolean forceFeedbackEvenIfPhoneCallActive();

  public abstract int ttsClearQueueGroup();

  public abstract boolean ttsInterruptSameGroup();

  public abstract boolean ttsSkipDuplicate();

  public abstract boolean ttsAddToHistory();

  public abstract boolean ttsForceFeedback();

  public abstract double ttsPitch();

  public abstract boolean preventDeviceSleep();

  public abstract boolean refreshSourceNode();

  public abstract boolean advanceContinuousReading();

  public abstract int haptic();

  public abstract int earcon();

  public abstract double earconRate();

  public abstract double earconVolume();

  public abstract boolean inlineFormatting();

  /**
   * Gets speech flag mask for the event. <strong>Note:</strong> This method doesn't handle {@link
   * FeedbackItem#FLAG_ADVANCE_CONTINUOUS_READING}, which should be handled after calling {@link
   * EventFeedback#advanceContinuousReading}.
   */
  public int getOutputSpeechFlags() {
    int flags = 0;
    if (!ttsAddToHistory()) {
      flags |= FeedbackItem.FLAG_NO_HISTORY;
    }
    if (ttsForceFeedback()) {
      flags |= FeedbackItem.FLAG_FORCE_FEEDBACK;
    }
    if (forceFeedbackEvenIfAudioPlaybackActive()) {
      flags |= FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE;
    }
    if (forceFeedbackEvenIfMicrophoneActive()) {
      flags |= FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE;
    }
    if (forceFeedbackEvenIfSsbActive()) {
      flags |= FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE;
    }
    if (forceFeedbackEvenIfPhoneCallActive()) {
      flags |= FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE;
    }
    if (ttsSkipDuplicate()) {
      flags |= FeedbackItem.FLAG_SKIP_DUPLICATE;
    }
    if (ttsClearQueueGroup() != SpeechController.UTTERANCE_GROUP_DEFAULT) {
      flags |= FeedbackItem.FLAG_CLEAR_QUEUED_UTTERANCES_WITH_SAME_UTTERANCE_GROUP;
    }
    if (ttsInterruptSameGroup()) {
      flags |= FeedbackItem.FLAG_INTERRUPT_CURRENT_UTTERANCE_WITH_SAME_UTTERANCE_GROUP;
    }
    if (preventDeviceSleep()) {
      flags |= FeedbackItem.FLAG_NO_DEVICE_SLEEP;
    }
    if (inlineFormatting()) {
      flags |= FeedbackItem.FLAG_INLINE_FORMATTING;
    }

    return flags;
  }

  @Override
  public final String toString() {
    return StringBuilderUtils.joinFields(
        String.format("ttsOutput= %s  ", ttsOutput().orElseGet(() -> "")),
        StringBuilderUtils.optionalInt(
            "queueMode", queueMode(), SpeechController.QUEUE_MODE_INTERRUPT),
        StringBuilderUtils.optionalTag("ttsAddToHistory", ttsAddToHistory()),
        StringBuilderUtils.optionalTag(
            "forceFeedbackEvenIfAudioPlaybackActive", forceFeedbackEvenIfAudioPlaybackActive()),
        StringBuilderUtils.optionalTag(
            "forceFeedbackEvenIfMicrophoneActive", forceFeedbackEvenIfMicrophoneActive()),
        StringBuilderUtils.optionalTag(
            "forceFeedbackEvenIfSsbActive", forceFeedbackEvenIfSsbActive()),
        StringBuilderUtils.optionalTag(
            "forceFeedbackEvenIfPhoneCallActive", forceFeedbackEvenIfPhoneCallActive()),
        StringBuilderUtils.optionalTag("ttsForceFeedback", ttsForceFeedback()),
        StringBuilderUtils.optionalTag("ttsInterruptSameGroup", ttsInterruptSameGroup()),
        StringBuilderUtils.optionalInt(
            "ttsClearQueueGroup", ttsClearQueueGroup(), SpeechController.UTTERANCE_GROUP_DEFAULT),
        StringBuilderUtils.optionalTag("ttsSkipDuplicate", ttsSkipDuplicate()),
        StringBuilderUtils.optionalDouble("ttsPitch", ttsPitch(), 1.0d),
        StringBuilderUtils.optionalTag("advanceContinuousReading", advanceContinuousReading()),
        StringBuilderUtils.optionalTag("preventDeviceSleep", preventDeviceSleep()),
        StringBuilderUtils.optionalTag("refreshSourceNode", refreshSourceNode()),
        StringBuilderUtils.optionalInt("haptic", haptic(), -1),
        StringBuilderUtils.optionalInt("earcon", earcon(), -1),
        StringBuilderUtils.optionalDouble("earconRate", earconRate(), 1.0d),
        StringBuilderUtils.optionalDouble("earconVolume", earconVolume(), 1.0d),
        StringBuilderUtils.optionalTag("inlineFormatting", inlineFormatting()));
  }

  public static EventFeedback.Builder builder() {
    return new AutoValue_EventFeedback.Builder()
        .setTtsOutput(Optional.of(""))
        .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
        .setTtsAddToHistory(false)
        .setForceFeedbackEvenIfAudioPlaybackActive(false)
        .setForceFeedbackEvenIfMicrophoneActive(false)
        .setForceFeedbackEvenIfSsbActive(false)
        .setForceFeedbackEvenIfPhoneCallActive(true)
        .setTtsForceFeedback(false)
        .setTtsInterruptSameGroup(false)
        .setTtsClearQueueGroup(SpeechController.UTTERANCE_GROUP_DEFAULT)
        .setTtsSkipDuplicate(false)
        .setTtsPitch(1.0d)
        .setAdvanceContinuousReading(false)
        .setPreventDeviceSleep(false)
        .setRefreshSourceNode(false)
        .setHaptic(-1)
        .setEarcon(-1)
        .setEarconRate(1.0d)
        .setEarconVolume(1.0d)
        .setInlineFormatting(false);
  }

  /** Builder for compositor event feedback data. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setTtsOutput(Optional<CharSequence> value);

    public abstract Builder setQueueMode(int value);

    public abstract Builder setForceFeedbackEvenIfAudioPlaybackActive(boolean value);

    public abstract Builder setForceFeedbackEvenIfMicrophoneActive(boolean value);

    public abstract Builder setForceFeedbackEvenIfSsbActive(boolean value);

    public abstract Builder setForceFeedbackEvenIfPhoneCallActive(boolean value);

    public abstract Builder setTtsClearQueueGroup(int value);

    public abstract Builder setTtsInterruptSameGroup(boolean value);

    public abstract Builder setTtsSkipDuplicate(boolean value);

    public abstract Builder setTtsAddToHistory(boolean value);

    public abstract Builder setTtsForceFeedback(boolean value);

    public abstract Builder setTtsPitch(double value);

    public abstract Builder setPreventDeviceSleep(boolean value);

    public abstract Builder setRefreshSourceNode(boolean value);

    public abstract Builder setAdvanceContinuousReading(boolean value);

    public abstract Builder setHaptic(int value);

    public abstract Builder setEarcon(int value);

    public abstract Builder setEarconRate(double value);

    public abstract Builder setEarconVolume(double value);

    public abstract Builder setInlineFormatting(boolean value);

    public abstract EventFeedback build();
  }
}
