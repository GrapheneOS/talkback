/*
 * Copyright (C) 2023 Google Inc.
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

package com.google.android.accessibility.brailleime.settings;

import static com.google.android.accessibility.braille.common.BrailleImeAction.HIDE_KEYBOARD;
import static com.google.android.accessibility.braille.common.BrailleUserPreferences.BRAILLE_SHARED_PREFS_FILENAME;
import static com.google.android.accessibility.brailleime.BrailleImeGestureAction.getActionWithSameRoot;
import static com.google.android.accessibility.brailleime.SupportedCommand.Category.TEXT_SELECTION_AND_EDITING;
import static com.google.android.accessibility.brailleime.SupportedCommand.SubCategory.LINE;
import static com.google.android.accessibility.brailleime.Utils.collapseNotificationPanel;
import static com.google.android.accessibility.brailleime.Utils.highlightTalkBackSettings;
import static com.google.android.accessibility.brailleime.settings.BrailleImeGestureActivity.CATEGORY;
import static com.google.common.collect.ImmutableList.toImmutableList;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.Pair;
import android.view.ContextThemeWrapper;
import androidx.annotation.StringRes;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.accessibility.braille.common.BrailleCommonTalkBackSpeaker;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.interfaces.TalkBackForBrailleIme;
import com.google.android.accessibility.braille.interfaces.TalkBackForBrailleIme.ServiceStatus;
import com.google.android.accessibility.brailleime.BrailleImeGestureAction;
import com.google.android.accessibility.brailleime.BrailleImeGestureController;
import com.google.android.accessibility.brailleime.BrailleImeLog;
import com.google.android.accessibility.brailleime.BrailleImeVibrator;
import com.google.android.accessibility.brailleime.CustomGestureView.CustomGestureCallback;
import com.google.android.accessibility.brailleime.FeatureFlagReader;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.brailleime.SupportedCommand;
import com.google.android.accessibility.brailleime.SupportedCommand.Category;
import com.google.android.accessibility.brailleime.SupportedCommand.SubCategory;
import com.google.android.accessibility.brailleime.Utils;
import com.google.android.accessibility.brailleime.dialog.ChangeGestureDialog;
import com.google.android.accessibility.brailleime.dialog.CustomGestureDialog;
import com.google.android.accessibility.brailleime.dialog.CustomGestureTalkBackOffDialog;
import com.google.android.accessibility.brailleime.dialog.CustomGestureTooFewTouchPointsDialog;
import com.google.android.accessibility.brailleime.dialog.MirroredGestureDialog;
import com.google.android.accessibility.brailleime.dialog.MirroredGestureDialog.ButtonClickListener;
import com.google.android.accessibility.brailleime.dialog.ResetToDefaultDialog;
import com.google.android.accessibility.brailleime.input.DotHoldSwipe;
import com.google.android.accessibility.brailleime.input.Gesture;
import com.google.android.accessibility.brailleime.input.Swipe;
import com.google.android.accessibility.brailleime.input.UnassignedGesture;
import com.google.android.accessibility.brailleime.keyboardview.AccessibilityOverlayKeyboardView;
import com.google.android.accessibility.brailleime.keyboardview.KeyboardView;
import com.google.android.accessibility.brailleime.keyboardview.KeyboardView.KeyboardViewCallback;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.preference.PreferencesActivity;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/** An activity for showing BrailleIme gesture commands. */
public class BrailleImeGestureCommandActivity extends PreferencesActivity {
  private static TalkBackForBrailleIme talkBackForBrailleIme;

  /** TalkBack invokes this to provide us with the TalkBackForBrailleIme instance. */
  public static void initialize(TalkBackForBrailleIme talkBackForBrailleIme) {
    BrailleImeGestureCommandActivity.talkBackForBrailleIme = talkBackForBrailleIme;
  }

  @Override
  protected PreferenceFragmentCompat createPreferenceFragment() {
    return new BrailleImeGestureCommandFragment();
  }

