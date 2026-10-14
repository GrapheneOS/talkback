package com.google.android.accessibility.talkback.actor.gemini.progress;

import com.google.android.accessibility.talkback.Feedback;

/** Provides an infinite sequence of earcon tones to indicate loading progress. */
public interface ProgressToneProvider {

  /**
   * An earcon to play, along with how long to wait after starting playing it before starting the
   * next Tone.
   */
  class Tone {
    /** The earcon to play */
    public final Feedback.Part.Builder tone;

    /**
     * How long to wait after <i>starting</i> playback of {@link #tone} before starting the next, in
     * milliseconds. i.e. this should include the time it takes to play the tone.
     */
    public final int delayMs;

    public Tone(Feedback.Part.Builder tone, int delayMs) {
      this.tone = tone;
      this.delayMs = delayMs;
    }
  }

  /** Iterator that always returns a next item, forever. */
  interface InfiniteIterator<T> {
    T next();
  }

  /**
   * Call at the beginning of a progress session. The returned tones should be played in sequence,
   * until loading is finished.
   *
   * <p>This allows us to choose randomly from groups of similar tones, in case we want to mix it up
   * for the user each time.
   */
  InfiniteIterator<Tone> getNextToneGroup();
}
