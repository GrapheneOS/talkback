package com.google.android.accessibility.braille.brltty.device;

import com.google.android.accessibility.braille.brltty.DeviceInfo;

/** Interface for finding a device info by device name or vendor and product IDs. */
public interface Finder {
  DeviceInfo match(String deviceName);

  DeviceInfo matchById(int vendorId, int prodId);
}
