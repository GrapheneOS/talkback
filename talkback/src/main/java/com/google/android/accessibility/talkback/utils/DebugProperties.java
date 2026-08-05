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
package com.google.android.accessibility.talkback.utils;

import android.content.Context;
import android.os.Build;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads programmer-provided properties on non-prod builds.
 *
 * <p>To use: create a Properties file called debug.properties on the host machine that conforms to
 * the Java Properties spec, then adb push the file to the location
 * /data/data/TALKBACK_PACKAGE/files/debug.properties, where TALKBACK_PACKAGE is the package name of
 * the TalkBack app.
 */
public class DebugProperties {

  private static final String ENABLE_TREEDEBUG_SCREENSHOT = "enable_treedebug_screenshot";

  public static boolean includeScreenshotWithTreeDebug(Context context) {
    String valueAsString =
        getDebugProperties(context).getProperty(ENABLE_TREEDEBUG_SCREENSHOT, String.valueOf(false));
    return Boolean.parseBoolean(valueAsString);
  }

  private static Properties getDebugProperties(Context context) {
    Properties properties = new Properties();
    if ("eng".equals(Build.TYPE) || "userdebug".equals(Build.TYPE)) {
      File file = new File(context.getFilesDir(), "debug.properties");
      if (file.exists()) {
        try {
          FileInputStream fos = new FileInputStream(file);
          properties.load(fos);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    return properties;
  }
}
