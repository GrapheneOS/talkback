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

package com.google.android.accessibility.braille.brailledisplay.platform;


import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.PowerManager;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.analytics.BrailleDisplayAnalytics;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableBluetoothDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableUsbDevice;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.brltty.DeviceInfo;
import com.google.android.accessibility.braille.brltty.SupportedDevicesHelper;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.DeviceProvider;
import com.google.android.accessibility.braille.common.translate.BrailleLanguages.Code;
import java.util.Optional;

/** Manages the interface to a braille display, on behalf of an AccessibilityService. */
public class BrailleDisplayManager {
  private static final String TAG = "BrailleDisplayManager";
  private final Context context;
  private final Controller controller;
  private final PowerManager.WakeLock wakeLock;
  private final Connectioneer connectioneer;
  private Displayer displayer;
  private boolean connectedService;
  private boolean connectedToDisplay;

  /** Provides instance of accessibility service context. */
  public interface AccessibilityServiceContextProvider {
    Context getAccessibilityServiceContext();
  }

  @SuppressLint("InvalidWakeLockTag")
  public BrailleDisplayManager(Context context, Controller controller) {
    this.context = context;
    this.controller = controller;
    wakeLock =
        ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
            .newWakeLock(PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP, TAG);
    displayer = new Displayer(displayerCallback);
    connectioneer = Connectioneer.getInstance(context);
  }

  /** Sets accessibility service context provider. */
  public void setAccessibilityServiceContextProvider(
      AccessibilityServiceContextProvider accessibilityServiceContextProvider) {
    connectioneer.setAccessibilityServiceContextProvider(accessibilityServiceContextProvider);
  }

  /** Notifies this manager the owning service has started. */
  public void onServiceStarted() {
    connectedService = true;
    // To ensure callbacks are triggered after an auto-connection, attach aspectConnection and
    // aspectTraffic before onServiceEnabledChanged.
    connectioneer.aspectConnection.attach(connectionCallback);
    connectioneer.aspectTraffic.attach(trafficCallback);
    connectioneer.onServiceEnabledChanged(true);
  }

  /** Notifies this manager the owning service has stopped. */
  public void onServiceStopped() {
    controller.onDestroy();
    connectedService = false;
    connectioneer.aspectConnection.detach(connectionCallback);
    connectioneer.aspectTraffic.detach(trafficCallback);
    connectioneer.onServiceEnabledChanged(false);
    displayer.stop();
  }

  /** Sends an {@code AccessibilityEvent} to this manager for processing. */
  public void onAccessibilityEvent(AccessibilityEvent accessibilityEvent) {
    if (canSendRenderPackets()) {
      controller.onAccessibilityEvent(accessibilityEvent);
    }
  }

