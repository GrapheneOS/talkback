/*
 * Copyright (C) 2024 Google Inc.
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
package com.google.android.accessibility.talkback.dynamicfeature;

import static com.google.android.accessibility.talkback.actor.ImageCaptioner.supportsIconDetection;
import static com.google.android.accessibility.talkback.actor.ImageCaptioner.supportsImageDescription;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.DownloadStateListener;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.Requester;
import com.google.android.accessibility.talkback.dynamicfeature.ModuleDownloadPrompter.UninstallStateListener;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.ImageCaptionPreferenceKeys;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Optional;

// TODO: b/319748366 - Add ModuleStateManagerTest.
/** A class to manage download prompters. */
public class ModuleStateManager {

  private static final String TAG = "ModuleStateManager";

  @SuppressWarnings("NonFinalStaticField")
  private static boolean supportCombinedDownload = true;

  private final Context context;
  private final ModuleDownloadPrompterProvider moduleDownloadPrompterProvider;
  private final Downloader downloader;
  private final Optional<Downloader> downloaderLegacy;

  public ModuleStateManager(
      Context context, Downloader downloader, @Nullable Downloader downloaderLegacy) {
    this(
        context,
        downloader,
        downloaderLegacy,
        new ModuleDownloadPrompterProvider(context, downloader, downloaderLegacy));
  }

  @VisibleForTesting
  public ModuleStateManager(
      Context context,
      Downloader downloader,
      @Nullable Downloader downloaderLegacy,
      ModuleDownloadPrompterProvider moduleDownloadPrompterProvider) {
    this.context = context;
    this.moduleDownloadPrompterProvider = moduleDownloadPrompterProvider;
    this.downloader = downloader;
    this.downloaderLegacy = Optional.ofNullable(downloaderLegacy);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    moduleDownloadPrompterProvider.setPipeline(pipeline);
  }

  public void setActorState(ActorState actorState) {
    moduleDownloadPrompterProvider.setActorState(actorState);
  }

  public void setDownloadStateListener(
      DownloadStateListener imageAndIconDownloadStateListener,
      DownloadStateListener iconDownloadStateListener,
      DownloadStateListener imageDownloadStateListener) {
    moduleDownloadPrompterProvider.setDownloadStateListener(
        imageAndIconDownloadStateListener, iconDownloadStateListener, imageDownloadStateListener);
  }

  public void setImageAndIconStateListener(
      DownloadStateListener downloadStateListener, UninstallStateListener uninstallStateListener) {
    moduleDownloadPrompterProvider.setImageAndIconStateListener(
        downloadStateListener, uninstallStateListener);
  }

  public void setIconStateListener(
      DownloadStateListener downloadStateListener, UninstallStateListener uninstallStateListener) {
    moduleDownloadPrompterProvider.setIconStateListener(
        downloadStateListener, uninstallStateListener);
  }

  public void setImageStateListener(
      DownloadStateListener downloadStateListener, UninstallStateListener uninstallStateListener) {
    moduleDownloadPrompterProvider.setImageStateListener(
        downloadStateListener, uninstallStateListener);
  }

  public void setHasAiCore(Optional<Boolean> hasAiCore) {
    moduleDownloadPrompterProvider.setHasAiCore(hasAiCore);
  }

  public void initialize(Context context) {
    downloader.initialize(context);
    downloaderLegacy.ifPresent(legacy -> legacy.initialize(context));
  }

  public void shutdown() {
    moduleDownloadPrompterProvider.shutdown();
  }

  /** Checks if the legacy module or module has downloaded but hasn't been uninstalled.l */
  public boolean isLibraryReady(CaptionType captionType) {
    return switch (captionType) {
      case ICON_LABEL ->
          supportsIconDetection(context)
              && (isLegacyModuleInstalled(
                      moduleDownloadPrompterProvider.getSingleIconDetectionModuleDownloadPrompter())
                  || isModuleInstalled(
                      moduleDownloadPrompterProvider.getIconDetectionModuleDownloadPrompter())
                  || isModuleInstalled(
                      moduleDownloadPrompterProvider
                          .getSingleIconDetectionModuleDownloadPrompter()));
      case IMAGE_DESCRIPTION ->
          supportsImageDescription(context)
              && (isLegacyModuleInstalled(
                      moduleDownloadPrompterProvider
                          .getSingleImageDescriptionModuleDownloadPrompter())
                  || isModuleInstalled(
                      moduleDownloadPrompterProvider.getImageDescriptionModuleDownloadPrompter()));
      default -> {
        LogUtils.e(TAG, "isLibraryReady() with unexpected CaptionType.");
        yield false;
      }
    };
  }

