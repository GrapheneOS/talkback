/*
 * Copyright (C) 2023 Google Inc.
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

package com.google.android.accessibility.utils;

import android.content.Context;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Utils class handles Emoji representations. */
public class EmojiUtils {
  // TODO: Extends Emoji regex to support more Emojis. Try Character.isEmoji() after
  // upgrade to Java 21.
  public static final String EMOJI_REGEX = "[\uD83C\uDC00-\uD83F\uDFFF]|[\u2600-\u27BF]";
  private static final Pattern EMOJI_REGEX_PATTERN = Pattern.compile(EMOJI_REGEX);

  /** Finds Emoji unicode. */
  public static Matcher findEmoji(CharSequence text) {
    return EMOJI_REGEX_PATTERN.matcher(text);
  }

  /**
   * Gets emoji's short code which is a human readable description.
   *
   * @param emoji an emoji String composed by 2 unicode.
   * @return Emoji short code.
   */
  public static String getEmojiShortCode(String emoji) {
    // TODO: Makes emoji has localization.
    return Character.getName(emoji.codePointAt(0));
  }

  /**
   * Gets emoji's name with the postfix "emoji".
   *
   * @param emoji an emoji String composed by 2 unicode.
   * @return Emoji name.
   */
  public static String getEmojiName(Context context, String emoji) {
    return context.getString(R.string.emoji_name, emoji);
  }

  private EmojiUtils() {}
}
