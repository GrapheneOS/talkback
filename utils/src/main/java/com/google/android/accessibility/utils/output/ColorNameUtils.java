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

import static android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE;

import android.content.Context;
import android.graphics.Color;
import android.text.SpannableString;
import android.text.style.TtsSpan;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import com.google.android.accessibility.utils.R;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.HashMap;

/**
 * Utility class for color naming. The color group map is referenced from
 * https://www.w3schools.com/colors/colors_groups.asp.
 */
public final class ColorNameUtils {
  private static final String TAG = "ColorNameUtils";
  private static final double SIMILAR_COLOR_DIFFERENCE_THRESHOLD = 20;
  private static final HashMap<LabColor, Integer> colorNameMap;

  static {
    colorNameMap = new HashMap<>();
    // White group
    colorNameMap.put(newLabColor("#FFFFFF"), R.string.color_white); // White
    colorNameMap.put(newLabColor("#FFFAFA"), R.string.color_white); // Snow
    colorNameMap.put(newLabColor("#F0FFF0"), R.string.color_white); // HoneyDew
    colorNameMap.put(newLabColor("#F5FFFA"), R.string.color_white); // MintCream
    colorNameMap.put(newLabColor("#F0FFFF"), R.string.color_white); // Azure
    colorNameMap.put(newLabColor("#F0F8FF"), R.string.color_white); // AliceBlue
    colorNameMap.put(newLabColor("#F8F8FF"), R.string.color_white); // GhostWhite
    colorNameMap.put(newLabColor("#F5F5F5"), R.string.color_white); // WhiteSmoke
    colorNameMap.put(newLabColor("#FFF5EE"), R.string.color_white); // SeaShell
    colorNameMap.put(newLabColor("#F5F5DC"), R.string.color_white); // Beige
    colorNameMap.put(newLabColor("#FDF5E6"), R.string.color_white); // OldLace
    colorNameMap.put(newLabColor("#FFFAF0"), R.string.color_white); // FloralWhite
    colorNameMap.put(newLabColor("#FFFFF0"), R.string.color_white); // Ivory
    colorNameMap.put(newLabColor("#FAEBD7"), R.string.color_white); // AntiqueWhite
    colorNameMap.put(newLabColor("#FAF0E6"), R.string.color_white); // Linen
    colorNameMap.put(newLabColor("#FFF0F5"), R.string.color_white); // LavenderBlush
    colorNameMap.put(newLabColor("#FFE4E1"), R.string.color_white); // MistyRose
    // Grey group
    colorNameMap.put(newLabColor("#DCDCDC"), R.string.color_light_grey); // Gainsboro
    colorNameMap.put(newLabColor("#D3D3D3"), R.string.color_light_grey); // LightGray
    colorNameMap.put(newLabColor("#C0C0C0"), R.string.color_light_grey); // Silver
    colorNameMap.put(newLabColor("#808080"), R.string.color_grey); // Grey
    colorNameMap.put(newLabColor("#A9A9A9"), R.string.color_grey); // DarkGray
    colorNameMap.put(newLabColor("#778899"), R.string.color_grey); // LightSlateGray
    colorNameMap.put(newLabColor("#708090"), R.string.color_grey); // SlateGray
    colorNameMap.put(newLabColor("#2F4F4F"), R.string.color_dark_grey); // DarkSlateGray
    colorNameMap.put(newLabColor("#696969"), R.string.color_dark_grey); // DimGray
    colorNameMap.put(newLabColor("#000000"), R.string.color_black); // Black
    // Pink group
    colorNameMap.put(newLabColor("#FFC0CB"), R.string.color_pink); // Pink
    colorNameMap.put(newLabColor("#FFB6C1"), R.string.color_pink); // LightPink
    colorNameMap.put(newLabColor("#FF69B4"), R.string.color_pink); // HotPink
    colorNameMap.put(newLabColor("#FF1493"), R.string.color_pink); // DeepPink
    colorNameMap.put(newLabColor("#DB7093"), R.string.color_pink); // PaleVioletRed
    colorNameMap.put(newLabColor("#C71585"), R.string.color_pink); // MediumVioletRed
    // Purple group
    colorNameMap.put(newLabColor("#E6E6FA"), R.string.color_purple); // Lavender
    colorNameMap.put(newLabColor("#D8BFD8"), R.string.color_purple); // Thistle
    colorNameMap.put(newLabColor("#DDA0DD"), R.string.color_purple); // Plum
    colorNameMap.put(newLabColor("#DA70D6"), R.string.color_purple); // Orchid
    colorNameMap.put(newLabColor("#EE82EE"), R.string.color_purple); // Violet
    colorNameMap.put(newLabColor("#FF00FF"), R.string.color_purple); // Magenta
    colorNameMap.put(newLabColor("#BA55D3"), R.string.color_purple); // MediumOrchid
    colorNameMap.put(newLabColor("#9932CC"), R.string.color_purple); // DarkOrchid
    colorNameMap.put(newLabColor("#9400D3"), R.string.color_purple); // DarkViolet
    colorNameMap.put(newLabColor("#8A2BE2"), R.string.color_purple); // BlueViolet
    colorNameMap.put(newLabColor("#8B008B"), R.string.color_purple); // DarkMagenta
    colorNameMap.put(newLabColor("#800080"), R.string.color_purple); // Purple
    colorNameMap.put(newLabColor("#9370DB"), R.string.color_purple); // MediumPurple
    colorNameMap.put(newLabColor("#7B68EE"), R.string.color_purple); // MediumSlateBlue
    colorNameMap.put(newLabColor("#6A5ACD"), R.string.color_purple); // SlateBlue
    colorNameMap.put(newLabColor("#483D8B"), R.string.color_purple); // DarkSlateBlue
    colorNameMap.put(newLabColor("#663399"), R.string.color_purple); // RebeccaPurple
    colorNameMap.put(newLabColor("#4B0082"), R.string.color_purple); // Indigo
    // Red group
    colorNameMap.put(newLabColor("#FFA07A"), R.string.color_red); // LightSalmon
    colorNameMap.put(newLabColor("#FA8072"), R.string.color_red); // Salmon
    colorNameMap.put(newLabColor("#E9967A"), R.string.color_red); // DarkSalmon
    colorNameMap.put(newLabColor("#F08080"), R.string.color_red); // LightCoral
    colorNameMap.put(newLabColor("#CD5C5C"), R.string.color_red); // IndianRed
    colorNameMap.put(newLabColor("#DC143C"), R.string.color_red); // Crimson
    colorNameMap.put(newLabColor("#FF0000"), R.string.color_red); // Red
    colorNameMap.put(newLabColor("#B22222"), R.string.color_red); // FireBrick
    colorNameMap.put(newLabColor("#8B0000"), R.string.color_red); // DarkRed
    colorNameMap.put(
        newLabColor("#F44336"), R.string.color_red); // b/407906843: Red from Google Chat
    // Orange group
    colorNameMap.put(newLabColor("#FFA500"), R.string.color_orange); // Orange
    colorNameMap.put(newLabColor("#FF8C00"), R.string.color_orange); // DarkOrange
    colorNameMap.put(newLabColor("#FF7F50"), R.string.color_orange); // Coral
    colorNameMap.put(newLabColor("#FF6347"), R.string.color_orange); // Tomato
    colorNameMap.put(newLabColor("#FF4500"), R.string.color_orange); // OrangeRed
    // Yellow group
    colorNameMap.put(newLabColor("#FFD700"), R.string.color_yellow); // Gold
    colorNameMap.put(newLabColor("#FFFF00"), R.string.color_yellow); // Yellow
    colorNameMap.put(newLabColor("#FFFFE0"), R.string.color_yellow); // LightYellow
    colorNameMap.put(newLabColor("#FFFACD"), R.string.color_yellow); // LemonChiffon
    colorNameMap.put(newLabColor("#FAFAD2"), R.string.color_yellow); // LightGoldenRodYellow
    colorNameMap.put(newLabColor("#FFEFD5"), R.string.color_yellow); // PapayaWhip
    colorNameMap.put(newLabColor("#FFE4B5"), R.string.color_yellow); // Moccasin
    colorNameMap.put(newLabColor("#FFDAB9"), R.string.color_yellow); // PeachPuff
    colorNameMap.put(newLabColor("#EEE8AA"), R.string.color_yellow); // PaleGoldenRod
    colorNameMap.put(newLabColor("#F0E68C"), R.string.color_yellow); // Khaki
    colorNameMap.put(newLabColor("#BDB76B"), R.string.color_yellow); // DarkKhaki
    colorNameMap.put(newLabColor("#ABC800"), R.string.color_yellow_green);
    // Green group
    colorNameMap.put(newLabColor("#ADFF2F"), R.string.color_green); // GreenYellow
    colorNameMap.put(newLabColor("#7FFF00"), R.string.color_green); // Chartreuse
    colorNameMap.put(newLabColor("#7CFC00"), R.string.color_green); // LawnGreen
    colorNameMap.put(newLabColor("#00FF00"), R.string.color_green); // Lime
    colorNameMap.put(newLabColor("#32CD32"), R.string.color_green); // LimeGreen
    colorNameMap.put(newLabColor("#98FB98"), R.string.color_green); // PaleGreen
    colorNameMap.put(newLabColor("#90EE90"), R.string.color_green); // LightGreen
    colorNameMap.put(newLabColor("#00FA9A"), R.string.color_green); // MediumSpringGreen
    colorNameMap.put(newLabColor("#00FF7F"), R.string.color_green); // SpringGreen
    colorNameMap.put(newLabColor("#3CB371"), R.string.color_green); // MediumSeaGreen
    colorNameMap.put(newLabColor("#2E8B57"), R.string.color_green); // SeaGreen
    colorNameMap.put(newLabColor("#228B22"), R.string.color_green); // ForestGreen
    colorNameMap.put(newLabColor("#008000"), R.string.color_green); // Green
    colorNameMap.put(newLabColor("#006400"), R.string.color_green); // DarkGreen
    colorNameMap.put(newLabColor("#9ACD32"), R.string.color_green); // YellowGreen
    colorNameMap.put(newLabColor("#6B8E23"), R.string.color_green); // OliveDrab
    colorNameMap.put(newLabColor("#556B2F"), R.string.color_green); // DarkOliveGreen
    colorNameMap.put(newLabColor("#66CDAA"), R.string.color_green); // MediumAquaMarine
    colorNameMap.put(newLabColor("#8FBC8F"), R.string.color_green); // DarkSeaGreen
    colorNameMap.put(newLabColor("#20B2AA"), R.string.color_green); // LightSeaGreen
    colorNameMap.put(newLabColor("#008B8B"), R.string.color_green); // DarkCyan
    colorNameMap.put(newLabColor("#008080"), R.string.color_green); // Teal
    // Cyan group
    colorNameMap.put(newLabColor("#00FFFF"), R.string.color_cyan); // Cyan
    colorNameMap.put(newLabColor("#E0FFFF"), R.string.color_cyan); // LightCyan
    colorNameMap.put(newLabColor("#AFEEEE"), R.string.color_cyan); // PaleTurquoise
    colorNameMap.put(newLabColor("#7FFFD4"), R.string.color_cyan); // Aquamarine
    colorNameMap.put(newLabColor("#40E0D0"), R.string.color_cyan); // Turquoise
    colorNameMap.put(newLabColor("#48D1CC"), R.string.color_cyan); // MediumTurquoise
    colorNameMap.put(newLabColor("#00CED1"), R.string.color_cyan); // DarkTurquoise
    // Blue group
    colorNameMap.put(newLabColor("#5F9EA0"), R.string.color_blue); // CadetBlue
    colorNameMap.put(newLabColor("#4682B4"), R.string.color_blue); // SteelBlue
    colorNameMap.put(newLabColor("#B0C4DE"), R.string.color_blue); // LightSteelBlue
    colorNameMap.put(newLabColor("#ADD8E6"), R.string.color_blue); // LightBlue
    colorNameMap.put(newLabColor("#B0E0E6"), R.string.color_blue); // PowderBlue
    colorNameMap.put(newLabColor("#87CEFA"), R.string.color_blue); // LightSkyBlue
    colorNameMap.put(newLabColor("#87CEEB"), R.string.color_blue); // SkyBlue
    colorNameMap.put(newLabColor("#6495ED"), R.string.color_blue); // CornflowerBlue
    colorNameMap.put(newLabColor("#00BFFF"), R.string.color_blue); // DeepSkyBlue
    colorNameMap.put(newLabColor("#1E90FF"), R.string.color_blue); // DodgerBlue
    colorNameMap.put(newLabColor("#4169E1"), R.string.color_blue); // RoyalBlue
    colorNameMap.put(newLabColor("#0000FF"), R.string.color_blue); // Blue
    colorNameMap.put(newLabColor("#0000CD"), R.string.color_blue); // MediumBlue
    colorNameMap.put(newLabColor("#00008B"), R.string.color_blue); // DarkBlue
    colorNameMap.put(newLabColor("#000080"), R.string.color_blue); // Navy
    colorNameMap.put(newLabColor("#191970"), R.string.color_blue); // MidnightBlue
    // Brown group
    colorNameMap.put(newLabColor("#FFF8DC"), R.string.color_brown); // Cornsilk
    colorNameMap.put(newLabColor("#FFEBCD"), R.string.color_brown); // BlanchedAlmond
    colorNameMap.put(newLabColor("#FFE4C4"), R.string.color_brown); // Bisque
    colorNameMap.put(newLabColor("#FFDEAD"), R.string.color_brown); // NavajoWhite
    colorNameMap.put(newLabColor("#F5DEB3"), R.string.color_brown); // Wheat
    colorNameMap.put(newLabColor("#DEB887"), R.string.color_brown); // BurlyWood
    colorNameMap.put(newLabColor("#D2B48C"), R.string.color_brown); // Tan
    colorNameMap.put(newLabColor("#BC8F8F"), R.string.color_brown); // RosyBrown
    colorNameMap.put(newLabColor("#F4A460"), R.string.color_brown); // SandyBrown
    colorNameMap.put(newLabColor("#DAA520"), R.string.color_brown); // GoldenRod
    colorNameMap.put(newLabColor("#B8860B"), R.string.color_brown); // DarkGoldenRod
    colorNameMap.put(newLabColor("#CD853F"), R.string.color_brown); // Peru
    colorNameMap.put(newLabColor("#D2691E"), R.string.color_brown); // Chocolate
    colorNameMap.put(newLabColor("#808000"), R.string.color_olive); // Olive
    colorNameMap.put(newLabColor("#8B4513"), R.string.color_brown); // SaddleBrown
    colorNameMap.put(newLabColor("#A0522D"), R.string.color_brown); // Sienna
    colorNameMap.put(newLabColor("#A52A2A"), R.string.color_brown); // Brown
    colorNameMap.put(newLabColor("#800000"), R.string.color_brown); // Maroon
  }

