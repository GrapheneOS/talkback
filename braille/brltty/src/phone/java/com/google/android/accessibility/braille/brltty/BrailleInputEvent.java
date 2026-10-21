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

package com.google.android.accessibility.braille.brltty;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * An input event, originating from a braille display.
 *
 * <p>An event contains a command that is a high-level representation of the key or key combination
 * that was pressed on the display such as a navigation key or braille keyboard combination. For
 * some commands, there is also an integer argument that contains additional information.
 *
 * <p>Members of this class are accessed by native brltty code.
 */
public class BrailleInputEvent implements Parcelable {

  // Movement commands.

  /** Keyboard command: Used when there is no actual command. */
  public static final int CMD_NONE = -1;

  /** Keyboard command: Navigate backwards by line. */
  public static final int CMD_NAV_LINE_PREVIOUS = 1;

  /** Keyboard command: Navigate forwards by line. */
  public static final int CMD_NAV_LINE_NEXT = 2;

  /** Keyboard command: Navigate left one item. */
  public static final int CMD_NAV_ITEM_PREVIOUS = 3;

  /** Keyboard command: Navigate right one item. */
  public static final int CMD_NAV_ITEM_NEXT = 4;

  /** Keyboard command: Navigate one display window to the up. */
  public static final int CMD_NAV_PAN_UP = 5;

  /** Keyboard command: Navigate one display window to the down. */
  public static final int CMD_NAV_PAN_DOWN = 6;

  /** Keyboard command: Navigate to the top or beginning. */
  public static final int CMD_NAV_TOP = 7;

  /** Keyboard command: Navigate to the bottom or end. */
  public static final int CMD_NAV_BOTTOM = 8;

  /** Keyboard command: Navigate previous character. */
  public static final int CMD_NAV_CHARACTER_PREVIOUS = 9;

  /** Keyboard command: Navigate next character. */
  public static final int CMD_NAV_CHARACTER_NEXT = 10;

  /** Keyboard command: Navigate previous word. */
  public static final int CMD_NAV_WORD_PREVIOUS = 11;

  /** Keyboard command: Navigate next word. */
  public static final int CMD_NAV_WORD_NEXT = 12;

  /** Keyboard command: Navigate next window. */
  public static final int CMD_NAV_WINDOW_PREVIOUS = 13;

  /** Keyboard command: Navigate previous window. */
  public static final int CMD_NAV_WINDOW_NEXT = 14;

  /** Keyboard command: Navigate backward by reading granularity or adjust reading control down. */
  public static final int CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD =
      15;

  /** Keyboard command: Navigate forward by reading granularity or adjust reading control up. */
  public static final int CMD_NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD =
      16;

  /** Keyboard command: Enter or exit captions. */
  public static final int CMD_CAPTION_ENTER_OR_EXIT = 17;

  // Activation commands.

  /** Keyboard command: Activate the currently selected/focused item. */
  public static final int CMD_ACTIVATE_CURRENT = 20;

  /** Keyboard command: Long press the currently selected/focused item. */
  public static final int CMD_LONG_PRESS_CURRENT = 21;

  /** Keyboard command: Navigate table previous row. */
  public static final int CMD_TABLE_ROW_PREVIOUS = 22;

  /** Keyboard command: Navigate table next row. */
  public static final int CMD_TABLE_ROW_NEXT = 23;

  /** Keyboard command: Navigate table previous column. */
  public static final int CMD_TABLE_COL_PREVIOUS = 24;

  /** Keyboard command: Navigate table next column. */
  public static final int CMD_TABLE_COL_NEXT = 25;

  // Scrolling.

  /** Keyboard command: Scroll backward. */
  public static final int CMD_SCROLL_BACKWARD = 30;

  /** Keyboard command: Scroll forward. */
  public static final int CMD_SCROLL_FORWARD = 31;

  // Selection commands.

  /** Keyboard command: Set the start ot the selection. */
  public static final int CMD_SELECTION_START = 40;

  /** Keyboard command: Set the end of the selection. */
  public static final int CMD_SELECTION_END = 41;

  /** Keyboard command: Select all content of the current field. */
  public static final int CMD_SELECTION_SELECT_ALL = 42;

