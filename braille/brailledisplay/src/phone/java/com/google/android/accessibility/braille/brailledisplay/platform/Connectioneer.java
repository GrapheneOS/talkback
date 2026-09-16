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

import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static java.util.stream.Collectors.toCollection;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.usb.UsbManager;
import android.util.Pair;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.platform.BrailleDisplayManager.AccessibilityServiceContextProvider;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.ConnectManager;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.ConnectManager.ConnectType;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.ConnectManager.Reason;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.ConnectManagerProxy;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.D2dConnection;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.usb.UsbAttachedReceiver;
import com.google.android.accessibility.braille.brailledisplay.platform.lib.SetupWizardFinishReceiver;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.brltty.BrlttyEncoder;
import com.google.android.accessibility.braille.brltty.Encoder;
import com.google.android.accessibility.braille.common.lib.ScreenOnOffReceiver;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages connectivity between a phone and a BT rfcomm-capable or usb-capable braille display, in
 * addition to monitoring the state of the controlling service and providing access to the
 * device-specific BrailleDisplayProperties.
 */
public class Connectioneer {
  private static final String TAG = "Connectioneer";
  private static final Encoder.Factory ENCODER_FACTORY = new BrlttyEncoder.BrlttyFactory();

  @SuppressLint("StaticFieldLeak")
  private static Connectioneer instance;

  /** Get the static singleton instance, creating it if necessary. */
  public static Connectioneer getInstance(Context context) {
    if (instance == null) {
      instance = new Connectioneer(context);
    }
    return instance;
  }

  @VisibleForTesting
  public static void reset() {
    instance = null;
  }

  private enum ConnectReason {
    USER_CHOSE_CONNECT_DEVICE,
    AUTO_CONNECT_DEVICE_SEEN,
    AUTO_CONNECT_BONDED_REMEMBERED_BD_ENABLED,
    AUTO_CONNECT_BONDED_REMEMBERED_AUTO_CONNECT_ENABLED,
    AUTO_CONNECT_BONDED_REMEMBERED_BT_TURNED_ON,
    AUTO_CONNECT_BONDED_REMEMBERED_SCREEN_ON,
    AUTO_CONNECT_USB_UNPLUGGED,
    AUTO_CONNECT_USB_PLUGGED,
  }

  public final AspectEnablement aspectEnablement = new AspectEnablement(this);
  public final AspectConnection aspectConnection = new AspectConnection(this);
  public final AspectTraffic aspectTraffic = new AspectTraffic(this);
  public final AspectDisplayer aspectDisplayer = new AspectDisplayer(this);
  private final Set<String> userDisconnectedDevices = new HashSet<>();
  private final Set<String> userDeniedDevices = new HashSet<>();
  private final Context context;
  private final ScreenOnOffReceiver screenOnOffReceiver;
  private final UsbAttachedReceiver usbAttachedReceiver;
  private final SetupWizardFinishReceiver setupWizardFinishReceiver;
  private final ConnectManagerProxy connectManagerProxy;
  private final ConnectManagerCallback connectManagerCallback;
  private Encoder encoder;
  private BrailleDisplayProperties displayProperties;
  private boolean controllingServiceEnabled;
  // Store if user connects to a braille display via usb during SetupWizard.
  private boolean usbConnected;
  private boolean manualConnect;

  private Connectioneer(Context context) {
    this.context = context.getApplicationContext();
    encoder =
        ENCODER_FACTORY.createEncoder(
            this.context,
            new Encoder.Callback() {
              @Override
              public void sendPacketToDevice(byte[] packet) {
                BrailleDisplayLog.v(TAG, "sendPacketToDevice");
                aspectTraffic.notifySendPacketToDevice(packet);
              }

              @Override
              public void readAfterDelay(int delayMs) {
                BrailleDisplayLog.v(TAG, "readAfterDelay");
                aspectTraffic.notifyReadDelay(delayMs);
              }
            });
    screenOnOffReceiver = new ScreenOnOffReceiver(this.context, screenOnOffReceiverCallback);
    usbAttachedReceiver = new UsbAttachedReceiver(this.context, usbAttachedReceiverCallback);
    setupWizardFinishReceiver =
        new SetupWizardFinishReceiver(this.context, setupWizardFinishReceiverCallback);
    connectManagerCallback = new ConnectManagerCallback();
    connectManagerProxy = new ConnectManagerProxy(this.context, connectManagerCallback);
    // We knowingly register this listener with no intention of deregistering it.  As this
    // registration happens in the constructor, and the constructor runs only once per process,
    // because we are a singleton, the lack of deregistration is okay.
    PersistentStorage.registerListener(this.context, preferencesListener);
  }

