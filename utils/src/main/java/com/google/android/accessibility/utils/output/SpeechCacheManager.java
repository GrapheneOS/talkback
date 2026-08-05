/*
 * Copyright (C) 2025 Google Inc.
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

import static android.speech.tts.TextToSpeech.QUEUE_ADD;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.output.SpeechCachePlayer.PlaybackStateListener;
import com.google.android.accessibility.utils.output.SpeechCachePlayer.SpeechInfo;
import com.google.android.libraries.accessibility.utils.concurrent.HandlerExecutor;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.io.File;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the caching of synthesized speech to improve performance by avoiding repeated synthesis
 * of the same text.
 *
 * <p>This class orchestrates the process of synthesizing text to a temporary WAV file, loading the
 * audio data into an in-memory cache via {@link SpeechCachePlayer}, and then playing it back on
 * demand. All file operations and cache management are performed on a dedicated worker thread to
 * avoid blocking the main thread.
 */
public class SpeechCacheManager {
  public static final String CACHE_UTTERANCE_ID_PREFIX = "cache_";

  private static final String TAG = "SpeechCacheManager";
  private static final String DIRECTORY_CACHE = "SpeechCache";
  private static final long TIMEOUT_WAIT_FOR_SYNTHESIZE_RESULT_MS = 10000;

  private final Map<String, LoadSpeechFileRequest> utteranceIdToRequest = new ConcurrentHashMap<>();
  private final Map<String, Runnable> utteranceIdToTimeoutAction = new ConcurrentHashMap<>();
  private final File speechFilesRootDirectory;

  private final Handler workerHandler;
  private final SpeechCachePlayer speechCachePlayer;
  private int synthesisCacheIndex = 0;

  public SpeechCacheManager(Context context, Looper workerlooper) {
    workerHandler = new Handler(workerlooper);
    speechCachePlayer = new AudioTrackSpeechCachePlayer(new HandlerExecutor(workerHandler));
    speechFilesRootDirectory = new File(context.getFilesDir().getPath(), DIRECTORY_CACHE);
    if (speechFilesRootDirectory.exists()) {
      // In case the file is not deleted accidentally,always delete the files.
      workerHandler.post(() -> deleteAllSubFiles(speechFilesRootDirectory));

    } else {
      if (!speechFilesRootDirectory.mkdir()) {
        LogUtils.e(TAG, "create speechFilesRootDirectory failed");
      }
    }
  }

  private void deleteAllSubFiles(File directory) {
    workerHandler.post(
        () -> {
          File[] dirs = directory.listFiles();
          if (dirs != null) {
            for (File file : dirs) {
              deleteDirectory(file);
            }
          }
        });
  }