  /** Keyboard command: Cut the content of the selection. */
  public static final int CMD_SELECTION_CUT = 43;

  /** Keyboard command: Copy the current selection. */
  public static final int CMD_SELECTION_COPY = 44;

  /** Keyboard command: Paste the content of the clipboard at the current insertion point. */
  public static final int CMD_SELECTION_PASTE = 45;

  /** Keyboard command: Select from current cursor to the start of text field. */
  public static final int CMD_SELECTION_SELECT_CURRENT_TO_START = 46;

  /** Keyboard command: Select from current cursor to the end of text field. */
  public static final int CMD_SELECTION_SELECT_CURRENT_TO_END = 47;

  /**
   * Keyboard command: Primary routing key pressed, typically used to move the insertion point or
   * click/tap on the item under the key. The argument is the zero-based position, relative to the
   * first cell on the display, of the cell that is closed to the key that was pressed.
   */
  public static final int CMD_ROUTE = 50;

  /**
   * Keyboard command: Primary routing key long pressed, typically used to long press the item under
   * the key. The argument is the zero-based position, relative to the first cell on the display, of
   * the cell that is closed to the key that was pressed.
   */
  public static final int CMD_LONG_PRESS_ROUTE = 51;

  // Braille keyboard input.

  /**
   * Keyboard command: A key combination was pressed on the braille keyboard. The argument contains
   * the dots that were pressed as a bitmask.
   */
  public static final int CMD_BRAILLE_KEY = 60;

  // Editing keys.

  /** Keyboard command: Enter key. */
  public static final int CMD_KEY_ENTER = 70;

  /** Keyboard command: Delete backward. */
  public static final int CMD_KEY_DEL = 71;

  /** Keyboard command: Delete a word backward. */
  public static final int CMD_DEL_WORD = 72;

  /** Keyboard command: Select previous character. */
  public static final int CMD_SELECT_PREVIOUS_CHARACTER = 77;

  /** Keyboard command: Select next character. */
  public static final int CMD_SELECT_NEXT_CHARACTER = 78;

  /** Keyboard command: Select previous word. */
  public static final int CMD_SELECT_PREVIOUS_WORD = 79;

  /** Keyboard command: Select next word. */
  public static final int CMD_SELECT_NEXT_WORD = 80;

  /** Keyboard command: Select previous line. */
  public static final int CMD_SELECT_PREVIOUS_LINE = 81;

  /** Keyboard command: Select next line. */
  public static final int CMD_SELECT_NEXT_LINE = 82;

  /** Keyboard command: Next input method. */
  public static final int CMD_NEXT_INPUT_METHOD = 83;

  // Global navigation keys.

  /** Keyboard command: Back button. */
  public static final int CMD_GLOBAL_BACK = 90;

  /** Keyboard command: Home button. */
  public static final int CMD_GLOBAL_HOME = 91;

  /** Keyboard command: Recent apps button. */
  public static final int CMD_GLOBAL_RECENTS = 92;

  /** Keyboard command: Show notifications. */
  public static final int CMD_GLOBAL_NOTIFICATIONS = 93;

  /** Keyboard command: Quick Settings. */
  public static final int CMD_QUICK_SETTINGS = 94;

  /** Keyboard command: All apps. */
  public static final int CMD_ALL_APPS = 95;

  /** Keyboard command: Play or pause media. */
  public static final int CMD_PLAY_PAUSE_MEDIA = 134;

  // Miscellaneous commands.

  /** Keyboard command: Invoke keyboard help. */
  public static final int CMD_HELP = 100;

  /** Keyboard command: Edit custom label. */
  public static final int CMD_EDIT_CUSTOM_LABEL = 117;

  /** Keyboard command: Open TalkBack menu. */
  public static final int CMD_OPEN_TALKBACK_MENU = 118;

  /** Keyboard command: Switch to next input language. */
  public static final int CMD_SWITCH_TO_NEXT_INPUT_LANGUAGE = 119;

  /** Keyboard command: Switch to next output language. */
  public static final int CMD_SWITCH_TO_NEXT_OUTPUT_LANGUAGE = 120;

  /** Keyboard command: Navigate Braille display settings. */
  public static final int CMD_BRAILLE_DISPLAY_SETTINGS = 121;

