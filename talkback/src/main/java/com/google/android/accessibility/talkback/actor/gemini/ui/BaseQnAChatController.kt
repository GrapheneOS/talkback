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
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.Action
import com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.ImageQnaMessage
import com.google.android.accessibility.utils.Performance
import com.google.android.libraries.accessibility.utils.log.LogUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Controller for the a generic Q&A chatting UI. */
open class BaseQnAChatController(
  protected val context: Context,
  protected val pipeline: FeedbackReturner,
  protected val action: Action,
) : QnAChatController {

  protected val _uiState: MutableStateFlow<UiState> =
    MutableStateFlow(UiState.Chatting(listOf(), isLoading = true))
  override val uiState = _uiState.asStateFlow()

  override fun onNewCommand(command: String, imageBytesForQna: ByteArray, isVoiceInput: Boolean) {
    val state = uiState.value
    if (state !is UiState.Chatting) {
      LogUtils.e(TAG, "Invalid state for onNewCommand: not in chatting state")
      return
    }

    val questionWithHistory =
      DataFieldUtils.appendHistoryToImageQna(context, command, state.messages, action)

    val userMessage =
      ImageQnaMessage(ImageQnaChatAdapter.TYPE_USER_MESSAGE, command).apply {
        setVoiceInput(isVoiceInput)
      }
    addNewMessage(isLoading = true, userMessage)

    if (action == Action.SCREEN_QNA) {
      pipeline.returnFeedback(
        Performance.EVENT_ID_UNTRACKED,
        Feedback.geminiRequestScreenQna(questionWithHistory, imageBytesForQna),
      )
    } else {
      pipeline.returnFeedback(
        Performance.EVENT_ID_UNTRACKED,
        Feedback.geminiRequestImageQna(questionWithHistory, imageBytesForQna),
      )
    }
  }

  override fun onVoiceError(error: Int) {
    // TODO: add an "Error message" subclass of ImageQnaMessage that shows the error.
    val userMessage =
      ImageQnaMessage(
          ImageQnaChatAdapter.TYPE_USER_MESSAGE,
          context.getString(R.string.image_qna_voice_input_empty),
        )
        .apply { setVoiceInput(true) }
    val modelMessage =
      ImageQnaMessage(
        ImageQnaChatAdapter.TYPE_MODEL_MESSAGE,
        context.getString(R.string.image_qna_answer_unavailable),
      )
    addNewMessage(isLoading = false, userMessage, modelMessage)
  }

  override fun onImageQnaResponse(message: String) {
    val modelMessage =
      ImageQnaMessage(ImageQnaChatAdapter.TYPE_MODEL_MESSAGE).apply { text = message }
    addNewMessage(isLoading = false, modelMessage)
  }

  protected fun addNewMessage(isLoading: Boolean = false, vararg messages: ImageQnaMessage) {
    val state = uiState.value
    val oldMessages = if (state is UiState.Chatting) state.messages else listOf()
    _uiState.value = UiState.Chatting(oldMessages + messages, isLoading)
  }

  companion object {
    private const val TAG = "BaseQnAChatController"
  }
}
