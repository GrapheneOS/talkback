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

package com.google.android.accessibility.talkback.imagecaption;

import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.AutomaticImageCaptioningState.OFF;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.AutomaticImageCaptioningState.ON_ALL_IMAGES;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.AutomaticImageCaptioningState.ON_UNLABELLED_ONLY;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.ICON_LABEL;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.CaptionType.IMAGE_DESCRIPTION;
import static java.lang.Math.max;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.text.TextUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.AutomaticImageCaptioningState;
import com.google.android.accessibility.talkback.imagecaption.ImageCaptionConstants.FeatureSwitchDialogResources;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.ClassLoadingCache;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.Role.RoleName;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.WindowUtils;

/** Utils class for Image Caption feature. */
public class ImageCaptionUtils {

  /** Type of image caption. */
  public enum CaptionType {
    OCR,
    ICON_LABEL,
    IMAGE_DESCRIPTION,
    SCREEN_OVERVIEW,
  }

  /** The height restriction is used to avoid the cases when an OpenGL view is one big leaf node. */
  @VisibleForTesting static final int MAX_CAPTION_ABLE_LEAF_VIEW_HEIGHT_IN_DP = 150;

  @VisibleForTesting static final int MIN_IMAGE_SIZE_FOR_AUTOMATIC_IMAGE_CAPTIONING_IN_DP = 60;

  private static final boolean ENABLE_CAPTION_FOR_LEAF_VIEW = true;

  /** Returns the state of automatic image captioning features. */
  public static AutomaticImageCaptioningState getAutomaticImageCaptioningState(
      Context context,
      SharedPreferences prefs,
      FeatureSwitchDialogResources switchDialogResources) {
    return SharedPreferencesUtils.getBooleanPref(
            prefs,
            context.getResources(),
            switchDialogResources.switchPreferenceKeys.switchKey,
            switchDialogResources.switchPreferenceKeys.switchDefaultValue)
        ? (!FeatureFlagReader.enableAutomaticCaptioningForAllImages(context)
                || SharedPreferencesUtils.getBooleanPref(
                    prefs,
                    context.getResources(),
                    switchDialogResources.switchPreferenceKeys.switchOnUnlabelledOnlyKey,
                    switchDialogResources.switchPreferenceKeys.switchOnUnlabelledOnlyDefaultValue))
            ? ON_UNLABELLED_ONLY
            : ON_ALL_IMAGES
        : OFF;
  }

  /**
   * Constructs the result text for manually triggered image caption. Returns empty string if all of
   * the arguments are empty or null.
   */
  public static String constructCaptionTextForManually(
      Context context,
      @Nullable Result imageDescriptionResult,
      @Nullable Result iconLabelResult,
      @Nullable Result ocrTextResult) {
    StringBuilder stringBuilder = new StringBuilder();
    // Order: Image -> Icon -> Text (OCR).
    if (!Result.isEmpty(imageDescriptionResult)) {
      stringBuilder.append(constructImageDescriptionText(context, imageDescriptionResult));
    }
    if (!Result.isEmpty(iconLabelResult)) {
      stringBuilder.append(context.getString(R.string.detected_icon_label, iconLabelResult.text()));
    }
    if (!Result.isEmpty(ocrTextResult)) {
      stringBuilder.append(
          context.getString(R.string.detected_recognized_text, ocrTextResult.text()));
    }
    return TextUtils.isEmpty(stringBuilder)
        ? context.getString(R.string.image_caption_no_result)
        : context.getString(R.string.detected_result, stringBuilder);
  }

  /**
   * Constructs the result text for auto triggered image caption. Returns empty string if all of the
   * arguments are empty or null.
   */
  public static String constructCaptionTextForAuto(
      Context context,
      @Nullable Result imageDescriptionResult,
      @Nullable Result iconLabelResult,
      @Nullable Result ocrTextResult) {
    StringBuilder stringBuilder = new StringBuilder();
    // Order: Icon -> Image -> Text (OCR).
    if (!Result.isEmpty(iconLabelResult)) {
      stringBuilder.append(context.getString(R.string.detected_icon_label, iconLabelResult.text()));
    }
    if (!Result.isEmpty(imageDescriptionResult)) {
      stringBuilder.append(constructImageDescriptionText(context, imageDescriptionResult));
    }
    if (!Result.isEmpty(ocrTextResult)) {
      stringBuilder.append(
          context.getString(R.string.detected_recognized_text, ocrTextResult.text()));
    }
    return TextUtils.isEmpty(stringBuilder)
        ? ""
        : context.getString(R.string.detected_result, stringBuilder);
  }

  private static String constructImageDescriptionText(
      Context context, Result imageDescriptionResult) {
    String resultText =
        context.getString(R.string.detected_image_description, imageDescriptionResult.text());
    if (imageDescriptionResult.confidence()
        >= FeatureFlagReader.getImageDescriptionHighQualityThreshold(context)) {
      return resultText;
    } else if (imageDescriptionResult.confidence()
        >= FeatureFlagReader.getImageDescriptionLowQualityThreshold(context)) {
      return context.getString(R.string.medium_confidence_image_description, resultText);
    } else {
      return context.getString(R.string.low_confidence_image_description, resultText);
    }
  }

  /**
   * Checks if the node needs automatic image captioning.
   *
   * <p>The nodes without text and content description need image captioning.
   */
  @VisibleForTesting
  public static boolean needImageCaption(
      Context context, @Nullable AccessibilityNodeInfoCompat node) {
    return needImageCaption(
        context,
        node,
        /* enableAutomaticImageCaptionForAllImages= */ false,
        /* needSizeCheck= */ false);
  }