  /** Keyboard command: Navigate TalkBack settings. */
  public static final int CMD_TALKBACK_SETTINGS = 122;

  /** Keyboard command: Turn off braille display. */
  public static final int CMD_TURN_OFF_BRAILLE_DISPLAY = 123;

  /** Keyboard command: Mute/Unmute voice feedback. */
  public static final int CMD_TOGGLE_VOICE_FEEDBACK = 124;

  /** Keyboard command: Switch to previous reading control. */
  public static final int CMD_PREVIOUS_READING_CONTROL = 125;

  /** Keyboard command: Switch to next reading control. */
  public static final int CMD_NEXT_READING_CONTROL = 126;

  /** Keyboard command: Toggle braille grade. */
  public static final int CMD_TOGGLE_BRAILLE_GRADE = 127;

  /** Keyboard command: In editing text, navigate to the top. In common state, activate key. */
  public static final int CMD_NAV_TOP_OR_KEY_ACTIVATE = 128;

  /** Keyboard command: In editing text, navigate to the bottom. In common state, activate key. */
  public static final int CMD_NAV_BOTTOM_OR_KEY_ACTIVATE = 129;

  /** Keyboard command: Stop reading. */
  public static final int CMD_STOP_READING = 130;

  /** Keyboard command: Start or stop auto scroll. */
  public static final int CMD_TOGGLE_AUTO_SCROLL = 131;

  /** Keyboard command: Increase auto scroll duration. */
  public static final int CMD_INCREASE_AUTO_SCROLL_DURATION = 132;

  /** Keyboard command: Decrease auto scroll duration. */
  public static final int CMD_DECREASE_AUTO_SCROLL_DURATION = 133;

  /** Keyboard command: Open History Dialog. */
  public static final int CMD_SHOW_POPUP_MESSAGE_HISTORY = 135;

  /** Keyboard command: Toggle Screen search. */
  public static final int CMD_TOGGLE_SCREEN_SEARCH = 116;

  // Web content commands.

  /** Keyboard command: Toggle browse mode. */
  public static final int CMD_TOGGLE_BROWSE_MODE = 140;

  /** Keyboard command: Next landmark in page. */
  public static final int CMD_ARIA_LANDMARK_NEXT = 141;

  /** Keyboard command: Next landmark in page in browse mode. */
  public static final int CMD_ARIA_LANDMARK_NEXT_IN_BROWSE = 142;

  /** Keyboard command: Previous landmark in page. */
  public static final int CMD_ARIA_LANDMARK_PREVIOUS = 143;

  /** Keyboard command: Next graphic in page. */
  public static final int CMD_GRAPHIC_NEXT = 144;

  /** Keyboard command: Next graphic in page in browse mode. */
  public static final int CMD_GRAPHIC_NEXT_IN_BROWSE = 145;

  /** Keyboard command: Previous graphic in page. */
  public static final int CMD_GRAPHIC_PREVIOUS = 146;

  /** Keyboard command: Next list in page. */
  public static final int CMD_LIST_NEXT = 147;

  /** Keyboard command: Next list in page in browse mode. */
  public static final int CMD_LIST_NEXT_IN_BROWSE = 148;

  /** Keyboard command: Previous list in page. */
  public static final int CMD_LIST_PREVIOUS = 149;

  /** Keyboard command: Next list item in page. */
  public static final int CMD_LIST_ITEM_NEXT = 150;

  /** Keyboard command: Next list item in page in browse mode. */
  public static final int CMD_LIST_ITEM_NEXT_IN_BROWSE = 151;

  /** Keyboard command: Previous list item in page. */
  public static final int CMD_LIST_ITEM_PREVIOUS = 152;

  /** Keyboard command: Next table in page. */
  public static final int CMD_TABLE_NEXT = 153;

  /** Keyboard command: Next table in page in browse mode. */
  public static final int CMD_TABLE_NEXT_IN_BROWSE = 154;

  /** Keyboard command: Previous table in page. */
  public static final int CMD_TABLE_PREVIOUS = 155;

  /** Keyboard command: Next heading in page. */
  public static final int CMD_HEADING_NEXT = 156;

  /** Keyboard command: Next heading in page. */
  public static final int CMD_HEADING_NEXT_IN_BROWSE = 157;

