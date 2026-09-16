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
package com.google.android.accessibility.braille.common.translate;

import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.DOTS3456;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.getDotsText;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.appendSymbol;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.changeToSymbolAboutLastChar;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.checkSpaceNumberAndCharBeforeCommit;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.combineDots;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.convertStringToNfc;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.convertStringToNfd;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.convertToSymbol;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.correctIfPossible;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.generateToInputText;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.getCircleNumber;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.getFinalConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.getInitialConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.getNumber;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.getOpenSymbolTranslation;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.getSpecialOTranslation;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.getVowelConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isAvailableSymbolWithNumeric;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isCircleNumberDot;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isCompletedHangul;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleFinalConsonant;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleFirstSymbol;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleInitConsonant;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleVowelConsonant;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isFinalConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isFinalHangul;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isInitHangul;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isInitialConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isNumber;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isNumberDot;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isOpenSymbolTranslation;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isSpecialOTranslation;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isSpecialOWithTwoBrailles;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isVowelConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isVowelHangul;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.loadCompletedHangul;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.removeLastString;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.shouldChangeToSymbol;
import static java.text.Normalizer.Form.NFC;
import static java.text.Normalizer.Form.NFD;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.braille.common.BrailleCommonLog;
import com.google.android.accessibility.braille.common.ImeConnection;
import com.google.android.accessibility.braille.common.TalkBackSpeaker;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import com.google.android.accessibility.braille.translate.TranslationResult;
import com.google.common.collect.ImmutableMap;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Korean Braille Grade 2 EditBuffer. */
public class EditBufferKorean2 extends EditBufferContracted {
  private static final HashMap<String, String> previousResultMap = new HashMap<>();
  private static final String TAG = "EditBufferKorean2";
  private static final ImmutableMap<String, String> ABBREVIATION_VOWELS =
      ImmutableMap.<String, String>builder()
          .put("1456", "ᅥᆨ")
          .put("23456", "ᅥᆫ")
          .put("2345", "ᅥᆯ")
          .put("16", "ᅧᆫ")
          .put("1256", "ᅧᆯ")
          .put("12456", "ᅧᆼ")
          .put("1346", "ᅩᆨ")
          .put("12356", "ᅩᆫ")
          .put("123456", "ᅩᆼ")
          .put("1245", "ᅮᆫ")
          .put("12346", "ᅮᆯ")
          .put("1356", "ᅳᆫ")
          .put("2346", "ᅳᆯ")
          .put("12345", "ᅵᆫ")
          .buildOrThrow();
  private static final ImmutableMap<String, String> COMBINE_FINAL_MAP =
      ImmutableMap.<String, String>builder()
          .put("ᆨᆨ", "ᆩ")
          .put("ᆨᆺ", "ᆪ")
          .put("ᆫᆽ", "ᆬ")
          .put("ᆫᇂ", "ᆭ")
          .put("ᆯᆨ", "ᆰ")
          .put("ᆯᆷ", "ᆱ")
          .put("ᆯᆸ", "ᆲ")
          .put("ᆯᆺ", "ᆳ")
          .put("ᆯᇀ", "ᆴ")
          .put("ᆯᇁ", "ᆵ")
          .put("ᆯᇂ", "ᆶ")
          .buildOrThrow();
  private static final ImmutableMap<String, String> SPECIAL_ABBREVIATION_VOWELS =
      ImmutableMap.<String, String>builder()
          .put("1456", "억")
          .put("23456", "언")
          .put("2345", "얼")
          .put("16", "연")
          .put("1256", "열")
          .put("12456", "영")
          .put("1346", "옥")
          .put("12356", "온")
          .put("123456", "옹")
          .put("1245", "운")
          .put("12346", "울")
          .put("1356", "은")
          .put("2346", "을")
          .put("12345", "인")
          .buildOrThrow();
  private static final ImmutableMap<String, String> SPECIAL_ABBREVIATION_CHARS =
      ImmutableMap.<String, String>builder()
          .put("1246", "가")
          .put("123", "사")
          .put("6-1246", "까")
          .put("6-123", "싸")
          .buildOrThrow();
  private static final ImmutableMap<String, String> ABBREVIATION_CHARACTERS =
      ImmutableMap.<String, String>builder()
          .put("14", "나")
          .put("24", "다")
          .put("15", "마")
          .put("45", "바")
          .put("46", "자")
          .put("124", "카")
          .put("125", "타")
          .put("145", "파")
          .put("245", "하")
          .buildOrThrow();
  private static final ImmutableMap<String, String> ABBREVIATION_WORD =
      ImmutableMap.<String, String>builder()
          .put("456-234", "것")
          .put("1-234", "그래서")
          .put("1-14", "그러나")
          .put("1-25", "그러면")
          .put("1-26", "그러므로")
          .put("1-1345", "그런데")
          .put("1-136", "그리고")
          .put("1-156", "그리하여")
          .buildOrThrow();
  private final KoreanContractedExceptionalCase exceptionalCase =
      new KoreanContractedExceptionalCase();
  private final Context koreanContext;
  private boolean appendBraille = false;
  private boolean deleteWord = false;

