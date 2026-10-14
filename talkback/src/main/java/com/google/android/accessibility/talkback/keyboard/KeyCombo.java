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

package com.google.android.accessibility.talkback.keyboard;

import androidx.annotation.IntDef;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import com.google.common.collect.ImmutableSet;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * A class to represent a key combo. A key combo is a combination of modifiers and at most two
 * non-modifier keys (the prefix key and the key).
 *
 * <p>For example, a non-sequenced key combo consists of the modifiers and the key. Users can
 * perform a command that is mapped to the non-sequenced key combo by pressing the modifiers and the
 * key.
 *
 * <p>A sequenced key combo consists of the modifiers, the prefix key, and the key. Users first
 * press the modifiers and the prefix key to enter the sequence key mode, and then press the key to
 * perform a command that is mapped to the sequenced key combo.
 */
public final class KeyCombo {
  private static final String TAG = "KeyCombo";

  /** Defines the match result of a key combo. */
  @IntDef({NO_MATCH, PARTIAL_MATCH, EXACT_MATCH})
  @Retention(RetentionPolicy.SOURCE)
  public @interface MatchResult {}

  public static final int NO_MATCH = 0;
  public static final int PARTIAL_MATCH = 1;
  public static final int EXACT_MATCH = 2;

  public static final int KEY_EVENT_MODIFIER_MASK =
      KeyEvent.META_SHIFT_ON | KeyEvent.META_CTRL_ON | KeyEvent.META_ALT_ON | KeyEvent.META_META_ON;

  public static final ImmutableSet<Integer> SEQUENCE_KEY_COMBO_CODE_PREFIX_KEYS =
      ImmutableSet.of(KeyEvent.KEYCODE_R, KeyEvent.KEYCODE_O);

  /** Returns key combo code which is combination of modifiers and key code. */
  public static long computeKeyComboCode(int modifiers, int keyCode) {
    return computeKeyComboCode(modifiers, KeyComboModel.NO_PREFIX_KEY_CODE, keyCode);
  }

  /** Returns key combo code which is combination of modifiers, prefix key code, and key code. */
  public static long computeKeyComboCode(int modifiers, int prefixKeyCode, int keyCode) {
    return (((long) modifiers) << 32)
        + (((long) prefixKeyCode & 0xFFFF) << 16)
        + ((long) keyCode & 0xFFFF);
  }

  /** Returns modifier part of key combo code. */
  public static int extractModifiers(long keyComboCode) {
    return (int) (keyComboCode >>> 32);
  }

  /**
   * Returns the prefix key code part of key combo code, which does not include the modifier part
   * and the key code part.
   */
  public static int extractPrefixKeyCode(long keyComboCode) {
    return (int) (keyComboCode & 0xFFFF0000) >>> 16;
  }

  /** Returns the key code part of key combo code, which does not include the prefix key code. */
  public static int extractKeyCode(long keyComboCode) {
    return (int) (keyComboCode & 0x0000FFFF);
  }

  private final boolean triggerModifierUsed;

  /**
   * Long value is used to store the key combo. The first 32 bits are used to store the modifiers.
   * The last 32 bits are used to store the key code(s) as follows:
   *
   * <ul>
   *   <li>The first 16 bits: store the prefix key code if it is a sequenced key combo; empty
   *       otherwise.
   *   <li>The last 16 bits: store the key code.
   * </ul>
   */
  private final long keyComboCode;

  /**
   * Constructor for an empty KeyCombo instance.
   *
   * <p>The default value of `keyComboCode` is {@link KeyComboModel#KEY_COMBO_CODE_UNASSIGNED} to
   * indicate that the key combo is unassigned. The default value of `triggerModifierUsed` is true
   * for historical reasons; it is set to true to support {@link DefaultKeyComboModel} when
   * introducing {@link NewKeyComboModel}.
   */
  public KeyCombo() {
    triggerModifierUsed = true;
    keyComboCode = KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
  }

  public KeyCombo(long keyComboCode, boolean triggerModifierUsed) {
    this.triggerModifierUsed = triggerModifierUsed;
    this.keyComboCode = keyComboCode;
  }

  public KeyCombo(int modifiers, int keyCode, boolean triggerModifierUsed) {
    this(modifiers, KeyComboModel.NO_PREFIX_KEY_CODE, keyCode, triggerModifierUsed);
  }

  public KeyCombo(int modifiers, int prefixKeyCode, int keyCode, boolean triggerModifierUsed) {
    this.triggerModifierUsed = triggerModifierUsed;
    this.keyComboCode = computeKeyComboCode(modifiers, prefixKeyCode, keyCode);
  }

  public int getModifiers() {
    return extractModifiers(keyComboCode);
  }

  public boolean isTriggerModifierUsed() {
    return triggerModifierUsed;
  }

  public int getPrefixKeyCode() {
    return (int) (keyComboCode & 0xFFFF0000) >>> 16;
  }

  public int getKeyCode() {
    return (int) (keyComboCode & 0xFFFF);
  }

  public long getKeyComboCode() {
    return keyComboCode;
  }

  @MatchResult
  public int matchWith(int triggerModifier, KeyCombo targetKeyCombo) {
    if (equals(targetKeyCombo)) {
      return EXACT_MATCH;
    }

    if (getPrefixKeyCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      // If the current key combo code is a sequenced key combo code, it is no match.
      return NO_MATCH;
    }

    // If not `enteredSequenceKeyMode`, check if the current key combo code is a partial match.
    int currentMetaState =
        this.triggerModifierUsed ? getModifiers() | triggerModifier : getModifiers();
    int currentKeyCode = getKeyCode();
    int targetMetaState =
        triggerModifierUsed
            ? targetKeyCombo.getModifiers() | triggerModifier
            : targetKeyCombo.getModifiers();

    // If the current key combo code has no modifier but the target key combo code has any modifier,
    // it is no match; it's not considered as a partial match.
    if (targetMetaState != KeyEvent.KEYCODE_UNKNOWN
        && currentMetaState == KeyEvent.KEYCODE_UNKNOWN) {
      return NO_MATCH;
    }

    // If any modifiers are shared between the current key combo code and the target key combo, it
    // is a partial match.
    if (KeyEvent.isModifierKey(currentKeyCode)
        && targetMetaState != KeyEvent.KEYCODE_UNKNOWN
        && (targetMetaState & currentMetaState) != 0) {
      return PARTIAL_MATCH;
    }

    return NO_MATCH;
  }

  public boolean isSequenceKeyPrefixCombo() {
    // Check if it matches with Modifier + R or Modifier + O. Note that the prefix key code will be
    // stored in the key code field when the prefix key combo is pressed.
    return SEQUENCE_KEY_COMBO_CODE_PREFIX_KEYS.contains(getKeyCode())
        && getModifiers() == KeyComboModel.NO_MODIFIER
        && getPrefixKeyCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED
        && triggerModifierUsed;
  }

  @Override
  public boolean equals(@Nullable Object object) {
    return object instanceof KeyCombo that
        && triggerModifierUsed == that.triggerModifierUsed
        && keyComboCode == that.keyComboCode;
  }

  @Override
  public int hashCode() {
    return Objects.hash(keyComboCode, triggerModifierUsed);
  }

  @Override
  public String toString() {
    return "modifier: "
        + extractModifiers(keyComboCode)
        + ", prefix key: "
        + getPrefixKeyCode()
        + ", key: "
        + getKeyCode()
        + ", triggerModifierUsed: "
        + triggerModifierUsed;
  }
}