  /** Set accessibility service context provider. */
  public void setAccessibilityServiceContextProvider(
      AccessibilityServiceContextProvider accessibilityServiceContextProvider) {
    connectManagerProxy.setAccessibilityServiceContextProvider(accessibilityServiceContextProvider);
  }

  /** Informs that the controlling service has changed its enabled status. */
  public void onServiceEnabledChanged(boolean serviceEnabled) {
    this.controllingServiceEnabled = serviceEnabled;
    if (serviceEnabled) {
      usbAttachedReceiver.registerSelf();
    } else {
      usbAttachedReceiver.unregisterSelf();
    }
    figureEnablement();
  }

  private boolean shouldUseUsbConnection() {
    UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    if (usbManager == null) {
      return false;
    }
    return usbManager.getDeviceList().values().stream()
        .anyMatch(
            device ->
                allowDevice(device.getProductName())
                    || allowDeviceById(device.getVendorId(), device.getProductId()));
  }

  private void figureEnablement() {
    // Connect type may change before braille display toggle is enabled so update the manager before
    // it starts.
    connectManagerProxy.switchTo(
        shouldUseUsbConnection() ? ConnectType.USB : ConnectType.BLUETOOTH);
    // Enable the Connectioneer as long as the controlling service is on and user-controlled
    // enablement is true; we purposely do not burden ourselves with other concerns such as
    // bluetooth radio being on or permissions are granted.
    if (isBrailleDisplayEnabled()) {
      if (aspectConnection.isBluetoothOn() && aspectConnection.useBluetoothConnection()) {
        PersistentStorage.syncRememberedDevice(context, getBondedDeviceSet());
      }
      autoConnectIfPossibleToBondedDevice(ConnectReason.AUTO_CONNECT_BONDED_REMEMBERED_BD_ENABLED);
      screenOnOffReceiver.registerSelf();
      setupWizardFinishReceiver.registerSelf();
      connectManagerProxy.onStart();
    } else {
      screenOnOffReceiver.unregisterSelf();
      setupWizardFinishReceiver.unregisterSelf();
      connectManagerProxy.onStop();
      // Switch back to bt by default.
      connectManagerProxy.switchTo(ConnectType.BLUETOOTH);
      // Clear auto connect restricted devices list.
      userDisconnectedDevices.clear();
      userDeniedDevices.clear();
    }
    aspectEnablement.notifyEnablementChange();
  }

  private boolean isBrailleDisplayEnabled() {
    boolean userSettingEnabled = PersistentStorage.isConnectionEnabled(context);
    BrailleDisplayLog.d(
        TAG,
        "serviceEnabled: "
            + controllingServiceEnabled
            + ", userSettingEnabled: "
            + userSettingEnabled);
    return controllingServiceEnabled && userSettingEnabled;
  }

  private void enableBrailleDisplay() {
    PersistentStorage.setConnectionEnabled(context, /* enabled= */ true);
    figureEnablement();
  }

  private void onUserSettingEnabledChanged() {
    figureEnablement();
  }

  private void onAutoConnectChanged() {
    // Don't callback when the braille display is disabled.
    if (isBrailleDisplayEnabled() && PersistentStorage.isAutoConnect(context)) {
      autoConnectIfPossibleToBondedDevice(
          ConnectReason.AUTO_CONNECT_BONDED_REMEMBERED_AUTO_CONNECT_ENABLED);
    }
  }

  private ImmutableSet<ConnectableDevice> getBondedDeviceSet() {
    return connectManagerProxy.getBondedDevices().stream()
        .filter(device -> allowDevice(device.name()))
        .collect(toImmutableSet());
  }

  private void autoConnectIfPossibleToBondedDevice(ConnectReason reason) {
    autoConnectIfPossible(getBondedDeviceSet(), reason);
  }

