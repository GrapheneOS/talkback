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

/** Supported device info for Baum VarioUltra. */
public class SupportedDeviceBmVarioUltra extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("VarioUltra"));

  @Override
  public String driverCode() {
    return "bm";
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
        .routing()
        .add("Display2", R.string.key_APH_AdvanceLeft)
        .add("Display5", R.string.key_APH_AdvanceRight)
        .add("B9", R.string.key_Space)
        .add("B10", R.string.key_Space)
        .build();
  }

  @Override
  public Set<Pair<Integer, Integer>> vendorProdIds() {
    return ImmutableSet.of();
  }

  @Override
  public List<Pattern> nameRegexes() {
    return NAME_REGEXES;
  }
}
