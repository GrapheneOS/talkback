/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.android.accessibility.braille.brailledisplay.platform.connect.bt;

import static android.os.Build.VERSION_CODES.VANILLA_ICE_CREAM;

import android.accessibilityservice.BrailleDisplayController;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import androidx.annotation.RequiresApi;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.Connector;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableBluetoothDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.hid.HidConnector;
import java.lang.reflect.Method;

/**
 * Sets up a Bluetooth hid connection to a remote device that is advertising itself or already
 * bonded.
 */
@RequiresApi(api = VANILLA_ICE_CREAM)
public class BtHidConnector extends HidConnector {
  private static final String TAG = "BtHidConnector";
  private BtBrailleDisplayCallback callback;

  public BtHidConnector(
      Context context,
      ConnectableDevice device,
      Connector.Callback callback,
      BrailleDisplayController controller) {
    super(context, device, callback, controller);
  }

  @Override
  public void connect() {
    if (!isAvailable()) {
      BrailleDisplayLog.w(TAG, "Braille HID is not supported.");
      return;
    }
    BluetoothDevice device = ((ConnectableBluetoothDevice) getDevice()).bluetoothDevice();
    if (!isConnected(device)) {
      BrailleDisplayLog.w(TAG, "Braille display is not connected.");
    }
    if (getBrailleDisplayController().isConnected()) {
      BrailleDisplayLog.w(TAG, "BrailleDisplayController already connected");
      return;
    }
    callback =
        new BtBrailleDisplayCallback(
            getBrailleDisplayController(), getConnectorCallback(), getDevice());
    getBrailleDisplayController().connect(device, callback);
  }

  @Override
  public void disconnect() {
    if (!isAvailable()) {
      BrailleDisplayLog.w(TAG, "Braille HID is not supported.");
      return;
    }
    getBrailleDisplayController().disconnect();
  }

  private boolean isConnected(BluetoothDevice device) {
    try {
      Method method = device.getClass().getMethod("isConnected");
      if (method != null) {
        return (boolean) method.invoke(device);
      }
    } catch (ReflectiveOperationException e) {
      BrailleDisplayLog.w(TAG, "Unable to call isConnected. ", e);
    }
    return false;
  }

  @VisibleForTesting
  BrailleDisplayCallback testing_getBrailleDisplayCallback() {
    return callback;
  }

  private static class BtBrailleDisplayCallback extends BrailleDisplayCallback {
    public BtBrailleDisplayCallback(
        BrailleDisplayController controller,
        Connector.Callback callback,
        ConnectableDevice device) {
      super(controller, callback, device);
    }
  }
}