  /** Checks if the library can be downloaded. */
  public boolean needDownloadDialog(CaptionType type, Requester requester) {
    return switch (type) {
      case ICON_LABEL ->
          !isLibraryReady(type)
              && moduleDownloadPrompterProvider
                  .getIconDetectionModuleDownloadPrompter()
                  .needDownloadDialog(requester);
      case IMAGE_DESCRIPTION ->
          !isLibraryReady(type)
              && moduleDownloadPrompterProvider
                  .getImageDescriptionModuleDownloadPrompter()
                  .needDownloadDialog(requester);
      default -> {
        LogUtils.e(TAG, "needDownloadDialog() with unexpected CaptionType.");
        yield false;
      }
    };
  }

  /** Shows a download dialog for a downloadable module. */
  public boolean showDownloadDialog(CaptionType captionType, Requester requester) {
    return showDownloadDialog(captionType, requester, /* node= */ null);
  }

  /** Shows a download dialog for a downloadable module. */
  public boolean showDownloadDialog(
      CaptionType captionType, Requester requester, @Nullable AccessibilityNodeInfoCompat node) {
    ModuleDownloadPrompter moduleDownloadPrompter;
    switch (captionType) {
      case ICON_LABEL ->
          moduleDownloadPrompter =
              moduleDownloadPrompterProvider.getIconDetectionModuleDownloadPrompter();
      case IMAGE_DESCRIPTION ->
          moduleDownloadPrompter =
              moduleDownloadPrompterProvider.getImageDescriptionModuleDownloadPrompter();
      default -> {
        LogUtils.e(TAG, "showDownloadDialog() with unexpected CaptionType.");
        return false;
      }
    }
    if (needDownloadDialog(captionType, requester)) {
      if (node != null) {
        moduleDownloadPrompter.setCaptionNode(node);
      }
      moduleDownloadPrompter.showDownloadDialog(requester);
      return true;
    }
    return false;
  }

  /** Shows a uninstallation dialog for a downloaded module. */
  public void showUninstallDialog(CaptionType captionType) {
    switch (captionType) {
      case ICON_LABEL -> {
        {
          ModuleDownloadPrompter moduleDownloadPrompter =
              moduleDownloadPrompterProvider.getIconDetectionModuleDownloadPrompter();
          if (moduleDownloadPrompter.isModuleAvailable()) {
            moduleDownloadPrompter.showUninstallDialog();
          } else {
            moduleDownloadPrompterProvider
                .getSingleIconDetectionModuleDownloadPrompter()
                .showUninstallDialog();
          }
        }
        return;
      }
      case IMAGE_DESCRIPTION -> {
        {
          ModuleDownloadPrompter moduleDownloadPrompter =
              moduleDownloadPrompterProvider.getImageDescriptionModuleDownloadPrompter();
          if (moduleDownloadPrompter.isModuleAvailable()) {
            moduleDownloadPrompter.showUninstallDialog();
          } else {
            moduleDownloadPrompterProvider
                .getSingleImageDescriptionModuleDownloadPrompter()
                .showUninstallDialog();
          }
        }
        return;
      }
      default -> {}
    }
  }

  /** Gets the announcement of the module state. */
  public int getStateAnnouncement(CaptionType captionType) {
    ModuleDownloadPrompter moduleDownloadPrompter;
    switch (captionType) {
      case ICON_LABEL ->
          moduleDownloadPrompter =
              moduleDownloadPrompterProvider.getIconDetectionModuleDownloadPrompter();
      case IMAGE_DESCRIPTION ->
          moduleDownloadPrompter =
              moduleDownloadPrompterProvider.getImageDescriptionModuleDownloadPrompter();
      default -> {
        LogUtils.e(TAG, "getStateAnnouncement() with unexpected CaptionType");
        return -1;
      }
    }

    return moduleDownloadPrompter.isModuleAvailable()
        ? moduleDownloadPrompter.getDownloadSuccessfulHint()
        : moduleDownloadPrompter.getDownloadingHint();
  }

  /** Installs the downloaded module. */
  public boolean installModule(CaptionType captionType) {
    return switch (captionType) {
      case ICON_LABEL ->
          moduleDownloadPrompterProvider
              .getSingleIconDetectionModuleDownloadPrompter()
              .installModule();
      case IMAGE_DESCRIPTION ->
          moduleDownloadPrompterProvider
              .getSingleImageDescriptionModuleDownloadPrompter()
              .installModule();
      default -> {
        LogUtils.e(TAG, "installModule() with unexpected CaptionType.");
        yield false;
      }
    };
  }

  /** Checks if the legacy module is uninstalled. */
  public boolean isLegacyUninstalled(CaptionType captionType) {
    return switch (captionType) {
      case ICON_LABEL ->
          moduleDownloadPrompterProvider
              .getSingleIconDetectionModuleDownloadPrompter()
              .isUninstalled();
      case IMAGE_DESCRIPTION ->
          moduleDownloadPrompterProvider
              .getSingleImageDescriptionModuleDownloadPrompter()
              .isUninstalled();
      default -> {
        LogUtils.e(TAG, "isLegacyUninstalled() with unexpected CaptionType");
        yield true;
      }
    };
  }

