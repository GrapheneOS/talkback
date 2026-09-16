/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.accessibility.utils.output;

/**
 * Defines the contract for displaying text overlays on the screen, particularly for text-to-speech
 * feedback. This interface allows for different implementations of text overlays, such as simple
 * text displays or more complex visual presentations like bubble overlays, while ensuring a
 * consistent API for displaying and controlling the overlay's visibility.
 */
public interface TextToSpeechOverlay {
  void displayText(CharSequence text);

  void displayText(CharSequence text, int eventType);

  void hide();

  void show();

  void onConfigurationChanged();
}
