package com.google.android.accessibility.utils.output;

import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import com.google.android.accessibility.utils.Logger;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * Provides a common interface for playing synthesized speech data from the memory. This class is
 * responsible for managing the lifecycle of speech synthesis, including loading, playback, and
 * state notifications.
 */
public abstract class SpeechCachePlayer {

  @Nullable Handler handler;

  @Nullable private final Executor callbackExecutor;

  public SpeechCachePlayer(@Nullable Handler handler, @Nullable Executor callbackExecutor) {
    this.handler = handler;
    this.callbackExecutor = callbackExecutor;
  }

  public SpeechCachePlayer(@Nullable Executor callbackExecutor) {
    this(null, callbackExecutor);
  }

  /** Dumps debugging information about the cached speech data. */
  public abstract void dumpCachedSpeechInfo(Logger logger);

  /** Removes a specific speech item from the cache. */
  public abstract void removeCache(SpeechInfo speechInfo);

  /** Clears the entire speech caches. */
  public abstract void cleanCaches();

  /**
   * Loads a WAV file, extracts its PCM data into a byte array, and stores it. If this is the first
   * file and the player hasn't initialized with a format, its format will be used to initialize
   * AudioTrack. Subsequent files MUST match this format.
   *
   * @param speechInfo the information about the speech
   * @return {@code true} if the request success.
   */
  public abstract boolean loadSpeechFileIntoMemory(SpeechInfo speechInfo);

  /** Returns true if audio is currently playing. */
  public abstract boolean isPlaying();

  /** Stops the currently playing speech. */
  public abstract void stopSpeech();

  /** Releases any resources held by the player. */
  public abstract void release();

  /**
   * Checks if the speech data for the given {@link SpeechInfo} is already cached in memory.
   *
   * @param speechInfo The speech information to check for in the cache.
   * @return {@code true} if the speech data is cached, {@code false} otherwise.
   */
  public abstract boolean hasCache(SpeechInfo speechInfo);

  /**
   * Plays the given cached speech with given {@link SpeechInfo}. The playback should have its own
   * thread.
   *
   * @param speechInfo the speech information to play
   * @param playStateCallback the listener for playback state changes
   * @return true if the speech is played, false if the associated speech is not found.
   */
  public abstract boolean playSpeech(
      SpeechInfo speechInfo, PlaybackStateListener playStateCallback);

  /**
   * Dispatches the playback state via the callback executor.
   *
   * @param item The playback item to dispatch the state to.
   * @param state The playback state to dispatch.
   */
  public void dispatchPlaybackState(SpeechPlaybackItem item, @State int state) {
    item.setState(state);
    if (callbackExecutor != null) {
      callbackExecutor.execute(() -> item.dispatchPlaybackStatus(state));
    } else {
      item.dispatchPlaybackStatus(state);
    }
  }

  /** Gets the memory usage of the speech cache in MB. */
  public abstract long getCacheSizeMb();

  public static final int STATE_UNKNOWN = 0;

  /** The initial state where the player is prepared but not yet started. */
  public static final int STATE_PREPARED = 1;

  /** State indicating that the request is being processed. */
  public static final int STATE_START = 2;

  /** State indicating that audio chunks are available and ready for playback. */
  public static final int STATE_READY = 3;

  /** State after the first audio chunk has been written to the audio track. */
  public static final int STATE_PLAYED = 4;

  /** State indicating that playback has been stopped by a user or the system. */
  public static final int STATE_STOPPED = 5;

  /** State indicating that playback has completed normally. */
  public static final int STATE_COMPLETED = 6;

  /** The state indicating that an error has occurred. */
  public static final int STATE_ERROR = 7;

  /** The state of the playback. */
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({
    STATE_UNKNOWN, // Optional, but good practice
    STATE_PREPARED,
    STATE_START,
    STATE_READY,
    STATE_PLAYED,
    STATE_STOPPED,
    STATE_COMPLETED,
    STATE_ERROR,
  })
  public @interface State {}

  private static String stateToString(@State int state) {
    if (state == STATE_UNKNOWN) {
      return "STATE_UNKNOWN";
    }
    if (state == STATE_PLAYED) {
      return "STATE_PLAYED";
    }
    if (state == STATE_STOPPED) {
      return "STATE_STOPPED";
    }
    if (state == STATE_COMPLETED) {
      return "STATE_COMPLETED";
    }
    if (state == STATE_ERROR) {
      return "STATE_ERROR";
    }
    if (state == STATE_READY) {
      return "STATE_READY";
    }
    if (state == STATE_START) {
      return "STATE_START";
    }
    if (state == STATE_PREPARED) {
      return "STATE_PREPARED";
    }
    return Integer.toString(state);
  }

  /** The listener to notify the playback state. */
  public interface PlaybackStateListener {
    void onStateChanged(String utteranceId, @State int state);
  }

