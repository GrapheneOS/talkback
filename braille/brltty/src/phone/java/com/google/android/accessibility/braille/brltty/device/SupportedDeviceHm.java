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

/** Supported device info for HIMS displays. */
public class SupportedDeviceHm extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("Hansone|HansoneXL|SmartBeetle"));
  private static final ImmutableSet<Pair<Integer, Integer>> VENDOR_PROD_IDS =
      ImmutableSet.of(
          // HIMS [Braille Sense (USB 1.1)]
          // HIMS [Braille Sense (USB 2.0)]
          // HIMS [Braille Sense U2 (USB 2.0)]
          // HIMS [BrailleSense 6 (USB 2.1)]
          new Pair<>(0X045E, 0X930A),
          // HIMS [Braille Edge and QBrailleXL]
          new Pair<>(0X045E, 0X930B));

  @Override
  public String driverCode() {
    return "hm";
  }

  @Override
  public boolean connectSecurely() {
    return false;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        .dots8()
        .routing()
        .add("Space", R.string.key_Space)
        .add("F1", R.string.key_F1)
        .add("F2", R.string.key_F2)
        .add("F3", R.string.key_F3)
        .add("F4", R.string.key_F4)
        .add("Backward", R.string.key_Backward)
        .add("Forward", R.string.key_Forward)
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
