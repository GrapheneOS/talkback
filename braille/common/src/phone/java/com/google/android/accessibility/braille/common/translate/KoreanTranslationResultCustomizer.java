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

import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isNumber;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isVowelConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isVowelHangul;

import android.content.Context;
import android.text.TextUtils;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.common.BrailleCommonLog;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import com.google.android.accessibility.braille.translate.TranslationResult;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Customizer for Korean braille translation. */
public class KoreanTranslationResultCustomizer extends GoogleTranslationResultCustomizer {
  private static final String TAG = "KoreanTranslationResultCustomizer";
  private static final int CODE_POINT_OF_WON = 8361;
  private static final String WON = "4-2456";
  private static final String COMMA_SYMBOL = "2";
  private static final String SLASH = "3456";
  private static final String HYPHEN = "36";
  private static final String TILDE = "36-36";
  private static final String LEFT_SINGLE_QUOTE = "6-236";
  private static final String RIGHT_SINGLE_QUOTE = "356-3";
  private static final String LEFT_DOUBLE_QUOTE = "236";
  private static final String RIGHT_DOUBLE_QUOTE = "356";
  private static final String GEOSS = "4-234-34"; // 겄
  private static final String IR_DAN = "6-4-234-34"; // 껐
  private static final String CH = "56"; // ㅊ
  private static final String ONG = "123456"; // 옹
  private static final String PERIOD = "256";

  public KoreanTranslationResultCustomizer(Context context) {
    super(context);
  }

  @Nullable
  @Override
  public TranslationResult customize(TranslationResult result, BrailleTranslator translator) {
    TranslationResult ret = super.customize(result, translator);
    if (ret == null) {
      return null;
    }
    CharSequence text = ret.text();
    String original = ret.cells().toString();
    ret = customizeAlphabet(ret);
    ret = customizeFullDot(ret);
    ret = customizeConnect(ret);
    ret = customizeConnect2(ret);
    ret = customizeDoubleFinal(ret);
    ret = customizeWord(ret);
    ret = customizeSymbol(ret);
    ret = customizeExceptionalSymbol(ret);
    BrailleCommonLog.d(
        TAG, "text: " + text + " original: " + original + " customize: " + ret.cells());
    textToBrailleLog(ret);
    brailleToTextLog(ret);
    return ret;
  }

  private TranslationResult customizeExceptionalSymbol(TranslationResult ret) {
    String original = ret.cells().toString();
    ret = replaceWonSymbol(ret);
    ret = replaceCommaSymbol(ret);
    BrailleCommonLog.d(
        TAG, "customizeExceptionalSymbol:" + !original.equals(ret.cells().toString()));
    return ret;
  }

  private TranslationResult replaceWonSymbol(TranslationResult ret) {
    String text = ret.text().toString();
    for (int index = text.indexOf(CODE_POINT_OF_WON);
        -1 < index && index < text.length();
        index = text.indexOf(CODE_POINT_OF_WON, index + 1)) {
      BrailleWord brailleWord = new BrailleWord();
      brailleWord.append(new BrailleWord(WON));
      ret = TranslationResult.correctTranslation(ret, brailleWord, index, index + 1);
    }
    return ret;
  }

  private TranslationResult replaceCommaSymbol(TranslationResult ret) {
    String text = ret.text().toString();
    for (int index = text.indexOf(",");
        0 < index && index < text.length() - 1;
        index = text.indexOf(",", index + 1)) {
      // 1,234,567 > comma is "2" at this case
      if (text.substring(index - 1, index).matches("^[0-9]*$")
          && text.substring(index + 1, index + 2).matches("^[0-9]*$")) {
        BrailleWord brailleWord = new BrailleWord();
        brailleWord.append(new BrailleWord(COMMA_SYMBOL));
        ret = TranslationResult.correctTranslation(ret, brailleWord, index, index + 1);
      }
    }
    return ret;
  }

