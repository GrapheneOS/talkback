/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.accessibility.talkback.utils;

import static android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS;
import static android.speech.SpeechRecognizer.ERROR_NO_MATCH;
import static android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY;
import static android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT;
import static androidx.core.content.ContextCompat.RECEIVER_EXPORTED;
import static com.google.android.accessibility.talkback.permission.PermissionRequestActivity.ACTION_DONE;
import static com.google.android.accessibility.talkback.permission.PermissionRequestActivity.GRANT_RESULTS;
import static com.google.android.accessibility.talkback.permission.PermissionRequestActivity.PERMISSIONS;

import android.Manifest;
import android.Manifest.permission;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.talkback.permission.PermissionUtils;
import com.google.android.accessibility.utils.DelayHandler;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Starts and ends the voice recognition for Talkback. For more information on implementation check
 * REFERTO
 */
public class SpeechRecognizerPerformer {

  private static final String TAG = "SpeechRecognizerPerformer";
  // When set to true, TalkBack will drop the partial results if the SpeechRecognizer had callback
  // with onResults.
  private static final boolean PREFER_FINAL_RESULT = false;

  /** Wait up to a second between command words, before executing command. */
  private static final int PARTIAL_SPEECH_COMMAND_PROCESS_DELAY_MS = 1000;

  /** Wait less time when the speech is known to be final. */
  private static final int FINAL_SPEECH_COMMAND_PROCESS_DELAY_MS = 250;

  private final int maxRecognitionDelayMs;
  private int partialSpeechFeedbackDelayMs = PARTIAL_SPEECH_COMMAND_PROCESS_DELAY_MS;

  private boolean recognizerProducesFinalResults = false;
  private final Context context;
  private SpeechRecognitionDelayedSender speechRecognitionDelayedSender;
  private boolean hasMicPermission;

  /** Wait between/after command words, before executing command. */
  private final Handler executeCommandDelayHandler = new Handler();

  private final DelayHandler<Object> stopListeningDelayHandler =
      new DelayHandler<Object>() {
        @Override
        public void handle(Object arg) {
          timeOut();
        }
      };
  public @Nullable SpeechRecognizer speechRecognizer;
  private boolean isListening = false;
  private boolean sessionActive = false;
  private final boolean partialResultFastFeedback;
  private String cachedResult = "";

  private @Nullable Intent recognizerIntent;

  /** Interface for communicating with SpeechRecognizerRequester user. */
  public interface SpeechRecognizerRequester {
    int EVENT_UNKNOWN = 0;
    int EVENT_PLATFORM_NOT_SUPPORT = 1;
    int EVENT_OPERATION_FORBIDDEN_DURING_SUW = 2;
    int EVENT_DIALOG_CONFIRM = 3;
    int EVENT_MIC_PERMISSION_REQUESTED = 4;
    int EVENT_MIC_PERMISSION_NOT_GRANTED = 5;
    int EVENT_START_LISTENING = 6;
    int EVENT_STOP_LISTENING = 7;
    int EVENT_FEATURE_UNAVAILABLE = 8;

