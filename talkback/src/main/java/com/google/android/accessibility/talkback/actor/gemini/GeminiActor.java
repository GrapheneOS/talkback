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

package com.google.android.accessibility.talkback.actor.gemini;

import static android.widget.Toast.LENGTH_SHORT;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.GEMINI_ON_DEVICE_RESPONSE_LATENCY;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.GEMINI_RESPONSE_LATENCY;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason.BITMAP_COMPRESSION_FAIL;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason.DISABLED;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason.EMPTY_PROMPT;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason.FEATURE_DOWNLOADING;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason.NETWORK_ERROR;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason.NO_IMAGE;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason.UNSUPPORTED;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason.ERROR_BLOCKED;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason.ERROR_NOT_FINISHED;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason.ERROR_PARSING_RESULT;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason.ERROR_RESPONSE;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason.STOP;
import static com.google.android.accessibility.talkback.actor.gemini.ui.screenqa.ErrorMessagesKt.getErrorMessage;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GeminiFeedbackType.GEMINI_FEEDBACK_NONE;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GeminiFeedbackType.GEMINI_FEEDBACK_THUMBS_DOWN;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GeminiFeedbackType.GEMINI_FEEDBACK_THUMBS_UP;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static java.util.concurrent.TimeUnit.SECONDS;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.SystemClock;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.StringRes;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.GeminiRequest;
import com.google.android.accessibility.talkback.Feedback.ScreenOverviewResult;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.PrimesController;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.gemini.AiCoreEndpoint.AiFeatureDownloadCallback;
import com.google.android.accessibility.talkback.actor.gemini.GeminiCommand.CommonRequest;
import com.google.android.accessibility.talkback.actor.gemini.GeminiCommand.ScreenOverview;
import com.google.android.accessibility.talkback.actor.gemini.GeminiCommand.ScreenQuery;
import com.google.android.accessibility.talkback.actor.gemini.progress.DefaultProgressToneProvider;
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressTonePlayer;
import com.google.android.accessibility.talkback.actor.gemini.progress.ProgressToneProvider.Tone;
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse;
import com.google.android.accessibility.talkback.actor.gemini.ui.BottomSheetResultOverlay;
import com.google.android.accessibility.talkback.actor.gemini.ui.CaptionResultDialog;
import com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.ImageQnaMessage;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GeminiChatEntry;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GeminiDescription;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType;
import com.google.android.accessibility.talkback.imagecaption.Result;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** GeminiActor performs Gemini commands, via {@link GeminiEndpoint}. */
public class GeminiActor {

  /** Enumerates the supported actions for requesting a Gemini command. */
  public enum Action {
    UNKNOWN,
    IMAGE_DESCRIPTION,
    SCREEN_DESCRIPTION,
    IMAGE_QNA,
    SCREEN_QNA
  }

  /** Enumerates the possible reasons that is associated with a Gemini response. */
  public enum FinishReason {
    STOP, // Natural stop point of the model or provided stop sequence.
    ERROR_PARSING_RESULT,
    ERROR_RESPONSE,
    ERROR_BLOCKED,
    ERROR_NOT_FINISHED
  }

  /** Enumerates the possible error reasons for requesting a Gemini command. */
  public enum ErrorReason {
    UNSUPPORTED,
    DISABLED,
    NETWORK_ERROR,
    NO_IMAGE,
    EMPTY_PROMPT,
    BITMAP_COMPRESSION_FAIL,
    FEATURE_DOWNLOADING,
    JOB_CANCELLED
  }

  /** Defines a callback interface for handling responses received from Gemini server. */
  public interface GeminiResponseListener extends GeminiResponseCallback<String> {}

  /** Defines the core contract for classes implementing a connection to Gemini server. */
  public interface GeminiEndpoint {
    boolean createRequestGeminiCommand(
        String text,
        Bitmap image,
        boolean manualTrigger,
        GeminiResponseListener geminiResponseListener);

