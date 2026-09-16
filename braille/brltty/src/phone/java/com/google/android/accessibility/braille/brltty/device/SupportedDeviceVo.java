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

/** Supported device info for BraillePen. */
public class SupportedDeviceVo extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("EL12-"));

  @Override
  public String driverCode() {
    return "vo";
  }

  @Override
  public boolean connectSecurely() {
    return true;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .dots6()
        .add("Shift", R.string.key_BP_Shift)
        .add("Space", R.string.key_Space)
        .add("Control", R.string.key_BP_Control)
        .add("JoystickLeft", R.string.key_JoystickLeft)
        .add("JoystickRight", R.string.key_JoystickRight)
        .add("JoystickUp", R.string.key_JoystickUp)
        .add("JoystickDown", R.string.key_JoystickDown)
        .add("JoystickEnter", R.string.key_JoystickCenter)
        .add("ScrollLeft", R.string.key_BP_ScrollLeft)
        .add("ScrollRight", R.string.key_BP_ScrollRight)
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