  /**
   * Checks if the node needs automatic image captioning.
   *
   * <p>All image nodes need image captioning if {@code enableAutomaticImageCaptionForAllImages} is
   * true.
   *
   * <p>For non-image view, only the nodes without text and content description need image
   * captioning.
   */
  public static boolean needImageCaption(
      Context context,
      @Nullable AccessibilityNodeInfoCompat node,
      boolean enableAutomaticImageCaptionForAllImages,
      boolean needSizeCheck) {
    return needImageCaptionForUnlabelledView(context, node)
        || (enableAutomaticImageCaptionForAllImages
            && isCaptionable(context, node)
            && (!needSizeCheck || isLargeImage(context, node)));
  }

  /**
   * Checks if the node which has no text and context description needs automatic image captioning.
   */
  public static boolean needImageCaptionForUnlabelledView(
      Context context, @Nullable AccessibilityNodeInfoCompat node) {
    if (!isCaptionable(context, node)) {
      return false;
    }

    @RoleName int role = Role.getRole(node);
    // Do not perform image captions for the view which has slider percent or state description to
    // prevent the additional feedback, like the sign “-” for seekbars and the letter “O” for
    // radio buttons.
    if (role == Role.ROLE_SEEK_CONTROL
        || role == Role.ROLE_PROGRESS_BAR
        || !TextUtils.isEmpty(node.getStateDescription())
        || !TextUtils.isEmpty(node.getHintText())) {
      return false;
    }

    return TextUtils.isEmpty(AccessibilityNodeInfoUtils.getNodeText(node));
  }

  /**
   * Checks if the node can be captioned.
   *
   * <p>Both images and small leaf views can be captioned.
   */
  public static boolean isCaptionable(Context context, @Nullable AccessibilityNodeInfoCompat node) {
    if (node == null || !FeatureSupport.canTakeScreenShotByAccessibilityService()) {
      return false;
    }

    @RoleName int role = Role.getRole(node);
    // Checks if a view in WebView is an image by the AccessibilityNodeInfo.hasImage attribute
    // instead of a11y class name.
    if (isImageOrImageButton(role) || WebInterfaceUtils.containsImage(node)) {
      return true;
    } else if (ENABLE_CAPTION_FOR_LEAF_VIEW && (node.getChildCount() == 0)) {
      return isSmallSizeNode(context, node);
    }

    return false;
  }

  /**
   * Returns the preferred image caption module for the given node.
   *
   * @return returns icon-detection module is the node size is small. Otherwise, returns
   *     image-description module.
   */
  public static CaptionType getPreferredModuleOnNode(
      Context context, AccessibilityNodeInfoCompat node) {
    return isSmallSizeNode(context, node) ? ICON_LABEL : IMAGE_DESCRIPTION;
  }

  private static boolean isImageOrImageButton(@RoleName int role) {
    return role == Role.ROLE_IMAGE || role == Role.ROLE_IMAGE_BUTTON;
  }

  /**
   * Checks whether the image needs image description by determining if the node meets all the
   * following criteria:
   *
   * <ul>
   *   <li>It is not a small image which means (i.e. its height or width is larger than {@link
   *       #MIN_IMAGE_SIZE_FOR_AUTOMATIC_IMAGE_CAPTIONING_IN_DP}).
   *   <li>It is not a button on the navigation bar.
   * </ul>
   *
   * <p>OCR and icon detection don't follow the rules.
   */
  private static boolean isLargeImage(Context context, @Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return false;
    }

    // It's unnecessary to check the size of non-image views.
    CharSequence className = node.getClassName();
    if (className == null
        // Navigation bar shouldn't been performed auto-captioning.
        || WindowUtils.isNavigationBar(context, node.unwrap().getWindow())
        // IMAGE_BUTTON shouldn't been performed auto-captioning but it's difficult to distinguish
        // between ImageView and ImageButton by the Role.java, because in TalkBack ImageButton just
        // means the image is clickable. So instead of using Role.java, here the code checks the
        // class name directly.
        || ((!ClassLoadingCache.checkInstanceOf(className, ImageView.class)
                || ClassLoadingCache.checkInstanceOf(className, ImageButton.class))
            // "android.widget.Image" plays as an image which is applicable for image description.
            && !className.toString().equals("android.widget.Image")
            // Checks if a view in WebView is an image by the AccessibilityNodeInfo.hasImage
            // attribute instead of a11y class name.
            && !WebInterfaceUtils.containsImage(node))) {

      return false;
    }

    Rect rect = new Rect();
    node.getBoundsInScreen(rect);
    return !rect.isEmpty()
        && max(rect.height(), rect.width())
            > context.getResources().getDisplayMetrics().density
                * MIN_IMAGE_SIZE_FOR_AUTOMATIC_IMAGE_CAPTIONING_IN_DP;
  }

  /** Returns {@code true} if the size of the given node is small. */
  private static boolean isSmallSizeNode(Context context, AccessibilityNodeInfoCompat node) {
    Rect rect = new Rect();
    node.getBoundsInScreen(rect);
    return !rect.isEmpty()
        // Refer b/111422024: For TextView or some custom views which may contain unlabelled icon.
        // Here we allow the image/icon description only for view node which does not have
        // text/content description.
        && TextUtils.isEmpty(AccessibilityNodeInfoUtils.getNodeText(node))
        && rect.height()
            <= context.getResources().getDisplayMetrics().density
                * MAX_CAPTION_ABLE_LEAF_VIEW_HEIGHT_IN_DP;
  }

  private ImageCaptionUtils() {}
}
