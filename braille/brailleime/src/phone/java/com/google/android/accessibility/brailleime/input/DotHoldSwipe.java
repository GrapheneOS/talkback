/*
 * Copyright 2023 Google Inc.
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

import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.getDotsDescription;

import android.content.res.Resources;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.brailleime.input.Swipe.Direction;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Describes a user-initiated swipe and dots hold on the touch screen.
 *
 * <p>Dot hold and swipe includes the braille dots hold, a swipe direction{@link Direction}, a touch
 * count which is the number of fingers that were touching the screen.
 */
public class DotHoldSwipe implements Gesture {
  private static final Pattern REGEX_PATTERN =
      Pattern.compile("hold_dot(?<dot>\\d+)_swipe_(?<direction>[A-Za-z]+)_(?<touchpoints>\\d+)");
  private final Swipe swipe;
  private final BrailleCharacter heldBrailleCharacter;

  public DotHoldSwipe(Swipe swipe, BrailleCharacter heldBrailleCharacter) {
    this.swipe = swipe;
    this.heldBrailleCharacter = heldBrailleCharacter;
  }

  public DotHoldSwipe(String id) {
    Matcher matcher = REGEX_PATTERN.matcher(id);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Invalid id");
    }
    this.swipe =
        new Swipe(
            Direction.valueOf(matcher.group("direction").toUpperCase(Locale.ENGLISH)),
            Integer.parseInt(matcher.group("touchpoints")));
    this.heldBrailleCharacter = new BrailleCharacter(matcher.group("dot"));
  }

  @Override
  public Swipe getSwipe() {
    return swipe;
  }

  @Override
  public BrailleCharacter getHeldDots() {
    return new BrailleCharacter(heldBrailleCharacter.toString());
  }

  @Override
  public Gesture mirrorDots() {
    return new DotHoldSwipe(swipe, heldBrailleCharacter.toMirror());
  }

  @Override
  public String getDescription(Resources resources) {
    if (getSwipe().getTouchCount() > 1) {
      return resources.getString(
          R.string.gesture_hold_and_swipe_multiple_finger,
          getDotsDescription(resources, getHeldDots()),
          getSwipe().getDirection().getDescription(resources),
          NumberFormat.getNumberInstance(Locale.getDefault()).format(getSwipe().getTouchCount()));
    }
    return resources.getString(
        R.string.gesture_hold_and_swipe_one_finger,
        getDotsDescription(resources, getHeldDots()),
        getSwipe().getDirection().getDescription(resources));
  }

  @Override
  public String toString() {
    return "DotHoldSwipe{hold="
        + heldBrailleCharacter
        + ", direction="
        + swipe.getDirection()
        + ", touchCount="
        + swipe.getTouchCount()
        + "}";
  }

  @Override
  public String getId() {
    return "hold_dot" + heldBrailleCharacter + "_" + swipe.getId();
  }

  @Override
  public int hashCode() {
    /*
     * Hashing function taken from an example in "Effective Java" page 38/39. The number 13 is
     * arbitrary, but choosing non-zero number to start decreases the number of collisions. 37
     * is used as it's an odd prime. If multiplication overflowed and the 37 was an even number,
     * it would be equivalent to bit shifting. The fact that 37 is prime is standard practice.
     */
    int result = 13;
    result = 37 * result + (swipe.getDirection() == null ? 0 : swipe.getDirection().hashCode());
    result = 37 * result + (heldBrailleCharacter == null ? 0 : heldBrailleCharacter.hashCode());
    result = 37 * result + swipe.getTouchCount();
    return result;
  }

  @Override
  public boolean equals(@Nullable Object obj) {
    if (!(obj instanceof DotHoldSwipe that)) {
      return false;
    }
    return swipe.equals(that.swipe) && heldBrailleCharacter.equals(that.heldBrailleCharacter);
  }
}
