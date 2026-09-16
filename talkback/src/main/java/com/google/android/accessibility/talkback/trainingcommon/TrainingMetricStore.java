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

package com.google.android.accessibility.talkback.trainingcommon;

import android.content.Context;
import androidx.annotation.NonNull;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.TrainingSectionId;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;

/** A delegate class to transit the statistics to a persistent storage. */
public class TrainingMetricStore {
  /** Types of the training. */
  public enum Type {
    TUTORIAL,
    ONBOARDING,
  }

  /** Class stores the aggregated training data. */
  @AutoValue
  public abstract static class TrainingMetric {
    public abstract TrainingMetricStore.Type type();

    public abstract boolean trainingStarted();

    public abstract Duration trainingCompletedDuration();

    public abstract ImmutableMap<PageId, Duration> stayingPageDuration();

    public abstract ImmutableSet<PageId> completedPages();

    public static Builder builder() {
      return new AutoValue_TrainingMetricStore_TrainingMetric.Builder()
          .setStayingPageDuration(ImmutableMap.of())
          .setTrainingCompletedDuration(Duration.ZERO)
          .setCompletedPages(ImmutableSet.of());
    }

    @NonNull
    @Override
    public final String toString() {
      return "TrainingMetric: "
          + StringBuilderUtils.joinFields(
              StringBuilderUtils.optionalField("type", type()),
              StringBuilderUtils.optionalTag("trainingStarted", trainingStarted()),
              StringBuilderUtils.optionalField(
                  "trainingCompletedDuration", trainingCompletedDuration()),
              StringBuilderUtils.optionalField("stayingPageDuration", stayingPageDuration()),
              StringBuilderUtils.optionalField("completedPages", completedPages()));
    }

    /** Builder of {@link TrainingMetric}. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setType(TrainingMetricStore.Type type);

      public abstract Builder setTrainingStarted(boolean trainingStarted);

      public abstract Builder setTrainingCompletedDuration(Duration completedDuration);

      public abstract Builder setStayingPageDuration(
          ImmutableMap<PageId, Duration> stayingPageDuration);

      public abstract Builder setCompletedPages(ImmutableSet<PageId> completedPages);

      public abstract TrainingMetric build();
    }
  }

  public TrainingMetricStore(Context context, Type trainingType) {}

  public void onTutorialEntered(@TrainingSectionId int event) {}

  public void onTutorialEvent(@TrainingSectionId int event) {}

  public void onTrainingPause(PageId pageId) {}

  public void onTrainingResume(PageId pageId) {}

  public void onTrainingPageEntered(PageId pageId) {}

  public void onTrainingPageLeft(PageId pageId) {}

  public void onTrainingPageCompleted(PageId pageId) {}
}
