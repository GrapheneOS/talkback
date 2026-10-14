/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.android.accessibility.talkback.actor.gemini;

import android.content.Context;

/** This class implements the configuration flags for Gemini requests. */
public final class GeminiConfiguration {

  private GeminiConfiguration() {}

  public static boolean isGeminiVoiceCommandEnabled(Context context) {
    return false;
  }

  static String getGeminiModel(Context context) {
    return "";
  }

  public static String getQnaSupportLocales(Context context) {
    return "";
  }

  static String getPrefixPrompt(Context context) {
    return "";
  }

  static String getSafetyThresholdHarassment(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  static String getSafetyThresholdHateSpeech(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  static String getSafetyThresholdSexuallyExplicit(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  static String getSafetyThresholdDangerousContent(Context context) {
    return "BLOCK_LOW_AND_ABOVE";
  }

  public static boolean isScreenQnaActionsEnabled(Context context) {
    return false;
  }

  public static boolean isServerSideGeminiImageCaptioningEnabled(Context context) {
    return false;
  }

  public static boolean isOnDeviceGeminiImageCaptioningEnabled(Context context) {
    return false;
  }

  public static boolean useAratea(Context context) {
    return false;
  }

  public static boolean imageQnaEnabled(Context context) {
    return false;
  }

  /**
   * If true, use the separate screen overview endpoint for rich screen overviews. If false, just
   * pipe the screenshot into the Image Q&A.
   */
  public static boolean screenOverviewEndpointEnabled(Context context) {
    return false;
  }

  public static boolean screenOverviewEnabled(Context context) {
    return false;
  }

  public static int imageQnaTypingSizeLimit(Context context) {
    return 1000;
  }

  public static int imageQnaQuestionSizeLimit(Context context) {
    return 10000;
  }

  public static boolean supportGeminiByFormFactor() {
    return false;
  }
}
