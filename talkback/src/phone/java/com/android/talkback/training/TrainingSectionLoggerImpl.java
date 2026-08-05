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

package com.google.android.accessibility.talkback.training;

import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivityInterfaceInjector.TrainingSectionLogger;
import com.google.android.accessibility.talkback.trainingcommon.TrainingConfig;
import com.google.android.accessibility.talkback.trainingcommon.TrainingMetricStore;

// TODO: Move specific handset logging logic in TrainingActivity#logEnterPages to
//  TrainingSectionLoggerImpl in overlay/handset.
/**
 * A logger for {@link TalkBackAnalytics.TrainingSectionId} events in an activity session on
 * Handset.
 */
public class TrainingSectionLoggerImpl implements TrainingSectionLogger {

  public TrainingSectionLoggerImpl(TrainingMetricStore trainingMetricStore) {}

  @Override
  public void logEnterSection(TrainingConfig config, @Nullable PageConfig pageConfig) {}
}
