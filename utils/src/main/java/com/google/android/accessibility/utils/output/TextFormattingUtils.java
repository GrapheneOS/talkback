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

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
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
import com.google.android.accessibility.utils.SpannableUtils.SpannableWithOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Utility class for text formatting. */
public final class TextFormattingUtils {
  private static final String TAG = "TextFormattingUtils";

  public static final int OPTION_NONE = 0;
  public static final int OPTION_BOLD = 1 << 0;
  public static final int OPTION_ITALIC = 1 << 2;
  public static final int OPTION_UNDERLINE = 1 << 3;
  public static final int OPTION_STRIKETHROUGH = 1 << 4;
  public static final int OPTION_FONT_COLOR_NAME = 1 << 5;
  public static final int OPTION_FONT_COLOR_HEX = 1 << 6;
  public static final int OPTION_FONT_SIZE = 1 << 7;
  public static final int OPTION_FONT_FAMILY = 1 << 8;

  public static final int FEEDBACK_MODE_NONE = 0;
  public static final int FEEDBACK_MODE_SPEECH = 1 << 0;
  public static final int FEEDBACK_MODE_SOUND = 1 << 1;
  public static final int FEEDBACK_MODE_SPEECH_SOUND = FEEDBACK_MODE_SPEECH | FEEDBACK_MODE_SOUND;

  /** The list of span classes that are supported for all text formatting. */
  public static final List<Class<? extends CharacterStyle>> getFormattingSpanClasses() {
    List<Class<? extends CharacterStyle>> spanClasses = new ArrayList<>();
    spanClasses.add(StyleSpan.class);
    spanClasses.add(UnderlineSpan.class);
    spanClasses.add(StrikethroughSpan.class);
    spanClasses.add(ForegroundColorSpan.class);
    spanClasses.add(BackgroundColorSpan.class);
    spanClasses.add(RelativeSizeSpan.class);
    spanClasses.add(AbsoluteSizeSpan.class);
    spanClasses.add(TypefaceSpan.class);
    spanClasses.add(TextAppearanceSpan.class);
    return spanClasses;
  }

  /** The list of span classes that are supported for text formatting options. */
  public static final Set<Class<?>> getFormattingSpanClasses(int options) {
    Set<Class<?>> spanClasses = new HashSet<>();
    if ((options & OPTION_BOLD) != 0 || (options & OPTION_ITALIC) != 0) {
      spanClasses.add(StyleSpan.class);
      spanClasses.add(TextAppearanceSpan.class);
    }
    if ((options & OPTION_UNDERLINE) != 0) {
      spanClasses.add(UnderlineSpan.class);
    }
    if ((options & OPTION_STRIKETHROUGH) != 0) {
      spanClasses.add(StrikethroughSpan.class);
    }
    if ((options & OPTION_FONT_COLOR_NAME) != 0 || (options & OPTION_FONT_COLOR_HEX) != 0) {
      spanClasses.add(ForegroundColorSpan.class);
      spanClasses.add(BackgroundColorSpan.class);
      spanClasses.add(TextAppearanceSpan.class);
    }
    if ((options & OPTION_FONT_SIZE) != 0) {
      spanClasses.add(RelativeSizeSpan.class);
      spanClasses.add(AbsoluteSizeSpan.class);
      spanClasses.add(TextAppearanceSpan.class);
    }
    if ((options & OPTION_FONT_FAMILY) != 0) {
      spanClasses.add(TypefaceSpan.class);
      spanClasses.add(TextAppearanceSpan.class);
    }
    return spanClasses;
  }

  /** Checks if the span is enabled by the options. */
  public static boolean isSpanMatchingOptions(Object span, int options) {
    if (span instanceof StyleSpan styleSpan) {
      return ((options & OPTION_BOLD) != 0 && (styleSpan.getStyle() & Typeface.BOLD) != 0)
          || ((options & OPTION_ITALIC) != 0 && (styleSpan.getStyle() & Typeface.ITALIC) != 0);
    } else if (span instanceof UnderlineSpan && (options & OPTION_UNDERLINE) != 0) {
      return true;
    } else if (span instanceof StrikethroughSpan && (options & OPTION_STRIKETHROUGH) != 0) {
      return true;
    } else if ((span instanceof ForegroundColorSpan || span instanceof BackgroundColorSpan)
        && ((options & OPTION_FONT_COLOR_NAME) != 0 || (options & OPTION_FONT_COLOR_HEX) != 0)) {
      return true;
    } else if ((span instanceof RelativeSizeSpan || span instanceof AbsoluteSizeSpan)
        && (options & OPTION_FONT_SIZE) != 0) {
      return true;
    } else if (span instanceof TypefaceSpan typefaceSpan) {
      return ((options & OPTION_FONT_FAMILY) != 0 && !TextUtils.isEmpty(typefaceSpan.getFamily()));
    } else if (span instanceof TextAppearanceSpan textAppearanceSpan) {
      return ((options & OPTION_BOLD) != 0
              && (textAppearanceSpan.getTextStyle() & Typeface.BOLD) != 0)
          || ((options & OPTION_ITALIC) != 0
              && (textAppearanceSpan.getTextStyle() & Typeface.ITALIC) != 0)
          || (((options & OPTION_FONT_COLOR_NAME) != 0 || (options & OPTION_FONT_COLOR_HEX) != 0)
              && textAppearanceSpan.getTextColor() != null
              && textAppearanceSpan.getTextColor().getDefaultColor() != 0)
          || ((options & OPTION_FONT_SIZE) != 0 && textAppearanceSpan.getTextSize() != -1)
          || ((options & OPTION_FONT_FAMILY) != 0
              && !TextUtils.isEmpty(textAppearanceSpan.getFamily()));
    }
    return false;
  }

  /** Generates the summary feedback for all text formatting. */
  public static CharSequence getTextFormattingSummary(
      Context context, List<SpannableWithOffset> spannableStrings) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    for (SpannableWithOffset spannableWithOffset : spannableStrings) {
      if (spannableWithOffset == null || spannableWithOffset.spannableString == null) {
        continue;
      }
      SpannableString spannable = spannableWithOffset.spannableString;
      final CharacterStyle[] spans =
          spannable.getSpans(0, spannable.length(), CharacterStyle.class);
      if ((spans == null) || (spans.length == 0)) {
        continue;
      }
      TextFormattingInfo textFormattingSummary =
          new TextFormattingInfo(
              context,
              Arrays.asList(spans),
              spannable,
              TextFormattingUtils.OPTION_BOLD
                  | TextFormattingUtils.OPTION_ITALIC
                  | TextFormattingUtils.OPTION_UNDERLINE
                  | TextFormattingUtils.OPTION_STRIKETHROUGH
                  | TextFormattingUtils.OPTION_FONT_COLOR_NAME
                  | TextFormattingUtils.OPTION_FONT_COLOR_HEX
                  | TextFormattingUtils.OPTION_FONT_SIZE
                  | TextFormattingUtils.OPTION_FONT_FAMILY);
      CharSequence formattingPositionFeedback =
          textFormattingSummary.getFormattingPositionFeedback(context, spannableWithOffset.offset);
      if (!TextUtils.isEmpty(formattingPositionFeedback)) {
        builder.append(formattingPositionFeedback);
        builder.append(DEFAULT_BREAKING_SEPARATOR);
      }
    }
    if (builder.length() > 0) {
      builder.delete(builder.length() - DEFAULT_BREAKING_SEPARATOR.length(), builder.length());
    }
    return builder;
  }

  private TextFormattingUtils() {}
}
