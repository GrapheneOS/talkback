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

import static com.google.android.accessibility.utils.StringBuilderUtils.DEFAULT_SEPARATOR;

import android.text.Spannable;
import android.text.TextUtils;

/** Utility class for word formatting. */
public final class WordUtils {
  /** Returns true if spanned in CJKV(Chinese, Japanese, Korean and Vietnamese) or word boundary. */
  public static boolean isSpannedFullWord(Spannable spannable, int spanStart, int spanEnd) {
    if (spanStart < 0 || spanStart >= spanEnd || spanEnd > spannable.length()) {
      return false;
    }
    char first = spannable.charAt(spanStart);
    char last = spannable.charAt(spanEnd - 1);
    char previous = (spanStart == 0) ? ' ' : spannable.charAt(spanStart - 1);
    char next = (spanEnd == spannable.length()) ? ' ' : spannable.charAt(spanEnd);
    if ((isCharacterInWord(first) && isCharacterInWord(previous))
        || (isCharacterInWord(last) && isCharacterInWord(next))) {
      return false;
    }
    return true;
  }

  /** Returns true if the character is in a word. */
  private static boolean isCharacterInWord(char ch) {
    if (Character.isIdeographic(ch)) {
      return false;
    }
    // @ is not a letter or digit, but it is commonly used in email addresses.
    return Character.isLetterOrDigit(ch) || ch == '@';
  }

  /** Returns true if the string is a single word. */
  public static boolean isSingleWord(String string) {
    if (TextUtils.isEmpty(string) || string.contains(DEFAULT_SEPARATOR)) {
      return false;
    }
    if (string.length() > 1) {
      for (int i = 0; i < string.length(); i++) {
        char ch = string.charAt(i);
        if (Character.isIdeographic(ch)) {
          return false;
        }
      }
    }
    return true;
  }

  private WordUtils() {}
}
