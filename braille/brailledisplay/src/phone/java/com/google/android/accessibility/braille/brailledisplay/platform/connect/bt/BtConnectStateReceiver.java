/*
 * Copyright 2021 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.braille.brailledisplay.platform.connect.bt;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import com.google.android.accessibility.braille.common.lib.ActionReceiver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** A BroadcastReceiver that listens for the bonding of this device with a bluetooth device. */
public class BtConnectStateReceiver
    extends ActionReceiver<BtConnectStateReceiver, BtConnectStateReceiver.Callback> {

  /** The callback associated with the actions of this receiver. */
  public interface Callback {
    void onBonded(BluetoothDevice device);

    void onUnBonded(BluetoothDevice device);

    void onConnected(BluetoothDevice device);

    void onDisconnected(BluetoothDevice device);

    void onUpdated(BluetoothDevice device);
  }

  public BtConnectStateReceiver(Context context, Callback callback) {
    super(context, callback);
  }

  @Override
  protected void onReceive(Callback callback, String action, Bundle extras) {
    BluetoothDevice bluetoothDevice = extras.getParcelable(BluetoothDevice.EXTRA_DEVICE);
    if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
      int bondState = extras.getInt(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE);
      if (bondState == BluetoothDevice.BOND_BONDED) {
        callback.onBonded(bluetoothDevice);
      } else if (bondState == BluetoothDevice.BOND_NONE) {
        callback.onUnBonded(bluetoothDevice);
      }
    } else if (Objects.equals(action, BluetoothDevice.ACTION_ACL_CONNECTED)) {
      callback.onConnected(bluetoothDevice);
    } else if (Objects.equals(action, BluetoothDevice.ACTION_ACL_DISCONNECTED)) {
      callback.onDisconnected(bluetoothDevice);
    } else if (Objects.equals(action, BluetoothDevice.ACTION_NAME_CHANGED)
        || (Build.VERSION.SDK_INT >= VERSION_CODES.R
            && Objects.equals(action, BluetoothDevice.ACTION_ALIAS_CHANGED))) {
      callback.onUpdated(bluetoothDevice);
    }
  }

  @Override
  protected String[] getActionsList() {
    List<String> actions =
        new ArrayList<>(
            Arrays.asList(
                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED,
                BluetoothDevice.ACTION_ACL_DISCONNECTED,
                BluetoothDevice.ACTION_NAME_CHANGED));
    if (Build.VERSION.SDK_INT >= VERSION_CODES.R) {
      actions.add(BluetoothDevice.ACTION_ALIAS_CHANGED);
    }
    return actions.stream().toArray(String[]::new);
  }
}
