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

package com.google.android.accessibility.talkback.actor.gemini.screenqa

import com.google.android.accessibility.gemineye.api.AccessibilityTree
import com.google.android.accessibility.gemineye.screenoverview.json.ScreenOverview
import com.google.android.accessibility.gemineye.screenoverview.json.ScreenQueryAnswer
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason

/** A response from Gemini for a [GeminiCommand] request */
sealed interface OverviewResponse {
  /** A successful response to a [GeminiCommand.ScreenOverview] request */
  data class Success(
    val overview: ScreenOverview,
    val a11yTree: AccessibilityTree,
    val imageBytes: ByteArray,
  ) : OverviewResponse

  /** An answer to a user's ScreenQuery */
  data class QueryAnswer(
    val answer: ScreenQueryAnswer,
    val a11yTree: AccessibilityTree,
    val imageBytes: ByteArray,
  ) : OverviewResponse

  /** An error occurred. */
  data class Error(val errorReason: ErrorReason?, val finishReason: FinishReason?) :
    OverviewResponse
}
