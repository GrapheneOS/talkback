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
package com.google.android.accessibility.braille.translate.liblouis;

import static com.google.android.accessibility.utils.BuildVersionUtils.isRobolectric;

import android.content.Context;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import com.google.android.accessibility.braille.translate.R;
import com.google.android.accessibility.braille.translate.TableLoader;
import com.google.android.accessibility.utils.BuildVersionUtils;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** A singleton class that loads LibLouis tables to device storage. */
public class LiblouisLoader implements TableLoader {
  @SuppressWarnings("NonFinalStaticField")
  private static LiblouisLoader instance;

  private volatile FileState dataFileState = FileState.FILES_NOT_EXTRACTED;
  private final WeakReference<Context> context;
  private final ExecutorService ioExecutor;
  private final File tablesDir;

  private LiblouisLoader(Context context) {
    this.context = new WeakReference<>(context);
    this.ioExecutor = Executors.newSingleThreadExecutor();
    // Extract tables to device storage so we can read tables before device is unlocked after
    // reboot.
    if (BuildVersionUtils.isAtLeastN()) {
      context = ContextCompat.createDeviceProtectedStorageContext(context);
    }
    File customTablesDir = context.getExternalFilesDir(/* type= */ null);
    File customTablesSubDir = new File(customTablesDir, "/liblouis/tables");
    File[] files = customTablesSubDir.listFiles();
    if (files != null && files.length != 0) {
      // For external developer to use custom tables.
      tablesDir = customTablesDir;
    } else {
      tablesDir = context.getDir("translator", Context.MODE_PRIVATE);
      ensureDataFiles();
    }
    LouisTranslation.setTablesDir(tablesDir.getPath());
  }

  public static LiblouisLoader getInstance(Context context) {
    if (instance == null) {
      instance = new LiblouisLoader(context);
    }
    return instance;
  }

  @Override
  public void ensureDataFiles() {
    if (dataFileState != FileState.FILES_NOT_EXTRACTED) {
      return;
    }
    if (isRobolectric()) {
      extractFiles();
    } else {
      ioExecutor.execute(this::extractFiles);
    }
  }

  @Override
  public boolean isExtracted() {
    return dataFileState == FileState.FILES_EXTRACTED;
  }

  private void extractFiles() {
    boolean loaded =
        TranslateUtils.extractTables(
            context.get().getResources(), R.raw.translationtables, tablesDir);
    if (loaded) {
      dataFileState = FileState.FILES_EXTRACTED;
    } else {
      dataFileState = FileState.FILES_ERROR;
    }
  }

  @VisibleForTesting
  void reset() {
    instance = null;
  }
}