  /** Sends an {@code KeyEvent} to this manager for processing. */
  public boolean onKeyEvent(KeyEvent keyEvent) {
    if (canSendRenderPackets()) {
      if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_BUTTON_1) {
        // Incorrectly sent by Humanware Brailliant BI20X on every dot-1 keystroke. Ignore for now.
        return true;
      }
    }
    return false;
  }

  private boolean canSendPackets() {
    return connectedService && connectedToDisplay;
  }

  private boolean canSendRenderPackets() {
    return connectedService
        && connectedToDisplay
        && displayer != null
        && displayer.isDisplayReady();
  }

  // Keeps the phone awake as if there was user activity registered by the system.
  private void keepAwake() {
    wakeLock.acquire(/* timeout= */ 0);
    try {
      wakeLock.release();
    } catch (RuntimeException e) {
      // A wakelock acquired with a timeout may be released by the system before calling
      // `release`.
      // Ignore: already released by timeout.
    }
  }

  private void logConnectStarted(ConnectStage stage, boolean initial) {
    switch (stage) {
      case HID ->
          BrailleDisplayAnalytics.getInstance(context).logStartToEstablishHidConnection(initial);
      case RFCOMM ->
          BrailleDisplayAnalytics.getInstance(context).logStartToEstablishRfcommConnection(initial);
      case SERIAL ->
          BrailleDisplayAnalytics.getInstance(context).logStartToConnectToSerialConnection(initial);
      case BRLTTY ->
          BrailleDisplayAnalytics.getInstance(context).logStartToConnectToBrailleDisplay();
    }
  }

  private void logSessionMetrics(ConnectableDevice device) {
    boolean contracted = BrailleUserPreferences.readContractedMode(context);
    Code inputCode = BrailleUserPreferences.readCurrentActiveInputCodeAndCorrect(context);
    boolean inputContracted = inputCode.isSupportsContracted(context) && contracted;
    Code outputCode = BrailleUserPreferences.readCurrentActiveOutputCodeAndCorrect(context);
    boolean outputContracted = outputCode.isSupportsContracted(context) && contracted;

    BrailleDisplayAnalytics.getInstance(context)
        .logStartedEvent(
            displayer.getDeviceProperties().getDriverCode(),
            device.truncatedName(), // TODO: Use truncated name in DeviceInfo.
            inputCode,
            outputCode,
            inputContracted,
            outputContracted,
            device.useHid(),
            device instanceof ConnectableBluetoothDevice);
  }

  private void logConnectAttempt(boolean manualConnect, ConnectableDevice device) {
    if (TextUtils.isEmpty(device.name())) {
      return;
    }
    DeviceProvider<?> deviceProvider = null;
    if (device instanceof ConnectableBluetoothDevice connectableBluetoothDevice) {
      deviceProvider = new DeviceProvider<>(connectableBluetoothDevice.bluetoothDevice());
    } else if (device instanceof ConnectableUsbDevice connectableUsbDevice) {
      deviceProvider = new DeviceProvider<>(connectableUsbDevice.usbDevice());
    }
    DeviceInfo info = SupportedDevicesHelper.getDeviceInfo(device.name(), device.useHid());
    if (info != null) {
      BrailleDisplayAnalytics.getInstance(context)
          .logConnectAttempt(
              info.driverCode(), info.truncatedName(), manualConnect, deviceProvider);
    }
  }

  private void logConnectStage(ConnectStage stage, boolean success) {
    switch (stage) {
      case HID -> BrailleDisplayAnalytics.getInstance(context).logHidConnectRecord(success);
      case RFCOMM -> BrailleDisplayAnalytics.getInstance(context).logRfcommConnectRecord(success);
      case BRLTTY -> BrailleDisplayAnalytics.getInstance(context).logBrlttyConnectRecord(success);
      default -> {}
    }
  }

  @VisibleForTesting
  Displayer.Callback testing_getDisplayerCallback() {
    return displayerCallback;
  }

  @VisibleForTesting
  public void testing_setDisplayer(Displayer displayer) {
    this.displayer = displayer;
  }

  private final Connectioneer.AspectConnection.Callback connectionCallback =
      new Connectioneer.AspectConnection.Callback() {
        @Override
        public void onScanningChanged() {
          BrailleDisplayLog.d(TAG, "onScanningChanged");
        }

        @Override
        public void onConnectStarted(boolean initial, ConnectStage stage) {
          BrailleDisplayLog.d(TAG, "onConnectStarted: " + stage);
          logConnectStarted(stage, initial);
        }

        @Override
        public void onDeviceListCleared() {
          BrailleDisplayLog.d(TAG, "onDeviceListCleared");
        }

        @Override
        public void onConnectableDeviceSeenOrUpdated(ConnectableDevice device) {
          BrailleDisplayLog.d(TAG, "onConnectableDeviceSeenOrUpdated");
        }

        @Override
        public void onConnectableDeviceDeleted(ConnectableDevice device) {
          BrailleDisplayLog.d(TAG, "onConnectableDeviceDeleted");
        }

        @Override
        public void onConnectionDisconnected(boolean manualConnect, ConnectableDevice device) {
          BrailleDisplayLog.d(TAG, "onConnectionDisconnected device = " + device);
          connectedToDisplay = false;
          controller.onStop();
          displayer.stop();
        }

        @Override
        public void onConnectionConnected(
            boolean manualConnect, ConnectStage stage, ConnectableDevice device) {
          BrailleDisplayLog.d(TAG, "onConnectionConnected deviceName = " + device);
          connectedToDisplay = true;
          logConnectStage(stage, /* success= */ true);
          displayer.start(manualConnect, device);
        }

        @Override
        public void onConnectFailed(
            boolean manualConnect, ConnectStage stage, ConnectableDevice device) {
          BrailleDisplayLog.d(TAG, "onConnectFailed device:" + device);
          logConnectStage(stage, /* success= */ false);
          logConnectAttempt(manualConnect, device);
        }
      };

  private final Connectioneer.AspectTraffic.Callback trafficCallback =
      new Connectioneer.AspectTraffic.Callback() {
        @Override
        public void onPacketArrived(byte[] buffer) {
          BrailleDisplayLog.v(TAG, "onPacketArrived " + buffer.length + " bytes");
          // An incoming packet may arrive while the displayer is still null, because the
          // notification informing this instance that the connection is open (which leads to the
          // instantiation of the displayer), is received after the opening of that connection; in
          // that case we ignore the incoming packet.
          if (displayer != null) {
            displayer.consumePacketFromDevice(buffer);
          }
        }

        @Override
        public void onRead() {
          if (canSendPackets()) {
            displayer.readCommand();
          }
        }

        @Override
        public void onReadDelay(int delayMs) {
          if (canSendPackets()) {
            displayer.readCommandDelay(delayMs);
          }
        }

        @Override
        public void onSendTrafficOutgoingMessage(byte[] packet) {
          if (canSendPackets()) {
            connectioneer.aspectDisplayer.sendPacketToDisplay(packet);
          }
        }
      };

  private final Displayer.Callback displayerCallback =
      new Displayer.Callback() {
        @Override
        public void onStartFailed(boolean manualConnect, ConnectableDevice device) {
          BrailleDisplayLog.e(TAG, "onStartFailed");
          connectioneer.aspectConnection.onDisplayerStartFailed(device.address());
          displayer.stop();
          logConnectStage(ConnectStage.BRLTTY, /* success= */ false);
          logConnectAttempt(manualConnect, device);
        }

        @Override
        public void onDisplayReady(
            boolean manualConnect, ConnectableDevice device, BrailleDisplayProperties bdr) {
          BrailleDisplayLog.d(TAG, "onDisplayReady");
          if (canSendRenderPackets()) {
            connectioneer.aspectDisplayer.onDisplayerStarted(bdr);
            controller.onStart(displayer);
            logConnectStage(ConnectStage.BRLTTY, /* success= */ true);
            logSessionMetrics(device);
            logConnectAttempt(manualConnect, device);
          }
        }

        @Override
        public void onDisplayStop() {
          BrailleDisplayLog.d(TAG, "onDisplayStop");
          connectioneer.aspectDisplayer.onDisplayStopped();
          controller.onStop();
        }

        @Override
        public void onBrailleInputEvent(BrailleInputEvent brailleInputEvent) {
          BrailleDisplayLog.v(TAG, "onBrailleInputEvent " + brailleInputEvent);
          if (canSendRenderPackets()) {
            KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            if (keyguardManager.isKeyguardLocked()) {
              keepAwake();
            }
            controller.onBrailleInputEvent(brailleInputEvent);
          }
        }

        @Override
        public Optional<BrailleDisplayProperties> start(
            String deviceName, int vendorId, int prodId, boolean useHid, String parameters) {
          return connectioneer.aspectDisplayer.start(
              deviceName, vendorId, prodId, useHid, parameters);
        }

        @Override
        public void consumePacketFromDevice(byte[] packet) {
          connectioneer.aspectDisplayer.consumePacketFromDevice(packet);
        }

        @Override
        public void stop() {
          connectioneer.aspectDisplayer.stop();
        }

        @Override
        public int readCommand() {
          return connectioneer.aspectDisplayer.readCommand();
        }

        @Override
        public void writeBrailleDots(byte[] brailleDotBytes) {
          connectioneer.aspectDisplayer.writeBrailleDots(brailleDotBytes);
        }
      };
}
