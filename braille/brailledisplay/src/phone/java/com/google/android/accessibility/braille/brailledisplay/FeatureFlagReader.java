package com.google.android.accessibility.braille.brailledisplay;

import static com.google.android.accessibility.utils.FeatureSupport.supportInputConnectionByA11yService;

import android.content.Context;

/** Reader class for accessing feature flags from Google experimentation framework. */
public final class FeatureFlagReader {

  /** Whether braille display hid protocol supported. */
  public static boolean isBdHidSupported(Context context) {
    return true;
  }

  /** Whether to enable select current from current cursor to start or end. */
  public static boolean useSelectCurrentToStartOrEnd(Context context) {
    return supportInputConnectionByA11yService();
  }

  /** Whether to use popup message. */
  public static boolean usePopupMessage(Context context) {
    return true;
  }

  /** Whether to use show captions. */
  public static boolean useShowCaptions(Context context) {
    return false;
  }

  /** Whether to enable braille display learn mode. */
  public static boolean enableBrailleDisplayLearnMode(Context context) {
    return false;
  }

  /** Whether to use browse mode. */
  public static boolean useBrowseMode(Context context) {
    return true;
  }

  private FeatureFlagReader() {}
}