    /**
     * SpeechRecognizer feedback events. User can decide what message to be fed-back accordingly.
     */
    @IntDef({
      EVENT_UNKNOWN,
      EVENT_PLATFORM_NOT_SUPPORT,
      EVENT_OPERATION_FORBIDDEN_DURING_SUW,
      EVENT_DIALOG_CONFIRM,
      EVENT_MIC_PERMISSION_REQUESTED,
      EVENT_MIC_PERMISSION_NOT_GRANTED,
      EVENT_START_LISTENING,
      EVENT_STOP_LISTENING,
      EVENT_FEATURE_UNAVAILABLE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FeedbackEvent {}

    /**
     * Notify the user that the speech (partial) result is received. For partial result, user can
     * early break the recognizer by returning true.
     */
    boolean onResult(String command, boolean isPartialResult);

    /**
     * Notify the user the proceeding of speech recognition process. User may provide audio feedback
     * by the eventId.
     */
    default void onFeedbackEvent(@FeedbackEvent int eventId) {}

    /** Notifying the user the error causes speech recognition to be reported. */
    default void onError(int error) {}
  }

  SpeechRecognizerRequester speechRecognizerRequester;

  public RecognitionListener speechRecognitionListener =
      new RecognitionListener() {
        @Override
        public void onBeginningOfSpeech() {
          // Nothing to do.
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
          // Nothing to do.
        }

        /** Turns the mic off after the user has stopped speaking. */
        @Override
        public void onEndOfSpeech() {
          setListeningState(false);
          speechRecognizer.stopListening();
        }

        /** If there is an error, alerts the user. */
        @Override
        public void onError(int error) {
          if (feedbackPendingResult()) {
            return;
          }
          LogUtils.v(TAG, "Speech recognizer onError() error=%d", error);
          stopListeningDelayHandler.removeMessages();
          // Note: This will only occur if user turned off mic permissions after initial use.
          if (error == ERROR_INSUFFICIENT_PERMISSIONS) {
            hasMicPermission = false;
          } else if (error == ERROR_RECOGNIZER_BUSY) {
            // Backup case: This should not happen.
            speechRecognizer.stopListening();
          } else if (error == ERROR_NO_MATCH) {
            // No recognition result matched.
          } else if (error == ERROR_SPEECH_TIMEOUT) {
            // Nothing heard.
          } else {
            if (partialResultFastFeedback
                && executeCommandDelayHandler.hasCallbacks(speechRecognitionDelayedSender)) {
              LogUtils.v(
                  TAG, "Perform the pending command immediately before the error determined.");
              // When a runnable handling the received command is pending, EXEC the runnable and
              // check whether the command is valid.
              executeCommandDelayHandler.removeCallbacks(speechRecognitionDelayedSender);
              speechRecognitionDelayedSender.setPartial(false).run();
              if (speechRecognitionDelayedSender.hasRecognized()) {
                // Skip the error handling if command is successfully recognized.
                return;
              }
            }
          }
          speechRecognizerRequester.onError(error);
          reset();
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
          // Nothing to do.
        }

        /** Speech recognition did not fully understand. */
        @Override
        public void onPartialResults(Bundle partialResults) {
          LogUtils.v(TAG, "Speech recognizer onPartialResults()");
          // For watches SpeechRecognizer returns partial results, but the string is recognized
          // correctly and that is enough for Talkback to process voice commands.
          // Hence we try to handle partial results to improve the performance.
          handleResult(
              partialResults.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION),
              /* isPartialResult= */ true);
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
          // Nothing to do.
        }

        /** Gets the results from SpeechRecognizer and converts to a string. */
        @Override
        public void onResults(Bundle results) {
          LogUtils.v(TAG, "Speech recognizer onResults()");
          handleResult(
              results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION),
              /* isPartialResult= */ false);
        }

        @Override
        public void onRmsChanged(float rmsdB) {
          // Nothing to do.
        }
      };

  // This is used only when partialResultFastFeedback is false.
  // The return value @true implies the application acknowledges the recognized speech.
  private boolean feedbackPendingResult() {
    if (partialResultFastFeedback || TextUtils.isEmpty(cachedResult)) {
      return false;
    }
    // When the cachedResult has a pending (partial-result) speech, call-back with the
    // result immediately.
    if (executeCommandDelayHandler.hasCallbacks(speechRecognitionDelayedSender)) {
      executeCommandDelayHandler.removeCallbacks(speechRecognitionDelayedSender);
    }
    speechRecognitionDelayedSender =
        new SpeechRecognitionDelayedSender(SpeechRecognizerPerformer.this);
    speechRecognitionDelayedSender.setPartial(false).setSpeech(cachedResult).run();
    if (speechRecognitionDelayedSender.hasRecognized()) {
      cachedResult = "";
      return true;
    }
    return false;
  }

