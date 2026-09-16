package com.google.android.accessibility.brailleime;

import static com.google.android.accessibility.utils.FeatureSupport.supportInputConnectionByA11yService;

import android.content.Context;

/** Reader class for flags of experimental feature. */
public final class FeatureFlagReader {

  /** Whether to enable select current from current cursor to start or end. */
  public static boolean useSelectCurrentToStartOrEnd(Context context) {
    return supportInputConnectionByA11yService();
  }

  /** Whether to enable braille ime on device without five touch points. */
  public static boolean enableBrailleImeOnDeviceWithoutFivePointers(Context context) {
    return false;
  }

  /** Whether to use gesture customization. */
  public static boolean useGestureCustomization(Context context) {
    return true;
  }

  private FeatureFlagReader() {}
}
