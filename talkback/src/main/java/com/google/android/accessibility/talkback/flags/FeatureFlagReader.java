/*
 * Copyright (C) 2022 Google Inc.
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

package com.google.android.accessibility.talkback.flags;

import android.content.Context;
import com.google.android.accessibility.utils.FeatureSupport;

/**
 * Reader class for flags of experimental feature.
 *
 * <p>Order the functions alphabetically.
 */
public final class FeatureFlagReader {
  public static boolean allowAutomaticTurnOff(Context context) {
    return true;
  }

  public static boolean allowFocusResting(Context context) {
    return true;
  }

  public static boolean clearFocusAfterSetAccessibilityFocus(Context context) {
    return false;
  }

  public static boolean deprecateTypeAnnouncementEvent(Context context) {
    return false;
  }

  public static boolean enableAggressiveChunking(Context context) {
    return true;
  }

  public static boolean enableAnnounceBatteryState(Context context) {
    return true;
  }

  public static boolean enableAnnounceCurrentTitle(Context context) {
    return true;
  }

  public static boolean enableAnnounceCurrentTimeAndDate(Context context) {
    return true;
  }

  public static boolean enableAnnouncePhoneticPronunciation(Context context) {
    return true;
  }

  public static boolean enableAnnounceRichTextDescription(Context context) {
    return true;
  }

  public static boolean enableAnnounceTableHeader(Context context) {
    return true;
  }

  public static boolean enableAutomaticCaptioningForAllImages(Context context) {
    return true;
  }

  public static boolean enableBrowseMode(Context context) {
    return true;
  }

  public static boolean enableContainerNavigationKeyboard(Context context) {
    return true;
  }

  public static boolean enableDescribeImage(Context context) {
    return true;
  }

  public static boolean enableRefocusCurrentNodeKeyboard(Context context) {
    return true;
  }

  public static boolean enableHeadingControlLinkNativeNavigation(Context context) {
    return true;
  }

  public static boolean enableImageDescription(Context context) {
    return true;
  }

  public static boolean enableMdd(Context context) {
    return false;
  }

  public static boolean enableMediaControlHintForCall(Context context) {
    return true;
  }

  public static boolean enableDoubleClickKeyboard(Context context) {
    return false;
  }

  public static boolean enableNewKeymap(Context context) {
    return true;
  }

  public static boolean enableOnlyAnnounceChangedLiveRegionNodes(Context context) {
    return false;
  }

  public static boolean enableOnlyCtrlToStopSpeech(Context context) {
    return true;
  }

  public static boolean enableOpenTalkbackSettingsKeyboard(Context context) {
    return true;
  }

  public static boolean enableOpenTtsSettingsKeyboard(Context context) {
    return true;
  }

  public static boolean enableQuickNavigationToNonmodalAlerts(Context context) {
    return false;
  }

  public static boolean enableQuickNavigationToHunGesture(Context context) {
    return true;
  }

  public static boolean enableResetSpeechSettingsKeyboard(Context context) {
    return true;
  }

  public static boolean enableScreenOverview(Context context) {
    return true;
  }

  public static boolean enableScreenSearchNavigation(Context context) {
    return true;
  }

  public static boolean enableSequenceKeyInfra(Context context) {
    return true;
  }

  public static boolean enableShortAndLongDurationsForSpecificApps(Context context) {
    return false;
  }

  public static boolean enableShowVariousListOnScreenSearch(Context context) {
    return true;
  }

  public static boolean enableShowLearnModePageKeyboard(Context context) {
    return true;
  }

  public static boolean enableShowTutorialKeyboard(Context context) {
    return true;
  }

  public static boolean enableShowKeyboardShortcutsDialog(Context context) {
    return true;
  }

  public static boolean enableShowSpeechBubbleOverlay(Context context) {
    return true;
  }

  public static boolean enableShowTalkbackKeyboardTutorial(Context context) {
    return true;
  }

  public static boolean enableSmartBrowseMode(Context context) {
    return true;
  }

  public static boolean enableSpeechPitchKeyboard(Context context) {
    return true;
  }

  public static boolean enableSpeechRateKeyboard(Context context) {
    return true;
  }

  public static boolean enableSpeechVolumeKeyboard(Context context) {
    return true;
  }

  public static boolean enableSynchronizedFocusWithKeyboardNavigation(Context context) {
    return false;
  }

  public static boolean enableToggleBrailleOnScreenOverlay(Context context) {
    return true;
  }

  public static boolean enableToggleBrailleContractedMode(Context context) {
    return true;
  }

  public static boolean enableToggleVoiceFeedback(Context context) {
    return true;
  }

  public static boolean enableToggleSelection(Context context) {
    return true;
  }

  public static boolean focusContainedInApplicationWindow(Context context) {
    return true;
  }

  public static float gestureDoubleTapSlopMultiplier(Context context) {
    return (float) 1.0;
  }