  /**
   * @param context through which to get the resource.
   * @param maxRecognitionDelayMs defines the session maximum delays in case the recognizer keeps
   *     processing for a long time.
   * @param partialResultFastFeedback tells the performer to notify the recognized partial speech or
   *     notify with it as the final resort.
   */
  public SpeechRecognizerPerformer(
      Context context, int maxRecognitionDelayMs, boolean partialResultFastFeedback) {
    this.context = context;
    this.maxRecognitionDelayMs = maxRecognitionDelayMs;
    this.partialResultFastFeedback = partialResultFastFeedback;
    // Provide a default listener to avoid NPE.
    speechRecognizerRequester = (command, isPartialResult) -> false;
  }

  /**
   * @param speechRecognizerRequester the communication medium to interact with Speech Recognition
   *     user.
   */
  public void setListener(SpeechRecognizerRequester speechRecognizerRequester) {
    this.speechRecognizerRequester = speechRecognizerRequester;
  }

  public void doListening() {
    if (speechRecognizerRequester == null) {
      LogUtils.e(TAG, "doListening:Speech listener's not set.");
      return;
    }
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      LogUtils.e(TAG, "Platform does not support voice command.");
      speechRecognizerRequester.onFeedbackEvent(
          SpeechRecognizerRequester.EVENT_PLATFORM_NOT_SUPPORT);
      return;
    } else if (!SettingsUtils.allowLinksOutOfSettings(context)) {
      LogUtils.e(TAG, "Reject voice command during setup.");
      speechRecognizerRequester.onFeedbackEvent(
          SpeechRecognizerRequester.EVENT_OPERATION_FORBIDDEN_DURING_SUW);
      return;
    }