  private void deleteDirectory(File dir) {
    if (dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          deleteDirectory(file);
        }
      }
    }
    var unused = dir.delete();
  }

  /**
   * A callback invoked by the TTS engine when speech synthesis to a file is completed.
   *
   * @param utteranceId The utterance ID associated with the synthesis request.
   * @return {@code true} if the utterance ID was handled by this cache manager, {@code false}
   *     otherwise.
   */
  public boolean handleOnDone(String utteranceId) {
    if (!utteranceId.startsWith(CACHE_UTTERANCE_ID_PREFIX)) {
      return false;
    }

    LoadSpeechFileRequest loadSpeechFileRequest = utteranceIdToRequest.remove(utteranceId);
    if (loadSpeechFileRequest == null) {
      LogUtils.w(TAG, "cache is removed due to timeout:%s", utteranceId);
      return true;
    }
    LogUtils.d(TAG, "onDone -- synthesize speech completed: %s", utteranceId);
    Runnable timeoutAction = utteranceIdToTimeoutAction.remove(utteranceId);
    if (timeoutAction != null) {
      workerHandler.removeCallbacks(timeoutAction);
    }
    Runnable loadSpeechFile =
        () -> {
          SpeechInfo speechInfo = loadSpeechFileRequest.speechInfo();
          boolean success = speechCachePlayer.loadSpeechFileIntoMemory(speechInfo);
          loadSpeechFileRequest.notifyStateChanged(success);
          speechInfo.deleteSpeechFile(null);
          LogUtils.d(TAG, "load speech file %s- speechInfo=%s", success, speechInfo);
        };
    workerHandler.post(loadSpeechFile);
    return true;
  }

  /**
   * Initiates the process of synthesizing and caching speech. It doesn't accept the speech having
   * {@link android.text.style.TtsSpan}.
   *
   * <p>If the speech is already cached, the status notifier is called immediately. Otherwise, it
   * requests the TTS engine to synthesize the text to a file.
   *
   * @param tts The TTS engine to use for synthesis.
   * @param text The text to be synthesized.
   * @param spokenLocale The locale of the speech.
   * @param statusNotifier A callback to notify whether the caching was successful. It won't be
   *     invoked if the {@code text} has TtsSpan.
   * @param engineName The name of the TTS engine, used for organizing cache files.
   */
  public void addSpeechCache(
      FailoverTextToSpeech tts,
      CharSequence text,
      Locale spokenLocale,
      LoadSpeechResultNotifier statusNotifier,
      Bundle speechParameters,
      String engineName) {
    if (speechCachePlayer == null) {
      return;
    }
    String cacheUtteranceId = generateUtteranceId();
    final SpeechInfo simpleSpeechInfo =
        new SpeechInfo(cacheUtteranceId, text, spokenLocale, speechParameters);
    if (simpleSpeechInfo.hasTtsSpan()) {
      return;
    }
    if (speechCachePlayer.hasCache(simpleSpeechInfo)) {
      statusNotifier.onFinished(simpleSpeechInfo, true);
      return;
    }

    File speechFile = createSpeechFile(cacheUtteranceId, simpleSpeechInfo, engineName);
    if (speechFile == null) {
      LogUtils.w(TAG, "create speechFile failed");
      workerHandler.post(
          () -> {
            statusNotifier.onFinished(simpleSpeechInfo, false);
          });
      return;
    }

    final SpeechInfo speechInfo =
        new SpeechInfo(cacheUtteranceId, text, spokenLocale, speechParameters, speechFile);
    workerHandler.post(
        () -> {
          if (tts.synthesizeToFile(
              text, spokenLocale, speechParameters, speechFile, cacheUtteranceId)) {
            utteranceIdToRequest.put(
                cacheUtteranceId, new LoadSpeechFileRequest(speechInfo, statusNotifier));
            LogUtils.d(
                TAG,
                "synthesize %s success, waiting for callback: speechInfo= %s",
                cacheUtteranceId,
                speechInfo);
            Runnable synthesizeTimeoutAction =
                createTimeoutAction(statusNotifier, cacheUtteranceId);
            utteranceIdToTimeoutAction.put(cacheUtteranceId, synthesizeTimeoutAction);
            workerHandler.postDelayed(
                synthesizeTimeoutAction, TIMEOUT_WAIT_FOR_SYNTHESIZE_RESULT_MS);
          } else {
            statusNotifier.onFinished(speechInfo, false);
          }
        });
  }

  @Nullable
  private File createSpeechFile(String utteranceId, SpeechInfo speechInfo, String engineName) {
    File engineDirectory = getEngineDirectory(engineName);
    if (engineDirectory != null) {
      return new File(engineDirectory, utteranceId + "_" + speechInfo.text + ".wav");
    }
    return null;
  }

  @Nullable
  private File getEngineDirectory(String engineName) {
    File engineDirectory = new File(speechFilesRootDirectory, engineName);
    if (!engineDirectory.exists()) {
      if (!engineDirectory.mkdirs()) {
        LogUtils.w(TAG, "Failed to create engine directory: " + engineName);
        return null;
      }
    } else if (!engineDirectory.isDirectory()) {
      LogUtils.w(TAG, "Engine path exists but is not a directory: " + engineName);
      return null;
    }
    return engineDirectory;
  }

  @NonNull
  private Runnable createTimeoutAction(
      LoadSpeechResultNotifier statusNotifier, String cacheUtteranceId) {
    return () -> {
      utteranceIdToTimeoutAction.remove(cacheUtteranceId);
      LoadSpeechFileRequest loadSpeechRequest = utteranceIdToRequest.remove(cacheUtteranceId);
      if (loadSpeechRequest == null) {
        LogUtils.d(TAG, "loadSpeechRequest is unavailable:" + cacheUtteranceId);
        return;
      }
      statusNotifier.onFinished(loadSpeechRequest.speechInfo(), false);
      LogUtils.w(TAG, "synthesize timeout:" + cacheUtteranceId);
      loadSpeechRequest.speechInfo().deleteSpeechFile(null);
    };
  }

  private String generateUtteranceId() {
    return CACHE_UTTERANCE_ID_PREFIX + synthesisCacheIndex++;
  }

  /**
   * Checks if a specific piece of speech is already in the in-memory cache.
   *
   * @param utteranceId The utterance ID for the speech.
   * @param text The text of the speech.
   * @param locale The locale of the speech.
   * @param queueMode The queue mode of the speech request. Caching is ignored for {@link
   *     android.speech.tts.TextToSpeech#QUEUE_ADD}.
   * @return {@code true} if the speech is cached, {@code false} otherwise.
   */
  public boolean hasCache(
      String utteranceId,
      CharSequence text,
      Locale locale,
      int queueMode,
      float pitch,
      float rate) {
    if (queueMode == QUEUE_ADD) {
      return false;
    }
    SpeechInfo speechInfo = new SpeechInfo(text, utteranceId, locale, pitch, rate);
    return speechCachePlayer.hasCache(speechInfo);
  }

  /**
   * Plays speech from the cache if it is available.
   *
   * <p>This method will stop any currently playing speech before starting the new one.
   *
   * @return {@code true} if the speech was found in the cache and playback was initiated, {@code
   *     false} otherwise.
   */
  public boolean speakWithSpeechCachePlayer(
      String utteranceId,
      int queueMode,
      CharSequence text,
      Bundle parameters,
      PlaybackStateListener callback,
      Locale locale) {
    if (queueMode == QUEUE_ADD) {
      return false;
    }

    SpeechInfo speechInfo = new SpeechInfo(utteranceId, text, locale, parameters);
    speechCachePlayer.stopSpeech();
    LogUtils.d(TAG, "speakWithSpeechCachePlayer:speechInfo=%s", speechInfo);
    if (speechInfo.hasTtsSpan() || !speechCachePlayer.hasCache(speechInfo)) {
      return false;
    }
    return speechCachePlayer.playSpeech(speechInfo, callback);
  }

  /** Releases all resources, including the worker thread and the speech player. */
  public void shutDown() {
    speechCachePlayer.release();
    workerHandler.getLooper().quitSafely();
  }

  /**
   * Dumps debugging information about the cached speech to the provided logger.
   *
   * @param logger The logger to which the debug info will be written.
   */
  public void dumpCachedSpeechInfo(Logger logger) {
    speechCachePlayer.dumpCachedSpeechInfo(logger);
  }

  /**
   * Removes a specific speech item from the cache.
   *
   * @param speechInfo The speech item to remove.
   */
  public void removeSpeech(SpeechInfo speechInfo) {
    if (speechInfo.speechFile != null && speechInfo.speechFile.exists()) {
      var unused = speechInfo.speechFile.delete();
    }
    speechCachePlayer.removeCache(speechInfo);
  }

  /** Clears the entire in-memory speech cache. */
  public void cleanCache() {
    speechCachePlayer.cleanCaches();
  }

  /**
   * Checks if the speech cache player is currently playing speech.
   *
   * @return {@code true} if the speech cache player is playing speech, {@code false} otherwise.
   */
  public boolean isSpeaking() {
    return speechCachePlayer.isPlaying();
  }

  /**
   * A callback invoked by the TTS engine when a synthesis error occurs.
   *
   * @param utteranceId The utterance ID of the failed synthesis.
   * @return {@code true} if the utterance ID was handled by this manager, {@code false} otherwise.
   */
  public boolean handleOnError(String utteranceId) {
    if (!utteranceId.startsWith(CACHE_UTTERANCE_ID_PREFIX)) {
      return false;
    }
    LogUtils.w(TAG, "onError :" + utteranceId);
    notifyFailureIfNeeded(utteranceId);
    return true;
  }

  /**
   * A callback invoked by the TTS engine when a synthesis request is stopped.
   *
   * @param utteranceId the utterance ID of the stopped synthesis
   * @param interrupted whether the synthesis was interrupted when processing the request
   * @return {@code true} if the utterance ID was handled by this manager, {@code false} otherwise.
   */
  public boolean handleOnStop(String utteranceId, boolean interrupted) {
    if (!utteranceId.startsWith(CACHE_UTTERANCE_ID_PREFIX)) {
      return false;
    }
    LogUtils.d(TAG, "handleOnStop -- utteranceId= %s , interrupted=%s :", utteranceId, interrupted);
    notifyFailureIfNeeded(utteranceId);
    return true;
  }

  private void notifyFailureIfNeeded(String utteranceId) {
    LogUtils.e(TAG, "notifyFailureIfNeeded:" + utteranceId);
    Runnable timeoutTask = utteranceIdToTimeoutAction.remove(utteranceId);
    if (timeoutTask == null) {
      return;
    }
    workerHandler.removeCallbacks(timeoutTask);
    final LoadSpeechFileRequest loadSpeechFileRequest = utteranceIdToRequest.remove(utteranceId);
    if (loadSpeechFileRequest == null) {
      LogUtils.w(
          TAG, "notifyFailureIfNeeded -- synthesis result %s is already notified", utteranceId);
    } else {
      workerHandler.post(() -> loadSpeechFileRequest.notifyStateChanged(false));
    }
  }

  @VisibleForTesting
  Looper getLooper() {
    return workerHandler.getLooper();
  }

  public long getCacheSizeMb() {
    return speechCachePlayer.getCacheSizeMb();
  }

  /** Callback to notify the result of loading a speech file. */
  public interface LoadSpeechResultNotifier {

    /**
     * Called when the loading of a speech file is finished.
     *
     * @param speechInfo The speech information that was loaded.
     * @param success Whether the loading was successful.
     */
    void onFinished(SpeechInfo speechInfo, Boolean success);
  }

  /**
   * Represents a request to load a speech file into memory.
   *
   * <p>This record holds the {@link SpeechInfo} containing details about the speech to be loaded
   * and a {@link LoadSpeechResultNotifier} to call back with the result of the load operation.
   */
  public record LoadSpeechFileRequest(
      SpeechInfo speechInfo, LoadSpeechResultNotifier loadSpeechResultNotifier) {

    /**
     * Notifies the listener about the result of the load operation.
     *
     * @param success True if the file was loaded successfully, false otherwise.
     */
    public void notifyStateChanged(boolean success) {
      loadSpeechResultNotifier.onFinished(speechInfo, success);
    }
  }
}
