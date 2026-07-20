/*
 * Copyright (C) 2021 Google Inc.
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

package com.google.android.accessibility.talkback.imagecaption;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

/**
 * A stub {@link CaptionRequest} for OCR (optical character recognition). OCR through Google ML Kit
 * has been removed to avoid a Google Play Services dependency, so this request performs no work.
 */
public class CharacterCaptionRequest extends CaptionRequest {

  private final Bitmap screenCapture;

  /** Stub constructor. This object takes ownership of node; caller should not recycle. */
  public CharacterCaptionRequest(
      int requestId,
      AccessibilityService service,
      AccessibilityNodeInfoCompat node,
      Bitmap screenCapture,
      @NonNull OnFinishListener onFinishListener,
      @NonNull OnErrorListener onErrorListener,
      boolean isUserRequested) {
    super(requestId, node, onFinishListener, onErrorListener, isUserRequested);
    this.screenCapture = screenCapture;
  }

  /** Stub: OCR support has been removed, so this performs no work. */
  @Override
  public void perform() {}
}
