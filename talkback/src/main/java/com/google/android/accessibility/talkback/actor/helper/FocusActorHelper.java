/*
 * Copyright (C) 2024 Google Inc.
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
package com.google.android.accessibility.talkback.actor.helper;

import static com.google.android.accessibility.utils.monitor.InputModeTracker.INPUT_MODE_KEYBOARD;

import android.content.Context;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.PackageNameProvider;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenState;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManagerHelper;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;

/** A helper class for handling common logic in the family of FocusActor. */
public final class FocusActorHelper {

  private static final String LOG_TAG = "FocusActorHelper";

  /**
   * Checks whether the feedback for the focused node should be muted.
   *
   * @param nodeToFocus the node to check
   * @param screenState the screen state for current active window
   * @return {@code true} if the focused node should be muted.
   */
  public static boolean shouldMuteFeedbackForFocusedNode(
      AccessibilityNodeInfoCompat nodeToFocus, ScreenState screenState) {
    // We mute the feedback of focused node only when it wakes up on the Home Screen and not on CB.
    return FormFactorUtils.isAndroidWear()
        && screenState.isInterpretFirstTimeWhenWakeUp()
        && isHomeScreenShowing(screenState.getActiveWindow());
  }

  /**
   * Checks whether the node and the active window are not for cell broadcast.
   *
   * @param nodeToFocus the node to check
   * @param windowInfo the active window info to check
   * @return {@code true} if the node and the active window are not for cell broadcast.
   */
  private static boolean isNotCellBroadcastFocusOnScreen(
      @Nullable AccessibilityNodeInfoCompat nodeToFocus,
      @Nullable AccessibilityWindowInfo windowInfo) {
    return isNotCellBroadcastPackageNode(nodeToFocus)
        && isNotCellBroadcastPackageNode(AccessibilityWindowInfoUtils.getRootCompat(windowInfo));
  }

  private static boolean isNotCellBroadcastPackageNode(@Nullable AccessibilityNodeInfoCompat node) {
    String cellBroadcastPackageName = PackageNameProvider.getCellBroadcastReceiverPackageName();
    if (node == null || cellBroadcastPackageName == null) {
      return true;
    }

    CharSequence packageName = node.getPackageName();
    if (packageName == null) {
      return true;
    }

    return !TextUtils.equals(packageName.toString(), cellBroadcastPackageName);
  }

  // modified based on
  // cts/tests/accessibilityservice/src/android/accessibilityservice/cts/utils/ActivityLaunchUtils.java
  private static boolean isHomeScreenShowing(@Nullable AccessibilityWindowInfo windowInfo) {

    if (windowInfo == null) {
      LogUtils.v(LOG_TAG, "The active window is null so it is not the Home screen.");
      return false;
    }

    List<ResolveInfo> homeScreenResolveInfoList = PackageNameProvider.getHomeActivityResolvedInfo();

    // Look for an active focused window with a package name that matches
    // the default home screen.
    if (!FormFactorUtils.isAndroidAuto()) {
      // Auto does not set its home screen app as active+focused, so only non-auto
      // devices enforce that the home screen is active+focused.
      if (!windowInfo.isActive() || !windowInfo.isFocused()) {
        LogUtils.v(LOG_TAG, "The window is not active or focused so it is not Home screen.");
        return false;
      }
    }

    final AccessibilityNodeInfo root = windowInfo.getRoot();
    if (root != null) {
      final CharSequence packageName = root.getPackageName();
      if (packageName != null) {
        for (ResolveInfo resolveInfo : homeScreenResolveInfoList) {
          if ((resolveInfo.activityInfo != null)
              && TextUtils.equals(packageName, resolveInfo.activityInfo.packageName)) {
            LogUtils.v(LOG_TAG, "The active window is the Home screen.");
            return true;
          }
        }
      }
    }

    LogUtils.v(LOG_TAG, "The active window is not the Home screen.");
    return false;
  }

  /** Whether the input focus should be synced to the accessibility focus. */
  public static boolean shouldSyncInputFocusToAccessibilityFocus(
      @NonNull Context context,
      @NonNull AccessibilityNodeInfoCompat node,
      @NonNull FocusActionInfo focusActionInfo) {
    if (!node.isFocusable() || node.isFocused()) {
      return false;
    }
    // On TV, we always keep input focus synchronized with the accessibility focus.
    if (FormFactorUtils.isAndroidTv()) {
      return true;
    }

    if (isKeyboardNavigation(focusActionInfo)) {
      if (FeatureFlagReader.enableSynchronizedFocusWithKeyboardNavigation(context)) {
        return true;
      } else if (KeyComboManagerHelper.isSmartBrowseModeEnabled(context)
          && KeyComboManagerHelper.shouldTurnOffBrowseMode(node)) {
        return true;
      }
    }
    // TODO: b/249453158 - Input focus should follow accessibility focus when the device is
    // connecting to an external keyboard without the soft keyboard being on screen.

    return false;
  }

  /** Whether to clear the input focus after setting the accessibility focus. */
  public static boolean shouldClearFocusAfterSetAccessibilityFocus(
      @NonNull Context context, @NonNull FocusActionInfo focusActionInfo) {
    return FeatureFlagReader.clearFocusAfterSetAccessibilityFocus(context)
        && isKeyboardNavigation(focusActionInfo)
        && BuildVersionUtils.isAtLeastBaklava()
        && !FormFactorUtils.isAndroidTv();
  }

  private static boolean isKeyboardNavigation(@NonNull FocusActionInfo focusActionInfo) {
    return focusActionInfo.navigationAction != null
        && focusActionInfo.navigationAction.inputMode == INPUT_MODE_KEYBOARD;
  }

  private FocusActorHelper() {}
}