  /**
   * Finds the nearest color from the given color list. The algorithm is referenced from
   * https://en.wikipedia.org/wiki/Color_difference#CIE76.
   */
  @Nullable
  private static LabColor findNearestColor(LabColor color, Iterable<LabColor> colors) {
    LabColor nearestColor = null;
    double minDistance = Double.MAX_VALUE;
    for (LabColor toColor : colors) {
      double distance = getLabColorDifference(color, toColor);
      if (distance == 0) {
        return toColor;
      }
      if (distance < minDistance) {
        minDistance = distance;
        nearestColor = toColor;
      }
    }
    if (minDistance >= SIMILAR_COLOR_DIFFERENCE_THRESHOLD) {
      LogUtils.w(TAG, "%s isn't similar to %s with distance =%f", color, nearestColor, minDistance);
    }
    return nearestColor;
  }

  private static double getLabColorDifference(LabColor color1, LabColor color2) {
    return Math.sqrt(
        (color1.getL() - color2.getL()) * (color1.getL() - color2.getL())
            + (color1.getA() - color2.getA()) * (color1.getA() - color2.getA())
            + (color1.getB() - color2.getB()) * (color1.getB() - color2.getB()));
  }

  private static LabColor newLabColor(String rgbHexString) {
    int colorInt = Color.parseColor(rgbHexString);
    return newLabColor(colorInt);
  }