    default boolean createRequestGeminiCommand(GeminiCommand command) {
      return false;
    }

    void cancelCommand();

    /**
     * In general, TalkBack will maintain at most one outstanding Gemini transaction at all time. It
     * will abort the pending request before requesting a new one. One exception is when a manual
     * triggered transaction is pending, then an auto triggered transaction is not allowed until the
     * manual triggered one is done.
     *
     * @return if any manual triggered transaction is pending.
     */
    boolean hasPendingTransaction();
  }

  /** Read-only interface for GeminiActor state data. */
  public class State {
    public boolean hasAiCore() {
      return aiCoreEndpoint.hasAiCore();
    }

    public boolean isAiFeatureAvailable() {
      return aiCoreEndpoint.isAiFeatureAvailable();
    }
  }

  private static final String TAG = "GeminiActor";

  private static final long KEEP_WAITING_TIME_MS = SECONDS.toMillis(35);

  private final Context context;
  private final GeminiEndpoint geminiEndpoint;
  private final AiCoreEndpoint aiCoreEndpoint;
  private Pipeline.FeedbackReturner pipeline;
  private TalkBackAnalytics analytics;
  private final PrimesController primesController;
  // Record the start time of Gemini request, with which TalkBack can measure the latency when the
  // Gemini response is received.
  private long startTime;
  private final Handler mainHandler;
  private final ProgressTonePlayer progressTonePlayer;

  private final BottomSheetResultOverlay bottomSheetResultOverlay;

  public final State state;

  private long downloadedSizeInBytes = -1;
  private long featureSizeInBytes = -1;

  // Cached images for requesting Image Q&A.
  private final Map<Integer, byte[]> cachedImageMap = new HashMap<>();

  // Record the Gemini requestId to Gemini type(Server-side/On-device) mapping. Theoretically, the
  // map would be at most only one entry. We check each time before a new entry added, and clear the
  // map when it reaches the maximum value(REQUEST_ID_MAP_CAPACITY).
  // TODO: Consider to remove this protection scheme in the future.
  private final Map<Integer, Boolean> requestIdMap = new HashMap<>();
  private static final int REQUEST_ID_MAP_CAPACITY = 100;

  private AiFeatureDownloadCallback aiFeatureDownloadCallback =
      new AiFeatureDownloadCallback() {
        @Override
        public void onDownloadProgress(long currentSizeInBytes, long totalSizeInBytes) {
          downloadedSizeInBytes = currentSizeInBytes;
          featureSizeInBytes = totalSizeInBytes;
        }

        @Override
        public void onDownloadCompleted() {
          LogUtils.d(TAG, "GeminiActor - Feature download completed.");
        }
      };

  /** Retrieves necessary data from clients. */
  public interface GeminiChatMetricProcessor {
    void sendLog(boolean isScreenDescription, List<ImageQnaMessage> collectionList);
  }

  private final GeminiChatMetricProcessor chatMetrics =
      (isScreenDescription, collectionList) -> {
        GeminiDescription description = null;
        List<GeminiChatEntry> chatList = new ArrayList<>();
        boolean isFirstEntry = true;
        for (ImageQnaMessage collection : collectionList) {
          if (isFirstEntry) {
            description =
                new GeminiDescription(
                    isScreenDescription,
                    collection.isThumbUp()
                        ? GEMINI_FEEDBACK_THUMBS_UP
                        : collection.isThumbDown()
                            ? GEMINI_FEEDBACK_THUMBS_DOWN
                            : GEMINI_FEEDBACK_NONE);
            isFirstEntry = false;
          } else {
            chatList.add(
                new GeminiChatEntry(
                    isScreenDescription,
                    collection.isThumbUp()
                        ? GEMINI_FEEDBACK_THUMBS_UP
                        : collection.isThumbDown()
                            ? GEMINI_FEEDBACK_THUMBS_DOWN
                            : GEMINI_FEEDBACK_NONE,
                    collection.isVoiceInput(),
                    collection.getQuestionLength()));
          }
        }
        if (description != null) {
          analytics.onGeminiChatEntry(description, chatList);
        }
      };