  public EditBufferKorean2(
      Context context, BrailleTranslator translator, TalkBackSpeaker talkBack) {
    super(context, new Korean2BrailleTranslator(translator), talkBack);
    koreanContext = context;
    ((Korean2BrailleTranslator) this.translator).setExceptionalCase(exceptionalCase);
    previousResultMap.clear();
    loadCompletedHangul(context);
  }

  @Override
  protected void fillTranslatorMaps(
      Map<String, String> initialCharacterTranslationMap,
      Map<String, String> nonInitialCharacterTranslationMap) {
    // Do nothing.
  }

  @Override
  public String appendBraille(ImeConnection imeConnection, BrailleCharacter brailleCharacter) {
    appendBraille = true;
    if (getNumeric().equals(brailleCharacter)) {
      exceptionalCase.setNumericMode(true);
      BrailleCommonLog.d(TAG, "numericMode = true by getNumeric().equals(brailleCharacter)");
    }
    return super.appendBraille(imeConnection, brailleCharacter);
  }

  @Override
  public void deleteCharacterBackward(ImeConnection imeConnection) {
    appendBraille = false;
    super.deleteCharacterBackward(imeConnection);
  }

  @Override
  public void deleteWord(ImeConnection imeConnection) {
    deleteWord = true;
    super.deleteWord(imeConnection);
    deleteWord = false;
  }

  @Override
  protected void checkSpaceBetweenNumberAndChar(ImeConnection imeConnection, String appendedText) {
    checkSpaceNumberAndCharBeforeCommit(imeConnection, appendedText);
  }

  @Override
  protected String getAnnouncement(
      Resources resources, BrailleTranslator translator, BrailleWord brailleWord, int index) {
    if (deleteWord) {
      return index == 0 ? generateToInputText(previousResultMap.get(brailleWord.toString())) : "";
    }
    String result = getAppendResult(brailleWord, exceptionalCase);
    if (!TextUtils.isEmpty(result)) {
      previousResultMap.put(brailleWord.toString(), result);
    } else {
      String textToSpeak = getTextToSpeak(koreanContext.getResources(), brailleWord.get(index));
      if (!textToSpeak.isEmpty()) {
        return textToSpeak;
      }
      return getDotsText(koreanContext.getResources(), brailleWord.get(index));
    }
    result = getAnnouncement(brailleWord, index, appendBraille);
    return generateToInputText(result);
  }

  private String getAnnouncement(BrailleWord brailleWord, int index, boolean append) {
    BrailleWord prevWord = index > 0 ? brailleWord.subword(0, index) : null;
    BrailleWord fullWord = brailleWord.subword(0, brailleWord.size());
    String prevStr = prevWord == null ? "" : previousResultMap.get(prevWord.toString());
    String fullStr = previousResultMap.get(fullWord.toString());
    if (prevStr == null) {
      prevStr = "";
    }
    if (fullStr == null) {
      fullStr = "";
    }
    if (!append) {
      prevStr = Normalizer.normalize(prevStr, NFD);
      fullStr = Normalizer.normalize(fullStr, NFD);
    } else {
      return Normalizer.normalize(fullStr, NFC);
    }
    String result = findCommonString(prevStr, fullStr);
    if (TextUtils.isEmpty(result)) {
      result = getDotsText(koreanContext.getResources(), brailleWord.get(index));
    }
    return Normalizer.normalize(result, NFC);
  }

  @Override
  public void appendSpace(ImeConnection imeConnection) {
    exceptionalCase.setNumericMode(false);
    BrailleCommonLog.d(TAG, "numericMode = false by appendSpace");
    super.appendSpace(imeConnection);
  }

