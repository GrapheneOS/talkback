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

package com.google.android.accessibility.talkback.actor.gemini.ui.screenqa

import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.R
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason
import com.google.android.accessibility.talkback.actor.gemini.screenqa.OverviewResponse

/** Returns a nice user-facing message for the given error */
@StringRes
fun getErrorMessage(error: OverviewResponse.Error): Int {
  if (error.finishReason != null) {
    return when (error.finishReason) {
      FinishReason.ERROR_BLOCKED -> R.string.gemini_block_message
      FinishReason.ERROR_PARSING_RESULT -> R.string.gemini_error_parsing_result
      FinishReason.ERROR_RESPONSE -> R.string.gemini_error_message
      else -> R.string.gemini_error_message
    }
  } else if (error.errorReason != null) {
    return when (error.errorReason) {
      ErrorReason.DISABLED -> R.string.summary_pref_gemini_support_disabled
      ErrorReason.NETWORK_ERROR -> R.string.gemini_network_error
      ErrorReason.NO_IMAGE,
      GeminiActor.ErrorReason.BITMAP_COMPRESSION_FAIL -> R.string.gemini_screenshot_unavailable
      else -> R.string.gemini_error_message
    }
  }

  return R.string.gemini_error_message
}
