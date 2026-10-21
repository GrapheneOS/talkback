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
package com.google.android.accessibility.utils.output;

import static com.google.android.accessibility.utils.StringBuilderUtils.DEFAULT_BREAKING_SEPARATOR;
import static com.google.android.accessibility.utils.StringBuilderUtils.DEFAULT_SEPARATOR;
import static com.google.android.accessibility.utils.output.TextFormattingUtils.OPTION_BOLD;
import static com.google.android.accessibility.utils.output.TextFormattingUtils.OPTION_FONT_COLOR_HEX;
import static com.google.android.accessibility.utils.output.TextFormattingUtils.OPTION_FONT_COLOR_NAME;
import static com.google.android.accessibility.utils.output.TextFormattingUtils.OPTION_FONT_FAMILY;
import static com.google.android.accessibility.utils.output.TextFormattingUtils.OPTION_FONT_SIZE;
import static com.google.android.accessibility.utils.output.TextFormattingUtils.OPTION_ITALIC;

import android.content.Context;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.AbsoluteSizeSpan;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TextAppearanceSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.Pair;
import android.util.TypedValue;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.utils.R;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.android.accessibility.utils.SpannableUtils.SourceTextSpan;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.output.SpeechControllerImpl.InlineFormattingHistory;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Holds the formatting information for a text chunk and composes the formatting feedback. */
public class TextFormattingInfo {
  private static final String TAG = "TextFormattingInfo";

  /** The formatting infos from the current text chunk. */
  private ImmutableList<FormattingInfo> selfInfos = ImmutableList.of();

  /** The formatting infos by comparing the changes between the previous and current text chunks. */
  private ImmutableList<FormattingInfo> diffInfos = ImmutableList.of();

  /** The start index of {@link SourceTextSpan} in the current text chunk. */
  private int sourceTextBegin = -1;

  /** The end index of {@link SourceTextSpan} in the current text chunk. */
  private int sourceTextEnd = -1;

  /**
   * Constructs specified formatting information from {@link InlineFormattingHistory} cache.
   *
   * @param context The caller's context.
   * @param history The cached inline feedback history.
   * @param options The specified text formatting options.
   */
  public TextFormattingInfo(Context context, InlineFormattingHistory history, int options) {
    selfInfos = buildInfosFromCache(context, history, options);
    LogUtils.v(
        TAG,
        "TextFormattingInfo from previous chunk= %s with inlineFeedbackHistory= %s",
        this.toString(),
        history);
  }

  /**
   * Constructs specified formatting information from the current text chunk.
   *
   * @param context The caller's context.
   * @param spans The spans to build the formatting infos.
   * @param spannable The spannable contains the spans.
   * @param options The specified text formatting options.
   */
  public TextFormattingInfo(
      Context context, List<CharacterStyle> spans, Spannable spannable, int options) {
    this(context, spans, spannable, 0, spannable.length(), options, /* previousInfos= */ null);
  }

  /**
   * Constructs specified formatting information by comparing the changes between current and
   * previous text chunk if {@code previousInfos} is not null. Otherwise, extracts the specified
   * information from the current text chunk.
   *
   * @param context The caller's context.
   * @param spans The spans to build the formatting infos.
   * @param spannable The spannable contains the spans.
   * @param chunkStart The start index of the text chunk in the spannable.
   * @param chunkEnd The end index of the text chunk in the spannable.
   * @param options The specified text formatting options.
   * @param previousInfos The formatting infos from the previous text chunk.
   */
  public TextFormattingInfo(
      Context context,
      List<CharacterStyle> spans,
      Spannable spannable,
      int chunkStart,
      int chunkEnd,
      int options,
      ImmutableList<FormattingInfo> previousInfos) {
    selfInfos = buildInfos(context, spans, spannable, options);
    if (previousInfos == null) {
      this.sourceTextBegin = chunkStart;
      this.sourceTextEnd = chunkEnd;
      diffInfos = selfInfos;
    } else {
      computeSourceTextIndex(spannable, chunkStart, chunkEnd);
      diffInfos = computeStartEndInfos(previousInfos);
    }
    LogUtils.v(
        TAG,
        "TextFormattingInfo from current chunk= %s with previousInfos= %s",
        this.toString(),
        previousInfos);
  }

