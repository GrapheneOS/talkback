/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils;

import static com.google.android.accessibility.utils.StringBuilderUtils.DEFAULT_SEPARATOR;

import android.os.LocaleList;
import android.os.PersistableBundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.SpannedString;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.text.style.URLSpan;
import android.util.Log;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utility methods for working with spannable objects. */
public final class SpannableUtils {

  private static final String TAG = "SpannableUtils";

  /** Identifies separators attached in spoken feedback. */
  public static class IdentifierSpan {}

  /** Marks in spoken feedback. */
  public static class NonCopyableTextSpan {}

  /**
   * Identifies the visible text from the composed feedback for text formatting, e.g. it can
   * identify the text like "Airplane mode" from the composed string "Off, Airplane mode, Switch,
   * ... Double Tap to toggle".
   */
  public static class SourceTextSpan {}

  /** A class to hold the spannable string and its offset in the grouping focus. */
  public static class SpannableWithOffset {
    public final SpannableString spannableString;
    public final int offset;

    public SpannableWithOffset(SpannableString spannableString, int offset) {
      this.spannableString = spannableString;
      this.offset = offset;
    }
  }

  public static CharSequence wrapWithIdentifierSpan(CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return text;
    }
    SpannableString spannedText = new SpannableString(text);
    spannedText.setSpan(
        new SpannableUtils.IdentifierSpan(),
        /* start= */ 0,
        /* end= */ text.length(),
        /* flags= */ 0);
    return spannedText;
  }

  public static CharSequence wrapWithNonCopyableTextSpan(CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return text;
    }
    SpannableString spannedText = new SpannableString(text);
    spannedText.setSpan(
        new SpannableUtils.NonCopyableTextSpan(),
        /* start= */ 0,
        /* end= */ text.length(),
        /* flags= */ Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannedText;
  }

  /** Wraps the text with {@link SourceTextSpan} to indicate the text is from the source. */
  public static CharSequence wrapWithSourceTextSpan(CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return text;
    }
    SpannableString spannedText = new SpannableString(text);
    spannedText.setSpan(
        new SpannableUtils.SourceTextSpan(),
        /* start= */ 0,
        /* end= */ text.length(),
        /* flags= */ Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannedText;
  }

  /**
   * Separates text segments by {@link SpannableUtils.IdentifierSpan}, removes text segments wrapped
   * with {@link SpannableUtils.NonCopyableTextSpan}, and reconstructs the copyable text result.
   *
   * @param text Original text sequence that might contain non-copyable components
   * @return Text without non-copyable components and with a space between each text segment
   */
  public static CharSequence getCopyableText(CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return text;
    }

    Queue<CharSequence> queuedCopyableTextSegments = new ArrayDeque<>();
    SpannableString spannable = new SpannableString(text);

    int textStart = 0;
    int textEnd = 0;

    while (textEnd >= 0 && textEnd < text.length()) {
      // The non-identifier text ends at the begin index of next IdentifierSpan-wrapped object
      textEnd =
          spannable.nextSpanTransition(
              textStart, text.length(), SpannableUtils.IdentifierSpan.class);

      CharSequence textSegment = text.subSequence(textStart, textEnd);
      if (!TextUtils.isEmpty(textSegment)
          && !SpannableUtils.isWrappedWithTargetSpan(
              textSegment, SpannableUtils.NonCopyableTextSpan.class, false)) {
        queuedCopyableTextSegments.offer(textSegment);
      }

      // Since textEnd itself is always wrapped with IdentifierSpan, we start to search for the
      // begin of non-identifier text from the next character of textEnd.
      textStart = textEnd + 1;
      while (textStart < text.length()
          && SpannableUtils.isWrappedWithTargetSpan(
              text.subSequence(textStart, textStart + 1),
              SpannableUtils.IdentifierSpan.class,
              false)) {
        textStart += 1;
      }
    }

    // Combine copyable text segments with space separators
    SpannableStringBuilder copyableText = new SpannableStringBuilder("");
    CharSequence textSegment = queuedCopyableTextSegments.poll();
    boolean first = true;
    while (textSegment != null) {
      if (first) {
        first = false;
      } else {
        copyableText.append(DEFAULT_SEPARATOR);
      }
      copyableText.append(textSegment);
      textSegment = queuedCopyableTextSegments.poll();
    }

    return copyableText;
  }

  /**
   * Returns true if the text contains the target span.
   *
   * @param text The text to check.
   * @param spanClass The class of the span to check.
   * @param shouldTrim Whether to trim the text before checking.
   */
  public static <T> boolean hasTargetSpan(
      CharSequence text, Class<T> spanClass, boolean shouldTrim) {
    if (TextUtils.isEmpty(text) || !(text instanceof Spannable)) {
      return false;
    }
    if (shouldTrim) {
      text = trimText(text);
    }
    if (TextUtils.isEmpty(text)) {
      return false;
    }
    Spannable spannable = (Spannable) text;
    T[] spans = spannable.getSpans(0, text.length(), spanClass);
    if ((spans == null) || (spans.length == 0)) {
      return false;
    }

    return true;
  }

  /**
   * Returns true if the text is fully wrapped with the target span.
   *
   * @param text The text to check.
   * @param spanClass The class of the span to check.
   * @param shouldTrim Whether to trim the text before checking.
   */
  public static <T> boolean isWrappedWithTargetSpan(
      CharSequence text, Class<T> spanClass, boolean shouldTrim) {
    if (TextUtils.isEmpty(text) || !(text instanceof Spannable)) {
      return false;
    }
    if (shouldTrim) {
      text = trimText(text);
    }
    if (TextUtils.isEmpty(text)) {
      return false;
    }
    Spannable spannable = (Spannable) text;
    T[] spans = spannable.getSpans(0, text.length(), spanClass);
    if ((spans == null) || (spans.length != 1)) {
      return false;
    }

    T span = spans[0];
    return (spannable.getSpanStart(span) == 0)
        && (spannable.getSpanEnd(span) == spannable.length());
  }

  // Avoid using String.trim() so that Span info is not lost. Use this method for CharSequence trim.
  public static CharSequence trimText(CharSequence text) {
    int start = 0;
    int last = text.length() - 1;
    while ((start <= last) && Character.isWhitespace(text.charAt(start))) {
      start++;
    }

    while ((last > start) && Character.isWhitespace(text.charAt(last))) {
      last--;
    }
    return text.subSequence(start, (last + 1));
  }

  /**
   * Retrieves SpannableString containing the target type of {@link CharacterStyle} subclasses, i.e.
   * ClickableSpan in the accessibility node.
   *
   * <p><b>Note: Only spans in text, not in content description, can be passed to accessibility
   * service.</b>
   *
   * <p><b>Note: {@code spanClasses} should be able to be parcelable and transmitted by IPC which
   * depends on the implementation of {@link AccessibilityNodeInfo#setText(CharSequence)} in the
   * framework side.</b>
   *
   * @param node the AccessibilityNodeInfoCompat where the text comes from
   * @param spanClasses a list of {@link CharacterStyle} subclasses that is parcelable.
   * @return SpannableString with at least 1 {@link CharacterStyle} subclasses, null if not found in
   *     the node
   */
  public static @Nullable SpannableString getSpannableStringWithCharacterStyle(
      AccessibilityNodeInfoCompat node, List<Class<? extends CharacterStyle>> spanClasses) {

    CharSequence text = AccessibilityNodeInfoUtils.getText(node);
    if (isEmptyOrNotSpannableStringType(text)) {
      LogUtils.v(TAG, "text(%s) isEmptyOrNotSpannableStringType", text);
      return null;
    }

    SpannableString spannable = SpannableString.valueOf(text);
    CharacterStyle[] spans = spannable.getSpans(0, spannable.length(), CharacterStyle.class);
    if (spans != null) {
      for (CharacterStyle span : spans) {
        for (Class<? extends CharacterStyle> spanClass : spanClasses) {
          if (ClassLoadingCache.checkInstanceOf(span.getClass().getName(), spanClass)) {
            return spannable;
          }
        }
      }
    }
    LogUtils.v(TAG, "text(%s) has null or empty parcelableSpans[%s]", text, spanClasses);
    return null;
  }

  /**
   * Strip out all the spans of target span class from the given text.
   *
   * @param text Text to remove span.
   * @param spanClass class of span to be removed.
   */
  public static <T> void stripTargetSpanFromText(CharSequence text, Class<T> spanClass) {
    if (TextUtils.isEmpty(text) || !(text instanceof SpannableString spannable)) {
      return;
    }
    T[] spans = spannable.getSpans(0, spannable.length(), spanClass);
    if (spans != null) {
      for (T span : spans) {
        if (span != null) {
          spannable.removeSpan(span);
        }
      }
    }
  }

  /**
   * Logs the type, position and args of spans which attach to given text, but only if log priority
   * is equal to Log.VERBOSE. Format is {type 'spanned text' extra-data} {type 'other text'
   * extra-data} ..."
   *
   * @param text Text to be logged.
   * @param spanClass class of span to be logged.
   */
  public static <T> @Nullable String spansToStringForLogging(
      CharSequence text, Class<T> spanClass) {
    if (!LogUtils.shouldLog(Log.VERBOSE)) {
      return null;
    }

    if (isEmptyOrNotSpannableStringType(text)) {
      return null;
    }

    Spanned spanned = (Spanned) text;
    T[] spans = spanned.getSpans(0, spanned.length(), spanClass);
    if (spans.length == 0) {
      return null;
    }

    StringBuilder stringBuilder = new StringBuilder();
    for (T span : spans) {
      stringBuilder.append("{");
      // Span type.
      stringBuilder.append(span.getClass().getSimpleName());

      // Span text.
      int start = spanned.getSpanStart(span);
      int end = spanned.getSpanEnd(span);
      if (start < 0 || end < 0 || start == end) {
        stringBuilder.append(" invalid index:[");
        stringBuilder.append(start);
        stringBuilder.append(",");
        stringBuilder.append(end);
        stringBuilder.append("]}");
        continue;
      } else {
        stringBuilder.append(" '");
        stringBuilder.append(spanned, start, end);
        stringBuilder.append("'");
      }

      // Extra data.
      if (span instanceof LocaleSpan localeSpan) {
        LocaleList localeList = localeSpan.getLocales();
        int size = localeList.size();
        if (size > 0) {
          stringBuilder.append(" locale=[");
          for (int i = 0; i < size - 1; i++) {
            stringBuilder.append(localeList.get(i));
            stringBuilder.append(",");
          }
          stringBuilder.append(localeList.get(size - 1));
          stringBuilder.append("]");
        }

      } else if (span instanceof TtsSpan ttsSpan) {
        stringBuilder.append(" ttsType=");
        stringBuilder.append(ttsSpan.getType());
        PersistableBundle bundle = ttsSpan.getArgs();
        Set<String> keys = bundle.keySet();
        if (!keys.isEmpty()) {
          for (String key : keys) {
            stringBuilder.append(" ");
            stringBuilder.append(key);
            stringBuilder.append("=");
            stringBuilder.append(bundle.get(key));
          }
        }
      } else if (span instanceof URLSpan urlSpan) {
        stringBuilder.append(" url=");
        stringBuilder.append(urlSpan.getURL());
      } else {
        // For other spans, just print the span itself.
        stringBuilder.append(span);
      }
      stringBuilder.append("}");
    }
    return stringBuilder.toString();
  }

  private static boolean isEmptyOrNotSpannableStringType(CharSequence text) {
    return TextUtils.isEmpty(text)
        || !(text instanceof SpannedString
            || text instanceof SpannableString
            || text instanceof SpannableStringBuilder);
  }

  /**
   * Creates CharSequence from {@code templateString} and its {@code parameters}. And spans in
   * parameters are keep in result.
   *
   * @param templateString template string that may contains parameters with spans.
   * @param parameters object arrays that are supposed but not necessary to be Spanned. If it is
   *     Spanned, the spans are keep in result Spannable.
   * @return CharSequence that composed by formatted template string and parameters.
   */
  @SuppressWarnings("AnnotateFormatMethod")
  public static CharSequence getSpannedFormattedString(
      String templateString, Object... parameters) {
    List<CharSequence> stringTypeList = new ArrayList<>();
    for (Object param : parameters) {
      if (param instanceof CharSequence spannedParams) {
        stringTypeList.add(spannedParams);
      }
    }

    String formattedString = String.format(templateString, parameters);
    if (stringTypeList.isEmpty()) {
      LogUtils.v(TAG, "getSpannedFormattedString return original string");
      return formattedString;
    }

    String expandableTemplate = toExpandableTemplate(templateString, parameters);
    try {
      // It will throw IllegalArgumentException if the template requests a value that was not
      // provided, or if more than 9 values are provided.
      return TextUtils.expandTemplate(
          expandableTemplate, stringTypeList.toArray(new CharSequence[stringTypeList.size()]));
    } catch (IllegalArgumentException exception) {
      // This is a fall-back method that may copy spans inaccurately
      LogUtils.e(TAG, "TextUtils.expandTemplate failed, e=%s", exception);
      return copySpansFromTemplateParameters(
          formattedString, stringTypeList.toArray(new CharSequence[stringTypeList.size()]));
    }
  }

  /**
   * Creates CharSequence from template string by its parameters. The template string will be
   * transformed to contain "^1"-style placeholder values dynamically to match the format of {@link
   * TextUtils#expandTemplate(CharSequence, CharSequence...)} and formatted by other none-string
   * type parameters.
   *
   * @param templateString template string that may contains parameters with strings.
   * @param parameters object arrays that are supposed but not necessary to be string. If it is
   *     string, the corresponding placeholder value will be changed to "^1"-style. If not string
   *     type, the placeholder is kept and adjust the index.
   * @return CharSequence that composed by template string with "^1"-style placeholder values.
   */
  @VisibleForTesting
  static String toExpandableTemplate(String templateString, Object[] parameters) {
    String expandTemplateString = templateString;
    List<Object> otherTypeList = new ArrayList<>();

    int spanTypeIndex = 1;
    int otherTypeIndex = 1;
    for (int i = 1; i <= parameters.length; i++) {
      Object param = parameters[i - 1];
      if (param instanceof CharSequence) {
        // replaces string type "%1$s" or "%s" to "^1" and so on.
        if (expandTemplateString.contains("%" + i + "$s")) {
          expandTemplateString =
              expandTemplateString.replace(("%" + i + "$s"), ("^" + spanTypeIndex));
        } else if (expandTemplateString.contains("%s")) {
          expandTemplateString = expandTemplateString.replaceFirst("%s", ("^" + spanTypeIndex));
        }
        spanTypeIndex++;
      } else {
        // keeps and assigns correct index to other type parameters
        expandTemplateString = expandTemplateString.replace(("%" + i), ("%" + otherTypeIndex));
        otherTypeList.add(param);
        otherTypeIndex++;
      }
    }
    return String.format(expandTemplateString, otherTypeList.toArray());
  }

  /**
   * Creates spannable from text that includes some Spanned. If a template parameter occurs multiple
   * times in the final text, this function copies the parameter's spans to the first instance.
   *
   * @param text some text that potentially contains CharSequence parameters.
   * @param templateParameters CharSequence arrays that contains spans and need to be copied to
   *     result Spannable.
   * @return Spannable object that contains incoming text and spans from templateParameters.
   */
  private static Spannable copySpansFromTemplateParameters(
      String text, CharSequence[] templateParameters) {
    SpannableString result = new SpannableString(text);
    for (CharSequence params : templateParameters) {
      if (params instanceof Spanned spannedParams) {
        int index = text.indexOf(spannedParams.toString());
        if (index >= 0) {
          copySpans(result, spannedParams, index);
        }
      }
    }
    return result;
  }

  /**
   * Utility that copies spans from {@code fromSpan} to {@code toSpan}.
   *
   * @param toSpan Spannable that is supposed to contain fromSpan.
   * @param fromSpan Spannable that could contain spans that would be copied to toSpan.
   * @param toSpanStartIndex Starting index of occurrence fromSpan in toSpan.
   */
  private static void copySpans(Spannable toSpan, Spanned fromSpan, int toSpanStartIndex) {
    if (toSpanStartIndex < 0 || toSpanStartIndex >= toSpan.length()) {
      LogUtils.e(
          TAG,
          "startIndex parameter (%d) is out of toSpan length %d",
          toSpanStartIndex,
          toSpan.length());
      return;
    }

    Object[] spans = fromSpan.getSpans(0, fromSpan.length(), Object.class);
    if (spans != null && spans.length > 0) {
      for (Object span : spans) {
        int spanStartIndex = fromSpan.getSpanStart(span);
        int spanEndIndex = fromSpan.getSpanEnd(span);
        if (spanStartIndex >= spanEndIndex) {
          continue;
        }
        int spanFlags = fromSpan.getSpanFlags(span);
        toSpan.setSpan(
            span,
            (toSpanStartIndex + spanStartIndex),
            (toSpanStartIndex + spanEndIndex),
            spanFlags);
      }
    }
  }

  private SpannableUtils() {}
}
