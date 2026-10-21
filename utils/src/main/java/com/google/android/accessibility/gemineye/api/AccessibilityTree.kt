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

import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.protobuf.ByteString

/**
 * Converts an AccessibilityWindowInfoCompat into a proto or other serialized format to send over
 * the wire, and then also lets us retrieve the original AccessibilityWindowInfoCompat from the
 * serialized ids.
 *
 * This allows the server to examine the a11y tree, and return nodeIds to us that link up to our
 * live AccessibilityNodeInfo tree in the app.
 */
interface AccessibilityTree {
  /** Finds a node by its ID. */
  fun findNodeById(nodeId: NodeId): AccessibilityNodeInfoCompat?

  /** Serializes the tree to a protobuf. */
  fun serialize(): ByteString
}
