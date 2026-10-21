/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.text.TextUtils;

/** Utility class to copy text to clipboard. */
public final class ClipboardUtils {

  /**
   * Copies {@code text} to the clipboard.
   *
   * @return false if the given text is null or empty.
   */
  public static boolean copyToClipboard(Context context, CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return false;
    }

    final ClipboardManager clipboard =
        (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
    return copyToClipboard(clipboard, text);
  }

  /**
   * Copies {@code text} to the clipboard.
   *
   * @return false if the given text is null or empty.
   */
  public static boolean copyToClipboard(ClipboardManager clipboard, CharSequence text) {
    if (TextUtils.isEmpty(text)) {
      return false;
    }

    ClipData clipData = ClipData.newPlainText(null, text);
    clipboard.setPrimaryClip(clipData);
    return true;
  }
}
