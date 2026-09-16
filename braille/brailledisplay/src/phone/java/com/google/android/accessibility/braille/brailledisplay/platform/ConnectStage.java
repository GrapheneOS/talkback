package com.google.android.accessibility.braille.brailledisplay.platform;

/** Defines the various stages in a connection process. */
public enum ConnectStage {
  SERIAL,
  HID,
  RFCOMM,
  BRLTTY
}
