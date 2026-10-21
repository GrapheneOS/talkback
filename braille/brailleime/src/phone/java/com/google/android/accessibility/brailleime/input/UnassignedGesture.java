/*
 * Copyright 2025 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.brailleime.input;

import android.content.res.Resources;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.brailleime.R;
import java.util.Objects;

/** A gesture that is not assigned to any action. */
public class UnassignedGesture implements Gesture {
  private static final String ID = "unassigned";

  @Nullable
  @Override
  public Swipe getSwipe() {
    return null;
  }

  @Nullable
  @Override
  public BrailleCharacter getHeldDots() {
    return null;
  }

  @Nullable
  @Override
  public Gesture mirrorDots() {
    return null;
  }

  @Override
  public String getDescription(Resources resources) {
    return resources.getString(R.string.bk_gesture_unassigned);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof UnassignedGesture that)) {
      return false;
    }
    return that.getId().equals(getId());
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(getId());
  }
}
