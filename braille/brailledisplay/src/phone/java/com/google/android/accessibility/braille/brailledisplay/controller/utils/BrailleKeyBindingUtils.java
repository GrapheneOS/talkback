/*
 * Copyright (C) 2013 Google Inc.
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

package com.google.android.accessibility.braille.brailledisplay.controller.utils;

import static com.google.android.accessibility.braille.brailledisplay.SupportedCommand.getAllCommands;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.changeToSentence;
import static com.google.android.accessibility.braille.common.translate.BrailleTranslateUtils.getDotsDescription;

import android.content.Context;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.SupportedCommand;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.brltty.BrailleKeyBinding;
import com.google.android.accessibility.braille.brltty.BrlttyUtils;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Contains utility methods for working with BrailleKeyBindings. */
public class BrailleKeyBindingUtils {
  /** Returns a sorted list of bindings supported by the display. */
  public static ArrayList<BrailleKeyBinding> getSortedBindingsForDisplay(
      BrailleDisplayProperties props) {
    BrailleKeyBinding[] bindings = props.getKeyBindings();
    ArrayList<BrailleKeyBinding> sortedBindings = new ArrayList<>(Arrays.asList(bindings));
    Collections.sort(sortedBindings, COMPARE_BINDINGS);
    return sortedBindings;
  }

  /**
   * Returns the binding that matches the specified command in the sorted list of
   * BrailleKeyBindings. Returns null if not found.
   */
  @Nullable
  public static BrailleKeyBinding getBrailleKeyBindingForCommand(
      int command, ArrayList<BrailleKeyBinding> sortedBindings) {
    BrailleKeyBinding dummyBinding = new BrailleKeyBinding();
    dummyBinding.setCommand(command);
    int index = Collections.binarySearch(sortedBindings, dummyBinding, COMPARE_BINDINGS_BY_COMMAND);
    if (index < 0) {
      return null;
    }
    while (index > 0 && sortedBindings.get(index - 1).getCommand() == command) {
      index -= 1;
    }
    return sortedBindings.get(index);
  }

  /** Returns the friendly name for the specified key binding. */
  public static String getFriendlyKeyNamesForCommand(
      BrailleKeyBinding binding, Map<String, String> friendlyKeyNames, Context context) {
    List<String> keyNames =
        getFriendlyKeyNames(Lists.newArrayList(binding.getKeyNames()), friendlyKeyNames);
    BrailleCharacter brailleCharacter = BrlttyUtils.extractBrailleCharacter(context, keyNames);
    if (!brailleCharacter.isEmpty()) {
      keyNames.add(getDotsDescription(context.getResources(), brailleCharacter));
    }
    String keys = changeToSentence(context.getResources(), keyNames);
    return binding.isLongPress()
        ? context.getString(R.string.bd_commands_touch_and_hold_template, keys)
        : context.getString(R.string.bd_commands_press_template, keys);
  }

  /** Returns friendly key names (if available) based on the map. */
  public static List<String> getFriendlyKeyNames(
      List<String> unfriendlyNames, Map<String, String> friendlyNames) {
    List<String> result = new ArrayList<>();
    for (String unfriendlyName : unfriendlyNames) {
      String friendlyName = friendlyNames.get(unfriendlyName);
      if (friendlyName != null) {
        result.add(friendlyName);
      } else {
        result.add(unfriendlyName);
      }
    }
    return result;
  }

  /**
   * Compares key bindings by command number, then in an order that is deterministic and that makes
   * sure that the binding that should appear on the help screen comes first.
   */
  public static final Comparator<BrailleKeyBinding> COMPARE_BINDINGS =
      new Comparator<BrailleKeyBinding>() {
        @Override
        public int compare(BrailleKeyBinding lhs, BrailleKeyBinding rhs) {
          int command1 = lhs.getCommand();
          int command2 = rhs.getCommand();
          if (command1 != command2) {
            return command1 - command2;
          }
          // Prefer a binding without long press.
          boolean longPress1 = lhs.isLongPress();
          boolean longPress2 = rhs.isLongPress();
          if (longPress1 != longPress2) {
            return longPress1 ? 1 : -1;
          }
          // Prefer unified KeyBinding.
          boolean unified1 = lhs.isUnifiedKeyBinding();
          boolean unified2 = rhs.isUnifiedKeyBinding();
          if (unified1 != unified2) {
            return unified1 ? -1 : 1;
          }
          String[] names1 = lhs.getKeyNames();
          String[] names2 = rhs.getKeyNames();
          // Prefer fewer keys.
          if (names1.length != names2.length) {
            return names1.length - names2.length;
          }
          // Compare key names for determinism.
          for (int i = 0; i < names1.length; ++i) {
            String key1 = names1[i];
            String key2 = names2[i];
            int res = key1.compareTo(key2);
            if (res != 0) {
              return res;
            }
          }
          return 0;
        }
      };

  /** Compares key bindings by command number. Used for search. */
  public static final Comparator<BrailleKeyBinding> COMPARE_BINDINGS_BY_COMMAND =
      new Comparator<BrailleKeyBinding>() {
        @Override
        public int compare(BrailleKeyBinding lhs, BrailleKeyBinding rhs) {
          return lhs.getCommand() - rhs.getCommand();
        }
      };

  /** Converts {@link BrailleInputEvent} to {@link SupportedCommand} by press key. */
  @Nullable
  public static SupportedCommand convertToCommand(Context context, boolean hasSpace, int pressDot) {
    return getAllCommands(context).stream()
        .filter(c -> c.hasSpace() == hasSpace && c.getPressDot().toInt() == pressDot)
        .findFirst()
        .orElse(null);
  }

  /**
   * Converts {@link BrailleInputEvent} to {@link SupportedCommand}. Returns null if {@link
   * BrailleInputEvent} is not supported.
   */
  @Nullable
  public static SupportedCommand convertToCommand(Context context, BrailleInputEvent event) {
    return getAllCommands(context).stream()
        .filter(c -> c.getCommand() == event.getCommand())
        .findFirst()
        .orElse(null);
  }
}