  public GeminiActor(
      TalkBackService service,
      TalkBackAnalytics analytics,
      PrimesController primesController,
      GeminiEndpoint geminiEndpoint,
      AiCoreEndpoint aiCoreEndpoint) {
    context = service;
    this.analytics = analytics;
    this.primesController = primesController;
    this.geminiEndpoint = geminiEndpoint;
    this.mainHandler = new Handler(context.getMainLooper());
    this.progressTonePlayer =
        new ProgressTonePlayer(
            new DefaultProgressToneProvider(),
            this::playTone,
            KEEP_WAITING_TIME_MS,
            this::onTimeout);
    this.aiCoreEndpoint = aiCoreEndpoint;
    this.aiCoreEndpoint.setAiFeatureDownloadCallback(aiFeatureDownloadCallback);
    state = new State();
    bottomSheetResultOverlay = new BottomSheetResultOverlay(service, chatMetrics);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
    bottomSheetResultOverlay.setPipeline(pipeline);
  }

  /**
   * Requests a new Gemini online session, utilizing the provided text and image data.
   *
   * @param requestId The requestId for identify the result
   * @param text The text content to be included in the Gemini session.
   * @param image The image to be associated with the Gemini session.
   */
  public void requestOnlineGeminiCommand(int requestId, Action action, String text, Bitmap image) {
    if (requestIdMap.size() > REQUEST_ID_MAP_CAPACITY) {
      LogUtils.w(TAG, "The requestIdMap reaches its max capacity.");
      requestIdMap.clear();
    }
    requestIdMap.put(requestId, /* isServerSide= */ true);
    analytics.onGeminiEvent(
        TalkBackAnalytics.GEMINI_REQUEST, /* serverSide= */ true, /* manualRequest= */ true);
    startTime = SystemClock.uptimeMillis();
    GeminiResponseListener responseListener =
        new GeminiResponseListener() {
          @Override
          public void onResponse(FinishReason finishReason, String text) {
            // Instead of providing direct feedback, #handleImageCaptionResponse will send the
            // result to the pipeline and rearrange it in ImageCaptioner. For use cases other than
            // image captioning, we need another method to handle the result.
            handleImageCaptionResponse(requestId, finishReason, text, /* manualTrigger= */ true);
          }

          @Override
          public void onError(ErrorReason errorReason) {
            handleImageCaptionErrorResponse(requestId, errorReason, /* manualTrigger= */ true);
          }
        };

    byte[] imageBytes = DataFieldUtils.encodeImageToByteArray(image);
    if (geminiEndpoint.createRequestGeminiCommand(
        new CommonRequest(action, text, imageBytes, /* manualTrigger= */ true, responseListener))) {
      if (requestIdMap.size() > REQUEST_ID_MAP_CAPACITY) {
        LogUtils.w(TAG, "The requestIdMap reaches its max capacity.");
        requestIdMap.clear();
      }
      requestIdMap.put(requestId, /* isServerSide= */ true);
      progressTonePlayer.stop();
      aiCoreEndpoint.cancelCommand();
      progressTonePlayer.play(/* playTone= */ true, /* delayPlayTone= */ false);
      cachedImageMap.put(requestId, imageBytes);
    }
  }

