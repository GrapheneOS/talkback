/*
 * Copyright 2021 Google Inc.
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

package com.google.android.accessibility.braille.interfaces;

import com.google.auto.value.AutoValue;
import java.nio.ByteBuffer;

/** Allows BrailleIme to signal to BrailleDisplay. */
public interface BrailleDisplayForBrailleIme {
  /** Tells when braille keyboard visibility changed. */
  void onImeVisibilityChanged(boolean visible);

  /** Tells BrailleDisplay to show the result on a braille display. */
  void showOnDisplay(ResultForDisplay result);

  /** Whether a physical braille display is connected and not suspended. */
  boolean isBrailleDisplayConnectedAndNotSuspended();

  /** Suspends braille display so it will not render dots and receive inputs. */
  void suspendInFavorOfBrailleKeyboard();

  /** Results returns to BrailleDisplay. */
  @AutoValue
  abstract class ResultForDisplay {

    /** Represents the text in the edit field that is currently displayed on the screen. */
    public abstract CharSequence onScreenText();

    /** Represents the text selection range in the edit field. */
    public abstract SelectionRange textSelection();

    /** Represents the holdings that yet to submit. */
    public abstract HoldingsInfo holdingsInfo();

    /** Indicates if the text in the edit field is displayed across multiple lines. */
    public abstract boolean isMultiLine();

    /** Represents the hints of the edit field. */
    public abstract String hint();

    /** Represents the action of the text in the edit field. */
    public abstract String action();

    /** Determines if the password should be displayed visibly. */
    public abstract boolean textMasked();

    /** Indicates whether the result is a re-translation. */
    public abstract boolean retranslate();

    public static ResultForDisplay.Builder builder() {
      return new AutoValue_BrailleDisplayForBrailleIme_ResultForDisplay.Builder()
          .setOnScreenText("")
          .setAction("")
          .setHint("")
          .setTextSelection(new SelectionRange(0, 0))
          .setHoldingsInfo(HoldingsInfo.create(ByteBuffer.wrap(new byte[] {}), -1))
          .setIsMultiLine(false)
          .setTextMasked(false)
          .setRetranslate(false);
    }

    /** Builder for result to braille display. */
    @AutoValue.Builder
    public abstract static class Builder {
      public abstract Builder setOnScreenText(CharSequence onScreenText);

      public abstract Builder setTextSelection(SelectionRange range);

      public abstract Builder setHoldingsInfo(HoldingsInfo holdingsInfo);

      public abstract Builder setIsMultiLine(boolean isMultiLine);

      public abstract Builder setHint(String hint);

      public abstract Builder setAction(String action);

      public abstract Builder setTextMasked(boolean textMasked);

      public abstract Builder setRetranslate(boolean retranslate);

      public abstract ResultForDisplay build();
    }

    /** Information of holdings. */
    @AutoValue
    public abstract static class HoldingsInfo {

      public abstract ByteBuffer holdings();

      public abstract int position();

      public static HoldingsInfo create(ByteBuffer holdings, int holdingsPosition) {
        return new AutoValue_BrailleDisplayForBrailleIme_ResultForDisplay_HoldingsInfo(
            holdings, holdingsPosition);
      }
    }
  }
}