    sessionActive = true;
    if (!hasMicPermission()) {
      if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
          == PackageManager.PERMISSION_GRANTED) {
        hasMicPermission = true;
      } else {
        getMicPermission();
        return;
      }
    }

    startListening();
  }

  public void stopListening() {
    sessionActive = false;
    if (speechRecognizer != null) {
      speechRecognizer.stopListening();
    }
    if (executeCommandDelayHandler.hasCallbacks(speechRecognitionDelayedSender)) {
      executeCommandDelayHandler.removeCallbacks(speechRecognitionDelayedSender);
    }
  }

  /** Start the speech recognition session. */
  @VisibleForTesting
  void startListening() {
    if (isListening) {
      return;
    }

    if (speechRecognizer == null) {
      createSpeechObjects();
    }
    cachedResult = "";
    setListeningState(true);
    speechRecognizer.startListening(recognizerIntent);
    stopListeningDelayHandler.delay(maxRecognitionDelayMs, null);
  }

  public void reset() {
    try {
      if (speechRecognizer != null) {
        speechRecognizer.setRecognitionListener(null);
        speechRecognizer.cancel();
        speechRecognizer.destroy();
        speechRecognizer = null;
      }
    } catch (java.lang.IllegalArgumentException e) {
      // SpeechRecognizer#destroy may throw exception for immature service connection.
      e.printStackTrace();
    }
    setListeningState(false);
    recognizerIntent = null;
  }

  private boolean hasMicPermission() {
    return hasMicPermission;
  }

  /**
   * Handles the result recognized by speech recognizer and sends that over to gesture controller.
   * Called by onPartialResults(), which generates a whole sequence of incomplete results, that need
   * to be de-duplicated. Also called by onResults() never, once, or possibly more than once, also
   * requiring de-duplication. So handleResult() delays responding to un/recognized commands, to
   * allow newer results to replace older results.
   *
   * @param result A series of speech strings recognized.
   * @param isPartialResult Does the result come from onResults() or onPartialResults()
   */
  private void handleResult(List<String> result, boolean isPartialResult) {

    // Refresh auto-shut-off timer.
    stopListeningDelayHandler.removeMessages();
    stopListeningDelayHandler.delay(maxRecognitionDelayMs, null);

    // Record whether the internal recognizer can produce final (non-partial) results.
    if (!isPartialResult) {
      recognizerProducesFinalResults = true;
    }

    // If a final-result is expected, discard the partial-result.
    if (recognizerProducesFinalResults && isPartialResult) {
      if (PREFER_FINAL_RESULT) {
        // When network is temporarily unreachable, SpeechRecognizer may stop callback the
        // onResults. In this case, TalkBack can still get the valid command from onPartialResults.
        // We consider to remove this logic.
        return;
      }
    }

    LogUtils.v(
        TAG,
        "Speech recognized %s: %s",
        (isPartialResult ? "partial" : "final"),
        (result == null) ? "null" : String.format("\"%s\"", TextUtils.join("\" \"", result)));

    if (!isPartialResult) {
      stopListeningDelayHandler.removeMessages();
      reset();
    }

    // Cancel commands from overlapping partial/results.
    executeCommandDelayHandler.removeCallbacksAndMessages(null);

    final String speech = (result == null || result.isEmpty()) ? null : result.get(0);
    // SpeechRecognizer after recognizing the speech is triggering onPlaybackConfigChanged
    // in AudioPlaybackMonitor for config USAGE_ASSISTANCE_NAVIGATION_GUIDANCE, which is
    // activating VoiceActionMonitor#AudioPlaybackStateChangedListener() and interrupting
    // all talkback feedback. This delay would help avoid the interruption while processing
    // voice commands in GestureController.
    // Although AudioPlayback is only available from O, adding this delay for all versions,
    // helps to make REFERTO difficult to reproduce.
    // This delay also de-duplicates commands from partial-recognition results.
    // Wait longer if final-result is unknown, because more partial-results may be coming.
    long commandDelayMs =
        isPartialResult ? partialSpeechFeedbackDelayMs : FINAL_SPEECH_COMMAND_PROCESS_DELAY_MS;
    cachedResult = "";
    if (!TextUtils.isEmpty(speech)) {
      if (partialResultFastFeedback || !isPartialResult) {
        speechRecognitionDelayedSender =
            new SpeechRecognitionDelayedSender(this).setPartial(isPartialResult).setSpeech(speech);
        executeCommandDelayHandler.postDelayed(speechRecognitionDelayedSender, commandDelayMs);
      } else {
        cachedResult = speech;
      }
    }
  }

  /** Calls method to create speech recognizer, recognition intent and speechRecognitionListener. */
  private void createSpeechObjects() {
    createSpeechRecognizer();
    createRecognizerIntent();
    setSpeechRecognitionListener();
  }

  /** Creates a speech recognizer & checks if the user has voice recognition ability. */
  private void createSpeechRecognizer() {
    if (speechRecognizerRequester == null) {
      LogUtils.e(TAG, "createSpeechRecognizer:Speech listener's not set.");
      return;
    }
    // Checks if user can use voice recognition.
    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
    if (!SpeechRecognizer.isRecognitionAvailable(context)) {
      speechRecognizerRequester.onFeedbackEvent(
          SpeechRecognizerRequester.EVENT_FEATURE_UNAVAILABLE);
    }
  }

  /** Activated when SpeechRecognizer has not stopped listening after 10 seconds. */
  private void timeOut() {
    if (speechRecognizerRequester == null) {
      LogUtils.e(TAG, "timeOut:Speech listener's not set.");
      return;
    }
    stopListening();
    if (!feedbackPendingResult()) {
      // Check whether a partial result's pending.
      speechRecognizerRequester.onError(ERROR_SPEECH_TIMEOUT);
    }
    reset();
  }

  private void setListeningState(boolean isListening) {
    if (speechRecognizerRequester == null) {
      LogUtils.e(TAG, "setListeningState:Speech listener's not set.");
      return;
    }
    if (this.isListening == isListening) {
      return;
    }
    this.isListening = isListening;
    speechRecognizerRequester.onFeedbackEvent(
        isListening
            ? SpeechRecognizerRequester.EVENT_START_LISTENING
            : SpeechRecognizerRequester.EVENT_STOP_LISTENING);
  }

  /** Create and initialize the recognition intent. */
  private void createRecognizerIntent() {
    // Works without wifi, but provides many extra partial results. Respects the system language.
    recognizerIntent =
        new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName())
            .putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
  }

  /** Creates RecognitionListener and connects recognition listener to speech recognizer. */
  private void setSpeechRecognitionListener() {
    // Note: Need to setRecognitionListener before you can start listening to anything.
    if (speechRecognizer != null) {
      speechRecognizer.setRecognitionListener(speechRecognitionListener);
    }
  }

  /** Calls activity that asks user for mic access for talkback. */
  @VisibleForTesting
  protected void getMicPermission() {
    // Creates an intent filter for broadcast receiver.
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_DONE);
    ContextCompat.registerReceiver(context, receiver, filter, RECEIVER_EXPORTED);
    PermissionUtils.requestPermissions(context, permission.RECORD_AUDIO);
    speechRecognizerRequester.onFeedbackEvent(
        SpeechRecognizerRequester.EVENT_MIC_PERMISSION_REQUESTED);
  }

  private final BroadcastReceiver receiver =
      new BroadcastReceiver() {
        /** Broadcast to start speech recognition if the user accepts. */
        @Override
        public void onReceive(Context context, Intent intent) {
          context.unregisterReceiver(receiver);
          if (speechRecognizerRequester == null) {
            LogUtils.e(TAG, "onReceive:Speech listener's not set.");
            return;
          }
          String[] permissions = intent.getStringArrayExtra(PERMISSIONS);
          int[] grantResults = intent.getIntArrayExtra(GRANT_RESULTS);
          if (permissions == null || grantResults == null) {
            return;
          }
          // If the mic permission request is accepted by the user.
          for (int i = 0; i < permissions.length; i++) {
            if (TextUtils.equals(permissions[i], permission.RECORD_AUDIO)
                && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
              hasMicPermission = true;
              if (sessionActive) {
                // If user hadn't abort the session, notify user & resume the speech recognition
                // after Mic permission's granted.
                speechRecognizerRequester.onFeedbackEvent(
                    SpeechRecognizerRequester.EVENT_DIALOG_CONFIRM);
                startListening();
              }
              return;
            }
          }
          // If not accepted, show a toast for the user.
          speechRecognizerRequester.onFeedbackEvent(
              SpeechRecognizerRequester.EVENT_MIC_PERMISSION_NOT_GRANTED);
        }
      };

  private static class SpeechRecognitionDelayedSender implements Runnable {
    private boolean isPartial;
    private String speech;
    private boolean hasRecognized = false;
    private final SpeechRecognizerPerformer parent;

    SpeechRecognitionDelayedSender(SpeechRecognizerPerformer parent) {
      this.parent = parent;
    }

    public SpeechRecognizerPerformer.SpeechRecognitionDelayedSender setSpeech(String speech) {
      this.speech = speech;
      return this;
    }

    public SpeechRecognizerPerformer.SpeechRecognitionDelayedSender setPartial(boolean isPartial) {
      this.isPartial = isPartial;
      return this;
    }

    public boolean hasRecognized() {
      return hasRecognized;
    }

    private boolean handleSpeech(String speech, boolean isPartialResult) {
      boolean result = parent.speechRecognizerRequester.onResult(speech, isPartialResult);
      if (result) {
        parent.stopListeningDelayHandler.removeMessages();
        parent.reset();
      }
      return result;
    }

    @Override
    public void run() {
      hasRecognized = handleSpeech(speech, isPartial);
    }
  }
}
