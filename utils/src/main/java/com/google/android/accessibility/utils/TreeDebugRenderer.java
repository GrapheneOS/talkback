/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.hardware.HardwareBuffer;
import android.os.Environment;
import android.view.Display;
import androidx.annotation.RequiresApi;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Captures a screenshot of the current screen, and then overlays the resulting bitmap with focus
 * indicator rectangles for every node that is accessibility-focusable, then writes the result to
 * file.
 */
@RequiresApi(api = 30)
public class TreeDebugRenderer {

  private static final String TAG = TreeDebugRenderer.class.getSimpleName();

  private static final String OUTPUT_FILE = "focus_indicators.png";

  private final List<AccessibilityNodeInfoCompat> nodes;
  private final AccessibilityService service;
  private final FocusIndicatorSpec indicatorSpec;

  private final ExecutorService executor;

  /** Holds focus indicator render parameters. */
  public static class FocusIndicatorSpec {
    private final int color;
    private final float strokeWidth;

    public FocusIndicatorSpec(int color, float strokeWidth) {
      this.color = color;
      this.strokeWidth = strokeWidth;
    }
  }

  public TreeDebugRenderer(
      AccessibilityService service,
      List<AccessibilityNodeInfoCompat> nodes,
      FocusIndicatorSpec spec) {
    this.service = service;
    this.nodes = nodes;
    this.indicatorSpec = spec;
    this.executor = Executors.newSingleThreadExecutor();
  }

  public void startRenderOperation() {
    service.takeScreenshot(Display.DEFAULT_DISPLAY, executor, new MyTakeScreenshotCallback());
  }

  private void onScreenshotArrived(ScreenshotResult screenshot) {
    Bitmap bitmapMutable;
    try (HardwareBuffer hardwareBuffer = screenshot.getHardwareBuffer()) {
      Bitmap bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.getColorSpace());
      bitmapMutable = bitmap.copy(Config.ARGB_8888, /* isMutable= */ true);
      bitmap.recycle();
    } catch (IllegalArgumentException e) {
      LogUtils.e(TAG, "screenshot could not be converted to bitmap", e);
      shutdown();
      return;
    }

    File outputFolder =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
    File outputFile = new File(outputFolder, OUTPUT_FILE);

    executor.execute(
        () -> {
          renderFocusIndicatorsAndWriteToFile(bitmapMutable, outputFile);
          bitmapMutable.recycle();
        });
  }

  private void shutdown() {
    executor.shutdown();
  }

  private class MyTakeScreenshotCallback implements TakeScreenshotCallback {

    @Override
    public void onFailure(int errorCode) {
      LogUtils.d(TAG, "screenshot failure " + errorCode);
      shutdown();
    }

    @Override
    public void onSuccess(ScreenshotResult screenshot) {
      TreeDebugRenderer.this.onScreenshotArrived(screenshot);
    }
  }

  private void renderFocusIndicatorsAndWriteToFile(Bitmap bitmapMutable, File outputFile) {
    Canvas canvas = new Canvas();
    canvas.setBitmap(bitmapMutable);
    Paint myPaint = new Paint();
    myPaint.setColor(indicatorSpec.color);
    myPaint.setStrokeWidth(indicatorSpec.strokeWidth);
    myPaint.setStyle(Paint.Style.STROKE);
    for (AccessibilityNodeInfoCompat node : nodes) {
      Rect rect = new Rect();
      node.getBoundsInScreen(rect);
      canvas.drawRect(rect.left, rect.top, rect.right, rect.bottom, myPaint);
    }

    try {
      writeBitmapToPngFile(bitmapMutable, outputFile);
      LogUtils.v(TAG, "Wrote bitmap to %s", outputFile.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
      LogUtils.e(TAG, "failed to write bitmap to file", e);
    }
    shutdown();
  }

  private static void writeBitmapToPngFile(Bitmap bitmap, File outputFile) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    bitmap.compress(CompressFormat.PNG, 0 /*ignored for PNG*/, bos);
    byte[] bitmapAsPngBytes = bos.toByteArray();
    try (FileOutputStream fos = new FileOutputStream(outputFile)) {
      fos.write(bitmapAsPngBytes);
    }
  }
}
