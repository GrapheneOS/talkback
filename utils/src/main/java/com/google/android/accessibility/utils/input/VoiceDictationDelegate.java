package com.google.android.accessibility.utils.input;

import android.view.accessibility.AccessibilityEvent;

/** Delegates voice dictation related data to target instances. */
public interface VoiceDictationDelegate {
  void onTextEventAndInterpretation(
      AccessibilityEvent event, TextEventInterpretation interpretation);
}
