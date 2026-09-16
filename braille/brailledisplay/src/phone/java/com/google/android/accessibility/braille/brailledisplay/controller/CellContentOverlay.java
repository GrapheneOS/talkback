package com.google.android.accessibility.braille.brailledisplay.controller;

import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import java.util.ArrayList;
import java.util.List;

/** A class that updates the cell content. */
public class CellContentOverlay {
  private int overlayIndex = 0;
  private final List<BrailleWord> list = new ArrayList<>();

  /** Sets a plain BrailleWord and the overlay content to be updated. */
  public void setOverlay(List<BrailleWord> overlay) {
    this.list.clear();
    this.list.addAll(overlay);
  }

  /** Updates the overlay. Iterate through the list of overlays in a loop until cancel. */
  public void update() {
    overlayIndex++;
    // When the index reaches the last one, then the next is the first one.
    overlayIndex %= list.size();
  }

  /** Returns the updated content. */
  @Nullable
  public BrailleWord getOverlay() {
    return list == null ? null : list.get(overlayIndex);
  }
}
