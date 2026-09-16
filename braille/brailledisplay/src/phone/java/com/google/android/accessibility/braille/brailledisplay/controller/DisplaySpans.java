/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.android.accessibility.braille.brailledisplay.controller;

import static com.google.android.accessibility.braille.common.translate.EditBufferUtils.NO_CURSOR;

import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Range;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/** Static utilities for text spans that control how text is displayed on the braille display. */
public class DisplaySpans {
  private static final String TAG = "DisplaySpans";

  /** Marks a text selection or cursor on the display. */
  private static class SelectionSpan extends DisplaySpans {}

  /** Marks a progress bar on the display. */
  private static class ProgressBarSpan extends DisplaySpans {}

  /**
   * Marks a portion of {@code spannable} as containing text selection. If {@code start} and {@code
   * end} are equal, then then added span marks a cursor.
   */
  public static void addSelection(Spannable spannable, int start, int end) {
    int flags;
    if (start == end) {
      flags = Spanned.SPAN_EXCLUSIVE_INCLUSIVE;
    } else {
      flags = Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
    }
    // If start and end are out of order... swap start and end. Required by setSpan().
    if (end < start) {
      int oldStart = start;
      start = end;
      end = oldStart;
    }
    spannable.setSpan(new SelectionSpan(), start, end, flags);
  }

  /** Marks a portion of {@code spannable} as containing progress bar. */
  public static void addProgressBar(Spannable spannable, int start, int end) {
    int flags = start == end ? Spanned.SPAN_EXCLUSIVE_INCLUSIVE : Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;
    // If start and end are out of order... swap start and end. Required by setSpan().
    if (end < start) {
      int oldStart = start;
      start = end;
      end = oldStart;
    }
    spannable.setSpan(new ProgressBarSpan(), start, end, flags);
  }

  /** Finds the range of selection in the given text. */
  public static Range<Integer> findSelectionRange(CharSequence text) {
    return findRange(text, SelectionSpan.class);
  }

  /** Finds the range of progress bar in the given text. */
  public static Range<Integer> findProgressBarRange(CharSequence text) {
    return findRange(text, ProgressBarSpan.class);
  }

  /**
   * Marks the whole of {@code spannable} as containing the content coming from {@code node}. A copy
   * of {@code node} is stored.
   */
  public static void setAccessibilityNode(Spannable spannable, AccessibilityNodeInfoCompat node) {
    spannable.setSpan(node, 0, spannable.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  /**
   * Finds the shortest accessibility node span that overlaps {@code position} in {@code chars}. If
   * a node is found, it is returned, otherwise {@code null} is returned.
   */
  @Nullable
  public static AccessibilityNodeInfoCompat getAccessibilityNodeFromPosition(
      int position, CharSequence chars) {
    if (!(chars instanceof Spanned)) {
      return null;
    }
    Spanned spanned = (Spanned) chars;
    AccessibilityNodeInfoCompat[] spans =
        spanned.getSpans(position, position, AccessibilityNodeInfoCompat.class);
    if (spans.length == 0) {
      return null;
    }
    AccessibilityNodeInfoCompat found = spans[0];
    int foundLength = spanned.getSpanEnd(found) - spanned.getSpanStart(found);
    for (int i = 1; i < spans.length; ++i) {
      AccessibilityNodeInfoCompat span = spans[i];
      int length = spanned.getSpanEnd(span) - spanned.getSpanStart(span);
      if (length < foundLength) {
        found = span;
        foundLength = length;
      }
    }
    return found;
  }

  /** Finds the range of specified display spans class in the given text. */
  private static Range<Integer> findRange(CharSequence text, Class<? extends DisplaySpans> spans) {
    Spanned spanned = SpannableString.valueOf(text);
    if (spanned != null) {
      DisplaySpans[] displaySpans = spanned.getSpans(0, spanned.length(), spans);
      if (displaySpans.length > 0) {
        return new Range<>(
            spanned.getSpanStart(displaySpans[0]), spanned.getSpanEnd(displaySpans[0]));
      }
    }
    return new Range<>(NO_CURSOR, NO_CURSOR);
  }
}
