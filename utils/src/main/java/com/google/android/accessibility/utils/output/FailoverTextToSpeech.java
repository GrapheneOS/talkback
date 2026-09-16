/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.utils.output;

import static android.speech.tts.TextToSpeech.LANG_AVAILABLE;
import static android.speech.tts.TextToSpeech.LANG_COUNTRY_AVAILABLE;
import static android.speech.tts.TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
import static android.speech.tts.TextToSpeech.QUEUE_ADD;
import static androidx.core.content.ContextCompat.RECEIVER_EXPORTED;
import static com.google.android.accessibility.utils.output.SpeechCacheManager.CACHE_UTTERANCE_ID_PREFIX;
import static java.util.Locale.forLanguageTag;

import android.content.ComponentCallbacks;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings.Secure;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.Engine;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;
import android.telephony.TelephonyManager;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.LocaleSpan;
import android.text.style.TtsSpan;
import android.util.Log;
import android.util.Pair;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.ChangeLocaleAction;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.SpannableUtils.IdentifierSpan;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.accessibility.utils.broadcast.SameThreadBroadcastReceiver;
import com.google.android.accessibility.utils.compat.provider.SettingsCompatUtils.SecureCompatUtils;
import com.google.android.accessibility.utils.compat.speech.tts.TextToSpeechCompatUtils;
import com.google.android.accessibility.utils.output.SpeechCacheManager.LoadSpeechResultNotifier;
import com.google.android.accessibility.utils.output.SpeechCachePlayer.PlaybackStateListener;
import com.google.android.accessibility.utils.output.SpeechCachePlayer.SpeechInfo;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.auto.value.AutoValue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Wrapper for {@link TextToSpeech} that handles fail-over when a specific engine does not work.
 *
 * <p>Does <strong>NOT</strong> implement queuing! Every call to {@link #speak} flushes the global
 * speech queue.
 *
 * <p>This wrapper handles the following:
 *
 * <ul>
 *   <li>Fail-over from a failing TTS to a working one
 *   <li>Splitting utterances into &lt;4k character chunks
 *   <li>Switching to the system TTS when media is unmounted
 *   <li>Utterance-specific pitch and rate changes
 *   <li>Pitch and rate changes relative to the user preference
 * </ul>
 */
@SuppressWarnings("deprecation")
public class FailoverTextToSpeech {
  private static final String TAG = "FailoverTextToSpeech";

  /** The package name for the Google TTS engine. */
  private static final String PACKAGE_GOOGLE_TTS = "com.google.android.tts";

  /** Number of times a TTS engine can fail before switching. */
  private static final int MAX_TTS_FAILURES = 3;

  /** Maximum number of TTS error messages to print to the log. */
  private static final int MAX_LOG_MESSAGES = 10;

  public static final String AGGRESSIVE_CHUNK = "AggressiveChunk";

  public static final int VALUE_ON = 1;

  // Some apps have their own rate setting and send the absolute rate
  // Others have rate increase/decrease controls and just send the multiplier
  public static final String RATE_PARAMETER_TYPE = "RateParameterType";
  public static final int MULTIPLIER = 0; // This is the default
  public static final int ABSOLUTE = 1;

  /**
   * Ensure the consecutive manipulation is thread safe. For example, to synthesize file with give
   * locale, we need to invoke {@link TextToSpeech#setLanguage(Locale)} and {@link
   * TextToSpeech#synthesizeToFile(CharSequence, Bundle, File, String)}.
   */
  private final Object ttsLock = new Object();

  /** Removes the speech from the speech cache with the given {@link SpeechInfo}. */
  void removeSpeech(SpeechInfo speechInfo) {
    if (speechCacheManager != null) {
      speechCacheManager.removeSpeech(speechInfo);
    }
  }

  /** Clears the entire in-memory speech cache. */
  void cleanCache() {
    if (speechCacheManager != null) {
      speechCacheManager.cleanCache();
    }
  }

  /**
   * Sets the voice for the TTS engine.
   *
   * @param voice The voice to set.
   */
  public void setVoice(Voice voice) {
    if (isReady()) {
      synchronized (ttsLock) {
        tts.setVoice(voice);
      }
    }
  }

  public long getCacheSizeMb() {
    if (speechCacheManager != null) {
      return speechCacheManager.getCacheSizeMb();
    }
    return -1;
  }

  /** Class defining constants used for describing speech parameters. */
  public static final class SpeechParam {
    /** Float parameter for controlling speech volume. Range is {0 ... 2}. */
    public static final String VOLUME = Engine.KEY_PARAM_VOLUME;

    /** Float parameter for controlling speech rate. Range is {0 ... 2}. */
    public static final String RATE = "rate";

    /** Float parameter for controlling speech pitch. Range is {0 ... 2}. */
    public static final String PITCH = "pitch";

    public static final String FALLBACK_LOCALE =
        "com.google.android.tts:EnableEnUsVoiceSelectionFallback";

    private SpeechParam() {}
  }

  /**
   * Constant to flush speech globally. The constant corresponds to the non-public API {@link
   * TextToSpeech#QUEUE_DESTROY}. To avoid a bug, we always need to use {@link
   * TextToSpeech#QUEUE_FLUSH} before using {@link #SPEECH_FLUSH_ALL} -- on Android version M only.
   */
  static final int SPEECH_FLUSH_ALL = 2;

  /**
   * What fraction of the volume seekbar corresponds to a doubling of audio volume.
   *
   * <p>During a phone call, TalkBack speech is redirected from STREAM_MUSIC to STREAM_VOICE_CALL,
   * causing an unexpected change in TalkBack speech volume. During a phone call, we reduce the
   * TalkBack speech volume based on the volume difference between STREAM_MUSIC and
   * STREAM_VOICE_CALL. VOLUME_FRAC_PER_DOUBLING controls the amount of volume reduction per
   * difference of STREAM_MUSIC vs STREAM_VOICE_CALL.
   *
   * <p>On nexus 6, volume doubles every 11% volume seekbar step. On samsung s5, volume doubles
   * every 27% volume step. Setting adjustment too aggressively (too low) causes effective volume to
   * go down when call volume is higher -- the call volume seekbar would work in reverse for
   * TalkBack speech. Setting this adjustment too conservatively (too high) causes the original
   * volume jump to continue, though in lesser degree.
   */
  private static final float VOLUME_FRAC_PER_DOUBLING = 0.25f;

  /**
   * {@link BroadcastReceiver} for determining changes in the media state used for switching the TTS
   * engine.
   */
  private final MediaMountStateMonitor mediaStateMonitor = new MediaMountStateMonitor();

  /** A list of installed TTS engines. */
  private final LinkedList<String> installedTtsEngines = new LinkedList<>();

  private final Context context;
  private final ContentResolver resolver;

  /** The TTS engine. */
  private TextToSpeech tts;

  /** The engine loaded into the current TTS. */
  private String ttsEngine;

  /** The number of time the current TTS has failed consecutively. */
  private int ttsFailures;

  /** The package name of the preferred TTS engine. */
  private String defaultTtsEngine;

  /** The package name of the system TTS engine. */
  private String systemTtsEngine;

  /** A temporary TTS used for switching engines. */
  private @Nullable TextToSpeech tempTts;

  /** The engine loading into the temporary TTS. */
  private @Nullable String tempTtsEngine;

  /** The rate adjustment specified in {@link Settings}. */
  private float defaultRate;

  /** The pitch adjustment specified in {@link Settings}. */
  private float defaultPitch;

  private final List<FailoverTtsListener> listeners = new ArrayList<>();

  /** Wake lock for keeping the device unlocked while reading */
  private WakeLock wakeLock;

  private final AudioManager audioManager;
  private final TelephonyManager telephonyManager;

  private boolean shouldHandleTtsCallbackInHandlerThread = true;

  /**
   * A buffer of N most recent utterance ids, used to ensure that a recent utterance's completion
   * handler does not unlock a WakeLock used by the currently speaking utterance.
   */
  private final Deque<String> recentUtteranceIds =
      new ConcurrentLinkedDeque<>(); // may contain nulls

  private final @Nullable SpeechCacheManager speechCacheManager;

  public FailoverTextToSpeech(Context context) {
    this(context, /* enableSpeechCache= */ false);
  }

  /**
   * Constructs a {@link FailoverTextToSpeech} instance.
   *
   * @param context the context to use
   * @param enableSpeechCache whether to enable local speech cache
   */
  public FailoverTextToSpeech(Context context, boolean enableSpeechCache) {
    this.context = context;
    ContextCompat.registerReceiver(
        context, mediaStateMonitor, mediaStateMonitor.getFilter(), RECEIVER_EXPORTED);
    if (enableSpeechCache) {
      HandlerThread handlerThread =
          new HandlerThread("SpeechCacheManager", Process.THREAD_PRIORITY_AUDIO);
      handlerThread.start();
      Looper looper = handlerThread.getLooper();
      speechCacheManager = new SpeechCacheManager(context, looper);
    } else {
      speechCacheManager = null;
    }

    final Uri defaultSynth = Secure.getUriFor(Secure.TTS_DEFAULT_SYNTH);
    final Uri defaultPitch = Secure.getUriFor(Secure.TTS_DEFAULT_PITCH);
    final Uri defaultRate = Secure.getUriFor(Secure.TTS_DEFAULT_RATE);

    resolver = context.getContentResolver();
    resolver.registerContentObserver(defaultSynth, false, mSynthObserver);
    resolver.registerContentObserver(defaultPitch, false, mPitchObserver);
    resolver.registerContentObserver(defaultRate, false, mRateObserver);

    registerGoogleTtsFixCallbacks();

    updateDefaultPitch();
    updateDefaultRate();

    // Updating the default engine reloads the list of installed engines and
    // the system engine. This also loads the default engine.
    updateDefaultEngine();

    // connect to system services
    initWakeLock(context);
    audioManager = (AudioManager) this.context.getSystemService(Context.AUDIO_SERVICE);
    telephonyManager = (TelephonyManager) this.context.getSystemService(Context.TELEPHONY_SERVICE);
  }

  @VisibleForTesting(otherwise = VisibleForTesting.NONE)
  protected @Nullable Looper getSpeechCacheManagerLooper() {
    return speechCacheManager == null ? null : speechCacheManager.getLooper();
  }

  /** Separate function for overriding in unit tests, because WakeLock cannot be mocked. */
  protected void initWakeLock(Context context) {
    wakeLock =
        ((PowerManager) context.getSystemService(Context.POWER_SERVICE))
            .newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, TAG);
  }

  /**
   * Adds a new listener for changes in speaking state.
   *
   * @param listener The listener to add.
   */
  public void addListener(FailoverTtsListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes the given listener.
   *
   * @param listener The listener to remove.
   */
  public void removeListener(FailoverTtsListener listener) {
    listeners.remove(listener);
  }

  /**
   * Whether the text-to-speech engine is ready to speak.
   *
   * @return {@code true} if calling {@link #speak} is expected to succeed.
   */
  public boolean isReady() {
    return (tts != null);
  }

  /**
   * Returns the label for the current text-to-speech engine.
   *
   * @return The localized name of the current engine.
   */
  public @Nullable CharSequence getEngineLabel() {
    return TextToSpeechUtils.getLabelForEngine(context, ttsEngine);
  }

  /**
   * Returns the {@link TextToSpeech} instance that is currently being used as the engine.
   *
   * @return The engine instance.
   */
  @SuppressWarnings("UnusedDeclaration") // Used by analytics
  public TextToSpeech getEngineInstance() {
    return tts;
  }

  /**
   * Sets whether to handle TTS callback in handler thread. If {@code false}, the callback will be
   * handled in binder thread.
   */
  public void setHandleTtsCallbackInHandlerThread(boolean shouldHandleTtsCallbackInHandlerThread) {
    this.shouldHandleTtsCallbackInHandlerThread = shouldHandleTtsCallbackInHandlerThread;
  }

  /**
   * Speak the specified text.
   *
   * @param text The text to speak.
   * @param locale Language of the text.
   * @param pitch The pitch adjustment, in the range [0 ... 1].
   * @param rate The rate adjustment, in the range [0 ... 1].
   * @param params The parameters to pass to the text-to-speech engine.
   * @param customFlags to adapt information such as performance features.
   * @param flushGlobalTtsQueue Whether to flush the global TTS queue.
   * @param eventId {@link EventTypeId}
   */
  public void speak(
      CharSequence text,
      @Nullable Locale locale,
      float pitch,
      float rate,
      Map<String, String> params,
      Map<String, Integer> customFlags,
      int stream,
      float volume,
      boolean preventDeviceSleep,
      boolean flushGlobalTtsQueue,
      EventId eventId) {
    String utteranceId = params.get(Engine.KEY_PARAM_UTTERANCE_ID);
    addRecentUtteranceId(utteranceId);

    // Handle empty text immediately.
    if (TextUtils.isEmpty(text)) {
      mHandler.onUtteranceCompleted(params.get(Engine.KEY_PARAM_UTTERANCE_ID), /* success= */ true);
      return;
    }

    int result;

    volume *= calculateVolumeAdjustment();

    if (preventDeviceSleep && wakeLock != null && !wakeLock.isHeld()) {
      wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/);
    }

    Exception failureException = null;
    try {
      result =
          trySpeak(
              text,
              locale,
              pitch,
              rate,
              params,
              customFlags,
              stream,
              volume,
              flushGlobalTtsQueue,
              eventId);
    } catch (Exception e) {
      failureException = e;
      result = TextToSpeech.ERROR;
      allowDeviceSleep();
    }

    if (result == TextToSpeech.ERROR) {
      attemptTtsFailover(ttsEngine);
    }

    if ((result != TextToSpeech.SUCCESS) && params.containsKey(Engine.KEY_PARAM_UTTERANCE_ID)) {
      if (failureException != null) {
        LogUtils.w(TAG, "Failed to speak %s due to an exception", text);
        failureException.printStackTrace();
      } else {
        LogUtils.w(TAG, "Failed to speak %s", text);
      }

      mHandler.onUtteranceCompleted(params.get(Engine.KEY_PARAM_UTTERANCE_ID), /* success= */ true);
    }
  }

  /** Adjust volume if we are in a phone call and speaking with phone audio stream */
  private float calculateVolumeAdjustment() {
    float multiple = 1.0f;

    // Accessibility services will eventually have their own audio stream, making this
    // adjustment unnecessary.
    if (!BuildVersionUtils.isAtLeastN()) {

      // If we are in a phone call...
      // (Phone call state is often reported late, missing the first utterance.)
      if (telephonyManager != null) {
        int callState = telephonyManager.getCallState();
        if (callState != TelephonyManager.CALL_STATE_IDLE) {
          // find audio stream volumes
          if (audioManager != null) {
            int volumeMusic = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volumeMusic <= 0) {
              return 0.0f;
            }
            int volumeVoice = audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            int maxVolMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int maxVolVoice = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
            float volumeMusicFrac =
                (maxVolMusic <= 0) ? -1.0f : (float) volumeMusic / (float) maxVolMusic;
            float volumeVoiceCallFrac =
                (maxVolVoice <= 0) ? -1.0f : (float) volumeVoice / (float) maxVolVoice;
            // If phone volume is higher than talkback/media volume...
            if (0.0f <= volumeMusicFrac && volumeMusicFrac < volumeVoiceCallFrac) {
              // Reduce effective volume closer to media volume.
              // The UI volume seekbars have an exponential effect on volume,
              // but text-to-speech volume multiple has a linear effect.
              // So take the Nth root of the volume difference to reduce speech
              // volume multiplier exponentially, to match the volume seekbar effect.
              float diff = volumeVoiceCallFrac - volumeMusicFrac;
              float numberDoublingSteps = diff / VOLUME_FRAC_PER_DOUBLING;
              multiple = (float) Math.pow(2.0f, -numberDoublingSteps);
            }
          }
        }
      }
    }
    return multiple;
  }

  /** Releases the {@link WakeLock} */
  private void allowDeviceSleep() {
    allowDeviceSleep(null);
  }

  private void allowDeviceSleep(@Nullable String completedUtteranceId) {
    if (wakeLock != null && wakeLock.isHeld()) {
      boolean isRecent = recentUtteranceIds.contains(completedUtteranceId);
      boolean isLast = Objects.equals(recentUtteranceIds.peekLast(), completedUtteranceId);
      if (completedUtteranceId == null || isLast || !isRecent) {
        try {
          wakeLock.release();
        } catch (RuntimeException unused) {
          // Ignore: already released by timeout.
          // TODO: Log this exception from GoogleLogger.
        }
      }
    }
  }

  private void addRecentUtteranceId(String utteranceId) {
    recentUtteranceIds.add(utteranceId);
    while (recentUtteranceIds.size() > 10) {
      recentUtteranceIds.poll();
    }
  }

  @VisibleForTesting
  public List<String> getRecentUtteranceIds() {
    return Collections.unmodifiableList(new ArrayList<>(recentUtteranceIds));
  }

  /** Stops speech from all applications. No utterance callbacks will be sent. */
  public void stopAll() {
    try {
      allowDeviceSleep();
      ensureQueueFlush();
      tts.speak("", SPEECH_FLUSH_ALL, null);
    } catch (Exception e) {
      // Don't care, we're not speaking.
    }
  }

  /** Stops all speech that originated from TalkBack. No utterance callbacks will be sent. */
  public void stopFromTalkBack() {
    try {
      allowDeviceSleep();
      tts.speak("", TextToSpeech.QUEUE_FLUSH, null);
    } catch (Exception e) {
      // Don't care, we're not speaking.
    }
  }

  /**
   * Unregisters receivers, observers, and shuts down the text-to-speech engine. No calls should be
   * made to this object after calling this method.
   */
  public void shutdown() {
    allowDeviceSleep();
    context.unregisterReceiver(mediaStateMonitor);
    unregisterGoogleTtsFixCallbacks();
    mHandler.removeCallbacksAndMessages(null);

    resolver.unregisterContentObserver(mSynthObserver);
    resolver.unregisterContentObserver(mPitchObserver);
    resolver.unregisterContentObserver(mRateObserver);

    TextToSpeechUtils.attemptTtsShutdown(tts);
    tts = null;

    TextToSpeechUtils.attemptTtsShutdown(tempTts);
    tempTts = null;
    if (speechCacheManager != null) {
      speechCacheManager.shutDown();
    }
  }

  /**
   * Primes the TTS engine for the selected voice. This is recommended to minimize latency on first
   * synthesis.
   */
  public void primeTtsEngine() {
    if (tts == null) {
      addListener(
          new FailoverTtsListener() {
            @Override
            public void onTtsInitialized(boolean wasSwitchingEngines, String enginePackageName) {
              primeTtsEngineInternal();
            }

            @Override
            public void onUtteranceRangeStarted(String utteranceId, int start, int end) {}

            @Override
            public void onUtteranceCompleted(String utteranceId, boolean success) {}
          });
    } else {
      primeTtsEngineInternal();
    }
  }

  private void primeTtsEngineInternal() {
    if (tts == null) {
      return;
    }
    try {
      File tempFile = File.createTempFile("tmpsynthesize", null, context.getCacheDir());
      tts.synthesizeToFile("1 2 3", null, tempFile, "tmpsynthesize");
      tempFile.deleteOnExit();
    } catch (IOException e) {
      LogUtils.w(TAG, "Exception during TTS init:", e);
    }
  }

  /**
   * Attempts to speak the specified text.
   *
   * @param text to speak, must be under 3999 chars.
   * @param locale language to speak with. Use default language if it's null.
   * @param pitch to speak text in.
   * @param rate to speak text in.
   * @param params to the TTS.
   * @param customFlags to adapt information such as performance features.
   * @param flushGlobalTtsQueue If the global TTS queue should be flushed.
   * @param eventId event id
   * @return The result of speaking the specified text.
   */
  private int trySpeak(
      CharSequence text,
      @Nullable Locale locale,
      float pitch,
      float rate,
      Map<String, String> params,
      Map<String, Integer> customFlags,
      int stream,
      float volume,
      boolean flushGlobalTtsQueue,
      EventId eventId) {
    if (tts == null) {
      return TextToSpeech.ERROR;
    }

    float effectivePitch = (pitch * defaultPitch);
    float effectiveRate;
    if (customFlags.containsKey(RATE_PARAMETER_TYPE)
        && customFlags.get(RATE_PARAMETER_TYPE) == ABSOLUTE) {
      effectiveRate = rate;
    } else {
      effectiveRate = rate * defaultRate;
    }

    synchronized (ttsLock) {
      String utteranceId = params.get(Engine.KEY_PARAM_UTTERANCE_ID);
      boolean isLocaleAttached = locale != null;
      Locale previousLocale = getUsedLocale();
      boolean changeLocale = false;
      int queueMode = flushGlobalTtsQueue ? SPEECH_FLUSH_ALL : QUEUE_ADD;
      if (isLocaleAttached
          && speechCacheManager != null
          && speechCacheManager.hasCache(
              utteranceId, text, locale, queueMode, effectivePitch, effectiveRate)) {
        LogUtils.d(TAG, "cached speech available, don't need to change locale");
      } else if (isLocaleAttached && !locale.equals(mLastUtteranceLocale)) {
        localeInUse = attemptSetLanguage(locale);
        if (localeInUse != null) {
          mLastUtteranceLocale = locale;
        }
        changeLocale = true;
      } else if (!isLocaleAttached && (mLastUtteranceLocale != null)) {
        localeInUse = ensureSupportedLocale();
        mLastUtteranceLocale = null;
        changeLocale = true;
      }
      int changeLocaleAction = Performance.CHANGE_LOCALE_NONE;
      if (changeLocale) {
        changeLocaleAction = getChangeLocaleAction(previousLocale);
      }
      UtteranceInfoCombo.Builder utteranceInfoCombo =
          UtteranceInfoCombo.builder(text, localeInUse, isLocaleAttached, changeLocaleAction)
              .setFlushGlobalTtsQueue(flushGlobalTtsQueue);

      if ((text instanceof Spannable spannable)) {
        IdentifierSpan[] identifierSpans =
            spannable.getSpans(0, text.length(), IdentifierSpan.class);
        if (identifierSpans.length > 0) {
          utteranceInfoCombo.setIsSeparatorInUtterance(true);
          for (IdentifierSpan identifierSpan : identifierSpans) {
            spannable.removeSpan(identifierSpan);
          }
        }
      }

      if (customFlags.get(AGGRESSIVE_CHUNK) != null
          && customFlags.get(AGGRESSIVE_CHUNK) == VALUE_ON) {
        utteranceInfoCombo.setIsAggressiveChunking(true);
      }
      UtteranceInfoCombo utteranceInfo = utteranceInfoCombo.build();
      for (FailoverTtsListener mListener : listeners) {
        mListener.onBeforeUtteranceRequested(utteranceId, utteranceInfo);
      }
      int result =
          speak(
              text,
              locale,
              params,
              utteranceId,
              effectivePitch,
              effectiveRate,
              stream,
              volume,
              utteranceInfo,
              eventId);

      if (result != TextToSpeech.SUCCESS) {
        localeInUse = ensureSupportedLocale();
      }

      LogUtils.d(TAG, "Speak call for %s returned %d", utteranceId, result);
      return result;
    }
  }

  @ChangeLocaleAction
  private int getChangeLocaleAction(Locale previousLocale) {
    if (Objects.equals(localeInUse, previousLocale)) {
      return Performance.CHANGE_LOCALE_SAME;
    }
    if (localeInUse == null) {
      return Performance.CHANGE_LOCALE_UNDEFINED;
    }
    if (previousLocale != null
        && Objects.equals(previousLocale.getLanguage(), localeInUse.getLanguage())) {
      return Performance.CHANGE_LOCALE_SAME_LANGUAGE;
    }
    return Performance.CHANGE_LOCALE_DIFFERENT;
  }

  private int speak(
      CharSequence text,
      Locale locale,
      Map<String, String> params,
      String utteranceId,
      float pitch,
      float rate,
      int stream,
      float volume,
      UtteranceInfoCombo utteranceInfoCombo,
      EventId eventId) {
    Bundle bundle = new Bundle();

    if (params != null) {
      for (String key : params.keySet()) {
        bundle.putString(key, params.get(key));
      }
    }

    bundle.putInt(SpeechParam.PITCH, (int) (pitch * 100));
    bundle.putInt(SpeechParam.RATE, (int) (rate * 100));
    bundle.putInt(Engine.KEY_PARAM_STREAM, stream);
    bundle.putFloat(SpeechParam.VOLUME, volume);
    // When the language in use in not available, TTS will use en-us as the fallback locale.
    bundle.putString(SpeechParam.FALLBACK_LOCALE, "true");

    ensureQueueFlush();

    int queueMode =
        utteranceInfoCombo.flushGlobalTtsQueue() ? SPEECH_FLUSH_ALL : TextToSpeech.QUEUE_ADD;

    // Track latency from event received to feedback queued.
    if (eventId != null && utteranceId != null) {
      Performance.getInstance().onFeedbackQueued(eventId, utteranceId, utteranceInfoCombo);
    }

    if (locale == null) {
      locale = getUsedLocale();
    }

    return speakWithCacheOrTts(utteranceId, text, queueMode, locale, bundle);
  }

  private Locale getUsedLocale() {
    synchronized (ttsLock) {
      return cachedTtsLocale;
    }
  }

  private final PlaybackStateListener playbackStateListener =
      new PlaybackStateListener() {
        @Override
        public void onStateChanged(@Nullable String utteranceId, int state) {
          if ((state == SpeechCachePlayer.STATE_START)) {
            utteranceProgressCallback.onStart(utteranceId);
          } else if (state == SpeechCachePlayer.STATE_READY) {
            utteranceProgressCallback.onAudioCacheAvailable(utteranceId);
          } else if (state == SpeechCachePlayer.STATE_COMPLETED) {
            // Consume the speeches with queue_MODE_ADD.
            List<SpeakRequest> suspendQueueCopy = new ArrayList<>(suspendQueue);
            suspendQueue.clear();
            for (SpeakRequest speakRequest : suspendQueueCopy) {
              LogUtils.d(TAG, "consume suspend queue speakRequest= %s: ", speakRequest);
              mHandler.post(
                  () -> {
                    if (isReady()) {
                      attemptSetLanguage(speakRequest.locale);
                      tts.speak(
                          speakRequest.text,
                          speakRequest.queueMode,
                          speakRequest.bundle,
                          utteranceId);
                    }
                  });
            }
            mHandler.onUtteranceCompleted(utteranceId, /* success= */ true);
          } else if (state == SpeechCachePlayer.STATE_STOPPED) {
            mHandler.onUtteranceCompleted(utteranceId, /* success= */ false);
          }
        }
      };

  private final List<SpeakRequest> suspendQueue = Collections.synchronizedList(new ArrayList<>());

  /**
   * Speaks the text with cache if possible, otherwiase fallback to {@link
   * TextToSpeech#speak(CharSequence, int, Bundle, String)}} and drop the suspend queue if needed.
   * If the queue mode is {@link TextToSpeech#QUEUE_ADD} and the cache is speaking, the speech will
   * be added to the {@link #suspendQueue} and speaks until the cache is spoken completed.
   *
   * @param utteranceId the id to know status of the utterance when the callback is called.
   * @param text the text to speak
   * @param queueMode The queue mode to use.
   * @param locale the locale to use
   * @param bundle The bundle to use.
   * @return The result of speaking the text.
   */
  private int speakWithCacheOrTts(
      String utteranceId, CharSequence text, int queueMode, Locale locale, Bundle bundle) {
    if (speechCacheManager == null) {
      return tts.speak(text, queueMode, bundle, utteranceId);
    }

    if (speechCacheManager.isSpeaking()) {
      if (queueMode == TextToSpeech.QUEUE_ADD) {
        LogUtils.d(TAG, "speakWithCacheOrTts -- speaking cache, play %s later", utteranceId);
        SpeakRequest speakRequest = new SpeakRequest(locale, text, queueMode, bundle, utteranceId);
        suspendQueue.add(speakRequest);
        return TextToSpeech.SUCCESS;
      } else {
        LogUtils.d(
            TAG,
            "speaking cache, speak %s now and clean queue size %s: ",
            utteranceId,
            suspendQueue.size());
        notifyInterruptedForSuspendQueue();
        // speak next speech immediately
        if (speechCacheManager.speakWithSpeechCachePlayer(
            utteranceId, queueMode, text, bundle, playbackStateListener, locale)) {
          tts.speak("", SPEECH_FLUSH_ALL, null);
          return TextToSpeech.SUCCESS;
        }
        LogUtils.d(TAG, "tts.speak, utteranceId =" + utteranceId);
        return tts.speak(text, queueMode, bundle, utteranceId);
      }
    } else {
      if (speechCacheManager.speakWithSpeechCachePlayer(
          utteranceId, queueMode, text, bundle, playbackStateListener, locale)) {
        tts.speak("", SPEECH_FLUSH_ALL, null);
        return TextToSpeech.SUCCESS;
      }
      LogUtils.d(TAG, "tts.speak, utteranceId =" + utteranceId);
      return tts.speak(text, queueMode, bundle, utteranceId);
    }
  }

  private void notifyInterruptedForSuspendQueue() {
    List<SpeakRequest> suspendQueueCopy = new ArrayList<>(suspendQueue);
    suspendQueue.clear();
    LogUtils.d(TAG, "notifyInterruptedForSuspendQueue suspendQueueCopy = %s", suspendQueueCopy);
    suspendQueueCopy.forEach(
        speakRequest -> utteranceProgressCallback.onStop(speakRequest.utteranceId, true));
  }

  /**
   * Flushes the TextToSpeech queue for fast speech queueing, needed only on Android M.
   * REFERTO
   */
  private void ensureQueueFlush() {
    if (BuildVersionUtils.isM()) {
      tts.speak("", TextToSpeech.QUEUE_FLUSH, null, null);
    }
  }

  /**
   * Try to switch the TTS engine.
   *
   * @param engine The package name of the desired TTS engine
   */
  private void setTtsEngine(String engine, boolean resetFailures) {
    if (resetFailures) {
      ttsFailures = 0;
    }

    // Always try to stop the current engine before switching.
    TextToSpeechUtils.attemptTtsShutdown(tts);
    TextToSpeechUtils.attemptTtsShutdown(tempTts);

    if (tempTts == null || tempTts.getLanguage() == null) {
      // The TTS instance is not existing or not responding, the service has likely stopped, so
      // dispose of our handle and create another one below.
      LogUtils.i(TAG, "Bad TextToSpeech instance detected. Re-creating.");
    } else {
      // We use the fact that a getLanguage() call should never return null unless there is a
      // failure talking to the service. This is tested on tv, but not on other platforms yet.
      LogUtils.e(TAG, "Can't start TTS engine %s while still loading previous engine", engine);
      return;
    }

    LogUtils.logWithLimit(
        TAG, Log.INFO, ttsFailures, MAX_LOG_MESSAGES, "Switching to TTS engine: %s", engine);

    tempTtsEngine = engine;
    tempTts = new TextToSpeech(context, mTtsChangeListener, engine);
  }

  /**
   * Assumes the current engine has failed and attempts to start the next available engine.
   *
   * @param failedEngine The package name of the engine to switch from.
   */
  private void attemptTtsFailover(String failedEngine) {
    LogUtils.logWithLimit(
        TAG,
        Log.ERROR,
        ttsFailures,
        MAX_LOG_MESSAGES,
        "Attempting TTS failover from %s",
        failedEngine);

    ttsFailures++;

    // If there is only one installed engine, or if the current engine
    // hasn't failed enough times, just restart the current engine.
    if ((installedTtsEngines.size() <= 1) || (ttsFailures < MAX_TTS_FAILURES)) {
      setTtsEngine(failedEngine, false);
      return;
    }

    // Move the engine to the back of the list.
    if (failedEngine != null) {
      installedTtsEngines.remove(failedEngine);
      installedTtsEngines.addLast(failedEngine);
    }

    // Try to use the first available TTS engine.
    final String nextEngine = installedTtsEngines.getFirst();

    setTtsEngine(nextEngine, true);
  }

  /**
   * Handles TTS engine initialization.
   *
   * @param status The status returned by the TTS engine.
   */
  @SuppressWarnings("deprecation")
  private void handleTtsInitialized(int status) {
    if (tempTts == null) {
      LogUtils.e(TAG, "Attempted to initialize TTS more than once!");
      return;
    }

    final TextToSpeech tempTts = this.tempTts;
    final String tempTtsEngine = this.tempTtsEngine;

    this.tempTts = null;
    this.tempTtsEngine = null;

    synchronized (ttsLock) {
      cachedTtsLocale = null;
    }

    if (status != TextToSpeech.SUCCESS) {
      attemptTtsFailover(tempTtsEngine);
      return;
    }

    final boolean isSwitchingEngines = (tts != null);

    if (isSwitchingEngines) {
      TextToSpeechUtils.attemptTtsShutdown(tts);
    }

    tts = tempTts;
    tts.setOnUtteranceProgressListener(utteranceProgressCallback);

    if (tempTtsEngine == null) {
      ttsEngine = TextToSpeechCompatUtils.getCurrentEngine(tts);
    } else {
      ttsEngine = tempTtsEngine;
    }

    updateDefaultLocale();

    tts.setAudioAttributes(
        new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
            .setFlags(AudioAttributes.FLAG_LOW_LATENCY)
            .build());

    LogUtils.i(TAG, "Switched to TTS engine: %s", tempTtsEngine);

    for (FailoverTtsListener mListener : listeners) {
      mListener.onTtsInitialized(isSwitchingEngines, tempTtsEngine);
    }
  }

  /**
   * Adds a speech to the speech cache.
   *
   * @param text the text to be spoken
   * @param pitch the pitch of the utterance
   * @param rate the speech rate of the utterance
   * @param resultNotifier the callback to notify whether the speech cache is loaded or failed.
   * @param eventId the event id triggering this action, used for logging
   */
  void addSpeech(
      CharSequence text,
      LoadSpeechResultNotifier resultNotifier,
      float pitch,
      float rate,
      @Nullable EventId eventId) {
    if (!isReady()) {
      LogUtils.e(TAG, "addSpeech TTS is not ready: text=" + text);
      resultNotifier.onFinished(
          new SpeechInfo(text, /* utteranceId= */ null, /* locale= */ null, pitch, rate), false);
      return;
    }
    if (speechCacheManager == null) {
      return;
    }
    Locale locale = null;
    if (text instanceof Spannable spannable) {
      TtsSpan[] ttsSpans = spannable.getSpans(0, text.length(), TtsSpan.class);
      if (ttsSpans.length > 0) {
        resultNotifier.onFinished(null, false);
        return;
      }
      LocaleSpan[] spans = spannable.getSpans(0, text.length(), LocaleSpan.class);
      if (spans.length > 1) {
        return;
      }
      if (spans.length == 1) {
        locale = spans[0].getLocale();
      }
    }
    if (locale == null) {
      locale = getUsedLocale();
    }

    Bundle bundle = new Bundle();
    float effectiveRate = rate * defaultRate;
    float effectivePitch = pitch * defaultPitch;
    bundle.putInt(FailoverTextToSpeech.SpeechParam.PITCH, (int) (effectivePitch * 100));
    bundle.putInt(FailoverTextToSpeech.SpeechParam.RATE, (int) (effectiveRate * 100));
    bundle.putFloat(FailoverTextToSpeech.SpeechParam.VOLUME, 1.0f);
    speechCacheManager.addSpeechCache(this, text, locale, resultNotifier, bundle, ttsEngine);
  }

  /**
   * Method that's called by TTS whenever an utterance starts.
   *
   * @param utteranceId The utteranceId from the onUtteranceStarted callback - we expect this to
   *     consist of UTTERANCE_ID_PREFIX followed by the utterance index.
   * @param delay The time in milliseconds elapsed between {@link
   *     UtteranceProgressListener#onStart(String)} invoked and the callback dispatched by {@link
   *     SpeechHandler}.
   */
  private void handleUtteranceStarted(String utteranceId, long delay) {
    for (FailoverTtsListener mListener : listeners) {
      mListener.onUtteranceStarted(utteranceId, delay);
    }
  }

  /**
   * Method that's called by TTS to update the range of utterance being spoken.
   *
   * @param utteranceId The utteranceId from the onUtteranceStarted callback - we expect this to
   *     consist of UTTERANCE_ID_PREFIX followed by the utterance index.
   * @param start The start index of the range in the utterance text.
   * @param end The end index of the range in the utterance text.
   */
  private void handleUtteranceRangeStarted(String utteranceId, int start, int end) {
    for (FailoverTtsListener mListener : listeners) {
      mListener.onUtteranceRangeStarted(utteranceId, start, end);
    }
  }

  /**
   * Method that's called by TTS whenever an utterance is completed. Do common tasks and execute any
   * UtteranceCompleteActions associate with this utterance index (or an earlier index, in case one
   * was accidentally dropped).
   *
   * @param utteranceId The utteranceId from the onUtteranceCompleted callback - we expect this to
   *     consist of UTTERANCE_ID_PREFIX followed by the utterance index.
   * @param success {@code true} if the utterance was spoken successfully.
   */
  private void handleUtteranceCompleted(String utteranceId, boolean success) {
    if (success) {
      ttsFailures = 0;
    }
    allowDeviceSleep(utteranceId);
    for (FailoverTtsListener mListener : listeners) {
      mListener.onUtteranceCompleted(utteranceId, success);
    }
  }

  /**
   * Handles media state changes.
   *
   * @param action The current media state.
   */
  private void handleMediaStateChanged(String action) {
    if (Intent.ACTION_MEDIA_UNMOUNTED.equals(action)) {
      if (!TextUtils.equals(systemTtsEngine, ttsEngine)) {
        // Temporarily switch to the system TTS engine.
        LogUtils.v(TAG, "Saw media unmount");
        setTtsEngine(systemTtsEngine, true);
      }
    }

    if (Intent.ACTION_MEDIA_MOUNTED.equals(action)) {
      if (!TextUtils.equals(defaultTtsEngine, ttsEngine)) {
        // Try to switch back to the default engine.
        LogUtils.v(TAG, "Saw media mount");
        setTtsEngine(defaultTtsEngine, true);
      }
    }
  }

  public void updateDefaultEngine() {
    final ContentResolver resolver = context.getContentResolver();

    // Always refresh the list of available engines, since the user may have
    // installed a new TTS and then switched to it.
    installedTtsEngines.clear();
    systemTtsEngine =
        TextToSpeechUtils.reloadInstalledTtsEngines(
            context.getPackageManager(), installedTtsEngines);

    // This may be null if the user hasn't specified an engine.
    defaultTtsEngine = Secure.getString(resolver, Secure.TTS_DEFAULT_SYNTH);

    // Switch engines when the system default changes and it's not the current engine.
    if (ttsEngine == null || !ttsEngine.equals(defaultTtsEngine)) {
      if (installedTtsEngines.contains(defaultTtsEngine)) {
        // Can load the default engine.
        setTtsEngine(defaultTtsEngine, true);
      } else if (!installedTtsEngines.isEmpty()) {
        // We'll take whatever TTS we can get.
        setTtsEngine(installedTtsEngines.getFirst(), true);
      }
    }
  }

  /**
   * Loads the default pitch adjustment from {@link Secure#TTS_DEFAULT_PITCH}. This will take effect
   * during the next call to {@link #trySpeak}.
   */
  private void updateDefaultPitch() {
    defaultPitch = (Secure.getInt(resolver, Secure.TTS_DEFAULT_PITCH, 100) / 100.0f);
    for (FailoverTtsListener listener : listeners) {
      listener.onDefaultPitchChanged(defaultPitch);
    }
  }

  /**
   * Loads the default rate adjustment from {@link Secure#TTS_DEFAULT_RATE}. This will take effect
   * during the next call to {@link #trySpeak}.
   */
  private void updateDefaultRate() {
    defaultRate = (Secure.getInt(resolver, Secure.TTS_DEFAULT_RATE, 100) / 100.0f);
    for (FailoverTtsListener listener : listeners) {
      listener.onDefaultRateChanged(defaultRate);
    }
  }

  /** Preferred locale for fallback language. */
  private static final Locale PREFERRED_FALLBACK_LOCALE = Locale.US;

  /** The system's default locale. */
  private Locale mSystemLocale = Locale.getDefault();

  /**
   * The current engine's default locale. This will be {@code null} if the user never specified a
   * preference.
   */
  private @Nullable Locale mDefaultLocale = null;

  /**
   * The locale specified by the last utterance with {@link #speak(CharSequence, Locale, float,
   * float, HashMap, int, float, boolean, boolean)}.
   */
  private @Nullable Locale mLastUtteranceLocale = null;

  // TODO: Replace it with cachedTtsLocale.
  /**
   * Keep the recently in used locale by TTS. Querying current TTS' language is time-consuming, we
   * use this cache variable to save time.
   */
  private @Nullable Locale localeInUse = null;

  /**
   * Caches the locale previously set using {@link TextToSpeech#setLanguage(Locale)}. Since setting
   * the language on a `TextToSpeech` instance can be a time-consuming operation, this cache helps
   * to avoid redundant calls and improve performance.
   */
  private @Nullable Locale cachedTtsLocale;

  /**
   * Helper method that ensures the text-to-speech engine works even when the user is using the
   * Google TTS and has the system set to a non-embedded language.
   *
   * <p>This method should be called whenever the TTS engine is loaded, the system locale changes,
   * or the default TTS locale changes.
   */
  private @Nullable Locale ensureSupportedLocale() {
    if (needsFallbackLocale()) {
      return attemptSetFallbackLanguage();
    } else {
      // We might need to restore the system locale. Or, if we've ever
      // explicitly set the locale, we'll need to work around a bug where
      // there's no way to tell the TTS engine to use whatever it thinks
      // the default language should be.
      return attemptRestorePreferredLocale();
    }
  }

  /** Returns whether we need to attempt to use a fallback language. */
  private boolean needsFallbackLocale() {
    // If the user isn't using Google TTS, or if they set a preferred
    // locale, we do not need to check locale support.
    if (!Objects.equals(ttsEngine, PACKAGE_GOOGLE_TTS) || (mDefaultLocale != null)) {
      return false;
    }

    if (tts == null) {
      return false;
    }

    // Otherwise, the TTS engine will attempt to use the system locale which
    // may not be supported. If the locale is embedded or advertised as
    // available, we're fine.
    final Set<String> features = tts.getFeatures(mSystemLocale);
    return !(((features != null) && features.contains(Engine.KEY_FEATURE_EMBEDDED_SYNTHESIS))
        || !isNotAvailableStatus(tts.isLanguageAvailable(mSystemLocale)));
  }

  /** Attempts to obtain and set a fallback TTS locale. */
  private @Nullable Locale attemptSetFallbackLanguage() {
    final Locale fallbackLocale = getBestAvailableLocale();
    if (fallbackLocale == null) {
      LogUtils.e(TAG, "Failed to find fallback locale");
      return null;
    }
    LogUtils.v(TAG, "Attempt setting fallback TTS locale.");
    LogUtils.v(TAG, "attemptSetFallbackLanguage fallback tts locale.");
    return attemptSetLanguage(fallbackLocale);
  }

  /**
   * Attempts to set a TTS locale.
   *
   * @param locale TTS locale to set.
   * @return {@code true} if successfully set the TTS locale.
   */
  private @Nullable Locale attemptSetLanguage(@Nullable Locale locale) {
    if (locale == null) {
      LogUtils.w(TAG, "Cannot set null locale.");
      return null;
    }
    if (tts == null) {
      LogUtils.e(TAG, "mTts null when setting locale.");
      return null;
    }

    final int status = setTtsLocale(locale);
    if (isNotAvailableStatus(status)) {
      LogUtils.e(TAG, "Failed to set locale to %s", locale);
      return null;
    }

    LogUtils.v(TAG, "attemptSetLanguage- Set locale to %s", locale);
    return locale;
  }

  /**
   * Synthesizes the speech to file with the given locale. If the locale is not set, then it will be
   * {@link #cachedTtsLocale}, the current locale used in {@link TextToSpeech}.
   */
  @WorkerThread
  protected boolean synthesizeToFile(
      final CharSequence text,
      @Nullable Locale textLocale,
      final Bundle params,
      final File file,
      final String utteranceId) {
    if (!isReady()) {
      return false;
    }
    synchronized (ttsLock) {
      if (textLocale != null) {
        Locale actualLocale = attemptSetLanguage(textLocale);
        if (actualLocale == null) {
          return false;
        }
        mLastUtteranceLocale = actualLocale;
      }
      // Use getter to override it in testing.
      if (getEngineInstance().synthesizeToFile(text, params, file, utteranceId)
          == TextToSpeech.SUCCESS) {
        return true;
      }
      return false;
    }
  }

  private int setTtsLocale(Locale locale) {
    synchronized (ttsLock) {
      if (cachedTtsLocale != null && cachedTtsLocale.equals(locale)) {
        return LANG_AVAILABLE;
      }

      int status = tts.setLanguage(locale);
      LogUtils.d(TAG, "set ttsLocale: %s with status %s", locale, status);
      if (!isNotAvailableStatus(status)) {
        cachedTtsLocale = locale;
      }
      return status;
    }
  }

  /** Dumps the important variables for debugging. */
  public void dump(Logger dumpLogger) {
    dumpLogger.log(" cachedTtsLocale=%s", getUsedLocale());
    dumpLogger.log(" mDefaultLocale=%s", toLanguageTag(mDefaultLocale));
    dumpLogger.log(" mSystemLocale=%s", toLanguageTag(mSystemLocale));
    dumpLogger.log(" mLastUtteranceLocale=%s", toLanguageTag(mLastUtteranceLocale));
    dumpLogger.log(" getVoice=%s", getTtsVoice());
    if (speechCacheManager != null) {
      speechCacheManager.dumpCachedSpeechInfo(dumpLogger);
    }
  }

  private String toLanguageTag(@Nullable Locale locale) {
    return locale != null ? locale.toLanguageTag() : "null";
  }

  private @Nullable Voice getTtsVoice() {
    return tts != null ? tts.getVoice() : null;
  }

  /**
   * Attempts to obtain a supported TTS locale with preference given to {@link
   * #PREFERRED_FALLBACK_LOCALE}. The resulting locale may not be optimal for the user, but it will
   * likely be enough to understand what's on the screen.
   */
  private @Nullable Locale getBestAvailableLocale() {
    if (tts == null) {
      return null;
    }

    // Always attempt to use the preferred locale first.
    if (tts.isLanguageAvailable(PREFERRED_FALLBACK_LOCALE) >= 0) {
      return PREFERRED_FALLBACK_LOCALE;
    }

    // Since there's no way to query available languages from an engine,
    // we'll need to check every locale supported by the device.
    Locale bestLocale = null;
    int bestScore = -1;

    final Locale[] locales = Locale.getAvailableLocales();
    for (Locale locale : locales) {
      final int status = tts.isLanguageAvailable(locale);
      if (isNotAvailableStatus(status)) {
        continue;
      }

      final int score = compareLocales(mSystemLocale, locale);
      if (score > bestScore) {
        bestLocale = locale;
        bestScore = score;
      }
    }

    return bestLocale;
  }

  /**
   * Attempts to restore the user's preferred TTS locale, if set. Otherwise attempts to restore the
   * system locale.
   */
  private @Nullable Locale attemptRestorePreferredLocale() {
    if (tts == null) {
      return null;
    }
    mLastUtteranceLocale = null;
    final Locale preferredLocale = (mDefaultLocale != null ? mDefaultLocale : mSystemLocale);
    try {
      final int status = setTtsLocale(preferredLocale);
      if (!isNotAvailableStatus(status)) {
        LogUtils.i(TAG, "Restored TTS locale to %s", preferredLocale);
        return preferredLocale;
      }
    } catch (Exception e) {
      LogUtils.e(TAG, "Failed to setLanguage(): %s", e.toString());
    }

    LogUtils.e(TAG, "Failed to restore TTS locale to %s", preferredLocale);
    return null;
  }

  /** Handles updating the default locale. */
  private void updateDefaultLocale() {
    final String defaultLocale = TextToSpeechUtils.getDefaultLocaleForEngine(resolver, ttsEngine);
    mDefaultLocale = !TextUtils.isEmpty(defaultLocale) ? forLanguageTag(defaultLocale) : null;

    // The default locale changed, which may mean we can restore the user's
    // preferred locale.
    localeInUse = ensureSupportedLocale();
  }

  /** Handles updating the system locale. */
  private void onConfigurationChanged(Configuration newConfig) {
    final Locale newLocale = newConfig.locale;
    if (newLocale.equals(mSystemLocale)) {
      return;
    }

    mSystemLocale = newLocale;

    // The system locale changed, which may mean we need to override the
    // current TTS locale.
    localeInUse = ensureSupportedLocale();
  }

  /** Registers the configuration change callback. */
  private void registerGoogleTtsFixCallbacks() {
    final Uri defaultLocaleUri = Secure.getUriFor(SecureCompatUtils.TTS_DEFAULT_LOCALE);
    resolver.registerContentObserver(defaultLocaleUri, false, mLocaleObserver);
    context.registerComponentCallbacks(mComponentCallbacks);
  }

  /** Unregisters the configuration change callback. */
  private void unregisterGoogleTtsFixCallbacks() {
    resolver.unregisterContentObserver(mLocaleObserver);
    context.unregisterComponentCallbacks(mComponentCallbacks);
  }

  /**
   * Compares a locale against a primary locale. Returns higher values for closer matches. A return
   * value of 3 indicates that the locale is an exact match for the primary locale's language,
   * country, and variant.
   *
   * @param primary The primary locale for comparison.
   * @param other The other locale to compare against the primary locale.
   * @return A value indicating how well the other locale matches the primary locale. Higher is
   *     better.
   */
  private static int compareLocales(Locale primary, Locale other) {
    final String lang = primary.getLanguage();
    if ((lang == null) || !lang.equals(other.getLanguage())) {
      return 0;
    }

    final String country = primary.getCountry();
    if ((country == null) || !country.equals(other.getCountry())) {
      return 1;
    }

    final String variant = primary.getVariant();
    if ((variant == null) || !variant.equals(other.getVariant())) {
      return 2;
    }

    return 3;
  }

  /**
   * Returns {@code true} if the specified status indicates that the language is available.
   *
   * @param status A language availability code, as returned from {@link
   *     TextToSpeech#isLanguageAvailable}.
   * @return {@code true} if the status indicates that the language is available.
   */
  private static boolean isNotAvailableStatus(int status) {
    return (status != LANG_AVAILABLE)
        && (status != LANG_COUNTRY_AVAILABLE)
        && (status != LANG_COUNTRY_VAR_AVAILABLE);
  }

  private final SpeechHandler mHandler = new SpeechHandler(this);

  /** Handles changes to the default TTS engine. */
  private final ContentObserver mSynthObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultEngine();
        }
      };

  private final ContentObserver mPitchObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultPitch();
        }
      };

  private final ContentObserver mRateObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultRate();
        }
      };

  /** Callbacks used to observe changes to the TTS locale. */
  private final ContentObserver mLocaleObserver =
      new ContentObserver(mHandler) {
        @Override
        public void onChange(boolean selfChange) {
          updateDefaultLocale();
        }
      };

  /**
   * A callback for speech progress of {@link TextToSpeech} and {@link SpeechCacheManager}.
   *
   * <p><strong>Note: </strong> By default, the callback is invoked in TTS thread and we hand over
   * the message to handler thread for processing. In some special cases when we want to handle the
   * callback in TTS thread, call {@link #setHandleTtsCallbackInHandlerThread(boolean)}.
   */
  private class UtteranceProgressCallback extends UtteranceProgressListener {
    private @Nullable String lastUpdatedUtteranceId = null;

    private void updatePerformanceMetrics(String utteranceId, boolean localCache) {
      // Update performance for this utterance, only if we did not recently update
      // for the same utterance.
      if (utteranceId != null && !utteranceId.equals(lastUpdatedUtteranceId)) {
        Performance.getInstance().onFeedbackOutput(utteranceId, localCache);
      }
      lastUpdatedUtteranceId = utteranceId;
    }

    @Override
    public void onStart(String utteranceId) {
      if (utteranceId.startsWith(CACHE_UTTERANCE_ID_PREFIX)) {
        return;
      }
      Performance.getInstance().onFeedbackReady(utteranceId);
      if (shouldHandleTtsCallbackInHandlerThread) {
        mHandler.onUtteranceStarted(utteranceId);
      } else {
        FailoverTextToSpeech.this.handleUtteranceStarted(utteranceId, /* delay= */ 0);
      }
    }

    @Override
    public void onAudioAvailable(String utteranceId, byte[] audio) {
      // onAudioAvailable() is usually called many times per utterance,
      // once for each audio chunk.
      updatePerformanceMetrics(utteranceId, /* localCache= */ false);
    }

    void onAudioCacheAvailable(String utteranceId) {
      // onAudioAvailable() is usually called many times per utterance,
      // once for each audio chunk.
      updatePerformanceMetrics(utteranceId, /* localCache= */ true);
    }

    @Override
    public void onRangeStart(String utteranceId, int start, int end, int frame) {
      Performance.getInstance().onFeedbackRangeStarted(utteranceId);
      if (shouldHandleTtsCallbackInHandlerThread) {
        mHandler.onUtteranceRangeStarted(utteranceId, start, end);
      } else {
        FailoverTextToSpeech.this.handleUtteranceRangeStarted(utteranceId, start, end);
      }
    }

    @Override
    public void onStop(String utteranceId, boolean interrupted) {
      if (speechCacheManager != null && speechCacheManager.handleOnStop(utteranceId, interrupted)) {
        return;
      }
      handleUtteranceCompleted(utteranceId, /* success= */ !interrupted);
    }

    @Override
    public void onError(String utteranceId) {
      if (speechCacheManager != null && speechCacheManager.handleOnError(utteranceId)) {
        return;
      }
      handleUtteranceCompleted(utteranceId, /* success= */ false);
    }

    @Override
    public void onDone(String utteranceId) {
      if (speechCacheManager != null && speechCacheManager.handleOnDone(utteranceId)) {
        return;
      }
      handleUtteranceCompleted(utteranceId, /* success= */ true);
    }
  }

  private final UtteranceProgressCallback utteranceProgressCallback =
      new UtteranceProgressCallback();

  /**
   * When changing TTS engines, switches the active TTS engine when the new engine is initialized.
   */
  private final OnInitListener mTtsChangeListener =
      new OnInitListener() {
        @Override
        public void onInit(int status) {
          mHandler.onTtsInitialized(status);
        }
      };

  /** Callbacks used to observe configuration changes. */
  private final ComponentCallbacks mComponentCallbacks =
      new ComponentCallbacks() {
        @Override
        public void onLowMemory() {
          // Do nothing.
        }

        @Override
        public void onConfigurationChanged(Configuration newConfig) {
          FailoverTextToSpeech.this.onConfigurationChanged(newConfig);
        }
      };

  /** {@link BroadcastReceiver} for detecting media mount and unmount. */
  private class MediaMountStateMonitor extends SameThreadBroadcastReceiver {
    private final IntentFilter mMediaIntentFilter;

    public MediaMountStateMonitor() {
      mMediaIntentFilter = new IntentFilter();
      mMediaIntentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
      mMediaIntentFilter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
      mMediaIntentFilter.addDataScheme("file");
    }

    public IntentFilter getFilter() {
      return mMediaIntentFilter;
    }

    @Override
    public void onReceiveIntent(Intent intent) {
      final String action = intent.getAction();

      mHandler.onMediaStateChanged(action);
    }
  }

  /** Handler used to return to the main thread from the TTS thread. */
  private static class SpeechHandler extends WeakReferenceHandler<FailoverTextToSpeech> {
    /** Hand-off engine initialized. */
    private static final int MSG_INITIALIZED = 1;

    /** Hand-off utterance started. */
    private static final int MSG_UTTERANCE_STARTED = 2;

    /** Hand-off utterance completed. */
    private static final int MSG_UTTERANCE_COMPLETED = 3;

    /** Hand-off media state changes. */
    private static final int MSG_MEDIA_STATE_CHANGED = 4;

    /** Hand-off a range of utterance started. */
    private static final int MSG_UTTERANCE_RANGE_STARTED = 5;

    public SpeechHandler(FailoverTextToSpeech parent) {
      super(parent);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void handleMessage(Message msg, FailoverTextToSpeech parent) {
      switch (msg.what) {
        case MSG_INITIALIZED -> parent.handleTtsInitialized(msg.arg1);
        case MSG_UTTERANCE_STARTED -> {
          long talkbackDelay = SystemClock.uptimeMillis() - msg.getWhen();
          parent.handleUtteranceStarted((String) msg.obj, talkbackDelay);
        }
        case MSG_UTTERANCE_COMPLETED -> {
          Pair<String, Boolean> data = (Pair<String, Boolean>) msg.obj;
          parent.handleUtteranceCompleted(
              /* utteranceId= */ data.first, /* success= */ data.second);
        }
        case MSG_MEDIA_STATE_CHANGED -> parent.handleMediaStateChanged((String) msg.obj);
        case MSG_UTTERANCE_RANGE_STARTED ->
            parent.handleUtteranceRangeStarted((String) msg.obj, msg.arg1, msg.arg2);
        default -> {}
      }
    }

    public void onTtsInitialized(int status) {
      obtainMessage(MSG_INITIALIZED, status, 0).sendToTarget();
    }

    public void onUtteranceStarted(String utteranceId) {
      obtainMessage(MSG_UTTERANCE_STARTED, utteranceId).sendToTarget();
    }

    public void onUtteranceRangeStarted(String utteranceId, int start, int end) {
      obtainMessage(MSG_UTTERANCE_RANGE_STARTED, start, end, utteranceId).sendToTarget();
    }

    public void onUtteranceCompleted(String utteranceId, boolean success) {
      obtainMessage(MSG_UTTERANCE_COMPLETED, Pair.create(utteranceId, success)).sendToTarget();
    }

    public void onMediaStateChanged(String action) {
      obtainMessage(MSG_MEDIA_STATE_CHANGED, action).sendToTarget();
    }
  }

  /** Listener for TTS events. */
  public interface FailoverTtsListener {
    /** Called after the class has initialized with a tts engine. */
    default void onTtsInitialized(boolean wasSwitchingEngines, String enginePackageName) {}

    /** Called before an utterance is sent to the TTS engine. */
    default void onBeforeUtteranceRequested(
        String utteranceId, UtteranceInfoCombo utteranceInfoCombo) {}

    /*
     * Called before an utterance starts speaking.
     */
    default void onUtteranceStarted(String utteranceId) {}

    /**
     * Called before an utterance starts speaking.
     *
     * @param delay The time (in milliseconds) elapsed between {@link
     *     UtteranceProgressListener#onStart(String)} invoked and the callback dispatched by {@link
     *     SpeechHandler}.
     */
    default void onUtteranceStarted(String utteranceId, long delay) {
      onUtteranceStarted(utteranceId);
    }

    /*
     * Called before speaking the range of an utterance.
     */
    void onUtteranceRangeStarted(String utteranceId, int start, int end);

    /*
     * Called after an utterance has completed speaking.
     */
    void onUtteranceCompleted(String utteranceId, boolean success);

    /** Called when the default pitch changes. */
    default void onDefaultPitchChanged(float pitch) {}

    /** Called when the default rate changes. */
    default void onDefaultRateChanged(float rate) {}
  }

  /** Details of the utterance that is sent to the TTS engine. */
  @AutoValue
  public abstract static class UtteranceInfoCombo {
    public abstract CharSequence text();

    public abstract @Nullable Locale locale();

    public abstract boolean isLocaleAttached();

    public abstract boolean isSeparatorInUtterance();

    public abstract boolean isAggressiveChunking();

    public abstract boolean flushGlobalTtsQueue();

    @ChangeLocaleAction
    public abstract int changeLocaleAction();

    public static Builder builder(
        CharSequence text, @Nullable Locale locale, boolean isLocaleAttached) {
      return builder(text, locale, isLocaleAttached, Performance.CHANGE_LOCALE_NONE);
    }

    public static Builder builder(
        CharSequence text,
        @Nullable Locale locale,
        boolean isLocaleAttached,
        @ChangeLocaleAction int changeLocaleAction) {
      return new AutoValue_FailoverTextToSpeech_UtteranceInfoCombo.Builder()
          .setText(text)
          .setLocale(locale)
          .setIsLocaleAttached(isLocaleAttached)
          .setIsSeparatorInUtterance(false)
          .setIsAggressiveChunking(false)
          .setFlushGlobalTtsQueue(true)
          .setChangeLocaleAction(changeLocaleAction);
    }

    /** Builders of the utterance info combo. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setText(CharSequence text);

      public abstract Builder setLocale(@Nullable Locale locale);

      public abstract Builder setIsLocaleAttached(boolean value);

      public abstract Builder setIsSeparatorInUtterance(boolean value);

      public abstract Builder setIsAggressiveChunking(boolean value);

      public abstract Builder setFlushGlobalTtsQueue(boolean value);

      public abstract Builder setChangeLocaleAction(@ChangeLocaleAction int changeLocaleAction);

      public abstract UtteranceInfoCombo build();
    }
  }

  /** Details of the utterance that is sent to the TTS engine. */
  public record SpeakRequest(
      Locale locale, CharSequence text, int queueMode, Bundle bundle, String utteranceId) {}

}
