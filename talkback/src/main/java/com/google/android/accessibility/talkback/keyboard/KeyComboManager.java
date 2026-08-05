/*
 * Copyright (C) 2013 The Android Open Source Project
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

import static android.content.res.Configuration.HARDKEYBOARDHIDDEN_NO;
import static android.content.res.Configuration.HARDKEYBOARDHIDDEN_UNDEFINED;
import static android.content.res.Configuration.KEYBOARDHIDDEN_YES;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BROWSE_MODE_TURNED_OFF_BY_SMART_BROWSE_MODE;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BROWSE_MODE_TURNED_OFF_BY_USER;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BROWSE_MODE_TURNED_OFF_ON_SERVICE_STOP;
import static com.google.android.accessibility.talkback.keyboard.KeyCombo.EXACT_MATCH;
import static com.google.android.accessibility.talkback.keyboard.KeyCombo.KEY_EVENT_MODIFIER_MASK;
import static com.google.android.accessibility.talkback.keyboard.KeyCombo.PARTIAL_MATCH;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH;
import static java.lang.Math.max;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.DirectionNavigationActor;
import com.google.android.accessibility.talkback.actor.FullScreenReadActor;
import com.google.android.accessibility.talkback.actor.SpeechRateAndPitchActor;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.BrowseModeLogTrigger;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorPhoneticLetters;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.gesture.GestureShortcutMapping;
import com.google.android.accessibility.talkback.monitor.BatteryMonitor;
import com.google.android.accessibility.talkback.selector.SelectorController;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivity;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.ServiceKeyEventListener;
import com.google.android.accessibility.utils.ServiceStateListener;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.WindowUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.accessibility.utils.monitor.CollectionState;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableSet;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/** Manages state related to detecting key combinations. */
public class KeyComboManager
    implements ServiceKeyEventListener, ServiceStateListener, AccessibilityEventListener {
  private static final String TAG = "KeyComboManager";
  public static final int KEYMAP_DEFAULT = R.string.default_keymap_entry_value;

  /** These KeyEvent can works independently due to handle by input frameworks. */
  private static final ImmutableSet<Integer> KEY_EVENT_INDEPENDENT_FUNCTIONAL_KEYS =
      ImmutableSet.of(
          KeyEvent.KEYCODE_DPAD_UP,
          KeyEvent.KEYCODE_DPAD_DOWN,
          KeyEvent.KEYCODE_DPAD_LEFT,
          KeyEvent.KEYCODE_DPAD_RIGHT,
          KeyEvent.KEYCODE_ENTER);

  public static final String CONCATENATION_STR = " + ";
  private static final String KEYCODE_PREFIX = "KEYCODE_";

  /** When user has pressed same key twice less than this interval, we handle them as double tap. */
  @VisibleForTesting static final Duration TIME_TO_DETECT_DOUBLE_TAP = Duration.ofMillis(300);

  /**
   * Speak options for interruptive speech feedback. This is used to interrupt any existing,
   * interruptible speech feedback.
   */
  @VisibleForTesting
  static final SpeakOptions INTERRUPTIVE_SPEAK_OPTIONS =
      SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_INTERRUPT)
          .setFlags(
              FeedbackItem.FLAG_NO_HISTORY
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
                  | FeedbackItem.FLAG_SKIP_DUPLICATE);

  /** Speak options for speech feedback that is uninterruptible by new speech. */
  @VisibleForTesting
  static final SpeakOptions UNINTERRUPTIBLE_BY_NEW_SPEECH_SPEAK_OPTIONS =
      SpeakOptions.create()
          .setQueueMode(SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)
          .setFlags(
              FeedbackItem.FLAG_NO_HISTORY
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE
                  | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_PHONE_CALL_ACTIVE
                  | FeedbackItem.FLAG_SKIP_DUPLICATE);

  /**
   * Time to wait for a key after the prefix key combo has been pressed. If no key is pressed within
   * this interval, exit the sequence key mode.
   */
  private static final Duration SEQUENCE_KEY_MODE_TIMEOUT = Duration.ofSeconds(3);

  /** Returns keyComboCode that represent keyEvent. */
  public static long getKeyComboCode(KeyEvent keyEvent) {
    if (keyEvent == null) {
      return KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
    }

    int modifier = keyEvent.getModifiers() & KEY_EVENT_MODIFIER_MASK;
    return KeyCombo.computeKeyComboCode(modifier, getConvertedKeyCode(keyEvent));
  }

  /**
   * Returns converted key code. This method converts the following key events. - Convert
   * KEYCODE_HOME with meta to KEYCODE_ENTER. - Convert KEYCODE_BACK with meta to KEYCODE_DEL.
   *
   * @param event Key event to be converted.
   * @return Converted key code.
   */
  public static int getConvertedKeyCode(KeyEvent event) {
    // We care only when meta key is pressed with.
    if ((event.getModifiers() & KeyEvent.META_META_ON) == 0) {
      return event.getKeyCode();
    }

    if (event.getKeyCode() == KeyEvent.KEYCODE_HOME) {
      return KeyEvent.KEYCODE_ENTER;
    } else if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
      return KeyEvent.KEYCODE_DEL;
    } else {
      return event.getKeyCode();
    }
  }

  /** Whether the user performed a combo during the current interaction. */
  private boolean performedCombo;

  /** Whether the user may be performing a combo and we should intercept keys. */
  private boolean hasPartialMatch;

  private final Set<Integer> currentKeysDown = new HashSet<>();
  private final Set<Integer> passedKeys = new HashSet<>();
  private final Set<Integer> keyComboPassThroughPressedKeys = new HashSet<>();

  /** Commands only work in Browse Mode. Built during initialization. */
  private final ImmutableSet<String> browseModeOnlyCommands;

  private KeyCombo currentKeyCombo = new KeyCombo();
  private long currentKeyComboTime = 0;
  private KeyCombo previousKeyCombo = new KeyCombo();
  private long previousKeyComboTime = 0;
  private int maxKeysDown = 0;

  private final Context context;
  private SharedPreferences sharedPreferences;
  private Pipeline.FeedbackReturner pipeline;
  private AccessibilityFocusMonitor accessibilityFocusMonitor;
  private boolean matchKeyCombo = true;
  private boolean browseModeEnabled = false;
  private long browseModeEnabledTimeMills = 0;
  private boolean captureTrainingCombos = false;
  private KeyComboModel keyComboModel;
  private int serviceState = SERVICE_STATE_INACTIVE;
  private ServiceKeyEventListener keyEventDelegate;
  private KeyComboMapper keyComboMapper;
  private TalkBackAnalytics analytics;
  private int hardwareKeyboardStatus = HARDKEYBOARDHIDDEN_UNDEFINED;
  private A11yAlertDialogWrapper updateModifierKeysDialog;
  private KeyComboPassThroughState keyComboPassThroughState = KeyComboPassThroughState.INACTIVE;
  private ScheduledExecutorService scheduler;
  private Future<Boolean> scheduleToOpenSearchApp;
  private NewKeymapNotificationDialog newKeymapNotificationDialog;

  private enum KeyComboPassThroughState {
    // KeyComboPassThrough is not enabled and TalkBack will act on key events as normal.
    INACTIVE,
    // User has pressed the keys assigned to the KeyComboPassThrough shortcut, but has not yet
    // released the keys.
    PENDING_PASS_THROUGH_SHORTCUT_RELEASE,
    // KeyComboPassThrough is enabled and TalkBack will pass the next key events through to the
    // system.
    ACTIVE
  }

  public static KeyComboManager create(Context context) {
    return new KeyComboManager(context);
  }

  private KeyComboManager(Context context) {
    this.context = context;
    sharedPreferences = SharedPreferencesUtils.getSharedPreferences(context);
    initializeDefaultPreferenceValues();
    keyComboModel = createKeyComboModel();
    // Create a set of commands that only work in Browse Mode.
    browseModeOnlyCommands =
        FeatureFlagReader.enableBrowseMode(context)
            ? ImmutableSet.copyOf(initializeBrowseModeOnlyCommands())
            : ImmutableSet.of();
  }

  // TODO: KeyComboManager would be separated into KeyComboManager
  // and KeyComboModelManager
  public KeyComboManager(
      AccessibilityService accessibilityService,
      Pipeline.FeedbackReturner pipeline,
      ActorState actorState,
      SelectorController selectorController,
      SpeechRateAndPitchActor speechRateAndPitchActor,
      ListMenuManager menuManager,
      FullScreenReadActor fullScreenReadActor,
      TalkBackAnalytics analytics,
      DirectionNavigationActor.StateReader stateReader,
      BatteryMonitor batteryMonitor,
      ScreenStateMonitor.State screenState,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      ProcessorPhoneticLetters processorPhoneticLetters,
      CollectionState collectionState,
      GestureShortcutMapping gestureShortcutMapping) {
    this(accessibilityService);
    this.analytics = analytics;
    hardwareKeyboardStatus =
        accessibilityService.getResources().getConfiguration().hardKeyboardHidden;
    this.pipeline = pipeline;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    keyComboMapper =
        new KeyComboMapper(
            accessibilityService,
            pipeline,
            actorState,
            selectorController,
            speechRateAndPitchActor,
            menuManager,
            fullScreenReadActor,
            stateReader,
            batteryMonitor,
            screenState,
            accessibilityFocusMonitor,
            processorPhoneticLetters,
            collectionState,
            gestureShortcutMapping);
    this.pipeline = pipeline;
    this.browseModeEnabled = false;
    this.scheduler = Executors.newScheduledThreadPool(1);
  }

  /** Terminates instances to prevent leakage. */
  public void shutdown() {
    if (hardwareKeyboardStatus == HARDKEYBOARDHIDDEN_NO) {
      analytics.onKeymapTypeUsed(keyComboModel);
      analytics.onModifierKeyUsed(keyComboModel.getTriggerModifier());
    }
    if (browseModeEnabled) {
      browseModeEnabled = false;
      // Record a Browse Mode log if Browse Mode is on when TalkBack is being turned off.
      recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_ON_SERVICE_STOP);
    }
    dismissUpdateModifierKeysDialog();
  }

  @Override
  public void onServiceStateChanged(int newState) {
    // Unfortunately, key events are lost when the TalkBackService becomes active. If a key-down
    // occurs that triggers TalkBack to resume, the corresponding key-up event will not be
    // sent, causing the partially-matched key history to become inconsistent.
    // The following method will cause the key history to be reset.
    setMatchKeyCombo(matchKeyCombo);
    serviceState = newState;
  }

  /**
   * Handles incoming key events. May intercept keys if the user seems to be performing a key combo.
   *
   * @param event The key event.
   * @return {@code true} if the key was intercepted.
   */
  @Override
  public boolean onKeyEvent(KeyEvent event, EventId eventId) {
    if (keyEventDelegate != null) {
      if (keyEventDelegate.onKeyEvent(event, eventId)) {
        return true;
      }
    }

    if (!hasPartialMatch && !performedCombo && !matchKeyCombo) {
      return false;
    }

    return switch (event.getAction()) {
      case KeyEvent.ACTION_DOWN -> onKeyDown(event);
      case KeyEvent.ACTION_MULTIPLE -> hasPartialMatch;
      case KeyEvent.ACTION_UP -> onKeyUp(event);
      default -> false;
    };
  }

  @Override
  public boolean processWhenServiceSuspended() {
    return true;
  }

  @Override
  public int getEventTypes() {
    return AccessibilityEvent.TYPE_VIEW_FOCUSED
        | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED;
  }

  /**
   * Handles incoming accessibility events. {@link KeyComboManager} handles input focus changed and
   * accessibility focus changed events only when the smart browse mode is enabled and the browse
   * mode is supported.
   *
   * @param event The accessibility event.
   * @param eventId The event ID.
   */
  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    if (!KeyComboManagerHelper.isSmartBrowseModeEnabled(context) || !isBrowseModeSupported()) {
      LogUtils.d(TAG, "Smart browse mode is not enabled, or browse mode is not supported");
      return;
    }

    switch (event.getEventType()) {
      case AccessibilityEvent.TYPE_VIEW_FOCUSED ->
          // Handle input focus changed event
          handleViewInputFocusedEvent(event, eventId);
      case AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED ->
          // Handle accessibility focus changed event
          handleViewAccessibilityFocusedEvent(event, eventId);
      default -> {}
    }
  }

  /**
   * Sets delegate for key events. If it's set, it can listen and consume key events before
   * KeyComboManager does. Sets null to remove current one.
   */
  public void setKeyEventDelegate(ServiceKeyEventListener delegate) {
    keyEventDelegate = delegate;
  }

  /**
   * Returns keymap by reading preference.
   *
   * @return key map. Returns default key map as default.
   */
  public String getKeymap() {
    return sharedPreferences.getString(
        context.getString(R.string.pref_select_keymap_key), context.getString(KEYMAP_DEFAULT));
  }

  /** Refreshes key combo model after key map changes. */
  public void refreshKeyComboModel() {
    keyComboModel = createKeyComboModel();
  }

  /** Returns key combo model. */
  public KeyComboModel getKeyComboModel() {
    return keyComboModel;
  }

  /** Set whether to process keycombo */
  public void setMatchKeyCombo(boolean value) {
    matchKeyCombo = value;
  }

  public void setCaptureTrainingCombos(boolean value) {
    captureTrainingCombos = value;
  }

  /** Returns user friendly string representations of key combo */
  public String getKeyComboStringRepresentation(KeyCombo keyCombo) {
    if (keyCombo.getKeyComboCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      return context.getString(R.string.keycombo_unassigned);
    }

    int triggerModifier = keyComboModel.getTriggerModifier();
    boolean sequenceKeyCombo =
        isSequenceKeyInfraSupported()
            && (keyCombo.getPrefixKeyCode() != KeyComboModel.NO_PREFIX_KEY_CODE);

    StringBuilder assignedShortcut = new StringBuilder();
    // Append trigger modifier if key combo code contains it.
    if (keyCombo.isTriggerModifierUsed()) {
      appendModifiers(triggerModifier, assignedShortcut);
    }
    // Append modifier except trigger modifier.
    appendModifiers(keyCombo.getModifiers(), assignedShortcut);
    // Append prefix key code if it's in sequenced key mode.
    if (sequenceKeyCombo) {
      appendKey(keyCombo.getPrefixKeyCode(), assignedShortcut);
    }
    // Append key code.
    appendKey(keyCombo.getKeyCode(), assignedShortcut);

    return assignedShortcut.toString();
  }

  /** Notifies configuration changed. */
  public void onConfigurationChanged(Configuration newConfig) {
    if (newConfig.hardKeyboardHidden != hardwareKeyboardStatus) {
      hardwareKeyboardStatus = newConfig.hardKeyboardHidden;
      if (serviceState == SERVICE_STATE_ACTIVE) {
        if (hardwareKeyboardStatus == KEYBOARDHIDDEN_YES) {
          analytics.onKeymapTypeUsed(keyComboModel);
          analytics.onModifierKeyUsed(keyComboModel.getTriggerModifier());
        } else if (hardwareKeyboardStatus == HARDKEYBOARDHIDDEN_NO
            && FeatureFlagReader.enableNewKeymap(context)) {
          maybeSwitchKeymapAndShowNewKeymapNotificationDialog();
        }
      }
    }
  }

  /**
   * Switches to the default keymap if the user was using the classic keymap or if the new keymap
   * gets disabled after its release. Also shows the new keymap notification dialog if the new
   * keymap is enabled and the user hasn't seen the dialog yet.
   */
  public void maybeSwitchKeymapAndShowNewKeymapNotificationDialog() {
    if (shouldSwitchToDefaultKeymap()) {
      // If the user switched to the new keymap after its release and the new keymap PH flag is
      // disabled, fall back to the default keymap; in this case, the new keymap notification dialog
      // will not be shown due to the new keymap flag being disabled.
      switchKeymap(R.string.default_keymap_entry_value);
      refreshKeyComboModel();
    }

    if (shouldShowNewKeymapNotificationDialog()) {
      showNewKeymapNotificationDialog();
    }
  }

  /** Returns true if browse mode is supported and it's using the new key combo model. */
  public boolean isBrowseModeSupported() {
    return FeatureFlagReader.enableBrowseMode(context) && keyComboModel instanceof NewKeyComboModel;
  }

  /**
   * Returns true if the sequence key infra is supported.
   *
   * <p>The sequence key infra is supported if the feature flag is enabled and the key combo model
   * is the new key combo model.
   */
  public boolean isSequenceKeyInfraSupported() {
    return FeatureFlagReader.enableSequenceKeyInfra(context)
        && keyComboModel instanceof NewKeyComboModel;
  }

  /** Returns true if the given key code is a meta key. */
  public boolean isMetaKey(int keyCode) {
    return keyCode == KeyEvent.KEYCODE_META_LEFT || keyCode == KeyEvent.KEYCODE_META_RIGHT;
  }

  /** Returns the user facing name of the given modifier. */
  public String getTriggerKeyUserFacingName(int modifier) {
    return switch (modifier) {
      case KeyEvent.META_ALT_ON -> context.getString(R.string.keycombo_key_modifier_alt);
      case KeyEvent.META_META_ON -> context.getString(R.string.keycombo_key_modifier_meta);
      default -> "";
    };
  }

  /** Returns true if browse mode is supported and enabled. */
  public boolean isBrowseModeEnabled() {
    return isBrowseModeSupported() && browseModeEnabled;
  }

  /** Sets whether browse mode is enabled. It creates an interruptive speech feedback. */
  public void setBrowseModeEnabled(boolean enabled) {
    setBrowseModeEnabledInternal(enabled, /* makeSpeechFeedbackInterruptive= */ true);
  }

  /**
   * Sets whether browse mode is enabled.
   *
   * @param enabled Whether to enable or disable browse mode.
   * @param makeSpeechFeedbackInterruptive Whether to make the speech feedback interruptive.
   */
  private void setBrowseModeEnabledInternal(
      boolean enabled, boolean makeSpeechFeedbackInterruptive) {
    if (!isBrowseModeSupported() || browseModeEnabled == enabled) {
      return;
    }

    browseModeEnabled = enabled;
    String browseModeStateDescription;
    int browseModeSoundResId;
    if (browseModeEnabled) {
      browseModeStateDescription = context.getString(R.string.browse_mode_on_hint);
      browseModeSoundResId = R.raw.browse_mode_on_v4_2;
      // Start the timer to track the browse mode enabled time.
      browseModeEnabledTimeMills = SystemClock.uptimeMillis();
    } else {
      browseModeStateDescription = context.getString(R.string.browse_mode_off_hint);
      browseModeSoundResId = R.raw.browse_mode_off_v4_2;
    }
    if (isOnTrainingPage()) {
      // Specify the trigger modifier used in the hint.
      SpannableStringBuilder browseModeHint = new SpannableStringBuilder();
      int triggerModifier = keyComboModel.getTriggerModifier();
      StringBuilderUtils.appendWithSeparator(
          browseModeHint,
          context.getString(
              R.string.browse_mode_double_trigger_modifier,
              getTriggerKeyUserFacingName(triggerModifier)),
          browseModeStateDescription);
      // If it's on the training page, make the speech feedback announce interruptive.
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.part()
              .sound(browseModeSoundResId)
              .speech(browseModeHint.toString(), INTERRUPTIVE_SPEAK_OPTIONS));
    } else {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.part()
              .sound(browseModeSoundResId)
              .speech(
                  browseModeStateDescription,
                  makeSpeechFeedbackInterruptive
                      ? INTERRUPTIVE_SPEAK_OPTIONS
                      : UNINTERRUPTIBLE_BY_NEW_SPEECH_SPEAK_OPTIONS));
    }
  }

  /**
   * Returns the set of commands that are only available in browse mode. The set stores their key
   * strings.
   */
  public ImmutableSet<String> getBrowseModeOnlyCommands() {
    return browseModeOnlyCommands;
  }

  // TODO: b/411392314 - Do not show the new keymap notification dialog on TV.
  private boolean shouldShowNewKeymapNotificationDialog() {
    if (!FeatureFlagReader.enableNewKeymap(context)) {
      return false;
    }

    int userNotifiedOfNewKeymap =
        SharedPreferencesUtils.getIntPref(
            sharedPreferences,
            context.getResources(),
            R.string.pref_notify_new_keymap_key,
            R.integer.pref_notify_new_keymap_value_default);
    int userNotNotifiedOfNewKeymapYet =
        context.getResources().getInteger(R.integer.pref_notify_new_keymap_value_default);

    return userNotifiedOfNewKeymap == userNotNotifiedOfNewKeymapYet
        && !(keyComboModel instanceof NewKeyComboModel);
  }

  private boolean shouldSwitchToDefaultKeymap() {
    return !FeatureFlagReader.enableNewKeymap(context)
        && !(keyComboModel instanceof DefaultKeyComboModel);
  }

  private void showNewKeymapNotificationDialog() {
    boolean shouldShowDialogForClassicKeymapUsers =
        sharedPreferences.getBoolean(
            context.getString(R.string.pref_used_classic_keymap_key),
            context.getResources().getBoolean(R.bool.pref_used_classic_keymap_default));
    if (shouldShowDialogForClassicKeymapUsers) {
      newKeymapNotificationDialog =
          new NewKeymapNotificationDialog(
              context,
              R.string.keycombo_new_keymap_notification_dialog_title_for_classic_keymap,
              R.string.keycombo_new_keymap_notification_dialog_message_for_classic_keymap);
    } else {
      newKeymapNotificationDialog =
          new NewKeymapNotificationDialog(
              context,
              R.string.keycombo_new_keymap_notification_dialog_title_for_default_keymap,
              R.string.keycombo_new_keymap_notification_dialog_message_for_default_keymap);
    }
    newKeymapNotificationDialog.showDialog();
  }

  /**
   * Switch to the given keymap. This method is only used when the classic keymap is removed and/or
   * when the new keymap is enabled.
   */
  private void switchKeymap(@StringRes int keymapEntryValueResId) {
    String keymapEntryValue = context.getString(keymapEntryValueResId);
    if (TextUtils.isEmpty(keymapEntryValue)) {
      LogUtils.w(TAG, "switchKeymap: keymapEntryValue is empty");
      return;
    }

    sharedPreferences
        .edit()
        .putString(context.getString(R.string.pref_select_keymap_key), keymapEntryValue)
        .apply();
  }

  /**
   * Make keymap default value consist with xml set up, which is the intended default keymap value.
   */
  private void initializeDefaultPreferenceValues() {
    if (sharedPreferences.contains(context.getString(R.string.pref_select_keymap_key))) {
      fallBackToDefaultKeymapIfNeeded();
      return;
    }

    // Once the new keymap is released, the new keymap will be set by default for new users.
    if (FeatureFlagReader.enableNewKeymap(context)) {
      sharedPreferences
          .edit()
          .putString(
              context.getString(R.string.pref_select_keymap_key),
              context.getString(R.string.new_keymap_entry_value))
          .apply();
    } else {
      sharedPreferences
          .edit()
          .putString(
              context.getString(R.string.pref_select_keymap_key), context.getString(KEYMAP_DEFAULT))
          .apply();
    }
  }

  private void fallBackToDefaultKeymapIfNeeded() {
    if (FeatureFlagReader.enableNewKeymap(context)) {
      return;
    }

    String keymap = getKeymap();
    if (keymap.equals(context.getString(R.string.new_keymap_entry_value))) {
      switchKeymap(KEYMAP_DEFAULT);
      refreshKeyComboModel();
      LogUtils.w(TAG, "fallBackToDefaultKeymapIfNeeded: switch to default keymap");
    }
  }

  private ImmutableSet<String> initializeBrowseModeOnlyCommands() {
    // TODO: b/380361706 - Move all properties of a command into a single structure/object instead
    // of separate sets.
    ImmutableSet.Builder<String> commands = ImmutableSet.builder();
    commands.addAll(
        ImmutableSet.of(
            // Reading granularity commands
            context.getString(R.string.keycombo_shortcut_navigate_next_character),
            context.getString(R.string.keycombo_shortcut_navigate_previous_character),
            context.getString(R.string.keycombo_shortcut_navigate_next_word),
            context.getString(R.string.keycombo_shortcut_navigate_previous_word),
            context.getString(R.string.keycombo_shortcut_navigate_next_line),
            context.getString(R.string.keycombo_shortcut_navigate_previous_line),
            // Jump commands
            context.getString(R.string.keycombo_shortcut_navigate_next_aria_landmark),
            context.getString(R.string.keycombo_shortcut_navigate_previous_aria_landmark),
            context.getString(R.string.keycombo_shortcut_navigate_next_button),
            context.getString(R.string.keycombo_shortcut_navigate_previous_button),
            context.getString(R.string.keycombo_shortcut_navigate_next_checkbox),
            context.getString(R.string.keycombo_shortcut_navigate_previous_checkbox),
            context.getString(R.string.keycombo_shortcut_navigate_next_combobox),
            context.getString(R.string.keycombo_shortcut_navigate_previous_combobox),
            context.getString(R.string.keycombo_shortcut_navigate_next_control),
            context.getString(R.string.keycombo_shortcut_navigate_previous_control),
            context.getString(R.string.keycombo_shortcut_navigate_next_edit_field),
            context.getString(R.string.keycombo_shortcut_navigate_previous_edit_field),
            context.getString(R.string.keycombo_shortcut_navigate_next_graphic),
            context.getString(R.string.keycombo_shortcut_navigate_previous_graphic),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_1),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_1),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_2),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_2),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_3),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_3),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_4),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_4),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_5),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_5),
            context.getString(R.string.keycombo_shortcut_navigate_next_heading_6),
            context.getString(R.string.keycombo_shortcut_navigate_previous_heading_6),
            context.getString(R.string.keycombo_shortcut_navigate_next_link),
            context.getString(R.string.keycombo_shortcut_navigate_previous_link),
            context.getString(R.string.keycombo_shortcut_navigate_next_list),
            context.getString(R.string.keycombo_shortcut_navigate_previous_list),
            context.getString(R.string.keycombo_shortcut_navigate_next_list_item),
            context.getString(R.string.keycombo_shortcut_navigate_previous_list_item),
            context.getString(R.string.keycombo_shortcut_navigate_next_radio),
            context.getString(R.string.keycombo_shortcut_navigate_previous_radio),
            context.getString(R.string.keycombo_shortcut_navigate_next_table),
            context.getString(R.string.keycombo_shortcut_navigate_previous_table),
            context.getString(R.string.keycombo_shortcut_navigate_next_unvisited_link),
            context.getString(R.string.keycombo_shortcut_navigate_previous_unvisited_link),
            context.getString(R.string.keycombo_shortcut_navigate_next_visited_link),
            context.getString(R.string.keycombo_shortcut_navigate_previous_visited_link),
            // Navigation commands
            context.getString(R.string.keycombo_shortcut_navigate_next_column),
            context.getString(R.string.keycombo_shortcut_navigate_previous_column),
            context.getString(R.string.keycombo_shortcut_navigate_next_column_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_previous_column_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_next_row),
            context.getString(R.string.keycombo_shortcut_navigate_previous_row),
            context.getString(R.string.keycombo_shortcut_navigate_next_row_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_previous_row_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_next_table_bounds),
            context.getString(R.string.keycombo_shortcut_navigate_previous_table_bounds)));
    // Table header reading commands.
    if (FeatureFlagReader.enableAnnounceTableHeader(context)) {
      commands.addAll(
          ImmutableSet.of(
              context.getString(R.string.keycombo_shortcut_other_announce_column_header),
              context.getString(R.string.keycombo_shortcut_other_announce_row_header)));
    }
    if (FeatureFlagReader.enableReadCurrentUrl(context)) {
      commands.add(context.getString(R.string.keycombo_shortcut_other_read_current_url));
    }
    return commands.build();
  }

  /** Creates key combo model by keymap key and return it. */
  private KeyComboModel createKeyComboModel() {
    String keymap = getKeymap();
    if (keymap.equals(context.getString(R.string.new_keymap_entry_value))) {
      return new NewKeyComboModel(context);
    } else if (keymap.equals(context.getString(R.string.classic_keymap_entry_value))) {
      // Classic keymap is not supported in 16.1 and later. If the user selected the classic keymap
      // before its deprecation, record it in the shared preferences to show the new keymap
      // notification dialog designed for classic keymap users and fall back to the default keymap.
      sharedPreferences
          .edit()
          .putBoolean(context.getString(R.string.pref_used_classic_keymap_key), true)
          .apply();
      switchKeymap(KEYMAP_DEFAULT);
    }
    return new DefaultKeyComboModel(context);
  }

  /** Appends modifier. */
  private void appendModifiers(int modifier, StringBuilder sb) {
    appendModifier(
        modifier, KeyEvent.META_ALT_ON, context.getString(R.string.keycombo_key_modifier_alt), sb);
    appendModifier(
        modifier,
        KeyEvent.META_SHIFT_ON,
        context.getString(R.string.keycombo_key_modifier_shift),
        sb);
    appendModifier(
        modifier,
        KeyEvent.META_CTRL_ON,
        context.getString(R.string.keycombo_key_modifier_ctrl),
        sb);
    appendModifier(
        modifier,
        KeyEvent.META_META_ON,
        context.getString(R.string.keycombo_key_modifier_meta),
        sb);
  }

  /** Appends string representation of target modifier if modifier contains it. */
  private void appendModifier(
      int modifier, int targetModifier, String stringRepresentation, StringBuilder sb) {
    if ((modifier & targetModifier) != 0) {
      appendPlusSignIfNotEmpty(sb);
      sb.append(stringRepresentation);
    }
  }

  /** Appends string representation of target key code. */
  private void appendKey(int keyCode, StringBuilder sb) {
    if (keyCode <= 0 || KeyEvent.isModifierKey(keyCode)) {
      return;
    }

    appendPlusSignIfNotEmpty(sb);
    switch (keyCode) {
      case KeyEvent.KEYCODE_DPAD_RIGHT ->
          sb.append(context.getString(R.string.keycombo_key_arrow_right));
      case KeyEvent.KEYCODE_DPAD_LEFT ->
          sb.append(context.getString(R.string.keycombo_key_arrow_left));
      case KeyEvent.KEYCODE_DPAD_UP -> sb.append(context.getString(R.string.keycombo_key_arrow_up));
      case KeyEvent.KEYCODE_DPAD_DOWN ->
          sb.append(context.getString(R.string.keycombo_key_arrow_down));
      case KeyEvent.KEYCODE_DEL -> sb.append(context.getString(R.string.keycombo_key_backspace));
      default -> {
        String keyCodeString = KeyEvent.keyCodeToString(keyCode);
        if (keyCodeString != null) {
          String keyCodeNoPrefix;
          if (keyCodeString.startsWith(KEYCODE_PREFIX)) {
            keyCodeNoPrefix = keyCodeString.substring(KEYCODE_PREFIX.length());
          } else {
            keyCodeNoPrefix = keyCodeString;
          }
          sb.append(keyCodeNoPrefix.replace('_', ' '));
        }
      }
    }
  }

  private void appendPlusSignIfNotEmpty(StringBuilder sb) {
    if (sb.length() > 0) {
      sb.append(CONCATENATION_STR);
    }
  }

  /** Returns true if the active window is training activity. */
  private boolean isOnTrainingPage() {
    // Compares resource ID with all child nodes on the active window if navigate-up arrow is
    // shown because the first child node is the navigate-up arrow which is no resource ID.
    AccessibilityNodeInfoCompat root = accessibilityFocusMonitor.getAccessibilityFocus(false);
    if (root == null) {
      return false;
    }

    return WindowUtils.rootChildMatchesResId(context, root.unwrap(), TrainingActivity.ROOT_RES_ID)
        && captureTrainingCombos;
  }

  private boolean onKeyDown(KeyEvent event) {
    if (serviceState != SERVICE_STATE_ACTIVE) {
      return false;
    }

    currentKeysDown.add(event.getKeyCode());
    currentKeyComboTime = event.getDownTime();

    if (isKeyComboPassThroughFeatureAvailable()) {
      // If no modifier is pressed, we know that the user for sure is not performing a shortcut and
      // passthrough should be inactive. So clear the set of pressed keys in case dangling key downs
      // didn't get a key up to ensure the state is clean.
      boolean modifierKeyPressed = isAnyModifierKeyPressed(event);
      if (!modifierKeyPressed) {
        keyComboPassThroughPressedKeys.clear();
      }

      // When KeyComboPassThroughState is enabled key downs are not consumed by TalkBack.
      if (keyComboPassThroughState == KeyComboPassThroughState.ACTIVE) {
        passedKeys.addAll(currentKeysDown);
        keyComboPassThroughPressedKeys.add(event.getKeyCode());
        return false;
      }
    }

    // Check modifier.
    int triggerModifier = keyComboModel.getTriggerModifier();
    event = convertMetaKeyCombo(triggerModifier, event);
    boolean hasModifier = triggerModifier != KeyComboModel.NO_MODIFIER;
    boolean pressedTriggerModifier =
        hasModifier && ((triggerModifier & event.getModifiers()) == triggerModifier);
    boolean startedSequenceKeyCombo =
        isSequenceKeyInfraSupported()
            && previousKeyCombo.getPrefixKeyCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
    boolean enabledBrowseMode = isBrowseModeEnabled();
    // Trigger modifier needs to be pressed for TalkBack to handle the key event unless:
    // * one of the prefix key combos in the sequenced key combos was pressed, or
    // * Browse mode is on.
    boolean triggerModifierNotPressedWhenNeeded =
        hasModifier && !pressedTriggerModifier && !startedSequenceKeyCombo && !enabledBrowseMode;
    if (triggerModifierNotPressedWhenNeeded) {
      // TalkBack may not handle the key event in this case. Add the current keys to the passed
      // keys set to avoid the key event being handled by TalkBack when not needed.
      passedKeys.addAll(currentKeysDown);
      return false;
    }

    if (startedSequenceKeyCombo) {
      // If the prefix key code has been stored in `previousKeyComboCode` when entering the
      // sequence key mode, check whether the time limit has been reached.
      if (currentKeyComboTime - previousKeyComboTime > SEQUENCE_KEY_MODE_TIMEOUT.toMillis()) {
        // The time limit has been reached. Reset the previous key combo code and continue
        // handling the key event without the sequence key mode.
        previousKeyCombo = new KeyCombo();
      } else {
        // The prefix key code has been stored in `previousKeyComboCode` when entering the
        // sequence key mode. Create and process a sequenced key combo.
        KeyCombo sequencedKeyComboCode =
            new KeyCombo(
                previousKeyCombo.getModifiers(),
                previousKeyCombo.getPrefixKeyCode(),
                getConvertedKeyCode(event),
                previousKeyCombo.isTriggerModifierUsed());
        performedCombo = matchAndPerformKeyCombo(sequencedKeyComboCode, triggerModifier);
        hasPartialMatch = false;
        // No matter whether the key combo is performed or not, we should return true to consume
        // the key event in the sequenced key combo mode.
        previousKeyCombo = new KeyCombo();
        return true;
      }
    }

    currentKeyCombo =
        new KeyCombo(
            (event.getModifiers() & KEY_EVENT_MODIFIER_MASK) & ~triggerModifier,
            KeyComboModel.NO_PREFIX_KEY_CODE,
            getConvertedKeyCode(event),
            pressedTriggerModifier);
    // If the current set of keys is a partial combo, consume the event.
    hasPartialMatch = false;

    performedCombo = matchAndPerformKeyCombo(currentKeyCombo, triggerModifier);
    if (performedCombo) {
      return true;
    }

    if (previousKeyCombo.equals(currentKeyCombo)
        && currentKeyComboTime - previousKeyComboTime < TIME_TO_DETECT_DOUBLE_TAP.toMillis()
        && currentKeyCombo.getModifiers() == KeyComboModel.NO_MODIFIER
        && currentKeyCombo.getPrefixKeyCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED
        && isMetaKey(currentKeyCombo.getKeyCode())) {
      if (isBrowseModeSupported()) {
        // If `isBrowseModeSupported()`, cancel the executor for opening the search app and toggle
        // Browse Mode if the user has pressed search key (meta key) twice within
        // `TIME_TO_DETECT_DOUBLE_TAP`. This cancellation needs to be done in `onKeyDown()` to
        // handle the case where the user double-presses and holds the search key; in this case, the
        // search app shouldn't be opened and Browse Mode should be toggled on/off.
        // When the user explicitly toggles browse mode, make the speech feedback interruptive.
        cancelOpenSearchAppExecutor();
        setBrowseModeEnabled(!browseModeEnabled);
        // Record a Browse Mode log when it's toggled off by the user.
        if (!browseModeEnabled) {
          recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_BY_USER);
        }
      } else {
        // If not `isBrowseModeSupported()`, do not handle key event if user has pressed search key
        // (meta key) twice to open search app. Reset `currentKeyCombo` to not open search app again
        // with following search key event.
        currentKeyCombo = new KeyCombo();
        passedKeys.addAll(currentKeysDown);
        return false;
      }
    }

    if (!hasPartialMatch) {
      passedKeys.addAll(currentKeysDown);
    }
    return hasPartialMatch;
  }

  private void announcePerformedShortcutAndAction(
      TalkBackPhysicalKeyboardShortcut action, KeyCombo keyCombo) {
    SpannableStringBuilder gestureAndAction = new SpannableStringBuilder();
    StringBuilderUtils.appendWithSeparator(
        gestureAndAction,
        getKeyComboStringRepresentation(keyCombo),
        action.getDescription(context.getResources()));
    pipeline.returnFeedback(
        /* eventId= */ null,
        Feedback.speech(
            gestureAndAction.toString(),
            SpeakOptions.create().setQueueMode(QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH)));
  }

  private boolean matchAndPerformKeyCombo(KeyCombo keyCombo, int triggerModifier) {
    // Check if it's a sequenced key combo.
    if (isSequenceKeyInfraSupported()) {
      if (keyCombo.isSequenceKeyPrefixCombo()) {
        // If the prefix key combo is matched, store the key code in the prefix key code field of
        // `previousKeyCombo` to indicate that the sequence key mode is entered.
        previousKeyCombo =
            new KeyCombo(
                keyCombo.getModifiers(),
                keyCombo.getKeyCode(),
                KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
                keyCombo.isTriggerModifierUsed());
        // Reset the previous key combo time to the current key combo time to handle the case that
        // the user doesn't press a key after the prefix key combo within the time limit.
        previousKeyComboTime = currentKeyComboTime;
        hasPartialMatch = true;
        return true;
      }
    }

    for (Map.Entry<String, KeyCombo> entry : keyComboModel.getKeyComboMap().entrySet()) {
      final String key = entry.getKey();
      final KeyCombo targetKeyCombo = entry.getValue();
      final int match = keyCombo.matchWith(triggerModifier, targetKeyCombo);
      if (isBrowseModeSupported() && !browseModeEnabled && browseModeOnlyCommands.contains(key)) {
        continue;
      }
      if (match == EXACT_MATCH) {
        TalkBackPhysicalKeyboardShortcut action =
            TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.getResources(), key);
        EventId eventId =
            Performance.getInstance().onKeyComboEventReceived(action.getKeyboardShortcutOrdinal());

        // Checks interrupt events if matches key combos. To prevent interrupting actions generated
        // by key combos, we should send interrupt events before performing key combos.
        interrupt(action);

        if (isKeyComboPassThroughFeatureAvailable()) {
          // If the action is the assigned KeyComboPassThroughOnce action shortcut, set the
          // state to pending so KeyComboPassThroughOnce is enabled on the next key up.
          if (action == TalkBackPhysicalKeyboardShortcut.TOGGLE_KEY_COMBO_PASS_THROUGH_ONCE) {
            setKeyComboPassThroughState(
                KeyComboPassThroughState.PENDING_PASS_THROUGH_SHORTCUT_RELEASE);
          }
        }

        // Checks if the key combo is handled by training, if so, return true to consume the key
        // event.
        if (FeatureFlagReader.enableKeyboardLearnModePage(context)
            && isOnTrainingPage()
            && !isBasicLinearNavigation(key)) {
          announcePerformedShortcutAndAction(action, keyCombo);
          return true;
        }

        performedCombo = keyComboMapper.performKeyComboAction(action, eventId);
        analytics.onKeyboardShortcutUsed(
            action, keyComboModel.getTriggerModifier(), keyCombo.getKeyComboCode());
        return true;
      }

      if (match == PARTIAL_MATCH) {
        hasPartialMatch = true;
      }
    }

    return false;
  }

  private boolean isBasicLinearNavigation(String key) {
    return key.equals(context.getString(R.string.keycombo_shortcut_navigate_next_default))
        || key.equals(context.getString(R.string.keycombo_shortcut_navigate_previous_default));
  }

  /**
   * Android converts META_META_ON + KEYCODE_ENTER and META_META_ON + KEYCODE_DEL to KEYCODE_HOME
   * and KEYCODE_BACK without META_META_ON. We recover it to the original event and add META_META_ON
   * to this key event to satisfy trigger modifier condition.
   */
  private KeyEvent convertMetaKeyCombo(int triggerModifier, KeyEvent keyEvent) {
    int keyCode = keyEvent.getKeyCode();
    if (triggerModifier == KeyEvent.META_META_ON
        && (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_BACK)
        && (currentKeysDown.contains(KeyEvent.KEYCODE_META_LEFT)
            || currentKeysDown.contains(KeyEvent.KEYCODE_META_RIGHT))) {

      // The converted KeyEvent has no value of metaState, we should add META_META_ON to satisfy
      // trigger modifier condition.
      int metaState = keyEvent.getMetaState() | KeyEvent.META_META_ON;
      switch (keyCode) {
        case KeyEvent.KEYCODE_HOME -> keyCode = KeyEvent.KEYCODE_ENTER;
        case KeyEvent.KEYCODE_BACK -> keyCode = KeyEvent.KEYCODE_DEL;
        default -> {}
      }
      return new KeyEvent(
          keyEvent.getDownTime(),
          keyEvent.getEventTime(),
          keyEvent.getAction(),
          keyCode,
          keyEvent.getRepeatCount(),
          metaState);
    }
    return keyEvent;
  }

  private void setKeyComboPassThroughState(KeyComboPassThroughState nextState) {
    if (keyComboPassThroughState == nextState) {
      return;
    }

    boolean activatingPassThrough = nextState == KeyComboPassThroughState.ACTIVE;
    boolean deactivatingPassThrough = keyComboPassThroughState == KeyComboPassThroughState.ACTIVE;
    if (activatingPassThrough) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.part()
              .sound(R.raw.chime_up)
              .speech(
                  context.getString(R.string.hint_pass_through_mode_on_for_keyboard_shortcuts),
                  INTERRUPTIVE_SPEAK_OPTIONS));
    } else if (deactivatingPassThrough) {
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.sound(R.raw.chime_down));
    }
    keyComboPassThroughState = nextState;
  }

  private boolean isAnyModifierKeyPressed(KeyEvent event) {
    int modifiers = event.getModifiers();
    // Check if any of the modifiers are set. If any are set, we return true.
    return (modifiers & KEY_EVENT_MODIFIER_MASK) != 0;
  }

  private boolean isKeyComboPassThroughFeatureAvailable() {
    return FeatureFlagReader.enableToggleKeyComboPassThroughOnce(context)
        && getKeyComboModel() instanceof NewKeyComboModel;
  }

  /**
   * Notifies the {@link KeyComboMapper} whether should interrupt or not by checking the action
   * enum.
   *
   * @param performedAction the action generating from key combos.
   */
  private void interrupt(TalkBackPhysicalKeyboardShortcut performedAction) {
    if (keyComboMapper != null) {
      keyComboMapper.interruptByKeyCombo(performedAction);
    }
  }

  private void openSearchAppAfterDelay() {
    scheduleToOpenSearchApp =
        scheduler.schedule(
            () -> {
              try {
                return pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.systemAction(
                        AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS));
              } catch (RuntimeException e) {
                LogUtils.e(TAG, "Error in openSearchAppAfterDelay(): " + e.getMessage());
                return false;
              }
            },
            // if the user doesn't press the search key again within this time, open the search app.
            TIME_TO_DETECT_DOUBLE_TAP.toMillis(),
            TimeUnit.MILLISECONDS);
  }

  private void cancelOpenSearchAppExecutor() {
    if (scheduleToOpenSearchApp != null) {
      scheduleToOpenSearchApp.cancel(false);
    }
  }

  private boolean onKeyUp(KeyEvent event) {
    if (currentKeysDown.contains(event.getKeyCode())) {
      maxKeysDown = max(maxKeysDown, currentKeysDown.size());
      if (maxKeysDown == 1 && KEY_EVENT_INDEPENDENT_FUNCTIONAL_KEYS.contains(event.getKeyCode())) {
        analytics.onKeyboardShortcutUsed(
            TalkBackPhysicalKeyboardShortcut.getActionFromInputFrameworkAction(event.getKeyCode()),
            KeyCombo.extractModifiers(getKeyComboCode(event)),
            getKeyComboCode(event));
      }
      currentKeysDown.remove(event.getKeyCode());
    }
    boolean passed = passedKeys.remove(event.getKeyCode());

    // If `isBrowseModeSupported()`, schedule to open the search app if the user has pressed search
    // key (meta key) once.
    if (isBrowseModeSupported()
        && maxKeysDown == 1
        && isMetaKey(event.getKeyCode())
        && !isEqualToLastKeyComboAndDurationLessThanThreshold()) {
      // Schedule to open search app if it's not already scheduled.
      if (scheduleToOpenSearchApp == null
          || scheduleToOpenSearchApp.isCancelled()
          || scheduleToOpenSearchApp.isDone()) {
        openSearchAppAfterDelay();
      }
    }

    if (isKeyComboPassThroughFeatureAvailable()) {
      if (keyComboPassThroughState != KeyComboPassThroughState.INACTIVE) {
        // Remove the key from the set of pressed keys to detect when the full key combo is
        // released.
        keyComboPassThroughPressedKeys.remove(event.getKeyCode());

        if (keyComboPassThroughState
                == KeyComboPassThroughState.PENDING_PASS_THROUGH_SHORTCUT_RELEASE
            && !isAnyModifierKeyPressed(event)
            && currentKeysDown.isEmpty()) {
          // After the shortcut key combo is released (including all modifiers), set the state to
          // active to await the next key
          // combo to pass through.
          setKeyComboPassThroughState(KeyComboPassThroughState.ACTIVE);
        } else if (keyComboPassThroughState == KeyComboPassThroughState.ACTIVE
            && keyComboPassThroughPressedKeys.isEmpty()) {
          // After the full key combo is released, set the state to inactive to disable pass
          // through.
          setKeyComboPassThroughState(KeyComboPassThroughState.INACTIVE);
        }
      }
    }

    if (currentKeysDown.isEmpty()
        && previousKeyCombo.getPrefixKeyCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      if (!performedCombo) {
        interrupt(TalkBackPhysicalKeyboardShortcut.ACTION_UNKNOWN);
      }
      // The interaction is over, reset the state.
      performedCombo = false;
      hasPartialMatch = false;
      previousKeyCombo = currentKeyCombo;
      previousKeyComboTime = currentKeyComboTime;
      currentKeyCombo = new KeyCombo();
      currentKeyComboTime = 0;
      maxKeysDown = 0;
      passedKeys.clear();
    }

    return !passed;
  }

  private void dismissUpdateModifierKeysDialog() {
    if (updateModifierKeysDialog != null && updateModifierKeysDialog.isShowing()) {
      updateModifierKeysDialog.dismiss();
    }
  }

  private void recordBrowseModeStateWhenOff(@BrowseModeLogTrigger int trigger) {
    if (browseModeEnabledTimeMills == 0) {
      LogUtils.w(TAG, "browseModeEnabledTimeMills is 0");
      return;
    }

    analytics.onBrowseModeStopped(
        Duration.ofMillis(SystemClock.uptimeMillis() - browseModeEnabledTimeMills), trigger);
    browseModeEnabledTimeMills = 0;
  }

  private boolean isEqualToLastKeyComboAndDurationLessThanThreshold() {
    return previousKeyCombo.equals(currentKeyCombo)
        && currentKeyComboTime - previousKeyComboTime < TIME_TO_DETECT_DOUBLE_TAP.toMillis();
  }

  // TODO: b/443789728 - Use `eventId` to track the performance of smart browse mode.
  private void handleViewAccessibilityFocusedEvent(AccessibilityEvent event, EventId eventId) {
    AccessibilityNodeInfoCompat sourceNode = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (sourceNode == null) {
      // Invalid TYPE_VIEW_ACCESSIBILITY_FOCUSED event.
      return;
    }
    // Smart browse mode turns on browse mode when an a11y focused node in a WebView. Do nothing if
    // the a11y focused node is not in a WebView or the a11y focused node falls into the conditions
    // to turn off browse mode.
    if (!AccessibilityNodeInfoUtils.isSelfOrAncestorRoleWebView(sourceNode)
        || KeyComboManagerHelper.shouldTurnOffBrowseMode(
            sourceNode, accessibilityFocusMonitor.getInputFocus())) {
      return;
    }

    // When smart browse mode turns on browse mode, don't make speech feedback interruptive.
    setBrowseModeEnabledInternal(true, /* makeSpeechFeedbackInterruptive= */ false);
  }

  // TODO: b/443789728 - Use `eventId` to track the performance of smart browse mode.
  private void handleViewInputFocusedEvent(AccessibilityEvent event, EventId eventId) {
    AccessibilityNodeInfoCompat sourceNode = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    if (sourceNode == null) {
      // Invalid TYPE_VIEW_FOCUSED event.
      return;
    }

    if (KeyComboManagerHelper.shouldTurnOffBrowseMode(sourceNode)) {
      // When smart browse mode turns off browse mode, don't make speech feedback interruptive.
      setBrowseModeEnabledInternal(false, /* makeSpeechFeedbackInterruptive= */ false);
      // Record a Browse Mode log when it's toggled off by smart browse mode.
      recordBrowseModeStateWhenOff(BROWSE_MODE_TURNED_OFF_BY_SMART_BROWSE_MODE);
      return;
    }
  }

  @VisibleForTesting
  public void testing_setKeyComboMapper(KeyComboMapper keyComboMapper) {
    this.keyComboMapper = keyComboMapper;
  }

  @VisibleForTesting
  public void testing_setKeyComboModel(KeyComboModel keyComboModel) {
    this.keyComboModel = keyComboModel;
  }

  @VisibleForTesting
  public void testing_setScheduledExecutorService(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  @VisibleForTesting
  public boolean testing_isSequenceKeyMode() {
    return previousKeyCombo.getPrefixKeyCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED;
  }

  @VisibleForTesting
  public ImmutableSet<String> testing_getBrowseModeOnlyCommands() {
    return browseModeOnlyCommands;
  }
}
