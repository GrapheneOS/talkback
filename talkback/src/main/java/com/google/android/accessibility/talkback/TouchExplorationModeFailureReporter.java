package com.google.android.accessibility.talkback;

import static android.accessibilityservice.TouchInteractionController.STATE_DRAGGING;
import static android.accessibilityservice.TouchInteractionController.STATE_TOUCH_EXPLORING;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.IntDef;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

/** Reports tne failure of entering touch-exploration mode. */
public class TouchExplorationModeFailureReporter {
  private static final Duration ENTER_TOUCH_EXPLORATION_TIMEOUT = Duration.ofSeconds(1);
  private static final String TAG = "TouchExplorationModeFailureReporter";

  /** Describes the state at each stage for entering touch-exploration mode. */
  @IntDef({STATE_CLEAR, STATE_REQUESTED, STATE_TOUCH_EXPLORING_MODE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface State {}

  /** The initial state or the completed state receiving hover_enter event from the view */
  public static final int STATE_CLEAR = 0;

  public static final int STATE_REQUESTED = 1;
  public static final int STATE_TOUCH_EXPLORING_MODE = 2;

  @State private volatile int trackingTouchExplorationState = 0;
  private final AtomicInteger maxReportFailureTimes = new AtomicInteger(3);
  private final TalkBackAnalytics talkBackAnalytics;

  public TouchExplorationModeFailureReporter(TalkBackAnalytics talkBackAnalytics) {
    this.talkBackAnalytics = talkBackAnalytics;
  }

  private final Handler failureReportHandler =
      new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message arg) {
          LogUtils.d(
              TAG,
              "sendRequestTouchExplorationFailureLog: handleMessage, state=%d",
              trackingTouchExplorationState);
          if (trackingTouchExplorationState == STATE_CLEAR) {
            return;
          }
          var unused = maxReportFailureTimes.decrementAndGet();
          LogUtils.d(
              TAG,
              "sendRequestTouchExplorationFailureLog: state=%d",
              trackingTouchExplorationState);
          talkBackAnalytics.sendRequestTouchExplorationFailureLog(trackingTouchExplorationState);
          clear();
        }
      };

  public void onTouchStateChanged(int state) {
    if (trackingTouchExplorationState != STATE_REQUESTED) {
      return;
    }
    LogUtils.d(TAG, "onTouchStateChanged: %d", state);
    if (state == STATE_DRAGGING) {
      clear();
    } else if (state == STATE_TOUCH_EXPLORING) {
      setState(STATE_TOUCH_EXPLORING_MODE);
    }
  }

  public void onHoverEntered() {
    if (trackingTouchExplorationState == STATE_TOUCH_EXPLORING_MODE) {
      LogUtils.d(TAG, "onHoverEntered, clear state");
      clear();
    }
  }

  public void onRequestTouchExploration() {
    LogUtils.d(TAG, "onRequestTouchExploration state=%d", trackingTouchExplorationState);
    if (maxReportFailureTimes.get() > 0 && trackingTouchExplorationState == STATE_CLEAR) {
      setState(STATE_REQUESTED);
      failureReportHandler.sendEmptyMessageDelayed(
          /* what= */ 0, ENTER_TOUCH_EXPLORATION_TIMEOUT.toMillis());
    }
  }

  public void clear() {
    failureReportHandler.removeCallbacksAndMessages(null);
    setState(STATE_CLEAR);
  }

  private void setState(@State int state) {
    LogUtils.d(TAG, "setState : %d", state);
    trackingTouchExplorationState = state;
  }
}