  /**
   * Computes the start and end index of {@link SourceTextSpan} to identify the scope of the visible
   * text. For example, for composed text "ON, toggle button, Switch", the source text should be the
   * main text "toggle button", and the "toggle button" should be wrap of {@link SourceTextSpan} in
   * Compositor.
   *
   * <p>Ideally, there should be only one formatted {@link SourceTextSpan} in the chunk because
   * others having formattings should be chunked separately. If there are multiple {@link
   * SourceTextSpan}, pick the first one for {@code sourceTextBegin} and the last one for {@code
   * sourceTextEnd}.
   *
   * @param spannable The spannable contains the spans.
   * @param startIndex The start index of the chunk in the spannable.
   * @param endIndex The end index of the chunk in the spannable.
   */
  private void computeSourceTextIndex(Spannable spannable, int startIndex, int endIndex) {
    SourceTextSpan[] spans = spannable.getSpans(startIndex, endIndex, SourceTextSpan.class);
    if (spans == null || spans.length == 0) {
      return;
    }
    sourceTextBegin = Math.max(spannable.getSpanStart(spans[0]), startIndex);
    sourceTextEnd = Math.min(spannable.getSpanEnd(spans[spans.length - 1]), endIndex);
  }

  /**
   * Computes the start and end of the formatting infos.
   *
   * <p>If there is no {@code selfInfos}, it will add the previous end info in the beginning of the
   * list. Otherwise, it will remove the previous end info same as the self start info.
   *
   * @param previousEndInfos The ending formatting infos from the previous text chunk.
   */
  private ImmutableList<FormattingInfo> computeStartEndInfos(
      ImmutableList<FormattingInfo> previousEndInfos) {
    List<FormattingInfo> result = new ArrayList<>();
    if (selfInfos == null || selfInfos.isEmpty()) {
      if (previousEndInfos != null) {
        // Add End previous info in the beginning of the list.
        for (TextFormattingInfo.FormattingInfo item : Lists.reverse(previousEndInfos)) {
          FormattingInfo info = item.newInstance();
          info.setIsPreviousEnd(true);
          result.add(0, info);
        }
      }
      return ImmutableList.copyOf(result);
    }
    List<FormattingInfo> tempSelfInfos = new ArrayList<>();
    for (FormattingInfo info : selfInfos) {
      tempSelfInfos.add(info.newInstance());
    }
    result.addAll(tempSelfInfos);
    if (previousEndInfos != null && !previousEndInfos.isEmpty()) {
      List<FormattingInfo> tempPreviousEndInfos = new ArrayList<>();
      tempPreviousEndInfos.addAll(previousEndInfos);
      // Remove span start info same as previous info.
      for (FormattingInfo info : tempSelfInfos) {
        if (info.range.first == sourceTextBegin) {
          for (FormattingInfo previousEndInfo : previousEndInfos) {
            if (previousEndInfo instanceof StyleFormattingInfo preInfo
                && info instanceof StyleFormattingInfo curInfo) {
              dedupStyleInfo(preInfo, curInfo);
              if (preInfo.style == 0) {
                tempPreviousEndInfos.remove(previousEndInfo);
              }
              if (curInfo.style == 0) {
                info.setIsPreviousStart(true);
              }
              break;
            } else if (previousEndInfo.equals(info)) {
              info.setIsPreviousStart(true);
              tempPreviousEndInfos.remove(previousEndInfo);
              break;
            }
          }
        }
      }
      // Add End previous info in the beginning of the list.
      for (TextFormattingInfo.FormattingInfo item : Lists.reverse(tempPreviousEndInfos)) {
        FormattingInfo info = item.newInstance();
        info.setIsPreviousEnd(true);
        result.add(0, info);
      }
    }
    return ImmutableList.copyOf(result);
  }

  /**
   * Builds the formatting infos from the {@link InlineFormattingHistory} cache. Only extracts
   * information from the last character to identify the ending formattings.
   */
  private ImmutableList<FormattingInfo> buildInfosFromCache(
      Context context, InlineFormattingHistory previousInlineFeedbackHistory, int options) {
    if (previousInlineFeedbackHistory == null) {
      return ImmutableList.of();
    }
    CharSequence lastSourceText = previousInlineFeedbackHistory.getLastSourceText();
    if (TextUtils.isEmpty(lastSourceText)
        || !(lastSourceText instanceof Spannable previousSpannable)) {
      return ImmutableList.of();
    }
    CharacterStyle[] spans =
        previousSpannable.getSpans(0, previousSpannable.length(), CharacterStyle.class);
    if (spans == null || spans.length == 0) {
      return ImmutableList.of();
    }
    List<FormattingInfo> infos = new ArrayList<>();
    ImmutableList<FormattingInfo> tempInfos =
        buildInfos(context, Arrays.asList(spans), previousSpannable, options);
    for (FormattingInfo info : tempInfos) {
      // Only extracts information from the last character or last character with one punctuation.
      if (info.range.second == previousSpannable.length()
          || info.range.second == previousSpannable.length() - 1) {
        infos.add(info);
      }
    }
    return ImmutableList.copyOf(infos);
  }

