/*
 * Copyright (C) 2021 Google Inc.
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

package com.google.android.accessibility.talkback.actor;

import static com.google.android.accessibility.talkback.Feedback.Focus.Action.MUTE_NEXT_FOCUS;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.IMAGE_CAPTION_ICON_LABEL_FAILED;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.IMAGE_CAPTION_ICON_LABEL_SUCCEED;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.IMAGE_CAPTION_IMAGE_DESCRIPTION_FAILED;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.IMAGE_CAPTION_IMAGE_DESCRIPTION_SUCCEED;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.IMAGE_CAPTION_IMAGE_PROCESS_BLOCK_OVERLAY;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.IMAGE_CAPTION_OCR_FAILED;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.IMAGE_CAPTION_OCR_SUCCEED;
import static com.google.android.accessibility.talkback.PrimesController.TimerAction.LATENCY_BETWEEN_SCREENSHOT_CAPTURE_REQUEST;
import static com.google.android.accessibility.talkback.actor.ImageCaptioner.CaptionNodeType.IMAGE;
import static com.google.android.accessibility.talkback.actor.ImageCaptioner.CaptionNodeType.NONE;
import static com.google.android.accessibility.talkback.actor.ImageCaptioner.CaptionNodeType.UNLABELLED_VIEW;
import static com.google.android.accessibility.talkback.actor.gemini.GeminiFunctionUtils.GEMINI_REPEAT_OPT_IN_CONFIRMED_COUNT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GEMINI_OPT_IN_CONSENT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GEMINI_OPT_IN_DISSENT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GEMINI_OPT_IN_SHOW_DIALOG;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_CANNOT_PERFORM_WHEN_SCREEN_HIDDEN;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_CAPTION_REQUEST;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_CAPTION_REQUEST_MANUAL;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_ICON_DETECT_ABORT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_ICON_DETECT_FAIL;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_ICON_DETECT_NO_RESULT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_ICON_DETECT_SUCCEED;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_IMAGE_CAPTION_CACHE_HIT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_ABORT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_FAIL;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_NO_RESULT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_SUCCEED;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_OCR_ABORT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_OCR_PERFORM_FAIL;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED_EMPTY;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.IMAGE_CAPTION_EVENT_SCREENSHOT_FAILED;
import static com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.Requester.MENU;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionStorage.ENABLE_CACHE_MECHANISM;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.ICON_LABEL;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.IMAGE_DESCRIPTION;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.OCR;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.constructCaptionTextForAuto;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.constructCaptionTextForManually;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.getAutomaticImageCaptioningState;
import static com.google.android.accessibility.talkback.imagecaption.Request.ERROR_INSUFFICIENT_STORAGE;
import static com.google.android.accessibility.talkback.imagecaption.Request.ERROR_NETWORK_ERROR;
import static com.google.android.accessibility.talkback.imagecaption.Request.INVALID_DURATION;
import static com.google.android.accessibility.talkback.utils.FocusIndicatorUtils.getTalkBackFocusStrokeWidth;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;
import static com.google.android.accessibility.utils.output.FeedbackItem.FLAG_FORCE_FEEDBACK;
import static com.google.android.accessibility.utils.output.FeedbackItem.FLAG_NO_DEVICE_SLEEP;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH_CAN_IGNORE_INTERRUPTS;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.GeminiRequest;
import com.google.android.accessibility.talkback.Feedback.TriggerIntent.Action;
import com.google.android.accessibility.talkback.Interpretation;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.PrimesController;
import com.google.android.accessibility.talkback.PrimesController.TimerAction;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.UserInterface.UserInputEventListener;
import com.google.android.accessibility.talkback.actor.gemini.DataFieldUtils;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.ErrorReason;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.FinishReason;
import com.google.android.accessibility.talkback.actor.gemini.GeminiOptInDialog;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.ImageCaptionLogKeys;
import com.google.android.accessibility.talkback.dynamicfeature.DownloaderFactory;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.DownloadStateListener;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.Requester;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleStateManager;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.icondetection.IconAnnotationsDetectorFactory;
import com.google.android.accessibility.talkback.imagecaption.CaptionRequest;
import com.google.android.accessibility.talkback.imagecaption.CharacterCaptionRequest;
import com.google.android.accessibility.talkback.imagecaption.IconDetectionRequest;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.AutomaticImageCaptioningState;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.DownloadDialogResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.DownloadStateListenerResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.FeatureSwitchDialogResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.ImageCaptionPreferenceKeys;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionStorage;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType;
import com.google.android.accessibility.talkback.imagecaption.ImageDescriptionRequest;
import com.google.android.accessibility.talkback.imagecaption.ImageNode;
import com.google.android.accessibility.talkback.imagecaption.Request;
import com.google.android.accessibility.talkback.imagecaption.Request.ErrorCode;
import com.google.android.accessibility.talkback.imagecaption.RequestList;
import com.google.android.accessibility.talkback.imagecaption.Result;
import com.google.android.accessibility.talkback.imagecaption.ScreenshotCaptureRequest;
import com.google.android.accessibility.talkback.imagedescription.ImageDescriptionProcessor;
import com.google.android.accessibility.talkback.utils.TalkbackFeatureSupport;
import com.google.android.accessibility.utils.AccessibilityNode;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.input.WindowEventInterpreter.EventInterpretation;
import com.google.android.accessibility.utils.input.WindowEventInterpreter.WindowEventHandler;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.screenunderstanding.IconAnnotationsDetector;
import com.google.android.libraries.accessibility.utils.bitmap.BitmapUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.auto.value.AutoValue;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/** Performs image caption and manages related state. */
public class ImageCaptioner extends Handler
    implements WindowEventHandler, OnSharedPreferenceChangeListener, UserInputEventListener {

  /** These constants of Gemini feature types. */
  public static final int GEMINI_DESCRIBE_IMAGE = 0;

  public static final int GEMINI_DESCRIBE_SCREEN = 1;

  /** Defines types of Gemini features Talkback uses. */
  @IntDef({
    GEMINI_DESCRIBE_IMAGE,
    GEMINI_DESCRIBE_SCREEN,
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface GeminiFeatureType {}

  /** The type of the captioning node. */
  enum CaptionNodeType {
    /** The node doesn't need automatic image captioning. */
    NONE,
    /** The node needs automatic image captioning because it is an image. */
    IMAGE,
    /** The node needs automatic image captioning because it is unlabelled. */
    UNLABELLED_VIEW
  }

  private static final String TAG = "ImageCaptioner";
  // Only the last taking screenshot request for the focused node will be performed to ensure the
  // caption requests are able to be performed soon.
  public static final int SCREENSHOT_REQUEST_CAPACITY = 1;
  // Once the take-screenshot request is finished, a CaptionResult will be created and pending until
  // all caption requests are finished. It’s difficult to cancel the caption requests which may be
  // executing, so only take-screenshot requests are able to handle the cancellation (capacity = 1).
  public static final int CAPTION_REQUEST_CAPACITY = 10;
  private static boolean supportIconDetection = false;
  @VisibleForTesting static final int MSG_RESULT_TIMEOUT = 0;
  private static final long RESULT_MAX_WAITING_TIME_MS = 5000;

  // Copy from AccessibilityService.ACCESSIBILITY_TAKE_SCREENSHOT_REQUEST_INTERVAL_TIMES_MS.
  private static final Duration TAKE_SCREENSHOT_REQUEST_INTERVAL_TIMES =
      FeatureSupport.supportTakeScreenshot3fps() ? Duration.ofMillis(333) : Duration.ofSeconds(1);
  private static final ExecutorService executorService = Executors.newSingleThreadExecutor();

  private final AccessibilityService service;
  private Pipeline.FeedbackReturner pipeline;
  private ActorState actorState;
  private final SharedPreferences prefs;
  private final ImageCaptionStorage imageCaptionStorage;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;
  private final TalkBackAnalytics analytics;
  private final PrimesController primesController;
  @VisibleForTesting @Nullable IconAnnotationsDetector iconAnnotationsDetector;
  private boolean iconAnnotationsDetectorStarted = false;
  @Nullable private AccessibilityNodeInfoCompat queuedNode;
  private final CaptionResults captionResults;
  @VisibleForTesting @Nullable ImageDescriptionProcessor imageDescriptionProcessor;
  @VisibleForTesting boolean isImageDescriptionProcessorInitializing;
  @VisibleForTesting @Nullable Future<Boolean> initImageDescriptionProcessFuture;
  @VisibleForTesting @Nullable Future<Boolean> shutDownImageDescriptionProcessFuture;
  private final ModuleStateManager moduleStateManager;

  /** A handler for showing UI elements, like a dialog. */
  private final Handler mainHandler;

  /**
   * The unique ID for caption requests. The ID of different caption requests for a node are the
   * same.
   */
  private int requestId = 0;

  private final RequestList<ScreenshotCaptureRequest> screenshotRequests;
  private final RequestList<CharacterCaptionRequest> characterCaptionRequests =
      new RequestList<>(CAPTION_REQUEST_CAPACITY);
  private final RequestList<IconDetectionRequest> iconDetectionRequests =
      new RequestList<>(CAPTION_REQUEST_CAPACITY);
  private final RequestList<ImageDescriptionRequest> imageDescriptionRequests =
      new RequestList<>(CAPTION_REQUEST_CAPACITY);
  private GeminiOptInDialog geminiOptInDialog;
  private GeminiOptInDialog geminiNanoOptInDialog;
  private GeminiOptInDialog configImageDescriptionDialog;
  private GeminiOptInDialog screenOverviewOptInDialog;
  private AccessibilityNodeInfoCompat nodeToBeDetailDescribed;

  @VisibleForTesting
  ImageCaptioner(
      AccessibilityService service,
      RequestList<ScreenshotCaptureRequest> screenshotRequests,
      ImageCaptionStorage imageCaptionStorage,
      ModuleStateManager moduleStateManager,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      TalkBackAnalytics analytics,
      PrimesController primesController) {
    super(Looper.myLooper());
    this.service = service;
    prefs = SharedPreferencesUtils.getSharedPreferences(service);
    this.screenshotRequests = screenshotRequests;
    this.imageCaptionStorage = imageCaptionStorage;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.captionResults = new CaptionResults();
    this.analytics = analytics;
    this.primesController = primesController;
    this.moduleStateManager = moduleStateManager;
    mainHandler = new Handler(Looper.getMainLooper());
    initialize();
  }

  public ImageCaptioner(
      AccessibilityService service,
      ImageCaptionStorage imageCaptionStorage,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      TalkBackAnalytics analytics,
      PrimesController primesController) {
    super(Looper.myLooper());
    this.service = service;
    prefs = SharedPreferencesUtils.getSharedPreferences(service);
    screenshotRequests =
        new RequestList<>(SCREENSHOT_REQUEST_CAPACITY, TAKE_SCREENSHOT_REQUEST_INTERVAL_TIMES);
    this.imageCaptionStorage = imageCaptionStorage;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.captionResults = new CaptionResults();
    this.analytics = analytics;
    this.primesController = primesController;
    moduleStateManager =
        new ModuleStateManager(
            service,
            DownloaderFactory.create(service.getApplication()),
            DownloaderFactory.legacy(service.getApplication()));
    mainHandler = new Handler(Looper.getMainLooper());
    initialize();
  }

  /**
   * Gets called in TalkBack's {@code onUnlockedBootCompleted()} whenever the device completes the
   * unlocked boot.
   */
  public void onUnlockedBoot() {
    moduleStateManager.initialize(service);
  }

  private void initialize() {
    SharedPreferencesUtils.getSharedPreferences(service)
        .registerOnSharedPreferenceChangeListener(this);

    // Try to initialize icon detection and image description when TalkBack on.
    if (initIconDetection()) {
      // Ensures automatic icon detection feature is still enabled after updating.
      if (!prefs.contains(
              service.getString(
                  ImageCaptionPreferenceKeys.ICON_DETECTION.switchPreferenceKeys.get(0).switchKey))
          && moduleStateManager.isLibraryReady(ICON_LABEL)) {
        putBooleanPref(
            ImageCaptionPreferenceKeys.ICON_DETECTION.switchPreferenceKeys.get(0).switchKey, true);
      }
    } else {
      LogUtils.v(TAG, "Icon detection is not initialized in ImageCaptioner()");
    }
    if (!initImageDescription()) {
      LogUtils.v(TAG, "Image description is not initialized in ImageCaptioner()");
    }
    geminiOptInDialog =
        new GeminiOptInDialog(
            service,
            R.string.title_gemini_opt_in_dialog,
            false,
            R.string.dialog_message_detailed_ai_promotion,
            R.string.positive_button_gemini_opt_in_dialog,
            R.string.negative_button_gemini_opt_in_dialog) {
          @Override
          public void handleDialogClick(int buttonClicked) {
            switch (buttonClicked) {
              case DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE -> {
                prefs
                    .edit()
                    .putBoolean(
                        context.getString(R.string.pref_detailed_image_description_key),
                        buttonClicked == DialogInterface.BUTTON_POSITIVE)
                    .apply();
                analytics.onGeminiOptInEvent(
                    buttonClicked == DialogInterface.BUTTON_POSITIVE
                        ? GEMINI_OPT_IN_CONSENT
                        : GEMINI_OPT_IN_DISSENT,
                    /* serverSide= */ true);

                int optInCount =
                    prefs.getInt(
                        context.getString(R.string.pref_gemini_repeat_opt_in_count_key), 0);
                if (optInCount < GEMINI_REPEAT_OPT_IN_CONFIRMED_COUNT) {
                  SharedPreferencesUtils.putIntPref(
                      prefs,
                      context.getResources(),
                      R.string.pref_gemini_repeat_opt_in_count_key,
                      ++optInCount);
                }
              }
              default -> {}
            }
          }

          @Override
          public void handleDialogDismiss() {
            if (nodeToBeDetailDescribed == null) {
              return;
            }
            if (SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_detailed_image_description_key,
                R.bool.pref_detailed_image_description_default)) {
              pipeline.returnFeedback(
                  EVENT_ID_UNTRACKED,
                  Feedback.performDetailedImageCaption(nodeToBeDetailDescribed));
            }
          }
        };
    screenOverviewOptInDialog =
        new GeminiOptInDialog(
            service,
            R.string.title_gemini_screen_overview_result_dialog,
            true,
            R.string.dialog_message_screen_overview_promotion,
            R.string.positive_button_gemini_opt_in_dialog,
            -1) {
          @Override
          public void handleDialogClick(int buttonClicked) {
            switch (buttonClicked) {
              case DialogInterface.BUTTON_POSITIVE, DialogInterface.BUTTON_NEGATIVE -> {
                prefs
                    .edit()
                    .putBoolean(
                        context.getString(R.string.pref_opt_in_screen_overview_key),
                        buttonClicked == DialogInterface.BUTTON_POSITIVE)
                    .apply();
                analytics.onScreenOverviewOptInEvent(
                    buttonClicked == DialogInterface.BUTTON_POSITIVE
                        ? GEMINI_OPT_IN_CONSENT
                        : GEMINI_OPT_IN_DISSENT);
              }
              default -> {}
            }
          }

          @Override
          public void handleDialogDismiss() {
            if (nodeToBeDetailDescribed == null) {
              return;
            }
            if (SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_opt_in_screen_overview_key,
                R.bool.pref_opt_in_screen_overview_default)) {
              pipeline.returnFeedback(
                  EVENT_ID_UNTRACKED, Feedback.performScreenOverview(nodeToBeDetailDescribed));
            }
          }
        };
    geminiNanoOptInDialog =
        new GeminiOptInDialog(
            service,
            R.string.title_on_device_gemini_opt_in_dialog,
            true,
            R.string.dialog_message_on_device_detailed_ai_promotion,
            R.string.positive_button_gemini_nano_opt_in_dialog,
            /* negativeButtonResId= */ -1) {
          @Override
          public void handleDialogClick(int buttonClicked) {
            switch (buttonClicked) {
              case DialogInterface.BUTTON_POSITIVE -> {
                prefs
                    .edit()
                    .putBoolean(
                        context.getString(R.string.pref_auto_on_devices_image_description_key),
                        true)
                    .apply();
                analytics.onGeminiOptInEvent(GEMINI_OPT_IN_CONSENT, /* serverSide= */ false);
              }
              case DialogInterface.BUTTON_NEGATIVE -> {
                prefs
                    .edit()
                    .putBoolean(
                        context.getString(R.string.pref_auto_on_devices_image_description_key),
                        false)
                    .apply();
                analytics.onGeminiOptInEvent(GEMINI_OPT_IN_DISSENT, /* serverSide= */ false);
              }
              default -> {}
            }
          }

          @Override
          public void handleDialogDismiss() {
            if (nodeToBeDetailDescribed == null) {
              return;
            }
            if (SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_auto_on_devices_image_description_key,
                R.bool.pref_auto_on_device_image_description_default)) {
              pipeline.returnFeedback(
                  EVENT_ID_UNTRACKED,
                  Feedback.performDetailedOnDeviceImageCaption(nodeToBeDetailDescribed));
            }
          }
        };
    configImageDescriptionDialog =
        new GeminiOptInDialog(
            service,
            R.string.dialog_title_turn_on_image_description,
            false,
            -1,
            R.string.positive_button_turn_on_image_descriptions_dialog,
            R.string.delete_dialog_negative_button_text) {
          @Override
          public void handleDialogClick(int buttonClicked) {
            switch (buttonClicked) {
              case DialogInterface.BUTTON_POSITIVE ->
                  pipeline.returnFeedback(
                      EVENT_ID_UNTRACKED,
                      Feedback.triggerIntent(Action.TRIGGER_IMAGE_DESCRIPTIONS_SETTINGS));
              case DialogInterface.BUTTON_NEGATIVE -> {}
              default -> {}
            }
          }

          @Override
          public void handleDialogDismiss() {}
        };
  }

  public static boolean supportsImageCaption(Context context) {
    return FeatureSupport.canTakeScreenShotByAccessibilityService()
        && TalkbackFeatureSupport.supportDynamicFeatures();
  }

  /**
   * Checks if the node needs automatic image captioning depending on the settings in Automatic
   * Image Description Settings page.
   */
  public static boolean needAutomaticCaptioning(
      Context context, AccessibilityNodeInfoCompat node, boolean hasAiCore) {
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    CaptionNodeType captionNodeType = getCaptionNodeType(context, node, /* needSizeCheck= */ false);
    return needAutomaticIconDetection(context, prefs, captionNodeType)
        || needAutomaticImageDescription(
            context, prefs, getCaptionNodeType(context, node, true), hasAiCore)
        || needAutomaticTextRecognition(context, prefs, captionNodeType);
  }

  public static boolean supportsIconDetection(Context context) {
    return FeatureSupport.canTakeScreenShotByAccessibilityService()
        && TalkbackFeatureSupport.supportDynamicFeatures()
        && ImageCaptioner.isSupportIconDetection();
  }

  public static boolean supportsImageDescription(Context context) {
    return FeatureSupport.canTakeScreenShotByAccessibilityService()
        && TalkbackFeatureSupport.supportDynamicFeatures()
        && FeatureFlagReader.enableImageDescription(context)
        && ImageDescriptionProcessor.isSupportImageDescription();
  }

  public static boolean isSupportIconDetection() {
    return supportIconDetection;
  }

  @VisibleForTesting
  public static void setSupportIconDetection(boolean supportIconDetection) {
    ImageCaptioner.supportIconDetection = supportIconDetection;
  }

  /** Checks if the library can be downloaded for onboarding and tutorial pages. */
  public boolean needDownloadDialog(CaptionType type, Requester requester) {
    return moduleStateManager.needDownloadDialog(type, requester);
  }

  /**
   * Shows a download dialog or speaks the downloading or downloaded hint for onboarding and
   * tutorial pages
   */
  public void showDownloadDialogOrAnnounceState(CaptionType type, Requester requester) {
    if (moduleStateManager.showDownloadDialog(type, requester, /* node= */ null)) {
      return;
    }
    int announcement = moduleStateManager.getStateAnnouncement(type);
    if (announcement == -1) {
      return;
    }
    returnFeedback(announcement);
  }

  /**
   * Shows the download confirmation dialog if the Talkback supports the feature and the module
   * hasn't been downloaded.
   */
  private boolean showDownloadDialog(
      CaptionType captionType, AccessibilityNodeInfoCompat node, Requester requester) {
    return moduleStateManager.showDownloadDialog(captionType, requester, node);
  }

  public boolean initIconDetection() {
    if (!moduleStateManager.isLibraryReady(ICON_LABEL)) {
      return false;
    }

    if (iconAnnotationsDetector != null) {
      // Icon detector has been initialized.
      return true;
    }

    if (!moduleStateManager.installModule(ICON_LABEL)) {
      // Icon detection module install failed.
      return false;
    }

    try {
      iconAnnotationsDetector =
          IconAnnotationsDetectorFactory.create(service.getApplicationContext());
    } catch (UnsatisfiedLinkError error) {
      LogUtils.e(TAG, error.getMessage());
      return false;
    }

    if (iconAnnotationsDetector == null) {
      return false;
    }

    imageCaptionStorage.setIconAnnotationsDetector(iconAnnotationsDetector);
    startIconDetector();
    return true;
  }

  @VisibleForTesting
  void startIconDetector() {
    if (iconAnnotationsDetector != null) {
      synchronized (this) {
        if (!iconAnnotationsDetectorStarted) {
          iconAnnotationsDetectorStarted = true;
          iconAnnotationsDetector.start();
        }
      }
    }
  }

  @VisibleForTesting
  void shutdownIconDetector() {
    removeMessages(MSG_RESULT_TIMEOUT);
    captionResults.cleanUp();

    if (iconAnnotationsDetector != null) {
      synchronized (this) {
        if (iconAnnotationsDetectorStarted) {
          iconAnnotationsDetectorStarted = false;
          iconAnnotationsDetector.shutdown();
        }
      }
      iconAnnotationsDetector = null;
    }
  }

  public boolean descriptionLibraryReady() {
    return moduleStateManager.isLibraryReady(IMAGE_DESCRIPTION);
  }

  @CanIgnoreReturnValue
  public boolean initImageDescription() {
    if (!moduleStateManager.isLibraryReady(IMAGE_DESCRIPTION)) {
      return false;
    }

    if (imageDescriptionProcessor != null || isImageDescriptionProcessorInitializing) {
      // ImageDescriptionProcessor has been initialized or is initializing.
      return true;
    }

    if (!moduleStateManager.installModule(IMAGE_DESCRIPTION)) {
      // Image description module install failed.
      return false;
    }
    isImageDescriptionProcessorInitializing = true;
    initImageDescriptionProcessFuture =
        executorService.submit(
            () -> {
              LogUtils.v(TAG, "Creating ImageDescriptionProcessor...");
              if (imageDescriptionProcessor != null) {
                LogUtils.v(TAG, "ImageDescriptionProcessor created");
                isImageDescriptionProcessorInitializing = false;
                return true;
              }

              try {
                imageDescriptionProcessor = new ImageDescriptionProcessor(service, analytics);
                LogUtils.v(TAG, "ImageDescriptionProcessor initialized.");
                return true;
              } catch (UnsatisfiedLinkError error) {
                LogUtils.e(TAG, error.getMessage());
                return false;
              } finally {
                isImageDescriptionProcessorInitializing = false;
              }
            });
    return true;
  }

  private void shutdownImageDescription() {
    LogUtils.v(
        TAG,
        "shutdownImageDescription isImageDescriptionProcessorInitializing=%b",
        isImageDescriptionProcessorInitializing);
    shutDownImageDescriptionProcessFuture =
        executorService.submit(
            () -> {
              isImageDescriptionProcessorInitializing = false;
              if (imageDescriptionProcessor != null) {
                imageDescriptionProcessor.stop();
                imageDescriptionProcessor = null;
                LogUtils.v(TAG, "ImageDescriptionProcessor stopped.");
              }
              return true;
            });
  }

  public void shutdown() {
    shutdownIconDetector();
    shutdownImageDescription();
    moduleStateManager.shutdown();

    SharedPreferencesUtils.getSharedPreferences(service)
        .unregisterOnSharedPreferenceChangeListener(this);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
    moduleStateManager.setPipeline(pipeline);
    moduleStateManager.setDownloadStateListener(
        new ManualDownloadStateListener(
            service,
            pipeline,
            analytics,
            this,
            DownloadStateListenerResources.ALL,
            ImageCaptionPreferenceKeys.ALL,
            DownloadDialogResources.ALL.moduleSizeInMb,
            ImageCaptionLogKeys.ALL) {
          @Override
          void initialize() {
            initIconDetection();
            initImageDescription();
          }
        },
        new ManualDownloadStateListener(
            service,
            pipeline,
            analytics,
            this,
            DownloadStateListenerResources.ICON_DETECTION,
            ImageCaptionPreferenceKeys.ICON_DETECTION,
            DownloadDialogResources.ICON_DETECTION.moduleSizeInMb,
            ImageCaptionLogKeys.ICON_DETECTION) {
          @Override
          void initialize() {
            initIconDetection();
          }
        },
        new ManualDownloadStateListener(
            service,
            pipeline,
            analytics,
            this,
            DownloadStateListenerResources.IMAGE_DESCRIPTION,
            ImageCaptionPreferenceKeys.IMAGE_DESCRIPTION,
            DownloadDialogResources.IMAGE_DESCRIPTION.moduleSizeInMb,
            ImageCaptionLogKeys.IMAGE_DESCRIPTION) {
          @Override
          void initialize() {
            initImageDescription();
          }
        });
  }

  public void setActorState(ActorState actorState) {
    this.actorState = actorState;
    moduleStateManager.setActorState(actorState);
  }

  @Override
  public void handle(EventInterpretation interpretation, @Nullable Performance.EventId eventId) {
    // Performs other image captions if there is a node waited for recognition.
    if (interpretation.areWindowsStable() && queuedNode != null) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED,
          Feedback.performImageCaptions(queuedNode, /* isUserRequested= */ true));
      queuedNode = null;
    }
  }

  @Override
  public void newItemFocused(
      AccessibilityNodeInfo nodeInfo, Interpretation.AccessibilityFocused axFocused) {
    @Nullable CaptionResult currentCaption = captionResults.getCaptionResult(requestId);
    if (currentCaption == null || currentCaption.isFinished()) {
      return;
    }
    if (!currentCaption.isUserRequest) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED, Feedback.geminiAction(GeminiRequest.Action.MUTE_GEMINI_SOUND));
    }
  }

  /**
   * Shows the confirmation dialog to download the icon detection and image description module, or
   * performs image captions for the given node.
   *
   * @return false, the caption request has not been performed and must wait until the icon
   *     detection module is installed or the dialog is cancelled.
   */
  public boolean confirmDownloadAndPerformCaption(AccessibilityNodeInfoCompat node) {
    if (!canTakeScreenshot()) {
      analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_CANNOT_PERFORM_WHEN_SCREEN_HIDDEN);
      returnFeedback(R.string.image_caption_with_hide_screen);
      return true;
    }

    if (moduleStateManager.isLibraryReady(ICON_LABEL) && iconAnnotationsDetector == null) {
      initIconDetection();
    }
    boolean iconDetectionAvailable = (iconAnnotationsDetector != null);

    if (moduleStateManager.isLibraryReady(IMAGE_DESCRIPTION) && imageDescriptionProcessor == null) {
      initImageDescription();
    }
    boolean imageDescriptionAvailable =
        (imageDescriptionProcessor != null) || isImageDescriptionProcessorInitializing;

    CaptionType captionType =
        supportsImageDescription(service)
            ? ImageCaptionUtils.getPreferredModuleOnNode(service, node)
            : ICON_LABEL;
    LogUtils.v(
        TAG,
        "iconDetectionAvailable=%s, imageDescriptionAvailable=%s, captionType = %s",
        iconDetectionAvailable,
        imageDescriptionAvailable,
        captionType);

    // Shows a dialog to download icon detection module for small size images; shows a dialog to
    // download image description module for large size images. Only one dialog will be shown.
    if ((captionType == ICON_LABEL && !iconDetectionAvailable)
        || (captionType == IMAGE_DESCRIPTION && !imageDescriptionAvailable)) {
      if (showDownloadDialog(captionType, node, MENU)) {
        return false;
      }
    }

    caption(node, /* isUserRequested= */ true);
    return true;
  }

  /**
   * Shows the Gemini opt-in dialog.
   *
   * @param node The focused node which is the potential target to be detailed described.
   * @param featureType to specify which Gemini service TalkBack user's opt-in now.
   */
  public boolean geminiOptInForManualTrigger(
      AccessibilityNodeInfoCompat node, @GeminiFeatureType int featureType) {
    nodeToBeDetailDescribed = node;
    if (featureType == GEMINI_DESCRIBE_IMAGE) {
      geminiOptInDialog.showDialog();
      int optInPopCount =
          prefs.getInt(service.getString(R.string.pref_gemini_repeat_opt_in_pop_key), 0);
      SharedPreferencesUtils.putIntPref(
          prefs,
          service.getResources(),
          R.string.pref_gemini_repeat_opt_in_pop_key,
          ++optInPopCount);
      analytics.onGeminiOptInEvent(GEMINI_OPT_IN_SHOW_DIALOG, /* serverSide= */ true);
    } else {
      screenOverviewOptInDialog.showDialog();
      analytics.onScreenOverviewOptInEvent(GEMINI_OPT_IN_SHOW_DIALOG);
    }

    return true;
  }

  /**
   * Shows the Gemini On-device opt-in dialog.
   *
   * @param node The focused node which is the potential target to be detailed described.
   */
  public boolean geminiOnDeviceOptInForManualTrigger(AccessibilityNodeInfoCompat node) {
    nodeToBeDetailDescribed = node;
    geminiNanoOptInDialog.showDialog();
    analytics.onGeminiOptInEvent(GEMINI_OPT_IN_SHOW_DIALOG, /* serverSide= */ false);

    return true;
  }

  public boolean geminiConfigDetailedImageDescriptionTrigger(AccessibilityNodeInfoCompat node) {
    configImageDescriptionDialog.showDialog();

    return true;
  }

  /**
   * Creates a {@link CaptionRequest} to perform corresponding image caption or reads out the result
   * if it exists in the cache.
   */
  public boolean caption(AccessibilityNodeInfoCompat node, boolean isUserRequested) {
    @Nullable ImageNode savedResult = imageCaptionStorage.getCaptionResults(node);
    analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_CAPTION_REQUEST);

    if (!canTakeScreenshot()) {
      analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_CANNOT_PERFORM_WHEN_SCREEN_HIDDEN);
      if (isUserRequested) {
        returnFeedback(R.string.image_caption_with_hide_screen);
      }
      return false;
    }

    if (isUserRequested) {
      analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_CAPTION_REQUEST_MANUAL);
      // If the caption request is triggered by users from TalkBack menu, TalkBack always performs
      // it even if the result has existed in the cache.
    } else {
      if (savedResult != null) {
        analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_IMAGE_CAPTION_CACHE_HIT);
        // Caption results for the focused view will be read out by node_text_or_label method of the
        // compositor.
        return true;
      }
    }

    if (iconAnnotationsDetector != null) {
      if (!iconAnnotationsDetectorStarted) {
        startIconDetector();
      }
    }

    screenshotRequests.addRequest(
        new ScreenshotCaptureRequest(
            service,
            node,
            this::onScreenshotCapturePending,
            this::handleScreenshotCaptureResponse,
            isUserRequested));
    return true;
  }

  /** Performs image captioning on the given {@link AccessibilityNodeInfoCompat} by Gemini. */
  public boolean captionWithGemini(AccessibilityNodeInfoCompat node) {
    if (!canTakeScreenshot()) {
      returnFeedback(R.string.image_caption_with_hide_screen);
      return false;
    }

    ScreenshotCaptureRequest screenshotCaptureRequest =
        new ScreenshotCaptureRequest(
            service,
            node,
            this::onScreenshotCapturePending,
            (focusedNode, screenCapture, isUserRequested, captureByWindow) -> {
              processImageCaptioningWithGemini(
                  focusedNode,
                  screenCapture,
                  isUserRequested,
                  captureByWindow,
                  /* isToAiCore= */ false);
            },
            /* isUserRequested= */ true);
    screenshotRequests.addRequest(screenshotCaptureRequest);
    return true;
  }

  public boolean geminiScreenOverview(AccessibilityNodeInfoCompat node) {
    if (!canTakeScreenshot()) {
      returnFeedback(R.string.image_caption_with_hide_screen);
      return false;
    }

    ScreenshotCaptureRequest screenshotCaptureRequest =
        new ScreenshotCaptureRequest(
            service,
            node,
            this::onScreenshotCapturePending,
            this::processScreenDescription,
            /* isUserRequested= */ true);
    screenshotRequests.addRequest(screenshotCaptureRequest);
    return true;
  }

  /** Performs image captioning on the given {@link AccessibilityNodeInfoCompat} by Gemini Nano. */
  public boolean captionWithOnDeviceGemini(AccessibilityNodeInfoCompat node) {
    if (!canTakeScreenshot()) {
      returnFeedback(R.string.image_caption_with_hide_screen);
      return false;
    }

    ScreenshotCaptureRequest screenshotCaptureRequest =
        new ScreenshotCaptureRequest(
            service,
            node,
            this::onScreenshotCapturePending,
            (focusedNode, screenCapture, isUserRequested, captureByWindow) -> {
              processImageCaptioningWithGemini(
                  focusedNode,
                  screenCapture,
                  isUserRequested,
                  captureByWindow,
                  /* isToAiCore= */ true);
            },
            /* isUserRequested= */ true);
    screenshotRequests.addRequest(screenshotCaptureRequest);
    return true;
  }

  private void processImageCaptioningWithGemini(
      AccessibilityNodeInfoCompat focusedNode,
      Bitmap screenCapture,
      boolean isUserRequested,
      boolean captureByWindow,
      boolean isToAiCore) {
    if (screenCapture == null) {
      handleScreenshotCaptureFailure(isUserRequested);
      return;
    }

    requestId++;
    ProcessedScreens screens =
        processImageForNodeCaption(
            focusedNode,
            screenCapture,
            getTalkBackFocusStrokeWidth(prefs, service.getResources()),
            captureByWindow);
    CaptionResult captionResult =
        new CaptionResult(
            AccessibilityNode.takeOwnership(focusedNode),
            isUserRequested,
            /* isImageDescriptionByGemini= */ true);
    captionResults.addResultAndCleanOldOnes(requestId, captionResult, isUserRequested);
    // For Bitmap recycle.
    captionResult.addUsedScreenshot(screens.croppedScreenCapture());
    if (!screens.croppedScreenCapture().equals(screens.blockedScreenCapture())) {
      // To avoid recycle the same image twice, check if the two images are the same objects.
      captionResult.addUsedScreenshot(screens.blockedScreenCapture());
    }

    boolean hasCaptionRequest = false;
    CaptionNodeType captionNodeType =
        getCaptionNodeType(service, focusedNode, /* needSizeCheck= */ false);
    // Text recognition
    if (isUserRequested || needAutomaticTextRecognition(service, prefs, captionNodeType)) {
      addCaptionRequest(requestId, focusedNode, screens.croppedScreenCapture(), isUserRequested);
      hasCaptionRequest = true;
    } else {
      // No need to perform text recognition.
      captionResult.setOcrText(null);
    }

    // Icon detection
    if (iconAnnotationsDetector != null
        && (isUserRequested || needAutomaticIconDetection(service, prefs, captionNodeType))) {
      addIconDetectionRequest(
          requestId, focusedNode, screens.blockedScreenCapture(), isUserRequested);
      hasCaptionRequest = true;
    } else {
      // No need to perform icon detection
      captionResult.setIconLabel(null);
    }

    // Image description with Gemini
    if (isUserRequested
        || needAutomaticImageDescription(
            service,
            prefs,
            getCaptionNodeType(service, focusedNode, /* needSizeCheck= */ true),
            /* hasAiCore= */ true)) {
      if (isToAiCore) {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.geminiOnDeviceImageCaptioning(
                requestId, screens.croppedScreenCapture(), isUserRequested));
      } else {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.geminiRequest(
                requestId,
                service.getResources().getString(R.string.image_caption_with_gemini_prefix),
                screens.croppedScreenCapture()));
      }
      hasCaptionRequest = true;
    } else {
      // No need to perform image description
      captionResult.setImageDescription(null);
    }

    if (hasCaptionRequest) {
      return;
    }

    screenshotRequests.performNextRequest();
  }

  private void processScreenDescription(
      AccessibilityNodeInfoCompat focusedNode,
      Bitmap screenCapture,
      boolean isUserRequested,
      boolean captureByWindow) {
    if (screenCapture == null) {
      handleScreenshotCaptureFailure(isUserRequested);
      return;
    }
    byte[] imageBytes = DataFieldUtils.encodeImageToByteArray(screenCapture);
    if (imageBytes == null) {
      handleScreenshotCaptureFailure(isUserRequested);
      return;
    }

    requestId++;
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED, Feedback.geminiScreenOverview(requestId, focusedNode, imageBytes));

    if (!screenCapture.isRecycled()) {
      screenCapture.recycle();
    }

    screenshotRequests.performNextRequest();
  }

  /**
   * Notifies the {@link IconAnnotationsDetector} that the whole screen content has changed if the
   * {@link IconAnnotationsDetector} is not {@code null}.
   *
   * @return {@code true} if {@link IconAnnotationsDetector} is not null
   */
  public boolean clearWholeScreenCache() {
    if (iconAnnotationsDetector == null) {
      return false;
    }

    iconAnnotationsDetector.clearWholeScreenCache();
    return true;
  }

  /**
   * Notifies the {@link IconAnnotationsDetector} that the content inside the specific {@code rect}
   * has changed if the {@link IconAnnotationsDetector} is not {@code null}.
   *
   * @return {@code true} if {@link IconAnnotationsDetector} is not null
   */
  public boolean clearPartialScreenCache(Rect rect) {
    if (iconAnnotationsDetector == null) {
      return false;
    }

    iconAnnotationsDetector.clearPartialScreenCache(rect);
    return true;
  }

  /**
   * Invalidates cached results for the content inside the {@code rect} because an icon inside a
   * view may change due to the click.
   *
   * @return {@code true} if {@link IconAnnotationsDetector} is not null
   */
  public boolean clearCacheForView(Rect rect) {
    if (iconAnnotationsDetector == null) {
      return false;
    }

    clearPartialScreenCache(rect);

    // In Talkback only the view being focused can be clicked by the user
    @Nullable
    AccessibilityNode focusedNode =
        AccessibilityNode.takeOwnership(
            accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false));
    if (focusedNode == null) {
      return true;
    }
    imageCaptionStorage.invalidateCaptionForNode(focusedNode);
    return true;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    if (service.getString(ImageCaptionPreferenceKeys.ICON_DETECTION.uninstalledKey).equals(key)
        && moduleStateManager.isLegacyUninstalled(ICON_LABEL)) {
      iconDetectionRequests.clear();
      shutdownIconDetector();
      return;
    }

    if (service.getString(ImageCaptionPreferenceKeys.IMAGE_DESCRIPTION.uninstalledKey).equals(key)
        && moduleStateManager.isLegacyUninstalled(IMAGE_DESCRIPTION)) {
      imageDescriptionRequests.clear();
      shutdownImageDescription();
      return;
    }

    if (service.getString(ImageCaptionPreferenceKeys.ALL.uninstalledKey).equals(key)
        && moduleStateManager.isCombinedUninstalled()) {
      iconDetectionRequests.clear();
      shutdownIconDetector();
      imageDescriptionRequests.clear();
      shutdownImageDescription();
    }
  }

  private boolean canTakeScreenshot() {
    return FeatureSupport.supportTakeScreenshotByWindow()
        || !actorState.getDimScreen().isDimmingEnabled();
  }

  /**
   * Blocks out views and system windows which overlap with the given view.
   *
   * @return {@code null} if no view overlaps with the node view.
   */
  @VisibleForTesting
  @Nullable
  Bitmap blockOverlaps(
      AccessibilityNodeInfoCompat root, AccessibilityNodeInfoCompat node, Bitmap screenshot) {
    if (root == null || node == null) {
      LogUtils.w(TAG, "Can't perform blockOverlaps; the caller may need to reuse screenshot");
      return null;
    }

    // Gets all ancestors.
    Set<AccessibilityNodeInfoCompat> selfAndAncestors = new HashSet<>();
    selfAndAncestors.add(node);
    AccessibilityNodeInfoCompat parent = node.getParent();
    while (parent != null) {
      selfAndAncestors.add(parent);
      parent = parent.getParent();
    }

    // Finds focusable and intersected nodes.
    Rect nodeBounds = new Rect();
    node.getBoundsInScreen(nodeBounds);
    List<AccessibilityNodeInfoCompat> overlaps =
        AccessibilityNodeInfoUtils.getMatchingDescendantsOrRoot(
            root,
            new Filter<AccessibilityNodeInfoCompat>() {
              @Override
              public boolean accept(AccessibilityNodeInfoCompat info) {
                Rect bounds = new Rect();
                info.getBoundsInScreen(bounds);
                if (nodeBounds.contains(bounds) && !selfAndAncestors.contains(info)) {
                  if (((nodeBounds.width() - bounds.width()) > nodeBounds.width() / 10)
                      && ((nodeBounds.height() - bounds.height()) > nodeBounds.height() / 10)) {
                    // b/348281762. If views within the container(image for captioning)
                    // completely(or highly) overlap, the blacken effect is applied to the parent
                    // rectangle only when it maintains at least 19%(1-0.9x0.9) visibility.
                    return AccessibilityNodeInfoUtils.isFocusable(info);
                  }
                }
                return false;
              }
            });

    boolean isBlockedOut = false;
    int width = screenshot.getWidth();
    int height = screenshot.getHeight();
    int[] pixels = new int[width * height];
    screenshot.getPixels(pixels, /* offset= */ 0, width, /* x= */ 0, /* y= */ 0, width, height);

    // Blackens views.
    if (overlaps != null) {
      for (AccessibilityNodeInfoCompat overlap : overlaps) {
        isBlockedOut = true;
        blackenView(overlap, pixels, width);
      }
    }

    // Blackens system windows.
    if (!FeatureSupport.supportTakeScreenshotByWindow()) {
      List<AccessibilityWindowInfo> windows =
          AccessibilityServiceCompatUtils.getSystemWindows(service);
      for (AccessibilityWindowInfo window : windows) {
        if (window.isAccessibilityFocused()) {
          continue;
        }
        Rect windowBounds = new Rect();
        window.getBoundsInScreen(windowBounds);
        if (windowBounds.intersect(nodeBounds)) {
          isBlockedOut = true;
          blackenWindow(window, pixels, width);
        }
      }
    }

    if (!isBlockedOut) {
      return null;
    }

    Bitmap blockedScreenshot = Bitmap.createBitmap(width, height, Config.ARGB_8888);
    blockedScreenshot.setPixels(
        pixels, /* offset= */ 0, width, /* x= */ 0, /* y= */ 0, width, height);

    return blockedScreenshot;
  }

  /** Makes the given node in the screenshot become black. */
  private static void blackenView(
      AccessibilityNodeInfoCompat node, int[] screenshotPixels, int width) {
    LogUtils.v(TAG, "Blocks out %s", node);
    Rect bounds = new Rect();
    node.getBoundsInScreen(bounds);
    blackenBlock(bounds, screenshotPixels, width);
  }

  /** Makes the given window in the screenshot become black. */
  private static void blackenWindow(
      AccessibilityWindowInfo window, int[] screenshotPixels, int width) {
    LogUtils.v(TAG, "Blocks window %s", window);
    Rect bounds = new Rect();
    window.getBoundsInScreen(bounds);
    blackenBlock(bounds, screenshotPixels, width);
  }

  /** Makes the block in the screenshot become black. */
  private static void blackenBlock(Rect bounds, int[] screenshotPixels, int width) {
    int size = screenshotPixels.length;
    for (int row = bounds.top; row < bounds.bottom; row++) {
      for (int column = bounds.left; column < bounds.right; column++) {
        int index = row * width + column;
        if (index >= size || index < 0) {
          LogUtils.e(TAG, "blacken - invalid index");
          return;
        }
        screenshotPixels[index] = Color.BLACK;
      }
    }
  }

  @VisibleForTesting
  void handleScreenshotCaptureResponse(
      AccessibilityNodeInfoCompat node,
      @Nullable Bitmap screenCapture,
      boolean isUserRequested,
      boolean captureByWindow) {
    if (screenCapture == null) {
      handleScreenshotCaptureFailure(isUserRequested);
      return;
    }

    LogUtils.v(TAG, "onScreenCaptureFinish() taking screenshot is successful.");

    // Image captioning with Gemini(auto-triggered).
    if (actorState.getGeminiState().hasAiCore()) {
      if (!isUserRequested) {
        LogUtils.v(TAG, "onScreenCaptureFinish(): auto-caption with Gemini");
        processImageCaptioningWithGemini(
            node,
            screenCapture,
            /* isUserRequested= */ false,
            captureByWindow,
            /* isToAiCore= */ true);
        return;
      } else {
        LogUtils.w(TAG, "Shouldn't trigger manual image description with AiCore here.");
      }
    }

    requestId++;

    ProcessedScreens screens =
        processImageForNodeCaption(
            node,
            screenCapture,
            getTalkBackFocusStrokeWidth(prefs, service.getResources()),
            captureByWindow);

    CaptionResult captionResult =
        new CaptionResult(AccessibilityNode.takeOwnership(node), isUserRequested);
    captionResults.addResultAndCleanOldOnes(requestId, captionResult, isUserRequested);
    // For Bitmap recycle.
    captionResult.addUsedScreenshot(screens.blockedScreenCapture());
    if (!screens.croppedScreenCapture().equals(screens.blockedScreenCapture())) {
      captionResult.addUsedScreenshot(screens.croppedScreenCapture());
    }
    sendResultTimeoutMessage();

    CaptionNodeType captionNodeType = getCaptionNodeType(service, node, /* needSizeCheck= */ false);
    boolean hasCaptionRequest = false;
    // Icon detection.
    if (iconAnnotationsDetector == null
        || (!isUserRequested && !needAutomaticIconDetection(service, prefs, captionNodeType))) {
      // Not support icon detection or automatic icon description is disabled.
      captionResult.setIconLabel(null);
    } else {
      // If the label for the matched icon is available in English, there is no need to process
      // the screenshot to detect icons. And it is possible that the label for the matched icon is
      // available in English but not available in current speech language.
      CharSequence iconLabel =
          ENABLE_CACHE_MECHANISM
              ? iconAnnotationsDetector.getIconLabel(Locale.ENGLISH, node)
              : null;
      if (iconLabel == null) {
        addIconDetectionRequest(requestId, node, screens.blockedScreenCapture(), isUserRequested);
        hasCaptionRequest = true;
      } else {
        analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_IMAGE_CAPTION_CACHE_HIT);
        LogUtils.v(TAG, "handleScreenshotCaptureResponse(): Icon label exists. icon=%s", iconLabel);
        captionResult.setIconLabel(Result.create(ICON_LABEL, iconLabel));
      }
    }

    // Text recognition.
    if (!isUserRequested && !needAutomaticTextRecognition(service, prefs, captionNodeType)) {
      // Automatic text recognition is disabled.
      captionResult.setOcrText(null);
    } else {
      addCaptionRequest(requestId, node, screens.croppedScreenCapture(), isUserRequested);
      hasCaptionRequest = true;
    }

    // Image description with Garcon.
    if (imageDescriptionProcessor == null) {
      // Image description isn't supported or ready.
      captionResult.setImageDescription(null);
      if (isImageDescriptionProcessorInitializing) {
        LogUtils.v(TAG, "Image description process is initializing...");
      }
    } else if (!isUserRequested
        && !needAutomaticImageDescription(
            service,
            prefs,
            getCaptionNodeType(service, node, /* needSizeCheck= */ true),
            /* hasAiCore= */ false)) {
      // Automatic image description is disabled.
      captionResult.setImageDescription(null);
    } else {
      addImageDescriptionRequest(requestId, node, screens.croppedScreenCapture(), isUserRequested);
      hasCaptionRequest = true;
    }

    if (hasCaptionRequest) {
      return;
    }

    LogUtils.v(TAG, "No caption request for the screenshot. Perform the next screenshot request.");
    screenshotRequests.performNextRequest();
  }

  private void handleScreenshotCaptureFailure(boolean isUserRequested) {
    analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_SCREENSHOT_FAILED);
    LogUtils.e(TAG, "onScreenCaptureFinish() taking screenshot has failed.");

    if (isUserRequested) {
      returnFeedback(R.string.gemini_screenshot_unavailable);
    }

    screenshotRequests.performNextRequest();
  }

  @VisibleForTesting
  void onScreenshotCapturePending(boolean scheduled, Duration intervalTime) {
    primeLogImageCaptionEvent(LATENCY_BETWEEN_SCREENSHOT_CAPTURE_REQUEST, intervalTime.toMillis());
    if (scheduled) {
      return;
    }

    analytics.onImageCaptionEvent(
        TalkBackAnalytics.IMAGE_CAPTION_EVENT_SCHEDULE_SCREENSHOT_CAPTURE_FAILURE);
    screenshotRequests.performNextRequest();
  }

  @VisibleForTesting
  void onCharacterCaptionFinish(
      CaptionRequest request, AccessibilityNode node, Result result, boolean isUserRequested) {
    if (request.getDurationMillis() != INVALID_DURATION) {
      primeLogImageCaptionEvent(IMAGE_CAPTION_OCR_SUCCEED, request.getDurationMillis());
    }
    analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED);
    LogUtils.v(
        TAG,
        "onCharacterCaptionFinish() "
            + StringBuilderUtils.joinFields(
                StringBuilderUtils.optionalSubObj("result", result.text()),
                StringBuilderUtils.optionalSubObj("node", node)));
    characterCaptionRequests.performNextRequest();

    handleResult(request.getRequestId(), node, result, isUserRequested);
    imageCaptionStorage.updateCharacterCaptionResult(node, result);
  }

  @VisibleForTesting
  void addCaptionRequest(
      int id, AccessibilityNodeInfoCompat node, Bitmap screenCapture, boolean isUserRequested) {
    characterCaptionRequests.addRequest(
        new CharacterCaptionRequest(
            id,
            service,
            node,
            screenCapture,
            /* onFinishListener= */ this::onCharacterCaptionFinish,
            /* onErrorListener= */ (errorRequest, errorNode, errorCode, userRequest) -> {
              if (errorRequest.getDurationMillis() != INVALID_DURATION) {
                primeLogImageCaptionEvent(
                    IMAGE_CAPTION_OCR_FAILED, errorRequest.getDurationMillis());
              }
              analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_OCR_PERFORM_FAIL);
              LogUtils.v(TAG, "onError(), error= %s", Request.errorName(errorCode));
              characterCaptionRequests.performNextRequest();
              handleResult(
                  errorRequest.getRequestId(),
                  AccessibilityNode.takeOwnership(node),
                  Result.create(OCR, /* result= */ null),
                  userRequest);
            },
            isUserRequested));
  }

  @VisibleForTesting
  void onIconDetectionFinish(
      CaptionRequest request, AccessibilityNode node, Result result, boolean isUserRequested) {
    if (request.getDurationMillis() != INVALID_DURATION) {
      primeLogImageCaptionEvent(IMAGE_CAPTION_ICON_LABEL_SUCCEED, request.getDurationMillis());
    }
    analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_ICON_DETECT_SUCCEED);
    LogUtils.v(TAG, "onIconDetectionFinish() result=%s node=%s", result.text(), node);
    iconDetectionRequests.performNextRequest();

    handleResult(request.getRequestId(), node, result, isUserRequested);
    imageCaptionStorage.updateDetectedIconLabel(node, result);
  }

  @VisibleForTesting
  void addIconDetectionRequest(
      int id, AccessibilityNodeInfoCompat node, Bitmap screenCapture, boolean isUserRequested) {
    iconDetectionRequests.addRequest(
        new IconDetectionRequest(
            id,
            node,
            screenCapture,
            iconAnnotationsDetector,
            getSpeechLocale(),
            /* onFinishListener= */ this::onIconDetectionFinish,
            /* onErrorListener= */ (errorRequest, errorNode, errorCode, userRequest) -> {
              if (errorRequest.getDurationMillis() != INVALID_DURATION) {
                primeLogImageCaptionEvent(
                    IMAGE_CAPTION_ICON_LABEL_FAILED, errorRequest.getDurationMillis());
              }
              analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_ICON_DETECT_FAIL);
              LogUtils.v(TAG, "onError(), error=%s", Request.errorName(errorCode));
              iconDetectionRequests.performNextRequest();
              handleResult(
                  errorRequest.getRequestId(),
                  AccessibilityNode.takeOwnership(node),
                  Result.create(ICON_LABEL, null),
                  userRequest);
            },
            isUserRequested));
  }

  private void onImageDescriptionFinish(
      CaptionRequest request, AccessibilityNode node, Result result, boolean isUserRequested) {
    if (request.getDurationMillis() != INVALID_DURATION) {
      primeLogImageCaptionEvent(
          IMAGE_CAPTION_IMAGE_DESCRIPTION_SUCCEED, request.getDurationMillis());
    }
    analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_SUCCEED);
    LogUtils.v(
        TAG,
        "onImageDescriptionFinish() "
            + StringBuilderUtils.joinFields(
                StringBuilderUtils.optionalSubObj("result", result.text()),
                StringBuilderUtils.optionalSubObj("node", node)));
    imageDescriptionRequests.performNextRequest();

    handleResult(request.getRequestId(), node, result, isUserRequested);
    imageCaptionStorage.updateImageDescriptionResult(node, result);
  }

  @VisibleForTesting
  void addImageDescriptionRequest(
      int id, AccessibilityNodeInfoCompat node, Bitmap screenCapture, boolean isUserRequested) {
    imageDescriptionRequests.addRequest(
        new ImageDescriptionRequest(
            id,
            service,
            node,
            screenCapture,
            imageDescriptionProcessor,
            /* onFinishListener= */ this::onImageDescriptionFinish,
            /* onErrorListener= */ (errorRequest, errorNode, errorCode, userRequest) -> {
              if (errorRequest.getDurationMillis() != INVALID_DURATION) {
                primeLogImageCaptionEvent(
                    IMAGE_CAPTION_IMAGE_DESCRIPTION_FAILED, errorRequest.getDurationMillis());
              }
              analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_FAIL);
              LogUtils.v(TAG, "onError(), error=%s", Request.errorName(errorCode));
              imageDescriptionRequests.performNextRequest();
              handleResult(
                  errorRequest.getRequestId(),
                  AccessibilityNode.takeOwnership(node),
                  Result.create(IMAGE_DESCRIPTION, null),
                  userRequest);
            },
            isUserRequested));
  }

  /** Returns why the node needs automatic image captioning. */
  private static CaptionNodeType getCaptionNodeType(
      Context context, AccessibilityNodeInfoCompat node, boolean needSizeCheck) {
    return ImageCaptionUtils.needImageCaptionForUnlabelledView(context, node)
        ? UNLABELLED_VIEW
        : FeatureFlagReader.enableAutomaticCaptioningForAllImages(context)
                && ImageCaptionUtils.needImageCaption(
                    context,
                    node,
                    /* enableAutomaticImageCaptionForAllImages= */ true,
                    needSizeCheck)
            ? IMAGE
            : NONE;
  }

  private static boolean needAutomaticIconDetection(
      Context context, SharedPreferences prefs, CaptionNodeType captionNodeType) {
    return needAutomaticImageCaptioning(
        context, prefs, FeatureSwitchDialogResources.ICON_DETECTION, captionNodeType);
  }

  private static boolean needAutomaticImageDescription(
      Context context,
      SharedPreferences prefs,
      CaptionNodeType captionNodeType,
      boolean hasAiCore) {
    return hasAiCore
        ? needAutomaticImageCaptioning(
            context,
            prefs,
            FeatureSwitchDialogResources.IMAGE_DESCRIPTION_AICORE_OPT_IN,
            captionNodeType)
        : needAutomaticImageCaptioning(
            context, prefs, FeatureSwitchDialogResources.IMAGE_DESCRIPTION, captionNodeType);
  }

  private static boolean needAutomaticTextRecognition(
      Context context, SharedPreferences prefs, CaptionNodeType captionNodeType) {
    return needAutomaticImageCaptioning(
        context, prefs, FeatureSwitchDialogResources.TEXT_RECOGNITION, captionNodeType);
  }

  private static boolean needAutomaticImageCaptioning(
      Context context,
      SharedPreferences prefs,
      FeatureSwitchDialogResources switchDialogResources,
      CaptionNodeType captionNodeType) {
    AutomaticImageCaptioningState state =
        getAutomaticImageCaptioningState(context, prefs, switchDialogResources);
    return switch (state) {
      case OFF -> false;
      case ON_ALL_IMAGES -> captionNodeType != NONE;
      case ON_UNLABELLED_ONLY -> captionNodeType == UNLABELLED_VIEW;
      default -> {
        LogUtils.e(TAG, "Invalid automatic image captioning state [%s]", state);
        yield true;
      }
    };
  }

  private void putBooleanPref(int key, boolean defaultValue) {
    SharedPreferencesUtils.putBooleanPref(prefs, service.getResources(), key, defaultValue);
  }

  @VisibleForTesting
  Locale getSpeechLocale() {
    Locale locale = actorState.getLanguageState().getCurrentLanguage();
    return (locale == null) ? Locale.getDefault() : locale;
  }

  @VisibleForTesting
  void clearRequests() {
    screenshotRequests.clear();
    characterCaptionRequests.clear();
    iconDetectionRequests.clear();
    captionResults.cleanUp();
  }

  @VisibleForTesting
  int getWaitingCharacterCaptionRequestSize() {
    return characterCaptionRequests.getWaitingRequestSize();
  }

  @VisibleForTesting
  int getWaitingIconDetectionRequestSize() {
    return iconDetectionRequests.getWaitingRequestSize();
  }

  @VisibleForTesting
  int getWaitingImageDescriptionRequestSize() {
    return imageDescriptionRequests.getWaitingRequestSize();
  }

  @VisibleForTesting
  int getWaitingScreenshotRequestSize() {
    return screenshotRequests.getWaitingRequestSize();
  }

  @VisibleForTesting
  void handleResult(int id, AccessibilityNode node, Result result, boolean isUserRequested) {
    @Nullable
    AccessibilityNode focusedNode =
        AccessibilityNode.takeOwnership(
            accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false));

    @Nullable CaptionResult captionResult = captionResults.getCaptionResult(id);
    boolean sameNodeWithCurrentFocus = node != null && node.equals(focusedNode);
    boolean needFeedbackToUser =
        isUserRequested
            || (sameNodeWithCurrentFocus && screenshotRequests.getWaitingRequestSize() < 1);

    if (captionResult == null) {
      if (needFeedbackToUser) {
        // There is no generate runnable for the request, so announcing the result directly.
        switch (result.type()) {
          case OCR ->
              returnCaptionResult(
                  id,
                  result,
                  /* iconLabelResult= */ null,
                  /* imageDescriptionResult= */ null,
                  isUserRequested,
                  /* isImageDescriptionByGemini= */ false,
                  /* isGeminiFinishedWithError= */ false,
                  /* isNetworkError= */ false);
          case ICON_LABEL ->
              returnCaptionResult(
                  id,
                  /* ocrTextResult= */ null,
                  result,
                  /* imageDescriptionResult= */ null,
                  isUserRequested,
                  /* isImageDescriptionByGemini= */ false,
                  /* isGeminiFinishedWithError= */ false,
                  /* isNetworkError= */ false);
          case IMAGE_DESCRIPTION ->
              returnCaptionResult(
                  id,
                  /* ocrTextResult= */ null,
                  /* iconLabelResult= */ null,
                  result,
                  isUserRequested,
                  /* isImageDescriptionByGemini= */ false,
                  /* isGeminiFinishedWithError= */ false,
                  /* isNetworkError= */ false);
          default -> {}
        }
      }
      screenshotRequests.performNextRequest();
    } else {
      switch (result.type()) {
        case OCR -> {
          captionResult.setOcrText(result);
          if (!needFeedbackToUser) {
            analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_OCR_ABORT);
          }
        }
        case ICON_LABEL -> {
          captionResult.setIconLabel(result);
          if (!needFeedbackToUser && iconAnnotationsDetector != null) {
            analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_ICON_DETECT_ABORT);
          }
        }
        case IMAGE_DESCRIPTION -> {
          captionResult.setImageDescription(result);
          if (!needFeedbackToUser
              && imageDescriptionProcessor != null
              && !captionResult.isImageDescriptionByGemini) {
            analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_ABORT);
          }
        }
        default -> {}
      }
      if (captionResult.isFinished()) {
        if (needFeedbackToUser) {
          returnCaptionResult(id, captionResult);
        }
        screenshotRequests.performNextRequest();
        captionResult.recycleAndClearScreenshots();
        captionResults.removeCaptionResult(id);
        removeMessages(MSG_RESULT_TIMEOUT);
      }
    }

    if (captionResults.isEmpty()) {
      removeMessages(MSG_RESULT_TIMEOUT);
    }

    if (captionResult == null) {
      LogUtils.v(TAG, "captionRequest is null");
    } else {
      LogUtils.v(
          TAG,
          "captionRequest finish=%b isUserRequested=%b, focused=%b",
          captionResult.isFinished(),
          isUserRequested,
          sameNodeWithCurrentFocus);
    }
  }

  /** Handles the image caption result from Gemini. */
  public boolean handleResultFromGemini(
      int requestId,
      String text,
      boolean isSuccess,
      @Nullable FinishReason finishReason,
      @Nullable ErrorReason errorReason,
      boolean manualTrigger) {
    LogUtils.d(
        TAG,
        "handleResultFromGemini, id = %d, isSuccess= %s, manualTrigger= %s",
        requestId,
        isSuccess,
        manualTrigger);
    @Nullable CaptionResult captionResult = captionResults.getCaptionResult(requestId);
    AccessibilityNode targetNode = null;

    if (captionResult != null) {
      targetNode = captionResult.node;
      captionResult.isGeminiFinishedWithError = !isSuccess;

      if (Objects.equals(errorReason, ErrorReason.NETWORK_ERROR)) {
        captionResult.isNetworkError = true;
      }
    }

    if (manualTrigger) {
      handleResult(
          requestId,
          targetNode,
          Result.create(IMAGE_DESCRIPTION, text),
          /* isUserRequested= */ true);
      return true;
    } else {
      // For auto-trigger, no need to announce the error message expect the ERROR_BLOCKED case.
      if (isSuccess || Objects.equals(finishReason, FinishReason.ERROR_BLOCKED)) {
        handleResult(
            requestId,
            targetNode,
            Result.create(IMAGE_DESCRIPTION, text),
            /* isUserRequested= */ false);
        return true;
      }
    }

    // Set the result text to empty to ignore the unused message. It's for recycling the images in
    // #handleResult.
    handleResult(requestId, targetNode, Result.create(IMAGE_DESCRIPTION, ""), manualTrigger);
    return true;
  }

  private void returnCaptionResult(int requestId, CaptionResult captionResult) {
    returnCaptionResult(
        requestId,
        captionResult.ocrTextResult,
        captionResult.iconLabelResult,
        captionResult.imageDescriptionResult,
        captionResult.isUserRequest,
        captionResult.isImageDescriptionByGemini,
        captionResult.isGeminiFinishedWithError,
        captionResult.isNetworkError);
  }

  private void returnCaptionResult(
      int requestId,
      @Nullable Result ocrTextResult,
      @Nullable Result iconLabelResult,
      @Nullable Result imageDescriptionResult,
      boolean isUserRequested,
      boolean isImageDescriptionByGemini,
      boolean isGeminiFinishedWithError,
      boolean isNetworkError) {
    if (Result.isEmpty(iconLabelResult)) {
      if (iconAnnotationsDetector != null) {
        analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_ICON_DETECT_NO_RESULT);
      }
    }
    if (Result.isEmpty(ocrTextResult)) {
      analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_OCR_PERFORM_SUCCEED_EMPTY);
    }
    if (Result.isEmpty(imageDescriptionResult)) {
      if (imageDescriptionProcessor != null && !isImageDescriptionByGemini) {
        analytics.onImageCaptionEvent(IMAGE_CAPTION_EVENT_IMAGE_DESCRIBE_NO_RESULT);
      }
    }

    if (isUserRequested && isImageDescriptionByGemini) {
      if (isGeminiFinishedWithError) {
        LogUtils.d(TAG, "Detailed image description finished with an error.");
        CharSequence fallbackResult = ""; // Fallback result from icon label and ocr text.
        if (!Result.isEmpty(iconLabelResult)) {
          fallbackResult =
              TextUtils.concat(
                  fallbackResult,
                  service.getString(
                      R.string.detailed_image_description_icon_result, iconLabelResult.text()));
        }

        if (!Result.isEmpty(ocrTextResult)) {
          fallbackResult =
              TextUtils.concat(
                  fallbackResult,
                  service.getString(
                      R.string.detailed_image_description_ocr_result, ocrTextResult.text()));
        }

        if (TextUtils.isEmpty(fallbackResult)) {
          // Fallback result is empty, so only announce the Gemini error result.
          returnFeedback(imageDescriptionResult.text());
        } else {
          // Gemini error with a fallback result.
          if (isNetworkError) {
            returnFeedback(
                service.getString(
                    R.string.detailed_image_description_fallback_result_network_error,
                    fallbackResult));
          } else {
            returnFeedback(
                service.getString(
                    R.string.detailed_image_description_fallback_result, fallbackResult));
          }
        }
      } else {
        LogUtils.d(TAG, "Request success, show the result dialog.");
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.displayImageCaptionResultDialog(
                requestId,
                imageDescriptionResult,
                iconLabelResult,
                ocrTextResult,
                /* isScreenDescription= */ false));
      }

      return;
    }

    // Customize the feedback if the only available result is the error message from Gemini.
    if (isGeminiFinishedWithError
        && !Result.isEmpty(imageDescriptionResult)
        && Result.isEmpty(iconLabelResult)
        && Result.isEmpty(ocrTextResult)) {
      returnFeedback(imageDescriptionResult.text());
      return;
    }

    String result =
        isUserRequested
            ? constructCaptionTextForManually(
                service, imageDescriptionResult, iconLabelResult, ocrTextResult)
            : constructCaptionTextForAuto(
                service, imageDescriptionResult, iconLabelResult, ocrTextResult);
    returnFeedback(result);
  }

  private void returnFeedback(@StringRes int text) {
    returnFeedback(service.getString(text));
  }

  /**
   * Reads out the feedback of image captioning even during playback, phone calls, speech
   * recognition, and microphone recording.
   */
  private void returnFeedback(CharSequence text) {
    pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
            text, SpeakOptions.create().setFlags(FLAG_FORCE_FEEDBACK | FLAG_NO_DEVICE_SLEEP)));
  }

  /** Sends a message to handle results timeout. */
  private void sendResultTimeoutMessage() {
    removeMessages(MSG_RESULT_TIMEOUT);

    Message message = new Message();
    message.what = MSG_RESULT_TIMEOUT;
    message.arg1 = requestId;

    sendMessageAtTime(message, System.currentTimeMillis() + RESULT_MAX_WAITING_TIME_MS);
  }

  @Override
  public void handleMessage(@NonNull Message msg) {
    super.handleMessage(msg);
    if (msg.what == MSG_RESULT_TIMEOUT) {
      // Obtains the caption result by requestId.
      @Nullable CaptionResult captionResult = captionResults.getCaptionResult(msg.arg1);
      if (captionResult != null) {
        @Nullable
        AccessibilityNode focusedNode =
            AccessibilityNode.takeOwnership(
                accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false));
        if (captionResult.node.equals(focusedNode)) {
          returnCaptionResult(msg.arg1, captionResult);
        }
      }
      LogUtils.w(TAG, "Caption request is timeout.");
      screenshotRequests.clear();
      characterCaptionRequests.clear();
      iconDetectionRequests.clear();
      imageDescriptionRequests.clear();
      captionResults.cleanUp();
    }
  }

  private ProcessedScreens processImageForNodeCaption(
      AccessibilityNodeInfoCompat node,
      Bitmap screenCapture,
      int focusStrokeWidth,
      boolean captureByWindow) {
    @Nullable AccessibilityNodeInfoCompat root = AccessibilityNodeInfoUtils.getRoot(node);
    Bitmap blockedScreenCapture;

    long startTime = SystemClock.uptimeMillis();
    blockedScreenCapture = blockOverlaps(root, node, screenCapture);
    primeLogImageCaptionEvent(
        IMAGE_CAPTION_IMAGE_PROCESS_BLOCK_OVERLAY, SystemClock.uptimeMillis() - startTime);
    if (blockedScreenCapture == null) {
      blockedScreenCapture = screenCapture;
    } else {
      if (!screenCapture.isRecycled()) {
        screenCapture.recycle();
      }
    }

    Bitmap croppedScreenCapture =
        cropImageWithNodeBounds(node, blockedScreenCapture, focusStrokeWidth, captureByWindow);

    return ProcessedScreens.create(blockedScreenCapture, croppedScreenCapture);
  }

  private void primeLogImageCaptionEvent(TimerAction timerAction, long duration) {
    if (duration > 0) {
      primesController.recordDuration(timerAction, duration);
    }
  }

  /**
   * Gets the node bounds in window coordinates.
   *
   * <p>{@link AccessibilityNodeInfoCompat#getBoundsInWindow} sometimes returns the Rect(0, 0 - 0,
   * 0) that causes an incorrect crop area. As a workaround, we calculate the node's bound by
   * offsetting it with window's bound.
   */
  private static Rect getBoundsInWindow(AccessibilityNodeInfoCompat node, Bitmap image) {
    Rect nodeBoundsInWindow = new Rect();
    node.getBoundsInWindow(nodeBoundsInWindow);
    if (nodeBoundsInWindow.left != 0
        || nodeBoundsInWindow.top != 0
        || nodeBoundsInWindow.right != 0
        || nodeBoundsInWindow.bottom != 0) {
      return nodeBoundsInWindow;
    }

    AccessibilityWindowInfoCompat window = node.getWindow();
    Rect nodeBounds = new Rect();
    if (window == null) {
      // Fallback to using platform API.
      node.getBoundsInWindow(nodeBounds);
      return nodeBounds;
    }
    Rect winBounds = new Rect();
    window.getBoundsInScreen(winBounds);
    node.getBoundsInScreen(nodeBounds);
    if (winBounds.height() < image.getHeight() || winBounds.width() < image.getWidth()) {
      LogUtils.w(TAG, "The screenshot region does not match the window bound.");
      return nodeBounds;
    }

    return new Rect(
        nodeBounds.left - winBounds.left,
        nodeBounds.top - winBounds.top,
        nodeBounds.right - winBounds.left,
        nodeBounds.bottom - winBounds.top);
  }

  private static Bitmap cropImageWithNodeBounds(
      AccessibilityNodeInfoCompat node,
      Bitmap image,
      int focusStrokeWidth,
      boolean captureByWindow) {
    if (node == null) {
      // As the original image may got recycled when returned to caller, we need a defense copy.
      return image.copy(Config.ARGB_8888, /* isMutable= */ image.isMutable());
    }

    Rect nodeBounds;
    if (captureByWindow) {
      nodeBounds = getBoundsInWindow(node, image);
    } else {
      nodeBounds = new Rect();
      node.getBoundsInScreen(nodeBounds);
    }
    // The bounds are moved inward to make the focus border exclude from the cropped image.
    nodeBounds.inset(focusStrokeWidth, focusStrokeWidth);
    Bitmap croppedImage = null;

    if (nodeBounds.height() <= image.getHeight() || nodeBounds.width() <= image.getWidth()) {
      try {
        croppedImage = BitmapUtils.cropBitmap(image, nodeBounds);
      } catch (IllegalArgumentException e) {
        LogUtils.w(TAG, e.getMessage() == null ? "Fail to crop screenshot." : e.getMessage());
      }
    }

    if (croppedImage == null) {
      croppedImage = image.copy(Config.ARGB_8888, /* isMutable= */ image.isMutable());
    }

    return croppedImage;
  }

  /** Stores the processed screens. */
  @AutoValue
  abstract static class ProcessedScreens {
    abstract Bitmap blockedScreenCapture();

    abstract Bitmap croppedScreenCapture();

    private static ProcessedScreens create(
        Bitmap blockedScreenCapture, Bitmap croppedScreenCapture) {
      return new AutoValue_ImageCaptioner_ProcessedScreens(
          blockedScreenCapture, croppedScreenCapture);
    }
  }

  /** Stores all caption results to announce them together. */
  private static class CaptionResult {
    private final AccessibilityNode node;
    private final boolean isUserRequest;
    private boolean isOcrFinished;
    @Nullable private Result ocrTextResult;
    private boolean isIconDetectionFinished;
    @Nullable private Result iconLabelResult;
    private boolean isImageDescriptionFinished;
    @Nullable private Result imageDescriptionResult;
    private final List<Bitmap> screenshots = new ArrayList<>();
    private boolean isImageDescriptionByGemini;
    private boolean isGeminiFinishedWithError;
    private boolean isNetworkError;

    private CaptionResult(AccessibilityNode node, boolean isUserRequest) {
      this(node, isUserRequest, /* isImageDescriptionByGemini= */ false);
    }

    private CaptionResult(
        AccessibilityNode node, boolean isUserRequest, boolean isImageDescriptionByGemini) {
      this.node = node;
      this.isUserRequest = isUserRequest;
      this.isImageDescriptionByGemini = isImageDescriptionByGemini;
    }

    /**
     * The screenshot uses to caption will be recycled in {@link #recycleAndClearScreenshots()} when
     * the request is finished.
     */
    public void addUsedScreenshot(Bitmap screenshot) {
      screenshots.add(screenshot);
    }

    public void recycleAndClearScreenshots() {
      LogUtils.d(TAG, "Recycle screenshots ");
      screenshots.forEach(
          screenshot -> {
            if (!screenshot.isRecycled()) {
              screenshot.recycle();
            }
          });
      screenshots.clear();
    }

    private void setOcrText(Result ocrTextResult) {
      isOcrFinished = true;
      this.ocrTextResult = ocrTextResult;
    }

    private void setIconLabel(Result iconLabelResult) {
      isIconDetectionFinished = true;
      this.iconLabelResult = iconLabelResult;
    }

    private void setImageDescription(Result imageDescriptionResult) {
      isImageDescriptionFinished = true;
      this.imageDescriptionResult = imageDescriptionResult;
    }

    private boolean isFinished() {
      return isOcrFinished && isIconDetectionFinished && isImageDescriptionFinished;
    }
  }

  /**
   * A {@link DownloadStateListener} to handle the state of the download which is triggered by
   * Talkback menu.
   */
  @VisibleForTesting
  abstract static class ManualDownloadStateListener implements DownloadStateListener {

    private final Context context;
    private final SharedPreferences prefs;
    private final Pipeline.FeedbackReturner pipeline;
    private final TalkBackAnalytics analytics;
    private final ImageCaptioner imageCapitoner;
    private final DownloadStateListenerResources listenerResources;
    private final ImageCaptionPreferenceKeys preferenceKeys;
    private final int moduleSizeInMb;
    private final ImageCaptionLogKeys logKeys;

    @VisibleForTesting
    ManualDownloadStateListener(
        Context context,
        Pipeline.FeedbackReturner pipeline,
        TalkBackAnalytics analytics,
        ImageCaptioner imageCaptioner,
        DownloadStateListenerResources listenerResources,
        ImageCaptionPreferenceKeys preferenceKeys,
        int moduleSizeInMb,
        ImageCaptionLogKeys logKeys) {
      this.context = context;
      prefs = SharedPreferencesUtils.getSharedPreferences(context);
      this.pipeline = pipeline;
      this.analytics = analytics;
      this.imageCapitoner = imageCaptioner;
      this.listenerResources = listenerResources;
      this.preferenceKeys = preferenceKeys;
      this.moduleSizeInMb = moduleSizeInMb;
      this.logKeys = logKeys;
    }

    /** Initialize the module after the module has been downloaded. */
    abstract void initialize();

    @Override
    public void onInstalled() {
      analytics.onImageCaptionEvent(logKeys.installSuccess);
      returnFeedbackUninterruptible(listenerResources.downloadSuccessfulHint);
      initialize();
    }

    @Override
    public void onFailed(@ErrorCode int errorCode) {
      analytics.onImageCaptionEvent(logKeys.installFail);
      switch (errorCode) {
        case ERROR_NETWORK_ERROR ->
            returnFeedbackUninterruptible(R.string.download_network_error_hint);
        case ERROR_INSUFFICIENT_STORAGE ->
            returnFeedbackUninterruptible(
                context.getString(R.string.download_storage_error_hint, moduleSizeInMb));
        default -> returnFeedbackUninterruptible(listenerResources.downloadFailedHint);
      }
    }

    @Override
    public void onAccepted() {
      analytics.onImageCaptionEvent(logKeys.installRequest);
      SharedPreferencesUtils.putBooleanPref(
          prefs, context.getResources(), preferenceKeys.uninstalledKey, false);

      // Clears the node which is waiting for recognition because it's unnecessary to
      // perform other captions if the user accepts the download.
      returnFeedbackUninterruptible(listenerResources.downloadingHint);
    }

    @Override
    public void onRejected() {
      analytics.onImageCaptionEvent(logKeys.installDeny);
      // Records how many times the button is tapped because the dialog shouldn't be shown
      // if the download is rejected more than three times.
      String dialogShownTimesKey = context.getString(preferenceKeys.downloadShownTimesKey);
      int dialogShownTimes = prefs.getInt(dialogShownTimesKey, /* defValue= */ 0);
      prefs.edit().putInt(dialogShownTimesKey, ++dialogShownTimes).apply();

      returnFeedback(R.string.confirm_download_negative_button_hint);
    }

    @Override
    public void onDialogDismissed(@Nullable AccessibilityNodeInfoCompat queuedNode) {
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
      // Image captioning for the node will be executed when windows are stable.
      imageCapitoner.queuedNode = queuedNode;
    }

    private void returnFeedbackUninterruptible(@StringRes int text) {
      returnFeedbackUninterruptible(context.getString(text));
    }

    private void returnFeedbackUninterruptible(CharSequence text) {
      returnFeedback(text, /* isUninterruptible= */ true);
    }

    private void returnFeedback(@StringRes int text) {
      returnFeedback(context.getString(text), /* isUninterruptible= */ false);
    }

    private void returnFeedback(CharSequence text, boolean isUninterruptible) {
      if (isUninterruptible) {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(
                text,
                SpeakOptions.create()
                    .setQueueMode(QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH_CAN_IGNORE_INTERRUPTS)
                    .setFlags(FLAG_FORCE_FEEDBACK | FLAG_NO_DEVICE_SLEEP)));
      } else {
        pipeline.returnFeedback(
            EVENT_ID_UNTRACKED,
            Feedback.speech(
                text, SpeakOptions.create().setFlags(FLAG_FORCE_FEEDBACK | FLAG_NO_DEVICE_SLEEP)));
      }
    }
  }

  /**
   * It holds the bitmap of the node and Describe-image results from different resource which
   * include image description, icon detection and OCR. The request is indexed with a requestId. To
   * avoid the heap overflow of bitmaps, the pending caption results has the upper bound of
   * MAX_WAITING_REQUESTS for auto & manual requests, respectively.
   */
  private static class CaptionResults {
    private static final int MAX_WAITING_REQUESTS = 3;
    private final Map<Integer, CaptionResult> captionResults;

    public CaptionResults() {
      captionResults = new HashMap<>();
    }

    public void cleanUp() {
      captionResults.forEach((key, value) -> value.recycleAndClearScreenshots());
      captionResults.clear();
    }

    @Nullable
    public CaptionResult getCaptionResult(int requestId) {
      return captionResults.get(requestId);
    }

    public void removeCaptionResult(int requestId) {
      captionResults.remove(requestId);
    }

    public boolean isEmpty() {
      return captionResults.isEmpty();
    }

    public void addResultAndCleanOldOnes(
        int requestId, CaptionResult captionResult, boolean isUserRequest) {
      // To avoid the bitmaps unexpectedly accumulated, we set the maximum value of waiting requests
      // and clean up all requests older than that.

      for (Iterator<Entry<Integer, CaptionResult>> it = captionResults.entrySet().iterator();
          it.hasNext(); ) {
        Map.Entry<Integer, CaptionResult> entry = it.next();
        if ((requestId - entry.getKey()) > MAX_WAITING_REQUESTS) {
          CaptionResult result = entry.getValue();
          if (isUserRequest || !result.isUserRequest) {
            entry.getValue().recycleAndClearScreenshots();
            it.remove();
          }
        }
      }
      captionResults.put(requestId, captionResult);
    }
  }
}
