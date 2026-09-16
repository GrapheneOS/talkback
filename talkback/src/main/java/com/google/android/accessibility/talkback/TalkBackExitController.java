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
package com.google.android.accessibility.talkback;

import static com.google.android.accessibility.talkback.TalkBackExitController.TalkBackMistriggeringRecoveryType.TYPE_AUTOMATIC_TURNOFF_LOCKSCREEN;
import static com.google.android.accessibility.talkback.TalkBackExitController.TalkBackMistriggeringRecoveryType.TYPE_AUTOMATIC_TURNOFF_SHUTDOWN;
import static com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId.PAGE_ID_WELCOME_TO_TALKBACK;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH_CAN_IGNORE_INTERRUPTS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.monitor.RingerModeAndScreenMonitor.ScreenChangedListener;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.monitor.ScreenMonitor;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Controller for TalkBack mis-triggering recovery.
 *
 * <ul>
 *   Turns off TalkBack settings
 *   <li>1. When TalkBack-exit button is single-tapped by the user,
 * </ul>
 *
 * <ul>
 *   TalkBack automatically turns off TalkBack settings
 *   <li>When it is device shutdown, the training is active and the current training page is the
 *       welcome-to-TalkBack,
 *   <li>When it enters lockscreen mode, the training is active and the current training page is the
 *       welcome-to-TalkBack,
 * </ul>
 */
public class TalkBackExitController implements AccessibilityEventListener, ScreenChangedListener {

  /** Interfaces to get training state for talkback automatic turn-off. */
  public interface TrainingState {
    /** Returns the current training page ID. */
    PageId getCurrentPageId();

    /** Returns {@code true} if training is active recently. */
    boolean isTrainingRecentActive();
  }

  /**
   * TalkBack mis-triggering recovery type and event. See TalkBackMistriggeringRecoveryEnums. And
   * the enum ordinal should match the proto enums.
   */
  public enum TalkBackMistriggeringRecoveryType {
    TYPE_UNSPECIFIED(0),
    TYPE_TALKBACK_EXIT_BANNER(1),
    TYPE_AUTOMATIC_TURNOFF_LOCKSCREEN(2),
    TYPE_AUTOMATIC_TURNOFF_SHUTDOWN(3),
    TYPE_ACCESSIBILITY_SHORTCUT(4),
    TYPE_TALKBACK_EXIT_WATERMARK(5),
    EVENT_SHOW_TALKBACK_EXIT_WATERMARK(6),
    ;

    private final int index;

    TalkBackMistriggeringRecoveryType(int index) {
      this.index = index;
    }

    int getIndex() {
      return index;
    }
  }

  private TrainingState trainingState;

  private static final String TAG = "TalkBackExitController";
  private static final String EXIT_BUTTON_RES_NAME =
      "com.google.android.marvin.talkback:id/training_exit_talkback_button";
  private static final long TAP_TIMEOUT_MS = ViewConfiguration.getJumpTapTimeout();

  private final TalkBackService service;
  private final Pipeline.FeedbackReturner pipeline;
  private ActorState actorState;

  private long touchInteractionStartTime;

  /** The button in Tutorial talkback-exit banner. */
  private AccessibilityNodeInfo targetNode = null;

  /** TalkBack-exit watermark on screen. */
  private @Nullable View talkbackExitWatermark;

  public TalkBackExitController(TalkBackService service, Pipeline.FeedbackReturner pipeline) {
    this.service = service;
    this.pipeline = pipeline;
  }