  /** Checks the combined download module is uninstalled. */
  public boolean isCombinedUninstalled() {
    return moduleDownloadPrompterProvider.getImageAndIconModuleDownloadPrompter().isUninstalled();
  }

  /** Checks if the module is downloading. */
  public boolean isDownloading(CaptionType captionType) {
    return switch (captionType) {
      case ICON_LABEL ->
          moduleDownloadPrompterProvider
              .getIconDetectionModuleDownloadPrompter()
              .isModuleDownloading();
      case IMAGE_DESCRIPTION ->
          moduleDownloadPrompterProvider
              .getImageDescriptionModuleDownloadPrompter()
              .isModuleDownloading();
      default -> {
        LogUtils.e(TAG, "isDownloading() with unexpected CaptionType.");
        yield false;
      }
    };
  }

  @VisibleForTesting
  public ModuleDownloadPrompter getIconDetectionModuleDownloadPrompter() {
    return moduleDownloadPrompterProvider.getIconDetectionModuleDownloadPrompter();
  }

  @VisibleForTesting
  public ModuleDownloadPrompter getImageDescriptionModuleDownloadPrompter() {
    return moduleDownloadPrompterProvider.getImageDescriptionModuleDownloadPrompter();
  }

  /** Sets all legacy modules are uninstalled. */
  public static void setLegacyUninstall(Context context, SharedPreferences prefs) {
    prefs
        .edit()
        .putBoolean(
            context.getString(ImageCaptionPreferenceKeys.ICON_DETECTION.uninstalledKey), true)
        .putBoolean(
            context.getString(ImageCaptionPreferenceKeys.ICON_DETECTION.installedKey), false)
        .putBoolean(
            context.getString(ImageCaptionPreferenceKeys.IMAGE_DESCRIPTION.uninstalledKey), true)
        .putBoolean(
            context.getString(ImageCaptionPreferenceKeys.IMAGE_DESCRIPTION.installedKey), false)
        .apply();
  }

  /**
   * Checks if modules are installed.
   *
   * @return false if modules haven't been downloaded or have been uninstalled.
   */
  private static boolean isModuleInstalled(ModuleDownloadPrompter moduleDownloadPrompter) {
    return moduleDownloadPrompter.isModuleAvailable() && !moduleDownloadPrompter.isUninstalled();
  }

  /**
   * Checks if legacy modules are installed.
   *
   * @return false if legacy modules haven't been downloaded or have been uninstalled.
   */
  private static boolean isLegacyModuleInstalled(ModuleDownloadPrompter moduleDownloadPrompter) {
    return moduleDownloadPrompter.isLegacyModuleAvailable()
        && !moduleDownloadPrompter.isUninstalled();
  }

  @VisibleForTesting
  public static void setSupportCombinedDownload(boolean supportCombinedDownload) {
    ModuleStateManager.supportCombinedDownload = supportCombinedDownload;
  }

  /** Provides {@link ModuleDownloadPrompter} instances for different situations. */
  @VisibleForTesting
  public static class ModuleDownloadPrompterProvider {
    private final boolean isIconSingle;
    private final boolean isImageSingle;

    @Nullable private ActorState actorState;
    private Optional<Boolean> hasAiCore = Optional.empty();

    private final ImageAndIconModuleDownloadPrompter imageAndIconModuleDownloadPrompter;
    private final IconDetectionModuleDownloadPrompter iconDetectionModuleDownloadPrompter;
    private final ImageDescriptionModuleDownloadPrompter imageDescriptionModuleDownloadPrompter;

    public ModuleDownloadPrompterProvider(
        Context context, Downloader downloader, @Nullable Downloader downloaderLegacy) {
      this(
          context,
          new ImageAndIconModuleDownloadPrompter(context, downloader, downloaderLegacy),
          new IconDetectionModuleDownloadPrompter(context, downloader, downloaderLegacy),
          new ImageDescriptionModuleDownloadPrompter(context, downloader, downloaderLegacy));
    }

    @VisibleForTesting
    public ModuleDownloadPrompterProvider(
        Context context,
        ImageAndIconModuleDownloadPrompter imageAndIconModuleDownloadPrompter,
        IconDetectionModuleDownloadPrompter iconDetectionModuleDownloadPrompter,
        ImageDescriptionModuleDownloadPrompter imageDescriptionModuleDownloadPrompter) {
      isIconSingle =
          !supportCombinedDownload
              // The AAB downloader does not support combined downloads.
              || !FeatureFlagReader.enableMdd(context)
              || !supportsImageDescription(context);
      isImageSingle =
          !supportCombinedDownload
              // The AAB downloader does not support combined downloads.
              || !FeatureFlagReader.enableMdd(context)
              || !supportsIconDetection(context);
      this.imageAndIconModuleDownloadPrompter = imageAndIconModuleDownloadPrompter;
      this.iconDetectionModuleDownloadPrompter = iconDetectionModuleDownloadPrompter;
      this.imageDescriptionModuleDownloadPrompter = imageDescriptionModuleDownloadPrompter;
    }

