/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.android.accessibility.talkback.preference.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.keyboard.KeyCombo;
import com.google.android.accessibility.talkback.keyboard.KeyComboManager;
import com.google.android.accessibility.talkback.keyboard.KeyComboModel;
import com.google.android.accessibility.talkback.preference.PreferencesActivityUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.ServiceKeyEventListener;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.accessibility.utils.material.MaterialComponentUtils;
import com.google.common.collect.ImmutableSet;

/**
 * A {@link Preference} which contains two dialogs, setUpKeyComboDialog and keyAlreadyInUseDialog.
 * SetUpKeyComboDialog is for all keyboard combo assigned key, it provides a customized dialog for
 * combo assigned key setting. KeyAlreadyInUseDialog is for warning users that the input key
 * combination is already in use.
 */
public class KeyboardShortcutDialogPreference extends Preference
    implements DialogInterface.OnKeyListener, ServiceKeyEventListener {

  private static final int KEY_EVENT_SOURCE_ACTIVITY = 0;
  private static final int KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE = 1;
  private static final String ANDROID_SCHEME = "http://schemas.android.com/apk/res/android";
  private static final String SUMMARY_ATTRIBUTE = "summary";

  private KeyComboManager keyComboManager;
  private int keyEventSource = KEY_EVENT_SOURCE_ACTIVITY;
  private KeyCombo tempKeyCombo =
      new KeyCombo(
          KeyComboModel.NO_MODIFIER,
          KeyComboModel.NO_PREFIX_KEY_CODE,
          KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
          /* triggerModifierUsed= */ false);

  private A11yAlertDialogWrapper keyAlreadyInUseDialog;
  private Dialog setUpKeyComboDialog;
  private TextView keyAssignmentView;
  private TextView errorTextView;
  private TextView instructionText;
  private boolean supportedSequenceKeyInfra;
  private boolean triggerModifierUp;
  private String summary;

  /** Defines the state of the sequence key mode. */
  @IntDef({STATE_PREFIX_KEY_NOT_PRESSED, STATE_PREFIX_KEY_PRESSED})
  public @interface SequenceKeyModeState {}

  private static final int STATE_PREFIX_KEY_NOT_PRESSED = 0;
  private static final int STATE_PREFIX_KEY_PRESSED = 1;
  @SequenceKeyModeState private int currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
  private static final ImmutableSet<Integer> INVALID_SEQUENCE_KEYCOMBO_FINAL_KEYS =
      ImmutableSet.of(KeyEvent.KEYCODE_DEL, KeyEvent.KEYCODE_ESCAPE, KeyEvent.KEYCODE_ENTER);

  public KeyboardShortcutDialogPreference(
      Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(attrs);
  }

  public KeyboardShortcutDialogPreference(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(attrs);
  }

  public KeyboardShortcutDialogPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(attrs);
  }

  public KeyboardShortcutDialogPreference(Context context) {
    super(context);
    init(null);
  }

  static KeyComboManager getKeyComboManager(Context context) {
    KeyComboManager keyComboManager;
    if (TalkBackService.getInstance() != null) {
      keyComboManager = TalkBackService.getInstance().getKeyComboManager();
    } else {
      keyComboManager = KeyComboManager.create(context);
    }

    return keyComboManager;
  }

  private void init(@Nullable AttributeSet attrs) {
    if (attrs != null) {
      int summaryResId = attrs.getAttributeResourceValue(ANDROID_SCHEME, SUMMARY_ATTRIBUTE, 0);
      if (summaryResId != 0) {
        summary = getContext().getString(summaryResId);
      }
    }
    setPersistent(true);
    updateKeyComboManager();
    supportedSequenceKeyInfra = keyComboManager.isSequenceKeyInfraSupported();
    setTemporaryKeyCombo(getKeyComboForKey(getKey()));
  }

  public void updateKeyComboManager() {
    keyComboManager = getKeyComboManager(getContext());
    throwExceptionIfKeyComboManagerIsNull();
    supportedSequenceKeyInfra = keyComboManager.isSequenceKeyInfraSupported();
  }

  public void onTriggerModifierChanged() {
    setTemporaryKeyCombo(getKeyComboForKey(getKey()));

    // Ensures it's updated through the preference framework without caching any class members.
    notifyChanged();
  }

  public void setKeyCombo(KeyCombo keyCombo) {
    setTemporaryKeyCombo(keyCombo);
  }

  @Override
  public void setSummary(@Nullable CharSequence summary) {
    super.setSummary(summary);
    if (TextUtils.equals(summary, this.summary)) {
      return;
    }
    this.summary = (summary == null) ? "" : summary.toString();
  }

  @Override
  public CharSequence getSummary() {
    return TextUtils.isEmpty(summary)
        ? keyComboManager.getKeyComboStringRepresentation(this.tempKeyCombo)
        : summary;
  }

  @Override
  public void notifyChanged() {
    super.notifyChanged();
  }

  @Override
  protected void onClick() {
    super.onClick();
    showSetUpKeyComboDialog();
  }

  @Override
  protected void onPrepareForRemoval() {
    super.onPrepareForRemoval();
  }

  /** Clears current temporary key combo code. */
  private void clearTemporaryKeyCombo() {
    tempKeyCombo =
        new KeyCombo(
            KeyComboModel.NO_MODIFIER,
            KeyComboModel.NO_PREFIX_KEY_CODE,
            KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
            /* triggerModifierUsed= */ false);
  }

  /** Sets a temporary KeyCombo object. */
  private void setTemporaryKeyCombo(long keyComboCode) {
    // Create a KeyCombo object without trigger modifier to extract modifiers, prefix key code,
    // and key code from the given `keyComboCode`. Note that `triggerModifierUsed` in this object
    // won't be used and must not be used in this function.
    KeyCombo keyCombo = new KeyCombo(keyComboCode, /* triggerModifierUsed= */ false);
    int triggerModifier = getTriggerModifier();
    this.tempKeyCombo =
        new KeyCombo(
            keyCombo.getModifiers() & ~triggerModifier,
            keyCombo.getPrefixKeyCode(),
            keyCombo.getKeyCode(),
            (keyCombo.getModifiers() & triggerModifier) != 0);
  }

  /** Sets temporary key combo. */
  private void setTemporaryKeyCombo(KeyCombo keyCombo) {
    this.tempKeyCombo = keyCombo;
  }

  private int getKeyEventSourceForCurrentKeyComboModel() {
    int triggerModifier = getTriggerModifier();

    if (triggerModifier == KeyComboModel.NO_MODIFIER) {
      return KEY_EVENT_SOURCE_ACTIVITY;
    } else {
      return KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE;
    }
  }

  private int getTriggerModifier() {
    return keyComboManager.getKeyComboModel().getTriggerModifier();
  }

  private void setKeyEventSource(int keyEventSource) {
    if (this.keyEventSource == keyEventSource) {
      return;
    }

    this.keyEventSource = keyEventSource;

    if (keyEventSource == KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE) {
      keyComboManager.setKeyEventDelegate(this);
    } else {
      keyComboManager.setKeyEventDelegate(null);
    }
  }

  /** Handles key combo when fragment closes. */
  private void onSetUpKeyComboDialogClosed() {
    setTemporaryKeyCombo(getKeyComboForKey(getKey()));

    keyComboManager.setMatchKeyCombo(true);
    setKeyEventSource(KEY_EVENT_SOURCE_ACTIVITY);
  }

  /**
   * Registers the key event listener to receive key event.
   *
   * @param dialog Dialog receives key event.
   */
  private void registerDialogKeyEvent(Dialog dialog) {
    if (dialog == null) {
      return;
    }

    dialog.setOnKeyListener(this);
    setKeyEventSource(getKeyEventSourceForCurrentKeyComboModel());
  }

  @Override
  public boolean onKeyEvent(KeyEvent event, EventId eventId) {
    if (keyEventSource != KEY_EVENT_SOURCE_ACCESSIBILITY_SERVICE) {
      return false;
    }

    return onKeyEventInternal(event);
  }

  @Override
  public boolean processWhenServiceSuspended() {
    return true;
  }

  @Override
  public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
    if (keyEventSource != KEY_EVENT_SOURCE_ACTIVITY) {
      return false;
    }

    return onKeyEventInternal(event);
  }

  private boolean onKeyEventInternal(KeyEvent event) {
    if (!processKeyEvent(event)) {
      return false;
    }

    if (event.getKeyCode() == KeyEvent.KEYCODE_DEL && event.hasNoModifiers()) {
      clearTemporaryKeyCombo();
      if (supportedSequenceKeyInfra) {
        currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
      }
      updateKeyAssignmentText();
      return true;
    }

    if (supportedSequenceKeyInfra
        && (event.getModifiers() & KeyCombo.KEY_EVENT_MODIFIER_MASK) == KeyEvent.META_CTRL_ON
        && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
      this.tempKeyCombo = keyComboManager.getKeyComboModel().getDefaultKeyCombo(getKey());
      currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
      updateKeyAssignmentText();
      return true;
    }

    if (supportedSequenceKeyInfra) {
      int currentKeyCode = event.getKeyCode();
      int eventEffectiveModifiers = event.getModifiers() & KeyCombo.KEY_EVENT_MODIFIER_MASK;
      int triggerModifier = keyComboManager.getKeyComboModel().getTriggerModifier();
      if (currentSequenceKeyState == STATE_PREFIX_KEY_NOT_PRESSED) {
        boolean onlyTriggerModifierPressed = eventEffectiveModifiers == triggerModifier;
        boolean pressedPrefixKeyCombo =
            onlyTriggerModifierPressed
                && KeyCombo.SEQUENCE_KEY_COMBO_CODE_PREFIX_KEYS.contains(currentKeyCode);

        if (pressedPrefixKeyCombo) {
          this.tempKeyCombo =
              new KeyCombo(
                  KeyComboModel.NO_MODIFIER,
                  currentKeyCode,
                  KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
                  /* triggerModifierUsed= */ true);
          currentSequenceKeyState = STATE_PREFIX_KEY_PRESSED;
          triggerModifierUp = false;
        } else {
          setTemporaryKeyCombo(KeyComboManager.getKeyComboCode(event));
        }
        updateKeyAssignmentText();
        return true;
      } else if (currentSequenceKeyState == STATE_PREFIX_KEY_PRESSED) {
        if (currentKeyCode == KeyEvent.KEYCODE_UNKNOWN) {
          clearTemporaryKeyCombo();
          currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
          updateKeyAssignmentText();
          return true;
        }

        if (KeyEvent.isModifierKey(currentKeyCode)) {
          showError(
              errorTextView,
              getContext()
                  .getString(
                      R.string.invalid_modifier_key_as_third_key_message,
                      keyComboManager.getTriggerKeyUserFacingName(
                          keyComboManager.getKeyComboModel().getTriggerModifier())));
          return true;
        }

        boolean triggerIsActive = eventEffectiveModifiers == triggerModifier;
        // The combined condition for completing a sequence key assignment.
        boolean isCompletingWithoutTriggerModifier =
            eventEffectiveModifiers == KeyComboModel.NO_MODIFIER && triggerModifierUp;
        boolean isCompletingWithTriggerModifierHeld =
            triggerIsActive
                && !triggerModifierUp
                && this.tempKeyCombo.getPrefixKeyCode() != KeyComboModel.NO_PREFIX_KEY_CODE;

        if (KeyCombo.SEQUENCE_KEY_COMBO_CODE_PREFIX_KEYS.contains(currentKeyCode)
            && triggerIsActive
            && triggerModifierUp) {
          // Scenario 1: Restart sequence with a new prefix key (e.g., Action+E -> Action+R).
          this.tempKeyCombo =
              new KeyCombo(
                  KeyComboModel.NO_MODIFIER,
                  currentKeyCode,
                  KeyComboModel.KEY_COMBO_CODE_UNASSIGNED,
                  /* triggerModifierUsed= */ true);
          triggerModifierUp = false;
        } else if (isCompletingWithoutTriggerModifier || isCompletingWithTriggerModifierHeld) {
          // Scenarios 2 & 3 Combined: Complete the sequence key assignment.
          this.tempKeyCombo =
              new KeyCombo(
                  KeyComboModel.NO_MODIFIER,
                  this.tempKeyCombo.getPrefixKeyCode(),
                  currentKeyCode,
                  /* triggerModifierUsed= */ true);
          currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
        } else {
          // Scenario 4: Abort the sequence key assignment and treat the keyevent as a normal key
          // combo.
          setTemporaryKeyCombo(KeyComboManager.getKeyComboCode(event));
          currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
        }
        updateKeyAssignmentText();
      }
    } else {
      setTemporaryKeyCombo(KeyComboManager.getKeyComboCode(event));
      updateKeyAssignmentText();
    }

    return true;
  }

  private boolean processKeyEvent(KeyEvent event) {
    if (event == null) {
      return false;
    }

    if (event.getRepeatCount() > 1) {
      return false;
    }

    // Check if a key up event is trigger modifier.
    if (keyComboManager.getKeyComboModel().getTriggerModifier() == KeyEvent.META_META_ON
        && keyComboManager.isMetaKey(event.getKeyCode())
        && event.getAction() == KeyEvent.ACTION_UP) {
      triggerModifierUp = true;
    }

    //noinspection SimplifiableIfStatement
    if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
        || event.getKeyCode() == KeyEvent.KEYCODE_HOME
        || event.getKeyCode() == KeyEvent.KEYCODE_CALL
        || event.getKeyCode() == KeyEvent.KEYCODE_ENDCALL) {
      return false;
    }

    // Let the system handle the shift + tab key event and the tab key event when sequence key infra
    // is supported.
    if (supportedSequenceKeyInfra) {
      if (event.getKeyCode() == KeyEvent.KEYCODE_TAB
          && (event.getModifiers() & KeyEvent.META_SHIFT_ON) != 0) {
        return false;
      }
      if (event.getKeyCode() == KeyEvent.KEYCODE_TAB) {
        return false;
      }
    }

    // Uses Enter key to confirm the key combo change. If keyAlreadyInUseDialog shows up, it will
    // not process the button click function since onKey() will receive enter key twice.
    // keyAlreadyInUseDialog will be null when this dialog dismisses. If the sequence key infra
    // is supported, it won't save invalid key combo and only save valid key combo when the dialog
    // view is accessibility focused and the event action is ACTION_DOWN.
    if (event.hasNoModifiers()
        && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)
        && (keyAlreadyInUseDialog == null)) {
      LinearLayout dialogView = setUpKeyComboDialog.findViewById(R.id.dialog_view);
      boolean enterPressedInDialogView =
          dialogView != null
              && dialogView.isAccessibilityFocused()
              && event.getAction() == KeyEvent.ACTION_DOWN;
      if (supportedSequenceKeyInfra && !enterPressedInDialogView) {
        return false;
      }
      if (shouldAnnounceInvalidShortcutMessage()) {
        return false;
      }
      processSaveButtonClickListener();
      return false;
    }

    // Enter and Esc are used to accept/dismiss dialogs. However, the default shortcuts
    // involve Enter and Esc (with modifiers), so we should only trap Enter and Esc without
    // modifiers.
    boolean isDialogNavigation =
        event.getKeyCode() == KeyEvent.KEYCODE_ENTER
            || event.getKeyCode() == KeyEvent.KEYCODE_ESCAPE;
    if (isDialogNavigation && event.hasNoModifiers()) {
      return false;
    }

    return event.getAction() == KeyEvent.ACTION_DOWN;
  }

  /** Shows dialog if there is duplicate key assigned. */
  private void showOverrideKeyComboDialog(final String key) {
    final Preference currentActionPreference = getPreferenceManager().findPreference(key);
    if (currentActionPreference == null) {
      return;
    }

    final Preference newActionPreference = getPreferenceManager().findPreference(getKey());
    if (newActionPreference == null) {
      return;
    }

    CharSequence currentAction = currentActionPreference.getTitle();
    CharSequence newAction = newActionPreference.getTitle();
    setKeyEventSource(KEY_EVENT_SOURCE_ACTIVITY);
    showOverrideKeyComboDialog(
        currentAction,
        newAction,
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface dialog, int which) {
            if (which != DialogInterface.BUTTON_POSITIVE) {
              setKeyEventSource(getKeyEventSourceForCurrentKeyComboModel());
              return;
            }

            saveKeyCombo();
            clearKeyComboCode(key);
            notifyListener(key, getKeyComboForKey(key));
            if (keyAlreadyInUseDialog != null) {
              keyAlreadyInUseDialog.dismiss();
            }
          }
        });
  }

  private void showOverrideKeyComboDialog(
      CharSequence currentAction,
      CharSequence newAction,
      final DialogInterface.OnClickListener clickListener) {
    String message =
        getContext()
            .getString(R.string.override_keycombo_message_two_params, currentAction, newAction);
    A11yAlertDialogWrapper.Builder builder =
        A11yAlertDialogWrapper.materialDialogBuilder(getContext());
    keyAlreadyInUseDialog =
        builder
            .setTitle(R.string.override_keycombo)
            .setMessage(message)
            .setNegativeButton(
                android.R.string.cancel,
                (dialog, which) -> {
                  dialog.dismiss();
                  clickListener.onClick(dialog, which);
                })
            .setPositiveButton(
                android.R.string.ok,
                (dialog, which) -> {
                  saveKeyCombo();
                  clickListener.onClick(dialog, which);
                  if (setUpKeyComboDialog != null && setUpKeyComboDialog.isShowing()) {
                    setUpKeyComboDialog.dismiss();
                  }
                })
            .create();
    keyAlreadyInUseDialog.setOnDismissListener(
        (dialog) -> {
          setKeyEventSource(getKeyEventSourceForCurrentKeyComboModel());
          keyAlreadyInUseDialog = null;
        });
    keyAlreadyInUseDialog.show();
  }

  /** Saves key code to keyComboManager and notifies the listeners. */
  private void saveKeyCombo() {
    KeyCombo newKeyCombo = this.tempKeyCombo;
    keyComboManager.getKeyComboModel().saveKeyCombo(getKey(), newKeyCombo);
    notifyListener(getKey(), newKeyCombo);
  }

  /** Clears key combo code for the given key in keyComboManager. */
  private void clearKeyComboCode(String key) {
    keyComboManager.getKeyComboModel().clearKeyCombo(key);
  }

  private void notifyListener(String key, Object newValue) {
    Preference preference = getPreferenceManager().findPreference(key);
    if (!(preference instanceof KeyboardShortcutDialogPreference)) {
      return;
    }

    OnPreferenceChangeListener listener = preference.getOnPreferenceChangeListener();
    if (listener != null) {
      listener.onPreferenceChange(preference, newValue);
    }
  }

  private void showSetUpKeyComboDialog() {
    if (supportedSequenceKeyInfra) {
      CharSequence title = getPreferenceManager().findPreference(getKey()).getTitle();
      A11yAlertDialogWrapper wrapper =
          A11yAlertDialogWrapper.materialDialogBuilder(getContext())
              .setTitle(getContext().getString(R.string.keyboard_assignment_dialog_title, title))
              .setView(getSetUpKeyComboDialogView())
              .setPositiveButton(R.string.save_button_in_keycombo_assign_dialog, null)
              .setNegativeButton(android.R.string.cancel, null)
              .setNeutralButton(R.string.clear_button_in_keycombo_assign_dialog, null)
              .create();
      // Get the inner Dialog.
      setUpKeyComboDialog = wrapper.getDialog();
      setUpDialogListeners(wrapper);
    } else {
      setUpKeyComboDialog =
          MaterialComponentUtils.alertDialogBuilder(getContext())
              .setView(getSetUpKeyComboDialogView())
              .create();
      setUpDialogListeners(/* wrapper= */ null);
    }

    setUpKeyComboDialog.setOnDismissListener(
        keyAlreadyInUseDialog -> onSetUpKeyComboDialogClosed());
    setUpKeyComboDialog.show();
  }

  private void setUpDialogListeners(A11yAlertDialogWrapper wrapper) {
    setUpKeyComboDialog.setOnShowListener(
        (dialogInterface) -> {
          Button saveButton = null;
          Button clearButton = null;
          if (wrapper != null) {
            saveButton = wrapper.getButton(DialogInterface.BUTTON_POSITIVE);
            clearButton = wrapper.getButton(DialogInterface.BUTTON_NEUTRAL);
          } else {
            saveButton =
                ((AlertDialog) setUpKeyComboDialog).getButton(DialogInterface.BUTTON_POSITIVE);
          }

          if (saveButton != null) {
            saveButton.setOnClickListener(
                v -> {
                  if (shouldAnnounceInvalidShortcutMessage()) {
                    return;
                  }
                  processSaveButtonClickListener();
                });
            saveButton.setFocusableInTouchMode(true);
          }

          if (clearButton != null) {
            clearButton.setOnClickListener(
                v -> {
                  clearTemporaryKeyCombo();
                  updateKeyAssignmentText();
                  currentSequenceKeyState = STATE_PREFIX_KEY_NOT_PRESSED;
                });
            clearButton.setFocusableInTouchMode(true);
          }
          registerDialogKeyEvent(setUpKeyComboDialog);
        });
  }

  /**
   * When the sequence key infra is supported and set up key combo dialog exists, announces the
   * invalid shortcut message when the shortcut is invalid or the {@code currentSequenceKeyState} is
   * STATE_PREFIX_KEY_PRESSED.
   */
  private boolean shouldAnnounceInvalidShortcutMessage() {
    if (!supportedSequenceKeyInfra || setUpKeyComboDialog == null) {
      return false;
    }

    if (currentSequenceKeyState == STATE_PREFIX_KEY_PRESSED) {
      if (errorTextView != null) {
        showError(
            errorTextView,
            getContext()
                .getString(
                    R.string.keycombo_sequence_key_assignment_instruction,
                    keyComboManager.getTriggerKeyUserFacingName(
                        keyComboManager.getKeyComboModel().getTriggerModifier())));
      }
      return true;
    }
    if (!isValidKeyCombo(this.tempKeyCombo)) {
      PreferencesActivityUtils.announceText(
          getContext().getString(R.string.invalid_shortcut_message), getContext());
      return true;
    }

    return false;
  }

  /** Processes when save button is clicked. */
  private void processSaveButtonClickListener() {
    if (!isValidKeyCombo(this.tempKeyCombo)) {
      instructionText.setTextColor(Color.RED);
      PreferencesActivityUtils.announceText(instructionText.getText().toString(), getContext());
      return;
    }

    String key = getKeyForKeyCombo(this.tempKeyCombo);
    if (key == null) {
      saveKeyCombo();
      notifyChanged();
    } else if (!key.equals(getKey())) {
      showOverrideKeyComboDialog(key);
      return;
    }
    if (setUpKeyComboDialog != null) {
      setUpKeyComboDialog.dismiss();
    }
  }

  private KeyCombo getKeyComboForKey(String key) {
    throwExceptionIfKeyComboManagerIsNull();
    final KeyComboModel keyComboModel = keyComboManager.getKeyComboModel();
    final KeyCombo keyCombo = keyComboModel.getKeyComboForKey(key);
    return keyCombo != null && keyCombo.getKeyComboCode() != KeyComboModel.KEY_COMBO_CODE_UNASSIGNED
        ? keyCombo
        : new KeyCombo(KeyComboModel.KEY_COMBO_CODE_UNASSIGNED, /* triggerModifierUsed= */ false);
  }

  @Nullable
  private String getKeyForKeyCombo(KeyCombo keyCombo) {
    throwExceptionIfKeyComboManagerIsNull();
    KeyComboModel keyComboModel = keyComboManager.getKeyComboModel();
    return keyComboModel.getKeyForKeyCombo(keyCombo);
  }

  private View getSetUpKeyComboDialogView() {
    final View dialogView =
        LayoutInflater.from(getContext())
            .inflate(
                supportedSequenceKeyInfra
                    ? R.layout.keyboard_shortcut_material_dialog
                    : R.layout.keyboard_shortcut_dialog,
                /* root= */ null);
    String key = getKey();

    updateKeyComboManager();

    setTemporaryKeyCombo(getKeyComboForKey(key));
    keyAssignmentView = (TextView) dialogView.findViewById(R.id.assigned_combination);
    instructionText = (TextView) dialogView.findViewById(R.id.instruction);
    instructionText.setText(
        supportedSequenceKeyInfra
            ? getContext()
                .getString(
                    R.string.keycombo_assign_dialog_new_instruction,
                    keyComboManager.getTriggerKeyUserFacingName(
                        keyComboManager.getKeyComboModel().getTriggerModifier()))
            : keyComboManager.getKeyComboModel().getDescriptionOfEligibleKeyCombo());

    keyAssignmentView.setText(keyComboManager.getKeyComboStringRepresentation(this.tempKeyCombo));

    if (!supportedSequenceKeyInfra) {
      View clear = dialogView.findViewById(R.id.clear);
      clear.setOnClickListener(
          (View v) -> {
            instructionText.setTextColor(Color.BLACK);
            clearTemporaryKeyCombo();
            updateKeyAssignmentText();
          });
    }

    keyComboManager.setMatchKeyCombo(false);

    return dialogView;
  }

  /** Updates key assignment by getting the summary of th preference. */
  private void updateKeyAssignmentText() {
    keyAssignmentView.setText(getSummary());
    if (supportedSequenceKeyInfra) {
      updateErrorTextView();
    }
  }

  private void updateErrorTextView() {
    errorTextView = setUpKeyComboDialog.findViewById(R.id.error_message);
    if (errorTextView == null) {
      return;
    }

    // If the shortcut is invalid and the user doesn't enter sequence key mode, show the error
    // message.
    if (!isValidKeyCombo(this.tempKeyCombo)
        && currentSequenceKeyState != STATE_PREFIX_KEY_PRESSED) {
      showError(errorTextView, getContext().getString(R.string.invalid_shortcut_message));
      return;
    }

    // If the shortcut is unassigned, the error message should be hidden.
    if (this.tempKeyCombo.getKeyComboCode() == KeyComboModel.KEY_COMBO_CODE_UNASSIGNED) {
      errorTextView.setVisibility(View.GONE);
      return;
    }

    String key = getKeyForKeyCombo(this.tempKeyCombo);
    if (key == null) {
      // This is a new valid shortcut.
      errorTextView.setVisibility(View.GONE);
      return;
    }
    if (!key.equals(getKey())) {
      // This shortcut has been assigned to another action.
      handleDuplicateShortcut(errorTextView, key);
      return;
    }

    // If reassign the same shortcut to the same action, the error message
    // should be hidden.
    errorTextView.setVisibility(View.GONE);
  }

  /**
   * If sequence key infra flag is supported, the key combo code is valid in the following cases:
   *
   * <ul>
   *   <li>If user doesn't enter sequence key mode, it is a valid non-sequence key combo.
   *   <li>If user enters sequence key mode, it is a sequence key combo. It has the same valid
   *       pre-requisites as the non-sequence key combo and its key code should not be an invalid
   *       final key.
   * </ul>
   *
   * If sequence key infra is not supported, the key combo code is valid if it is a valid
   * non-sequence.
   */
  private boolean isValidKeyCombo(KeyCombo keyCombo) {
    String key = getKey();
    boolean isBrowseModeCommand =
        keyComboManager.isBrowseModeSupported()
            && keyComboManager.getBrowseModeOnlyCommands().contains(key);

    boolean eligible = keyComboManager.getKeyComboModel().isEligibleKeyCombo(keyCombo);
    if (isBrowseModeCommand && !eligible) {
      // For browse mode commands, it's eligible even without trigger modifier when it's assigned to
      // their default key combo. `isEligibleKeyCombo()` returns false only when a key combo is
      // assigned but trigger modifier is not used.
      eligible = keyCombo.equals(keyComboManager.getKeyComboModel().getDefaultKeyCombo(key));
    }

    boolean validNonSequenceKeyCombo =
        keyCombo.getKeyComboCode() != KeyComboModel.KEY_COMBO_CODE_INVALID && eligible;
    boolean sequenceKeyCombo =
        supportedSequenceKeyInfra
            && (keyCombo.getPrefixKeyCode() != KeyComboModel.NO_PREFIX_KEY_CODE);
    if (sequenceKeyCombo) {
      return validNonSequenceKeyCombo
          && !INVALID_SEQUENCE_KEYCOMBO_FINAL_KEYS.contains(keyCombo.getKeyCode());
    }
    return validNonSequenceKeyCombo;
  }

  /** Handles duplicate shortcut if the shortcut is already assigned to another action. */
  private void handleDuplicateShortcut(TextView errorTextView, String conflictingKey) {
    final Preference currentActionPreference =
        getPreferenceManager().findPreference(conflictingKey);

    if (currentActionPreference == null) {
      return;
    }

    CharSequence currentAction = currentActionPreference.getTitle();
    String message = getContext().getString(R.string.already_in_use_error_message, currentAction);
    showError(errorTextView, message);
  }

  /** Sets up the error text view and shows error message in the error text view. */
  private void showError(TextView errorTextView, String errorMessage) {
    errorTextView.setText(errorMessage);
    errorTextView.setVisibility(View.VISIBLE);
    PreferencesActivityUtils.announceText(errorMessage, getContext());
  }

  private void throwExceptionIfKeyComboManagerIsNull() {
    if (keyComboManager == null) {
      throw new IllegalStateException(
          "KeyboardShortcutDialogPreference should never appear on systems where KeyComboManager is"
              + " unavailable");
    }
  }
}