  /** Fragment that holds the braille keyboard gesture command preference. */
    public static class BrailleImeGestureCommandFragment extends PreferenceFragmentCompat {
    private static final String TAG = "BrailleImeGestureCommandFragment";
    private static final Duration CUSTOM_GESTURE_TIMEOUT = Duration.ofSeconds(30);
    private static final UnassignedGesture UNASSIGNED_GESTURE = new UnassignedGesture();
    private static final Duration PERFORM_GESTURE_REPLAY_INTERVAL = Duration.ofSeconds(15);
    private static final int MSG_GESTURE_TIMEOUT = 1;
    private static final int MSG_PERFORM_GESTURE_REPLAY = 2;
    private KeyboardView keyboardView;
    private CustomGestureDialog customGesturedialog;
    private ChangeGestureDialog changeGestureDialog;
    private CustomGestureTalkBackOffDialog customGestureTalkBackOffDialog;
    private CustomGestureTooFewTouchPointsDialog customGestureTooFewTouchPointsDialog;
    private ResetToDefaultDialog resetToDefaultDialog;
    private MirroredGestureDialog mirroredGestureDialog;
    private BrailleImeAction brailleImeAction;
    private Gesture assignedGesture;
    private Handler handler;
    private int orientation;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
      getPreferenceManager().setSharedPreferencesName(BRAILLE_SHARED_PREFS_FILENAME);
      Category category = (Category) getActivity().getIntent().getSerializableExtra(CATEGORY);
      PreferenceSettingsUtils.addPreferencesFromResource(this, R.xml.brailleime_gesture_commands);
      getActivity().setTitle(category.getTitle(getResources()));

      String description = category.getDescription(getResources());
      Preference descriptionPreference =
          findPreference(getString(R.string.pref_key_brailleime_action_category_description));
      if (TextUtils.isEmpty(description)) {
        getPreferenceScreen().removePreference(descriptionPreference);
      } else {
        descriptionPreference.setSummary(description);
      }

      for (SubCategory subCategory : category.getSubCategories()) {
        if (category == TEXT_SELECTION_AND_EDITING && subCategory == LINE) {
          // TODO: As the text selection for line granularity movement does not work,
          // we mask off the preference of selecting text by line.
          continue;
        }
        String subCategoryTitle = subCategory.getName(getResources());
        PreferenceCategory preferenceCategory = null;
        if (!TextUtils.isEmpty(subCategoryTitle)) {
          preferenceCategory = new PreferenceCategory(getContext());
          preferenceCategory.setTitle(subCategory.getName(getResources()));
          getPreferenceScreen().addPreference(preferenceCategory);
        }
        ImmutableList<SupportedCommand> filteredCommands =
            SupportedCommand.getSupportedCommands(getContext()).stream()
                .filter(
                    (SupportedCommand supportedCommand) ->
                        supportedCommand.getCategory() == category
                            && supportedCommand.getSubCategory() == subCategory)
                .collect(toImmutableList());
        for (SupportedCommand command : filteredCommands) {
          Preference preference = new Preference(getContext());
          preference.setTitle(command.getActionDescription(getContext()));
          preference.setSummary(command.getGestureDescription(getContext()));
          // setEnabled is to gray out preference. In older design, it's not gray out.
          preference.setEnabled(
              !FeatureFlagReader.useGestureCustomization(getContext()) || command.isEditable());
          BrailleImeAction action = command.getBrailleImeAction();
          preference.setKey(action.name());
          preference.setOnPreferenceClickListener(
              clickedPreference -> {
                brailleImeAction = action;
                customGesturedialog.setAction(action);
                customGesturedialog.show();
                return true;
              });
          if (preferenceCategory != null) {
            preferenceCategory.addPreference(preference);
          } else {
            getPreferenceScreen().addPreference(preference);
          }
        }
        handler =
            new Handler(
                msg -> {
                  return switch (msg.what) {
                    case MSG_GESTURE_TIMEOUT -> {
                      showNoGesturePerformedHint();
                      yield true;
                    }
                    case MSG_PERFORM_GESTURE_REPLAY -> {
                      BrailleCommonTalkBackSpeaker.getInstance().speak(getPerformGestureHint());
                      yield true;
                    }
                    default -> false;
                  };
                });
        orientation = getResources().getConfiguration().orientation;
        keyboardView = new AccessibilityOverlayKeyboardView(getContext(), keyboardViewCallback);
        customGesturedialog = new CustomGestureDialog(getContext(), customGestureDialogCallback);
        changeGestureDialog = new ChangeGestureDialog(getContext(), changeGestureDialogCallback);
        customGestureTalkBackOffDialog =
            new CustomGestureTalkBackOffDialog(getContext(), talkbackOffDialogCallback);
        customGestureTooFewTouchPointsDialog =
            new CustomGestureTooFewTouchPointsDialog(getContext());
        resetToDefaultDialog = new ResetToDefaultDialog(getContext(), resetToDefaultDialogCallback);
        mirroredGestureDialog =
            new MirroredGestureDialog(getContext(), mirroredGestureDialogCallback);
      }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
      if (orientation != newConfig.orientation) {
        orientation = newConfig.orientation;
        keyboardView.onOrientationChanged(newConfig.orientation);
      }
    }

