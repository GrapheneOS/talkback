/*
 * Copyright 2020 Google Inc.
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

package com.google.android.accessibility.braille.translate.liblouis;

import static com.google.android.accessibility.utils.StringUtils.capitalizeFirstLetter;

import android.content.Context;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.common.base.Ascii;

/** LibLouis Unified English Braille grade 2 translator. */
public class LibLouisTranslatorUeb2 extends LibLouisTranslator {
  private static final BrailleCharacter CAPITALIZE = new BrailleCharacter(6);

  public LibLouisTranslatorUeb2(Context context) {
    super(context, "en-ueb-g2.ctb");
    fillBypassMap();
  }

  // LINT.IfChange
  // If the corrected translation is not in the frequency words, please add it in
  // LibLouisUeb2TranslationWorkaroundTest.
  private void fillBypassMap() {
    addToBypassMap("1234-256-234-256", "p.s.");
    addToBypassMap("1234-256-1234-256-234-256", "p.p.s.");
    addToBypassMap("136-256-234-256", "u.s.");
    addToBypassMap("6-136-256-6-234-256", "U.S.");
    addToBypassMap("24-256-15-256", "i.e.");
    addToBypassMap("15-256-1245-256", "e.g.");
    addToBypassMap("1234-256-134-256", "p.m.");
    addToBypassMap("1-256-134-256", "a.m.");
    addToBypassMap("1234-256-1234-256", "p.p.");
    addToBypassMap("6-1234-125-256-6-145-256", "Ph.D.");
    addToBypassMap("1-256-136-256", "a.u.");
    addToBypassMap("12-1246-123456-145-146-24-1235-15", "bedfordshire");
    addToBypassMap("123-24-3-123", "li'l");
    addToBypassMap("14-1-1345-3-34", "can'st");
    addToBypassMap("145-256-134-256", "d.m.");
    addToBypassMap("124-1235-123-24-123-13456", "friendlily");
    addToBypassMap("2345-36-134-135-12-24-123-15", "t-mobile");
    addToBypassMap("2345-36-6-134-135-12-24-123-15", "t-Mobile");
    addToBypassMap("1346-36-6-1235-1-13456", "x-Ray");
  }

  // LINT.ThenChange(//depot/google3/javatests/com/google/android/accessibility/braille/translate/liblouis/LibLouisUeb2TranslationWorkaroundTest.java)

  private void addToBypassMap(String dashEncoded, String print) {
    // Handle lower-case (example: somewhere).
    getBypassMap().put(BrailleWord.create(dashEncoded), print);
    // Handle initial-letter-upper-case (example: Somewhere).
    getBypassMap()
        .put(BrailleWord.create(CAPITALIZE + "-" + dashEncoded), capitalizeFirstLetter(print));
    // Handle all-word-upper-case (example: SOMEWHERE).
    getBypassMap()
        .put(
            BrailleWord.create(CAPITALIZE + "-" + CAPITALIZE + "-" + dashEncoded),
            Ascii.toUpperCase(print));
  }
}