  @SuppressLint("DefaultLocale")
  private void autoConnectIfPossible(Collection<ConnectableDevice> devices, ConnectReason reason) {
    BrailleDisplayLog.d(
        TAG,
        "autoConnectIfPossible; reason: " + reason + "; examining " + devices.size() + " devices");
    if (isConnectingOrConnected()) {
      BrailleDisplayLog.d(TAG, "isConnectingOrConnected");
      return;
    }
    if (PersistentStorage.isAutoConnect(context)) {
      Optional<ConnectableDevice> connectableDevice;
      if (aspectConnection.useBluetoothConnection()) {
        // Find the first in the remembered devices.
        connectableDevice =
            PersistentStorage.getRememberedDevices(context).stream()
                .map(
                    new Function<Pair<String, String>, ConnectableDevice>() {
                      @Nullable
                      @Override
                      public ConnectableDevice apply(Pair<String, String> devicePair) {
                        return devices.stream()
                            .filter(device -> devicePair.second.equals(device.address()))
                            .findAny()
                            .orElse(null);
                      }
                    })
                .filter(
                    device -> {
                      if (device == null) {
                        return false;
                      }
                      return acceptAutoConnect(device);
                    })
                .findFirst();
      } else {
        // We don't remember usb devices. Connect the first one directly.
        connectableDevice = devices.stream().findFirst();
      }
      if (connectableDevice.isPresent()) {
        BrailleDisplayLog.d(
            TAG,
            "autoConnectIfPossible; found bonded remembered device " + connectableDevice.get());
        submitConnectionRequest(connectableDevice.get(), reason);
      }
    }
  }

  private boolean acceptAutoConnect(ConnectableDevice device) {
    return allowDevice(device.name())
        && !userDisconnectedDevices.contains(device.address())
        && !userDeniedDevices.contains(device.address());
  }

  private boolean allowDevice(String name) {
    if (name == null) {
      return false;
    }
    return encoder.getDeviceNameFilter().test(name);
  }

  private boolean allowDeviceById(int vendorId, int prodId) {
    return encoder.getDeviceVendorProdIdFilter().test(vendorId, prodId);
  }

  private boolean isConnectingOrConnected() {
    return connectManagerProxy.isConnectingOrConnected();
  }

  private void submitConnectionRequest(ConnectableDevice device, ConnectReason reason) {
    BrailleDisplayLog.d(TAG, "submitConnectionRequest to " + device + ", reason:" + reason);
    manualConnect = reason == ConnectReason.USER_CHOSE_CONNECT_DEVICE;
    connectManagerProxy.connect(device);
  }

  private static class Aspect<A extends Aspect<A, L>, L> {
    protected final Connectioneer connectioneer;
    protected final List<L> listeners = new ArrayList<>();

    public Aspect(Connectioneer connectioneer) {
      this.connectioneer = connectioneer;
    }

    @CanIgnoreReturnValue
    @SuppressWarnings("unchecked")
    public A attach(L callback) {
      listeners.add(callback);
      return (A) this;
    }

    @CanIgnoreReturnValue
    @SuppressWarnings("unchecked")
    public A detach(L callback) {
      listeners.remove(callback);
      return (A) this;
    }

    protected void notifyListeners(Consumer<L> consumer) {
      for (L callback : listeners) {
        consumer.accept(callback);
      }
    }
  }

  /** Aspect for enablement. */
  public static class AspectEnablement extends Aspect<AspectEnablement, AspectEnablement.Callback> {
    public AspectEnablement(Connectioneer connectioneer) {
      super(connectioneer);
    }

    /** Callback for this aspect. */
    public interface Callback {
      void onEnablementChanged();
    }

    private void notifyEnablementChange() {
      notifyListeners(AspectEnablement.Callback::onEnablementChanged);
    }

    /** Asks if the controlling service is enabled. */
    public boolean isServiceEnabled() {
      return connectioneer.controllingServiceEnabled;
    }
  }

  /** Aspect for the connectivity between this device and the remote device. */
  public static class AspectConnection extends Aspect<AspectConnection, AspectConnection.Callback> {
    private AspectConnection(Connectioneer connectioneer) {
      super(connectioneer);
    }

    /** Callback for this aspect. */
    public interface Callback {

      /** Callbacks when scanning changed. */
      void onScanningChanged();

      /** Callbacks when starting a device connection. */
      void onConnectStarted(boolean initial, ConnectStage stage);

      /** Callbacks when stored device list cleared. */
      void onDeviceListCleared();

      /** Callbacks when connectable device seen or updated. */
      void onConnectableDeviceSeenOrUpdated(ConnectableDevice device);