    @Override
    public void onStop() {
      super.onStop();
      BrailleImeLog.d(TAG, "onStop");
      changeGestureDialog.dismiss();
      customGesturedialog.dismiss();
      customGestureTalkBackOffDialog.dismiss();
      customGestureTooFewTouchPointsDialog.dismiss();
      resetToDefaultDialog.dismiss();
      assignedGesture = null;
      keyboardView.tearDown();
      handler.removeCallbacksAndMessages(null);
    }

    private void addInputView() {
      if (keyboardView.isViewContainerCreated()) {
        return;
      }
      keyboardView.setWindowManager(talkBackForBrailleIme.getWindowManager());
      keyboardView.createViewContainer();
      keyboardView.createAndAddCustomGestureView(customGestureCallback);
      showPerformGestureHint();
      handler.sendEmptyMessageDelayed(MSG_GESTURE_TIMEOUT, CUSTOM_GESTURE_TIMEOUT.toMillis());
      handler.sendEmptyMessageDelayed(
          MSG_PERFORM_GESTURE_REPLAY, PERFORM_GESTURE_REPLAY_INTERVAL.toMillis());
    }

    private void setUpGesture(Gesture gesture) {
      if (assignedGesture == null) {
        if (isEditable(gesture)) {
          assignedGesture = gesture;
        }
        showGestureResultHint(gesture);
      } else if (assignedGesture.equals(gesture)) {
        assignedGesture = null;
        keyboardView.tearDown();
        removeGestureAndUpdatePreferenceSummary(gesture);
        writeGestureAction(brailleImeAction, ImmutableList.of(gesture.getId()));
        updatePreferenceSummary(brailleImeAction);
        updateMirroredGesture(gesture);
      } else {
        assignedGesture = null;
        showGestureNotMatchHint();
      }
    }

    private void writeGestureAction(BrailleImeAction action, List<String> gestures) {
      BrailleUserPreferences.writeGestureAction(getContext(), new Pair<>(action, gestures));
      List<BrailleImeAction> sameActions = getActionWithSameRoot(action);
      for (BrailleImeAction sameAction : sameActions) {
        BrailleUserPreferences.writeGestureAction(getContext(), new Pair<>(sameAction, gestures));
      }
    }

    private void resetGestureAction(BrailleImeAction action) {
      BrailleUserPreferences.resetGestureAction(getContext(), action);
      List<BrailleImeAction> sameActions = getActionWithSameRoot(action);
      for (BrailleImeAction sameAction : sameActions) {
        BrailleUserPreferences.resetGestureAction(getContext(), sameAction);
      }
    }

    private void removeGestureAndUpdatePreferenceSummary(Gesture gesture) {
      ImmutableList<BrailleImeAction> otherActions = getOtherAction(gesture);
      if (otherActions.isEmpty()) {
        return;
      }
      for (BrailleImeAction oldAction : otherActions) {
        List<Gesture> gestures = BrailleImeGestureAction.getGesture(getContext(), oldAction);
        gestures.remove(gesture);
        writeGestureAction(
            oldAction,
            gestures.isEmpty()
                ? ImmutableList.of(UNASSIGNED_GESTURE.getId())
                : gestures.stream().map(Gesture::getId).collect(toImmutableList()));
        updatePreferenceSummary(oldAction);
      }
    }

