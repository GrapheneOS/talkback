package com.google.android.accessibility.talkback.actor.gemini.progress;

import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.R;

/** Provides progress/loading sounds. */
public class DefaultProgressToneProvider implements ProgressToneProvider {
  /**
   * Usually this should be the duration of each file in progressToneGroups. However, those files
   * have lots of blank silence after them, so we end them a bit early.
   */
  private static final int EARCON_PLAY_CYCLE_MS = 3000;

  @Override
  public InfiniteIterator<Tone> getNextToneGroup() {
    return () ->
        new Tone(
            Feedback.sound(R.raw.loading, /* ignoreVolumeAdjustment= */ false),
            EARCON_PLAY_CYCLE_MS);
  }
}
