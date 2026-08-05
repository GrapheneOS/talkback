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
package com.google.android.accessibility.utils.input;

import android.os.Looper;
import android.os.Message;
import android.util.Pair;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.Nullable;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.checkerframework.checker.initialization.qual.UnderInitialization;

/**
 * Event emitter which cache specific AccessibilityEvent type under specific conditions for giving
 * duration then emit once reached the time. A new coming event will make the cached event with same
 * type dropped even if the new event does not match the condition.
 */
public class EventDelayEmitter {
  private static final String TAG = "EventDelayEmitter";

  /** The event mask to filter the events which should be delayed. */
  private final int eventMask;

  private final Duration delayDuration;

  /** The cache conditions to decide whether the event should cache or emit immediately. */
  private final Predicate<AccessibilityEvent> cacheCondition;

  private final BiConsumer<AccessibilityEvent, Optional<EventId>> consumer;
  private final Map<Integer, Pair<AccessibilityEvent, Optional<EventId>>> eventMap =
      new ConcurrentHashMap<>();

  @SuppressWarnings({"nullness:assignment"})
  private final EventDelayer eventDelayer = new EventDelayer(this);

  public EventDelayEmitter(
      int eventMask,
      Duration delayDuration,
      Predicate<AccessibilityEvent> cacheCondition,
      BiConsumer<AccessibilityEvent, Optional<EventId>> consumer) {
    this.eventMask = eventMask;
    this.delayDuration = delayDuration;
    this.cacheCondition = cacheCondition;
    this.consumer = consumer;
  }

  /** Handles the event deciding it should cache of emit immediately. */
  public void handle(AccessibilityEvent accessibilityEvent, @Nullable EventId eventId) {
    int eventType = accessibilityEvent.getEventType();
    Optional<EventId> eventIdOptional = eventId == null ? Optional.empty() : Optional.of(eventId);
    if ((eventType & eventMask) == 0) {
      consumer.accept(accessibilityEvent, eventIdOptional);
      return;
    }
    if (cacheCondition.test(accessibilityEvent)) {
      LogUtils.v(
          TAG,
          "eventType: "
              + accessibilityEvent.getEventType()
              + " created at: "
              + accessibilityEvent.getEventTime()
              + " cached.");
      Pair<AccessibilityEvent, Optional<EventId>> previousValue =
          eventMap.put(eventType, new Pair<>(accessibilityEvent, eventIdOptional));
      if (previousValue != null) {
        LogUtils.v(
            TAG,
            "eventType: "
                + previousValue.first.getEventType()
                + " created at: "
                + previousValue.first.getEventTime()
                + " dropped.");
      } else {
        eventDelayer.sendMessageDelayed(
            eventDelayer.obtainMessage(EventDelayer.MSG_DELAY_EVENT, eventType),
            delayDuration.toMillis());
      }
    } else {
      eventMap.remove(eventType);
      consumer.accept(accessibilityEvent, eventIdOptional);
    }
  }

  private static class EventDelayer extends WeakReferenceHandler<EventDelayEmitter> {
    private static final int MSG_DELAY_EVENT = 1;

    @SuppressWarnings({"nullness:argument"})
    public EventDelayer(@UnderInitialization EventDelayEmitter parent) {
      super(parent, Looper.myLooper());
    }

    @Override
    protected void handleMessage(Message msg, EventDelayEmitter parent) {
      if (msg.what == MSG_DELAY_EVENT) {
        Pair<AccessibilityEvent, Optional<EventId>> args = parent.eventMap.remove(msg.obj);
        if (args == null) {
          return;
        }
        parent.consumer.accept(args.first, args.second);
      }
    }
  }
}
