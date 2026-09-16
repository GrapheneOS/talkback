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

package com.google.android.accessibility.talkback.actor.gemini.ui;

import static android.widget.Toast.LENGTH_SHORT;
import static com.google.android.accessibility.talkback.actor.gemini.ui.screenqa.ErrorMessagesKt.getErrorMessage;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.IMAGE_DESCRIPTION;

import android.content.Context;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.Feedback.ScreenOverviewResult;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.Action;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.GeminiChatMetricProcessor;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse;
import com.google.android.accessibility.talkback.imagecaption.Result;
import com.google.android.accessibility.talkback.utils.SpeechRecognizerPerformer;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Arrays;

/** A class to show the Gemini result in a bottom sheet dialog. */
public class BottomSheetResultOverlay {
  private static final String TAG = "BottomSheetResultOverlay";
  private static final int SPEECH_RECOGNIZER_TIMEOUT_MS = 10000;

  private final Context context;
  private final ImageCaptionResultBottomSheet imageCaptionResultBottomSheet;
  private BottomSheetDialog dialog;
  private FeedbackReturner pipeline;
  @Nullable private ScreenQAChatController screenQAChatController;

  public BottomSheetResultOverlay(TalkBackService service, GeminiChatMetricProcessor chatMetrics) {
    context = service.getBaseContext();
    imageCaptionResultBottomSheet =
        new ImageCaptionResultBottomSheet(
            service,
            new SpeechRecognizerPerformer(
                service, SPEECH_RECOGNIZER_TIMEOUT_MS, /* partialResultFastFeedback= */ false),
            chatMetrics);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
    imageCaptionResultBottomSheet.setPipeline(pipeline);
  }

  /**
   * Shows the image captioning result in a bottom sheet dialog.
   *
   * @param imageBytes The image to be used for image Q&A.
   * @param imageDescriptionResult The result of screen overview.
   * @param iconLabelResult The result of icon label.
   * @param ocrTextResult The result of OCR text.
   * @param screenDescription If the dialog is for screen description.
   */
  public void showImageCaptionResultBottomSheet(
      byte[] imageBytes,
      Result imageDescriptionResult,
      Result iconLabelResult,
      Result ocrTextResult,
      boolean screenDescription) {
    var controller =
        new ImageQnAChatController(
            context,
            pipeline,
            screenDescription ? Action.SCREEN_QNA : Action.IMAGE_QNA,
            Arrays.asList(imageDescriptionResult, iconLabelResult, ocrTextResult));
    dialog =
        imageCaptionResultBottomSheet.getBottomSheetDialog(
            controller,
            imageBytes,
            imageDescriptionResult,
            iconLabelResult,
            ocrTextResult,
            screenDescription);

    dialog.getWindow().setType(LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
    dialog.show();
  }

  public void requestDismissDialog() {
    if (dialog != null && dialog.isShowing()) {
      dialog.dismiss();
      dialog = null;
    }
  }

  /**
   * Notifies the image Q&A response to the image caption result bottom sheet.
   *
   * @param message The response of image Q&A.
   */
  public void onImageQnaResponse(String message) {
    imageCaptionResultBottomSheet.onImageQnaResponse(message);
  }

  /**
   * Shows the image captioning result in a bottom sheet dialog.
   *
   * @param screenOverviewResult The result of screen overview.
   */
  public void showScreenOverviewResultBottomSheet(ScreenOverviewResult screenOverviewResult) {
    if (GeminiConfiguration.screenOverviewEndpointEnabled(context)) {
      showRichScreenOverviewResultBottomSheet(screenOverviewResult);
    } else {
      // Rich overviews disabled, just treat it the same as image Q&A
      if (screenOverviewResult.response() instanceof OverviewResponse.Success overview) {
        showImageCaptionResultBottomSheet(
            overview.getImageBytes(),
            Result.create(IMAGE_DESCRIPTION, overview.getOverview().getSummary()),
            /* iconLabelResult= */ null,
            /* ocrTextResult= */ null,
            /* screenDescription= */ true);
      } else if (screenOverviewResult.response() instanceof OverviewResponse.Error error) {
        Toast.makeText(context, getErrorMessage(error), LENGTH_SHORT).show();
      }
    }
  }

  private void showRichScreenOverviewResultBottomSheet(ScreenOverviewResult screenOverviewResult) {
    if (screenOverviewResult.response() instanceof OverviewResponse.Success overview) {
      screenQAChatController =
          new ScreenQAChatController(context, pipeline, Action.SCREEN_QNA, overview);
      dialog =
          imageCaptionResultBottomSheet.getBottomSheetDialog(
              screenQAChatController,
              new byte[0],
              Result.create(IMAGE_DESCRIPTION, overview.getOverview().getSummary()),
              null,
              null, /* isScreenDescription */
              true);
      dialog.getWindow().setType(LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
      dialog.show();
    } else if (screenOverviewResult.response() instanceof OverviewResponse.QueryAnswer answer) {
      if (screenQAChatController == null) {
        LogUtils.e(TAG, "screenQAChatController is null when receiving a result");
        return;
      }
      screenQAChatController.onScreenQueryAnswer(answer);
    } else if (screenOverviewResult.response() instanceof OverviewResponse.Error error) {
      Toast.makeText(context, getErrorMessage(error), LENGTH_SHORT).show();
    }
  }
}