  private static LabColor newLabColor(int colorInt) {
    int r = Color.red(colorInt);
    int g = Color.green(colorInt);
    int b = Color.blue(colorInt);

    double[] lab = new double[3];
    ColorUtils.RGBToLAB(r, g, b, lab);
    return new LabColor(lab[0], lab[1], lab[2], colorInt);
  }

  /**
   * Generates the color name for the given color. Returns the nearest color if matched, otherwise
   * return the RGB value.
   */
  public static String getColorName(Context context, int color) {
    return context.getString(getColorGroup(color));
  }

  /** Returns the color group resource id for the given color. */
  public static int getColorGroup(int color) {
    LabColor nearestColor = findNearestColor(newLabColor(color), colorNameMap.keySet());
    return colorNameMap.get(nearestColor);
  }

  /**
   * Generates the color hex value for the given color and uses TtsSpan to speak the value one by
   * one.
   */
  public static CharSequence getColorHexValue(Context context, int color) {
    String hexColor =
        String.format("#%02X%02X%02X", Color.red(color), Color.green(color), Color.blue(color));
    SpannableString spannableString = new SpannableString(hexColor);
    TtsSpan hashSpan =
        new TtsSpan.TextBuilder().setText(context.getString(R.string.symbol_hash)).build();
    spannableString.setSpan(hashSpan, 0, 1, SPAN_EXCLUSIVE_EXCLUSIVE);
    TtsSpan digitsSpan = new TtsSpan.DigitsBuilder().setDigits(hexColor.substring(1)).build();
    spannableString.setSpan(digitsSpan, 1, hexColor.length(), SPAN_EXCLUSIVE_EXCLUSIVE);
    return spannableString;
  }

  private ColorNameUtils() {}

  /** Utility class for Lab color. */
  static class LabColor {
    private final double l;
    private final double a;
    private final double b;
    private final int colorInt;

    public LabColor(double l, double a, double b, int colorInt) {
      this.l = l;
      this.a = a;
      this.b = b;
      this.colorInt = colorInt;
    }

    public double getL() {
      return l;
    }

    public double getA() {
      return a;
    }

    public double getB() {
      return b;
    }

    @Override
    public String toString() {
      return "LabColor(int=" + colorInt + ")";
    }
  }
}
