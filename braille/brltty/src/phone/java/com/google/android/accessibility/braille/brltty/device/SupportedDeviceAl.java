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

/** Supported device info for Alva BC640/BC680. */
public class SupportedDeviceAl extends SupportedDevice {
  private static final ImmutableList<Pattern> NAME_REGEXES =
      ImmutableList.of(Pattern.compile("Alva BC", Pattern.CASE_INSENSITIVE));
  private static final ImmutableSet<Pair<Integer, Integer>> VENDOR_PROD_IDS =
      ImmutableSet.of(
          // Alva [BC624]
          new Pair<>(0X0798, 0X0624),
          // Alva [BC640]
          new Pair<>(0X0798, 0X0640),
          // Alva [BC680]
          new Pair<>(0X0798, 0X0680));

  @Override
  public String driverCode() {
    return "al";
  }

  @Override
  public boolean connectSecurely() {
    return false;
  }

  @Override
  public Map<String, Integer> friendlyKeyNames() {
    return new KeyNameMapBuilder()
        // No braille dot keys.
        .add("ETouchLeftRear", R.string.key_albc_ETouchLeftRear)
        .add("ETouchRightRear", R.string.key_albc_ETouchRightRear)
        .add("ETouchLeftFront", R.string.key_albc_ETouchLeftFront)
        .add("ETouchRightFront", R.string.key_albc_ETouchRightFront)
        .add("SmartpadF1", R.string.key_albc_SmartpadF1)
        .add("SmartpadF2", R.string.key_albc_SmartpadF2)
        .add("SmartpadF3", R.string.key_albc_SmartpadF3)
        .add("SmartpadF4", R.string.key_albc_SmartpadF4)
        .add("SmartpadUp", R.string.key_albc_SmartpadUp)
        .add("SmartpadDown", R.string.key_albc_SmartpadDown)
        .add("SmartpadLeft", R.string.key_albc_SmartpadLeft)
        .add("SmartpadRight", R.string.key_albc_SmartpadRight)
        .add("SmartpadEnter", R.string.key_albc_SmartpadEnter)
        .add("ThumbLeft", R.string.key_albc_ThumbLeft)
        .add("ThumbRight", R.string.key_albc_ThumbRight)
        .add("ThumbUp", R.string.key_albc_ThumbUp)
        .add("ThumbDown", R.string.key_albc_ThumbDown)
        .add("ThumbHome", R.string.key_albc_ThumbHome)
        .add("RoutingKey1", R.string.key_Routing)
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
