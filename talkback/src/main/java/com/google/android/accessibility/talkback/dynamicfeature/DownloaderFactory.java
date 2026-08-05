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

package com.google.android.accessibility.talkback.dynamicfeature;

import android.app.Application;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;

/** A factory class to create a {@link Downloader}. */
public class DownloaderFactory {

  private DownloaderFactory() {}

  public static Downloader create(Application application) {
    return FeatureFlagReader.enableMdd(application)
        ? MddDownloader.getInstance(application)
        : SplitApkDownloader.getInstance(application);
  }

  @Nullable
  public static Downloader legacy(Application application) {
    return FeatureFlagReader.enableMdd(application)
        ? SplitApkDownloader.getInstance(application)
        : null;
  }
}