  @Override
  public int getEventTypes() {
    return AccessibilityEvent.TYPE_TOUCH_INTERACTION_START
        | AccessibilityEvent.TYPE_TOUCH_INTERACTION_END
        | AccessibilityEvent.TYPE_VIEW_HOVER_ENTER;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    AccessibilityNodeInfo node = event.getSource();
    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> {
        touchInteractionStartTime = SystemClock.uptimeMillis();
        targetNode = null;
      }
      case AccessibilityEvent.TYPE_TOUCH_INTERACTION_END -> {
        // ExitBanner handles the click event action.
        if (targetNode != null
            && ((SystemClock.uptimeMillis() - touchInteractionStartTime) < TAP_TIMEOUT_MS)) {
          targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
          targetNode = null;
        }
      }
      case AccessibilityEvent.TYPE_VIEW_HOVER_ENTER -> {
        String viewResIdName = node == null ? "" : node.getViewIdResourceName();
        targetNode = TextUtils.equals(viewResIdName, EXIT_BUTTON_RES_NAME) ? node : null;
      }
      default -> {}
    }
  }

  public void onShutDown() {
    if (trainingState == null || !FeatureFlagReader.allowAutomaticTurnOff(service)) {
      return;
    }
    LogUtils.d(TAG, "onShutDown: ");
    turnOffTalkBackIfTutorialActive(TYPE_AUTOMATIC_TURNOFF_SHUTDOWN.getIndex());
  }

  @Override
  public void onScreenChanged(boolean isInteractive, EventId eventId) {
    // TODO optimize the conditions gradually
    if (actorState == null || trainingState == null) {
      return;
    }
    int lastPerformedSystemAction = actorState.getLastSystemAction();
    boolean isDeviceLocked = ScreenMonitor.isDeviceLocked(service);
    LogUtils.d(
        TAG,
        "onScreenChanged: isDeviceLocked=%b , lastPerformedSystemAction=%d",
        isDeviceLocked,
        lastPerformedSystemAction);
    // The device is locked and no system action performed since TalkBack is on.
    if (isDeviceLocked && lastPerformedSystemAction == 0) {
      turnOffTalkBackIfTutorialActive(TYPE_AUTOMATIC_TURNOFF_LOCKSCREEN.getIndex());
    }
  }

  public void setActorState(ActorState actorState) {
    this.actorState = actorState;
  }

  public void setTrainingState(TrainingState state) {
    trainingState = state;
  }

  /**
   * Show TalkBack-exit watermark on screen if Training is not finished properly. When the watermark
   * shows, it speaks a usage hint for TalkBack user and the watermark background fade out after
   * showing 1 min.
   */
  @SuppressLint("InflateParams")
  public void showTalkBackExitWatermark() {
    if (talkbackExitWatermark != null) {
      return;
    }

    LayoutInflater inflater = LayoutInflater.from(service);
    talkbackExitWatermark = inflater.inflate(R.layout.talkback_exit_watermark, /* root= */ null);

    WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    final WindowManager.LayoutParams parameters = new WindowManager.LayoutParams();
    parameters.type = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
    parameters.flags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
    parameters.format = PixelFormat.TRANSPARENT;
    parameters.width = LayoutParams.MATCH_PARENT;
    parameters.height = LayoutParams.WRAP_CONTENT;
    parameters.gravity = Gravity.TOP;

    // Prevent crash when IpcClient disconnected.
    try {
      wm.addView(talkbackExitWatermark, parameters);
    } catch (WindowManager.BadTokenException e) {
      LogUtils.e(TAG, "showTalkBackExitWatermark: " + e);
      talkbackExitWatermark = null;
      return;
    }

    announceUsageHint();
  }

  private void announceUsageHint() {
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
            service.getString(R.string.talkback_exit_watermark_hint_text),
            SpeakOptions.create()
                .setQueueMode(QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH_CAN_IGNORE_INTERRUPTS)
                .setFlags(
                    FeedbackItem.FLAG_NO_HISTORY
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE
                        | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE)));
  }

  /** Hides TalkBack-exit watermark. */
  public void hideTalkBackExitWatermark() {
    if (talkbackExitWatermark == null) {
      return;
    }
    WindowManager wm = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
    wm.removeView(talkbackExitWatermark);
    talkbackExitWatermark = null;
  }

  /** Returns {@code true} if TalkBack-exit watermark is shown. */
  public boolean isTalkBackExitWatermarkShown() {
    return talkbackExitWatermark != null;
  }

  /** Disable TalkBack-exit watermark settings and hide the UI. */
  public void disableTalkBackExitWatermark(Context context) {
    SharedPreferencesUtils.putBooleanPref(
        SharedPreferencesUtils.getSharedPreferences(context),
        context.getResources(),
        R.string.pref_show_exit_watermark_key,
        false);
  }

  private void turnOffTalkBackIfTutorialActive(int recoveryType) {
    boolean trainingRecentActive = trainingState.isTrainingRecentActive();
    PageId currentPageId = trainingState.getCurrentPageId();
    LogUtils.w(
        TAG,
        "turnOffTalkBackIfTutorialActive:  trainingActive=%b, current pageId=%d",
        trainingRecentActive,
        currentPageId.ordinal());
    if (!service.hasTrainingFinishedByUser()
        && trainingRecentActive
        && currentPageId == PAGE_ID_WELCOME_TO_TALKBACK) {
      service.requestDisableTalkBack(recoveryType);
    }
  }
}
