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
import android.content.Context;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType;

/** Downloads the dynamic feature via {@link MddManager}. */
public class MddDownloader implements Downloader {

  private MddDownloader() {}

  public static MddDownloader getInstance(Application unused) {
    return new MddDownloader();
  }

  @Override
  public String getModuleName(CaptionType captionType) {
    return "";
  }

  @Override
  public void download(Context context, String... name) {}

  @Override
  public void uninstall(Context context, String... name) {}

  @Override
  public boolean isDownloading(String name) {
    return false;
  }

  @Override
  public boolean isInstalled(String name) {
    return false;
  }

  @Override
  public void registerListener(DownloadStateUpdateListener listener) {}

  @Override
  public void unregisterListener(DownloadStateUpdateListener listener) {}

  @Override
  public DownloadStatus getDownloadStatus(String name) {
    return null;
  }

  @Override
  public void updateDownloadStatus(String name, DownloadStatus status) {}

  @Override
  public void updateAllDownloadStatus() {}

  @Override
  public boolean install(Context context, CaptionType captionType) {
    return false;
  }

  @Override
  public void initialize(Context context) {}
}
