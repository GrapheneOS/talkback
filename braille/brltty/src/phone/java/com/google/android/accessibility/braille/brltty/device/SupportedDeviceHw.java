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

/**
 * Supported device info for Brailliant. Secure connections currently fail on Android devices for
 * the Brailliant.
 */
public class SupportedDeviceHw extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(
          Pattern.compile("Brailliant B"),
          Pattern.compile("APH Mantis"),
          Pattern.compile("APH Chameleon"),
          Pattern.compile("NLS eReader H"),
          Pattern.compile("Humanware BrailleOne"),
          Pattern.compile("BrailleNote Touch"));
  private static final ImmutableSet<Pair<Integer, Integer>> VENDOR_PROD_IDS =
      ImmutableSet.of(
          // HumanWare [Brailliant BI 32/40, Brailliant B 80 (serial protocol)]
          new Pair<>(0X1C71, 0XC005),
          // HumanWare [Brailliant BI 14 (serial protocol)]
          new Pair<>(0X1C71, 0XC021),
          // HumanWare [APH Chameleon 20 (serial protocol)]
          new Pair<>(0X1C71, 0XC104),
          // HumanWare [APH Mantis Q40 (serial protocol)]
          new Pair<>(0X1C71, 0XC114),
          // HumanWare [Humanware BrailleOne (serial protocol)]
          new Pair<>(0X1C71, 0XC124),
          // HumanWare [NLS eReader (serial protocol)]
          new Pair<>(0X1C71, 0XCE04));

  @Override
  public String driverCode() {
    return "hw";
  }

  @Override
  public boolean connectSecurely() {
    return false;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .dots8()
        .add("Left", R.string.key_JoystickLeft)
        .add("Right", R.string.key_JoystickRight)
        .add("Up", R.string.key_JoystickUp)
        .add("Down", R.string.key_JoystickDown)
        .add("Press", R.string.key_JoystickCenter)
        .add("ThumbLeft", R.string.key_pan_left)
        .add("ThumbRight", R.string.key_pan_right)
        .routing()
        .add("Space", R.string.key_Space)
        .add("Power", R.string.key_brailliant_Power)
        .add("Display1", R.string.key_brailliant_Display1)
        .add("Display2", R.string.key_brailliant_Display2)
        .add("Display3", R.string.key_brailliant_Display3)
        .add("Display4", R.string.key_brailliant_Display4)
        .add("Display5", R.string.key_brailliant_Display5)
        .add("Display6", R.string.key_brailliant_Display6)
        .add("Thumb1", R.string.key_brailliant_Thumb1)
        .add("Thumb2", R.string.key_brailliant_Thumb2)
        .add("Thumb3", R.string.key_brailliant_Thumb3)
        .add("Thumb4", R.string.key_brailliant_Thumb4)
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
