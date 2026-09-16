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
import android.graphics.Rect
import android.util.Log
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.gemineye.api.NodeId
import com.google.android.accessibility.gemineye.screenoverview.json.ChatHistoryItem
import com.google.android.accessibility.gemineye.screenoverview.json.MessageType
import com.google.android.accessibility.talkback.Feedback
import com.google.android.accessibility.talkback.Feedback.GeminiRequest
import com.google.android.accessibility.talkback.Pipeline
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.Action
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse
import com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.ImageQnaMessage
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils
import com.google.android.accessibility.utils.Consumer
import com.google.android.accessibility.utils.Performance
import com.google.android.libraries.accessibility.utils.log.LogUtils

/** Controller for the Screen Q&A chatting UI. */
class ScreenQAChatController
@JvmOverloads
constructor(
  context: Context,
  pipeline: Pipeline.FeedbackReturner,
  action: Action,

  /** The initial response from the model. */
  private val overview: OverviewResponse.Success,

  /**
   * Checks a node to see if it can be focused. Returns true if the node can be focused, false
   * otherwise.
   *
   * This is used to double-check the [UiElement.potentiallyMatchingNodes] from the model, to make
   * sure we're not activating nodes that can't be focused.
   */
  private val shouldFocusNode: (AccessibilityNodeInfoCompat?) -> Boolean =
    AccessibilityNodeInfoUtils::shouldFocusNode,
) : BaseQnAChatController(context, pipeline, action) {

  init {
    addNewMessage(
      isLoading = false,
      ImageQnaMessage(ImageQnaChatAdapter.TYPE_MODEL_MESSAGE).apply {
        text = overview.overview.summary
        topImages = overview.overview.topImages
        uiElements = overview.overview.topUiElements
        onUiElementActionListener = Consumer<OverviewIntent> { onIntent(it) }
      },
    )
  }

  override fun onNewCommand(command: String, imageBytesForQna: ByteArray, isVoiceInput: Boolean) {
    val state = uiState.value
    if (state !is UiState.Chatting) {
      LogUtils.e(TAG, "Invalid state for onNewCommand: not in chatting state")
      return
    }

    val userMessage =
      ImageQnaMessage(ImageQnaChatAdapter.TYPE_USER_MESSAGE, command).apply {
        setVoiceInput(isVoiceInput)
      }
    addNewMessage(isLoading = true, userMessage)

    pipeline.returnFeedback(
      Performance.EVENT_ID_UNTRACKED,
      Feedback.Part.builder()
        .setGeminiRequest(
          GeminiRequest.builder()
            .setAction(GeminiRequest.Action.REQUEST_SCREEN_OVERVIEW)
            .setText(command)
            .setA11yTree(overview.a11yTree)
            .setImageBytes(overview.imageBytes)
            .setChatHistory(
              state.messages
                .filter { !it.text.isNullOrEmpty() }
                .map {
                  ChatHistoryItem(
                    it.text,
                    if (it.viewType == ImageQnaChatAdapter.TYPE_USER_MESSAGE) MessageType.USER
                    else MessageType.MODEL,
                  )
                }
            )
            .build()
        ),
    )
  }

  fun onScreenQueryAnswer(answer: OverviewResponse.QueryAnswer) {
    addNewMessage(
      isLoading = false,
      ImageQnaMessage(ImageQnaChatAdapter.TYPE_MODEL_MESSAGE).apply {
        text = answer.answer.answer
        uiElements = answer.answer.uiElements
        onUiElementActionListener = Consumer<OverviewIntent> { onIntent(it) }
      },
    )
  }

  @VisibleForTesting
  fun onIntent(intent: OverviewIntent) {
    when (intent) {
      is OverviewIntent.ChooseAction -> {
        val node = findFirstFocusableNode(intent.uiElement.potentiallyMatchingNodes)
        if (node == null) {
          LogUtils.e(TAG, "Failed to find node in tree %s", intent.uiElement)
          Toast.makeText(context, R.string.overviews_node_not_found, Toast.LENGTH_SHORT).show()
          return
        }

        // TODO: this should go through the pipeline
        node.performAction(intent.action.id)

        _uiState.value = UiState.FinishedAndExit
      }

      is OverviewIntent.CloseDialog -> {
        _uiState.value = UiState.FinishedAndExit
      }
    }
  }

  private fun findFirstFocusableNode(ids: List<NodeId>?): AccessibilityNodeInfoCompat? {
    ids?.forEach { id ->
      val node = overview.a11yTree.findNodeById(id)

      if (LogUtils.shouldLog(Log.VERBOSE)) {
        LogUtils.v(
          TAG,
          "Finding first focusable node in tree: id: %d bounds: %s description: %s node: %s",
          id,
          Rect().apply { node?.getBoundsInScreen(this) },
          node?.contentDescription,
          node,
        )
      }

      if (shouldFocusNode(node)) {
        return node
      }
    }

    return null
  }

  companion object {
    private const val TAG = "ScreenQnAChatController"
  }
}