  public static Object getGarconLibraryManifestConfig(Context context) {
    return null;
  }

  public static Object getIconDetectionLibraryManifestConfig(Context context) {
    return null;
  }

  public static double getImageDescriptionHighQualityThreshold(Context context) {
    return 0;
  }

  public static double getImageDescriptionLowQualityThreshold(Context context) {
    return 0;
  }

  public static boolean handleStateChangeInMainThread(Context context) {
    return false;
  }

  public static boolean invalidSwipeGestureEarlyDetection(Context context) {
    return false;
  }

  public static boolean splitTapEverywhere(Context context) {
    return true;
  }

  public static boolean enableSplitTapAndHold(Context context) {
    return false;
  }

  public static boolean requestStateChangeInSameThread(Context context) {
    return true;
  }

  public static boolean logEventBasedLatency(Context context) {
    return false;
  }

  public static boolean makeFabFirst(Context context) {
    return false;
  }

  public static int metricsPushPeriodInMinute(Context context) {
    return 720;
  }

  public static boolean immediatelyPushMetrics(Context context) {
    return false;
  }

  public static boolean removeDeviceLockCheck(Context context) {
    return true;
  }

  public static boolean readLineWhenMoveToAdjacentLineByUpDownKey(Context context) {
    return false;
  }

  public static boolean removeUnnecessarySpans(Context context) {
    return false;
  }

  public static boolean showExitWatermark(Context context) {
    return true;
  }

  public static boolean speedUpTouchExploreState(Context context) {
    return false;
  }

  public static boolean supportActiveSpellCheck(Context context) {
    return true;
  }

  public static boolean supportTurnOffSelectionModeAfterPerformedEditAction(Context context) {
    return true;
  }

  public static boolean supportRsbScrolling(Context context) {
    return false;
  }

  public static boolean enableToggleSoundFeedback(Context context) {
    return true;
  }

  public static boolean supportShowExitBanner(Context context) {
    return true;
  }

  public static boolean useGestureBrailleDisplayOnOff(Context context) {
    return false;
  }

  public static boolean useMultipleGestureSet(Context context) {
    return false;
  }

  public static boolean usePeriodAsSeparator(Context context) {
    return false;
  }

  public static boolean enableCheckedEventToReplaceClickEventForCheckedState(Context context) {
    return true;
  }

  public static boolean enableExpandedEventToReplaceClickEventForExpandedState(Context context) {
    return true;
  }

  public static boolean useTalkbackGestureDetection(Context context) {
    return true;
  }

  public static float getSuccessAutoScrollPercentageThreshold(Context context) {
    return 0.6f;
  }

  public static boolean enableAnnounceNotSelected(Context context) {
    return false;
  }

  public static boolean enableEarlyAnnounceForLiftToType(Context context) {
    return true;
  }

  public static boolean enableReportIssueKeyboard(Context context) {
    return true;
  }

  public static boolean enableCyclePunctuationVerbosityKeyboard(Context context) {
    return true;
  }

  public static boolean enableTableNavigation(Context context) {
    return true;
  }

  public static boolean enableRowColumnTwoGranularities(Context context) {
    return true;
  }

  public static boolean enableRowAndColumnOneGranularity(Context context) {
    return true;
  }

  public static boolean enableTextFormattingSummary(Context context) {
    return true;
  }

  public static boolean enableTextFormattingInline(Context context) {
    return true;
  }

  public static boolean enableNewJumpNavigationForClank(Context context) {
    return true;
  }

  public static boolean enableToggleKeyComboPassThroughOnce(Context context) {
    return true;
  }

  public static boolean catchIndexOutOfBounds(Context context) {
    return true;
  }

  public static boolean reduceDuplicateTextCutFeedback(Context context) {
    return true;
  }

  public static boolean cacheAndDropFrequentEvents(Context context) {
    return true;
  }

  public static boolean enableReadCurrentUrl(Context context) {
    return true;
  }

  public static boolean enableReadLinkUrl(Context context) {
    return true;
  }

  public static boolean enableKeyboardLearnModePage(Context context) {
    return true;
  }

  public static boolean enableCycleTypingEchoKeyboard(Context context) {
    return true;
  }

  public static boolean enableEmojiPostfixFeedback(Context context) {
    return true;
  }

  public static boolean enableVoiceDictationShortcut(Context context) {
    return true;
  }

  public static boolean enableSpeakDialogContent(Context context) {
    return true;
  }

  public static boolean enableAlwaysReadSelectionModeStatus(Context context) {
    return false;
  }

  public static boolean enableOpenVoiceCommands(Context context) {
    return true;
  }

  public static boolean typingFocusTimeout(Context context) {
    return FeatureSupport.supportGestureDetection();
  }

  public static boolean touchFocusTimeout(Context context) {
    return FeatureSupport.supportGestureDetection();
  }

  public static boolean hasLocalCacheCapability(Context context) {
    return true;
  }

  private FeatureFlagReader() {}
}