  @Override
  public void appendNewline(ImeConnection imeConnection) {
    ((Korean2BrailleTranslator) translator).appendedNewLine(imeConnection.editorInfo.inputType);
    super.appendNewline(imeConnection);
  }

  @Override
  protected boolean isLetter(char character) {
    return false;
  }

  @Override
  protected BrailleCharacter getCapitalize() {
    // TODO: May distinguish between this because cap indicator only appear after roman
    // indicator.
    return null;
  }

  @Override
  protected BrailleCharacter getNumeric() {
    return DOTS3456;
  }

  @Override
  protected boolean forceInitialDefaultTranslation(String dotsNumber) {
    return false;
  }

  @Override
  protected boolean forceNonInitialDefaultTranslation(String dotsNumber) {
    return false;
  }

  private String findCommonString(String prevString, String fullString) {
    StringBuilder prev = new StringBuilder(TextUtils.isEmpty(prevString) ? "" : prevString);
    StringBuilder full = new StringBuilder(TextUtils.isEmpty(fullString) ? "" : fullString);
    int index = 0;
    for (int i = 0; i < prev.length() && i < full.length(); i++, index++) {
      if (prev.charAt(i) != full.charAt(i)) {
        break;
      }
    }
    return full.substring(index);
  }

  private static void checkLastDotForSymbol(BrailleWord word) {
    if (word.isEmpty()) {
      return;
    }
    String lastDot = word.get(word.size() - 1).toString();
    String prevString = previousResultMap.get(word.toString());
    if (TextUtils.isEmpty(prevString)) {
      prevString = "";
    }
    StringBuilder current = new StringBuilder(Normalizer.normalize(prevString, NFD));
    if (shouldChangeToSymbol(current, lastDot, "")) {
      String result = changeToSymbolAboutLastChar(current, lastDot, "");
      current.append(result.replace(" ", ""));
      previousResultMap.put(word.toString(), current.toString());
    }
  }

  private static String getAppendResult(
      BrailleWord fullWord, KoreanContractedExceptionalCase exceptionalCase) {
    BrailleWord prevWord =
        fullWord.size() > 1 ? fullWord.subword(0, fullWord.size() - 1) : new BrailleWord();
    String prevTrans = prevWord.isEmpty() ? "" : previousResultMap.get(prevWord.toString());
    String lastDot =
        !fullWord.isEmpty() && !prevWord.isEmpty()
            ? fullWord.get(prevWord.size() - 1).toString()
            : "";
    String inputDot = !fullWord.isEmpty() ? fullWord.get(fullWord.size() - 1).toString() : "";
    BrailleCommonLog.d(TAG, "prevTrans:" + prevTrans + " / inputDot:" + inputDot);
    // If "3" or "6" dot is inserted, this dots are not numeric. It is symbol or character
    // So we need to check this what is meaning
    if (exceptionalCase.isNumericMode() && (inputDot.contains("3") || inputDot.contains("6"))) {
      exceptionalCase.setNumericMode(isAvailableSymbolWithNumeric(lastDot, inputDot));
    }
    String result = "";
    if (TextUtils.isEmpty(prevTrans)) {
      previousResultMap.clear();
      result = exceptionalCase.getStringAtFirstTime(fullWord, lastDot, inputDot);
    } else {
      result = correctIfPossible(fullWord, prevTrans, lastDot, inputDot, exceptionalCase);
    }
    BrailleCommonLog.d(TAG, "result: " + result);
    if (result == null) {
      result = "";
    }
    return Normalizer.normalize(result, NFC);
  }

  private static boolean isSpecialAbbreviationCharacters(String dot) {
    boolean result = SPECIAL_ABBREVIATION_CHARS.containsKey(dot);
    BrailleCommonLog.d(TAG, "isSpecialAbbreviationCharacters: " + result);
    return result;
  }

  // 억, 언, 얼, 연, 열, 영, 옥, 온, 옹, 운, 울, 은, 을, 인
  private static boolean isSpecialAbbreviationVowels(String dot) {
    boolean result = SPECIAL_ABBREVIATION_VOWELS.containsKey(dot);
    BrailleCommonLog.d(TAG, "isSpecialAbbreviationVowels by " + dot + ": " + result);
    return result;
  }

