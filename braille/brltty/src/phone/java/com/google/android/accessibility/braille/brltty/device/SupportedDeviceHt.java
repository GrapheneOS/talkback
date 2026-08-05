package com.google.android.accessibility.braille.brltty.device;

import android.util.Pair;
import com.google.android.accessibility.braille.brltty.KeyNameMapBuilder;
import com.google.android.accessibility.braille.brltty.R;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Supported device info for HandyTech displays. */
public class SupportedDeviceHt extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(
          Pattern.compile(
              "(Braille Wave( BRW)?|Braillino( BL2)?|Braille Star 40( BS4)?|Easy Braille("
                  + " EBR)?|Active Braille( AB4)?|Basic Braille"
                  + " BB[3,4,6]?)\\/[a-zA-Z][0-9]-[0-9]{5}|Actilino|Activator"),
          Pattern.compile("(BRW|BL2|BS4|EBR|AB4|BB(3|4|6)?)\\/[a-zA-Z][0-9]-[0-9]{5}"));
  private static final ImmutableSet<Pair<Integer, Integer>> VENDOR_PROD_IDS =
      ImmutableSet.of(
          // HandyTech [GoHubs chip]
          new Pair<>(0X0921, 0X1200),
          // HandyTech [Active Braille]
          new Pair<>(0X1FE4, 0X0054),
          // HandyTech [Connect Braille 40]
          new Pair<>(0X1FE4, 0X0055),
          // HandyTech [Actilino]
          new Pair<>(0X1FE4, 0X0061),
          // HandyTech [Active Star 40]
          new Pair<>(0X1FE4, 0X0064),
          // HandyTech [Basic Braille 16]
          new Pair<>(0X1FE4, 0X0081),
          // HandyTech [Basic Braille 20]
          new Pair<>(0X1FE4, 0X0082),
          // HandyTech [Basic Braille 32]
          new Pair<>(0X1FE4, 0X0083),
          // HandyTech [Basic Braille 40]
          new Pair<>(0X1FE4, 0X0084),
          // HandyTech [Basic Braille 64]
          new Pair<>(0X1FE4, 0X0086),
          // HandyTech [Basic Braille 80]
          new Pair<>(0X1FE4, 0X0087),
          // HandyTech [Basic Braille 48]
          new Pair<>(0X1FE4, 0X008A),
          // HandyTech [Basic Braille 160]
          new Pair<>(0X1FE4, 0X008B),
          // HandyTech [Activator]
          new Pair<>(0X1FE4, 0X00A4));

  @Override
  public String driverCode() {
    return "ht";
  }

  @Override
  public boolean connectSecurely() {
    return true;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .add("B4", R.string.key_Dot1)
        .add("B3", R.string.key_Dot2)
        .add("B2", R.string.key_Dot3)
        .add("B1", R.string.key_Dot7)
        .add("B5", R.string.key_Dot4)
        .add("B6", R.string.key_Dot5)
        .add("B7", R.string.key_Dot6)
        .add("B8", R.string.key_Dot8)
        .routing()
        .add("LeftRockerTop", R.string.key_handytech_LeftTrippleActionTop)
        .add("LeftRockerBottom", R.string.key_handytech_LeftTrippleActionBottom)
        .add("LeftRockerTop+LeftRockerBottom", R.string.key_handytech_LeftTrippleActionMiddle)
        .add("RightRockerTop", R.string.key_handytech_RightTrippleActionTop)
        .add("RightRockerBottom", R.string.key_handytech_RightTrippleActionBottom)
        .add("RightRockerTop+RightRockerBottom", R.string.key_handytech_RightTrippleActionMiddle)
        .add("SpaceLeft", R.string.key_handytech_LeftSpace)
        .add("SpaceRight", R.string.key_handytech_RightSpace)
        .add("Display1", R.string.key_hwg_brailliant_Display1)
        .add("Display2", R.string.key_hwg_brailliant_Display2)
        .add("Display3", R.string.key_hwg_brailliant_Display3)
        .add("Display4", R.string.key_hwg_brailliant_Display4)
        .add("Display5", R.string.key_hwg_brailliant_Display5)
        .add("Display6", R.string.key_hwg_brailliant_Display6)
        .build();
  }

  @Override
  public Set<Pair<Integer, Integer>> vendorProdIds() {
    return VENDOR_PROD_IDS;
  }

  @Override
  public List<Pattern> nameRegexes() {
    return NAME_REGEXES;
  }
}