  /**
   * Requests a new Gemini online session, utilizing the provided screen overview.
   *
   * @param request the request to be sent to Gemini.
   */
  public void requestScreenOverview(GeminiRequest request) {
    var requestId = request.requestId();
    var imageBytes = request.imageBytes();
    var focusedNode = request.focusedNode();
    var query = request.text();
    var a11yTree = request.a11yTree();
    if (imageBytes == null) {
      var image = request.image();
      if (image != null) {
        imageBytes = DataFieldUtils.encodeImageToByteArray(image);
      }
      if (!image.isRecycled()) {
        image.recycle();
      }
    }
    if (imageBytes == null) {
      LogUtils.e(TAG, "Invalid request for requestScreenOverview: required fields are null");
      return;
    }

    if (requestIdMap.size() > REQUEST_ID_MAP_CAPACITY) {
      LogUtils.w(TAG, "The requestIdMap reaches its max capacity.");
      requestIdMap.clear();
    }
    requestIdMap.put(requestId, /* isServerSide= */ true);
    analytics.onGeminiEvent(
        TalkBackAnalytics.GEMINI_REQUEST, /* serverSide= */ true, /* manualRequest= */ true);
    startTime = SystemClock.uptimeMillis();
    var responseListener =
        new GeminiResponseCallback<OverviewResponse>() {
          @Override
          public void onResponse(FinishReason finishReason, OverviewResponse response) {
            // Instead of providing direct feedback, #handleImageCaptionResponse will send the
            // result to the pipeline and rearrange it in ImageCaptioner. For use cases other than
            // image captioning, we need another method to handle the result.
            handleScreenOverviewResponse(requestId, finishReason, response);
          }

          @Override
          public void onError(ErrorReason errorReason) {
            handleScreenOverviewErrorResponse(requestId, errorReason);
          }
        };

    GeminiCommand command;
    if (TextUtils.isEmpty(query)) {
      // if the user didn't ask anything, we just give an overview.
      command = new ScreenOverview(imageBytes, focusedNode, responseListener);
    } else if (a11yTree != null) {
      command =
          new ScreenQuery(imageBytes, a11yTree, query, request.chatHistory(), responseListener);
    } else {
      throw new IllegalArgumentException("Invalid GeminiRequest " + request);
    }

    if (geminiEndpoint.createRequestGeminiCommand(command)) {
      if (requestIdMap.size() > REQUEST_ID_MAP_CAPACITY) {
        LogUtils.w(TAG, "The requestIdMap reaches its max capacity.");
        requestIdMap.clear();
      }
      requestIdMap.put(requestId, /* isServerSide= */ true);
      progressTonePlayer.stop();
      aiCoreEndpoint.cancelCommand();
      progressTonePlayer.play(/* playTone= */ true, /* delayPlayTone= */ false);
    }

  }