  private static void appendNumber(StringBuilder current, String lastDot, String inputDot) {
    String lastChar = Normalizer.normalize(current, NFC);
    lastChar = lastChar.substring(lastChar.length() - 1);
    if (Objects.equals(lastChar, "연")
        && Objects.equals(lastDot, "16")
        && Objects.equals(inputDot, "3456")) {
      // Remove "연"
      removeLastString(current);
      current.append(Normalizer.normalize("×", NFD));
    } else if (Objects.equals(lastChar, "옜")
        && Objects.equals(lastDot, "34")
        && Objects.equals(inputDot, "3456")) {
      // Remove "옜"
      removeLastString(current);
      current.append(Normalizer.normalize("÷", NFD));
    }
    current.append(Normalizer.normalize(getNumber(inputDot), NFD));
  }

  private static void appendSyllable(
      StringBuilder current, String lastChar, String lastDot, String syllable) {
    // This case: 10-3ㅏ >> 10-나
    if (isNumber(lastChar)
        && ABBREVIATION_CHARACTERS.containsKey(lastDot)
        && Objects.equals(syllable, "ᅡ")) {
      if (current.length() > 0) {
        current.deleteCharAt(current.length() - 1);
      }
      current.append(Normalizer.normalize(getInitialConsonants(lastDot), NFD));
    }
    current.append(Normalizer.normalize(syllable, NFD));
  }

  private static void appendDoubleSpecial(StringBuilder current, String lastDot, String inputDot) {
    convertStringToNfc(current);
    if (current.length() > 0) {
      current.deleteCharAt(current.length() - 1);
    }
    convertStringToNfd(current);
    current.append(
        Normalizer.normalize(getSpecialOTranslation(combineDots(lastDot, inputDot)), NFD));
  }

  private static void appendDoubleFinalWithAbbreviationVowel(
      StringBuilder current, String lastChar, String inputDot) {
    if (current.length() > 0) {
      current.deleteCharAt(current.length() - 1);
    }
    String finalCons = getFinalConsonants(inputDot);
    current.append(Normalizer.normalize(COMBINE_FINAL_MAP.get(lastChar + finalCons), NFD));
  }

  private static boolean isDoubleFinalWithAbbreviationVowel(
      StringBuilder current, String lastChar, String lastDot, String inputDot) {
    boolean result =
        ABBREVIATION_VOWELS.containsKey(lastDot)
            && isFinalConsonants(inputDot)
            && COMBINE_FINAL_MAP.containsKey(lastChar + getFinalConsonants(inputDot));
    if (result) {
      StringBuilder temp = new StringBuilder(current);
      if (temp.length() > 0) {
        temp.deleteCharAt(temp.length() - 1);
      }
      String finalCons = getFinalConsonants(inputDot);
      temp.append(Normalizer.normalize(COMBINE_FINAL_MAP.get(lastChar + finalCons), NFD));
      String tempStr = Normalizer.normalize(temp.toString(), NFC);
      result = isCompletedHangul(tempStr.substring(tempStr.length() - 1));
      BrailleCommonLog.d(
          TAG, "isDoubleFinalWithAbbreviationVowel double check tempStr:" + tempStr + " " + result);
    }
    BrailleCommonLog.d(TAG, "isDoubleFinalWithAbbreviationVowel by " + inputDot + " " + result);
    return result;
  }

  private static void appendAbbreviationWord(
      StringBuilder current, String lastChar, String lastDot, String inputDot) {
    String whole = combineDots(lastDot, inputDot);
    String result = Normalizer.normalize(ABBREVIATION_WORD.get(whole), NFD);
    if (Objects.equals(lastDot, "456")) {
      BrailleCommonLog.d(TAG, "lastChar " + lastChar);
      if (Objects.equals(lastChar, "ᄉ")) {
        if (current.length() > 0) {
          current.deleteCharAt(current.length() - 1);
        }
        result = Normalizer.normalize("껏", NFD);
      }
    } else if (Objects.equals(lastDot, "1")) {
      if (current.length() > 0) {
        current.deleteCharAt(current.length() - 1);
      }
    }
    current.append(result);
    BrailleCommonLog.d(TAG, "appendAbbreviationWord by " + whole + ": " + result);
  }

