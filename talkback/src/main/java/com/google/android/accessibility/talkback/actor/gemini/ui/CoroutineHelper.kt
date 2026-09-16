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

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.coroutineScope
import com.google.android.accessibility.utils.Consumer
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Helps using Kotlin coroutines from Java code */
object CoroutineHelper {

  /**
   * Allows you to call collect() on a StateFlow from Java code
   *
   * @param collector called whenever flow emits a new value
   */
  fun <T : Any> collect(lifecycle: Lifecycle, flow: StateFlow<T>, collector: Consumer<T>) {
    lifecycle.coroutineScope.launch { flow.collect { collector.accept(it) } }
  }
}