    private void showGestureInvalidHint() {
      String title = getString(R.string.custom_gesture_cannot_used);
      String hint =
          getString(R.string.custom_gesture_try_another, getString(R.string.custom_gesture_cancel));
      SpannableStringBuilder hintText =
          new SpannableStringBuilder().append(title).append("\n\n").append(hint);
      keyboardView.getCustomGestureView().showHint(hintText, title, hint, /* warning= */ true);
      BrailleCommonTalkBackSpeaker.getInstance().speak(hintText);
    }

    private void showPerformGestureHint() {
      String title =
          getString(
              R.string.custom_gesture_perform,
              getString(
                  R.string.custom_gesture_action,
                  brailleImeAction.getDescriptionRes(getResources())));
      String hint = getString(R.string.custom_gesture_cancel);
      SpannableStringBuilder hintText =
          new SpannableStringBuilder().append(title).append("\n\n").append(hint);
      keyboardView.getCustomGestureView().showHint(hintText, title, hint, /* warning= */ false);
      BrailleCommonTalkBackSpeaker.getInstance()
          .speak(
              getString(
                  R.string.custom_gesture_hint_announcement,
                  getLayoutMode(),
                  getPerformGestureHint()));
    }

    private void showNoGesturePerformedHint() {
      String title = getString(R.string.custom_gesture_no_gesture);
      String hint =
          getString(R.string.custom_gesture_try_another, getString(R.string.custom_gesture_cancel));
      SpannableStringBuilder hintText =
          new SpannableStringBuilder().append(title).append("\n\n").append(hint);
      keyboardView.getCustomGestureView().showHint(hintText, title, hint, /* warning= */ true);
      BrailleCommonTalkBackSpeaker.getInstance().speak(hintText);
    }

    private void showGestureNotMatchHint() {
      String title = getString(R.string.custom_gesture_no_match);
      String hint =
          getString(
              R.string.custom_gesture_perform_again, getString(R.string.custom_gesture_cancel));
      SpannableStringBuilder hintText =
          new SpannableStringBuilder().append(title).append("\n\n").append(hint);
      keyboardView.getCustomGestureView().showHint(hintText, title, hint, /* warning= */ true);
      BrailleCommonTalkBackSpeaker.getInstance().speak(hintText);
    }

    private void showGestureResultHint(Gesture gesture) {
      String title = gesture.getDescription(getResources());
      String hint = getString(R.string.custom_gesture_confirm);
      ImmutableList<BrailleImeAction> otherAction = getOtherAction(gesture);
      if (!otherAction.isEmpty()) {
        hint =
            getString(
                isEditable(gesture)
                    ? R.string.custom_gesture_conflict
                    : R.string.custom_gesture_conflict_try_another,
                otherAction.get(0).getDescriptionRes(getResources()),
                getString(R.string.custom_gesture_cancel));
      }
      SpannableStringBuilder hintText =
          new SpannableStringBuilder().append(title).append("\n\n").append(hint);
      keyboardView.getCustomGestureView().showHint(hintText, title, hint, /* warning= */ false);
      BrailleCommonTalkBackSpeaker.getInstance().speak(hintText);
    }

    private String getPerformGestureHint() {
      String performHint =
          getString(
              R.string.custom_gesture_perform, brailleImeAction.getDescriptionRes(getResources()));
      String cancelHint = getString(R.string.custom_gesture_cancel);
      return getString(R.string.custom_gesture_hint_announcement_repeat, performHint, cancelHint);
    }

    private void updateMirroredGesture(Gesture gesture) {
      if (gesture.equals(gesture.mirrorDots())) {
        showSnackBar(R.string.braille_keyboard_snackbar_change, Snackbar.LENGTH_LONG);
      } else {
        mirroredGestureDialog.setGestureAction(brailleImeAction, gesture);
        mirroredGestureDialog.show();
      }
    }

    private ImmutableList<BrailleImeAction> getOtherAction(Gesture gesture) {
      List<BrailleImeAction> brailleImeActionList =
          BrailleImeGestureAction.getAction(getContext(), gesture);
      return brailleImeActionList.stream()
          .filter(action -> !action.equals(brailleImeAction))
          .collect(toImmutableList());
    }