  private static void appendAbbreviationVowel(
      StringBuilder current, String lastChar, String inputDot) {
    if ((Objects.equals(lastChar, "ᄉ")
            || Objects.equals(lastChar, "ᄊ")
            || Objects.equals(lastChar, "ᄌ")
            || Objects.equals(lastChar, "ᄍ")
            || Objects.equals(lastChar, "ᄎ"))
        && Objects.equals(inputDot, "12456")) {
      current.append(Normalizer.normalize("ᅥᆼ", NFD));
    } else {
      current.append(Normalizer.normalize(ABBREVIATION_VOWELS.get(inputDot), NFD));
    }
  }

  private static boolean isDoubleSpecialAbbreviationCharacters(String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = SPECIAL_ABBREVIATION_CHARS.containsKey(whole);
    BrailleCommonLog.d(TAG, "isDoubleSpecialAbbreviationCharacters by " + whole + ": " + result);
    return result;
  }

  private static boolean isSpecialOTrans(String lastChar, String lastDot, String currentDot) {
    boolean result =
        isVowelConsonants(lastDot)
            || Objects.equals(lastChar, "ᅡ")
            || // 회사ㅔ >> 회사에
            isSpecialAbbreviationVowels(lastDot)
            || isFinalConsonants(lastDot)
            ||
            // 1. except for 10-3아
            // 2. 등자, 덩어리
            (isNumberDot(lastDot) && isNumber(lastChar) && !Objects.equals(currentDot, "126"));
    result = result && isVowelConsonants(currentDot);
    // vowel + "34" = vowel + "ㅆ"
    if ((isVowelHangul(lastChar) || isVowelConsonants(lastDot))
        && Objects.equals(currentDot, "34")) {
      result = false; // final "ㅆ", it is not "예"
    }
    if (Objects.equals(lastDot, "36") && Objects.equals(currentDot, "34")) {
      result = true; // It is "예" after vowel
    }
    if (Objects.equals(lastDot, "356") && Objects.equals(currentDot, "1234")) { // case: 99.9%
      result = false; // It is %, it is not "워"
    }
    BrailleCommonLog.d(
        TAG,
        "isSpecialOTrans lastChar:"
            + lastChar
            + " lastDot:"
            + lastDot
            + " currentDot:"
            + currentDot
            + " result:"
            + result);
    return result;
  }

  private static boolean isAbbreviationVowel(String lastDot, String currentDot) {
    boolean result =
        !Objects.equals(lastDot, "1245")
            && // 신문ㅡㄴ >> 신문은
            isInitialConsonants(lastDot)
            && ABBREVIATION_VOWELS.containsKey(currentDot);
    BrailleCommonLog.d(
        TAG, "isAbbreviationVowel lastDot:" + lastDot + " currentDot:" + currentDot + " " + result);
    return result;
  }

  private static boolean isAbbreviationWord(
      BrailleWord fullWord, String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = ABBREVIATION_WORD.containsKey(whole);
    if (result && Objects.equals(lastDot, "1")) {
      result = (fullWord.size() == 2);
    }
    BrailleCommonLog.d(TAG, "isAbbreviationWord by " + whole + ": " + result);
    return result;
  }

  private static boolean isAbbreviationChar(String lastDot, String currentDot) {
    boolean result = false;
    if (ABBREVIATION_CHARACTERS.containsKey(lastDot)
        && (TextUtils.isEmpty(currentDot)
            || isFinalConsonants(currentDot)
            || isInitialConsonants(currentDot)
            || Objects.equals(currentDot, "36"))) { // 다예, 나예, 따예
      result = true;
    }
    if (result
        && Objects.equals(currentDot, "34")
        && (Objects.equals(lastDot, "145") || Objects.equals(lastDot, "245"))) {
      result = false;
    }
    BrailleCommonLog.d(
        TAG, "isAbbreviationChar lastDot:" + lastDot + " currentDot:" + currentDot + " " + result);
    return result;
  }

  /** translateToPrint() is overridden only for Korean */
  @VisibleForTesting
  static class Korean2BrailleTranslator implements BrailleTranslator {
    private final BrailleTranslator brailleTranslator;
    private boolean appendNewLine = false;
    private KoreanContractedExceptionalCase exceptionalCase;

    public Korean2BrailleTranslator(BrailleTranslator brailleTranslator) {
      this.brailleTranslator = brailleTranslator;
    }