  /**
   * Builds and dedupes the formatting infos from the given spans.
   *
   * @param context The caller's context.
   * @param spans The spans to build the formatting infos.
   * @param spannable The spannable to build the formatting infos.
   * @param options The specified text formatting options.
   * @return The formatting infos.
   */
  private ImmutableList<FormattingInfo> buildInfos(
      Context context, List<CharacterStyle> spans, Spannable spannable, int options) {
    List<FormattingInfo> infos = new ArrayList<>();
    if (spans == null || spans.isEmpty()) {
      return ImmutableList.of();
    }
    for (CharacterStyle span : spans) {
      if (!TextFormattingUtils.isSpanMatchingOptions(span, options)) {
        continue;
      }
      int spanStart = spannable.getSpanStart(span);
      int spanEnd = spannable.getSpanEnd(span);
      final CharSequence text = spannable.subSequence(spanStart, spanEnd);
      boolean isPartial = !WordUtils.isSpannedFullWord(spannable, spanStart, spanEnd);
      Pair<Integer, Integer> range = new Pair<>(spanStart, spanEnd);
      if (span instanceof StyleSpan styleSpan) {
        StyleFormattingInfo style =
            new StyleFormattingInfo(range, isPartial, text, styleSpan.getStyle(), options);
        mergeStyleInfo(infos, style);
      } else if (span instanceof UnderlineSpan) {
        FormattingInfo underline = new UnderlineFormattingInfo(range, isPartial, text);
        insertOrOverride(infos, underline);
      } else if (span instanceof StrikethroughSpan) {
        FormattingInfo strikethrough = new StrikethroughFormattingInfo(range, isPartial, text);
        insertOrOverride(infos, strikethrough);
      } else if (span instanceof ForegroundColorSpan foregroundColorSpan) {
        FormattingInfo color =
            new ColorFormattingInfo(
                range, isPartial, text, foregroundColorSpan.getForegroundColor(), options);
        insertOrOverride(infos, color);
      } else if (span instanceof BackgroundColorSpan backgroundColorSpan) {
        FormattingInfo bgColor =
            new BackgroundColorFormattingInfo(
                range, isPartial, text, backgroundColorSpan.getBackgroundColor(), options);
        insertOrOverride(infos, bgColor);
      } else if (span instanceof RelativeSizeSpan relativeSizeSpan) {
        FormattingInfo size =
            new SizeFormattingInfo(range, isPartial, text, relativeSizeSpan.getSizeChange());
        insertOrOverride(infos, size);
      } else if (span instanceof AbsoluteSizeSpan absoluteSizeSpan) {
        int fontSize = absoluteSizeSpan.getSize();
        FormattingInfo size =
            new SizeFormattingInfo(
                range,
                isPartial,
                text,
                absoluteSizeSpan.getDip()
                    ? (int)
                        TypedValue.applyDimension(
                            TypedValue.COMPLEX_UNIT_DIP,
                            fontSize,
                            context.getResources().getDisplayMetrics())
                    : fontSize);
        insertOrOverride(infos, size);
      } else if (span instanceof TypefaceSpan typefaceSpan) {
        FormattingInfo fontFamily =
            new FontFamilyFormattingInfo(range, isPartial, text, typefaceSpan.getFamily());
        insertOrOverride(infos, fontFamily);
      } else if (span instanceof TextAppearanceSpan textAppearanceSpan) {
        // TextAppearanceSpan is used for style, font size, color and font family. Because of the
        // multiple data, it still needs to check the individual data although the span is matched.
        if (textAppearanceSpan.getTextStyle() != 0
            && ((options & OPTION_BOLD) != 0 || (options & OPTION_ITALIC) != 0)) {
          StyleFormattingInfo style =
              new StyleFormattingInfo(
                  range, isPartial, text, textAppearanceSpan.getTextStyle(), options);
          mergeStyleInfo(infos, style);
        }
        if (textAppearanceSpan.getTextSize() != -1 && (options & OPTION_FONT_SIZE) != 0) {
          FormattingInfo size =
              new SizeFormattingInfo(range, isPartial, text, textAppearanceSpan.getTextSize());
          insertOrOverride(infos, size);
        }
        if (textAppearanceSpan.getTextColor() != null
            && textAppearanceSpan.getTextColor().getDefaultColor() != 0
            && ((options & OPTION_FONT_COLOR_NAME) != 0
                || (options & OPTION_FONT_COLOR_HEX) != 0)) {
          FormattingInfo color =
              new ColorFormattingInfo(
                  range,
                  isPartial,
                  text,
                  textAppearanceSpan.getTextColor().getDefaultColor(),
                  options);
          insertOrOverride(infos, color);
        }
        if (!TextUtils.isEmpty(textAppearanceSpan.getFamily())
            && (options & OPTION_FONT_FAMILY) != 0) {
          FormattingInfo fontFamily =
              new FontFamilyFormattingInfo(range, isPartial, text, textAppearanceSpan.getFamily());
          insertOrOverride(infos, fontFamily);
        }
      }
    }
    return ImmutableList.copyOf(infos);
  }

