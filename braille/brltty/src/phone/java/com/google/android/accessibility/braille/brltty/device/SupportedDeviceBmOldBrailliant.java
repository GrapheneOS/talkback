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
 * Supported device info for older Brailliant, from Humanware group. Uses Baum protocol. No Braille
 * keyboard on this one. Secure connections currently fail on Android devices with this display..
 */
public class SupportedDeviceBmOldBrailliant extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("HWG Brailliant"), Pattern.compile("NLS eReader Z"));
  private static final ImmutableSet<Pair<Integer, Integer>> VENDOR_PROD_IDS =
      ImmutableSet.of(
          // Baum [Brailliant2 40 (40 cells)]
          new Pair<>(0X0904, 0X6006),
          // Baum [Brailliant2 24 (24 cells)]
          new Pair<>(0X0904, 0X6007),
          // Baum [Brailliant2 32 (32 cells)]
          new Pair<>(0X0904, 0X6008),
          // Baum [Brailliant2 64 (64 cells)]
          new Pair<>(0X0904, 0X6009),
          // Baum [Brailliant2 80 (80 cells)]
          new Pair<>(0X0904, 0X600A));

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
        .add("Display1", R.string.key_hwg_brailliant_Display1)
        .add("Display2", R.string.key_hwg_brailliant_Display2)
        .add("Display3", R.string.key_hwg_brailliant_Display3)
        .add("Display4", R.string.key_hwg_brailliant_Display4)
        .add("Display5", R.string.key_hwg_brailliant_Display5)
        .add("Display6", R.string.key_hwg_brailliant_Display6)
        .routing()
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
