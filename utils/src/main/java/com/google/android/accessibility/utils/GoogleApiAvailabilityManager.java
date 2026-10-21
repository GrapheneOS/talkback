/*
 * Copyright (C) 2025 Google Inc.
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

import android.content.Context;
import androidx.annotation.VisibleForTesting;

/**
 * A singleton class for verifying Google Play services availability
 * Stubbed implementation: always reports Google Play services as unavailable.
 */
public class GoogleApiAvailabilityManager {
  // Match com.google.android.gms.common.ConnectionResult.SERVICE_MISSING
  public static final int SERVICE_MISSING = 1;
  @SuppressWarnings("NonFinalStaticField")
  private static GoogleApiAvailabilityManager instance;

  public static GoogleApiAvailabilityManager getInstance() {
    if (instance == null) {
      instance = new GoogleApiAvailabilityManager();
    }
    return instance;
  }

  /**
   * Sets a fake GoogleApiAvailable {@link
   * com.google.android.gms.common.testing.FakeGoogleApiAvailability} for test. This should be
   * invoked before the first APIs are created.
   */
  @VisibleForTesting
  public static void initializeForTest(Object ignored) {
    instance = new GoogleApiAvailabilityManager();
  }

  @VisibleForTesting
  public static void stopInstanceForTest() {
    instance = null;
  }

  private GoogleApiAvailabilityManager() {}

  /**
   * Always reports Google Play services as unavailable.
   */
  public int isGooglePlayServicesAvailable(Context context, int minApkVersion) {
    return SERVICE_MISSING;
  }
}
