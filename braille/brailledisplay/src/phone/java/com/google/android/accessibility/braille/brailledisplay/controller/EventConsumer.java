/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.android.accessibility.braille.brailledisplay.controller;

import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;

/** Digests events from screen reader and phone */
interface EventConsumer {

  /** Called when the component is activated. */
  void onActivate();

  /** Called when the component is deactivated. */
  void onDeactivate();

  /** Called when an accessibility event occurs. */
  void onAccessibilityEvent(AccessibilityEvent event);

  /**
   * Called when a mapped braille input event occurs.
   *
   * @param event The {@link BrailleInputEvent} that occurred.
   * @return {@code true} if the event was handled, {@code false} otherwise.
   */
  boolean onMappedInputEvent(BrailleInputEvent event);

  /** Called when the value of a reading control changes. */
  default void onReadingControlValueChanged() {}
}
