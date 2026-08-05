/*
 * Copyright (C) 2024 The Android Open Source Project
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

import static com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.BASE_CLICKABLE_SPAN;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.SpannableUtils.SpannableWithOffset;
import com.google.android.accessibility.utils.traversal.SpannableTraversalUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;

/** Utility class to handle the native link. */
public final class LinkUtils {
  private static final String TAG = "LinkUtils";

  /** Returns the link spans in the node group. */
  public static List<LinkSpan> getLinkSpansInNodeGroup(AccessibilityNodeInfoCompat node) {
    final List<SpannableWithOffset> spannableStrings = new ArrayList<>();
    SpannableTraversalUtils.getSpannableStringsWithTargetClickableSpanInNodeTree(
        node, BASE_CLICKABLE_SPAN, spannableStrings);

    final List<LinkSpan> linkSpans = new ArrayList<>();
    for (SpannableWithOffset spannableWithOffset : spannableStrings) {
      if (spannableWithOffset == null || spannableWithOffset.spannableString == null) {
        continue;
      }
      SpannableString spannable = spannableWithOffset.spannableString;
      final ClickableSpan[] spans = spannable.getSpans(0, spannable.length(), BASE_CLICKABLE_SPAN);
      if ((spans == null) || (spans.length == 0)) {
        continue;
      }
      for (int i = 0; i < spans.length; i++) {
        final ClickableSpan span = spans[i];
        if (span == null) {
          continue;
        }
        final int start = spannable.getSpanStart(span);
        final int end = spannable.getSpanEnd(span);
        if (start < 0 || end < 0) {
          continue;
        }
        final CharSequence label = spannable.subSequence(start, end);
        if (TextUtils.isEmpty(label)) {
          continue;
        }
        linkSpans.add(new LinkSpan(span, label));
      }
    }
    return linkSpans;
  }

  /** Returns the link text at the index in the node group. */
  public static String getLinkText(AccessibilityNodeInfoCompat node, int index) {
    final List<LinkSpan> linkSpans = getLinkSpansInNodeGroup(node);
    if (linkSpans.isEmpty() || index < 0 || index >= linkSpans.size()) {
      return "";
    }
    return linkSpans.get(index).label.toString();
  }

  /** Activates the link span. */
  public static boolean activateLinkSpan(Context context, LinkSpan linkSpan) {
    try {
      linkSpan.span.onClick(null);
      return true;
    } catch (RuntimeException e) {
      LogUtils.e(TAG, "Failed to invoke LinkSpan: %s\n%s", linkSpan.span, e);
    }

    if (linkSpan.span instanceof URLSpan) {
      URLSpan urlSpan = (URLSpan) linkSpan.span;
      // Fall back to handle url with Intent of ACTION_VIEW
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlSpan.getURL()));
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      try {
        context.startActivity(intent);
        return true;
      } catch (ActivityNotFoundException e) {
        LogUtils.e(TAG, "Failed to invoke URLSpan: %s\n%s", linkSpan.span, e);
      }
    }
    return false;
  }

  /** A class to store the {@link ClickableSpan} and its label for a link. */
  public static class LinkSpan {
    public ClickableSpan span;
    public CharSequence label;

    public LinkSpan(ClickableSpan span, CharSequence label) {
      this.span = span;
      this.label = label;
    }
  }

  private LinkUtils() {}
}