  private void insertOrOverride(List<FormattingInfo> infos, FormattingInfo newInfo) {
    if (!newInfo.isValid()) {
      LogUtils.w(TAG, "Skip invalid FormattingInfo: %s", newInfo.toString());
      return;
    }
    FormattingInfo existingInfo = getExistingInfo(infos, newInfo);
    if (existingInfo != null) {
      infos.set(infos.indexOf(existingInfo), newInfo);
    } else {
      infos.add(newInfo);
    }
  }

  private void dedupStyleInfo(StyleFormattingInfo previousStyle, StyleFormattingInfo newStyle) {
    int duplicateStyle = previousStyle.style & newStyle.style;
    if (duplicateStyle == 0) {
      return;
    }
    previousStyle.style &= ~duplicateStyle;
    newStyle.style &= ~duplicateStyle;
  }

  private void mergeStyleInfo(List<FormattingInfo> infos, StyleFormattingInfo newStyle) {
    if (!newStyle.isValid()) {
      LogUtils.w(TAG, "Skip invalid StyleFormattingInfo: %s", newStyle.toString());
      return;
    }
    FormattingInfo existingStyle = getExistingInfo(infos, newStyle);
    if (existingStyle != null) {
      int preStyle = ((StyleFormattingInfo) existingStyle).style;
      ((StyleFormattingInfo) existingStyle).style = preStyle | newStyle.style;
    } else {
      infos.add(newStyle);
    }
  }

  /**
   * Returns the existing info with the same class and range as the given info, or null if not
   * found.
   */
  @Nullable
  private FormattingInfo getExistingInfo(List<FormattingInfo> infos, FormattingInfo givenInfo) {
    for (FormattingInfo info : infos) {
      if (info.getClass() == givenInfo.getClass() && info.range.equals(givenInfo.range)) {
        return info;
      }
    }
    return null;
  }

  /** Returns true if the text has {@link SourceTextSpan}. */
  public boolean hasSourceText() {
    return sourceTextBegin != -1 && sourceTextEnd != -1;
  }

  /** Returns the text formatting infos in the source text. */
  public ImmutableList<FormattingInfo> getSelfInfos() {
    return selfInfos;
  }

