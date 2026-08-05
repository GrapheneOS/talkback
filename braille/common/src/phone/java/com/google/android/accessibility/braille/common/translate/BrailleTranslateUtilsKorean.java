package com.google.android.accessibility.braille.common.translate;

import static java.text.Normalizer.Form.NFC;
import static java.text.Normalizer.Form.NFD;

import android.content.Context;
import android.content.res.AssetManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import com.google.android.accessibility.braille.common.BrailleCommonLog;
import com.google.android.accessibility.braille.common.BrailleCommonUtils;
import com.google.android.accessibility.braille.common.ImeConnection;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;

/** Utils for translation of Korean Braille. */
public class BrailleTranslateUtilsKorean {
  private static final String TAG = "BrailleTranslateUtilsKorean";
  private static final String HYPHEN = "-";
  private static final char INIT_CONSONANT_START_CHARACTER = 0x1100;
  private static final char VOWEL_CONSONANT_START_CHARACTER = 0x1161;
  private static final char FINAL_CONSONANT_START_CHARACTER = 0x11A8;
  private static final ArrayList<String> completedHangul = new ArrayList<>();
  private static final ImmutableSet<String> EXCEPT_FROM_COMPLETED_HANGUL =
      ImmutableSet.of(
          "엌", "읔", "같", "곁", "깥", "꼍", "낱", "돝", "릍", "뭍", "샅", "앝", "얕", "옅", "읕", "짙", "홑", "흩",
          "갚", "겊", "깊", "높", "닢", "덮", "랖", "릎", "붚", "섶", "싶", "엎", "읖", "짚", "톺");
  // This map is for converting pronunciation from initial cons. to used alone
  private static final ImmutableMap<String, String> INPUT_TEXT_MAP_FOR_SPEAKING =
      ImmutableMap.<String, String>builder()
          .put("ᄀ", "ㄱ")
          .put("ᄁ", "ㄲ")
          .put("ᄂ", "ㄴ")
          .put("ᄃ", "ㄷ")
          .put("ᄄ", "ㄸ")
          .put("ᄅ", "ㄹ")
          .put("ᄆ", "ㅁ")
          .put("ᄇ", "ㅂ")
          .put("ᄈ", "ㅃ")
          .put("ᄉ", "ㅅ")
          .put("ᄊ", "ㅆ")
          .put("ᄋ", "ㅇ")
          .put("ᄌ", "ㅈ")
          .put("ᄍ", "ㅉ")
          .put("ᄎ", "ㅊ")
          .put("ᄏ", "ㅋ")
          .put("ᄐ", "ㅌ")
          .put("ᄑ", "ㅍ")
          .put("ᄒ", "ㅎ")
          .buildOrThrow();

