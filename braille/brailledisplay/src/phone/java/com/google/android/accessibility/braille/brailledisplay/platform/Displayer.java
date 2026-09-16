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

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableBluetoothDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.connect.device.ConnectableUsbDevice;
import com.google.android.accessibility.braille.brailledisplay.platform.lib.Utils;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.brltty.Encoder;
import com.google.android.accessibility.braille.brltty.device.BrlttyParameterProviderFactory;
import com.google.android.accessibility.braille.brltty.device.ParameterProvider;
import com.google.android.accessibility.braille.common.DeviceProvider;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Encodes braille dots for rendering, via {@link Encoder} on the remote display and coordinates the
 * exchange of data between the {@link Controller} and the remote display.
 *
 * <p>This class uses a pair of handlers: a background handler which puts display encoding/decoding
 * operations onto a non-main-thread, and a main thread for bringing results and signals back to
 * main, so that Android components such as Services can make use of them.
 */
public class Displayer {
  private static final String TAG = "Displayer";
  private static final int COMMAND_CODE_MASK = 0xffff;
  private static final int COMMAND_ARGUMENT_MASK = 0x7fff0000;
  private static final int COMMAND_ARGUMENT_SHIFT = 16;

  /** Callback for {@link Displayer}. */
  public interface Callback {
    void onStartFailed(boolean manualConnect, ConnectableDevice device);

    void onDisplayReady(
        boolean manualConnect, ConnectableDevice device, BrailleDisplayProperties bdr);

    void onDisplayStop();

    void onBrailleInputEvent(BrailleInputEvent brailleInputEvent);

    Optional<BrailleDisplayProperties> start(
        String deviceName, int vendorId, int prodId, boolean useHid, String parameters);

    void consumePacketFromDevice(byte[] packet);

    void stop();

    int readCommand();

    void writeBrailleDots(byte[] brailleDotBytes);
  }

  private final Callback callback;
  private final Handler mainHandler;
  private final BrlttyParameterProviderFactory parameterProviderFactory;
  private final AtomicBoolean displayReady = new AtomicBoolean();
  private final Runnable runnable = () -> readCommand();
  private HandlerThread bgThread;
  private Handler bgHandler;
  private BrailleDisplayProperties displayProperties;
  private ConnectableDevice device;

  public Displayer(Callback callback) {
    this.callback = callback;
    parameterProviderFactory = new BrlttyParameterProviderFactory();
    mainHandler = new Handler(Looper.getMainLooper(), new MainHandlerCallback());
  }

  public BrailleDisplayProperties getDeviceProperties() {
    return displayProperties;
  }

  /**
   * Returns true if the encoder has finished the start-up procedure and the display is ready to
   * receive render packets.
   */
  public boolean isDisplayReady() {
    return displayReady.get();
  }

  /**
   * Starts this instance.
   *
   * <p>This will get processed on a background thread.
   */
  public void start(boolean manualConnect, ConnectableDevice device) {
    this.device = device;
    if (!isReady()) {
      BrailleDisplayLog.v(TAG, "start a new thread");
      bgThread = new HandlerThread("DisplayerBG");
      bgThread.start();
      bgHandler = new Handler(bgThread.getLooper(), new BackgroundHandlerCallback());
    }
    // Only keep one start in case it's called repeatedly.
    bgHandler.removeMessages(MessageBg.START.what());
    // It is permitted to send a message to the bg queue even if onLooperPrepared has not been
    // run.
    ParameterProvider brlttyDevice =
        parameterProviderFactory.getParameterProvider(device.useHid(), getDeviceProvider());
    Message message = bgHandler.obtainMessage(MessageBg.START.what());
    Bundle bundle = new Bundle();
    bundle.putString(MessageBg.KEY_PARAMETER, brlttyDevice.getParameters());
    bundle.putBoolean(MessageBg.KEY_MANUAL, manualConnect);
    message.setData(bundle);
    message.sendToTarget();
  }

