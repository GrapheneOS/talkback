package com.google.android.accessibility.braille.brltty;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A builder for the key name map.
 *
 * <p>The key name map is used to map the key name to the resource id of the key name.
 */
public class KeyNameMapBuilder {
  private final Map<String, Integer> nameMap = new HashMap<>();

  /**
   * Adds a mapping from the internal {@code name} to a friendly name with resource id {@code
   * resId}.
   */
  @CanIgnoreReturnValue
  public KeyNameMapBuilder add(String name, int resId) {
    nameMap.put(name, resId);
    return this;
  }

  @CanIgnoreReturnValue
  public KeyNameMapBuilder dots6() {
    add("Dot1", R.string.key_Dot1);
    add("Dot2", R.string.key_Dot2);
    add("Dot3", R.string.key_Dot3);
    add("Dot4", R.string.key_Dot4);
    add("Dot5", R.string.key_Dot5);
    add("Dot6", R.string.key_Dot6);
    return this;
  }

  @CanIgnoreReturnValue
  public KeyNameMapBuilder dots8() {
    dots6();
    add("Dot7", R.string.key_Dot7);
    add("Dot8", R.string.key_Dot8);
    return this;
  }

  @CanIgnoreReturnValue
  public KeyNameMapBuilder routing() {
    return add("RoutingKey", R.string.key_Routing);
  }

  @CanIgnoreReturnValue
  public KeyNameMapBuilder dualJoysticks() {
    add("LeftJoystickLeft", R.string.key_LeftJoystickLeft);
    add("LeftJoystickRight", R.string.key_LeftJoystickRight);
    add("LeftJoystickUp", R.string.key_LeftJoystickUp);
    add("LeftJoystickDown", R.string.key_LeftJoystickDown);
    add("LeftJoystickPress", R.string.key_LeftJoystickCenter);
    add("RightJoystickLeft", R.string.key_RightJoystickLeft);
    add("RightJoystickRight", R.string.key_RightJoystickRight);
    add("RightJoystickUp", R.string.key_RightJoystickUp);
    add("RightJoystickDown", R.string.key_RightJoystickDown);
    add("RightJoystickPress", R.string.key_RightJoystickCenter);
    return this;
  }

  public Map<String, Integer> build() {
    return Collections.unmodifiableMap(nameMap);
  }
}
