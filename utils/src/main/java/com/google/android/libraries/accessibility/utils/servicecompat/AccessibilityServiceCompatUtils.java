/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.android.libraries.accessibility.utils.servicecompat;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Collections;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utilities for interacting with an {@link AccessibilityService}. */
public final class AccessibilityServiceCompatUtils {

  private static final String TAG = "A11yServiceCompatUtils";

  private AccessibilityServiceCompatUtils() {}

  /** Returns the windows on the screen of the default display. */
  public static List<AccessibilityWindowInfo> getWindows(AccessibilityService service) {
    try {
      // Use try/catch to fix b/126910678
      return service.getWindows();
    } catch (SecurityException | ClassCastException | NullPointerException e) {
      // If build version is not isAtLeastN(), there is a chance of ClassCastException or
      // NullPointerException.
      LogUtils.e(TAG, e, "Exception occurred at AccessibilityService#getWindows");
      return Collections.emptyList();
    }
  }

  /** Returns the root node of the active window */
  public static @Nullable AccessibilityNodeInfoCompat getRootInActiveWindow(
      AccessibilityService service) {
    if (service == null) {
      return null;
    }

    AccessibilityNodeInfo root = service.getRootInActiveWindow();
    if (root == null) {
      return null;
    }
    return AccessibilityNodeInfoCompat.wrap(root);
  }

  /** Returns the package name of the active window */
  public static @Nullable String getActiveWindowPackageName(AccessibilityService service) {
    @Nullable AccessibilityNodeInfoCompat rootNode = getRootInActiveWindow(service);
    if (rootNode == null) {
      return null;
    }
    try {
      return (rootNode.getPackageName() == null) ? null : rootNode.getPackageName().toString();
    } finally {
      rootNode.recycle();
    }
  }
}