  private TranslationResult customizeSymbol(TranslationResult ret) {
    String original = ret.cells().toString();
    ret = replaceSymbol(ret, "", "'");
    ret = replaceSymbol(ret, "", "\"");
    ret = replaceSymbol(ret, "236-3", "\\(");
    ret = replaceSymbol(ret, "6-356", "\\)");
    ret = replaceSymbol(ret, "236-2", "\\{");
    ret = replaceSymbol(ret, "5-356", "\\}");
    ret = replaceSymbol(ret, "236-23", "\\[");
    ret = replaceSymbol(ret, "56-356", "\\]");
    ret = replaceSymbol(ret, "5-236", "\\<");
    ret = replaceSymbol(ret, "356-2", "\\>");
    ret = replaceSymbol(ret, "26", "\\+");
    ret = replaceHypon(ret, "\\-");
    ret = replaceSlash(ret, "/");
    ret = replaceSymbol(ret, "5-23", "·"); // matching for center dot
    ret = replaceSymbol(ret, "356-4-1-256", "@");
    ret = replaceSymbol(ret, "356-456-1456-256", "#");
    ret = replaceSymbol(ret, "356-4-26-256", "\\^");
    ret = replaceSymbol(ret, "356-4-12346-256", "&");
    BrailleCommonLog.d(TAG, "customizeSymbol:" + !original.equals(ret.cells().toString()));
    return ret;
  }

  private TranslationResult replaceSlash(TranslationResult ret, String compare) {
    Pattern regexPattern = Pattern.compile(compare);
    Matcher p = regexPattern.matcher(ret.text());
    while (p.find()) {
      String replacement = p.group();
      if (p.start() - 1 < 0) {
        continue;
      }
      if (p.end() + 1 > ret.text().length()) {
        continue;
      }
      String prevText = ret.text().toString().substring(p.start() - 1, p.start());
      String nextText = ret.text().toString().substring(p.end(), p.end() + 1);
      BrailleCommonLog.d(
          TAG,
          "replaceSlash prev:"
              + prevText
              + " next:"
              + nextText
              + " s:"
              + p.start()
              + " e:"
              + p.end());
      if (isNumber(prevText) && isNumber(nextText)) {
        BrailleWord brailleWord = new BrailleWord();
        appendBrailleWord(ret, brailleWord, p.start(), p.end());
        brailleWord.append(new BrailleCharacter(SLASH));
        ret = TranslationResult.correctTranslation(ret, brailleWord, p.start(), p.end());
        BrailleCommonLog.d(
            TAG,
            "replacement:"
                + replacement
                + " brailleWord:"
                + brailleWord
                + " start:"
                + p.start()
                + " end:"
                + p.end());
      }
    }
    return ret;
  }

  private TranslationResult replaceHypon(TranslationResult ret, String compare) {
    Pattern regexPattern = Pattern.compile(compare);
    Matcher p = regexPattern.matcher(ret.text());
    while (p.find()) {
      BrailleWord brailleWord = new BrailleWord();
      ret = appendHypon(ret, brailleWord, p);
    }
    return ret;
  }

  private TranslationResult appendHypon(TranslationResult ret, BrailleWord word, Matcher p) {
    int textIdx = p.start();
    int start = ret.textToBraillePositions().get(p.start());
    int end = ret.textToBraillePositions().get(p.end());
    for (int i = start; ; i++) {
      int compare = ret.brailleToTextPositions().get(i);
      if (compare > textIdx) {
        break;
      }
      end = i + 1;
    }
    if (end > ret.cells().size()) {
      return ret;
    }
    BrailleWord sub = ret.cells().subword(start, end);
    BrailleCommonLog.d(TAG, "checkHypon:" + sub + ":" + Objects.equals(sub.toString(), TILDE));
    word.append(new BrailleWord(HYPHEN));
    BrailleCommonLog.d(
        TAG,
        "replacement:" + p.group() + " ret:" + ret.cells() + " start:" + p.start() + " end:" + end);
    return TranslationResult.correctTranslation(
        ret, word, p.start(), ret.brailleToTextPositions().get(end));
  }

  private TranslationResult replaceSymbol(TranslationResult ret, String dots, String compare) {
    Pattern regexPattern = Pattern.compile(compare);
    Matcher p = regexPattern.matcher(ret.text());
    int openAndCloseFlagForSymbol = 1;
    while (p.find()) {
      String replacement = p.group();
      BrailleWord brailleWord = new BrailleWord();
      if (Objects.equals(replacement, "'")) {
        if ((openAndCloseFlagForSymbol % 2) == 1) {
          brailleWord.append(new BrailleWord(LEFT_SINGLE_QUOTE));
        } else {
          brailleWord.append(new BrailleWord(RIGHT_SINGLE_QUOTE));
        }
        openAndCloseFlagForSymbol++;
      } else if ("\"".contentEquals(replacement)) {
        if ((openAndCloseFlagForSymbol % 2) == 1) {
          brailleWord.append(new BrailleWord(LEFT_DOUBLE_QUOTE));
        } else {
          brailleWord.append(new BrailleWord(RIGHT_DOUBLE_QUOTE));
        }
        openAndCloseFlagForSymbol++;
      } else {
        brailleWord.append(new BrailleWord(dots));
      }
      BrailleCommonLog.d(
          TAG,
          "replacement:"
              + replacement
              + " brailleWord:"
              + brailleWord
              + " start:"
              + p.start()
              + " end:"
              + p.end());
      ret = TranslationResult.correctTranslation(ret, brailleWord, p.start(), p.end());
    }
    return ret;
  }