  /**
   * Generates the feedback for text formattings with its position in the source text.
   *
   * @param context the context of the source text
   * @param offset the offset of the source text in the grouping focus
   * @return the summary feedback with the position information.
   */
  public CharSequence getFormattingPositionFeedback(Context context, int offset) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    Map<Pair<Integer, Integer>, List<FormattingInfo>> infosByRange =
        selfInfos.stream()
            .collect(
                Collectors.groupingBy(
                    FormattingInfo::getRange, LinkedHashMap::new, Collectors.toList()));
    infosByRange.forEach(
        (range, infos) -> {
          SpannableStringBuilder descriptionBuilder = new SpannableStringBuilder();
          infos.forEach(
              info -> {
                CharSequence description =
                    info.getFormattingDescription(context, /* includePartial= */ true);
                if (!TextUtils.isEmpty(description)) {
                  descriptionBuilder.append(description);
                  descriptionBuilder.append(DEFAULT_BREAKING_SEPARATOR);
                }
              });
          if (descriptionBuilder.length() > 0) {
            descriptionBuilder.delete(
                descriptionBuilder.length() - DEFAULT_BREAKING_SEPARATOR.length(),
                descriptionBuilder.length());
            builder.append(
                SpannableUtils.getSpannedFormattedString(
                    context.getString(R.string.format_position),
                    descriptionBuilder,
                    range.first + offset,
                    range.second + offset,
                    infos
                        .get(0)
                        .text
                        .toString())); // Use plain text to prevent speaking inline formatting.
            builder.append(DEFAULT_BREAKING_SEPARATOR);
          }
        });
    if (builder.length() > 0) {
      builder.delete(builder.length() - DEFAULT_BREAKING_SEPARATOR.length(), builder.length());
    }
    return builder;
  }

  /** Generates the formatting start feedback, including the previous end feedback. */
  public CharSequence getFormattingInlineFeedback(Context context) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    builder.append(getFormattingPreviousEnd(context));
    if (builder.length() > 0) {
      builder.append(DEFAULT_BREAKING_SEPARATOR);
    }
    builder.append(getFormattingSelfStart(context));
    return builder;
  }

  private CharSequence getFormattingSelfStart(Context context) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    for (FormattingInfo info : diffInfos) {
      if (info.range.first == sourceTextBegin && !info.isPreviousStart() && !info.isPreviousEnd()) {
        CharSequence description =
            info.getFormattingDescription(context, /* includePartial= */ false);
        if (!TextUtils.isEmpty(description)) {
          builder.append(description);
          builder.append(DEFAULT_BREAKING_SEPARATOR);
        }
      }
    }
    if (builder.length() > 0) {
      builder.delete(builder.length() - DEFAULT_BREAKING_SEPARATOR.length(), builder.length());
    }
    return builder.length() > 0
        ? SpannableUtils.getSpannedFormattedString(
            context.getString(R.string.format_start), builder)
        : "";
  }

  private CharSequence getFormattingPreviousEnd(Context context) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    for (FormattingInfo info : diffInfos) {
      if (info.isPreviousEnd()) {
        CharSequence description =
            info.getFormattingDescription(context, /* includePartial= */ false);
        if (!TextUtils.isEmpty(description)) {
          builder.append(description);
          builder.append(DEFAULT_BREAKING_SEPARATOR);
        }
      }
    }
    if (builder.length() > 0) {
      builder.delete(builder.length() - DEFAULT_BREAKING_SEPARATOR.length(), builder.length());
    }
    return builder.length() > 0
        ? SpannableUtils.getSpannedFormattedString(context.getString(R.string.format_end), builder)
        : "";
  }

  @Override
  public String toString() {
    return StringBuilderUtils.joinFields(
        StringBuilderUtils.optionalInt("sourceTextBegin", sourceTextBegin, -1),
        StringBuilderUtils.optionalInt("sourceTextEnd", sourceTextEnd, -1),
        StringBuilderUtils.optionalField("diffInfos", diffInfos),
        StringBuilderUtils.optionalField("selfInfos", selfInfos));
  }

  /** The base class of text formatting. */
  @VisibleForTesting
  abstract static class FormattingInfo {
    /** The range of the formatting in the source text. */
    final Pair<Integer, Integer> range;

    /** Whether the formatting is partial (not the whole text). */
    final boolean isPartial;

    /** The wrapped text of the formatting. */
    final CharSequence text;

    /** Whether the formatting is already started in the previous speech. */
    private boolean isPreviousStart = false;

    /** Whether the formatting just ended in the previous speech. */
    private boolean isPreviousEnd = false;

    public FormattingInfo(Pair<Integer, Integer> range, boolean isPartial, CharSequence text) {
      this.range = range;
      this.isPartial = isPartial;
      this.text = text;
    }

    /** Returns a new instance of the formatting. */
    public abstract FormattingInfo newInstance();

    /** Returns true if the formatting is valid. */
    public boolean isValid() {
      return true;
    }

    public Pair<Integer, Integer> getRange() {
      return range;
    }

    public CharSequence getText() {
      return text;
    }

    public void setIsPreviousStart(boolean isPreviousStart) {
      this.isPreviousStart = isPreviousStart;
    }

    public void setIsPreviousEnd(boolean isPreviousEnd) {
      this.isPreviousEnd = isPreviousEnd;
    }

    public boolean isPreviousStart() {
      return isPreviousStart;
    }

    public boolean isPreviousEnd() {
      return isPreviousEnd;
    }

    /** Generates the description for the text formatting. */
    @Nullable
    public CharSequence getFormattingDescription(Context context, boolean includePartial) {
      if (!isValid() || (!includePartial && isPartial)) {
        return null;
      }
      return getFormattingDescriptionInternal(context);
    }

    abstract CharSequence getFormattingDescriptionInternal(Context context);

    /**
     * Returns the hash code of the formatting. It doesn't care about the wrapped text but the
     * formatting values.
     */
    @Override
    public int hashCode() {
      return Boolean.hashCode(isPartial);
    }

    /**
     * Returns true if the formatting is equal to the given object. It doesn't care about the
     * spanned text but the values of the formatting.
     */
    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof FormattingInfo other)) {
        return false;
      }
      return isPartial == other.isPartial;
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          StringBuilderUtils.optionalText("text", text),
          StringBuilderUtils.optionalField("range", range),
          StringBuilderUtils.optionalTag("isPartial", isPartial),
          StringBuilderUtils.optionalTag("isPreviousStart", isPreviousStart),
          StringBuilderUtils.optionalTag("isPreviousEnd", isPreviousEnd));
    }
  }

  /** The information of a style formatting. */
  @VisibleForTesting
  static class StyleFormattingInfo extends FormattingInfo {
    private final int options;
    int style;

    public StyleFormattingInfo(
        Pair<Integer, Integer> range,
        boolean isPartial,
        CharSequence text,
        int style,
        int options) {
      super(range, isPartial, text);
      this.options = options;
      this.style = getStyle(style);
    }

    @Override
    public FormattingInfo newInstance() {
      return new StyleFormattingInfo(range, isPartial, text, style, options);
    }

    @Override
    public boolean isValid() {
      return (style & Typeface.BOLD) != 0 || (style & Typeface.ITALIC) != 0;
    }

    private int getStyle(int style) {
      if ((options & OPTION_BOLD) != 0 && (options & OPTION_ITALIC) == 0) {
        return style & Typeface.BOLD;
      } else if ((options & OPTION_ITALIC) != 0 && (options & OPTION_BOLD) == 0) {
        return style & Typeface.ITALIC;
      }
      return style;
    }

    @Nullable
    @Override
    CharSequence getFormattingDescriptionInternal(Context context) {
      if ((style & Typeface.BOLD_ITALIC) == Typeface.BOLD_ITALIC) {
        return context.getString(R.string.format_bold_italic);
      } else if ((style & Typeface.BOLD) == Typeface.BOLD) {
        return context.getString(R.string.format_bold);
      } else if ((style & Typeface.ITALIC) == Typeface.ITALIC) {
        return context.getString(R.string.format_italic);
      } else if (style == Typeface.NORMAL) {
        LogUtils.w(TAG, "Normal StyleSpan found");
      }
      return null;
    }

    @Override
    public int hashCode() {
      return super.hashCode() ^ StyleFormattingInfo.class.hashCode() ^ Integer.hashCode(style);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof StyleFormattingInfo other)) {
        return false;
      }
      return style == other.style && super.equals(o);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          StyleFormattingInfo.class.getSimpleName(),
          StringBuilderUtils.optionalInt("style", style, -1),
          StringBuilderUtils.optionalInt("options", options, -1),
          super.toString());
    }
  }

  /** The information of an underline formatting. */
  @VisibleForTesting
  static class UnderlineFormattingInfo extends FormattingInfo {
    public UnderlineFormattingInfo(
        Pair<Integer, Integer> range, boolean isPartial, CharSequence text) {
      super(range, isPartial, text);
    }

    @Override
    public FormattingInfo newInstance() {
      return new UnderlineFormattingInfo(range, isPartial, text);
    }

    @Override
    CharSequence getFormattingDescriptionInternal(Context context) {
      return context.getString(R.string.format_underline);
    }

    @Override
    public int hashCode() {
      return super.hashCode() ^ UnderlineFormattingInfo.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof UnderlineFormattingInfo)) {
        return false;
      }
      return super.equals(o);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          UnderlineFormattingInfo.class.getSimpleName(),
          StringBuilderUtils.optionalTag("underline", true),
          super.toString());
    }
  }

  /** The information of a strikethrough formatting. */
  @VisibleForTesting
  static class StrikethroughFormattingInfo extends FormattingInfo {
    public StrikethroughFormattingInfo(
        Pair<Integer, Integer> range, boolean isPartial, CharSequence text) {
      super(range, isPartial, text);
    }

    @Override
    public FormattingInfo newInstance() {
      return new StrikethroughFormattingInfo(range, isPartial, text);
    }

    @Override
    CharSequence getFormattingDescriptionInternal(Context context) {
      return context.getString(R.string.format_strikethrough);
    }

    @Override
    public int hashCode() {
      return super.hashCode() ^ StrikethroughFormattingInfo.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof StrikethroughFormattingInfo)) {
        return false;
      }
      return super.equals(o);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          StrikethroughFormattingInfo.class.getSimpleName(),
          StringBuilderUtils.optionalTag("strikethrough", true),
          super.toString());
    }
  }

  /** The information of a color formatting. */
  @VisibleForTesting
  static class ColorFormattingInfo extends FormattingInfo {
    private final int color;
    private final int colorGroup;
    private final boolean describeColorName;
    private final boolean describeHexColor;
    private final int options;

    public ColorFormattingInfo(
        Pair<Integer, Integer> range,
        boolean isPartial,
        CharSequence text,
        int color,
        int options) {
      super(range, isPartial, text);
      this.color = color;
      this.describeColorName = (options & OPTION_FONT_COLOR_NAME) != 0;
      this.describeHexColor = (options & OPTION_FONT_COLOR_HEX) != 0;
      this.options = options;
      colorGroup = ColorNameUtils.getColorGroup(color);
    }

    @Override
    public FormattingInfo newInstance() {
      return new ColorFormattingInfo(range, isPartial, text, color, options);
    }

    @Override
    CharSequence getFormattingDescriptionInternal(Context context) {
      SpannableStringBuilder colorDescription = new SpannableStringBuilder();
      colorDescription.append(context.getString(R.string.format_foreground_color));
      colorDescription.append(DEFAULT_SEPARATOR);
      if (describeColorName) {
        colorDescription.append(ColorNameUtils.getColorName(context, color));
      }
      if (describeHexColor) {
        if (describeColorName) {
          colorDescription.append(DEFAULT_BREAKING_SEPARATOR);
        }
        colorDescription.append(ColorNameUtils.getColorHexValue(context, color));
      }
      return colorDescription;
    }

    @Override
    public int hashCode() {
      return super.hashCode() ^ ColorFormattingInfo.class.hashCode() ^ Integer.hashCode(colorGroup);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof ColorFormattingInfo other)) {
        return false;
      }
      return colorGroup == other.colorGroup && super.equals(o);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          ColorFormattingInfo.class.getSimpleName(),
          StringBuilderUtils.optionalInt("color", color, -1),
          StringBuilderUtils.optionalInt("colorGroup", colorGroup, -1),
          StringBuilderUtils.optionalTag("describeColorName", describeColorName),
          StringBuilderUtils.optionalTag("describeHexColor", describeHexColor),
          StringBuilderUtils.optionalInt("options", options, -1),
          super.toString());
    }
  }

  /** The information of a background color formatting. */
  @VisibleForTesting
  static class BackgroundColorFormattingInfo extends FormattingInfo {
    private final int bgColor;
    private final int bgColorGroup;
    private final boolean describeColorName;
    private final boolean describeHexColor;
    private final int options;

    public BackgroundColorFormattingInfo(
        Pair<Integer, Integer> range,
        boolean isPartial,
        CharSequence text,
        int bgColor,
        int options) {
      super(range, isPartial, text);
      this.bgColor = bgColor;
      this.options = options;
      this.describeColorName = (options & OPTION_FONT_COLOR_NAME) != 0;
      this.describeHexColor = (options & OPTION_FONT_COLOR_HEX) != 0;
      bgColorGroup = ColorNameUtils.getColorGroup(bgColor);
    }

    @Override
    public FormattingInfo newInstance() {
      return new BackgroundColorFormattingInfo(range, isPartial, text, bgColor, options);
    }

    @Override
    CharSequence getFormattingDescriptionInternal(Context context) {
      SpannableStringBuilder colorDescription = new SpannableStringBuilder();
      colorDescription.append(context.getString(R.string.format_background_color));
      colorDescription.append(DEFAULT_SEPARATOR);
      if (describeColorName) {
        colorDescription.append(ColorNameUtils.getColorName(context, bgColor));
      }
      if (describeHexColor) {
        if (describeColorName) {
          colorDescription.append(DEFAULT_BREAKING_SEPARATOR);
        }
        colorDescription.append(ColorNameUtils.getColorHexValue(context, bgColor));
      }
      return colorDescription;
    }

    @Override
    public int hashCode() {
      return super.hashCode()
          ^ BackgroundColorFormattingInfo.class.hashCode()
          ^ Integer.hashCode(bgColorGroup);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof BackgroundColorFormattingInfo other)) {
        return false;
      }
      return bgColorGroup == other.bgColorGroup && super.equals(o);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          BackgroundColorFormattingInfo.class.getSimpleName(),
          StringBuilderUtils.optionalInt("bg_color", bgColor, -1),
          StringBuilderUtils.optionalInt("bgColorGroup", bgColorGroup, -1),
          StringBuilderUtils.optionalTag("describeColorName", describeColorName),
          StringBuilderUtils.optionalTag("describeHexColor", describeHexColor),
          StringBuilderUtils.optionalInt("options", options, -1),
          super.toString());
    }
  }

  /** The information of a size formatting. */
  @VisibleForTesting
  static class SizeFormattingInfo extends FormattingInfo {
    private int pixel = 0;
    private float percentage = 0;

    public SizeFormattingInfo(
        Pair<Integer, Integer> range, boolean isPartial, CharSequence text, int pixel) {
      super(range, isPartial, text);
      this.pixel = pixel;
    }

    public SizeFormattingInfo(
        Pair<Integer, Integer> range, boolean isPartial, CharSequence text, float percentage) {
      super(range, isPartial, text);
      this.percentage = percentage;
    }

    @Override
    public FormattingInfo newInstance() {
      if (pixel > 0) {
        return new SizeFormattingInfo(range, isPartial, text, pixel);
      } else {
        return new SizeFormattingInfo(range, isPartial, text, percentage);
      }
    }

    @Override
    public boolean isValid() {
      return pixel > 0 || percentage > 0;
    }

    @Nullable
    @Override
    CharSequence getFormattingDescriptionInternal(Context context) {
      if (percentage > 0) {
        return percentage > 1
            ? context.getString(R.string.format_size_bigger)
            : context.getString(R.string.format_size_smaller);
      }
      if (pixel > 0) {
        return context.getString(R.string.format_size_in_px, pixel);
      }
      return null;
    }

    @Override
    public int hashCode() {
      return super.hashCode()
          ^ SizeFormattingInfo.class.hashCode()
          ^ Integer.hashCode(pixel)
          ^ Float.hashCode(percentage);
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof SizeFormattingInfo other)) {
        return false;
      }
      return pixel == other.pixel && percentage == other.percentage && super.equals(o);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          SizeFormattingInfo.class.getSimpleName(),
          StringBuilderUtils.optionalInt("pixel", pixel, 0),
          StringBuilderUtils.optionalNum("percentage", percentage, 0.0f),
          super.toString());
    }
  }

  /** The information of a font family formatting. */
  @VisibleForTesting
  static class FontFamilyFormattingInfo extends FormattingInfo {
    private final String family;

    public FontFamilyFormattingInfo(
        Pair<Integer, Integer> range, boolean isPartial, CharSequence text, String family) {
      super(range, isPartial, text);
      this.family = family;
    }

    @Override
    public FormattingInfo newInstance() {
      return new FontFamilyFormattingInfo(range, isPartial, text, family);
    }

    @Override
    public boolean isValid() {
      return !TextUtils.isEmpty(family);
    }

    @Override
    CharSequence getFormattingDescriptionInternal(Context context) {
      return context.getString(R.string.format_font_family, family);
    }

    @Override
    public int hashCode() {
      return super.hashCode() ^ FontFamilyFormattingInfo.class.hashCode() ^ family.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object o) {
      if (o == this) {
        return true;
      }
      if (!(o instanceof FontFamilyFormattingInfo other)) {
        return false;
      }
      return family.equals(other.family) && super.equals(o);
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          FontFamilyFormattingInfo.class.getSimpleName(),
          StringBuilderUtils.optionalText("family", family),
          super.toString());
    }
  }
}