      /** Callbacks when connectable device deleted. */
      void onConnectableDeviceDeleted(ConnectableDevice device);

      /** Callbacks when a device connection status changed. */
      void onConnectionDisconnected(boolean manualConnect, ConnectableDevice device);

      /** Callbacks when a device connection status changed. */
      void onConnectionConnected(
          boolean manualConnect, ConnectStage stage, ConnectableDevice device);

      /** Callbacks when a device connection failed. */
      void onConnectFailed(boolean manualConnect, ConnectStage stage, ConnectableDevice device);
    }

    private void notifyScanningChanged() {
      notifyListeners(AspectConnection.Callback::onScanningChanged);
    }

    private void notifyDeviceListCleared() {
      notifyListeners(AspectConnection.Callback::onDeviceListCleared);
    }

    private void notifyConnectableDeviceSeenOrUpdated(ConnectableDevice device) {
      notifyListeners(callback -> callback.onConnectableDeviceSeenOrUpdated(device));
    }

    private void notifyConnectableDeviceDeleted(ConnectableDevice device) {
      notifyListeners(callback -> callback.onConnectableDeviceDeleted(device));
    }

    private void notifyConnectStarted(boolean initial, ConnectStage phase) {
      notifyListeners(callback -> callback.onConnectStarted(initial, phase));
    }

    private void notifyConnectionDisconnected(
        boolean manualConnect, @Nullable ConnectableDevice device) {
      notifyListeners(callback -> callback.onConnectionDisconnected(manualConnect, device));
    }

    private void notifyConnectionConnected(
        boolean manualConnect, ConnectStage stage, @Nullable ConnectableDevice device) {
      notifyListeners(callback -> callback.onConnectionConnected(manualConnect, stage, device));
    }

    private void notifyConnectFailed(
        boolean manualConnect, ConnectStage stage, ConnectableDevice device) {
      notifyListeners(callback -> callback.onConnectFailed(manualConnect, stage, device));
    }

    /** Informs that the user has requested a rescan. */
    public void onUserSelectedRescan() {
      connectioneer.connectManagerProxy.startSearch(Reason.START_USER_SELECTED_RESCAN);
      notifyListeners(AspectConnection.Callback::onScanningChanged);
    }

    /** Informs that the user has entered the Settings UI. */
    public void onSettingsEntered() {
      if (connectioneer.isBrailleDisplayEnabled()) {
        if (isBluetoothOn() && useBluetoothConnection()) {
          PersistentStorage.syncRememberedDevice(
              connectioneer.context, connectioneer.getBondedDeviceSet());
        }
        connectioneer.connectManagerProxy.startSearch(Reason.START_SETTINGS);
      }
    }

    /** Informs that the user has chosen to connect to a device. */
    public void onUserChoseConnectDevice(ConnectableDevice device) {
      connectioneer.userDisconnectedDevices.remove(device.address());
      connectioneer.userDeniedDevices.remove(device.address());
      connectioneer.submitConnectionRequest(device, ConnectReason.USER_CHOSE_CONNECT_DEVICE);
    }

    /** Informs that the user has chosen to disconnect from a device. */
    public void onUserChoseDisconnectFromDevice(String deviceAddress) {
      connectioneer.userDisconnectedDevices.add(deviceAddress);
      disconnectFromDevice(deviceAddress);
    }

    /** Informs that the user has chosen to forget a device. */
    public void onUserChoseForgetDevice(ConnectableDevice device) {
      connectioneer.connectManagerProxy.forget(device);
      disconnectFromDevice(device.address());
    }

    /** Informs that displayer failed to start. */
    public void onDisplayerStartFailed(String deviceAddress) {
      disconnectFromDevice(deviceAddress);
    }

    /** Asks if the device's bluetooth radio is on. */
    public boolean isBluetoothOn() {
      BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
      return adapter != null && adapter.isEnabled();
    }

    /** Returns whether is using Bluetooth connection. */
    public boolean useBluetoothConnection() {
      return connectioneer.connectManagerProxy.getType() == ConnectType.BLUETOOTH;
    }

    /** Asks if the device's bluetooth radio is currently scanning. */
    public boolean isScanning() {
      return connectioneer.connectManagerProxy.isScanning();
    }

    /** Gets a copy list of currently visible devices. */
    public List<ConnectableDevice> getScannedDevicesCopy() {
      return connectioneer.connectManagerProxy.getConnectableDevices().stream()
          .filter(device -> connectioneer.allowDevice(device.name()))
          .collect(toCollection(ArrayList::new));
    }