  private TranslationResult customizeWord(TranslationResult ret) {
    String original = ret.cells().toString();
    ret = replaceWord(ret, "그래서", "4-246-5-1235-6-234", "1-234");
    ret = replaceWord(ret, "그러나", "4-246-5-234-14", "1-14");
    ret = replaceWord(ret, "그러면", "4-246-5-234-15-16", "1-25");
    ret = replaceWord(ret, "그러므로", "4-246-5-234-15-246-5-136", "1-26");
    ret = replaceWord(ret, "그런데", "4-246-5-23456-24-1345", "1-1345");
    ret = replaceWord(ret, "그리고", "4-246-5-135-4-136", "1-136");
    ret = replaceWord(ret, "그리하여", "4-246-5-135-245-126-156", "1-156");
    BrailleCommonLog.d(TAG, "customizeWord:" + !original.equals(ret.cells().toString()));
    return ret;
  }

  private TranslationResult replaceWord(
      TranslationResult ret, String compare, String normalDots, String contractedDots) {
    Pattern regexPattern = Pattern.compile(compare);
    Matcher p = regexPattern.matcher(ret.text());
    while (p.find()) {
      String replacement = p.group();
      BrailleWord brailleWord = new BrailleWord();
      if (shouldContracted(ret, p.start())) {
        brailleWord.append(new BrailleWord(contractedDots));
      } else {
        brailleWord.append(new BrailleWord(normalDots));
      }
      BrailleCommonLog.d(
          TAG,
          "replacement:"
              + replacement
              + " brailleWord:"
              + brailleWord
              + " start:"
              + p.start()
              + " end:"
              + p.end());
      ret = TranslationResult.correctTranslation(ret, brailleWord, p.start(), p.end());
    }
    return ret;
  }

  private boolean shouldContracted(TranslationResult ret, int start) {
    boolean result = (start == 0) || " ".contentEquals(ret.text().subSequence(start - 1, start));
    BrailleCommonLog.d(TAG, "shouldContracted:" + result);
    return result;
  }

  private void textToBrailleLog(TranslationResult ret) {
    StringBuilder temp = new StringBuilder();
    for (int val : ret.textToBraillePositions()) {
      temp.append(val).append("-");
    }
    BrailleCommonLog.d(TAG, "tTob:" + temp);
  }

  private void brailleToTextLog(TranslationResult ret) {
    StringBuilder temp2 = new StringBuilder();
    for (int val : ret.brailleToTextPositions()) {
      temp2.append(val).append("-");
    }
    BrailleCommonLog.d(TAG, "bTot:" + temp2);
  }

  private TranslationResult customizeDoubleFinal(TranslationResult ret) {
    String original = ret.cells().toString();
    Pattern regexPattern = Pattern.compile("[겄껐팠]");
    Matcher p = regexPattern.matcher(ret.text());
    while (p.find()) {
      String replacement = p.group();
      BrailleCommonLog.d(TAG, "replacement:" + replacement);
      BrailleWord brailleWord = new BrailleWord();
      if ("겄".contentEquals(replacement)) {
        brailleWord.append(new BrailleWord(GEOSS));
      } else if ("껐".contentEquals(replacement)) {
        brailleWord.append(new BrailleWord(IR_DAN));
      } else {
        brailleWord.append(new BrailleWord("145-126-34"));
      }
      BrailleCommonLog.d(TAG, "customizeDoubleFinal:" + !original.equals(ret.cells().toString()));
      ret = TranslationResult.correctTranslation(ret, brailleWord, p.start(), p.end());
    }
    return ret;
  }

  // This api is for "ㅑ,ㅘ,ㅜ,ㅝ"+"36"+"애"
  // When "ㅑ,ㅘ,ㅜ,ㅝ" is followed by ‘애’, it is indicated by putting "36" in between it.
  private TranslationResult customizeConnect2(TranslationResult ret) {
    String original = ret.cells().toString();
    Pattern regexPattern = Pattern.compile("[애]");
    Matcher ye = regexPattern.matcher(ret.text());
    while (ye.find()) {
      String replacement = ye.group();
      BrailleCommonLog.d(TAG, "replacement:" + replacement);
      if (!needConnect2(ret, ye.start())) {
        continue;
      }
      BrailleWord brailleWord = new BrailleWord();
      brailleWord.append(new BrailleCharacter(HYPHEN));
      appendBrailleWord(ret, brailleWord, ye.start(), ye.end());
      ret = TranslationResult.correctTranslation(ret, brailleWord, ye.start(), ye.end());
    }
    BrailleCommonLog.d(TAG, "customizeConnect:" + !original.equals(ret.cells().toString()));
    return ret;
  }