  private static final ImmutableMap<String, String> INITIAL_CONSONANTS_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("4", String.valueOf(INIT_CONSONANT_START_CHARACTER)) // ㄱ
          .put("6-4", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 1))) // ㄲ
          .put("14", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 2))) // ㄴ
          .put("24", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 3))) // ㄷ
          .put("6-24", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 4))) // ㄸ
          .put("5", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 5))) // ㄹ
          .put("15", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 6))) // ㅁ
          .put("45", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 7))) // ㅂ
          .put("6-45", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 8))) // ㅃ
          .put("6", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 9))) // ㅅ
          .put("6-6", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 10))) // ㅆ
          .put("1245", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 11))) // ㅇ
          .put("46", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 12))) // ㅈ
          .put("6-46", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 13))) // ㅉ
          .put("56", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 14))) // ㅊ
          .put("124", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 15))) // ㅋ
          .put("125", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 16))) // ㅌ
          .put("145", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 17))) // ㅍ
          .put("245", String.valueOf((char) (INIT_CONSONANT_START_CHARACTER + 18))) // ㅎ
          .buildOrThrow();

  private static final ImmutableMap<String, String> VOWEL_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("126", String.valueOf(VOWEL_CONSONANT_START_CHARACTER)) // ㅏ
          .put("1235", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 1))) // ㅐ
          .put("345", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 2))) // ㅑ
          .put("345-1235", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 3))) // ㅒ
          .put("234", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 4))) // ㅓ
          .put("1345", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 5))) // ㅔ
          .put("156", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 6))) // ㅕ
          .put("34", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 7))) // ㅖ
          .put("136", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 8))) // ㅗ
          .put("1236", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 9))) // ㅘ
          .put("1236-1235", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 10))) // ㅙ
          .put("13456", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 11))) // ㅚ
          .put("346", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 12))) // ㅛ
          .put("134", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 13))) // ㅜ
          .put("1234", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 14))) // ㅝ
          .put("1234-1235", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 15))) // ㅞ
          .put("134-1235", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 16))) // ㅟ
          .put("146", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 17))) // ㅠ
          .put("246", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 18))) // ㅡ
          .put("2456", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 19))) // ㅢ
          .put("135", String.valueOf((char) (VOWEL_CONSONANT_START_CHARACTER + 20))) // ㅣ
          .buildOrThrow();
  private static final ImmutableMap<String, String> FINAL_CONSONANTS_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("1", String.valueOf(FINAL_CONSONANT_START_CHARACTER)) // ㄱ
          .put("1-1", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 1))) // ㄲ
          .put("1-3", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 2))) // ㄳ
          .put("25", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 3))) // ㄴ
          .put("25-13", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 4))) // ㄵ
          .put("25-356", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 5))) // ㄶ
          .put("35", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 6))) // ㄷ
          .put("2", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 7))) // ㄹ
          .put("2-1", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 8))) // ㄺ
          .put("2-26", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 9))) // ㄻ
          .put("2-12", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 10))) // ㄼ
          .put("2-3", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 11))) // ㄽ
          .put("2-236", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 12))) // ㄾ
          .put("2-256", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 13))) // ㄿ
          .put("2-356", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 14))) // ㅀ
          .put("26", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 15))) // ㅁ
          .put("12", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 16))) // ㅂ
          .put("12-3", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 17))) // ㅄ
          .put("3", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 18))) // ㅅ
          .put("34", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 19))) // ㅆ
          .put("2356", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 20))) // ㅇ
          .put("13", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 21))) // ㅈ
          .put("23", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 22))) // ㅊ
          .put("235", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 23))) // ㅋ
          .put("236", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 24))) // ㅌ
          .put("256", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 25))) // ㅍ
          .put("356", String.valueOf((char) (FINAL_CONSONANT_START_CHARACTER + 26))) // ㅎ
          .buildOrThrow();
  // If vowel is coming at first, it should be added "ㅇ" consonant
  private static final ImmutableMap<String, String> SPECIAL_O_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("126", "아")
          .put("345", "야")
          .put("234", "어")
          .put("156", "여")
          .put("136", "오")
          .put("346", "요")
          .put("134", "우")
          .put("146", "유")
          .put("246", "으")
          .put("135", "이")
          .put("36-1235", "애")
          .put("1235", "애")
          .put("1345", "에")
          .put("345-1235", "얘")
          .put("34", "예")
          .put("36-34", "예")
          .put("1236", "와")
          .put("1236-1235", "왜")
          .put("13456", "외")
          .put("2456", "의")
          .put("1234", "워")
          .put("1234-1235", "웨")
          .put("134-1235", "위")
          .buildOrThrow();
  private static final ImmutableMap<String, String> OPEN_SYMBOL_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("236", "“")
          .put("6-236", "'")
          .put("236-3", "(")
          .put("236-2", "{")
          .put("236-23", "[")
          .put("5-236", "<")
          .put("56-236", "《")
          .put("456-236", "?")
          .put("456-34", "/")
          .put("4-2456", "￦")
          .put("4-145", "$")
          .put("26", "+")
          .put("35", "-")
          .put("25-25", "=")
          .put("36-36", "~")
          .put("36", "-")
          .put("235", "!")
          .put("256", ".")
          .buildOrThrow();
  private static final ImmutableMap<String, String> CLOSE_SYMBOL_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("356", "”")
          .put("356-3", "'")
          .put("6-356", ")")
          .put("5-356", "}")
          .put("56-356", "]")
          .put("356-2", ">")
          .put("356-23", "》")
          .buildOrThrow();
  // For contracted Korean braille only.
  private static final ImmutableMap<String, String> CHANGE_SYMBOL_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("256-", ". ")
          .put("236-", "? ")
          .put("235-", "!")
          .buildOrThrow();
  private static final ImmutableMap<String, String> SYMBOL_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("356-1234", "%")
          .put("26", "+")
          .put("35", "-")
          .put("16", "×")
          .put("34-34", "÷")
          .put("25-25", "=")
          .put("36-36", "~")
          .put("36", "-")
          .put("6-6-6", "···")
          .put("235", "!")
          .put("256", ".")
          .put("5-23", "·")
          .put("2", ",")
          .put("5", ",")
          .put("5-", ", ")
          .put("236", "?")
          .put("456-236", "?")
          .put("5-2", ":")
          .put("56-23", ";")
          .put("35-35", "※")
          .put("456-34", "/")
          .buildOrThrow();
  private static final ImmutableMap<String, String> NUMBER_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("3456", "")
          .put("1", "1")
          .put("12", "2")
          .put("14", "3")
          .put("145", "4")
          .put("15", "5")
          .put("124", "6")
          .put("1245", "7")
          .put("125", "8")
          .put("24", "9")
          .put("245", "0")
          .buildOrThrow();
  private static final ImmutableMap<String, String> CIRCLE_NUMBER_TRANSLATION_MAP =
      ImmutableMap.<String, String>builder()
          .put("3456-2", "①")
          .put("3456-23", "②")
          .put("3456-25", "③")
          .put("3456-256", "④")
          .put("3456-26", "⑤")
          .put("3456-235", "⑥")
          .put("3456-2356", "⑦")
          .put("3456-236", "⑧")
          .put("3456-35", "⑨")
          .buildOrThrow();

  public static String combineDots(String firstDot, String secondDot) {
    return firstDot + HYPHEN + secondDot;
  }

  public static boolean isAvailableSymbolWithNumeric(String lastDot, String inputDot) {
    boolean result = false;
    if (!Objects.equals(lastDot, "36")
        && Objects.equals(inputDot, "36")) { // symbol "-" is keeping number, not "~"
      result = true;
    } else if (Objects.equals(lastDot, "5") && Objects.equals(inputDot, "23")) { // symbol "·"
      result = true;
    } else if (Objects.equals(inputDot, "256")) { // symbol "."
      result = true;
    } else if (Objects.equals(inputDot, "3456")) { // "3456" as number start
      result = true;
    } else if (Objects.equals(lastDot, "3456")
        && isNumberDot(inputDot)) { // This case is circle number
      result = true;
    }
    BrailleCommonLog.d(
        TAG,
        "isAvailableSymbolWithNumeric by lastDot:"
            + lastDot
            + " inputDot:"
            + inputDot
            + " "
            + result);
    return result;
  }

  public static boolean isNumberDot(String dot) {
    return NUMBER_TRANSLATION_MAP.containsKey(dot);
  }

  public static String getNumber(String dot) {
    return NUMBER_TRANSLATION_MAP.get(dot);
  }

  public static boolean isCircleNumberDot(String dot) {
    return CIRCLE_NUMBER_TRANSLATION_MAP.containsKey(dot);
  }

  public static String getCircleNumber(String dot) {
    return CIRCLE_NUMBER_TRANSLATION_MAP.get(dot);
  }

  public static boolean isInitialConsonants(String dot) {
    return INITIAL_CONSONANTS_TRANSLATION_MAP.containsKey(dot);
  }

  public static boolean isVowelConsonants(String dot) {
    return VOWEL_TRANSLATION_MAP.containsKey(dot);
  }

  public static boolean isFinalConsonants(String dot) {
    return FINAL_CONSONANTS_TRANSLATION_MAP.containsKey(dot);
  }

  public static boolean isSpecialOTranslation(String dot) {
    return SPECIAL_O_TRANSLATION_MAP.containsKey(dot);
  }

  public static boolean isOpenSymbolTranslation(String dot) {
    return OPEN_SYMBOL_TRANSLATION_MAP.containsKey(dot);
  }

  public static String getSpecialOTranslation(String dot) {
    return SPECIAL_O_TRANSLATION_MAP.get(dot);
  }

  public static String getOpenSymbolTranslation(String dot) {
    return OPEN_SYMBOL_TRANSLATION_MAP.get(dot);
  }

  public static String getInitialConsonants(String dot) {
    return INITIAL_CONSONANTS_TRANSLATION_MAP.get(dot);
  }

  public static String getVowelConsonants(String dot) {
    return VOWEL_TRANSLATION_MAP.get(dot);
  }

  public static String getFinalConsonants(String dot) {
    return FINAL_CONSONANTS_TRANSLATION_MAP.get(dot);
  }

  public static void loadCompletedHangul(Context context) {
    completedHangul.clear();
    AssetManager assetManager = context.getAssets();
    try {
      InputStream inputStream =
          assetManager.open("completed_hangul.txt", AssetManager.ACCESS_BUFFER);
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
      String line = "";
      while ((line = bufferedReader.readLine()) != null) {
        completedHangul.add(line);
      }
    } catch (IOException e) {
      BrailleCommonLog.e(TAG, "IOException: " + e);
    }
  }

  public static boolean isNumber(String lastInput) {
    boolean ret;
    if (TextUtils.isEmpty(lastInput)) {
      ret = false;
    } else {
      ret = lastInput.matches("^[#0-9]*$");
    }
    BrailleCommonLog.d(TAG, "isNumber by " + lastInput + ": " + ret);
    return ret;
  }

  /** Converts arg to input text for input. */
  public static String generateToInputText(String result) {
    if (TextUtils.isEmpty(result)) {
      return "";
    }
    StringBuilder input = new StringBuilder();
    String temp = "";
    for (int i = 0; i < result.length(); i++) {
      char ch = result.charAt(i);
      // This is first consonant range - 0x1100 : "ᄀ" , 0x115E: "ᄒ"
      temp =
          (0x1100 <= ch && ch <= 0x115E)
              ? INPUT_TEXT_MAP_FOR_SPEAKING.get(String.valueOf(ch))
              : String.valueOf(ch);
      input.append(temp);
    }
    return input.toString();
  }

  public static boolean isDuplicatedDot(String currentDot) {
    boolean result =
        Objects.equals(currentDot, "235")
            || // ㅋ or !
            Objects.equals(currentDot, "236")
            || // ㅌ or ?
            Objects.equals(currentDot, "256")
            || // ㅍ or .
            Objects.equals(currentDot, "356"); // ㅎ or "
    BrailleCommonLog.d(TAG, "isDuplicatedDot by " + currentDot + ": " + result);
    return result;
  }

  public static String appendSymbol(StringBuilder current, String lastDot, String currentDot) {
    String result = "";
    if (shouldChangeToSymbol(current, lastDot, currentDot)) {
      result = changeToSymbolAboutLastChar(current, lastDot, currentDot);
    } else if (isDoubleFirstSymbol(lastDot, currentDot)) {
      result = appendDoubleFirstSymbol(current, lastDot, currentDot);
    } else if (isDoubleMiddleSymbol(lastDot, currentDot)) {
      result = appendDoubleMiddleSymbol(current, lastDot, currentDot);
    } else if (isDoubleFinalSymbol(lastDot, currentDot)) {
      if (current.length() > 0) {
        current.deleteCharAt(current.length() - 1);
      }
      result = CLOSE_SYMBOL_TRANSLATION_MAP.get(combineDots(lastDot, currentDot));
    } else if (SYMBOL_TRANSLATION_MAP.containsKey(currentDot)) {
      result = SYMBOL_TRANSLATION_MAP.get(currentDot);
    } else if (CLOSE_SYMBOL_TRANSLATION_MAP.containsKey(currentDot)) {
      result = CLOSE_SYMBOL_TRANSLATION_MAP.get(currentDot);
    }
    if (Objects.equals(Normalizer.normalize(current.toString(), NFC), "연")
        && Objects.equals(lastDot, "16")
        && TextUtils.isEmpty(currentDot)) {
      // Replace "연" to "×" among characters
      removeLastString(current);
      current.append(Normalizer.normalize("×", NFD));
    } else if (Objects.equals(Normalizer.normalize(current.toString(), NFC), "옜")
        && Objects.equals(lastDot, "34")
        && TextUtils.isEmpty(currentDot)) {
      // Replace "옜" to "÷" among characters
      removeLastString(current);
      current.append(Normalizer.normalize("÷", NFD));
    }
    BrailleCommonLog.d(
        TAG,
        "getSymbol:"
            + result
            + "current:"
            + current
            + "lastDot:"
            + lastDot
            + "curDot:"
            + currentDot);
    return result;
  }

  public static String changeToSymbolAboutLastChar(
      StringBuilder current, String lastDot, String curDot) {
    if (current.length() > 0) {
      current.deleteCharAt(current.length() - 1);
    }
    String whole = combineDots(lastDot, curDot);
    String result = CHANGE_SYMBOL_TRANSLATION_MAP.get(whole);
    BrailleCommonLog.d(TAG, "changeToSymbolAboutLastChar by " + whole + ": " + result);
    return result;
  }

  public static boolean shouldChangeToSymbol(StringBuilder current, String lastDot, String curDot) {
    if (TextUtils.isEmpty(current)) {
      return false;
    }
    String temp = Normalizer.normalize(current.toString(), NFC);
    if (TextUtils.isEmpty(temp)) {
      return false;
    }
    String lastChar = temp.substring(temp.length() - 1);
    String whole = combineDots(lastDot, curDot);
    boolean result = false;
    if (EXCEPT_FROM_COMPLETED_HANGUL.contains(lastChar)
        && CHANGE_SYMBOL_TRANSLATION_MAP.containsKey(whole)) {
      result = true;
    }
    BrailleCommonLog.d(TAG, "shouldBeChangedToSymbol:" + lastChar + ":" + result);
    return result;
  }

  public static void removeLastString(StringBuilder current) {
    if (current.length() == 0) {
      return;
    }
    String temp = Normalizer.normalize(current, NFC);
    int length = current.length();
    current.delete(0, length);
    current.append(Normalizer.normalize(temp.substring(0, temp.length() - 1), NFD));
  }

  public static boolean isDoubleFinalSymbol(String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = CLOSE_SYMBOL_TRANSLATION_MAP.containsKey(whole);
    BrailleCommonLog.d(TAG, "isDoubleFinalSymbol by " + whole + ": " + result);
    return result;
  }

  public static boolean isDoubleMiddleSymbol(String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = SYMBOL_TRANSLATION_MAP.containsKey(whole);
    BrailleCommonLog.d(TAG, "isDoubleMiddleSymbol by " + whole + ": " + result);
    return result;
  }

  public static boolean isDoubleFirstSymbol(String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = OPEN_SYMBOL_TRANSLATION_MAP.containsKey(whole);
    BrailleCommonLog.d(TAG, "isDoubleFirstSymbol by " + whole + ": " + result);
    return result;
  }

  public static boolean isSpecialOWithTwoBrailles(
      StringBuilder current, String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = SPECIAL_O_TRANSLATION_MAP.containsKey(whole);
    String prevLastChar = "";
    if (current.length() > 1) {
      prevLastChar = current.substring(current.length() - 2, current.length() - 1);
    }
    if (isInitHangul(prevLastChar)) {
      result = false;
    }
    if (Objects.equals(lastDot, "36") && Objects.equals(currentDot, "34")) {
      result = true; // It is "ㅖ" after vowel
    }
    BrailleCommonLog.d(TAG, "isSpecialOWithTwoBrailles by " + whole + ": " + result);
    return result;
  }

  public static boolean isDoubleVowelConsonant(
      StringBuilder current, String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = VOWEL_TRANSLATION_MAP.containsKey(whole);
    BrailleCommonLog.d(TAG, "isDoubleVowelConsonant by " + whole + ": " + result);
    return result;
  }

  public static boolean isDoubleFinalConsonant(
      StringBuilder current, String lastDot, String currentDot) {
    String whole = combineDots(lastDot, currentDot);
    boolean result = FINAL_CONSONANTS_TRANSLATION_MAP.containsKey(whole);
    if (result) {
      StringBuilder temp = new StringBuilder(current);
      if (temp.length() > 0) {
        temp.deleteCharAt(temp.length() - 1);
      }
      temp.append(Normalizer.normalize(getFinalConsonants(combineDots(lastDot, currentDot)), NFD));
      String tempStr = Normalizer.normalize(temp.toString(), NFC);
      result = isCompletedHangul(tempStr.substring(tempStr.length() - 1));
      BrailleCommonLog.d(
          TAG, "isDoubleFinalConsonant double check tempStr:" + tempStr + " " + result);
    }
    BrailleCommonLog.d(TAG, "isDoubleFinalConsonant by " + whole + ": " + result);
    return result;
  }

  public static boolean isDoubleInitConsonant(String lastDot, String currentDot) {
    boolean result;
    String whole = combineDots(lastDot, currentDot);
    if (!Objects.equals(lastDot, "6")) {
      result = false;
    } else {
      result = INITIAL_CONSONANTS_TRANSLATION_MAP.containsKey(whole);
    }
    BrailleCommonLog.d(TAG, "isDoubleInitConsonant by " + whole + ": " + result);
    return result;
  }

  public static boolean isAllHangul(String all) {
    if (TextUtils.isEmpty(all)) {
      return false;
    }
    boolean result = Pattern.matches("^[ᄀ-ᄒ가-힣]*$", all);
    BrailleCommonLog.d(TAG, "isAllHangul " + all + " : " + result);
    return result;
  }

  public static boolean isCompletedHangul(String all) {
    if (TextUtils.isEmpty(all)) {
      return false;
    }
    boolean result = completedHangul.contains(all);
    BrailleCommonLog.d(TAG, "isCompletedHangul " + all + " : " + result);
    return result;
  }

  public static boolean isVowelHangul(String all) {
    if (TextUtils.isEmpty(all)) {
      return false;
    }
    boolean result = Pattern.matches("^[ᅡ-ᅵㅏ-ㅣ]*$", all);
    BrailleCommonLog.d(TAG, "isVowelHangul " + all + " : " + result);
    return result;
  }

  public static boolean isInitHangul(String all) {
    if (TextUtils.isEmpty(all)) {
      return false;
    }
    boolean result = Pattern.matches("^[ᄀ-ᄒ]*$", all);
    BrailleCommonLog.d(TAG, "isInitHangul " + all + " : " + result);
    return result;
  }

  public static boolean isFinalHangul(String all) {
    if (TextUtils.isEmpty(all)) {
      return false;
    }
    boolean result = Pattern.matches("^[ᆨ-ᇂ]*$", all);
    BrailleCommonLog.d(TAG, "isFinalHangul " + all + " : " + result);
    return result;
  }

  public static void convertStringToNfc(StringBuilder current) {
    String nfcString = Normalizer.normalize(current.toString(), NFC);
    current.delete(0, current.length());
    current.append(nfcString);
  }

  public static void convertStringToNfd(StringBuilder current) {
    String nfdString = Normalizer.normalize(current.toString(), NFD);
    current.delete(0, current.length());
    current.append(nfdString);
  }

  public static void convertToSymbol(
      StringBuilder current, String syllable, String lastDot, String curDot) {
    String currentStr = Normalizer.normalize(current.toString(), NFC);
    String lastSyllable = currentStr.substring(currentStr.length() - 1);
    if (!isAllHangul(lastSyllable)
        || (isDuplicatedDot(curDot) && !isCompletedHangul(lastSyllable))
        || isWonDollarSymbol(currentStr, lastDot, curDot)) {
      current.deleteCharAt(current.length() - 1);
      String symbol = appendSymbol(current, lastDot, curDot);
      if (TextUtils.isEmpty(symbol)) {
        current.append(Normalizer.normalize(syllable, NFD));
      } else {
        current.append(Normalizer.normalize(symbol, NFD));
      }
    }
  }

  public static String correctIfPossible(
      BrailleWord fullWord,
      String prevTrans,
      String lastDot,
      String inputDot,
      KoreanExceptionalCase exceptionalCase) {
    String prevResult = Normalizer.normalize(prevTrans, NFD);
    StringBuilder result = new StringBuilder(TextUtils.isEmpty(prevResult) ? "" : prevResult);
    getHangulIfPossible(result, fullWord, lastDot, inputDot, exceptionalCase);
    BrailleCommonLog.d(TAG, "correctIfPossible: " + result);
    return result.toString();
  }

  // Check sentence is number + " " + character, if it is true, we will remove space.
  public static void checkSpaceNumberAndCharBeforeCommit(
      ImeConnection imeConnection, String appendedText) {
    int index = EditBufferUtils.getCursorPosition(imeConnection.inputConnection);
    // If index is less than 2, text is one character. We don't need to check this case.
    if (index < 2) {
      return;
    }
    BrailleCommonLog.d(
        TAG, "full:" + EditBufferUtils.getTextFieldText(imeConnection.inputConnection) + ":");
    CharSequence before = imeConnection.inputConnection.getTextBeforeCursor(2, index);
    BrailleCommonLog.d(
        TAG, "index:" + index + ":before:" + before + ":appendedText:" + appendedText + ":");
    if (TextUtils.isEmpty(before) || before.length() < 2) {
      return;
    }
    if (TextUtils.isEmpty(appendedText)) {
      return;
    }
    String beforeChar = before.toString().substring(0, 1);
    String beforeLast = before.toString().substring(1, 2);
    String afterChar = Normalizer.normalize(appendedText, NFD).substring(0, 1);
    String afterStr = appendedText.substring(0, 1);
    BrailleCommonLog.d(
        TAG,
        "index:"
            + index
            + ":beforeChar:"
            + beforeChar
            + ":afterChar:"
            + afterChar
            + ":afterStr:"
            + afterStr
            + ":");
    if (isNumber(beforeChar)
        && Objects.equals(beforeLast, " ")
        && (Objects.equals(afterChar, "ᄂ")
            || Objects.equals(afterChar, "ᄃ")
            || Objects.equals(afterChar, "ᄆ")
            || Objects.equals(afterChar, "ᄏ")
            || Objects.equals(afterChar, "ᄐ")
            || Objects.equals(afterChar, "ᄑ")
            || Objects.equals(afterChar, "ᄒ")
            || Objects.equals(afterStr, "운"))) {
      BrailleCommonUtils.performKeyAction(imeConnection.inputConnection, KeyEvent.KEYCODE_DEL);
    }
  }

  private static String appendDoubleFirstSymbol(
      StringBuilder current, String lastDot, String currentDot) {
    // if last dot is "456", it is not removed previous character
    if (current.length() > 0
        && !(Objects.equals(lastDot, "456")
            ||
            // "=" should be not removing about previous character
            (Objects.equals(lastDot, "25") && Objects.equals(currentDot, "25")))) {
      current.deleteCharAt(current.length() - 1);
    } else if (current.length() > 0
        && !(Objects.equals(lastDot, "25") && Objects.equals(currentDot, "25"))) {
      // if last dot "25-25", it is not removed previous character

      current.deleteCharAt(current.length() - 1);
    }
    String whole = combineDots(lastDot, currentDot);
    String result = OPEN_SYMBOL_TRANSLATION_MAP.get(whole);
    BrailleCommonLog.d(TAG, "appendDoubleFirstSymbol by " + whole + ": " + result);
    return result;
  }

  private static String appendDoubleMiddleSymbol(
      StringBuilder current, String lastDot, String curDot) {
    // if last dot is "456", it is not removed previous character
    if (current.length() > 0 && !Objects.equals(lastDot, "456")) {
      current.deleteCharAt(current.length() - 1);
    }
    String whole = combineDots(lastDot, curDot);
    String result = SYMBOL_TRANSLATION_MAP.get(whole);
    BrailleCommonLog.d(TAG, "appendDoubleMiddleSymbol by " + whole + ": " + result);
    return result;
  }

  private static boolean isWonDollarSymbol(String currentStr, String lastDot, String curDot) {
    boolean result = false;
    // ￦ symbol or 긔
    if (currentStr.length() < 2 && Objects.equals(lastDot, "4") && Objects.equals(curDot, "2456")) {
      result = true;
    } else if (currentStr.length() < 3
        && Objects.equals(lastDot, "4")
        && Objects.equals(curDot, "145")) {
      // $ symbol or ㄱㅍ
      result = true;
    }
    BrailleCommonLog.d(TAG, "isWonDallarSymbol " + currentStr + " : " + result);
    return result;
  }

  private static void getHangulIfPossible(
      StringBuilder current,
      BrailleWord fullWord,
      String lastDot,
      String inputDot,
      KoreanExceptionalCase exceptionalCase) {
    String lastChar = current.length() > 0 ? current.substring(current.length() - 1) : "";
    BrailleCommonLog.d(
        TAG,
        "getHangulIfPossible"
            + " fullWord: "
            + fullWord
            + " current: "
            + current
            + " lastDot: "
            + lastDot
            + " inputDot: "
            + inputDot
            + " lastChar: "
            + lastChar);
    exceptionalCase.processExceptionalCase(current, fullWord, lastChar, lastDot, inputDot);
  }

  private BrailleTranslateUtilsKorean() {}
}