  /**
   * Encapsulates all the information needed to synthesize and play a piece of text.
   *
   * <p>This class holds the text to be spoken, its locale, and various Text-To-Speech (TTS)
   * parameters such as pitch, rate, and volume. It also includes an optional reference to a file
   * {@link #speechFile} which can be used for caching the synthesized audio.
   *
   * <p>Equality of {@link SpeechInfo} objects is determined by the text content and locale, which
   * is used for caching purposes. Note: This class does not support {@link TtsSpan}.
   */
  public static final class SpeechInfo {
    @Nullable private final Locale locale;
    public final String text;

    public final CharSequence charsequence;
    public final int pitch;
    public final int rate;
    public final float volume;
    public final TtsSpan[] ttsSpans;

    /* The utterance id used to identify the speech info. */
    @Nullable public final String utteranceId;

    @Nullable public final File speechFile;

    public SpeechInfo(
        CharSequence text,
        @Nullable String utteranceId,
        @Nullable Locale locale,
        float pitch,
        float rate) {
      SpannableString subsequenceWithSpans = SpannableString.valueOf(text);
      ttsSpans = subsequenceWithSpans.getSpans(0, text.length(), TtsSpan.class);
      this.utteranceId = utteranceId;
      this.charsequence = text;
      this.text = text.toString();
      this.locale = locale;
      this.pitch = (int) (pitch * 100);
      this.rate = (int) (rate * 100);
      this.volume = 1.0f;
      this.speechFile = null;
    }

    public SpeechInfo(
        @Nullable String utteranceId,
        CharSequence text,
        @Nullable Locale locale,
        Bundle parameter) {
      this(utteranceId, text, locale, parameter, /* speechFile= */ null);
    }

    public SpeechInfo(
        @Nullable String utteranceId,
        CharSequence text,
        @Nullable Locale locale,
        Bundle ttsParameters,
        @Nullable File speechFile) {
      SpannableString subsequenceWithSpans = SpannableString.valueOf(text);
      ttsSpans = subsequenceWithSpans.getSpans(0, text.length(), TtsSpan.class);
      this.utteranceId = utteranceId;
      this.charsequence = text;
      this.text = text.toString();
      this.locale = locale;
      this.pitch = ttsParameters.getInt(FailoverTextToSpeech.SpeechParam.PITCH, 100);
      this.rate = ttsParameters.getInt(FailoverTextToSpeech.SpeechParam.RATE, 100);
      this.volume = ttsParameters.getFloat(FailoverTextToSpeech.SpeechParam.VOLUME, 1.0f);
      this.speechFile = speechFile;
    }

    public void deleteSpeechFile(@Nullable Executor executor) {
      if (speechFile == null || !speechFile.exists()) {
        return;
      }
      if (executor != null) {
        executor.execute(speechFile::delete);
      } else {
        var unused = speechFile.delete();
      }
    }

    public boolean hasTtsSpan() {
      return ttsSpans.length != 0;
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof SpeechInfo that)) {
        return false;
      }
      // We don't support the speech having TtsSpan for now, so treat them are unequals if one of
      // them has TtsSpan.
      if (hasTtsSpan() || that.hasTtsSpan()) {
        return false;
      }
      return TextUtils.equals(text, that.text)
          && locale != null
          && locale.equals(that.locale)
          && pitch == that.pitch
          && rate == that.rate;
    }

    /**
     * Returns a string representation of this {@link SpeechInfo} object for debugging purposes.
     * This representation includes the text and locale.
     *
     * @return A string containing the text and locale of this speech info.
     */
    public String dump() {
      return "SpeechInfo{"
          + "text="
          + charsequence
          + ", locale='"
          + locale
          + "'"
          + ", pitch='"
          + pitch
          + ", rate='"
          + rate
          + "}";
    }

    @Override
    public String toString() {
      return "SpeechInfo{"
          + "utteranceId="
          + utteranceId
          + ", text="
          + charsequence
          + ", pitch="
          + pitch
          + ", rate="
          + rate
          + ", volume="
          + volume
          + ", locale="
          + locale
          + "'"
          + "}";
    }

    /** Since we only support one engine, comparing the locale and the text is enough. */
    @Override
    public int hashCode() {
      return Objects.hash(text, locale);
    }
  }

  /** A class to hold the information of a speech playback item. */
  public abstract static class SpeechPlaybackItem implements Runnable {
    private static final String TAG = "SpeechPlaybackItem";
    private final PlaybackStateListener stateListener;
    final String utteranceId;
    final SpeechInfo speechInfo;
    @State private volatile int state;

    public SpeechPlaybackItem(
        String utteranceId, PlaybackStateListener listener, SpeechInfo speechInfo) {
      this.utteranceId = utteranceId;
      this.stateListener = listener;
      this.speechInfo = speechInfo;
      this.state = STATE_READY;
    }

    public void dispatchPlaybackStatus(@State int state) {
      stateListener.onStateChanged(utteranceId, state);
      LogUtils.d(TAG, "dispatchPlaybackStatus %s for %s", stateToString(state), utteranceId);
    }

    public void setState(@State int state) {
      this.state = state;
    }

    public boolean isInterrupted() {
      return state == STATE_ERROR || state == STATE_COMPLETED || state == STATE_STOPPED;
    }

    public int getState() {
      return state;
    }
  }
}
