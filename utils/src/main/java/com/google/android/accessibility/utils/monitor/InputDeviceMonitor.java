package com.google.android.accessibility.utils.monitor;

import android.content.Context;
import android.hardware.input.InputManager;
import android.view.InputDevice;
import android.view.KeyEvent;

/** A wrapper class to isolate the untestable InputManager dependency. */
public class InputDeviceMonitor {

  private final Context context;

  private static final int[] alphabeticKeyCodes;

  static {
    alphabeticKeyCodes = new int[26];
    for (int i = 0; i < 26; i++) {
      alphabeticKeyCodes[i] = KeyEvent.KEYCODE_A + i;
    }
  }

  public InputDeviceMonitor(Context context) {
    this.context = context.getApplicationContext();
  }

  /**
   * Checks if any physical, alphabetic keyboard (built-in, USB, Bluetooth) is connected. This
   * ignores non-alphabetic "keyboards" like power/volume buttons.
   *
   * @return True if a physical, alphabetic keyboard is detected, false otherwise.
   */
  public boolean hasPhysicalKeyboard() {
    InputManager inputManager = (InputManager) this.context.getSystemService(Context.INPUT_SERVICE);
    if (inputManager == null) {
      return false;
    }

    for (int deviceId : inputManager.getInputDeviceIds()) {
      InputDevice device = inputManager.getInputDevice(deviceId);
      if (device != null && !device.isVirtual()) {
        boolean hasKeyboardSource = (device.getSources() & InputDevice.SOURCE_KEYBOARD) != 0;
        boolean hasAlphabeticKeys = false;
        boolean[] hasKeys = device.hasKeys(alphabeticKeyCodes);
        for (boolean hasKey : hasKeys) {
          if (hasKey) {
            hasAlphabeticKeys = true;
            break;
          }
        }

        if (hasKeyboardSource && hasAlphabeticKeys) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Checks if any pointing device that acts like a mouse is connected. This specifically excludes
   * the phone's main touchscreen.
   *
   * @return True if a mouse, trackpad, or stylus is detected, false otherwise.
   */
  public boolean hasPointingDevice() {
    InputManager inputManager = (InputManager) this.context.getSystemService(Context.INPUT_SERVICE);
    if (inputManager == null) {
      return false;
    }

    int pointingSources =
        InputDevice.SOURCE_MOUSE
            | InputDevice.SOURCE_STYLUS
            | InputDevice.SOURCE_TOUCHPAD
            | InputDevice.SOURCE_TRACKBALL
            | InputDevice.SOURCE_CLASS_TRACKBALL;

    for (int deviceId : inputManager.getInputDeviceIds()) {
      InputDevice device = inputManager.getInputDevice(deviceId);
      if (device != null && !device.isVirtual()) {

        boolean isExternal = device.isExternal();
        boolean hasPointerSource = (device.getSources() & pointingSources) != 0;

        if (isExternal && hasPointerSource) {
          return true;
        }
      }
    }
    return false;
  }

  public boolean hasTouchScreen() {
    InputManager inputManager = (InputManager) this.context.getSystemService(Context.INPUT_SERVICE);
    if (inputManager == null) {
      return false;
    }

    for (int deviceId : inputManager.getInputDeviceIds()) {
      InputDevice device = inputManager.getInputDevice(deviceId);
      if (device != null) {
        boolean hasTouchscreenSource = (device.getSources() & InputDevice.SOURCE_TOUCHSCREEN) != 0;
        if (hasTouchscreenSource) {
          return true;
        }
      }
    }
    return false;
  }
}