  /**
   * Delivers an encoded packet which just arrived from the remote device.
   *
   * <p>Do not invoke on the main thread, as this directly sends a message to the {@link Encoder}.
   */
  public void consumePacketFromDevice(byte[] packet) {
    Utils.assertNotMainThread();
    callback.consumePacketFromDevice(packet);
  }

  /**
   * Stops this instance.
   *
   * <p>This will get processed on a background thread.
   */
  public void stop() {
    mainHandler.removeCallbacksAndMessages(null);
    if (bgHandler != null) {
      bgHandler.removeCallbacksAndMessages(null);
      bgHandler.obtainMessage(MessageBg.STOP.what()).sendToTarget();
    }
  }

  /**
   * Delivers an unencoded string of bytes for encoding, and eventual delivery to the remote device.
   *
   * <p>This will get processed on a background thread.
   *
   * <p>The bytes will be encoded as a new packet in the protocol expected by the remote device, and
   * will be forwarded to the remote device via {@link Callback#onSendPacketToDisplay(byte[])}.
   */
  public void writeBrailleDots(byte[] brailleDotBytes) {
    if (isReady()) {
      if (bgHandler.hasMessages(MessageBg.WRITE_BRAILLE_DOTS.what())) {
        bgHandler.removeMessages(MessageBg.WRITE_BRAILLE_DOTS.what());
      }
      bgHandler.obtainMessage(MessageBg.WRITE_BRAILLE_DOTS.what(), brailleDotBytes).sendToTarget();
    }
  }

  /**
   * Asks for the currently queued read command, if it exists.
   *
   * <p>This will get processed on a background thread.
   *
   * <p>The result of the read will be sent to the callback via {@link Callback#onBrailleInputEvent}
   */
  public void readCommand() {
    if (isReady() && !bgHandler.hasMessages(MessageBg.READ_COMMAND.what())) {
      bgHandler.obtainMessage(MessageBg.READ_COMMAND.what()).sendToTarget();
    }
  }

  public void readCommandDelay(int delayMs) {
    if (isReady()) {
      // Don't send READ_COMMAND with delay because in readCommand() hasMessage will be true and
      // filter out real-time reads.
      bgHandler.removeCallbacks(runnable);
      bgHandler.postDelayed(runnable, delayMs);
    }
  }

  private boolean isReady() {
    if (bgHandler != null && bgThread.isAlive()) {
      return true;
    }
    BrailleDisplayLog.v(TAG, "thread has not started or has died.");
    return false;
  }

  private DeviceProvider<?> getDeviceProvider() {
    if (device instanceof ConnectableBluetoothDevice connectableBluetoothDevice) {
      return new DeviceProvider<>(connectableBluetoothDevice.bluetoothDevice());
    } else if (device instanceof ConnectableUsbDevice connectableUsbDevice) {
      return new DeviceProvider<>(connectableUsbDevice.usbDevice());
    }
    throw new IllegalArgumentException();
  }