  /** Keyboard command: Previous heading in page. */
  public static final int CMD_HEADING_PREVIOUS = 158;

  /** Keyboard command: Next heading 1 in page. */
  public static final int CMD_HEADING_1_NEXT = 159;

  /** Keyboard command: Next heading 1 in page in browse mode. */
  public static final int CMD_HEADING_1_NEXT_IN_BROWSE = 160;

  /** Keyboard command: Previous heading 1 in page. */
  public static final int CMD_HEADING_1_PREVIOUS = 161;

  /** Keyboard command: Next heading 2 in page. */
  public static final int CMD_HEADING_2_NEXT = 162;

  /** Keyboard command: Next heading 2 in page in browse mode. */
  public static final int CMD_HEADING_2_NEXT_IN_BROWSE = 163;

  /** Keyboard command: Previous heading 2 in page. */
  public static final int CMD_HEADING_2_PREVIOUS = 164;

  /** Keyboard command: Next heading 3 in page. */
  public static final int CMD_HEADING_3_NEXT = 165;

  /** Keyboard command: Next heading 3 in page in browse mode. */
  public static final int CMD_HEADING_3_NEXT_IN_BROWSE = 166;

  /** Keyboard command: Previous heading 3 in page. */
  public static final int CMD_HEADING_3_PREVIOUS = 167;

  /** Keyboard command: Next heading 4 in page. */
  public static final int CMD_HEADING_4_NEXT = 168;

  /** Keyboard command: Next heading 4 in page in browse mode. */
  public static final int CMD_HEADING_4_NEXT_IN_BROWSE = 169;

  /** Keyboard command: Previous heading 4 in page. */
  public static final int CMD_HEADING_4_PREVIOUS = 170;

  /** Keyboard command: Next heading 5 in page. */
  public static final int CMD_HEADING_5_NEXT = 171;

  /** Keyboard command: Next heading 5 in page in browse mode. */
  public static final int CMD_HEADING_5_NEXT_IN_BROWSE = 172;

  /** Keyboard command: Previous heading 5 in page. */
  public static final int CMD_HEADING_5_PREVIOUS = 173;

  /** Keyboard command: Next heading 6 in page. */
  public static final int CMD_HEADING_6_NEXT = 174;

  /** Keyboard command: Next heading 6 in page in browse mode. */
  public static final int CMD_HEADING_6_NEXT_IN_BROWSE = 175;

  /** Keyboard command: Previous heading 6 in page. */
  public static final int CMD_HEADING_6_PREVIOUS = 176;

  /** Keyboard command: Next button in page. */
  public static final int CMD_BUTTON_NEXT = 177;

  /** Keyboard command: Next button in page in browse mode. */
  public static final int CMD_BUTTON_NEXT_IN_BROWSE = 178;

  /** Keyboard command: Previous button in page. */
  public static final int CMD_BUTTON_PREVIOUS = 179;

  /** Keyboard command: Next checkbox in page. */
  public static final int CMD_CHECKBOX_NEXT = 180;

  /** Keyboard command: Next checkbox in page in browse mode. */
  public static final int CMD_CHECKBOX_NEXT_IN_BROWSE = 181;

  /** Keyboard command: Previous checkbox in page. */
  public static final int CMD_CHECKBOX_PREVIOUS = 182;

  /** Keyboard command: Next radio button in page. */
  public static final int CMD_RADIO_BUTTON_NEXT = 183;

  /** Keyboard command: Next radio button in page in browse mode. */
  public static final int CMD_RADIO_BUTTON_NEXT_IN_BROWSE = 184;

  /** Keyboard command: Previous radio button in page. */
  public static final int CMD_RADIO_BUTTON_PREVIOUS = 185;

  /** Keyboard command: Next combo box in page. */
  public static final int CMD_COMBO_BOX_NEXT = 186;

  /** Keyboard command: Next combo box in page in browse mode. */
  public static final int CMD_COMBO_BOX_NEXT_IN_BROWSE = 187;

  /** Keyboard command: Previous combo box in page. */
  public static final int CMD_COMBO_BOX_PREVIOUS = 188;

  /** Keyboard command: Next control in page. */
  public static final int CMD_CONTROL_NEXT = 189;

