/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.accessibility.talkback.actor.gemini.ui

import android.content.Context
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.Action
import com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.ImageQnaMessage
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils
import com.google.android.accessibility.talkback.imagecaption.Result

/**
 * Controller for the Image Q&A chatting UI.
 *
 * @param context
 * @param pipeline
 * @param descriptionResults A list of image description, OCR, and icon descriptions. The list may
 *   contain null items if not available. Image description should be first.
 */
class ImageQnAChatController(
  context: Context,
  pipeline: FeedbackReturner,
  action: Action,
  descriptionResults: List<Result?>,
) : BaseQnAChatController(context, pipeline, action) {

  init {
    addNewMessage(
      isLoading = false,
      // TODO: replace this with a custom message type that shows a separate layout
      //  holding all the fields
      ImageQnaMessage(ImageQnaChatAdapter.TYPE_MODEL_MESSAGE).apply {
        text =
          descriptionResults
            .filterNotNull()
            .mapNotNull { result ->
              val resultText = result.text()
              if (resultText.isNullOrBlank()) return@mapNotNull null

              return@mapNotNull when (result.type()) {
                // this always comes first, so it doesn't need a header
                ImageCaptionUtils.CaptionType.IMAGE_DESCRIPTION -> resultText
                ImageCaptionUtils.CaptionType.OCR ->
                  context.getString(R.string.detailed_image_description_ocr_result, resultText)
                ImageCaptionUtils.CaptionType.ICON_LABEL ->
                  context.getString(R.string.detailed_image_description_icon_result, resultText)
                ImageCaptionUtils.CaptionType.SCREEN_OVERVIEW -> resultText
              }
            }
            .joinToString("\n\n")
      },
    )
  }
}
