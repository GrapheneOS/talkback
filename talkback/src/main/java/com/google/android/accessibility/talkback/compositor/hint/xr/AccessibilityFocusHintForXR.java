/*
 * Copyright (C) 2023 Google Inc.
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
package com.google.android.accessibility.talkback.compositor.hint.xr;

import android.content.Context;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.compositor.hint.AccessibilityFocusHint;

/**
 * Provides accessibility focused hints for feedback. The usage hint appends clickable hint,
 * long-clickable hint and node role hint for the accessibility focused node.
 */
public class AccessibilityFocusHintForXR extends AccessibilityFocusHint {

  private static final String TAG = "AccessibilityFocusHintForXR";

  public AccessibilityFocusHintForXR(Context context, GlobalVariables globalVariables) {
    super(context, globalVariables, new NodeRoleHintForXR(context, globalVariables));
  }
}