    private boolean closeKeyboard(Swipe swipe) {
      if (!BrailleImeGestureAction.getGesture(getContext(), HIDE_KEYBOARD).contains(swipe)) {
        return false;
      }
      keyboardView.tearDown();
      handler.removeCallbacksAndMessages(null);
      showSnackBar(R.string.braille_keyboard_snackbar_cancel, Snackbar.LENGTH_SHORT);
      customGesturedialog.show();
      return true;
    }

    private void updatePreferenceSummary(BrailleImeAction action) {
      Optional<SupportedCommand> supportedCommand =
          SupportedCommand.getSupportedCommands(getContext()).stream()
              .filter(command -> command.getBrailleImeAction().equals(action))
              .findAny();
      supportedCommand.ifPresent(
          command -> {
            Preference preference = findPreference(action.name());
            if (preference != null) {
              preference.setSummary(command.getGestureDescription(getContext()));
            }
          });
    }

    private boolean isEditable(Gesture gesture) {
      // Gestures might not be listed in supported commands, so those not in uneditable are
      // editable.
      return SupportedCommand.getSupportedCommands(getContext()).stream()
          .noneMatch(
              command ->
                  !command.isEditable()
                      && command.getBrailleImeGesture(getContext()).contains(gesture));
    }

    private String getLayoutMode() {
      return getString(
          keyboardView.getCustomGestureView().isCurrentTableTopMode()
              ? R.string.tabletop
              : R.string.screen_away);
    }

    private final CustomGestureCallback customGestureCallback =
        new CustomGestureCallback() {
          @Override
          public boolean onInvalidGesture() {
            BrailleImeLog.v(TAG, "onInvalidGesture");
            talkBackForBrailleIme.interruptSpeak();
            if (assignedGesture != null) {
              showGestureNotMatchHint();
              return true;
            }
            showGestureInvalidHint();
            return true;
          }

          @Override
          public boolean onSwipeProduced(Swipe swipe) {
            BrailleImeLog.v(TAG, "onSwipeProduced");
            talkBackForBrailleIme.interruptSpeak();
            handler.removeCallbacksAndMessages(null);
            if (closeKeyboard(swipe)) {
              return true;
            }
            setUpGesture(swipe);
            return true;
          }

          @Override
          public boolean onDotHoldAndDotSwipe(DotHoldSwipe dotHoldSwipe) {
            BrailleImeLog.v(TAG, "onDotHoldAndDotSwipe");
            talkBackForBrailleIme.interruptSpeak();
            handler.removeCallbacksAndMessages(null);
            setUpGesture(dotHoldSwipe);
            return true;
          }

          @Override
          public boolean onHoldProduced(int pointersHeldCount) {
            BrailleImeLog.v(TAG, "onHoldProduced");
            return new BrailleImeGestureController(getContext(), /* brailleImeActor= */ null)
                .performDotHoldAction(pointersHeldCount);
          }

          @Override
          public void onDetectionChanged(boolean isTabletop) {
            talkBackForBrailleIme.interruptSpeak();
            keyboardView.setTableMode(isTabletop);
            BrailleCommonTalkBackSpeaker.getInstance().speak(getLayoutMode());
          }
        };

    private final KeyboardViewCallback keyboardViewCallback =
        new KeyboardViewCallback() {
          @Override
          public void onViewReady() {
            BrailleImeLog.d(TAG, "onViewReady");
            if (talkBackForBrailleIme.isVibrationFeedbackEnabled()) {
              BrailleImeVibrator.getInstance(getContext()).enable();
            }
            talkBackForBrailleIme.onBrailleImeActivated(
                /* disableEbt= */ true,
                Utils.useImeSuppliedInputWindow(),
                // Region might be null for short time before onTalkBackResumed() is called.
                keyboardView.obtainImeViewRegion().orElse(null));
          }

          @Override
          public void onViewUpdated() {
            BrailleImeLog.d(TAG, "onViewUpdated");
          }

          @Override
          public void onViewCleared() {
            BrailleImeLog.d(TAG, "onViewCleared");
            BrailleImeVibrator.getInstance(getContext()).disable();
            if (talkBackForBrailleIme != null) {
              talkBackForBrailleIme.onBrailleImeInactivated(Utils.useImeSuppliedInputWindow());
            }
          }

          @Override
          public boolean isHideScreenMode() {
            return talkBackForBrailleIme != null && talkBackForBrailleIme.isHideScreenMode();
          }
        };

