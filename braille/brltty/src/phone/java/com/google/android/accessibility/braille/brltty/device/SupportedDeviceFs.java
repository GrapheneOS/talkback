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

/** Supported device info for Freedom Scientific Focus blue displays. */
public class SupportedDeviceFs extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("Focus (5|40|14|80) (BT|HID)"), Pattern.compile("FOCUS"));
  private static final ImmutableSet<Pair<Integer, Integer>> VENDOR_PROD_IDS =
      ImmutableSet.of(
          // Focus 1.
          new Pair<>(0xF4E, 0x100),
          // Pacmate.
          new Pair<>(0xF4E, 0x111),
          // Focus 2.
          new Pair<>(0xF4E, 0x112),
          // Focus 3.
          new Pair<>(0xF4E, 0x114));

  @Override
  public String driverCode() {
    return "fs";
  }

  @Override
  public boolean connectSecurely() {
    return true;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .dots8()
        .add("Space", R.string.key_Space)
        .add("PanLeft", R.string.key_pan_left)
        .add("PanRight", R.string.key_pan_right)
        .add("LeftWheelPress", R.string.key_focus_LeftWheelPress)
        .add("LeftWheelDown", R.string.key_focus_LeftWheelDown)
        .add("LeftWheelUp", R.string.key_focus_LeftWheelUp)
        .add("RightWheelPress", R.string.key_focus_RightWheelPress)
        .add("RightWheelDown", R.string.key_focus_RightWheelDown)
        .add("RightWheelUp", R.string.key_focus_RightWheelUp)
        .routing()
        .add("LeftShift", R.string.key_focus_LeftShift)
        .add("RightShift", R.string.key_focus_RightShift)
        .add("LeftGdf", R.string.key_focus_LeftGdf)
        .add("RightGdf", R.string.key_focus_RightGdf)
        .add("LeftRockerUp", R.string.key_focus_LeftRockerUp)
        .add("LeftRockerDown", R.string.key_focus_LeftRockerDown)
        .add("RightRockerUp", R.string.key_focus_RightRockerUp)
        .add("RightRockerDown", R.string.key_focus_RightRockerDown)
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