    private void setExceptionalCase(KoreanContractedExceptionalCase exceptionalCase) {
      this.exceptionalCase = exceptionalCase;
    }

    private void appendedNewLine(int inputType) {
      if (EditBufferUtils.isMultiLineField(inputType)) {
        appendNewLine = true;
      }
    }

    @Override
    public String translateToPrint(BrailleWord word) {
      if (word.isEmpty()) {
        return "";
      }
      if (appendNewLine) {
        checkLastDotForSymbol(word);
        appendNewLine = false;
      }
      String result = previousResultMap.get(word.toString());
      if (TextUtils.isEmpty(result)) {
        result = getAppendResult(word, exceptionalCase);
      }
      BrailleCommonLog.d(TAG, "translateForCommit: " + result);
      return result;
    }

    @Override
    public String translateToPrintPartial(BrailleWord brailleWord) {
      return brailleTranslator.translateToPrintPartial(brailleWord);
    }

    @Override
    public TranslationResult translate(CharSequence text, int cursorPosition) {
      return brailleTranslator.translate(text, cursorPosition);
    }
  }

  private static class KoreanContractedExceptionalCase implements KoreanExceptionalCase {
    private boolean numericMode = false;

    public void setNumericMode(boolean numericMode) {
      this.numericMode = numericMode;
    }

    public boolean isNumericMode() {
      return numericMode;
    }

    @Override
    public String getStringAtFirstTime(BrailleWord fullWord, String lastDot, String inputDot) {
      BrailleCommonLog.d(TAG, "getStringAtFirstTime");
      String result;
      if (numericMode && isNumberDot(inputDot)) {
        result = getNumber(inputDot);
      } else if (isCircleNumberDot(combineDots(lastDot, inputDot))) {
        result = getCircleNumber(combineDots(lastDot, inputDot));
        numericMode = false;
      } else if (isDoubleFirstSymbol(lastDot, inputDot)) {
        result = getOpenSymbolTranslation(combineDots(lastDot, inputDot));
      } else if (isAbbreviationWord(fullWord, lastDot, inputDot)) {
        result = ABBREVIATION_WORD.get(combineDots(lastDot, inputDot));
      } else if (isOpenSymbolTranslation(inputDot)) {
        result = getOpenSymbolTranslation(inputDot);
      } else if (isSpecialAbbreviationCharacters(inputDot)) {
        result = SPECIAL_ABBREVIATION_CHARS.get(inputDot);
      } else if (isSpecialAbbreviationVowels(inputDot)) {
        result = SPECIAL_ABBREVIATION_VOWELS.get(inputDot);
      } else if (isSpecialOTranslation(inputDot)) {
        result = getSpecialOTranslation(inputDot);
      } else {
        result = getSyllable("", "", inputDot);
      }
      BrailleCommonLog.d(
          TAG,
          "getStringAtFirstTime numericMode:"
              + numericMode
              + " by "
              + inputDot
              + " result-"
              + result);
      return result;
    }

    @Override
    public String getSyllable(String lastChar, String lastDot, String inputDot) {
      BrailleCommonLog.d(
          TAG,
          "getSyllable lastChar:" + lastChar + ":lastDot:" + lastDot + ":inputDot:" + inputDot);
      if (TextUtils.isEmpty(inputDot)) {
        return " "; // appendSpace
      }
      String result = getInitialConsonants(inputDot);
      // We should check final at first than vowel, because of "ㅆ" and "ㅖ"
      if (TextUtils.isEmpty(result)) {
        result = getFinalConsonants(inputDot);
        // "ㅆ" cannot come with "ㄱ","ㅅ","ㄹ","ㅊ","ㅍ","ㅎ" so "ㅖ" is correct, it is not "ㅆ"
        if ((Objects.equals(lastDot, "4")
                || Objects.equals(lastDot, "6")
                || Objects.equals(lastDot, "5")
                || // 김정ㄹㅆ >> 김정례
                Objects.equals(lastDot, "56")
                || Objects.equals(lastDot, "145")
                || Objects.equals(lastDot, "245"))
            && Objects.equals(inputDot, "34")) {
          result = "";
        }
        // If final consonants is came after number or space, it is not confirmed
        if (TextUtils.isEmpty(lastChar) || TextUtils.isEmpty(lastDot) || isNumber(lastChar)) {
          result = "";
        }
      }
      if (TextUtils.isEmpty(result)) {
        result = getVowelConsonants(inputDot);
      }
      String ret = TextUtils.isEmpty(result) ? "" : result;
      BrailleCommonLog.d(TAG, "getSyllable: " + ret);
      return ret;
    }

