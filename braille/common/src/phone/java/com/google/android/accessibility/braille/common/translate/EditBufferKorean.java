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
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleFinalConsonant;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleFirstSymbol;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleInitConsonant;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isDoubleVowelConsonant;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isFinalConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isFinalHangul;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isInitialConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isNumber;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isNumberDot;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isOpenSymbolTranslation;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isSpecialOTranslation;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isSpecialOWithTwoBrailles;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.isVowelConsonants;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.loadCompletedHangul;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtilsKorean.shouldChangeToSymbol;
import static java.text.Normalizer.Form.NFC;
import static java.text.Normalizer.Form.NFD;

import android.content.Context;
import android.text.TextUtils;
import android.view.KeyEvent;
import com.google.android.accessibility.braille.common.BrailleCommonLog;
import com.google.android.accessibility.braille.common.BrailleCommonUtils;
import com.google.android.accessibility.braille.common.ImeConnection;
import com.google.android.accessibility.braille.common.R;
import com.google.android.accessibility.braille.common.TalkBackSpeaker;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.translate.BrailleTranslator;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Objects;

/** An EditBuffer for Korean Braille. */
public class EditBufferKorean extends EditBufferUnContracted {
  private static final String TAG = "EditBufferKorean";
  private final Context context;
  private final TalkBackSpeaker talkBack;
  private final HashMap<String, String> previousResultMap;
  private boolean numericMode = false;

  public EditBufferKorean(
      Context context, BrailleTranslator brailleTranslator, TalkBackSpeaker talkBack) {
    super(context, brailleTranslator, talkBack);
    this.context = context;
    this.talkBack = talkBack;
    this.previousResultMap = new HashMap<>();
    loadCompletedHangul(context);
  }

  @CanIgnoreReturnValue
  @Override
  public String appendBraille(ImeConnection imeConnection, BrailleCharacter brailleCharacter) {
    holdings.append(brailleCharacter);
    if (getNumeric().equals(brailleCharacter)) {
      numericMode = true;
      BrailleCommonLog.d(TAG, "numericMode = true by getNumeric().equals(brailleCharacter)");
    }
    String speak = getSpeakableAnnouncement(brailleCharacter);
    talkBack.speak(speak, TalkBackSpeaker.AnnounceType.INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH);
    String result = getAppendResult(brailleCharacter);
    String input = "";
    if (!TextUtils.isEmpty(result)) {
      checkSpaceNumberAndCharBeforeCommit(imeConnection, result);
      input = generateToInputText(result);
      imeConnection.inputConnection.setComposingText(input, input.length());
      previousResultMap.put(holdings.toString(), result);
    }
    lastCommitIndexOfHoldings = holdings.size();
    return input;
  }

  @Override
  public void deleteCharacterBackward(ImeConnection imeConnection) {
    if (holdings.isEmpty()) {
      BrailleCommonUtils.performKeyAction(imeConnection.inputConnection, KeyEvent.KEYCODE_DEL);
      return;
    }
    BrailleCharacter deleted = holdings.get(holdings.size() - 1);
    holdings.remove(holdings.size() - 1);
    String speak = getSpeakableAnnouncement(deleted);
    if (!TextUtils.isEmpty(speak)) {
      talkBack.speak(
          context.getString(R.string.read_out_deleted, speak),
          TalkBackSpeaker.AnnounceType.INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH);
    }
    if (!holdings.isEmpty()) {
      BrailleCharacter b = holdings.get(holdings.size() - 1);
      String result = getAppendResult(b);
      if (result != null) {
        String input = generateToInputText(result);
        imeConnection.inputConnection.setComposingText(input, input.length());
      }
    } else {
      BrailleCommonUtils.performKeyAction(imeConnection.inputConnection, KeyEvent.KEYCODE_DEL);
    }
    lastCommitIndexOfHoldings = holdings.size();
  }

