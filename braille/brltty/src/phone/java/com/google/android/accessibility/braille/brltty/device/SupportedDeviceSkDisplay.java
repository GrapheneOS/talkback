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

/** Supported device info for Seika Braille Display. No Braille keys on this display. */
public class SupportedDeviceSkDisplay extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("TS5"));

  @Override
  public String driverCode() {
    return "sk";
  }

  @Override
  public boolean connectSecurely() {
    return true;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .add("K1", R.string.key_skbdp_PanLeft)
        .add("K8", R.string.key_skbdp_PanRight)
        .add("K2", R.string.key_skbdp_LeftRockerLeft)
        .add("K3", R.string.key_skbdp_LeftRockerRight)
        .add("K4", R.string.key_skbdp_LeftLongKey)
        .add("K5", R.string.key_skbdp_RightLongKey)
        .add("K6", R.string.key_skbdp_RightRockerLeft)
        .add("K7", R.string.key_skbdp_RightRockerRight)
        .add("RoutingKey2", R.string.key_Routing)
        .routing()
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
