/*
 * Copyright (C) 2025 Google Inc.
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

package com.google.android.accessibility.talkback.keyboard;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;

/** Helper class for KeyComboManager. */
public class KeyComboManagerHelper {

  /**
   * Chrome role for list box. The role string should come from `ToString(ax::mojom::Role role)` at
   * ui/accessibility/ax_enum_util.cc in the Chromium repo.
   * https://source.chromium.org/chromium/chromium/src/+/main:ui/accessibility/ax_enum_util.cc?q=%22ToString(ax::mojom::Role%20role)%22%20f:ui%2Faccessibility%2Fax_enum_util.cc
   */
  private static final String CHROME_ROLE_LIST_BOX = "listBox";

  /**
   * Chrome role for menu bar. The role string should come from `ToString(ax::mojom::Role role)` at
   * ui/accessibility/ax_enum_util.cc in the Chromium repo.
   * https://source.chromium.org/chromium/chromium/src/+/main:ui/accessibility/ax_enum_util.cc?q=%22ToString(ax::mojom::Role%20role)%22%20f:ui%2Faccessibility%2Fax_enum_util.cc
   */
  private static final String CHROME_ROLE_MENU_BAR = "menuBar";

  /** Returns true if smart browse mode is enabled. */
  public static boolean isSmartBrowseModeEnabled(Context context) {
    SharedPreferences sharedPreferences = SharedPreferencesUtils.getSharedPreferences(context);
    return FeatureFlagReader.enableSmartBrowseMode(context)
        && sharedPreferences.getBoolean(
            context.getString(R.string.pref_smart_browse_mode_key),
            context.getResources().getBoolean(R.bool.pref_smart_browse_mode_default));
  }

  /**
   * Returns true if browse mode should be turned off.
   *
   * @param sourceNode The source node of the accessibility focused event or input focused event.
   * @param inputFocusNode The input focus node if the source node came from the accessibility
   *     focused event.
   */
  public static boolean shouldTurnOffBrowseMode(
      AccessibilityNodeInfoCompat sourceNode,
      @Nullable AccessibilityNodeInfoCompat inputFocusedNode) {
    // Turn off browse mode if the input focus is still in an editable one. In this case, users
    // should be able to keep typing, so browse mode should be turned off.
    if (inputFocusedNode != null && inputFocusedNode.isFocused() && inputFocusedNode.isEditable()) {
      return true;
    }

    return shouldTurnOffBrowseModeHelper(sourceNode);
  }

  /**
   * Returns true if browse mode should be turned off.
   *
   * @param sourceNode The source node of the accessibility focused event or input focused event.
   */
  public static boolean shouldTurnOffBrowseMode(AccessibilityNodeInfoCompat sourceNode) {
    return shouldTurnOffBrowseModeHelper(sourceNode);
  }

  private static boolean shouldTurnOffBrowseModeHelper(AccessibilityNodeInfoCompat sourceNode) {
    // Turn off browse mode if the focused node is editable so that the user can start typing.
    if (AccessibilityNodeInfoUtils.isSelfOrAncestorEditable(sourceNode)
        || AccessibilityNodeInfoUtils.isSelfOrAncestorRoleEditText(sourceNode)) {
      return true;
    }

    if (AccessibilityNodeInfoUtils.isInteractableWithArrowKeys(sourceNode)) {
      return true;
    }

    // Turn off browse mode if the focused node is in a list view of a list box or a menu bar so
    // that the user can use arrow keys to navigate the list view.
    if (AccessibilityNodeInfoUtils.isSelfOrAncestorWithChromeRole(sourceNode, CHROME_ROLE_LIST_BOX)
        || AccessibilityNodeInfoUtils.isSelfOrAncestorWithChromeRole(
            sourceNode, CHROME_ROLE_MENU_BAR)) {
      return true;
    }

    return false;
  }

  private KeyComboManagerHelper() {}
}