    /** Asks if the given device address is connecting-in-progress. */
    public boolean isConnectingTo(String candidateAddress) {
      return connectioneer.connectManagerProxy.isConnecting(candidateAddress);
    }

    /** Asks if the given device address is currently connected. */
    public boolean isConnectedTo(String candidateAddress) {
      return connectioneer.connectManagerProxy.isConnected(candidateAddress);
    }

    /** Asks if either connecting is in progress or if connection is active. */
    public boolean isConnectingOrConnected() {
      return connectioneer.isConnectingOrConnected();
    }

    private void disconnectFromDevice(String deviceAddress) {
      if (connectioneer.connectManagerProxy.isConnectingOrConnected(deviceAddress)) {
        connectioneer.connectManagerProxy.disconnect();
      }
    }
  }

  /** Aspect for the packet traffic between this device and the remote device. */
  public static class AspectTraffic extends Aspect<AspectTraffic, AspectTraffic.Callback> {
    private AspectTraffic(Connectioneer connectioneer) {
      super(connectioneer);
    }

    /** Callback for this aspect. */
    public interface Callback {
      void onPacketArrived(byte[] buffer);

      void onRead();

      void onReadDelay(int delayMs);

      void onSendTrafficOutgoingMessage(byte[] packet);
    }

    private void notifyPacketArrived(byte[] buffer) {
      notifyListeners(callback -> callback.onPacketArrived(buffer));
    }

    private void notifyRead() {
      notifyListeners(AspectTraffic.Callback::onRead);
    }

    private void notifyReadDelay(int delayMs) {
      notifyListeners(callback -> callback.onReadDelay(delayMs));
    }

    /** Informs that the given outgoing message should be sent to the remote device. */
    private void notifySendPacketToDevice(byte[] packet) {
      notifyListeners(callback -> callback.onSendTrafficOutgoingMessage(packet));
    }
  }

  /** Aspect for the display properties of the remote device. */
  public static class AspectDisplayer extends Aspect<AspectDisplayer, AspectDisplayer.Callback> {

    private AspectDisplayer(Connectioneer connectioneer) {
      super(connectioneer);
    }

    /** Callback for this aspect. */
    public interface Callback {
      void onDisplayStarted(BrailleDisplayProperties brailleDisplayProperties);

      void onDisplayStopped();
    }

    /** Informs that the display properties have arrived from the remote device. */
    public void onDisplayerStarted(BrailleDisplayProperties displayProperties) {
      BrailleDisplayLog.d(TAG, "onDisplayerStarted");
      connectioneer.displayProperties = displayProperties;
      notifyListeners(callback -> callback.onDisplayStarted(connectioneer.displayProperties));
    }

    /** Informs that the displayer stopped. */
    public void onDisplayStopped() {
      BrailleDisplayLog.d(TAG, "onDisplayStopped");
      notifyListeners(AspectDisplayer.Callback::onDisplayStopped);
    }

    /** Asks for the display properties of the remote device. */
    public BrailleDisplayProperties getDisplayProperties() {
      return connectioneer.displayProperties;
    }

    public void sendPacketToDisplay(byte[] packet) {
      BrailleDisplayLog.v(TAG, "onSendPacketToDisplay");
      connectioneer.connectManagerProxy.sendOutgoingPacket(packet);
    }

    public void consumePacketFromDevice(byte[] packet) {
      connectioneer.encoder.consumePacketFromDevice(packet);
    }

    public void writeBrailleDots(byte[] brailleDotBytes) {
      connectioneer.encoder.writeBrailleDots(brailleDotBytes);
    }

    public int readCommand() {
      return connectioneer.encoder.readCommand();
    }

    public void stop() {
      connectioneer.encoder.stop();
    }

    public Optional<BrailleDisplayProperties> start(
        String deviceName, int vendorId, int prodId, boolean useHid, String parameters) {
      return connectioneer.encoder.start(deviceName, vendorId, prodId, useHid, parameters);
    }
  }

