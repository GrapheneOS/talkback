/*
 * Copyright (C) 2023 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.braille.brltty;

import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.brltty.device.Finder;
import com.google.android.accessibility.braille.brltty.device.SupportedDevice;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceAl;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceBm;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceBmOldBrailliant;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceBmOrbit;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceBmVarioConnect;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceBmVarioUltra;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceEu;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceFs;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceHid;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceHm;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceHmBrailleSense;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceHt;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceHw;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceMm;
import com.google.android.accessibility.braille.brltty.device.SupportedDevicePm;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceSk;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceSkDisplay;
import com.google.android.accessibility.braille.brltty.device.SupportedDeviceVo;
import com.google.common.collect.ImmutableList;

/** Helper class maps device name patterns to device-related data. */
public class SupportedDevicesHelper {
  private static final ImmutableList<SupportedDevice> supportedDevices =
      ImmutableList.of(
          new SupportedDeviceVo(),
          new SupportedDeviceEu(),
          new SupportedDeviceFs(),
          new SupportedDeviceHw(),
          new SupportedDeviceHm(),
          new SupportedDeviceHmBrailleSense(),
          new SupportedDeviceBm(),
          new SupportedDeviceBmOrbit(),
          new SupportedDeviceBmVarioConnect(),
          new SupportedDeviceBmVarioUltra(),
          new SupportedDeviceBmOldBrailliant(),
          new SupportedDevicePm(),
          new SupportedDeviceAl(),
          new SupportedDeviceHt(),
          new SupportedDeviceSk(),
          new SupportedDeviceSkDisplay(),
          new SupportedDeviceMm(),
          new SupportedDeviceHid());
  private static final String HID_STUB_DEVICE_NAME = "HID";

  private SupportedDevicesHelper() {}

  @Nullable
  public static DeviceInfo getDeviceInfo(String deviceName, boolean useHid) {
    for (Finder supportedDevice : supportedDevices) {
      DeviceInfo deviceInfo = supportedDevice.match(useHid ? HID_STUB_DEVICE_NAME : deviceName);
      if (deviceInfo != null) {
        return deviceInfo;
      }
    }
    return null;
  }

  @Nullable
  public static DeviceInfo getDeviceInfoById(int vendorId, int prodId) {
    for (Finder supportedDevice : supportedDevices) {
      DeviceInfo deviceInfo = supportedDevice.matchById(vendorId, prodId);
      if (deviceInfo != null) {
        return deviceInfo;
      }
    }
    return null;
  }
}
