package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;

import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.Parser.SEPARATOR_PERIOD;

import android.text.SpannableStringBuilder;
import android.text.format.DateUtils;
import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * A Class stores announcement information.
 *
 * <p>This object encapsulates the data sent by an announcement, including the title, category, and
 * content as {@link String}.
 */
@AutoValue
public abstract class PopUpMessageInfo {
  public abstract long eventTime();

  abstract String title();

  abstract String category();

  public abstract String content();

  public abstract String output();

  abstract long timestamp();

  abstract CharSequence spannableTime();

  abstract CharSequence spannableDate();

  static Builder builder() {
    return new AutoValue_PopUpMessageInfo.Builder()
        .setEventTime(0)
        .setTitle("")
        .setCategory("")
        .setContent("")
        .setOutput("")
        .setTimestamp(0);
  }

  @AutoValue.Builder
  abstract static class Builder {
    abstract Builder setEventTime(long eventTime);

    abstract Builder setTitle(String title);

    abstract Builder setCategory(String category);

    abstract Builder setContent(String content);

    abstract Builder setOutput(String output);

    abstract Builder setTimestamp(long timeStamp);

    public abstract Builder setSpannableTime(CharSequence spannableTime);

    public abstract Builder setSpannableDate(CharSequence spannableDate);

    abstract PopUpMessageInfo build();
  }

  /**
   * Returns the {@link String} of the content (with timestamps) showing in History Dialog, and
   * calculate the index of TtsSpan start/end, thus it has to be called before setting indices
   */
  public SpannableStringBuilder getRecordOutput() {
    return appendWithDelimiter(
        new SpannableStringBuilder(getRelativeTime(this.timestamp())),
        this.output(),
        this.spannableDate(),
        this.spannableTime());
  }

  @CanIgnoreReturnValue
  private SpannableStringBuilder appendWithDelimiter(
      SpannableStringBuilder builder, CharSequence... charSequences) {
    for (CharSequence charSequence : charSequences) {
      builder.append(SEPARATOR_PERIOD).append(charSequence);
    }
    return builder;
  }

  /**
   * A function gets time-related text output elements. Because the relative time changed as time
   * goes by, it should be generated at the moment the textview is built and showed.
   */
  private static String getRelativeTime(long timestamp) {
    long currentTime = System.currentTimeMillis();
    return DateUtils.getRelativeTimeSpanString(timestamp, currentTime, MINUTE_IN_MILLIS).toString();
  }
}
