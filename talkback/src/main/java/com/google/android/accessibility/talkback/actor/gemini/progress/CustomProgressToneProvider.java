package com.google.android.accessibility.talkback.actor.gemini.progress;

import com.google.android.accessibility.talkback.Feedback;

/**
 * Provides sounds for a progress indication. Replace the implementation with your own sound files.
 */
public class CustomProgressToneProvider implements ProgressToneProvider {

  @Override
  public InfiniteIterator<Tone> getNextToneGroup() {
    // TODO: You should replace this with your own earcon sound file
    return () -> new Tone(Feedback.speech("Working..."), 1000);
  }
}