  private class MainHandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message message) {
      MessageMain messageMain = MessageMain.values()[message.what];
      BrailleDisplayLog.v(TAG, "handleMessage main " + messageMain);
      messageMain.handle(Displayer.this, message);
      return true;
    }
  }

  private enum MessageMain {
    START_FAILED {
      @Override
      void handle(Displayer displayer, Message message) {
        boolean manualConnect = message.getData().getBoolean(MessageBg.KEY_MANUAL);
        displayer.callback.onStartFailed(manualConnect, displayer.device);
      }
    },
    DISPLAY_READY {
      @Override
      void handle(Displayer displayer, Message message) {
        boolean manual = message.getData().getBoolean(MessageBg.KEY_MANUAL);
        displayer.displayProperties =
            (BrailleDisplayProperties) message.getData().get(MessageBg.KEY_PROPERTIES);
        displayer.callback.onDisplayReady(manual, displayer.device, displayer.displayProperties);
      }
    },
    DISPLAY_STOP {
      @Override
      void handle(Displayer displayer, Message message) {
        displayer.callback.onDisplayStop();
      }
    },
    READ_COMMAND_ARRIVED {
      @Override
      void handle(Displayer displayer, Message message) {
        displayer.callback.onBrailleInputEvent((BrailleInputEvent) message.obj);
      }
    };

    public int what() {
      return ordinal();
    }

    abstract void handle(Displayer displayer, Message message);
  }

  private class BackgroundHandlerCallback implements Handler.Callback {
    @Override
    public boolean handleMessage(Message message) {
      MessageBg messageBg = MessageBg.values()[message.what];
      BrailleDisplayLog.v(TAG, "handleMessage bg " + messageBg);
      messageBg.handle(Displayer.this, message);
      return true;
    }
  }

  private enum MessageBg {
    START {
      @Override
      public void handle(Displayer displayer, Message message) {
        if (displayer.displayReady.get()) {
          BrailleDisplayLog.d(TAG, "Braille display has started.");
          return;
        }
        String parameter = message.getData().getString(KEY_PARAMETER);
        boolean manual = message.getData().getBoolean(KEY_MANUAL);
        Optional<BrailleDisplayProperties> brailleDisplayProperties =
            displayer.callback.start(
                displayer.device.name(),
                displayer.device.vendorId(),
                displayer.device.productId(),
                displayer.device.useHid(),
                parameter);
        Bundle bundle = new Bundle();
        bundle.putBoolean(KEY_MANUAL, manual);
        if (brailleDisplayProperties.isPresent()) {
          displayer.displayReady.getAndSet(true);
          Message mainMessage =
              displayer.mainHandler.obtainMessage(MessageMain.DISPLAY_READY.what());
          bundle.putParcelable(KEY_PROPERTIES, brailleDisplayProperties.get());
          mainMessage.setData(bundle);
          mainMessage.sendToTarget();
        } else {
          Message mainMessage =
              displayer.mainHandler.obtainMessage(MessageMain.START_FAILED.what());
          mainMessage.setData(bundle);
          mainMessage.sendToTarget();
        }
      }
    },
    STOP {
      @Override
      public void handle(Displayer displayer, Message message) {
        if (!displayer.displayReady.getAndSet(false)) {
          BrailleDisplayLog.d(TAG, "Braille display has stopped");
          return;
        }
        displayer.displayProperties = null;
        displayer.callback.stop();
        Message mainMessage = displayer.mainHandler.obtainMessage(MessageMain.DISPLAY_STOP.what());
        mainMessage.sendToTarget();
        if (!displayer.bgHandler.hasMessages(START.what())) {
          BrailleDisplayLog.v(TAG, "stop a thread");
          displayer.bgThread.quitSafely();
        }
      }
    },
    WRITE_BRAILLE_DOTS {
      @Override
      public void handle(Displayer displayer, Message message) {
        byte[] brailleDotBytes = (byte[]) message.obj;
        displayer.callback.writeBrailleDots(brailleDotBytes);
      }
    },
    READ_COMMAND {
      @Override
      public void handle(Displayer displayer, Message message) {
        int commandComplex = displayer.callback.readCommand();
        if (commandComplex < 0) {
          return;
        }
        int cmd = commandComplex & COMMAND_CODE_MASK;
        int arg = (commandComplex & COMMAND_ARGUMENT_MASK) >> COMMAND_ARGUMENT_SHIFT;
        long eventTime = SystemClock.uptimeMillis();
        BrailleInputEvent brailleInputEvent = new BrailleInputEvent(cmd, arg, eventTime);
        displayer
            .mainHandler
            .obtainMessage(MessageMain.READ_COMMAND_ARRIVED.what(), brailleInputEvent)
            .sendToTarget();
      }
    };

    private static final String KEY_PARAMETER = "parameter";
    private static final String KEY_MANUAL = "manual";
    private static final String KEY_PROPERTIES = "properties";

    public int what() {
      return ordinal();
    }

    abstract void handle(Displayer displayer, Message message);
  }

  @VisibleForTesting
  Looper testing_getBackgroundLooper() {
    return bgThread.getLooper();
  }
}