    @Override
    public void processExceptionalCase(
        StringBuilder current,
        BrailleWord fullWord,
        String lastChar,
        String lastDot,
        String inputDot) {
      if (numericMode && isNumberDot(inputDot)) {
        appendNumber(current, lastDot, inputDot);
      } else if (isCircleNumberDot(combineDots(lastDot, inputDot))) {
        current.append(Normalizer.normalize(getCircleNumber(combineDots(lastDot, inputDot)), NFD));
        numericMode = false;
      } else if (isDoubleFinalWithAbbreviationVowel(current, lastChar, lastDot, inputDot)) {
        appendDoubleFinalWithAbbreviationVowel(current, lastChar, inputDot);
      } else if (isAbbreviationVowel(lastDot, inputDot) && !isNumber(lastChar)) {
        appendAbbreviationVowel(current, lastChar, inputDot);
      } else if (isDoubleSpecialAbbreviationCharacters(lastDot, inputDot)) { // 까, 싸
        if (current.length() > 0) {
          current.deleteCharAt(current.length() - 1);
        }
        current.append(
            Normalizer.normalize(
                SPECIAL_ABBREVIATION_CHARS.get(combineDots(lastDot, inputDot)), NFD));
      } else if (isSpecialAbbreviationCharacters(inputDot)) { // 가, 사
        if (isInitHangul(lastChar)) {
          current.append(Normalizer.normalize("ᅡ", NFD));
        }
        current.append(Normalizer.normalize(SPECIAL_ABBREVIATION_CHARS.get(inputDot), NFD));
      } else if (isAbbreviationChar(lastDot, inputDot) && !isNumber(lastChar)) {
        if (isInitHangul(lastChar)) {
          current.append(Normalizer.normalize("ᅡ", NFD));
        }
        processExceptionalCase(current, fullWord, "ᅡ", "126", inputDot);
      } else if (isSpecialAbbreviationVowels(inputDot)) {
        current.append(Normalizer.normalize(SPECIAL_ABBREVIATION_VOWELS.get(inputDot), NFD));
      } else if (isAbbreviationWord(fullWord, lastDot, inputDot)) {
        appendAbbreviationWord(current, lastChar, lastDot, inputDot);
      } else if (isSpecialOWithTwoBrailles(current, lastDot, inputDot)) {
        appendDoubleSpecial(current, lastDot, inputDot);
      } else if (isDoubleVowelConsonant(current, lastDot, inputDot)) {
        if (current.length() > 0) {
          current.deleteCharAt(current.length() - 1);
        }
        current.append(
            Normalizer.normalize(getVowelConsonants(combineDots(lastDot, inputDot)), NFD));
      } else if (isDoubleFinalConsonant(current, lastDot, inputDot)) {
        if (current.length() > 0) {
          current.deleteCharAt(current.length() - 1);
        }
        current.append(
            Normalizer.normalize(getFinalConsonants(combineDots(lastDot, inputDot)), NFD));
      } else if (isDoubleInitConsonant(lastDot, inputDot)) {
        if (current.length() > 0) {
          current.deleteCharAt(current.length() - 1);
        }
        current.append(
            Normalizer.normalize(getInitialConsonants(combineDots(lastDot, inputDot)), NFD));
      } else if (isSpecialOTrans(lastChar, lastDot, inputDot)) {
        current.append(Normalizer.normalize(getSpecialOTranslation(inputDot), NFD));
      } else {
        String syllable = getSyllable(lastChar, lastDot, inputDot);
        if (TextUtils.isEmpty(syllable)) {
          current.append(Normalizer.normalize(appendSymbol(current, lastDot, inputDot), NFD));
        } else {
          if (isFinalHangul(lastChar) && isFinalHangul(syllable)) {
            if (current.length() > 0) {
              current.deleteCharAt(current.length() - 1);
            }
            current.append(Normalizer.normalize(lastChar + syllable, NFC));
          } else {
            appendSyllable(current, lastChar, lastDot, syllable);
          }
          convertToSymbol(current, syllable, lastDot, inputDot);
        }
      }
    }
  }
}
