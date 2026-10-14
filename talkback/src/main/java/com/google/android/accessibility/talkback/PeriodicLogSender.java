package com.google.android.accessibility.talkback;

import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import java.util.concurrent.ScheduledExecutorService;

/** Stub class. */
public class PeriodicLogSender {
  public PeriodicLogSender(TalkBackAnalytics analytics, long intervalInMinute) {}

  @VisibleForTesting
  public void setScheduler(ScheduledExecutorService scheduler) {}
}
