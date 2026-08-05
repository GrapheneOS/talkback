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

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.accessibility.gemineye.screenoverview.json.UiElement
import com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.ImageQnaMessage
import kotlinx.coroutines.flow.StateFlow

/**
 * Controller for the business logic of the chat UI, using the MVI architecture pattern.
 *
 * You can override this to control what model endpoints and prompts are used, for instance Image
 * Q&A vs Screen Q&A.
 */
interface QnAChatController {
  /** Called with a new response from the model. */
  fun onImageQnaResponse(message: String)

  /** Current state of the UI */
  val uiState: StateFlow<UiState>

  /** The user has entered a new command via voice or typing */
  fun onNewCommand(command: String, imageBytesForQna: ByteArray, isVoiceInput: Boolean)

  /** The user tried to issue a voice command, but the speech-to-text failed */
  fun onVoiceError(error: Int)
}

/** UI state of the chat dialog */
sealed class UiState {
  /** If true, shows a loading indicator */
  abstract val isLoading: Boolean

  /** They have clicked close: close the dialog. */
  data object FinishedAndExit : UiState() {
    override val isLoading = false
  }

  /** The main state: displays a list of back-and-forth messages */
  data class Chatting(val messages: List<ImageQnaMessage>, override val isLoading: Boolean) :
    UiState()
}

/** Actions the user can take in the UI that will cause the state to change. */
sealed interface OverviewIntent {
  /** User clicked the close button: close the dialog. */
  data object CloseDialog : OverviewIntent

  /** The user activated or focused one of the available Actions */
  data class ChooseAction(
    val uiElement: UiElement,
    val action: AccessibilityNodeInfoCompat.AccessibilityActionCompat,
  ) : OverviewIntent
}
