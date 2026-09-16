/*
 * Copyright (C) 2020 Google Inc.
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

package com.google.android.accessibility.talkback.analytics;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.PeriodicLogSender;
import com.google.android.accessibility.talkback.TouchExplorationModeFailureReporter.State;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping.TalkbackAction;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;
import com.google.android.accessibility.talkback.keyboard.TalkBackPhysicalKeyboardShortcut;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.talkback.trainingcommon.TrainingMetricStore.TrainingMetric;
import com.google.android.accessibility.utils.input.CursorGranularity;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.List;

/** A base class to declare constants and callback functions defined for Analytics. */
public interface TalkBackAnalytics extends OnSharedPreferenceChangeListener {
  /** These constants of user action type. */
  int TYPE_UNKNOWN = 0;

  int TYPE_PREFERENCE_SETTING = 1;
  int TYPE_GESTURE = 2;
  int TYPE_CONTEXT_MENU = 3;
  int TYPE_SELECTOR = 4;
  int TYPE_KEYBOARD = 5;

  int MENU_ITEM_UNKNOWN = -1;

  /** Defines types of user actions to change granularity, setting. */
  @IntDef({
    TYPE_UNKNOWN,
    TYPE_PREFERENCE_SETTING,
    TYPE_GESTURE,
    TYPE_CONTEXT_MENU,
    TYPE_SELECTOR,
    TYPE_KEYBOARD
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface UserActionType {}

  /** These constants of local context menu type. */
  int MENU_TYPE_UNKNOWN = 0;

  int MENU_TYPE_GRANULARITY = 1;
  int MENU_TYPE_EDIT_OPTIONS = 2;
  int MENU_TYPE_VIEW_PAGER = 3;
  int MENU_TYPE_LABELING = 4;
  int MENU_TYPE_CUSTOM_ACTION = 5;
  int MENU_TYPE_SEEK_BAR = 6;
  int MENU_TYPE_SPANNABLES = 7;
  int MENU_TYPE_TEXT_FORMATTING = 8;

  /** Defines types of local context menu. */
  @IntDef({
    MENU_TYPE_UNKNOWN,
    MENU_TYPE_GRANULARITY,
    MENU_TYPE_EDIT_OPTIONS,
    MENU_TYPE_VIEW_PAGER,
    MENU_TYPE_LABELING,
    MENU_TYPE_CUSTOM_ACTION,
    MENU_TYPE_SEEK_BAR,
    MENU_TYPE_SPANNABLES,
    MENU_TYPE_TEXT_FORMATTING
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface LocalContextMenuType {}

  /** These constants of recognized gesture id. */
  int GESTURE_UNKNOWN = 0;

  int GESTURE_1FINGER_UP = 1;
  int GESTURE_1FINGER_DOWN = 2;
  int GESTURE_1FINGER_LEFT = 3;
  int GESTURE_1FINGER_RIGHT = 4;
  int GESTURE_1FINGER_LEFT_RIGHT = 5;
  int GESTURE_1FINGER_RIGHT_LEFT = 6;
  int GESTURE_1FINGER_UP_DOWN = 7;
  int GESTURE_1FINGER_DOWN_UP = 8;
  int GESTURE_1FINGER_LEFT_UP = 9;
  int GESTURE_1FINGER_LEFT_DOWN = 10;
  int GESTURE_1FINGER_RIGHT_UP = 11;
  int GESTURE_1FINGER_RIGHT_DOWN = 12;
  int GESTURE_1FINGER_UP_LEFT = 13;
  int GESTURE_1FINGER_UP_RIGHT = 14;
  int GESTURE_1FINGER_DOWN_LEFT = 15;
  int GESTURE_1FINGER_DOWN_RIGHT = 16;
  int GESTURE_1FINGER_2TAP = 17;
  int GESTURE_1FINGER_2TAP_HOLD = 18;
  int GESTURE_2FINGER_1TAP = 19;
  int GESTURE_2FINGER_2TAP = 20;
  int GESTURE_2FINGER_3TAP = 21;
  int GESTURE_3FINGER_1TAP = 22;
  int GESTURE_3FINGER_2TAP = 23;
  int GESTURE_3FINGER_3TAP = 24;
  int GESTURE_2FINGER_UP = 25;
  int GESTURE_2FINGER_DOWN = 26;
  int GESTURE_2FINGER_LEFT = 27;
  int GESTURE_2FINGER_RIGHT = 28;
  int GESTURE_3FINGER_UP = 29;
  int GESTURE_3FINGER_DOWN = 30;
  int GESTURE_3FINGER_LEFT = 31;
  int GESTURE_3FINGER_RIGHT = 32;
  int GESTURE_4FINGER_UP = 33;
  int GESTURE_4FINGER_DOWN = 34;
  int GESTURE_4FINGER_LEFT = 35;
  int GESTURE_4FINGER_RIGHT = 36;
  int GESTURE_4FINGER_1TAP = 37;
  int GESTURE_4FINGER_2TAP = 38;
  int GESTURE_4FINGER_3TAP = 39;
  int GESTURE_2FINGER_2TAP_HOLD = 40;
  int GESTURE_3FINGER_2TAP_HOLD = 41;
  int GESTURE_4FINGER_2TAP_HOLD = 42;
  int GESTURE_2FINGER_3TAP_HOLD = 43;
  int GESTURE_3FINGER_1TAP_HOLD = 44;
  int GESTURE_3FINGER_3TAP_HOLD = 45;

  int GESTURE_SPLIT_TAP = 61;
  int GESTURE_LIFT_TO_TYPE = 62;
  int GESTURE_2FINGER_1TAP_HOLD = 63;
  int GESTURE_SPLIT_TAP_HOLD = 64;

  /** Defines the recognized gesture id. */
  @IntDef({
    GESTURE_UNKNOWN,
    GESTURE_1FINGER_UP,
    GESTURE_1FINGER_DOWN,
    GESTURE_1FINGER_LEFT,
    GESTURE_1FINGER_RIGHT,
    GESTURE_1FINGER_LEFT_RIGHT,
    GESTURE_1FINGER_RIGHT_LEFT,
    GESTURE_1FINGER_UP_DOWN,
    GESTURE_1FINGER_DOWN_UP,
    GESTURE_1FINGER_LEFT_UP,
    GESTURE_1FINGER_LEFT_DOWN,
    GESTURE_1FINGER_RIGHT_UP,
    GESTURE_1FINGER_RIGHT_DOWN,
    GESTURE_1FINGER_UP_LEFT,
    GESTURE_1FINGER_UP_RIGHT,
    GESTURE_1FINGER_DOWN_LEFT,
    GESTURE_1FINGER_DOWN_RIGHT,
    GESTURE_1FINGER_2TAP,
    GESTURE_1FINGER_2TAP_HOLD,
    GESTURE_2FINGER_1TAP,
    GESTURE_2FINGER_2TAP,
    GESTURE_2FINGER_3TAP,
    GESTURE_3FINGER_1TAP,
    GESTURE_3FINGER_2TAP,
    GESTURE_3FINGER_3TAP,
    GESTURE_2FINGER_UP,
    GESTURE_2FINGER_DOWN,
    GESTURE_2FINGER_LEFT,
    GESTURE_2FINGER_RIGHT,
    GESTURE_3FINGER_UP,
    GESTURE_3FINGER_DOWN,
    GESTURE_3FINGER_LEFT,
    GESTURE_3FINGER_RIGHT,
    GESTURE_4FINGER_UP,
    GESTURE_4FINGER_DOWN,
    GESTURE_4FINGER_LEFT,
    GESTURE_4FINGER_RIGHT,
    GESTURE_4FINGER_1TAP,
    GESTURE_4FINGER_2TAP,
    GESTURE_4FINGER_3TAP,
    GESTURE_2FINGER_1TAP_HOLD,
    GESTURE_2FINGER_2TAP_HOLD,
    GESTURE_3FINGER_2TAP_HOLD,
    GESTURE_4FINGER_2TAP_HOLD,
    GESTURE_2FINGER_3TAP_HOLD,
    GESTURE_3FINGER_1TAP_HOLD,
    GESTURE_3FINGER_3TAP_HOLD,
    GESTURE_SPLIT_TAP,
    GESTURE_LIFT_TO_TYPE,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface GestureId {}

  /** These constants of voice command events. */
  int VOICE_COMMAND_ATTEMPT = 1;

  int VOICE_COMMAND_TIMEOUT = 2;
  int VOICE_COMMAND_RECOGNIZED = 3;
  int VOICE_COMMAND_UNRECOGNIZED = 4;
  int VOICE_COMMAND_ENGINE_ERROR = 5;

  /** Defines events of voice command. */
  @IntDef({
    VOICE_COMMAND_ATTEMPT,
    VOICE_COMMAND_TIMEOUT,
    VOICE_COMMAND_RECOGNIZED,
    VOICE_COMMAND_UNRECOGNIZED,
    VOICE_COMMAND_ENGINE_ERROR
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface VoiceCommandEventId {}

  // LINT.IfChange(voice_command_type)
  // These types should be maintained with the same order as the VoiceCommandType defined in the
  // proto , and the value must be continuous.
  int VOICE_COMMAND_TYPE_SELECT_ALL = 1;
  int VOICE_COMMAND_TYPE_HIDE_SCREEN = 2;
  int VOICE_COMMAND_TYPE_SCREEN_SEARCH = 3;
  int VOICE_COMMAND_TYPE_END_SELECT = 4;
  int VOICE_COMMAND_TYPE_START_SELECT = 5;
  int VOICE_COMMAND_TYPE_CUSTOM_ACTION = 6;
  int VOICE_COMMAND_TYPE_NEXT_HEADING = 7;
  int VOICE_COMMAND_TYPE_NEXT_CONTROL = 8;
  int VOICE_COMMAND_TYPE_NEXT_LINK = 9;
  int VOICE_COMMAND_TYPE_NEXT_LANDMARK = 10;
  int VOICE_COMMAND_TYPE_VERBOSITY = 11;
  int VOICE_COMMAND_TYPE_GRANULARITY = 12;
  int VOICE_COMMAND_TYPE_SHOW_SCREEN = 13;
  int VOICE_COMMAND_TYPE_BACK = 14;
  int VOICE_COMMAND_TYPE_SPEECH_RATE_INCREASE = 15;
  int VOICE_COMMAND_TYPE_SPEECH_RATE_DECREASE = 16;
  int VOICE_COMMAND_TYPE_FIND = 17;
  int VOICE_COMMAND_TYPE_INSERT = 18;
  int VOICE_COMMAND_TYPE_LABEL = 19;
  int VOICE_COMMAND_TYPE_READ_FROM_CURSOR = 20;
  int VOICE_COMMAND_TYPE_READ_FROM_TOP = 21;
  int VOICE_COMMAND_TYPE_COPY_LAST_UTTERANCE = 22;
  int VOICE_COMMAND_TYPE_QUICK_SETTING = 23;
  int VOICE_COMMAND_TYPE_TALKBACK_SETTING = 24;
  int VOICE_COMMAND_TYPE_COPY = 25;
  int VOICE_COMMAND_TYPE_PASTE = 26;
  int VOICE_COMMAND_TYPE_CUT = 27;
  int VOICE_COMMAND_TYPE_DELETE = 28;
  int VOICE_COMMAND_TYPE_FIRST = 29;
  int VOICE_COMMAND_TYPE_LAST = 30;
  int VOICE_COMMAND_TYPE_LANGUAGE = 31;
  int VOICE_COMMAND_TYPE_NOTIFICATION = 32;
  int VOICE_COMMAND_TYPE_RECENT_APPS = 33;
  int VOICE_COMMAND_TYPE_ALL_APPS = 34;
  int VOICE_COMMAND_TYPE_HOME = 35;
  int VOICE_COMMAND_TYPE_QUIT = 36;
  int VOICE_COMMAND_TYPE_ASSISTANT = 37;
  int VOICE_COMMAND_TYPE_HELP = 38;
  int VOICE_COMMAND_TYPE_GEMINI = 39;

  /** Defines types of voice command. */
  @IntDef({
    VOICE_COMMAND_TYPE_SELECT_ALL,
    VOICE_COMMAND_TYPE_HIDE_SCREEN,
    VOICE_COMMAND_TYPE_SCREEN_SEARCH,
    VOICE_COMMAND_TYPE_END_SELECT,
    VOICE_COMMAND_TYPE_START_SELECT,
    VOICE_COMMAND_TYPE_CUSTOM_ACTION,
    VOICE_COMMAND_TYPE_NEXT_HEADING,
    VOICE_COMMAND_TYPE_NEXT_CONTROL,
    VOICE_COMMAND_TYPE_NEXT_LINK,
    VOICE_COMMAND_TYPE_NEXT_LANDMARK,
    VOICE_COMMAND_TYPE_VERBOSITY,
    VOICE_COMMAND_TYPE_GRANULARITY,
    VOICE_COMMAND_TYPE_SHOW_SCREEN,
    VOICE_COMMAND_TYPE_BACK,
    VOICE_COMMAND_TYPE_SPEECH_RATE_INCREASE,
    VOICE_COMMAND_TYPE_SPEECH_RATE_DECREASE,
    VOICE_COMMAND_TYPE_FIND,
    VOICE_COMMAND_TYPE_INSERT,
    VOICE_COMMAND_TYPE_LABEL,
    VOICE_COMMAND_TYPE_READ_FROM_CURSOR,
    VOICE_COMMAND_TYPE_READ_FROM_TOP,
    VOICE_COMMAND_TYPE_COPY_LAST_UTTERANCE,
    VOICE_COMMAND_TYPE_QUICK_SETTING,
    VOICE_COMMAND_TYPE_TALKBACK_SETTING,
    VOICE_COMMAND_TYPE_COPY,
    VOICE_COMMAND_TYPE_PASTE,
    VOICE_COMMAND_TYPE_CUT,
    VOICE_COMMAND_TYPE_DELETE,
    VOICE_COMMAND_TYPE_FIRST,
    VOICE_COMMAND_TYPE_LAST,
    VOICE_COMMAND_TYPE_LANGUAGE,
    VOICE_COMMAND_TYPE_NOTIFICATION,
    VOICE_COMMAND_TYPE_RECENT_APPS,
    VOICE_COMMAND_TYPE_ALL_APPS,
    VOICE_COMMAND_TYPE_HOME,
    VOICE_COMMAND_TYPE_QUIT,
    VOICE_COMMAND_TYPE_ASSISTANT,
    VOICE_COMMAND_TYPE_HELP,
    VOICE_COMMAND_TYPE_GEMINI
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface VoiceCommandTypeId {}

  // LINT.ThenChange(//depot/google3/java/com/google/android/accessibility/talkback/overlay/google/analytics/proto/voice_command_enums.proto:voice_command_type)

  /** Constants of browse mode trigger. */
  int BROWSE_MODE_TURNED_OFF_UNSPECIFIED = 0;

  int BROWSE_MODE_TURNED_OFF_ON_SERVICE_STOP = 1;
  int BROWSE_MODE_TURNED_OFF_BY_USER = 2;
  int BROWSE_MODE_TURNED_OFF_BY_SMART_BROWSE_MODE = 3;

  /** Defines events of browse mode trigger. */
  @IntDef({
    BROWSE_MODE_TURNED_OFF_UNSPECIFIED,
    BROWSE_MODE_TURNED_OFF_ON_SERVICE_STOP,
    BROWSE_MODE_TURNED_OFF_BY_USER,
    BROWSE_MODE_TURNED_OFF_BY_SMART_BROWSE_MODE
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface BrowseModeLogTrigger {}

  @Override
  default void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {}

  default void onTalkBackServiceStarted() {}

  default void onTalkBackServiceStopped() {}

  default void onTtsInitialized(String ttsPackageName) {}

  default void onTtsUtteranceCompleted(int speechingTimeInSecond) {}

  default void onGesture(int gestureId) {}

  default void onGestureDebug(int debugEvent, int gestureId, int debugExtraValue) {}

  default void onMoveWithGranularity(CursorGranularity newGranularity) {}

  default void logPendingChanges() {}

  default void onManuallyChangeSetting(String prefKey, @UserActionType int userActionType) {}

  default void onGlobalContextMenuOpen(boolean isListStyle) {}

  default void onGlobalContextMenuAction(int menuItemId) {}

  default void onLocalContextMenuAction(@LocalContextMenuType int lcmType, int menuItemId) {}

  default void onLocalContextMenuOpen(boolean isListStyle) {}

  default void onVoiceCommandEvent(@VoiceCommandEventId int event) {}

  default void onVoiceCommandError(int error) {}

  default void onVoiceCommandType(@VoiceCommandTypeId int type) {}

  /** For select previous/next setting purpose in selector. */
  default void onSelectorEvent() {}

  /** For selected setting previous/next action purpose in selector. */
  default void onSelectorActionEvent(SelectorController.Setting setting) {}

  default void onTutorialEvent(@TrainingSectionId int section) {}

  default void onTrainingEvent(TrainingMetric trainingMetric) {}

  default void onSettingsPreferenceClicked(int event) {}

  default void onMagnificationUsed(int mode) {}

  default void onKeyboardShortcutUsed(
      TalkBackPhysicalKeyboardShortcut keyboardShortcut, int triggerModifier, long keyComboCode) {}

  default void onKeymapTypeUsed(KeyComboModel keyComboModel) {}

  default void onModifierKeyUsed(int modifierKey) {}

  default void onBrowseModeStopped(
      Duration browseModeDuration, @BrowseModeLogTrigger int trigger) {}

  default void sendMistriggerAndAliveTimeLog(int recoveryType) {}

  default void sendRequestTouchExplorationFailureLog(@State int failureState) {}

  default void sendLog() {}

  default void sendAliveTimeLog() {}

  int IMAGE_CAPTION_EVENT_CAPTION_REQUEST = 1;
  int IMAGE_CAPTION_EVENT_CAPTION_REQUEST_MANUAL = 2;
  int IMAGE_CAPTION_EVENT_SCREENSHOT_FAILED = 3;
  int IMAGE_CAPTION_EVENT_IMAGE_CAPTION_CACHE_HIT = 4;
  int IMAGE_CAPTION_EVENT_ICON_DETECT_PERFORM = 5; // Deprecated.
  int IMAGE_CAPTION_EVENT_ICON_DETECT_SUCCEED = 6;
  int IMAGE_CAPTION_EVENT_ICON_DETECT_NO_RESULT = 7;
  int IMAGE_CAPTION_EVENT_ICON_DETECT_FAIL = 8;
  int IMAGE_CAPTION_EVENT_OCR_PERFORM = 9; // Deprecated.
  int IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED = 10;
  int IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED_EMPTY = 11;
  int IMAGE_CAPTION_EVENT_OCR_PERFORM_FAIL = 12;
  int IMAGE_CAPTION_EVENT_ICON_DETECT_ABORT = 13;
  int IMAGE_CAPTION_EVENT_OCR_ABORT = 14;
  int IMAGE_CAPTION_EVENT_INSTALL_LIB_REQUEST = 15;
  int IMAGE_CAPTION_EVENT_INSTALL_LIB_DENY = 16;
  int IMAGE_CAPTION_EVENT_INSTALL_LIB_SUCCESS = 17;
  int IMAGE_CAPTION_EVENT_INSTALL_LIB_FAIL = 18;
  int IMAGE_CAPTION_EVENT_UNINSTALL_LIB_REQUEST = 19;
  int IMAGE_CAPTION_EVENT_UNINSTALL_LIB_DENY = 20;
  int IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_PERFORM = 21; // Deprecated.
  int IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_SUCCEED = 22;
  int IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_NO_RESULT = 23;
  int IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_FAIL = 24;
  int IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_ABORT = 25;
  int IMAGE_DESCRIBE_EVENT_INSTALL_LIB_REQUEST = 26;
  int IMAGE_DESCRIBE_EVENT_INSTALL_LIB_DENY = 27;
  int IMAGE_DESCRIBE_EVENT_INSTALL_LIB_SUCCESS = 28;
  int IMAGE_DESCRIBE_EVENT_INSTALL_LIB_FAIL = 29;
  int IMAGE_DESCRIBE_EVENT_UNINSTALL_LIB_REQUEST = 30;
  int IMAGE_DESCRIBE_EVENT_UNINSTALL_LIB_DENY = 31;
  int IMAGE_DESCRIBE_EVENT_QUALITY_LEVEL_HIGH = 32;
  int IMAGE_DESCRIBE_EVENT_QUALITY_LEVEL_MIDDLE = 33;
  int IMAGE_DESCRIBE_EVENT_QUALITY_LEVEL_LOW = 34;
  int IMAGE_CAPTION_EVENT_CANNOT_PERFORM_WHEN_SCREEN_HIDDEN = 35;
  int IMAGE_CAPTION_EVENT_SCHEDULE_SCREENSHOT_CAPTURE_FAILURE = 36;

  // All means it contains both icon detect and image describe libs.
  int ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_REQUEST = 37;
  int ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_DENY = 38;
  int ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_SUCCESS = 39;
  int ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_FAIL = 40;
  int ALL_IMAGE_CAPTION_EVENT_UNINSTALL_LIB_REQUEST = 41;
  int ALL_IMAGE_CAPTION_EVENT_UNINSTALL_LIB_DENY = 42;

  /** Defines events of image description. */
  @IntDef({
    IMAGE_CAPTION_EVENT_CAPTION_REQUEST,
    IMAGE_CAPTION_EVENT_CAPTION_REQUEST_MANUAL,
    IMAGE_CAPTION_EVENT_SCREENSHOT_FAILED,
    IMAGE_CAPTION_EVENT_IMAGE_CAPTION_CACHE_HIT,
    IMAGE_CAPTION_EVENT_ICON_DETECT_SUCCEED,
    IMAGE_CAPTION_EVENT_ICON_DETECT_NO_RESULT,
    IMAGE_CAPTION_EVENT_ICON_DETECT_FAIL,
    IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED,
    IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED_EMPTY,
    IMAGE_CAPTION_EVENT_OCR_PERFORM_FAIL,
    IMAGE_CAPTION_EVENT_ICON_DETECT_ABORT,
    IMAGE_CAPTION_EVENT_OCR_ABORT,
    IMAGE_CAPTION_EVENT_INSTALL_LIB_REQUEST,
    IMAGE_CAPTION_EVENT_INSTALL_LIB_DENY,
    IMAGE_CAPTION_EVENT_INSTALL_LIB_SUCCESS,
    IMAGE_CAPTION_EVENT_INSTALL_LIB_FAIL,
    IMAGE_CAPTION_EVENT_UNINSTALL_LIB_REQUEST,
    IMAGE_CAPTION_EVENT_UNINSTALL_LIB_DENY,
    IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_SUCCEED,
    IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_NO_RESULT,
    IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_FAIL,
    IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_ABORT,
    IMAGE_CAPTION_EVENT_CANNOT_PERFORM_WHEN_SCREEN_HIDDEN,
    IMAGE_CAPTION_EVENT_SCHEDULE_SCREENSHOT_CAPTURE_FAILURE,
    IMAGE_DESCRIBE_EVENT_INSTALL_LIB_REQUEST,
    IMAGE_DESCRIBE_EVENT_INSTALL_LIB_DENY,
    IMAGE_DESCRIBE_EVENT_INSTALL_LIB_SUCCESS,
    IMAGE_DESCRIBE_EVENT_INSTALL_LIB_FAIL,
    IMAGE_DESCRIBE_EVENT_UNINSTALL_LIB_REQUEST,
    IMAGE_DESCRIBE_EVENT_UNINSTALL_LIB_DENY,
    IMAGE_DESCRIBE_EVENT_QUALITY_LEVEL_HIGH,
    IMAGE_DESCRIBE_EVENT_QUALITY_LEVEL_MIDDLE,
    IMAGE_DESCRIBE_EVENT_QUALITY_LEVEL_LOW,
    ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_REQUEST,
    ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_DENY,
    ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_SUCCESS,
    ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_FAIL,
    ALL_IMAGE_CAPTION_EVENT_UNINSTALL_LIB_REQUEST,
    ALL_IMAGE_CAPTION_EVENT_UNINSTALL_LIB_DENY
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface ImageCaptionEventId {}

  default void onImageCaptionEvent(@ImageCaptionEventId int event) {}

  /** Defines statistics entries for multiple downloaders (from TalkBack menu) */
  public enum ImageCaptionLogKeys {
    ICON_DETECTION(
        IMAGE_CAPTION_EVENT_INSTALL_LIB_SUCCESS,
        IMAGE_CAPTION_EVENT_INSTALL_LIB_FAIL,
        IMAGE_CAPTION_EVENT_INSTALL_LIB_REQUEST,
        IMAGE_CAPTION_EVENT_INSTALL_LIB_DENY,
        IMAGE_CAPTION_EVENT_UNINSTALL_LIB_REQUEST,
        IMAGE_CAPTION_EVENT_UNINSTALL_LIB_DENY),
    IMAGE_DESCRIPTION(
        IMAGE_DESCRIBE_EVENT_INSTALL_LIB_SUCCESS,
        IMAGE_DESCRIBE_EVENT_INSTALL_LIB_FAIL,
        IMAGE_DESCRIBE_EVENT_INSTALL_LIB_REQUEST,
        IMAGE_DESCRIBE_EVENT_INSTALL_LIB_DENY,
        IMAGE_DESCRIBE_EVENT_UNINSTALL_LIB_REQUEST,
        IMAGE_DESCRIBE_EVENT_UNINSTALL_LIB_DENY),
    ALL(
        ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_SUCCESS,
        ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_FAIL,
        ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_REQUEST,
        ALL_IMAGE_CAPTION_EVENT_INSTALL_LIB_DENY,
        ALL_IMAGE_CAPTION_EVENT_UNINSTALL_LIB_REQUEST,
        ALL_IMAGE_CAPTION_EVENT_UNINSTALL_LIB_DENY);

    public final int installSuccess;
    public final int installFail;
    public final int installRequest;
    public final int installDeny;
    public final int uninstallRequest;
    public final int uninstallDeny;

    ImageCaptionLogKeys(
        int installSuccess,
        int installFail,
        int installRequest,
        int installDeny,
        int uninstallRequest,
        int uninstallDeny) {
      this.installSuccess = installSuccess;
      this.installFail = installFail;
      this.installRequest = installRequest;
      this.installDeny = installDeny;
      this.uninstallRequest = uninstallRequest;
      this.uninstallDeny = uninstallDeny;
    }
  }

  default void onImageCaptionEventFromSettings(@ImageCaptionEventId int event) {}

  default void onGeminiOptInFromSettings(@GeminiOptInId int event, boolean serverSide) {}

  default void onGeminiAiCoreDialogAction(@AiCoreDialogActionId int event) {}

  default void onShortcutActionEvent(TalkbackAction shortcut) {}

  int TRAINING_SECTION_ONBOARDING = 1;
  int TRAINING_SECTION_TUTORIAL = 2;
  int TRAINING_SECTION_TUTORIAL_BASIC_NAVIGATION = 3;
  int TRAINING_SECTION_TUTORIAL_TEXT_EDITING = 4;
  int TRAINING_SECTION_TUTORIAL_READING_NAVIGATION = 5;
  int TRAINING_SECTION_TUTORIAL_VOICE_COMMAND = 6;
  int TRAINING_SECTION_TUTORIAL_EVERYDAY_TASKS = 7;
  int TRAINING_SECTION_TUTORIAL_PRACTICE_GESTURES = 8;
  int TRAINING_BUTTON_NEXT = 9;
  int TRAINING_BUTTON_PREVIOUS = 10;
  int TRAINING_BUTTON_CLOSE = 11;
  int TRAINING_BUTTON_TURN_OFF_TALKBACK = 12;
  int TRAINING_SECTION_TUTORIAL_WEAR = 13;

  /** Defines events of training sections. */
  @IntDef({
    TRAINING_SECTION_ONBOARDING,
    TRAINING_SECTION_TUTORIAL,
    TRAINING_SECTION_TUTORIAL_BASIC_NAVIGATION,
    TRAINING_SECTION_TUTORIAL_TEXT_EDITING,
    TRAINING_SECTION_TUTORIAL_READING_NAVIGATION,
    TRAINING_SECTION_TUTORIAL_VOICE_COMMAND,
    TRAINING_SECTION_TUTORIAL_EVERYDAY_TASKS,
    TRAINING_SECTION_TUTORIAL_PRACTICE_GESTURES,

    // Tutorial button click events
    TRAINING_BUTTON_NEXT,
    TRAINING_BUTTON_PREVIOUS,
    TRAINING_BUTTON_CLOSE,
    TRAINING_BUTTON_TURN_OFF_TALKBACK,

    // Wear specific events
    TRAINING_SECTION_TUTORIAL_WEAR,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface TrainingSectionId {}

  int GEMINI_REQUEST = 1;
  int GEMINI_SUCCESS = 2;

  /** Defines events of Gemini request & successful response. */
  @IntDef({
    GEMINI_REQUEST,
    GEMINI_SUCCESS,
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface GeminiEventId {}

  default void onGeminiEvent(
      @GeminiEventId int eventId, boolean serverSide, boolean manualRequest) {}

  int GEMINI_FAIL_APIKEY_NOT_AVAILABLE = 1;
  int GEMINI_FAIL_USER_NOT_OPT_IN = 2;
  int GEMINI_FAIL_NETWORK_UNAVAILABLE = 3;
  int GEMINI_FAIL_NO_SCREENSHOT_PROVIDED = 4;
  int GEMINI_FAIL_COMMAND_NOT_PROVIDED = 5;
  int GEMINI_FAIL_FAIL_TO_ENCODE_PICTURE = 6;
  int GEMINI_FAIL_FAIL_TO_PARSE_RESPONSE = 7;
  int GEMINI_FAIL_CONTENT_BLOCKED = 8;
  int GEMINI_FAIL_PROTOCOL_ERROR = 9;
  // User may perform a Gemini request (manually/automatically) before the previous one has done.
  // Then TalkBack will abort the previous one and issue a new Gemini.
  int GEMINI_FAIL_USER_ABORT = 10;
  // When AiCore does not equip with necessary AI-Feature yet. (Wait download complete)
  int GEMINI_FAIL_SERVICE_UNAVAILABLE = 11;

  /** Defines events of fail cases of Gemini request. */
  @IntDef({
    GEMINI_FAIL_APIKEY_NOT_AVAILABLE,
    GEMINI_FAIL_USER_NOT_OPT_IN,
    GEMINI_FAIL_NETWORK_UNAVAILABLE,
    GEMINI_FAIL_NO_SCREENSHOT_PROVIDED,
    GEMINI_FAIL_COMMAND_NOT_PROVIDED,
    GEMINI_FAIL_FAIL_TO_ENCODE_PICTURE,
    GEMINI_FAIL_FAIL_TO_PARSE_RESPONSE,
    GEMINI_FAIL_CONTENT_BLOCKED,
    GEMINI_FAIL_PROTOCOL_ERROR,
    GEMINI_FAIL_USER_ABORT,
    GEMINI_FAIL_SERVICE_UNAVAILABLE,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface GeminiFailId {}

  default void onGeminiFailEvent(@GeminiFailId int failId, boolean serverSide) {}

  // Count the times dialog popped up.
  int GEMINI_OPT_IN_SHOW_DIALOG = 1;
  // Count the times dialog dismissed by positive ack.
  int GEMINI_OPT_IN_CONSENT = 2;
  // Count the times dialog dismissed by negative ack.
  int GEMINI_OPT_IN_DISSENT = 3;

  /** Defines user selection of Opt-in Dialog. */
  @IntDef({
    GEMINI_OPT_IN_SHOW_DIALOG,
    GEMINI_OPT_IN_CONSENT,
    GEMINI_OPT_IN_DISSENT,
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface GeminiOptInId {}

  default void onGeminiOptInEvent(@GeminiOptInId int optInId, boolean serverSide) {}

  default void onScreenOverviewOptInEvent(@GeminiOptInId int optInId) {}

  /** Defines user feedback types. */
  public enum GeminiFeedbackType {
    GEMINI_FEEDBACK_NONE,
    GEMINI_FEEDBACK_THUMBS_UP,
    GEMINI_FEEDBACK_THUMBS_DOWN,
  };

  /** The Gemini conversation records with feature type, input method and feedback. */
  public class GeminiDescription {
    boolean isScreenOverview;
    GeminiFeedbackType feedbackType;

    public GeminiDescription(boolean isScreenOverview, GeminiFeedbackType feedbackType) {
      this.isScreenOverview = isScreenOverview;
      this.feedbackType = feedbackType;
    }
  }

  /** The Gemini conversation records with feature type, input method and feedback. */
  public class GeminiChatEntry {
    boolean isScreenOverview;
    GeminiFeedbackType feedbackType;
    boolean isVoiceTyping;
    int questionLength;

    public GeminiChatEntry(
        boolean isScreenOverview,
        GeminiFeedbackType feedbackType,
        boolean isVoiceTyping,
        int questionLength) {
      this.isScreenOverview = isScreenOverview;
      this.feedbackType = feedbackType;
      this.isVoiceTyping = isVoiceTyping;
      this.questionLength = questionLength;
    }
  }

  default void onGeminiChatEntry(GeminiDescription description, List<GeminiChatEntry> chatList) {}

  int AI_CORE_AI_FEATURE_DOWNLOAD_REQUEST = 1;
  int AI_CORE_AI_FEATURE_DOWNLOAD_ACCEPT = 2;
  int AI_CORE_UPDATE_REQUEST = 3;
  int AI_CORE_UPDATE_ACCEPT = 4;
  int AI_CORE_ASTREA_UPDATE_REQUEST = 5;
  int AI_CORE_ASTREA_UPDATE_ACCEPT = 6;

  /** Defines user selection of Opt-in Dialog. */
  @IntDef({
    AI_CORE_AI_FEATURE_DOWNLOAD_REQUEST,
    AI_CORE_AI_FEATURE_DOWNLOAD_ACCEPT,
    AI_CORE_UPDATE_REQUEST,
    AI_CORE_UPDATE_ACCEPT,
    AI_CORE_ASTREA_UPDATE_REQUEST,
    AI_CORE_ASTREA_UPDATE_ACCEPT,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface AiCoreDialogActionId {}

  default void setActorState(ActorState actorState) {}

  @VisibleForTesting
  default PeriodicLogSender testing_getPeriodicLogSender() {
    return null;
  }

  /** Provides the data that wants to upload periodically */
  public interface PeriodicDataProvider {
    /** Returns the size of speech cache in MB. */
    long getSpeechCacheSizeMb();

    /** Returns the counts of windows that have been traversed and cached. */
    long getSpeechCacheWindowCounts();
  }
}
