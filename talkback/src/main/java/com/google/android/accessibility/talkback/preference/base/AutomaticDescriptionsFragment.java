/*
 * Copyright 2023 Google Inc.
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

import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GEMINI_OPT_IN_CONSENT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GEMINI_OPT_IN_DISSENT;
import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.GEMINI_OPT_IN_SHOW_DIALOG;
import static com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.Requester.SETTINGS;
import static com.google.android.accessibility.talkback.dynamicfeature.ModuleStateManager.setLegacyUninstall;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.FeatureSwitchDialogResources.IMAGE_DESCRIPTION_AICORE_OPT_IN;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.ICON_LABEL;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.IMAGE_DESCRIPTION;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.getAutomaticImageCaptioningState;
import static com.google.android.accessibility.talkback.imagecaption.Request.ERROR_INSUFFICIENT_STORAGE;
import static com.google.android.accessibility.talkback.imagecaption.Request.ERROR_NETWORK_ERROR;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.TwoStatePreference;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackAnalyticsImpl;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.ImageCaptioner;
import com.google.android.accessibility.talkback.actor.gemini.AiCoreEndpoint;
import com.google.android.accessibility.talkback.actor.gemini.AiCoreEndpoint.AiFeatureDownloadCallback;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.ImageCaptionLogKeys;
import com.google.android.accessibility.talkback.dynamicfeature.DownloaderFactory;
import com.google.android.accessibility.talkback.dynamicfeature.MddManager.MddNotification;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.DownloadStateListener;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.UninstallStateListener;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleStateManager;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.imagecaption.FeatureSwitchDialog;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.AutomaticImageCaptioningState;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.DownloadDialogResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.DownloadStateListenerResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.FeatureSwitchDialogResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.FeatureSwitchPreferenceKeys;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.ImageCaptionPreferenceKeys;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.UninstallDialogResources;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType;
import com.google.android.accessibility.talkback.imagecaption.Request.ErrorCode;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

/** A {@link TalkbackBaseFragment} to hold a set of automatic descriptions preferences. */
public class AutomaticDescriptionsFragment extends TalkbackBaseFragment {

  private static final String TAG = "AutomaticDescriptionsFragment";
  private final ImmutableMap<Integer, FeatureSwitchDialogResources>
      dynamicFeaturePreferenceResources =
          ImmutableMap.of(
              R.string.pref_icon_detection_key,
              FeatureSwitchDialogResources.ICON_DETECTION,
              R.string.pref_image_description_key,
              FeatureSwitchDialogResources.IMAGE_DESCRIPTION);
  private Context context;
  private TalkBackAnalytics analytics;
  private SharedPreferences prefs;
  @Nullable private ModuleStateManager moduleStateManager;
  @Nullable private AiCoreEndpoint aiCoreEndpoint;
  @Nullable private ListenableFuture<Boolean> hasAiCoreFuture;
  private Optional<Boolean> useAiCore = Optional.empty();

  public AutomaticDescriptionsFragment() {
    super(R.xml.automatic_descriptions_preferences);
  }

  @Override
  protected CharSequence getTitle() {
    return getText(R.string.title_pref_auto_image_captioning);
  }

  @Override
  public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
    super.onCreatePreferences(savedInstanceState, rootKey);
    context = getContext();
    if (context == null) {
      return;
    }

    analytics = new TalkBackAnalyticsImpl(context);
    prefs = SharedPreferencesUtils.getSharedPreferences(context);

    if (aiCoreEndpoint == null) {
      aiCoreEndpoint = new AiCoreEndpoint(context, /* withService= */ false);
    }
    hasAiCoreFuture = aiCoreEndpoint.hasAiCoreAsynchronous();

