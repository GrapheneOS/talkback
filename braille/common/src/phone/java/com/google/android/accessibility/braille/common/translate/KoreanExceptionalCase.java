/*
 * Copyright 2024 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.accessibility.braille.common.translate;

import com.google.android.accessibility.braille.interfaces.BrailleWord;

/**
 * Uncontracted korean and Contracted Korean is using same method, but operation is different. So we
 * make an interface so that method will be calling.
 */
public interface KoreanExceptionalCase {
  /**
   * Gets the translated string for the first braille dot.
   *
   * @param fullWord all entered braille characters
   * @param lastDot last dot of all braille
   * @param inputDot entered dot
   * @return a string that is translated from entered braille at first time
   */
  String getStringAtFirstTime(BrailleWord fullWord, String lastDot, String inputDot);

  /**
   * Finds the character as input braille dot about initial or middle or final consonant.
   *
   * @param lastChar last character that is translated from last braille dot
   * @param lastDot last dot of all braille
   * @param inputDot entered dot
   * @return a string according to the preceding letter or Braille
   */
  String getSyllable(String lastChar, String lastDot, String inputDot);

  /**
   * Modifies a character by previous braille dot and previous character.
   *
   * @param current full text that is translated from all entered braille characters
   * @param fullWord all entered braille characters
   * @param lastChar last character that is translated from last braille dot
   * @param lastDot last dot of all braille
   * @param inputDot entered dot
   */
  void processExceptionalCase(
      StringBuilder current,
      BrailleWord fullWord,
      String lastChar,
      String lastDot,
      String inputDot);
}
