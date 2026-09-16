package com.google.android.accessibility.talkback.actor;

import com.google.android.accessibility.braille.brailledisplay.BrailleDisplay;
import com.google.android.accessibility.utils.Supplier;

/** Braille display action performer. */
public class BrailleDisplayActor {
  private final Supplier<BrailleDisplay> brailleDisplaySupplier;

  public BrailleDisplayActor(Supplier<BrailleDisplay> brailleDisplaySupplier) {
    this.brailleDisplaySupplier = brailleDisplaySupplier;
  }

  /** Switches the braille display to on or off. */
  public void switchBrailleDisplayOnOrOff() {
    brailleDisplaySupplier.get().switchBrailleDisplayOnOrOff();
  }

  /** Toggles the braille contracted mode to contracted or uncontracted. */
  public void toggleBrailleContractedMode() {
    brailleDisplaySupplier.get().toggleBrailleContractedMode();
  }

  /** Toggles the braille on screen overlay to on or off. */
  public void toggleBrailleOnScreenOverlay() {
    brailleDisplaySupplier.get().toggleBrailleOnScreenOverlay();
  }
}
