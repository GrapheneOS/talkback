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
package com.google.android.accessibility.talkback.compositor;

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
import static com.google.common.base.Ascii.toLowerCase;
import static java.lang.Character.isUpperCase;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.utils.EmojiUtils;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.android.accessibility.utils.SpannableUtils.SourceTextSpan;
import com.google.android.accessibility.utils.output.SpeechCleanupUtils;
import com.google.android.accessibility.utils.output.TextFormattingUtils;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utils class that provides common methods for compositor to handle events and TTS output. */
public final class CompositorUtils {

  // Parameters used in join statement.
  public static final boolean PRUNE_EMPTY = true;
  private static final String SEPARATOR_COMMA = ", ";
  private static final String SEPARATOR_PERIOD = ". ";
  private static String separator = SEPARATOR_COMMA;

  private CompositorUtils() {}

  public static String getSeparator() {
    return separator;
  }

  public static void usePeriodAsSeparator() {
    separator = SEPARATOR_PERIOD;
  }

  public static CharSequence joinCharSequences(@Nullable CharSequence... list) {
    List<CharSequence> arrayList = new ArrayList<>(list.length);
    for (CharSequence charSequence : list) {
      if (charSequence != null) {
        arrayList.add(charSequence);
      }
    }
    return joinCharSequences(arrayList, separator, PRUNE_EMPTY);
  }

  public static CharSequence joinCharSequences(
      List<CharSequence> values, @Nullable CharSequence separator, boolean pruneEmpty) {
    SpannableStringBuilder builder = new SpannableStringBuilder();
    boolean first = true;
    for (CharSequence value : values) {
      if (!pruneEmpty || !TextUtils.isEmpty(value)) {
        if (separator != null) {
          if (first) {
            first = false;
          } else {
            // We have to wrap each separator with a different span, because a single span object
            // can only be used once in a CharSequence.
            builder.append(SpannableUtils.wrapWithIdentifierSpan(separator));
          }
        }
        builder.append(value);
      }
    }
    return builder;
  }

  /** Joins the unique char sequence texts of the input values to one char sequence. */
  public static CharSequence dedupJoin(
      CharSequence value1, CharSequence value2, CharSequence value3) {
    CharSequence[] values = {value1, value2, value3};
    SpannableStringBuilder builder = new SpannableStringBuilder();
    HashSet<String> uniqueValues = new HashSet<>();
    boolean first = true;
    for (CharSequence value : values) {
      if (TextUtils.isEmpty(value)) {
        continue;
      }
      String lvalue = toLowerCase(value.toString());
      if (uniqueValues.contains(lvalue)) {
        continue;
      }
      uniqueValues.add(lvalue);
      if (first) {
        first = false;
      } else {
        // We have to wrap each separator with a different span, because a single span object
        // can only be used once in a CharSequence. An IdentifierSpan indicates the text is a
        // separator, and the text will not be announced.
        builder.append(SpannableUtils.wrapWithIdentifierSpan(separator));
      }
      builder.append(value);
    }
    return builder;
  }

  public static CharSequence conditionalPrepend(
      CharSequence prependText, CharSequence conditionalText, CharSequence separator) {
    if (TextUtils.isEmpty(conditionalText)) {
      return "";
    }
    if (TextUtils.isEmpty(prependText)) {
      return conditionalText;
    }
    SpannableStringBuilder result = new SpannableStringBuilder();
    result
        .append(prependText)
        .append(SpannableUtils.wrapWithIdentifierSpan(separator))
        .append(conditionalText);
    return result;
  }

  public static CharSequence conditionalAppend(
      CharSequence conditionalText, CharSequence appendText, CharSequence separator) {
    if (TextUtils.isEmpty(conditionalText)) {
      return "";
    }
    if (TextUtils.isEmpty(appendText)) {
      return conditionalText;
    }
    SpannableStringBuilder result = new SpannableStringBuilder();
    result
        .append(conditionalText)
        .append(SpannableUtils.wrapWithIdentifierSpan(separator))
        .append(appendText);
    return result;
  }

  /** Returns the text that is prepended capital if needed. */
  public static CharSequence prependCapital(CharSequence text, Context context) {
    if (TextUtils.isEmpty(text)) {
      return "";
    } else if (text.length() == 1 && isUpperCase(text.charAt(0))) {
      String capitalizedText = context.getString(R.string.template_capital_letter, text.charAt(0));
      // Copy formatting spans to the new string.
      SpannableString fromSpan = SpannableString.valueOf(text);
      Spannable toSpan = new SpannableStringBuilder(capitalizedText);
      Object[] spans = fromSpan.getSpans(0, 1, Object.class);
      if (spans != null && spans.length > 0) {
        for (Object span : spans) {
          if (TextFormattingUtils.getFormattingSpanClasses().contains(span.getClass())
              || span instanceof SourceTextSpan) {
            toSpan.setSpan(span, 0, toSpan.length(), fromSpan.getSpanFlags(span));
          }
        }
      }
      return toSpan;
    } else {
      return text;
    }
  }

  /**
   * Refines the input text in ways:
   *
   * <ul>
   *   <li>1. Clean up the text.
   *   <li>2. Enhance emoji feedback if needed.
   *   <li>3. Wrap with {@link SourceTextSpan}.
   * </ul>
   */
  public static CharSequence refineInputText(Context context, CharSequence inputText) {
    return refineInputText(context, inputText, /* countRepeatedSymbols= */ false);
  }

  /**
   * Refines the input text in ways:
   *
   * <ul>
   *   <li>1. Clean up the text and count repeated symbols if needed.
   *   <li>2. Enhance emoji feedback if needed.
   *   <li>3. Wrap with {@link SourceTextSpan}.
   * </ul>
   *
   * @param countRepeatedSymbols If the text should count repeated symbols.
   */
  public static CharSequence refineInputText(
      Context context, CharSequence inputText, boolean countRepeatedSymbols) {
    if (TextUtils.isEmpty(inputText)) {
      return "";
    } else {
      CharSequence cleanupText =
          SpeechCleanupUtils.collapseRepeatedCharactersAndCleanUp(
              context, inputText, countRepeatedSymbols);
      return CompositorUtils.enhanceEmojiFeedback(
          context, SpannableUtils.wrapWithSourceTextSpan(cleanupText));
    }
  }

  /** Appends postfix "emoji" to the emoji in text but respect the original TtsSpan. */
  public static CharSequence enhanceEmojiFeedback(Context context, @Nullable CharSequence text) {
    if (!FeatureFlagReader.enableEmojiPostfixFeedback(context)) {
      return text;
    }
    if (TextUtils.isEmpty(text)) {
      return "";
    }
    SpannableString spannable = new SpannableString(text);
    Matcher matcher = EmojiUtils.findEmoji(text);
    while (matcher.find()) {
      int charUnicode = matcher.group().codePointAt(0);
      String unicodeString = new String(Character.toChars(charUnicode));
      String emojiName = EmojiUtils.getEmojiName(context, unicodeString);
      TtsSpan[] ttsSpans = spannable.getSpans(matcher.start(), matcher.end(), TtsSpan.class);
      if (ttsSpans.length == 0) {
        TtsSpan ttsSpan = new TtsSpan.TextBuilder(emojiName).build();
        spannable.setSpan(ttsSpan, matcher.start(), matcher.end(), SPAN_EXCLUSIVE_EXCLUSIVE);
      }
    }
    return spannable;
  }
}
