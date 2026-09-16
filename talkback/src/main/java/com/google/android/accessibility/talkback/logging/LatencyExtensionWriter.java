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

package com.google.android.accessibility.talkback.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.PrimesController.TimerAction;

/**
 * Responsible for writing latency data to a persistent storage, either a local database or a
 * cloud-based service.
 */
public interface LatencyExtensionWriter {
  /** Starts a timer associated with {@link TimerAction}. */
  void startTimer(TimerAction timerAction, @Nullable String id);

  /**
   * Stops the timer associated with {@link TimerAction} and record it.
   *
   * <p>At all times we only have one running timer for each {@code timerAction}.
   *
   * <p>Does nothing if no matching timer was started.
   */
  void stopTimer(TimerAction timerAction, @Nullable String id, LatencyExtension latencyExtension);

  /**
   * Records the duration between {@code startMs} and {@code endMs} under the given {@link
   * TimerAction}.
   *
   * @param timerAction the associated action.
   * @param startMs starting time of the event in milliseconds.
   * @param endMs ending time of the event in milliseconds.
   * @param latencyExtension extra data fro this event.
   */
  void recordDuration(
      @NonNull TimerAction timerAction,
      long startMs,
      long endMs,
      LatencyExtension latencyExtension);
}