    private final CustomGestureDialog.ButtonClickListener customGestureDialogCallback =
        new CustomGestureDialog.ButtonClickListener() {
          @Override
          public void onChangeGesture() {
            if (talkBackForBrailleIme == null
                || talkBackForBrailleIme.getServiceStatus() == ServiceStatus.OFF) {
              customGestureTalkBackOffDialog.show();
            } else if (!BrailleUserPreferences.readChangeGestureIntroNeverShow(getContext())) {
              changeGestureDialog.show();
            } else if (!Utils.isMultiTouchSupported(getContext())) {
              customGestureTooFewTouchPointsDialog.show();
            } else {
              addInputView();
            }
          }

          @Override
          public void onResetToDefault() {
            resetToDefaultDialog.setAction(brailleImeAction);
            resetToDefaultDialog.show();
          }

          @Override
          public void onRemove() {
            writeGestureAction(brailleImeAction, ImmutableList.of(UNASSIGNED_GESTURE.getId()));
            updatePreferenceSummary(brailleImeAction);
            showSnackBar(R.string.braille_keyboard_snackbar_remove, Snackbar.LENGTH_LONG);
          }
        };

    private final ChangeGestureDialog.ButtonClickListener changeGestureDialogCallback =
        new ChangeGestureDialog.ButtonClickListener() {
          @Override
          public void onContinue() {
            addInputView();
          }

          @Override
          public void onNeverShown() {
            BrailleUserPreferences.writeChangeGestureIntroNeverShow(getContext());
          }
        };

    private final CustomGestureTalkBackOffDialog.ButtonClickListener talkbackOffDialogCallback =
        new CustomGestureTalkBackOffDialog.ButtonClickListener() {
          @Override
          public void onLaunchSettings() {
            // Highlight TalkBack item in Accessibility Settings upon arriving there (Pixel only).
            highlightTalkBackSettings(getContext());
            // Collapse notification panel (quick settings).
            collapseNotificationPanel(getContext());
          }
        };

    private final ResetToDefaultDialog.ButtonClickListener resetToDefaultDialogCallback =
        new ResetToDefaultDialog.ButtonClickListener() {
          @Override
          public void onReset() {
            List<Gesture> defaultGestures =
                BrailleImeGestureAction.getDefaultGesture(brailleImeAction);
            for (Gesture gesture : defaultGestures) {
              if (gesture.equals(UNASSIGNED_GESTURE)) {
                continue;
              }
              removeGestureAndUpdatePreferenceSummary(gesture);
            }
            resetGestureAction(brailleImeAction);
            updatePreferenceSummary(brailleImeAction);
            showSnackBar(R.string.reset_to_default_dialog_snackbar, Snackbar.LENGTH_LONG);
          }
        };

    private final MirroredGestureDialog.ButtonClickListener mirroredGestureDialogCallback =
        new ButtonClickListener() {
          @Override
          public void onMirrored(Gesture gesture) {
            ImmutableList<BrailleImeAction> action = getOtherAction(gesture.mirrorDots());
            if (!action.isEmpty()) {
              // Mirrored is being used.
              writeGestureAction(action.get(0), ImmutableList.of(UNASSIGNED_GESTURE.getId()));
              updatePreferenceSummary(action.get(0));
            }
            writeGestureAction(
                brailleImeAction, ImmutableList.of(gesture.getId(), gesture.mirrorDots().getId()));
            updatePreferenceSummary(brailleImeAction);
            showSnackBar(
                R.string.braille_keyboard_snackbar_change_mirrored_gesture, Snackbar.LENGTH_LONG);
          }
        };

    private void showSnackBar(@StringRes int resId, int duration) {
      Context themedContext = getContext();
      Snackbar.make(themedContext, getView(), themedContext.getString(resId), duration).show();
    }

    @VisibleForTesting
    public KeyboardView testing_getKeyboardView() {
      return keyboardView;
    }
  }
}
