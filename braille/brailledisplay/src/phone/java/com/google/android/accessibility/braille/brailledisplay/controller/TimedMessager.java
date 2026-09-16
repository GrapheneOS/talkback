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

package com.google.android.accessibility.braille.brailledisplay.controller;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import java.util.ArrayDeque;

/**
 * Timed message controller controls the show and hide of timeliness message event on the braille
 * display.
 */
public class TimedMessager {
  /** Callback for notifying the events. */
  public interface Callback {
    /** Callbacks when a timed message is displayed. */
    void onTimedMessageDisplayed(Type type, CellsContent content);

    /** Callbacks when timed messages are cleared. */
    void onTimedMessageCleared(Type type);
  }

  /** Types of timed message. */
  public enum Type {
    CAPTION,
    ANNOUNCEMENT,
    POPUP
  }

  public static final int UNLIMITED_DURATION = -1;
  public static final String INDICATOR_START_OR_END = "⠿";
  private static final String TAG = "TimedMessager";
  private final ArrayDeque<CellsContent> captionBuffer = new ArrayDeque<>();
  private final Context context;
  private final Handler handler;
  private final Callback callback;
  private CellsContent captionToShow;

  public TimedMessager(Context context, Callback callback) {
    this.context = context;
    this.callback = callback;
    handler = new Handler(Looper.getMainLooper());
  }

  /**
   * Sets timed message with duration. The priority of message is popup > announcement > caption.
   */
  public void setTimedMessage(Type type, CellsContent content, int durationInMilliseconds) {
    switch (type) {
      case POPUP, ANNOUNCEMENT ->
          showMessage(
              type, content, () -> callback.onTimedMessageCleared(type), durationInMilliseconds);
      case CAPTION -> {
        if (captionToShow == null) {
          BrailleDisplayLog.v(TAG, "show new caption");
          captionToShow = content;
          showMessage(type, content, /* runnable= */ null, durationInMilliseconds);
        } else {
          if (content.getPanStrategy() == CellsContent.PAN_KEEP) {
            if (captionBuffer.isEmpty()) {
              BrailleDisplayLog.v(TAG, "same caption");
              showMessage(type, content, /* runnable= */ null, durationInMilliseconds);
            } else {
              BrailleDisplayLog.v(TAG, "replace the caption that is not current showing");
              captionBuffer.removeLast();
              content.setPanStrategy(CellsContent.PAN_RESET);
              captionBuffer.add(content);
            }
          } else if (content.getPanStrategy() == CellsContent.PAN_RESET) {
            BrailleDisplayLog.v(TAG, "enqueue new caption");
            captionBuffer.add(content);
          }
        }
      }
    }
  }

  /**
   * When a popup or an announcement ends, dismiss the timed message. When a caption ends, display
   * the end-of-caption indicator.
   */
  public void onReachedBeginningOrEnd(Type type) {
    BrailleDisplayLog.v(TAG, "reachBeginningOrEnd: " + type);
    switch (type) {
      case POPUP, ANNOUNCEMENT -> dismissTimedMessage(type);
      case CAPTION -> {
        // Timed messages is playing. Reset captionToShow to null to allow new captions to play.
        captionToShow = null;
        if (captionBuffer.isEmpty()) {
          BrailleDisplayLog.v(TAG, "Reach to the end.");
          setTimedMessage(
              Type.POPUP,
              new CellsContent(INDICATOR_START_OR_END),
              BrailleUserPreferences.getTimedMessageDurationInMillisecond(
                  context, INDICATOR_START_OR_END.length()));
        } else {
          setTimedMessage(Type.CAPTION, captionBuffer.remove(), UNLIMITED_DURATION);
        }
      }
    }
  }

  /** Clears all timed message with specified type. */
  public void clearAllTimedMessage(Type type) {
    BrailleDisplayLog.v(TAG, "clearAllTimedMessage: " + type);
    switch (type) {
      case CAPTION:
        captionToShow = null;
        captionBuffer.clear();
      // fall through.
      case ANNOUNCEMENT:
      case POPUP:
        dismissTimedMessage(type);
        break;
    }
  }

  /** Displays the most recent caption, while still adhering to the priority. */
  public boolean showLatestCaption() {
    if (!captionBuffer.isEmpty()) {
      CellsContent latest = captionBuffer.getLast();
      captionToShow = null;
      captionBuffer.clear();
      setTimedMessage(Type.CAPTION, latest, UNLIMITED_DURATION);
      return true;
    }
    return false;
  }

  private void dismissTimedMessage(Type type) {
    handler.removeCallbacksAndMessages(null);
    callback.onTimedMessageCleared(type);
  }

  private void showMessage(
      Type type, CellsContent content, Runnable runnable, int durationInMilliseconds) {
    callback.onTimedMessageDisplayed(type, content);
    if (durationInMilliseconds != UNLIMITED_DURATION) {
      handler.postDelayed(runnable, durationInMilliseconds);
    }
  }
}
