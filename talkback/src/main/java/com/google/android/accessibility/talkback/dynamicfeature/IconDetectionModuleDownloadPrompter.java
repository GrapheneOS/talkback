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

package com.google.android.accessibility.talkback.dynamicfeature;

import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.ICON_LABEL;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.DownloadDialogResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.ImageCaptionPreferenceKeys;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.UninstallDialogResources;
import com.google.common.collect.ImmutableList;

/**
 * Shows a confirmation dialog to guide users through the download of the icon detection module
 * dynamically.
 */
public class IconDetectionModuleDownloadPrompter extends ModuleDownloadPrompter {

  /**
   * Creates a {@link ModuleDownloadPrompter} to handle the process of icon detection download and
   * uninstallation.
   */
  public IconDetectionModuleDownloadPrompter(
      Context context, Downloader downloader, @Nullable Downloader downloaderLegacy) {
    super(
        context,
        downloader,
        downloaderLegacy,
        ImmutableList.of(ICON_LABEL),
        ImageCaptionPreferenceKeys.ICON_DETECTION,
        DownloadDialogResources.ICON_DETECTION,
        UninstallDialogResources.ICON_DETECTION);
  }

  @Override
  public int getDownloadSuccessfulHint() {
    return R.string.download_icon_detection_successful_hint;
  }

  @Override
  public int getDownloadingHint() {
    return R.string.downloading_icon_detection_hint;
  }

  @Override
  protected boolean initModule() {
    return returnFeedback(Feedback.initializeIconDetection());
  }
}