  /** Keyboard command: Next control in page. */
  public static final int CMD_CONTROL_NEXT_IN_BROWSE = 190;

  /** Keyboard command: Previous control in page. */
  public static final int CMD_CONTROL_PREVIOUS = 191;

  /** Keyboard command: Edit field next. */
  public static final int CMD_EDIT_FIELD_NEXT = 192;

  /** Keyboard command: Edit field next in browse mode. */
  public static final int CMD_EDIT_FIELD_NEXT_IN_BROWSE = 193;

  /** Keyboard command: Edit field previous. */
  public static final int CMD_EDIT_FIELD_PREVIOUS = 194;

  /** Keyboard command: Next link in page. */
  public static final int CMD_LINK_NEXT = 195;

  /** Keyboard command: Next link in page. */
  public static final int CMD_LINK_NEXT_IN_BROWSE = 196;

  /** Keyboard command: Previous link in page. */
  public static final int CMD_LINK_PREVIOUS = 197;

  /** Keyboard command: Next visited link in page. */
  public static final int CMD_VISITED_LINK_NEXT = 198;

  /** Keyboard command: Next visited link in page in browse mode. */
  public static final int CMD_VISITED_LINK_NEXT_IN_BROWSE = 199;

  /** Keyboard command: Previous visited link in page. */
  public static final int CMD_VISITED_LINK_PREVIOUS = 200;

  /** Keyboard command: Next unvisited link in page. */
  public static final int CMD_UNVISITED_LINK_NEXT = 201;

  /** Keyboard command: Next unvisited link in page in browse mode. */
  public static final int CMD_UNVISITED_LINK_NEXT_IN_BROWSE = 202;

  /** Keyboard command: Previous unvisited link in page. */
  public static final int CMD_UNVISITED_LINK_PREVIOUS = 203;

  // Meanings of the argument to a command.

  /** This command doesn't have an argument. */
  public static final int ARGUMENT_NONE = 0;

  /**
   * The lower order bits of the argument to this command represent braille dots. Dot 1 is
   * represented by the rightmost bit and so on until dot 8, which is represented by bit 7, counted
   * from the right.
   */
  public static final int ARGUMENT_DOTS = 1;

  /** The argument represents a 0-based position on the display counted from the leftmost cell. */
  public static final int ARGUMENT_POSITION = 2;

  private final int command;
  private final int argument;
  private final long eventTime;

  public BrailleInputEvent(int command, int argument, long eventTime) {
    this.command = command;
    this.argument = argument;
    this.eventTime = eventTime;
  }

  private BrailleInputEvent(Parcel in) {
    command = in.readInt();
    argument = in.readInt();
    eventTime = in.readLong();
  }

  /** Returns the keyboard command that this event represents. */
  public int getCommand() {
    return command;
  }

  /**
   * Returns the command-specific argument of the event, or zero if the command doesn't have an
   * argument. See the individual command constants for more details.
   */
  public int getArgument() {
    return argument;
  }

  /**
   * Returns the approximate time when this event happened as returned by {@link
   * android.os.SystemClock#uptimeMillis}.
   */
  public long getEventTime() {
    return eventTime;
  }

  /** Returns the type of argument for the given {@code command}. */
  public static int argumentType(int command) {
    return switch (command) {
      case CMD_SELECTION_START, CMD_SELECTION_END, CMD_ROUTE, CMD_LONG_PRESS_ROUTE ->
          ARGUMENT_POSITION;
      case CMD_BRAILLE_KEY -> ARGUMENT_DOTS;
      default -> ARGUMENT_NONE;
    };
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("BrailleInputEvent {");
    sb.append("cmd=");
    sb.append(command);
    sb.append(", arg=");
    sb.append(argument);
    sb.append("}");
    return sb.toString();
  }

  // For Parcelable support.

  public static final Creator<BrailleInputEvent> CREATOR =
      new Creator<BrailleInputEvent>() {
        @Override
        public BrailleInputEvent createFromParcel(Parcel in) {
          return new BrailleInputEvent(in);
        }

        @Override
        public BrailleInputEvent[] newArray(int size) {
          return new BrailleInputEvent[size];
        }
      };

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel out, int flags) {
    out.writeInt(command);
    out.writeInt(argument);
    out.writeLong(eventTime);
  }
}
