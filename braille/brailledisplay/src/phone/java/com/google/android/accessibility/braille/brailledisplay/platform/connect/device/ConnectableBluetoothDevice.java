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

package com.google.android.accessibility.braille.brailledisplay.platform.connect.device;

import android.bluetooth.BluetoothDevice;
import android.os.Build;
import android.text.TextUtils;
import com.google.auto.value.AutoValue;

/** Bluetooth connectable device. */
@AutoValue
public abstract class ConnectableBluetoothDevice extends ConnectableDevice {

  public abstract BluetoothDevice bluetoothDevice();

  @Override
  public String name() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      return bluetoothDevice().getAlias();
    }
    return bluetoothDevice().getName();
  }

  @Override
  public String address() {
    return bluetoothDevice().getAddress();
  }

  /** Returns a string with masked name and masked address. */
  @Override
  public final String toString() {
    String address = address();
    if (!TextUtils.isEmpty(address)) {
      char[] masked = address().toCharArray();
      for (int i = 0; i < masked.length - 5; i++) {
        if (masked[i] != ':') {
          masked[i] = 'x';
        }
      }
      address = new String(masked);
    }
    return truncatedName() + "(" + address + ")";
  }

  public static ConnectableBluetoothDevice.Builder builder() {
    return new AutoValue_ConnectableBluetoothDevice.Builder();
  }

  /** Builder for {@code ConnectableBluetoothDevice}. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setBluetoothDevice(BluetoothDevice device);

    public abstract ConnectableBluetoothDevice build();
  }
}