  private boolean needConnect2(TranslationResult ret, int start) {
    int brailleStart = 0;
    int brailleEnd = 0;
    int index = 0;
    for (int idx : ret.textToBraillePositions()) {
      if (index + 1 == start) {
        brailleStart = idx;
      } else if (index == start) {
        brailleEnd = idx;
        break;
      }
      index++;
    }
    BrailleCommonLog.d(TAG, "brailleStart:" + brailleStart + " brailleEnd:" + brailleEnd);
    if (brailleStart >= brailleEnd) {
      BrailleCommonLog.d(TAG, "needConnect2:" + false);
      return false;
    }
    BrailleWord word = new BrailleWord(ret.cells().subword(brailleStart, brailleEnd));
    BrailleCommonLog.d(TAG, "word:" + word);
    int pos = word.size();
    boolean result = pos > 0 && isSpecialConsonants(word.get(pos - 1).toString());
    BrailleCommonLog.d(TAG, "needConnect2:" + result);
    return result;
  }

  private boolean isSpecialConsonants(String dots) {
    return Objects.equals(dots, "345") // ㅝ
        || Objects.equals(dots, "1236") // ㅑ
        || Objects.equals(dots, "134") // ㅘ
        || Objects.equals(dots, "1234"); // ㅜ
  }

  // This api is for 모음+"36"+"예"
  // When a vowel letter is followed by ‘예’, it is indicated by putting "36" in between it.
  private TranslationResult customizeConnect(TranslationResult ret) {
    String original = ret.cells().toString();
    Pattern regexPattern = Pattern.compile("[예]");
    Matcher ye = regexPattern.matcher(ret.text());
    while (ye.find()) {
      String replacement = ye.group();
      BrailleCommonLog.d(TAG, "replacement:" + replacement);
      if (!needConnect(ret, ye.start())) {
        continue;
      }
      BrailleWord brailleWord = new BrailleWord();
      brailleWord.append(new BrailleCharacter(HYPHEN));
      appendBrailleWord(ret, brailleWord, ye.start(), ye.end());
      ret = TranslationResult.correctTranslation(ret, brailleWord, ye.start(), ye.end());
    }
    BrailleCommonLog.d(TAG, "customizeConnect:" + !original.equals(ret.cells().toString()));
    return ret;
  }

  private boolean needConnect(TranslationResult ret, int start) {
    int brailleStart = 0;
    int brailleEnd = 0;
    int index = 0;
    for (int idx : ret.textToBraillePositions()) {
      if (index + 1 == start) {
        brailleStart = idx;
      } else if (index == start) {
        brailleEnd = idx;
        break;
      }
      index++;
    }
    BrailleCommonLog.d(TAG, "brailleStart:" + brailleStart + " brailleEnd:" + brailleEnd);
    if (brailleStart >= brailleEnd) {
      return false;
    }
    BrailleWord word = new BrailleWord(ret.cells().subword(brailleStart, brailleEnd));
    BrailleCommonLog.d(TAG, "word:" + word);
    int pos = word.size();
    boolean result = pos > 0 && isVowelConsonants(word.get(pos - 1).toString());
    BrailleCommonLog.d(TAG, "needConnect:" + result);
    return result;
  }

  private TranslationResult customizeFullDot(TranslationResult ret) {
    String original = ret.cells().toString();
    Pattern regexPattern = Pattern.compile("[ㄱ-ㅎㅏ-ㅣ]");
    Matcher jamo = regexPattern.matcher(ret.text());
    while (jamo.find()) {
      String replacement = jamo.group();
      BrailleCommonLog.d(TAG, "replacement:" + replacement);
      BrailleWord brailleWord = new BrailleWord();
      brailleWord.append(new BrailleCharacter(ONG));
      appendBrailleWord(ret, brailleWord, jamo.start(), jamo.end());
      if (needSpace(ret, replacement, jamo.end())) {
        BrailleCommonLog.d(TAG, "append empty");
        brailleWord.append(new BrailleCharacter());
      }
      BrailleCommonLog.d(TAG, "customizeFullDot:" + !original.equals(ret.cells().toString()));
      ret = TranslationResult.correctTranslation(ret, brailleWord, jamo.start(), jamo.end());
    }
    return ret;
  }