    if (moduleStateManager == null) {
      moduleStateManager =
          new ModuleStateManager(
              getContext(),
              DownloaderFactory.create(getActivity().getApplication()),
              DownloaderFactory.legacy(getActivity().getApplication()));

      moduleStateManager.setImageAndIconStateListener(
          new AutomaticDescriptionDownloadStateListener(
              getPreferencesAndResourcesForDynamicFeature(),
              DownloadStateListenerResources.ALL,
              ImageCaptionPreferenceKeys.ALL,
              DownloadDialogResources.ALL.moduleSizeInMb,
              ImageCaptionLogKeys.ALL),
          new AutomaticDescriptionUninstallStateListener(
              getPreferencesForDynamicFeature(),
              ImageCaptionPreferenceKeys.ALL,
              ImageCaptionLogKeys.ALL,
              UninstallDialogResources.ALL));
    }
    setupIconDetectionPreference();
    setupImageDescriptionPreference();
    setupTextRecognitionPreference();
    setupDetailedImageDescriptionPreference(
        FeatureSwitchDialogResources.DETAILED_IMAGE_DESCRIPTION);
  }

  @Override
  public void onResume() {
    super.onResume();

    // Cancels the download completion notification because users may be led to the settings page by
    // the notification.
    if (FeatureFlagReader.enableMdd(context)) {
      MddNotification.cancel(context);
    }

    if (useAiCore.isPresent()) {
      // Refresh the UI of the image description preference.
      Preference imageDescriptionPreference =
          findPreferenceByResId(R.string.pref_image_description_key);

      if (imageDescriptionPreference == null || !ImageCaptioner.supportsIconDetection(context)) {
        return;
      }

      if (useAiCore.get()) {
        setupPreferenceForGeminiNano(imageDescriptionPreference);
      } else {
        setupPreferenceForGarcon(imageDescriptionPreference);
      }

      setupDetailedImageDescriptionPreference(
          FeatureSwitchDialogResources.DETAILED_IMAGE_DESCRIPTION);
    }
    prefs.registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  @Override
  public void onPause() {
    super.onPause();
    prefs.unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  private final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
      new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
          if (!isAdded() || (getActivity() == null)) {
            LogUtils.w(
                TAG, "Fragment is not attached to activity, do not update the setting page.");
            return;
          }

          if (TextUtils.equals(
              key,
              getString(
                  FeatureSwitchDialogResources.DETAILED_IMAGE_DESCRIPTION
                      .switchPreferenceKeys
                      .switchKey))) {
            TwoStatePreference preference =
                findPreference(
                    getString(
                        FeatureSwitchDialogResources.DETAILED_IMAGE_DESCRIPTION
                            .switchPreferenceKeys
                            .switchKey));
            if (preference != null) {
              preference.setChecked(
                  getBooleanPref(
                      FeatureSwitchDialogResources.DETAILED_IMAGE_DESCRIPTION
                          .switchPreferenceKeys
                          .switchKey,
                      FeatureSwitchDialogResources.DETAILED_IMAGE_DESCRIPTION
                          .switchPreferenceKeys
                          .switchDefaultValue));
            }
          }
        }
      };

  @Override
  public void onDestroy() {
    if (moduleStateManager != null) {
      moduleStateManager.shutdown();
      moduleStateManager = null;
    }

    if (aiCoreEndpoint != null) {
      aiCoreEndpoint.setAiFeatureDownloadCallback(null);
      aiCoreEndpoint.onUnbind();
      aiCoreEndpoint = null;
    }

    super.onDestroy();
  }

  @VisibleForTesting
  void setModuleStateManager(ModuleStateManager moduleStateManager) {
    this.moduleStateManager = moduleStateManager;
  }

  @VisibleForTesting
  void setAiCoreEndpoint(AiCoreEndpoint endpoint) {
    if (aiCoreEndpoint != null) {
      aiCoreEndpoint.onUnbind();
    }
    aiCoreEndpoint = endpoint;
  }

  private void setupIconDetectionPreference() {
    Preference iconDetectionPreference = findPreferenceByResId(R.string.pref_icon_detection_key);
    if (iconDetectionPreference == null) {
      return;
    }

    if (!ImageCaptioner.supportsIconDetection(context)) {
      removePreference(iconDetectionPreference);
      return;
    }

    if (moduleStateManager == null) {
      LogUtils.e(TAG, "ModuleDownloadPrompterProvider hasn't been initialized");
      removePreference(iconDetectionPreference);
      return;
    }

    moduleStateManager.setIconStateListener(
        new AutomaticDescriptionDownloadStateListener(
            ImmutableMap.of(iconDetectionPreference, FeatureSwitchDialogResources.ICON_DETECTION),
            DownloadStateListenerResources.ICON_DETECTION,
            ImageCaptionPreferenceKeys.ICON_DETECTION,
            DownloadDialogResources.ICON_DETECTION.moduleSizeInMb,
            ImageCaptionLogKeys.ICON_DETECTION),
        new AutomaticDescriptionUninstallStateListener(
            ImmutableList.of(iconDetectionPreference),
            ImageCaptionPreferenceKeys.ICON_DETECTION,
            ImageCaptionLogKeys.IMAGE_DESCRIPTION,
            UninstallDialogResources.ICON_DETECTION));

    setupPreferenceForDynamicFeature(
        ICON_LABEL, iconDetectionPreference, FeatureSwitchDialogResources.ICON_DETECTION);
  }

  private void setupImageDescriptionPreference() {
    Preference imageDescriptionPreference =
        findPreferenceByResId(R.string.pref_image_description_key);
    if (imageDescriptionPreference == null) {
      return;
    }

    if (!ImageCaptioner.supportsImageDescription(context)) {
      removePreference(imageDescriptionPreference);
      return;
    }

    Futures.addCallback(
        hasAiCoreFuture,
        new FutureCallback<Boolean>() {
          @Override
          public void onSuccess(Boolean hasAiCore) {
            if (hasAiCore && GeminiConfiguration.isOnDeviceGeminiImageCaptioningEnabled(context)) {
              useAiCore = Optional.of(true);
              getActivity()
                  .runOnUiThread(() -> setupPreferenceForGeminiNano(imageDescriptionPreference));
            } else {
              useAiCore = Optional.of(false);
              getActivity()
                  .runOnUiThread(() -> setupPreferenceForGarcon(imageDescriptionPreference));
            }
            moduleStateManager.setHasAiCore(useAiCore);
          }

          @Override
          public void onFailure(Throwable t) {
            useAiCore = Optional.of(false);
            getActivity().runOnUiThread(() -> setupPreferenceForGarcon(imageDescriptionPreference));
            moduleStateManager.setHasAiCore(useAiCore);
          }
        },
        directExecutor());
  }

  /** Updates the summary of preference and sets {@link OnPreferenceClickListener}. */
  private void setupPreferenceForDynamicFeature(
      CaptionType captionType,
      Preference preference,
      FeatureSwitchDialogResources switchDialogResources) {
    if (moduleStateManager == null) {
      LogUtils.e(TAG, "ModuleStateManager hasn't initialized.");
      return;
    }
    // The summary of preference will not be saved when exiting the Settings page, so they should be
    // restored when the preference is created.
    if (moduleStateManager.isDownloading(captionType)) {
      // The module is downloading.
      preference.setSummary(R.string.summary_pref_module_downloading);
    } else if (moduleStateManager.isLibraryReady(captionType)) {
      // The module is available.
      if (!prefs.contains(
          context.getString(switchDialogResources.switchPreferenceKeys.switchKey))) {
        // The module has been downloaded and the feature is enabled by default.
        putBooleanPref(switchDialogResources.switchPreferenceKeys.switchKey, true);
      }
      preference.setSummary(
          getSummaryFromFeatureSwitchDialog(context, prefs, switchDialogResources));
    } else {
      preference.setSummary(R.string.summary_pref_auto_image_captioning_disabled);
    }

    preference.setOnPreferenceClickListener(
        pref -> {
          // Shows the download confirmation dialog or feature configuration dialog.
          if (!moduleStateManager.showDownloadDialog(captionType, SETTINGS)) {
            new FeatureSwitchDialog(context, switchDialogResources, /* isDeletable= */ true) {
              @Override
              public void handleDialogClick(int buttonClicked) {
                super.handleDialogClick(buttonClicked);
                switch (buttonClicked) {
                  case DialogInterface.BUTTON_POSITIVE -> {
                    preference.setSummary(
                        getSummaryFromFeatureSwitchDialog(context, prefs, switchDialogResources));
                    return;
                  }
                  case DialogInterface.BUTTON_NEGATIVE -> {
                    LogUtils.v(TAG, "Requests a uninstallation.");
                    moduleStateManager.showUninstallDialog(captionType);
                    return;
                  }
                  default -> {
                    // do nothing.
                  }
                }
              }
            }.showDialog();
          }
          return true;
        });
  }

  private void setupPreferenceForGarcon(Preference imageDescriptionPreference) {
    if (moduleStateManager == null) {
      LogUtils.e(TAG, "moduleStateManager hasn't been initialized");
      removePreference(imageDescriptionPreference);
      return;
    }

    moduleStateManager.setImageStateListener(
        new AutomaticDescriptionDownloadStateListener(
            ImmutableMap.of(
                imageDescriptionPreference, FeatureSwitchDialogResources.IMAGE_DESCRIPTION),
            DownloadStateListenerResources.IMAGE_DESCRIPTION,
            ImageCaptionPreferenceKeys.IMAGE_DESCRIPTION,
            DownloadDialogResources.IMAGE_DESCRIPTION.moduleSizeInMb,
            ImageCaptionLogKeys.IMAGE_DESCRIPTION),
        new AutomaticDescriptionUninstallStateListener(
            ImmutableList.of(imageDescriptionPreference),
            ImageCaptionPreferenceKeys.IMAGE_DESCRIPTION,
            ImageCaptionLogKeys.IMAGE_DESCRIPTION,
            UninstallDialogResources.IMAGE_DESCRIPTION));

    setupPreferenceForDynamicFeature(
        IMAGE_DESCRIPTION,
        imageDescriptionPreference,
        FeatureSwitchDialogResources.IMAGE_DESCRIPTION);
  }

  private void setupPreferenceForGeminiNano(Preference optInPreference) {
    FeatureSwitchDialogResources switchDialogResources;
    if (getBooleanPref(
        IMAGE_DESCRIPTION_AICORE_OPT_IN.switchPreferenceKeys.switchKey,
        IMAGE_DESCRIPTION_AICORE_OPT_IN.switchPreferenceKeys.switchDefaultValue)) {
      boolean isAiFeatureDownloading = false;
      if (aiCoreEndpoint != null) {
        isAiFeatureDownloading = aiCoreEndpoint.isAiFeatureDownloading();
        if (isAiFeatureDownloading) {
          aiCoreEndpoint.setAiFeatureDownloadCallback(
              new AiFeatureDownloadCallback() {
                @Override
                public void onDownloadProgress(long currentSizeInBytes, long totalSizeInBytes) {
                  // Do nothing.
                }

                @Override
                public void onDownloadCompleted() {
                  FragmentActivity activity = getActivity();
                  if (activity == null) {
                    LogUtils.d(TAG, "AiFeature download completed - The activity is null.");
                    return;
                  }
                  getActivity().runOnUiThread(() -> setupPreferenceForGeminiNano(optInPreference));
                }
              });
        }
      }

      if (isAiFeatureDownloading) {
        optInPreference.setSummary(R.string.summary_pref_module_downloading);
        optInPreference.setOnPreferenceClickListener(
            pref -> {
              showToast(getActivity(), R.string.message_aifeature_downloading);
              return true;
            });
      } else {
        switchDialogResources = FeatureSwitchDialogResources.IMAGE_DESCRIPTION_AICORE_SCOPE;
        optInPreference.setSummary(
            getSummaryFromFeatureSwitchDialog(context, prefs, switchDialogResources));
        optInPreference.setOnPreferenceClickListener(
            pref -> {
              if (displayDialogForGeminiNano(optInPreference)) {
                return true;
              }

              new FeatureSwitchDialog(context, switchDialogResources, /* isDeletable= */ false) {
                @Override
                public void handleDialogClick(int buttonClicked) {
                  super.handleDialogClick(buttonClicked);
                  switch (buttonClicked) {
                    case DialogInterface.BUTTON_POSITIVE -> {
                      setupPreferenceForGeminiNano(optInPreference);
                      return;
                    }
                    case DialogInterface.BUTTON_NEGATIVE -> {
                      return;
                    }
                    default -> {
                      // do nothing.
                    }
                  }
                }
              }.setIncludeNegativeButton(false).showDialog();

              return true;
            });
      }
    } else {
      switchDialogResources = IMAGE_DESCRIPTION_AICORE_OPT_IN;
      FeatureSwitchDialog optinDialog =
          new FeatureSwitchDialog(
              context, switchDialogResources, /* isDeletable= */ false, R.string.enable_gemini) {
            @Override
            public void handleDialogClick(int buttonClicked) {
              switch (buttonClicked) {
                case DialogInterface.BUTTON_POSITIVE -> {
                  SharedPreferencesUtils.putBooleanPref(
                      prefs,
                      context.getResources(),
                      switchDialogResources.switchPreferenceKeys.switchKey,
                      true);
                  setupPreferenceForGeminiNano(optInPreference);
                  analytics.onGeminiOptInFromSettings(
                      GEMINI_OPT_IN_CONSENT, /* serverSide= */ false);
                  return;
                }
                case DialogInterface.BUTTON_NEGATIVE -> {
                  analytics.onGeminiOptInFromSettings(
                      GEMINI_OPT_IN_DISSENT, /* serverSide= */ false);
                  return;
                }
                default -> {
                  // do nothing.
                }
              }
            }
          };

      optInPreference.setSummary(R.string.summary_pref_auto_image_captioning_disabled);
      optInPreference.setOnPreferenceClickListener(
          pref -> {
            if (displayDialogForGeminiNano(optInPreference)) {
              return true;
            }

            optinDialog.showDialog();
            analytics.onGeminiOptInFromSettings(GEMINI_OPT_IN_SHOW_DIALOG, /* serverSide= */ false);
            return true;
          });
    }
  }

  private boolean displayDialogForGeminiNano(Preference optInPreference) {
    if (aiCoreEndpoint != null) {
      if (aiCoreEndpoint.needAiCoreUpdate()) {
        aiCoreEndpoint.displayAiCoreUpdateDialog();
        return true;
      } else if (aiCoreEndpoint.needAstreaUpdate()) {
        aiCoreEndpoint.displayAstreaUpdateDialog();
        return true;
      } else if (aiCoreEndpoint.isAiFeatureDownloadable()) {
        // Show the feature download dialog if the feature state backs to DOWNLOADABLE.
        aiCoreEndpoint.displayAiFeatureDownloadDialog(
            unused -> {
              setupPreferenceForGeminiNano(optInPreference);
            });
        return true;
      }
    }

    return false;
  }

  private ImmutableMap<Preference, FeatureSwitchDialogResources>
      getPreferencesAndResourcesForDynamicFeature() {
    Map<Preference, FeatureSwitchDialogResources> preferencesAndResources = new HashMap<>();
    for (Entry<Integer, FeatureSwitchDialogResources> prefResource :
        dynamicFeaturePreferenceResources.entrySet()) {
      Preference preference = findPreferenceByResId(prefResource.getKey());
      if (preference == null) {
        continue;
      }
      preferencesAndResources.put(preference, prefResource.getValue());
    }
    return ImmutableMap.copyOf(preferencesAndResources);
  }

  private ImmutableList<Preference> getPreferencesForDynamicFeature() {
    List<Preference> preferences = new ArrayList<>();
    for (Entry<Integer, FeatureSwitchDialogResources> prefResource :
        dynamicFeaturePreferenceResources.entrySet()) {
      Preference preference = findPreferenceByResId(prefResource.getKey());
      if (preference == null) {
        continue;
      }
      preferences.add(preference);
    }
    return ImmutableList.copyOf(preferences);
  }

  private boolean getBooleanPref(int key, int defaultValue) {
    return SharedPreferencesUtils.getBooleanPref(prefs, context.getResources(), key, defaultValue);
  }

  private void setupDetailedImageDescriptionPreference(
      FeatureSwitchDialogResources switchDialogResources) {
    TwoStatePreference optInPreference =
        (TwoStatePreference) findPreferenceByResId(R.string.pref_detailed_image_description_key);
    if (optInPreference == null) {
      return;
    }
    optInPreference.setChecked(
        getBooleanPref(
            switchDialogResources.switchPreferenceKeys.switchKey,
            switchDialogResources.switchPreferenceKeys.switchDefaultValue));
    optInPreference.setOnPreferenceChangeListener(
        (preference, newValue) -> {
          if (Boolean.TRUE.equals(newValue)) {
            new FeatureSwitchDialog(
                context, switchDialogResources, /* isDeletable= */ false, R.string.enable_gemini) {
              @Override
              public void handleDialogClick(int buttonClicked) {
                super.handleDialogClick(buttonClicked);
                switch (buttonClicked) {
                  case DialogInterface.BUTTON_POSITIVE -> {
                    optInPreference.setChecked(true);
                    analytics.onGeminiOptInFromSettings(
                        GEMINI_OPT_IN_CONSENT, /* serverSide= */ true);
                    return;
                  }
                  case DialogInterface.BUTTON_NEGATIVE -> {
                    LogUtils.v(TAG, "Does not accept the Opt-in.");
                    analytics.onGeminiOptInFromSettings(
                        GEMINI_OPT_IN_DISSENT, /* serverSide= */ true);
                    return;
                  }
                  default -> {
                    // do nothing.
                  }
                }
              }
            }.showDialog();
            analytics.onGeminiOptInFromSettings(GEMINI_OPT_IN_SHOW_DIALOG, /* serverSide= */ true);
            return false;
          } else {
            return true;
          }
        });
  }

  private void setupTextRecognitionPreference() {
    Preference textRecognitionPreference =
        findPreferenceByResId(R.string.pref_text_recognition_key);
    if (textRecognitionPreference == null) {
      return;
    }

    textRecognitionPreference.setSummary(
        getSummaryFromFeatureSwitchDialog(
            context, prefs, FeatureSwitchDialogResources.TEXT_RECOGNITION));

    textRecognitionPreference.setOnPreferenceClickListener(
        preference -> {
          new FeatureSwitchDialog(
              context, FeatureSwitchDialogResources.TEXT_RECOGNITION, /* isDeletable= */ false) {
            @Override
            public void handleDialogClick(int buttonClicked) {
              super.handleDialogClick(buttonClicked);
              textRecognitionPreference.setSummary(
                  getSummaryFromFeatureSwitchDialog(
                      context, prefs, FeatureSwitchDialogResources.TEXT_RECOGNITION));
            }
          }.showDialog();
          return true;
        });
  }

  private void updatePreferenceSummary(
      ImmutableList<Preference> preferences, @StringRes int summary) {
    for (Preference preference : preferences) {
      updatePreferenceSummary(preference, summary);
    }
  }

  private void updatePreferenceSummary(Preference preference, @StringRes int summary) {
    if (!isVisible() || !preference.isVisible()) {
      // The fragment is stopped, the preference needn't be updated.
      return;
    }
    getActivity().runOnUiThread(() -> preference.setSummary(summary));
  }

  private void removePreference(Preference preference) {
    getPreferenceScreen().removePreference(preference);
  }

  private void putBooleanPref(int key, boolean value) {
    SharedPreferencesUtils.putBooleanPref(prefs, context.getResources(), key, value);
  }

  private static void showToast(FragmentActivity activity, @StringRes int text) {
    activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_LONG).show());
  }

  private static void showToast(FragmentActivity activity, String text) {
    activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_LONG).show());
  }

  @StringRes
  private static int getSummaryFromFeatureSwitchDialog(
      Context context, SharedPreferences prefs, FeatureSwitchDialogResources featureResource) {
    AutomaticImageCaptioningState state =
        getAutomaticImageCaptioningState(context, prefs, featureResource);
    switch (state) {
      case ON_ALL_IMAGES -> {
        if (featureResource == FeatureSwitchDialogResources.ICON_DETECTION) {
          return R.string.title_pref_enable_auto_icon_detection;
        } else {
          return R.string.title_pref_enable_auto_image_caption;
        }
      }
      case ON_UNLABELLED_ONLY -> {
        if (featureResource == FeatureSwitchDialogResources.ICON_DETECTION) {
          return R.string.title_pref_enable_auto_icon_detection_unlabelled_only;
        } else {
          return R.string.title_pref_enable_auto_image_caption_unlabelled_only;
        }
      }
      case OFF -> {
        if (featureResource == FeatureSwitchDialogResources.ICON_DETECTION) {
          return R.string.summary_pref_auto_icon_detection_disabled;
        } else {
          return R.string.summary_pref_auto_image_captioning_disabled;
        }
      }
    }

    return -1;
  }

  private class AutomaticDescriptionDownloadStateListener implements DownloadStateListener {

    private final ImmutableMap<Preference, FeatureSwitchDialogResources> preferences;
    private final DownloadStateListenerResources listenerResources;
    private final ImageCaptionPreferenceKeys preferenceKeys;
    private final int moduleSize;
    private final ImageCaptionLogKeys logKeys;

    private AutomaticDescriptionDownloadStateListener(
        ImmutableMap<Preference, FeatureSwitchDialogResources> preferences,
        DownloadStateListenerResources listenerResources,
        ImageCaptionPreferenceKeys preferenceKeys,
        int moduleSize,
        ImageCaptionLogKeys logKeys) {
      this.preferences = preferences;
      this.listenerResources = listenerResources;
      this.preferenceKeys = preferenceKeys;
      this.moduleSize = moduleSize;
      this.logKeys = logKeys;
    }

    @Override
    public void onInstalled() {
      for (Entry<Preference, FeatureSwitchDialogResources> preference : preferences.entrySet()) {
        updatePreferenceSummary(
            preference.getKey(),
            getSummaryFromFeatureSwitchDialog(context, prefs, preference.getValue()));
      }

      // Message will send to TTS directly if TalkBack is active, no need to show the toast.
      if (!TalkBackService.isServiceActive()) {
        analytics.onImageCaptionEventFromSettings(logKeys.installSuccess);
        showToast(getActivity(), listenerResources.downloadSuccessfulHint);
      }
    }

    @Override
    public void onFailed(@ErrorCode int errorCode) {
      updatePreferenceSummary(
          preferences.keySet().asList(), R.string.summary_pref_auto_image_captioning_disabled);
      // Message will send to TTS directly if TalkBack is active, no need to show the toast.
      if (!TalkBackService.isServiceActive()) {
        analytics.onImageCaptionEventFromSettings(logKeys.installFail);
        switch (errorCode) {
          case ERROR_NETWORK_ERROR ->
              showToast(getActivity(), R.string.download_network_error_hint);
          case ERROR_INSUFFICIENT_STORAGE ->
              showToast(getActivity(), getString(R.string.download_storage_error_hint, moduleSize));
          default -> showToast(getActivity(), listenerResources.downloadFailedHint);
        }
      }
    }

    @Override
    public void onAccepted() {
      analytics.onImageCaptionEventFromSettings(logKeys.installRequest);
      updatePreferenceSummary(
          preferences.keySet().asList(), R.string.summary_pref_module_downloading);
      putBooleanPref(preferenceKeys.uninstalledKey, false);
    }

    @Override
    public void onRejected() {
      analytics.onImageCaptionEventFromSettings(logKeys.installDeny);
    }

    @Override
    public void onDialogDismissed(@Nullable AccessibilityNodeInfoCompat queuedNode) {}
  }

  private class AutomaticDescriptionUninstallStateListener implements UninstallStateListener {

    private final ImmutableList<Preference> preferences;
    private final ImageCaptionPreferenceKeys preferenceKeys;
    private final ImageCaptionLogKeys logKeys;
    private final UninstallDialogResources uninstallDialogResources;

    private AutomaticDescriptionUninstallStateListener(
        ImmutableList<Preference> preferences,
        ImageCaptionPreferenceKeys preferenceKeys,
        ImageCaptionLogKeys logKeys,
        UninstallDialogResources uninstallDialogResources) {
      this.preferences = preferences;
      this.preferenceKeys = preferenceKeys;
      this.logKeys = logKeys;
      this.uninstallDialogResources = uninstallDialogResources;
    }

    @Override
    public void onAccepted() {
      analytics.onImageCaptionEventFromSettings(logKeys.uninstallRequest);
      updatePreferenceSummary(preferences, R.string.summary_pref_auto_image_captioning_disabled);
      Editor editor = prefs.edit();
      editor
          .putBoolean(context.getString(preferenceKeys.uninstalledKey), true)
          // Uninstall lib guarantees the installed key to be false.
          .putBoolean(context.getString(preferenceKeys.installedKey), false);
      for (FeatureSwitchPreferenceKeys keys : preferenceKeys.switchPreferenceKeys) {
        editor
            .putBoolean(context.getString(keys.switchKey), false)
            // The key will be set as default once the library is uninstalled.
            .remove(context.getString(keys.switchOnUnlabelledOnlyKey));
      }
      editor.apply();
      showToast(getActivity(), uninstallDialogResources.deletedHintRes);
      setLegacyUninstall(context, prefs);
    }

    @Override
    public void onRejected() {
      analytics.onImageCaptionEventFromSettings(logKeys.uninstallDeny);
    }
  }
}