  private String getAppendResult(BrailleCharacter brailleCharacter) {
    BrailleWord fullWord = holdings;
    BrailleWord prevWord =
        fullWord.size() > 1 ? fullWord.subword(0, fullWord.size() - 1) : new BrailleWord();
    String prevTrans = prevWord.isEmpty() ? "" : previousResultMap.get(prevWord.toString());
    String lastDot =
        !fullWord.isEmpty() && !prevWord.isEmpty()
            ? fullWord.get(prevWord.size() - 1).toString()
            : "";
    String inputDot = brailleCharacter.toString();
    BrailleCommonLog.d(TAG, "prevTrans:" + prevTrans + " / inputDot:" + inputDot);
    if (numericMode && (inputDot.contains("3") || inputDot.contains("6"))) {
      numericMode = isAvailableSymbolWithNumeric(lastDot, inputDot);
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

  private void appendSyllable(
      StringBuilder current, String lastChar, String lastDot, String syllable) {
    // This case: 10-3ㅏ >> 10-나
    if (isNumber(lastChar) && isInitialConsonants(lastDot) && Objects.equals(syllable, "ᅡ")) {
      if (current.length() > 0) {
        current.deleteCharAt(current.length() - 1);
      }
      current.append(Normalizer.normalize(getInitialConsonants(lastDot), NFD));
    }
    current.append(Normalizer.normalize(syllable, NFD));
  }

  private void appendDoubleSpecial(StringBuilder current, String lastDot, String inputDot) {
    convertStringToNfc(current);
    if (current.length() > 0) {
      current.deleteCharAt(current.length() - 1);
    }
    convertStringToNfd(current);
    current.append(
        Normalizer.normalize(getSpecialOTranslation(combineDots(lastDot, inputDot)), NFD));
  }

  private boolean isSpecialOTrans(String lastChar, String lastDot, String currentDot) {
    boolean result =
        isVowelConsonants(lastDot)
            || (isFinalConsonants(lastDot) && isFinalHangul(lastChar))
            || // case: 99.9%
            // 1. except for 10-3아
            // 2. 등자, 덩어리
            (isNumberDot(lastDot) && isNumber(lastChar) && !Objects.equals(currentDot, "126"));
    result = result && isVowelConsonants(currentDot);
    if (isVowelConsonants(lastDot) && Objects.equals(currentDot, "34")) {
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

  @Override
  public void appendSpace(ImeConnection imeConnection) {
    numericMode = false;
    BrailleCommonLog.d(TAG, "numericMode = false by appendSpace");
    checkLastDotForCommaSymbol(imeConnection);
    checkLastDotForSymbol(imeConnection);
    super.appendSpace(imeConnection);
  }

  @Override
  public void appendNewline(ImeConnection imeConnection) {
    if (EditBufferUtils.isMultiLineField(imeConnection.editorInfo.inputType)) {
      checkLastDotForSymbol(imeConnection);
    }
    super.appendNewline(imeConnection);
  }

  private void checkLastDotForCommaSymbol(ImeConnection imeConnection) {
    if (holdings.isEmpty()) {
      return;
    }
    String lastDot = holdings.get(holdings.size() - 1).toString();
    if (Objects.equals(lastDot, "5")) {
      String prevString = previousResultMap.get(holdings.toString());
      if (!TextUtils.isEmpty(prevString)) {
        prevString = prevString.replace(getInitialConsonants("5"), ",");
        imeConnection.inputConnection.setComposingText(prevString, prevString.length());
        previousResultMap.put(holdings.toString(), prevString);
      }
    }
  }

  private void checkLastDotForSymbol(ImeConnection imeConnection) {
    if (holdings.isEmpty()) {
      return;
    }
    String lastDot = holdings.get(holdings.size() - 1).toString();
    String prevString = previousResultMap.get(holdings.toString());
    if (TextUtils.isEmpty(prevString)) {
      prevString = "";
    }
    StringBuilder current = new StringBuilder(Normalizer.normalize(prevString, NFD));
    if (shouldChangeToSymbol(current, lastDot, "")) {
      String result = changeToSymbolAboutLastChar(current, lastDot, "");
      current.append(result.replace(" ", ""));
      imeConnection.inputConnection.setComposingText(current.toString(), current.length());
      previousResultMap.put(holdings.toString(), current.toString());
    }
  }

  String getSpeakableAnnouncement(BrailleCharacter brailleCharacter) {
    if (isInitialConsonants(brailleCharacter.toString())
        || isVowelConsonants(brailleCharacter.toString())
        || isFinalConsonants(brailleCharacter.toString())) {
      return "";
    }
    String textToSpeak =
        getAppendBrailleTextToSpeak(context.getResources(), brailleCharacter).orElse(null);
    if (TextUtils.isEmpty(textToSpeak)) {
      textToSpeak = BrailleTranslateUtils.getDotsText(context.getResources(), brailleCharacter);
    }
    return textToSpeak;
  }

  private final KoreanExceptionalCase exceptionalCase =
      new KoreanExceptionalCase() {
        @Override
        public String getStringAtFirstTime(BrailleWord fullWord, String lastDot, String inputDot) {
          String result;
          if (numericMode && isNumberDot(inputDot)) {
            result = getNumber(inputDot);
          } else if (isCircleNumberDot(combineDots(lastDot, inputDot))) {
            result = getCircleNumber(combineDots(lastDot, inputDot));
            numericMode = false;
          } else if (isDoubleFirstSymbol(lastDot, inputDot)) {
            result = getOpenSymbolTranslation(combineDots(lastDot, inputDot));
          } else if (isOpenSymbolTranslation(inputDot)) {
            result = getOpenSymbolTranslation(inputDot);
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
          BrailleCommonLog.d(TAG, "getSyllable inputDot: " + inputDot);
          if (TextUtils.isEmpty(inputDot)) {
            return " "; // appendSpace
          }
          String result = getInitialConsonants(inputDot);
          // We should check final at first than vowel, because of "ㅆ" and "ㅖ"
          if (TextUtils.isEmpty(result)) {
            result = getFinalConsonants(inputDot);
            // "ㅆ" cannot come with "ㄱ","ㅅ","ㄹ","ㅊ", so "ㅖ" is correct, it is not "ㅆ"
            if ((Objects.equals(lastDot, "4")
                    || Objects.equals(lastDot, "6")
                    || Objects.equals(lastDot, "5")
                    || // 김정ㄹㅆ >> 김정례
                    Objects.equals(lastDot, "56"))
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
            current.append(Normalizer.normalize(getNumber(inputDot), NFD));
          } else if (isCircleNumberDot(combineDots(lastDot, inputDot))) {
            current.append(
                Normalizer.normalize(getCircleNumber(combineDots(lastDot, inputDot)), NFD));
            numericMode = false;
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
      };
}