  private boolean needSpace(TranslationResult ret, CharSequence replacement, int end) {
    if (isVowelHangul(replacement.toString())) {
      return false;
    }
    CharSequence text = ret.text();
    if (end + 1 > text.length()) {
      return false;
    }
    CharSequence sub = text.subSequence(end, end + 1);
    if (TextUtils.isEmpty(sub)) {
      return false;
    }
    BrailleCommonLog.d(
        TAG, "sub:" + sub + ":" + " ".contentEquals(sub.toString()) + ":" + sub.length());
    return !Objects.equals(sub.toString(), " ");
  }

  private TranslationResult customizeAlphabet(TranslationResult ret) {
    String original = ret.cells().toString();
    Map<Integer, Integer> alphabetMap = findAlphabet(ret.text());
    for (int start : alphabetMap.keySet()) {
      int end = alphabetMap.get(start);
      CharSequence replacement = ret.text().subSequence(start, end);
      BrailleCommonLog.d(TAG, "replacement:" + replacement);
      BrailleWord brailleWord = new BrailleWord();
      if (needStartSymbol(ret, start)) {
        brailleWord.append(new BrailleCharacter(RIGHT_DOUBLE_QUOTE));
      }
      appendBrailleWord(ret, brailleWord, start, end);
      if (needEndSymbol(ret, end)) {
        brailleWord.append(new BrailleCharacter(PERIOD));
      }
      BrailleCommonLog.d(TAG, "customizeAlphabet:" + !original.equals(ret.cells().toString()));
      ret = TranslationResult.correctTranslation(ret, brailleWord, start, end);
    }
    return ret;
  }

  private boolean needStartSymbol(TranslationResult ret, int start) {
    int idx = start - 1;
    String subtext = "";
    while (idx > 0) {
      subtext = ret.text().toString().substring(idx, idx + 1);
      if (!Objects.equals(subtext, " ")) {
        break;
      }
      BrailleCommonLog.d(TAG, "sub:" + subtext);
      idx--;
    }
    if (subtext.matches("[a-zA-Z]")) {
      BrailleCommonLog.d(TAG, "needEndSymbol false sub:" + subtext);
      return false;
    }
    int pos = ret.textToBraillePositions().get(start);
    boolean result = !Objects.equals(ret.cells().get(pos).toString(), CH);
    BrailleCommonLog.d(TAG, "needStartSymbol:" + result);
    return result;
  }

  private boolean needEndSymbol(TranslationResult ret, int end) {
    CharSequence text = ret.text();
    int idx = end + 1;
    String subtext = "";
    while (idx < text.length()) {
      subtext = text.toString().substring(idx, idx + 1);
      if (!Objects.equals(subtext, " ")) {
        break;
      }
      idx++;
    }
    if (subtext.matches("[a-zA-Z]")) {
      BrailleCommonLog.d(TAG, "needEndSymbol false sub:" + subtext);
      return false;
    }
    boolean result = false;
    if (end < text.length()) {
      result = !text.subSequence(end, end + 1).toString().matches("^[0-9]*$");
    }
    BrailleCommonLog.d(TAG, "needEndSymbol:" + result);
    return result;
  }

  private void appendBrailleWord(TranslationResult ret, BrailleWord newWord, int start, int end) {
    BrailleWord word = new BrailleWord(ret.cells());
    if (ret.text().length() == end) {
      newWord.append(word);
    } else {
      int brailleStart = ret.textToBraillePositions().get(start);
      int brailleEnd = ret.textToBraillePositions().get(end);
      newWord.append(word.subword(brailleStart, brailleEnd));
    }
    BrailleCommonLog.d(TAG, "newWord:" + newWord);
  }

  private Map<Integer, Integer> findAlphabet(CharSequence ret) {
    HashMap<Integer, Integer> map = new HashMap<>();
    if (TextUtils.isEmpty(ret)) {
      return map;
    }
    String text = ret.toString();
    int firstIdx = -1;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (('A' <= c && c <= 'Z') || ('a' <= c && c <= 'z')) {
        if (firstIdx == -1) {
          firstIdx = i;
        }
      } else {
        if (firstIdx > -1) {
          BrailleCommonLog.d(TAG, "firstIdx:" + firstIdx + " end:" + i);
          map.put(firstIdx, i);
          firstIdx = -1;
        }
      }
    }
    return map;
  }
}