  private final SharedPreferences.OnSharedPreferenceChangeListener preferencesListener =
      new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (Objects.equals(key, PersistentStorage.PREF_CONNECTION_ENABLED)) {
            onUserSettingEnabledChanged();
          } else if (Objects.equals(key, PersistentStorage.PREF_AUTO_CONNECT)) {
            onAutoConnectChanged();
          }
        }
      };

  private final ScreenOnOffReceiver.Callback screenOnOffReceiverCallback =
      new ScreenOnOffReceiver.Callback() {
        @Override
        public void onScreenOn() {
          BrailleDisplayLog.d(TAG, "onScreenOn");
          connectManagerProxy.startSearch(Reason.START_SCREEN_ON);
          autoConnectIfPossibleToBondedDevice(
              ConnectReason.AUTO_CONNECT_BONDED_REMEMBERED_SCREEN_ON);
        }

        @Override
        public void onScreenOff() {
          BrailleDisplayLog.d(TAG, "onScreenOff");
          // TODO consider disconnecting
          connectManagerProxy.stopSearch(Reason.STOP_SCREEN_OFF);
        }
      };

  private final UsbAttachedReceiver.Callback usbAttachedReceiverCallback =
      new UsbAttachedReceiver.Callback() {
        @Override
        public void onUsbAttached(ConnectableDevice device) {
          BrailleDisplayLog.d(TAG, "onUsbAttached");
          // TODO: Disable it in SUW util we can prevent USB users going to restore
          // process.
          if (!shouldUseUsbConnection() || !SettingsUtils.allowLinksOutOfSettings(context)) {
            return;
          }
          if (!isBrailleDisplayEnabled()) {
            enableBrailleDisplay();
          }
          usbConnected = true;
          if (!SettingsUtils.allowLinksOutOfSettings(context)) {
            PersistentStorage.setConnectionEnabled(context, true);
          }
          if (!aspectConnection.useBluetoothConnection()) {
            // Refresh device list.
            connectManagerProxy.startSearch(Reason.START_USB_ATTACH_DETACH);
            if (!isConnectingOrConnected()) {
              submitConnectionRequest(device, ConnectReason.AUTO_CONNECT_USB_PLUGGED);
            }
          } else {
            connectManagerProxy.switchTo(ConnectType.USB);
            connectManagerProxy.onStart();
            submitConnectionRequest(device, ConnectReason.AUTO_CONNECT_USB_PLUGGED);
          }
        }

        @Override
        public void onUsbDetached(ConnectableDevice device) {
          BrailleDisplayLog.d(TAG, "onUsbDetached");
          // TODO: Disable it in SUW util we can prevent USB users going to restore
          // process.
          if (shouldUseUsbConnection() || !SettingsUtils.allowLinksOutOfSettings(context)) {
            if (connectManagerProxy.getConnectingOrConnectedDevice().isPresent()
                && connectManagerProxy.getConnectingOrConnectedDevice().get().equals(device)) {
              connectManagerProxy.disconnect();
            }
            // Refresh device list.
            connectManagerProxy.startSearch(Reason.START_USB_ATTACH_DETACH);
            if (!isConnectingOrConnected()) {
              autoConnectIfPossibleToBondedDevice(ConnectReason.AUTO_CONNECT_USB_UNPLUGGED);
            }
          } else if (!aspectConnection.useBluetoothConnection()) {
            connectManagerProxy.switchTo(ConnectType.BLUETOOTH);
            connectManagerProxy.onStart();
            autoConnectIfPossibleToBondedDevice(ConnectReason.AUTO_CONNECT_USB_UNPLUGGED);
          }
        }
      };

  private final SetupWizardFinishReceiver.Callback setupWizardFinishReceiverCallback =
      new SetupWizardFinishReceiver.Callback() {
        @Override
        public void onFinished() {
          BrailleDisplayLog.d(TAG, "onFinished");
          if (!PersistentStorage.isConnectionEnabled(context) && !usbConnected) {
            // Disable braille display settings if user never connects a braille display via USB.
            figureEnablement();
          }
        }
      };

  private class ConnectManagerCallback implements ConnectManager.Callback {
    @Override
    public void onDeviceListCleared() {
      BrailleDisplayLog.d(TAG, "onDeviceListCleared");
      aspectConnection.notifyDeviceListCleared();
    }

    @Override
    public void onDeviceSeenOrUpdated(ConnectableDevice device) {
      if (allowDevice(device.name())) {
        autoConnectIfPossible(ImmutableSet.of(device), ConnectReason.AUTO_CONNECT_DEVICE_SEEN);
        aspectConnection.notifyConnectableDeviceSeenOrUpdated(device);
      }
    }

    @Override
    public void onDeviceDeleted(ConnectableDevice device) {
      PersistentStorage.deleteRememberedDevice(context, device.address());
      aspectConnection.notifyConnectableDeviceDeleted(device);
    }

    @Override
    public void onConnectivityEnabled(boolean enabled) {
      BrailleDisplayLog.d(TAG, "onConnectivityEnabled: " + enabled);
      if (isBrailleDisplayEnabled()) {
        connectManagerProxy.startSearch(Reason.START_BLUETOOTH_TURNED_ON);
        autoConnectIfPossibleToBondedDevice(
            ConnectReason.AUTO_CONNECT_BONDED_REMEMBERED_BT_TURNED_ON);
      } else {
        connectManagerProxy.stopSearch(Reason.STOP_BLUETOOTH_TURNED_OFF);
        connectManagerProxy.disconnect();
        aspectConnection.notifyDeviceListCleared();
      }
    }

    @Override
    public void onSearchStatusChanged() {
      BrailleDisplayLog.d(TAG, "onSearchStatusChanged");
      aspectConnection.notifyScanningChanged();
    }

    @Override
    public void onSearchFailure() {
      BrailleDisplayLog.d(TAG, "onSearchFailure");
      aspectConnection.notifyScanningChanged();
    }

    @Override
    public void onConnectStarted(boolean initial, ConnectStage stage) {
      aspectConnection.notifyConnectStarted(initial, stage);
    }

    @Override
    public void onDisconnected() {
      BrailleDisplayLog.d(TAG, "onDisconnected");
      displayProperties = null;
      aspectConnection.notifyConnectionDisconnected(manualConnect, /* device= */ null);
    }

    @Override
    public void onDenied(ConnectableDevice device) {
      userDeniedDevices.add(device.address());
    }

    @Override
    public void onConnected(ConnectStage stage, D2dConnection connection) {
      BrailleDisplayLog.d(TAG, "onConnectSuccess");
      ConnectableDevice device = connection.getDevice();
      if (device != null && device.name() != null && aspectConnection.useBluetoothConnection()) {
        // We don't remember usb devices.
        PersistentStorage.addOrUpdateRememberedDevice(
            context, new Pair<>(device.name(), device.address()));
      }
      // In case you are wondering what happens if the call to open() leads to failure... such
      // a failure will NOT be handled on the current tick (see the docs for
      // BtConnection.open()). Therefore, any code that follows the call to open() will
      // execute BEFORE any failure callback gets invoked, which is reasonable and consistent.
      connection.open(d2dConnectionCallback);
      aspectConnection.notifyConnectionConnected(manualConnect, stage, device);
    }

    @Override
    public void onConnectFailure(
        ConnectableDevice device, ConnectStage stage, Exception exception) {
      BrailleDisplayLog.d(TAG, "onConnectFailure: " + exception.getMessage());
      connectManagerProxy.disconnect();
      aspectConnection.notifyConnectFailed(manualConnect, stage, device);
    }
  }

  private final D2dConnection.Callback d2dConnectionCallback =
      new D2dConnection.Callback() {

        @Override
        public void onPacketArrived(byte[] packet) {
          // As stated in the docs for {@link D2dConnection#onTrafficConsume()}, we are on the
          // main thread right now, and if we have arrived here then the connection is viable (has
          // neither failed nor been shutdown). Any downstream method invocations stemming from
          // here can safely use the connection.
          aspectTraffic.notifyPacketArrived(packet);
        }

        @Override
        public void onRead() {
          aspectTraffic.notifyRead();
        }

        @Override
        public void onFatalError(Exception exception) {
          BrailleDisplayLog.e(TAG, "onFatalError: " + exception.getMessage());
          connectManagerProxy.disconnect();
          Toast.makeText(
                  context,
                  context.getString(R.string.bd_bt_connection_disconnected_message),
                  Toast.LENGTH_LONG)
              .show();
        }
      };

  @VisibleForTesting
  public ConnectManager.Callback testing_getConnectManagerCallback() {
    return connectManagerCallback;
  }

  @VisibleForTesting
  public ConnectManagerProxy testing_getConnectManagerProxy() {
    return connectManagerProxy;
  }

  @VisibleForTesting
  public void testing_setEncoder(Encoder encoder) {
    this.encoder = encoder;
  }
}
