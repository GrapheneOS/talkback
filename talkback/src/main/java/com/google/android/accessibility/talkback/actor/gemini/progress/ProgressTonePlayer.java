package com.google.android.accessibility.talkback.actor.gemini.progress;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressToneProvider.InfiniteIterator;
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressToneProvider.Tone;
import com.google.android.accessibility.utils.Consumer;
import com.google.android.accessibility.utils.Supplier;

/** Schedules and sequences playing a sequence of earcons to indicate loading progress */
public class ProgressTonePlayer {

  private static final int DELAY_PLAY_TONE_MS = 2500;
  private final Handler handler;
  private final ProgressToneProvider toneProvider;
  private final Consumer<Tone> tonePlayer;
  private final Runnable onTimeoutListener;
  private final Supplier<Long> currentTimeMsSupplier;
  private final long timeoutDelayMs;

  @Nullable private InfiniteIterator<Tone> tones;
  private long endTimeMs;

  /**
   * @param toneProvider provides the set of tones to play for each invocation of {@link
   *     #play(boolean)}
   * @param tonePlayer given a Tone, actually plays the sound via the feedback {@link Pipeline}
   * @param timeoutDelayMs how long to play tones before timing out
   * @param onTimeoutListener called after timeoutDelayMs is reached after calling {@link
   *     #play(boolean)}
   */
  public ProgressTonePlayer(
      ProgressToneProvider toneProvider,
      Consumer<Tone> tonePlayer,
      long timeoutDelayMs,
      Runnable onTimeoutListener) {
    this(
        toneProvider,
        tonePlayer,
        timeoutDelayMs,
        onTimeoutListener,
        new Handler(Looper.getMainLooper()),
        SystemClock::elapsedRealtime);
  }

  /**
   * @param toneProvider provides the set of tones to play for each invocation of {@link
   *     #play(boolean)}
   * @param tonePlayer given a Tone, actually plays the sound via the feedback {@link Pipeline}
   * @param timeoutDelayMs how long to play tones before timing out
   * @param onTimeoutListener called after timeoutDelayMs is reached
   * @param handler handler to use for testing
   * @param currentTimeMsSupplier returns the current elapsed system realtime in milliseconds
   */
  @VisibleForTesting
  public ProgressTonePlayer(
      ProgressToneProvider toneProvider,
      Consumer<Tone> tonePlayer,
      long timeoutDelayMs,
      Runnable onTimeoutListener,
      Handler handler,
      Supplier<Long> currentTimeMsSupplier) {
    this.handler = handler;
    this.toneProvider = toneProvider;
    this.tonePlayer = tonePlayer;
    this.onTimeoutListener = onTimeoutListener;
    this.currentTimeMsSupplier = currentTimeMsSupplier;
    this.timeoutDelayMs = timeoutDelayMs;
  }

  /**
   * Starts playback of the tones.
   *
   * @param playTone if true, plays the tones. If false, simply waits for the timeout without
   *     playing any tones.
   * @param delayPlayTone if true, delay the tone play.
   */
  public void play(boolean playTone, boolean delayPlayTone) {
    stop();
    long timeoutMs = currentTimeMsSupplier.get() + timeoutDelayMs;
    if (!playTone) {
      tones = null;
      handler.postDelayed(onTimeoutListener, timeoutDelayMs);
      return;
    }

    tones = toneProvider.getNextToneGroup();
    if (delayPlayTone) {
      handler.postDelayed(() -> playTonesInternal(timeoutMs), DELAY_PLAY_TONE_MS);
      return;
    }

    playTonesInternal(timeoutMs);
  }

  /** Stops playback and cancels pending tones. Does not call onTimeoutListener. */
  public void stop() {
    stopTones();
    handler.removeCallbacksAndMessages(null);
  }

  public void stopTones() {
    tones = null;
  }

  private void playTonesInternal(long endTimeMs) {
    this.endTimeMs = endTimeMs;
    playNextTone();
  }

  private void playNextTone() {
    if (currentTimeMsSupplier.get() >= endTimeMs) {
      onTimeoutListener.run();
      return;
    }

    if (tones == null) {
      long timeoutMs = endTimeMs - currentTimeMsSupplier.get();
      if (timeoutMs > 0) {
        handler.postDelayed(onTimeoutListener, timeoutMs);
      } else {
        onTimeoutListener.run();
      }
      return;
    }

    Tone tone = tones.next();
    tonePlayer.accept(tone);
    handler.postDelayed(this::playNextTone, tone.delayMs);
  }
}
