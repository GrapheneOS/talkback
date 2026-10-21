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

/** Supported device info for Braillex Trio. */
public class SupportedDevicePm extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("braillex trio"));

  @Override
  public String driverCode() {
    return "pm";
  }

  @Override
  public boolean connectSecurely() {
    return true;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .dots8()
        .add("LeftSpace", R.string.key_Space)
        .add("RightSpace", R.string.key_Space)
        .add("Space", R.string.key_Space)
        .add("LeftThumb", R.string.key_braillex_LeftThumb)
        .add("RightThumb", R.string.key_braillex_RightThumb)
        .add("RoutingKey1", R.string.key_Routing)
        .add("BarLeft1", R.string.key_braillex_BarLeft1)
        .add("BarLeft2", R.string.key_braillex_BarLeft2)
        .add("BarRight1", R.string.key_braillex_BarRight1)
        .add("BarRight2", R.string.key_braillex_BarRight2)
        .add("BarUp1", R.string.key_braillex_BarUp1)
        .add("BarUp2", R.string.key_braillex_BarUp2)
        .add("BarDown1", R.string.key_braillex_BarDown1)
        .add("BarDown2", R.string.key_braillex_BarDown2)
        .add("LeftKeyRear", R.string.key_braillex_LeftKeyRear)
        .add("LeftKeyFront", R.string.key_braillex_LeftKeyFront)
        .add("RightKeyRear", R.string.key_braillex_RightKeyRear)
        .add("RightKeyFront", R.string.key_braillex_RightKeyFront)
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
