package com.google.android.accessibility.braille.common.lib;

/** Interface that listens for changes. */
public interface Receiver<R> {

  /** Registers this receiver. */
  R registerSelf();

  /** Unregisters this receiver. */
  void unregisterSelf();
}
