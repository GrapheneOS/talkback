package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.text.style.TtsSpan.DateBuilder;
import android.text.style.TtsSpan.TimeBuilder;
import android.view.accessibility.AccessibilityEvent;
import java.text.DateFormat;
import java.util.Calendar;

/** An abstract class parses pop-up message from accessibility event. */
public abstract class Parser {
  Context context;
  AccessibilityEvent event;
  static final String SEPARATOR_PERIOD = ". ";

  public Parser(Context context) {
    this.context = context;
  }

  /** Parses an event to {@link PopUpMessageInfo}. */
  public PopUpMessageInfo parse(AccessibilityEvent event) {
    init(event);

    long timestamp = System.currentTimeMillis();
    this.event = event;
    // Start creating AnnouncementInfo
    return PopUpMessageInfo.builder()
        .setTitle(getTitle())
        .setEventTime(event.getEventTime())
        .setContent(getContent())
        .setCategory(getCategory())
        .setOutput(getOutput())
        .setTimestamp(timestamp)
        .setSpannableDate(getSpannableDate(timestamp))
        .setSpannableTime(getSpannableTime(timestamp))
        .build();
  }

  /** Gets the string output from the parser. */
  public String getOutput() {
    String content = getContent();
    if (TextUtils.isEmpty(content)) {
      return "";
    }
    String category = getCategory();
    String title = getTitle();
    if (TextUtils.isEmpty(category)) {
      if (TextUtils.isEmpty(title)) {
        return content;
      }
      return String.join(SEPARATOR_PERIOD, title, content);
    }
    if (TextUtils.isEmpty(title)) {
      return String.join(SEPARATOR_PERIOD, "[" + category + "]", content);
    }
    return String.join(SEPARATOR_PERIOD, "[" + category + "]", title, content);
  }

  /** An abstract function provide flexibility to initialize before parsing event. */
  abstract void init(AccessibilityEvent event);

  /** An abstract function to get the title of popup message if there is one. */
  abstract String getTitle();

  /** An abstract function to get the content of popup message. */
  abstract String getContent();

  /** An abstract function to get the category of popup message. */
  abstract String getCategory();

  /** functions about tts objects */
  private static SpannableStringBuilder getSpannableTime(long timestamp) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timestamp);
    TtsSpan timeSpan =
        new TimeBuilder(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE)).build();
    String timeString = getFormattedTime(timestamp);
    SpannableStringBuilder spannable = new SpannableStringBuilder(timeString);
    spannable.setSpan(timeSpan, 0, timeString.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }

  private static SpannableStringBuilder getSpannableDate(long timestamp) {
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(timestamp);
    TtsSpan dateSpan =
        new DateBuilder()
            .setYear(calendar.get(Calendar.YEAR))
            .setMonth(calendar.get(Calendar.MONTH))
            .setDay(calendar.get(Calendar.DATE))
            .build();
    String dateString = getFormattedDate(timestamp);
    SpannableStringBuilder spannable = new SpannableStringBuilder(dateString);
    spannable.setSpan(dateSpan, 0, dateString.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannable;
  }

  private static String getFormattedDate(long timestamp) {
    DateFormat dateFormatter = DateFormat.getDateInstance();
    return dateFormatter.format(timestamp);
  }

  private static String getFormattedTime(long timestamp) {
    DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
    return timeFormatter.format(timestamp);
  }
}
