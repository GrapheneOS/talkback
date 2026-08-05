/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

/** Utility class to access {@link android.provider.Settings}. */
public class SettingsUtils {

  /** Value of hidden constant {@code android.provider.Settings.Secure.USER_SETUP_COMPLETE} */
  public static final String USER_SETUP_COMPLETE = "user_setup_complete";

  /**
   * Value of hidden constant {@code
   * android.provider.Settings.Global.ACCESSIBILITY_VIBRATION_WATCH_ENABLED}
   */
  public static final String VIBRATION_WATCH_ENABLED = "a11y_vibration_watch_enabled";

  /** Tethered Configuration state. */
  public static final String TETHER_CONFIG_STATE = "tethered_config_state";

  /** Tethered configuration state is unknown. */
  public static final int TETHERED_CONFIG_UNKNOWN = 0;

  /** Device is set in tethered mode. */
  public static final int TETHERED_CONFIG_TETHERED = 2;

  /** Do-not-disturb, DND, state. */
  private static final String ZEN_MODE = "zen_mode";

  public static boolean allowLinksOutOfSettings(Context context) {
    // Do not allow access to web during setup. REFERTO affects android M-O.
    return 1 == Settings.Secure.getInt(context.getContentResolver(), USER_SETUP_COMPLETE, 0);
  }

  /**
   * Returns whether a specific accessibility service is enabled.
   *
   * @param context The parent context.
   * @param packageName The package name of the accessibility service.
   * @return {@code true} of the service is enabled.
   */
  public static boolean isAccessibilityServiceEnabled(Context context, String packageName) {
    final ContentResolver resolver = context.getContentResolver();
    final String enabledServices =
        Settings.Secure.getString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

    return enabledServices.contains(packageName);
  }

  /**
   * Returns flag whether animation setting is definitely disabled. If no setting found, returns
   * false.
   */
  public static boolean isAnimationDisabled(Context context) {
    return FeatureSupport.supportsUserDisablingOfGlobalAnimations()
        && (0 == getGlobalFloat(context, Settings.Global.WINDOW_ANIMATION_SCALE))
        && (0 == getGlobalFloat(context, Settings.Global.TRANSITION_ANIMATION_SCALE))
        && (0 == getGlobalFloat(context, Settings.Global.ANIMATOR_DURATION_SCALE));
  }

  /**
   * Requests system settings writing permission if the parent context needs.
   *
   * @param context The parent context
   * @return {@code true} has the permission; {@code false} need to request the permission
   */
  public static boolean requestWriteSettingsPermission(Context context) {
    boolean hasWritePermission = Settings.System.canWrite(context);
    if (hasWritePermission) {
      return true;
    }
    // Starting in M, we need the user to manually allow the app to modify system settings.
    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
    intent.setData(Uri.parse("package:" + context.getPackageName()));
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
    return false;
  }

  /** Works only when the caller is a system app. */
  public static boolean isVibrationWatchEnabled(Context context) {
    return Settings.Global.getInt(context.getContentResolver(), VIBRATION_WATCH_ENABLED, 0) != 0;
  }

  /** Returns whether the device is in standalone or restricted connectivity mode */
  public static boolean isStandaloneOrRestricted(Context context) {
    return !isConfigFullyTethered(getTetherConfiguration(context));
  }

  /** Returns whether the config is fully tethered or not. */
  public static boolean isConfigFullyTethered(int config) {
    return config == TETHERED_CONFIG_TETHERED || config == TETHERED_CONFIG_UNKNOWN;
  }

  /** Gets current tether configuration value. */
  public static int getTetherConfiguration(Context context) {
    return Settings.Global.getInt(
        context.getContentResolver(), TETHER_CONFIG_STATE, TETHERED_CONFIG_UNKNOWN);
  }

  /** Returns device do-not-disturb, DND, state. */
  public static int getDoNotDisturbState(Context context) {
    return Settings.Global.getInt(context.getContentResolver(), ZEN_MODE, 0);
  }

  /**
   * Returns whether touch exploration is enabled. This is more reliable than {@code
   * AccessibilityManager.isTouchExplorationEnabled()} because it updates atomically.
   */
  public static boolean isTouchExplorationEnabled(ContentResolver resolver) {
    return Settings.Secure.getInt(resolver, Settings.Secure.TOUCH_EXPLORATION_ENABLED, 0) == 1;
  }

  /** Returns value of constants in Settings.Global. */
  private static float getGlobalFloat(Context context, String constantName) {
    return Settings.Global.getFloat(context.getContentResolver(), constantName, -1);
  }
}