  /**
   * Requests a new Gemini online session for image Q&A.
   *
   * @param text The question text from the user.
   * @param imageBytes The image to be associated with the Gemini session.
   */
  public void requestImageQna(Action action, String text, byte[] imageBytes) {
    GeminiResponseListener responseListener =
        new GeminiResponseListener() {
          @Override
          public void onResponse(FinishReason finishReason, String text) {
            // TODO: Add error handling for Gemini responses.
            switch (finishReason) {
              case STOP ->
                  analytics.onGeminiEvent(
                      TalkBackAnalytics.GEMINI_SUCCESS,
                      /* serverSide= */ true,
                      /* manualRequest= */ true);
              case ERROR_PARSING_RESULT ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_FAIL_TO_PARSE_RESPONSE, /* serverSide= */ true);
              case ERROR_RESPONSE ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_PROTOCOL_ERROR, /* serverSide= */ true);
              case ERROR_BLOCKED ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_CONTENT_BLOCKED, /* serverSide= */ true);
              case ERROR_NOT_FINISHED -> {
                //  Do nothing.
              }
            }
            progressTonePlayer.stop();
            if (TextUtils.isEmpty(text)) {
              // Response blocked.
              text = context.getString(R.string.gemini_error_message);
            }
            speak(text);
            bottomSheetResultOverlay.onImageQnaResponse(text);
          }

          @Override
          public void onError(ErrorReason errorReason) {
            switch (errorReason) {
              case UNSUPPORTED ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_SERVICE_UNAVAILABLE, /* serverSide= */ true);
              case DISABLED ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_USER_NOT_OPT_IN, /* serverSide= */ true);
              case NETWORK_ERROR ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_NETWORK_UNAVAILABLE, /* serverSide= */ true);
              case NO_IMAGE ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_NO_SCREENSHOT_PROVIDED, /* serverSide= */ true);
              case BITMAP_COMPRESSION_FAIL ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_FAIL_TO_ENCODE_PICTURE, /* serverSide= */ true);
              case EMPTY_PROMPT ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_COMMAND_NOT_PROVIDED, /* serverSide= */ true);
              case FEATURE_DOWNLOADING -> {
                // Do nothing
              }
              case JOB_CANCELLED ->
                  analytics.onGeminiFailEvent(
                      TalkBackAnalytics.GEMINI_FAIL_USER_ABORT, /* serverSide= */ true);
            }
            progressTonePlayer.stop();
            String text = context.getString(R.string.gemini_error_message);
            speak(text);
            bottomSheetResultOverlay.onImageQnaResponse(text);
          }
        };

    analytics.onGeminiEvent(
        TalkBackAnalytics.GEMINI_REQUEST, /* serverSide= */ true, /* manualRequest= */ true);
    if (geminiEndpoint.createRequestGeminiCommand(
        new CommonRequest(action, text, imageBytes, /* manualTrigger= */ true, responseListener))) {
      progressTonePlayer.stop();
      aiCoreEndpoint.cancelCommand();
      progressTonePlayer.play(/* playTone= */ true, /* delayPlayTone= */ false);
    }
  }

  /**
   * Requests a new on-device Gemini session for image captioning.
   *
   * @param requestId The requestId for identify the result
   * @param image The image to be associated with the Gemini session.
   * @param manualTrigger Whether the request is triggered by user manually.
   */
  public void requestAiCoreImageCaptioning(int requestId, Bitmap image, boolean manualTrigger) {
    if (!manualTrigger
        && (aiCoreEndpoint.hasPendingTransaction() || geminiEndpoint.hasPendingTransaction())) {
      return;
    }
    if (requestIdMap.size() > REQUEST_ID_MAP_CAPACITY) {
      LogUtils.w(TAG, "The requestIdMap reaches its max capacity.");
      requestIdMap.clear();
    }
    requestIdMap.put(requestId, /* isServerSide= */ false);
    analytics.onGeminiEvent(
        TalkBackAnalytics.GEMINI_REQUEST, /* serverSide= */ false, manualTrigger);
    if (!aiCoreEndpoint.hasAiCore()) {
      handleImageCaptionErrorResponse(requestId, UNSUPPORTED, manualTrigger);
      return;
    }
    startTime = SystemClock.uptimeMillis();
    if (aiCoreEndpoint.createRequestGeminiCommand(
        context.getString(R.string.image_caption_with_gemini_prefix),
        image,
        manualTrigger,
        new GeminiResponseListener() {
          @Override
          public void onResponse(FinishReason finishReason, String text) {
            handleImageCaptionResponse(requestId, finishReason, text, manualTrigger);
          }

          @Override
          public void onError(ErrorReason errorReason) {
            handleImageCaptionErrorResponse(requestId, errorReason, manualTrigger);
          }
        })) {
      if (requestIdMap.size() > REQUEST_ID_MAP_CAPACITY) {
        LogUtils.w(TAG, "The requestIdMap reaches its max capacity.");
        requestIdMap.clear();
      }
      requestIdMap.put(requestId, /* isServerSide= */ false);
      progressTonePlayer.stop();
      geminiEndpoint.cancelCommand();
      progressTonePlayer.play(/* playTone= */ true, /* delayPlayTone= */ !manualTrigger);
    }
  }

  public void displayImageCaptioningResultDialog(
      int requestId,
      Result imageDescriptionResult,
      Result iconLabelResult,
      Result ocrTextResult,
      boolean isScreenDescription) {
    byte[] image = cachedImageMap.get(requestId);
    // Clear all caches once there is an image used by the image caption result dialog.
    cachedImageMap.clear();

    if (isScreenDescription && !GeminiConfiguration.screenOverviewEnabled(context)) {
      LogUtils.d(TAG, "Screen description is disabled");
      return;
    }

    if (isSupportImageQna(requestId)) {
      mainHandler.post(
          () ->
              bottomSheetResultOverlay.showImageCaptionResultBottomSheet(
                  image,
                  imageDescriptionResult,
                  iconLabelResult,
                  ocrTextResult,
                  isScreenDescription));
    } else {
      mainHandler.post(
          () ->
              new CaptionResultDialog(
                      context, imageDescriptionResult, iconLabelResult, ocrTextResult)
                  .showDialog());
    }
    if (requestIdMap.containsKey(requestId)) {
      requestIdMap.remove(requestId);
    }
  }

  public void displayScreenOverviewResultDialog(ScreenOverviewResult result) {
    if (GeminiConfiguration.screenOverviewEnabled(context)) {
      if (isSupportImageQna(result.requestId())) {
        mainHandler.post(
            () -> bottomSheetResultOverlay.showScreenOverviewResultBottomSheet(result));
      } else {
        // Dialog result without the Q&A feature.
        if (result.response() instanceof OverviewResponse.Success overview) {
          mainHandler.post(
              () ->
                  new CaptionResultDialog(
                          context,
                          Result.create(
                              CaptionType.SCREEN_OVERVIEW, overview.getOverview().getSummary()))
                      .showDialog());
        } else if (result.response() instanceof OverviewResponse.Error error) {
          Toast.makeText(context, getErrorMessage(error), LENGTH_SHORT).show();
        }
      }
    } else {
      Toast.makeText(
              context,
              context.getString(R.string.summary_pref_gemini_support_disabled),
              LENGTH_SHORT)
          .show();
    }
  }

  public void requestDismissDialog() {
    bottomSheetResultOverlay.requestDismissDialog();
  }

  public void stopProgressTones() {
    LogUtils.d(TAG, "stopProgressTones");
    progressTonePlayer.stopTones();
  }

  public void onUnbind() {
    if (aiCoreEndpoint != null) {
      aiCoreEndpoint.onUnbind();
    }
    cachedImageMap.clear();
  }

  private boolean isServerSideRequest(int requestId) {
    return requestIdMap.containsKey(requestId) && Objects.equals(requestIdMap.get(requestId), true);
  }

  private boolean isSupportImageQna(int requestId) {
    if (!isServerSideRequest(requestId)) {
      return false;
    }

    return GeminiConfiguration.imageQnaEnabled(context);
  }

  private void playTone(Tone tone) {
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, tone.tone);
  }

  private void handleImageCaptionResponse(
      int requestId, FinishReason finishReason, String text, boolean manualTrigger) {
    switch (finishReason) {
      case STOP -> {
        analytics.onGeminiEvent(
            TalkBackAnalytics.GEMINI_SUCCESS, isServerSideRequest(requestId), manualTrigger);
        responseImageCaptionResult(
            requestId, text, /* isSuccess= */ true, STOP, /* errorReason= */ null, manualTrigger);
        PrimesController.TimerAction action = GEMINI_RESPONSE_LATENCY;
        if (requestIdMap.containsKey(requestId)
            && Objects.equals(requestIdMap.get(requestId), false)) {
          action = GEMINI_ON_DEVICE_RESPONSE_LATENCY;
        }
        primesController.recordDuration(action, startTime, SystemClock.uptimeMillis());
      }
      case ERROR_PARSING_RESULT -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_FAIL_TO_PARSE_RESPONSE, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.gemini_error_parsing_result,
            /* isSuccess= */ false,
            ERROR_PARSING_RESULT,
            manualTrigger);
      }
      case ERROR_RESPONSE -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_PROTOCOL_ERROR, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.gemini_error_message,
            /* isSuccess= */ false,
            ERROR_RESPONSE,
            manualTrigger);
      }
      case ERROR_BLOCKED -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_CONTENT_BLOCKED, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.gemini_block_message,
            /* isSuccess= */ false,
            ERROR_BLOCKED,
            manualTrigger);
      }
      case ERROR_NOT_FINISHED -> {
        //  Do nothing.
      }
    }
    if (finishReason != STOP && requestIdMap.containsKey(requestId)) {
      // We don't remove the requestId from the map for the STOP (Gemini replied successfully) case,
      // because the requestId is needed to determine the UI (either bottomsheet or dialog) style.
      requestIdMap.remove(requestId);
    }
    progressTonePlayer.stop();
  }

  private void handleImageCaptionErrorResponse(
      int requestId, ErrorReason errorReason, boolean manualTrigger) {
    switch (errorReason) {
      case UNSUPPORTED -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_SERVICE_UNAVAILABLE, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.gemini_error_message,
            /* isSuccess= */ false,
            UNSUPPORTED,
            manualTrigger);
      }
      case DISABLED -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_USER_NOT_OPT_IN, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.summary_pref_gemini_support_disabled,
            /* isSuccess= */ false,
            DISABLED,
            manualTrigger);
      }
      case NETWORK_ERROR -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_NETWORK_UNAVAILABLE, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.gemini_network_error,
            /* isSuccess= */ false,
            NETWORK_ERROR,
            manualTrigger);
      }
      case NO_IMAGE -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_NO_SCREENSHOT_PROVIDED, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.gemini_screenshot_unavailable,
            /* isSuccess= */ false,
            NO_IMAGE,
            manualTrigger);
      }
      case BITMAP_COMPRESSION_FAIL -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_FAIL_TO_ENCODE_PICTURE, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            R.string.gemini_screenshot_unavailable,
            /* isSuccess= */ false,
            BITMAP_COMPRESSION_FAIL,
            manualTrigger);
      }
      case EMPTY_PROMPT -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_COMMAND_NOT_PROVIDED, isServerSideRequest(requestId));
        responseImageCaptionResult(
            requestId,
            context.getString(
                R.string.voice_commands_partial_result,
                context.getString(R.string.title_pref_help)),
            /* isSuccess= */ false,
            EMPTY_PROMPT,
            manualTrigger);
      }
      case FEATURE_DOWNLOADING -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_SERVICE_UNAVAILABLE, isServerSideRequest(requestId));
        if (featureSizeInBytes > 0
            && downloadedSizeInBytes >= 0
            && (featureSizeInBytes >= downloadedSizeInBytes)) {
          long downloadedSizeInMb = downloadedSizeInBytes / (1024 * 1024);
          long sizeInMb = featureSizeInBytes / (1024 * 1024);
          responseImageCaptionResult(
              requestId,
              context.getString(
                  R.string.message_aifeature_downloading_with_progress,
                  downloadedSizeInMb,
                  sizeInMb),
              /* isSuccess= */ false,
              FEATURE_DOWNLOADING,
              manualTrigger);
        } else {
          LogUtils.w(TAG, "Can't get the download progress.");
          responseImageCaptionResult(
              requestId,
              R.string.message_aifeature_downloading,
              /* isSuccess= */ false,
              FEATURE_DOWNLOADING,
              manualTrigger);
        }
      }
      case JOB_CANCELLED ->
          analytics.onGeminiFailEvent(
              TalkBackAnalytics.GEMINI_FAIL_USER_ABORT, isServerSideRequest(requestId));
    }
    progressTonePlayer.stop();
  }

  private void handleScreenOverviewResponse(
      int requestId, FinishReason finishReason, OverviewResponse response) {
    switch (finishReason) {
      case STOP -> {
        analytics.onGeminiEvent(
            TalkBackAnalytics.GEMINI_SUCCESS, /* serverSide= */ true, /* manualRequest= */ true);
        responseScreenOverviewResult(requestId, response);
      }
      case ERROR_PARSING_RESULT -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_FAIL_TO_PARSE_RESPONSE, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, response);
      }
      case ERROR_RESPONSE -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_PROTOCOL_ERROR, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, response);
      }
      case ERROR_BLOCKED -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_CONTENT_BLOCKED, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, response);
      }
      case ERROR_NOT_FINISHED -> {
        //  Do nothing.
      }
    }
    if (requestIdMap.containsKey(requestId)) {
      requestIdMap.remove(requestId);
    }
    progressTonePlayer.stop();
  }

  private void speak(CharSequence text) {
    pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(text));
  }

  private void handleScreenOverviewErrorResponse(int requestId, ErrorReason errorReason) {
    switch (errorReason) {
      case UNSUPPORTED -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_SERVICE_UNAVAILABLE, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, new OverviewResponse.Error(errorReason, null));
      }
      case DISABLED -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_USER_NOT_OPT_IN, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, new OverviewResponse.Error(errorReason, null));
      }
      case NETWORK_ERROR -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_NETWORK_UNAVAILABLE, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, new OverviewResponse.Error(errorReason, null));
      }
      case NO_IMAGE -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_NO_SCREENSHOT_PROVIDED, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, new OverviewResponse.Error(errorReason, null));
      }
      case BITMAP_COMPRESSION_FAIL -> {
        analytics.onGeminiFailEvent(
            TalkBackAnalytics.GEMINI_FAIL_FAIL_TO_ENCODE_PICTURE, /* serverSide= */ true);
        responseScreenOverviewResult(requestId, new OverviewResponse.Error(errorReason, null));
      }
      case JOB_CANCELLED ->
          analytics.onGeminiFailEvent(
              TalkBackAnalytics.GEMINI_FAIL_USER_ABORT, /* serverSide= */ true);
      default -> {}
    }
    progressTonePlayer.stop();
  }

  private void responseImageCaptionResult(
      int requestId,
      @StringRes int textId,
      boolean isSuccess,
      ErrorReason errorReason,
      boolean manualTrigger) {
    responseImageCaptionResult(
        requestId,
        context.getString(textId),
        isSuccess,
        ERROR_NOT_FINISHED,
        errorReason,
        manualTrigger);
  }

  private void responseImageCaptionResult(
      int requestId,
      @StringRes int textId,
      boolean isSuccess,
      FinishReason finishReason,
      boolean manualTrigger) {
    responseImageCaptionResult(
        requestId,
        context.getString(textId),
        isSuccess,
        finishReason,
        /* errorReason= */ null,
        manualTrigger);
  }

  private void responseImageCaptionResult(
      int requestId,
      String text,
      boolean isSuccess,
      ErrorReason errorReason,
      boolean manualTrigger) {
    responseImageCaptionResult(
        requestId, text, isSuccess, ERROR_NOT_FINISHED, errorReason, manualTrigger);
  }

  private void responseImageCaptionResult(
      int requestId,
      String text,
      boolean isSuccess,
      FinishReason finishReason,
      ErrorReason errorReason,
      Boolean manualTrigger) {
    if (!isSuccess) {
      // No need to cache the image for fail result.
      cachedImageMap.remove(requestId);
    }
    // Send the result to ImageCaptioner to integrate the resul with OCR and Icon detection, and
    // recycle images.
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.responseImageCaptionResult(
            requestId, text, isSuccess, finishReason, errorReason, manualTrigger));
  }

  private void responseScreenOverviewResult(
      int requestId, @StringRes int textId, boolean isSuccess, ErrorReason errorReason) {
    responseScreenOverviewResult(requestId, new OverviewResponse.Error(errorReason, null));
  }

  private void responseScreenOverviewResult(int requestId, OverviewResponse response) {
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED, Feedback.responseScreenOverviewResult(requestId, response));
  }

  private void onTimeout() {
    geminiEndpoint.cancelCommand();
    aiCoreEndpoint.cancelCommand();
  }
}
