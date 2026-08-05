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

package com.google.android.accessibility.utils;

import android.app.UiModeManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.VisibleForTesting;

/** Methods to return the form factor which TalkBack is running on. */
public class FormFactorUtils {

  private static FormFactorUtils instance;

  private static FormFactorUtils getInstance() {
    if (instance == null) {
      throw new IllegalStateException(
          "We need to initialize FormFactorUtils before checking the type of form factor.");
    }
    return instance;
  }

  public static void initialize(Context context) {
    instance = new FormFactorUtils(context);
  }

  private final boolean isAndroidAuto;
  private final boolean isAndroidWear;
  private final boolean isAndroidTv;
  private final boolean isAndroidPc;
  private final boolean isAndroidXr;

  @VisibleForTesting
  FormFactorUtils(Context context) {
    isAndroidAuto = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    isAndroidWear = context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_WATCH);
    isAndroidTv = initIsAndroidTv(context);
    isAndroidPc =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
            && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_PC);
    // Remove the check for FEATURE_XR_IMMERSIVE once FEATURE_XR_API_SPATIAL is fully supported.
    // FEATURE_XR_IMMERSIVE is currently being deprecated in favor of FEATURE_XR_API_SPATIAL.
    isAndroidXr =
        context.getPackageManager().hasSystemFeature("android.software.xr.api.spatial")
            || context.getPackageManager().hasSystemFeature("android.software.xr.immersive");
  }

  /** Returns whether TB is running on Android Tv. */
  private static boolean initIsAndroidTv(Context context) {
    UiModeManager modeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
    return ((modeManager != null)
        && (modeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION));
  }

  public static boolean isAndroidAuto() {
    return getInstance().isAndroidAuto;
  }

  public static boolean isAndroidWear() {
    return getInstance().isAndroidWear;
  }

  public static boolean isAndroidTv() {
    return getInstance().isAndroidTv;
  }

  public static boolean isAndroidPc() {
    return getInstance().isAndroidPc;
  }

  public static boolean isAndroidXr() {
    return getInstance().isAndroidXr;
  }
}
