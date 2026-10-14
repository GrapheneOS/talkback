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

package com.google.android.accessibility.talkback.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.monitor.RingerModeAndScreenMonitor;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Clock;

/** Utility class to get the current time and date. */
public final class DateTimeUtils {
  private static final String TAG = "DateTimeUtils";

  /** Ids of time feedback formats in advanced settings. */
  @IntDef({
    TIME_FEEDBACK_FORMAT_UNDEFINED,
    TIME_FEEDBACK_FORMAT_12_HOURS,
    TIME_FEEDBACK_FORMAT_24_HOURS
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface TimeFeedbackFormat {}

  public static final int TIME_FEEDBACK_FORMAT_UNDEFINED = 0;
  public static final int TIME_FEEDBACK_FORMAT_12_HOURS = 1;
  public static final int TIME_FEEDBACK_FORMAT_24_HOURS = 2;

  private static Clock clock = Clock.systemUTC();

  /** Returns the current time in text using the local format. */
  public static String getCurrentTime(Context context, @TimeFeedbackFormat int timeFormat) {
    int timeFlags = DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_CAP_NOON_MIDNIGHT;

    return formatCurrentTime(context, clock.millis(), timeFlags, timeFormat);
  }

  /** Returns the current time and date in text using the local format. */
  public static String getCurrentTimeAndDate(Context context) {
    int timeFlags =
        DateUtils.FORMAT_SHOW_TIME
            | DateUtils.FORMAT_SHOW_DATE
            | DateUtils.FORMAT_SHOW_WEEKDAY
            | DateUtils.FORMAT_SHOW_YEAR
            | DateUtils.FORMAT_CAP_NOON_MIDNIGHT;

    Resources resources = context.getResources();
    final SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    final String timeFeedbackFormat =
        SharedPreferencesUtils.getStringPref(
            prefs,
            resources,
            R.string.pref_time_feedback_format_key,
            R.string.pref_time_feedback_format_default);

    int timeFormat =
        RingerModeAndScreenMonitor.prefValueToTimeFeedbackFormat(resources, timeFeedbackFormat);

    return formatCurrentDateTime(context, timeFlags, timeFormat);
  }

  // Formats the output in a way that the time comes first and the date comes second.
  private static String formatCurrentDateTime(
      Context context, int timeFlags, @TimeFeedbackFormat int timeFormat) {

    long millis = clock.millis();
    String timeString = formatCurrentTime(context, millis, timeFlags, timeFormat);
    String dateString = formatCurrentDate(context, millis, timeFlags);

    if (timeString.isEmpty()) {
      return dateString;
    }
    if (dateString.isEmpty()) {
      return timeString;
    }
    return BidiFormatter.getInstance()
        .unicodeWrap(
            String.format("%s %s", timeString, dateString), TextDirectionHeuristics.LOCALE);
  }

  private static String formatCurrentTime(
      Context context, long millis, int timeFlags, @TimeFeedbackFormat int timeFormat) {
    int currentTimeFlags = timeFlags & DateUtils.FORMAT_SHOW_TIME;
    String timeString = "";
    if (currentTimeFlags != 0) {
      currentTimeFlags |=
          switch (timeFormat) {
            case DateTimeUtils.TIME_FEEDBACK_FORMAT_12_HOURS -> DateUtils.FORMAT_12HOUR;
            case DateTimeUtils.TIME_FEEDBACK_FORMAT_24_HOURS -> DateUtils.FORMAT_24HOUR;
            default ->
                DateFormat.is24HourFormat(context)
                    ? DateUtils.FORMAT_24HOUR
                    : DateUtils.FORMAT_12HOUR;
          };
      timeString = DateUtils.formatDateTime(context, millis, currentTimeFlags);
    }
    return timeString;
  }

  private static String formatCurrentDate(Context context, long millis, int timeFlags) {
    int dateFlags = timeFlags & ~DateUtils.FORMAT_SHOW_TIME;
    String dateString = "";
    if (dateFlags != 0) {
      dateString = DateUtils.formatDateTime(context, millis, dateFlags);
    }
    return dateString;
  }

  /** Injects a customized clock for testing. */
  @VisibleForTesting
  public static void setMockClock(Clock clock) {
    DateTimeUtils.clock = clock;
  }

  @VisibleForTesting
  public static void resetMockClock() {
    DateTimeUtils.clock = Clock.systemUTC();
  }

  private DateTimeUtils() {}
}