    public void setPipeline(Pipeline.FeedbackReturner pipeline) {
      imageAndIconModuleDownloadPrompter.setPipeline(pipeline);
      iconDetectionModuleDownloadPrompter.setPipeline(pipeline);
      imageDescriptionModuleDownloadPrompter.setPipeline(pipeline);
    }

    public void setActorState(ActorState actorState) {
      this.actorState = actorState;
    }

    public void setDownloadStateListener(
        DownloadStateListener imageAndIconDownloadStateListener,
        DownloadStateListener iconDownloadStateListener,
        DownloadStateListener imageDownloadStateListener) {
      imageAndIconModuleDownloadPrompter.setDownloadStateListener(
          imageAndIconDownloadStateListener);
      iconDetectionModuleDownloadPrompter.setDownloadStateListener(iconDownloadStateListener);
      imageDescriptionModuleDownloadPrompter.setDownloadStateListener(imageDownloadStateListener);
    }

    public void setImageAndIconStateListener(
        DownloadStateListener downloadStateListener,
        UninstallStateListener uninstallStateListener) {
      imageAndIconModuleDownloadPrompter.setDownloadStateListener(downloadStateListener);
      imageAndIconModuleDownloadPrompter.setUninstallStateListener(uninstallStateListener);
    }

    public void setIconStateListener(
        DownloadStateListener downloadStateListener,
        UninstallStateListener uninstallStateListener) {
      iconDetectionModuleDownloadPrompter.setDownloadStateListener(downloadStateListener);
      iconDetectionModuleDownloadPrompter.setUninstallStateListener(uninstallStateListener);
    }

    public void setImageStateListener(
        DownloadStateListener downloadStateListener,
        UninstallStateListener uninstallStateListener) {
      imageDescriptionModuleDownloadPrompter.setDownloadStateListener(downloadStateListener);
      imageDescriptionModuleDownloadPrompter.setUninstallStateListener(uninstallStateListener);
    }

    /**
     * Returns a {@link ModuleDownloadPrompter} for the combined module, which includes image
     * description and icon detection.
     */
    public ModuleDownloadPrompter getImageAndIconModuleDownloadPrompter() {
      return imageAndIconModuleDownloadPrompter;
    }

    /**
     * Returns a {@link ModuleDownloadPrompter} for the combined module if it supports combined
     * download; otherwise, returns a a {@link ModuleDownloadPrompter} for the icon detection
     * module.
     */
    public ModuleDownloadPrompter getIconDetectionModuleDownloadPrompter() {
      return isIconSingle || hasAiCore()
          ? iconDetectionModuleDownloadPrompter
          : imageAndIconModuleDownloadPrompter;
    }

    /**
     * Returns a {@link ModuleDownloadPrompter} for the combined module if it supports combined
     * download; otherwise, returns a a {@link ModuleDownloadPrompter} for the image description
     * module.
     */
    public ModuleDownloadPrompter getImageDescriptionModuleDownloadPrompter() {
      if (hasAiCore()) {
        LogUtils.w(TAG, "The device has AiCore shouldn't use garcon.");
        return imageDescriptionModuleDownloadPrompter;
      }

      if (isImageSingle) {
        return imageDescriptionModuleDownloadPrompter;
      } else {
        return imageAndIconModuleDownloadPrompter;
      }
    }

    /** Returns a {@link ModuleDownloadPrompter} for the icon detection module. */
    public ModuleDownloadPrompter getSingleIconDetectionModuleDownloadPrompter() {
      return iconDetectionModuleDownloadPrompter;
    }

    /** Returns a {@link ModuleDownloadPrompter} for the image description module. */
    public ModuleDownloadPrompter getSingleImageDescriptionModuleDownloadPrompter() {
      return imageDescriptionModuleDownloadPrompter;
    }

    public void shutdown() {
      imageAndIconModuleDownloadPrompter.shutdown();
      iconDetectionModuleDownloadPrompter.shutdown();
      imageDescriptionModuleDownloadPrompter.shutdown();
    }

    private void setHasAiCore(Optional<Boolean> hasAiCore) {
      this.hasAiCore = hasAiCore;
    }

    private boolean hasAiCore() {
      return hasAiCore.orElseGet(
          () -> actorState != null && actorState.getGeminiState().hasAiCore());
    }
  }
}
