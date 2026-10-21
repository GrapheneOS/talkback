package com.google.android.accessibility.braille.brltty.device;

import android.util.Pair;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.brltty.DeviceInfo;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Interface for the supported device info. */
public abstract class SupportedDevice implements Finder {

  /** The driver code of the device. */
  public abstract String driverCode();

  /** Whether the device should be connected securely. */
  public abstract boolean connectSecurely();

  /** The friendly key names of the device. */
  public abstract Map<String, Integer> friendlyKeyNames();

  /** The vendor and product IDs of the device. */
  public abstract Set<Pair<Integer, Integer>> vendorProdIds();

  /** The regexes of the device name. */
  public abstract List<Pattern> nameRegexes();

  @Nullable
  @Override
  public DeviceInfo match(String deviceName) {
    for (Pattern nameRegex : nameRegexes()) {
      if (nameRegex.matcher(deviceName).lookingAt()) {
        return DeviceInfo.builder()
            .setDriverCode(driverCode())
            .setType(DeviceInfo.Type.BLUETOOTH)
            .setTruncatedName(nameRegex.toString())
            .setFriendlyKeyNames(friendlyKeyNames())
            .setConnectSecurely(connectSecurely())
            .build();
      }
    }
    return null;
  }

  @Nullable
  @Override
  public DeviceInfo matchById(int vendorId, int prodId) {
    if (vendorProdIds().contains(new Pair<>(vendorId, prodId))) {
      return DeviceInfo.builder()
          .setDriverCode(driverCode())
          .setType(DeviceInfo.Type.USB)
          .setTruncatedName(String.format(Locale.ENGLISH, "%d:%d", vendorId, prodId))
          .setFriendlyKeyNames(friendlyKeyNames())
          .setConnectSecurely(connectSecurely())
          .build();
    }

    return null;
  }

  @Override
  public String toString() {
    StringBuilder s = new StringBuilder();
    s.append(driverCode());
    for (Pattern p : nameRegexes()) {
      s.append(" ").append(p);
    }
    return s.toString();
  }
}
