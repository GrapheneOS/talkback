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

package com.google.android.accessibility.talkback.actor.gemini

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.gemineye.api.AccessibilityTree
import com.google.android.accessibility.gemineye.screenoverview.json.ChatHistoryItem
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse

/** Called back when the Gemini model has a response */
interface GeminiResponseCallback<T> {
  fun onResponse(finishReason: FinishReason, response: T)

  fun onError(errorReason: ErrorReason)
}

/** Different commands you can send Gemini */
sealed interface GeminiCommand {

  /**
   * A common request that can be used for any Gemini action.
   *
   * @param action The action to perform.
   * @param text The text to send to Gemini.
   * @param screenshot The screenshot to send to Gemini.
   * @param manualTrigger Whether the request is triggered by user manually.
   * @param listener The listener to call back when the Gemini response is received.
   */
  data class CommonRequest(
    val action: GeminiActor.Action,
    val text: String,
    val screenshot: ByteArray,
    val manualTrigger: Boolean,
    val listener: GeminiActor.GeminiResponseListener,
  ) : GeminiCommand

  /** Generates a summary of the screen along with top actions and images */
  data class ScreenOverview(
    val screenshot: ByteArray,
    val focusedNode: AccessibilityNodeInfoCompat,
    val listener: GeminiResponseCallback<OverviewResponse>,
  ) : GeminiCommand

  data class ScreenQuery(
    val screenshot: ByteArray,
    val a11yTree: AccessibilityTree,
    val query: String,
    val chatHistory: List<ChatHistoryItem>?,
    val listener: GeminiResponseCallback<OverviewResponse>,
  ) : GeminiCommand
}
