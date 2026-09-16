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

package com.google.android.accessibility.gemineye.api

/** A unique identifier that links to an accessibility node, as assigned by [AccessibilityTree]. */
data class NodeId(
  /** The unique ID of the node, as assigned by [AccessibilityTree] */
  val uniqueId: Int,
  /** The window ID the node belongs to, as assigned by [AccessibilityTree] */
  val windowId: Int,
)
