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
package com.google.android.accessibility.talkback.compositor;

import static android.view.accessibility.AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_NONE;
import static com.google.android.accessibility.talkback.Constants.CLANK_PACKAGE_NAME;
import static com.google.android.accessibility.talkback.imagecaption.ImageCaptionUtils.constructCaptionTextForAuto;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.LocaleSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.Compositor.HandleEventOptions;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.imagecaption.ImageContents;
import com.google.android.accessibility.talkback.imagecaption.Result;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.LocaleUtils;
import com.google.android.accessibility.utils.PackageManagerUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.accessibility.utils.output.SpeechCleanupUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Utils class that provides common methods that provide accessibility information by {@link
 * AccessibilityNodeInfoCompat} for compositor event feedback output.
 */
public class AccessibilityNodeFeedbackUtils {
  private static final String TAG = "AccessibilityNodeFeedbackUtils";

  private AccessibilityNodeFeedbackUtils() {}

  /** Returns the node text. */
  public static CharSequence getNodeText(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    CharSequence text =
        prepareSpans(
            AccessibilityNodeInfoUtils.getText(node),
            node,
            context,
            globalVariables.getPreferredLocaleByNode(node),
            globalVariables.getCountRepeatedSymbols());
    // Only node text needs to handle emoji, other text like content description, supplemental
    // description, etc. don't need to handle emoji.
    CharSequence nodeText = CompositorUtils.enhanceEmojiFeedback(context, text);
    return SpannableUtils.wrapWithSourceTextSpan(nodeText);
  }

  /**
   * Returns the node text for description.
   *
   * <p>Note: It returns the node content and supplemental description if it is not empty. Or it
   * fallbacks to return node text.
   */
  public static CharSequence getNodeTextDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    CharSequence contentAndSupplementalDescription =
        getNodeContentAndSupplementalDescription(node, context, globalVariables);
    if (!TextUtils.isEmpty(contentAndSupplementalDescription)) {
      return globalVariables.getGlobalSayCapital()
          ? CompositorUtils.prependCapital(contentAndSupplementalDescription, context)
          : contentAndSupplementalDescription;
    }
    // Fallbacks to node text.
    CharSequence nodeText = getNodeText(node, context, globalVariables);
    return globalVariables.getGlobalSayCapital()
        ? CompositorUtils.prependCapital(nodeText, context)
        : nodeText;
  }

  /**
   * Returns the node text description or label description.
   *
   * <p>Note: Talkback doesn't need to read out unlabelled or view element IDs for the node that has
   * extra information, like CheckBox, which has state description, or like OCR text, icon label
   * recognized from captured screenshot.
   */
  public static CharSequence getNodeTextOrLabelDescription(
      AccessibilityNodeInfoCompat node,
      Context context,
      ImageContents imageContents,
      GlobalVariables globalVariables) {
    CharSequence nodeTextDescription = getNodeTextDescription(node, context, globalVariables);
    if (!TextUtils.isEmpty(nodeTextDescription)) {
      return nodeTextDescription;
    }
    // Fallbacks to node label.
    CharSequence nodeLabelText = getNodeLabelText(node, imageContents);
    if (!TextUtils.isEmpty(nodeLabelText)) {
      return globalVariables.getGlobalSayCapital()
          ? CompositorUtils.prependCapital(nodeLabelText, context)
          : nodeLabelText;
    }
    // Fallbacks to caption text.
    CharSequence nodeCaptionText =
        getNodeCaptionText(node, context, imageContents, globalVariables);
    if (!TextUtils.isEmpty(nodeCaptionText)) {
      return nodeCaptionText;
    }
    return "";
  }

  /** Returns the node text description or label or view element ID description. */
  public static CharSequence getNodeTextOrLabelOrIdDescription(
      AccessibilityNodeInfoCompat node,
      Context context,
      ImageContents imageContents,
      GlobalVariables globalVariables) {
    CharSequence nodeTextOrLabel =
        getNodeTextOrLabelDescription(node, context, imageContents, globalVariables);
    if (!TextUtils.isEmpty(nodeTextOrLabel)) {
      return nodeTextOrLabel;
    }
    // Fallbacks to element IDs.
    return globalVariables.getSpeakElementIds()
        ? AccessibilityNodeInfoUtils.getViewIdText(node)
        : "";
  }

  /** Returns the node content description. */
  public static CharSequence getNodeContentDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    return prepareSpans(
        node.getContentDescription(),
        node,
        context,
        globalVariables.getPreferredLocaleByNode(node),
        globalVariables.getCountRepeatedSymbols());
  }

  /** Returns the node supplemental description. */
  public static CharSequence getNodeSupplementalDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    return prepareSpans(
        /* text= */ "",
        node,
        context,
        globalVariables.getPreferredLocaleByNode(node),
        globalVariables.getCountRepeatedSymbols());
  }

  /** Returns the node content and supplemental description. */
  public static CharSequence getNodeContentAndSupplementalDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    CharSequence contentDescription = node.getContentDescription();
    CharSequence supplementalDescription = "";
    // De-duplicated content and supplemental description.
    CharSequence text =
        TextUtils.equals(contentDescription, supplementalDescription)
            ? contentDescription
            : CompositorUtils.joinCharSequences(contentDescription, supplementalDescription);
    return prepareSpans(
        text,
        node,
        context,
        globalVariables.getPreferredLocaleByNode(node),
        globalVariables.getCountRepeatedSymbols());
  }

  /**
   * Returns the node hint.
   *
   * <p>Note: The content should be non-copyable text for "copy last spoken phrase"
   */
  public static CharSequence getNodeHint(AccessibilityNodeInfoCompat node) {
    return SpannableUtils.wrapWithNonCopyableTextSpan(AccessibilityNodeInfoUtils.getHintText(node));
  }

  /**
   * Returns the node state description.
   *
   * <p>Note: The content should be non-copyable text for "copy last spoken phrase"
   */
  public static CharSequence getNodeStateDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    @Nullable CharSequence state = AccessibilityNodeInfoUtils.getState(node);
    if (node != null && node.isFieldRequired()) {
      state =
          TextUtils.isEmpty(state)
              ? context.getString(R.string.required_state)
              : context.getString(R.string.required_state_appended, state);
    }
    return SpannableUtils.wrapWithNonCopyableTextSpan(
        prepareSpans(
            state,
            node,
            context,
            globalVariables.getPreferredLocaleByNode(node),
            globalVariables.getCountRepeatedSymbols()));
  }

  /**
   * Returns the default node role description text.
   *
   * <p>Note:
   * <li>The content should be non-copyable text for "copy last spoken phrase".
   * <li>Returns EditText role description even if TalkBack doesn't speak roles.
   */
  public static CharSequence defaultRoleDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    if (!globalVariables.getSpeakRoles() && Role.getRole(node) != Role.ROLE_EDIT_TEXT) {
      return "";
    }
    CharSequence nodeRoleDescription = getNodeRoleDescription(node, context, globalVariables);
    if (!TextUtils.isEmpty(nodeRoleDescription)) {
      return nodeRoleDescription;
    }
    return getNodeRoleName(node, context);
  }

  /**
   * Returns the node role description, which should respect the AppLocale.
   *
   * <p>Note: The content should be non-copyable text for "copy last spoken phrase"
   */
  public static CharSequence getNodeRoleDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    return SpannableUtils.wrapWithNonCopyableTextSpan(
        prepareSpans(
            node.getRoleDescription(),
            node,
            context,
            globalVariables.getPreferredLocaleByNode(node),
            globalVariables.getCountRepeatedSymbols()));
  }

  /**
   * Returns the node role name.
   *
   * <p>Note: The content should be non-copyable text for "copy last spoken phrase"
   */
  public static CharSequence getNodeRoleName(AccessibilityNodeInfoCompat node, Context context) {
    int role = Role.getRole(node);
    CharSequence roleName = "";
    switch (role) {
      case Role.ROLE_BUTTON,
          Role.ROLE_IMAGE_BUTTON,
          Role.ROLE_FLOATING_ACTION_BUTTON,
          Role.ROLE_VOICE_DICTATION_BUTTON ->
          roleName = context.getString(R.string.value_button);
      case Role.ROLE_CHECK_BOX -> roleName = context.getString(R.string.value_checkbox);
      case Role.ROLE_DROP_DOWN_LIST -> roleName = context.getString(R.string.value_spinner);
      case Role.ROLE_EDIT_TEXT -> roleName = context.getString(R.string.value_edit_box);
      case Role.ROLE_GRID -> roleName = context.getString(R.string.value_gridview);
      case Role.ROLE_IMAGE -> roleName = context.getString(R.string.value_image);
      case Role.ROLE_LIST -> roleName = context.getString(R.string.value_listview);
      case Role.ROLE_PAGER -> roleName = context.getString(R.string.value_pager);
      case Role.ROLE_PROGRESS_BAR -> roleName = context.getString(R.string.value_progress_bar);
      case Role.ROLE_RADIO_BUTTON, Role.ROLE_CHECKED_TEXT_VIEW ->
          roleName = context.getString(R.string.value_radio_button);
      case Role.ROLE_SEEK_CONTROL -> roleName = context.getString(R.string.value_seek_bar);
      case Role.ROLE_SWITCH, Role.ROLE_TOGGLE_BUTTON ->
          roleName = context.getString(R.string.value_switch);
      case Role.ROLE_TAB_BAR -> roleName = context.getString(R.string.value_tabwidget);
      case Role.ROLE_WEB_VIEW -> roleName = context.getString(R.string.value_webview);
      default -> {
        // ROLE_VIEW_GROUP or else will return an empty Role name.
        return "";
      }
    }

    return SpannableUtils.wrapWithNonCopyableTextSpan(roleName);
  }

  /**
   * Returns {@code R.string.value_unlabelled} if the node is unlabeled and needs a label, otherwise
   * returns an empty text.
   */
  public static CharSequence getUnlabelledNodeDescription(
      int role,
      AccessibilityNodeInfoCompat node,
      Context context,
      @Nullable ImageContents imageContents,
      GlobalVariables globalVariables) {
    boolean needsLabel = imageContents != null && imageContents.needsLabel(node);
    boolean srcIsCheckable = node.isCheckable();
    LogUtils.v(
        TAG,
        StringBuilderUtils.joinFields(
            " getUnlabelledNodeDescription, ",
            StringBuilderUtils.optionalTag("needsLabel", needsLabel),
            StringBuilderUtils.optionalTag("srcIsCheckable", srcIsCheckable),
            StringBuilderUtils.optionalText("role", Role.roleToString(role))));
    if (!needsLabel
        || srcIsCheckable
        || (role == Role.ROLE_SEEK_CONTROL || role == Role.ROLE_PROGRESS_BAR)) {
      return "";
    }

    CharSequence nodeDescription = defaultRoleDescription(node, context, globalVariables);
    CharSequence nodeStateDescription = getNodeStateDescription(node, context, globalVariables);
    CharSequence nodeTextOrLabelId =
        getNodeTextOrLabelOrIdDescription(node, context, imageContents, globalVariables);
    // To accommodate apps (like Chrome) which don't set the text, hint text and isShowingHintText
    // properly, use getNodeHint instead of getHintDescription below.
    CharSequence nodeHintText = getNodeHint(node);
    CharSequence nodeDescriptionFromLabelNode =
        getDescriptionFromLabelNode(node, context, imageContents, globalVariables);
    if (TextUtils.isEmpty(nodeStateDescription)
        && TextUtils.isEmpty(nodeTextOrLabelId)
        && TextUtils.isEmpty(nodeHintText)
        && TextUtils.isEmpty(nodeDescriptionFromLabelNode)) {
      LogUtils.v(
          TAG, " getUnlabelledNodeDescription return Unlabelled/nodeRole because no text info");
      return TextUtils.isEmpty(nodeDescription)
          ? context.getString(R.string.value_unlabelled)
          : nodeDescription;
    }
    return "";
  }

  /**
   * Returns the node page role description.
   *
   * <p>Note: The content should be non-copyable text for "copy last spoken phrase"
   *
   * <p>TODO : move this method to PagerPageDescription after ParseTree design
   * obsoleted
   */
  public static CharSequence getPagerPageRoleDescription(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    if (globalVariables.getSpeakRoles()) {
      CharSequence roleDescription = getNodeRoleDescription(node, context, globalVariables);
      if (!TextUtils.isEmpty(roleDescription)) {
        return roleDescription;
      } else {
        return SpannableUtils.wrapWithNonCopyableTextSpan(
            context.getString(R.string.value_pager_page));
      }
    }
    return "";
  }

  /** Returns the node caption text with auto triggered approach. */
  public static CharSequence getNodeCaptionText(
      AccessibilityNodeInfoCompat node,
      Context context,
      @Nullable ImageContents imageContents,
      GlobalVariables globalVariables) {

    if (imageContents == null) {
      return "";
    }

    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    @Nullable
    Result ocrText =
        SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_auto_text_recognition_key,
                R.bool.pref_auto_text_recognition_default)
            ? imageContents.getCaptionResult(node)
            : null;
    Locale preferredLocale = globalVariables.getPreferredLocaleByNode(node);
    @Nullable
    Result iconLabel =
        SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_auto_icon_detection_key,
                R.bool.pref_auto_icon_detection_default)
            ? imageContents.getDetectedIconLabel(
                preferredLocale == null ? Locale.getDefault() : preferredLocale, node)
            : null;
    @Nullable
    Result imageDescription =
        SharedPreferencesUtils.getBooleanPref(
                prefs,
                context.getResources(),
                R.string.pref_auto_image_description_key,
                R.bool.pref_auto_image_description_default)
            ? imageContents.getImageDescriptionResult(node)
            : null;

    return constructCaptionTextForAuto(context, imageDescription, iconLabel, ocrText);
  }

  /** Returns the node label text. */
  public static CharSequence getNodeLabelText(
      AccessibilityNodeInfoCompat node, @Nullable ImageContents imageContents) {
    return imageContents == null ? "" : imageContents.getLabel(node);
  }

  /** Returns the node description text from the label node. */
  public static CharSequence getDescriptionFromLabelNode(
      AccessibilityNodeInfoCompat node,
      Context context,
      ImageContents imageContents,
      GlobalVariables globalVariables) {

    List<AccessibilityNodeInfoCompat> labelNodes = node.getLabeledByList();
    if (labelNodes.isEmpty()) {
      return "";
    }

    List<CharSequence> labelTexts = new ArrayList<>();
    // Use a Set to efficiently track unique labels and avoid duplicates.
    Set<String> uniqueLabels = new HashSet<>();
    for (AccessibilityNodeInfoCompat labelNode : labelNodes) {
      // ANI in ANICompat in a list returned by AccessibilityNodeInfoCompat#getLabeledByList() could
      // be null, so make sure that it's not null; see b/452129281 for more details.
      if (labelNode == null || labelNode.unwrap() == null) {
        continue;
      }
      CharSequence labelText =
          getNodeTextOrLabelOrIdDescription(labelNode, context, imageContents, globalVariables);
      // Add the labelText to labelTexts only if it's not empty and is a new unique label.
      if (!TextUtils.isEmpty(labelText) && uniqueLabels.add(labelText.toString())) {
        labelTexts.add(labelText);
      }
    }
    return CompositorUtils.joinCharSequences(
        labelTexts, CompositorUtils.getSeparator(), CompositorUtils.PRUNE_EMPTY);
  }

  /** Returns the node disabled state if the node should announce the disabled state. */
  public static CharSequence getDisabledStateText(
      AccessibilityNodeInfoCompat node, Context context) {
    return (node != null && announceDisabled(node))
        ? context.getString(R.string.value_disabled)
        : "";
  }

  /** Returns the node read only state. */
  public static CharSequence getReadOnlyStateText(
      AccessibilityNodeInfoCompat node, Context context) {
    return (node != null
            && Role.getRole(node) == Role.ROLE_EDIT_TEXT
            && node.isEnabled()
            && !node.isEditable())
        ? context.getString(R.string.value_read_only)
        : "";
  }

  /** Returns if the node should announce disabled state. */
  private static boolean announceDisabled(AccessibilityNodeInfoCompat node) {
    // In some situations Views marked as headings (see ViewCompat#setAccessibilityHeading)
    // are in the disabled state, even though being disabled is not very appropriate. An
    // example are TextViews styled as preferenceCategoryStyle in certain themes.
    if (node.isHeading()) {
      return false;
    }
    if (BuildVersionUtils.isAtLeastS()) {
      return !node.isEnabled();
    }
    return !node.isEnabled()
        && (WebInterfaceUtils.hasNativeWebContent(node)
            || AccessibilityNodeInfoUtils.isActionableForAccessibility(node));
  }

  /** Returns the node selected state. */
  public static CharSequence getSelectedStateText(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    // If the collection selection mode is not set, do not announce anything if the node is not
    // selected. Otherwise, provide the spoken feedback for the unselected state.
    CharSequence unselectedStateText =
        globalVariables.getCollectionSelectionMode() == SELECTION_MODE_NONE
                || !(FeatureFlagReader.enableAnnounceNotSelected(context)
                    && TextUtils.equals(node.getPackageName(), CLANK_PACKAGE_NAME))
            ? ""
            : context.getString(R.string.value_not_selected);
    return (node != null && node.isSelected())
        ? context.getString(R.string.value_selected)
        : unselectedStateText;
  }

  /** Returns the node collapsed or expanded state. */
  public static CharSequence getCollapsedOrExpandedStateText(
      AccessibilityNodeInfoCompat node, Context context) {
    if (AccessibilityNodeInfoUtils.isExpandable(node)) {
      return context.getString(R.string.value_collapsed);
    } else if (AccessibilityNodeInfoUtils.isCollapsible(node)) {
      return context.getString(R.string.value_expanded);
    }
    return "";
  }

  public static CharSequence getCheckedStateText(
      AccessibilityNodeInfoCompat node, Context context) {

    return node.isChecked()
        ? context.getString(R.string.value_checked)
        : context.getString(R.string.value_not_checked);
  }

  /**
   * Returns the unique node tooltip.
   *
   * <p>Note: if tooltip is the same as node text, it will return empty string to prevent duplicate
   * content.
   *
   * <p>TODO : move this method to NodeRoleDescription after ParseTree design obsoleted
   */
  public static CharSequence getUniqueTooltipText(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    if (node == null) {
      return "";
    }
    CharSequence tooltipText = node.getTooltipText();
    if (!TextUtils.isEmpty(tooltipText)
        && !TextUtils.equals(tooltipText, getNodeTextDescription(node, context, globalVariables))) {
      return tooltipText;
    }
    return "";
  }

  /**
   * Returns the node hint for node tree description.
   *
   * <p>Note: Description should append the node hint if it is not already showing the hint as its
   * text or the edit text is blank
   *
   * <p>TODO : move this method to NodeRoleDescription after ParseTree design obsoleted
   */
  public static CharSequence getHintDescription(AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return "";
    }
    return (Role.getRole(node) == Role.ROLE_EDIT_TEXT && !TextUtils.isEmpty(node.getText()))
            || node.isShowingHintText()
        ? ""
        : getNodeHint(node);
  }

  /**
   * Returns hint text for node actions that is in high verbosity.
   *
   * <p>Note: There are four cases of hint for the "Actions" item if it is available in the TalkBack
   * menu,
   *
   * <ul>
   *   <li>1. Custom-Action gesture is assigned. e.g. "Tap with 2 fingers" is configured to show
   *       custom action -> "Actions available, use tap with 2 fingers to view"
   *   <li>2. "Actions" item for EditText. The current setting may be reset to Character or
   *       Proofread later, so always use the menu shortcut as the hint of "Actions" item. e.g. "Tap
   *       with 3 fingers" is configured to open TalkBack menu -> "Actions available, use tap with 3
   *       fingers to view"
   *   <li>3. "Actions" item is available in the reading control and the current settings is
   *       "Actions". e.g. "Swipe up" and "Swipe down" are configured to select reading control ->
   *       "Actions available, swipe up or swipe down and double-tap to activate"
   *   <li>4. Otherwise, e.g. "Tap with 3 fingers" is configured to open TalkBack menu -> "Actions
   *       available, use tap with 3 fingers to view"
   * </ul>
   */
  public static CharSequence getHintForNodeActions(
      AccessibilityNodeInfoCompat node, Context context, GlobalVariables globalVariables) {
    int role = Role.getRole(node);
    NodeMenuProvider nodeMenuProvider = globalVariables.getNodeMenuProvider();
    if (nodeMenuProvider != null && role != Role.ROLE_TEXT_ENTRY_KEY) {
      List<String> menuTypeList = nodeMenuProvider.getSelfNodeMenuActionTypes(node);
      String actionMenuItemName = nodeMenuProvider.getActionMenuName();
      StringBuilder hint = new StringBuilder();
      if (!TextUtils.isEmpty(actionMenuItemName) && menuTypeList.contains(actionMenuItemName)) {
        CharSequence hintArgument = globalVariables.getGestureStringForActionShortcut();
        if (!TextUtils.isEmpty(hintArgument)) {
          // Custom-Action is a configured gesture shortcut.
          hint.append(
                  context.getString(
                      R.string.template_hint_menu_type_high_verbosity,
                      actionMenuItemName,
                      hintArgument))
              .append(CompositorUtils.getSeparator());
          // Actions item hint exists, so it's unnecessary to generate another hint for it.
          menuTypeList.remove(actionMenuItemName);
        } else if (globalVariables.isReadingMenuActionCurrentSettings(context)
            && globalVariables.hasReadingMenuActionSettings()
            // The current settings is not fixed and maybe be reset to default action (Character or
            // Proofread) later, so always providing menu shortcut for the hint of "Actions" item.
            && role != Role.ROLE_EDIT_TEXT) {
          hintArgument = globalVariables.getGestureStringForNodeActionsInReadingControl();
          if (!TextUtils.isEmpty(hintArgument)) {
            String actionDescription = globalVariables.getReadingMenuActionSettingsDescription();
            if (!TextUtils.isEmpty(actionDescription)) {
              hint.append(
                      context.getString(
                          R.string.template_hint_for_reading_control_actions_in_high_verbosity,
                          actionDescription,
                          hintArgument))
                  .append(CompositorUtils.getSeparator());
              // Actions item hint exists, so it's unnecessary to generate another hint for it.
              menuTypeList.remove(actionMenuItemName);
            }
          }
        }
      }

      // Gets a hint for node actions in the TalkBack menu.
      if (!menuTypeList.isEmpty()) {
        hint.append(
            context.getString(
                R.string.template_hint_menu_type_high_verbosity,
                Joiner.on(CompositorUtils.getSeparator()).join(menuTypeList),
                globalVariables.getGestureStringForNodeActions()));
      }
      return hint;
    }
    return "";
  }

  /**
   * Returns the accessibility node enabled state text.
   *
   * <p>TODO : move this method to NodeRoleDescription after ParseTree design obsoleted
   */
  public static CharSequence getAccessibilityEnabledState(
      AccessibilityNodeInfoCompat node, Context context) {
    if (node != null && AccessibilityNodeInfoUtils.isSelfOrAncestorFocused(node)) {
      return node.isEnabled()
          ? context.getString(R.string.value_enabled)
          : context.getString(R.string.value_disabled);
    }
    return "";
  }

  /**
   * Returns the accessibility node error text for description.
   *
   * <p>TODO : move this method to NodeRoleDescription after ParseTree design obsoleted
   */
  public static CharSequence getAccessibilityNodeErrorText(
      AccessibilityNodeInfoCompat node, Context context) {
    if (node != null && node.isContentInvalid()) {
      CharSequence errorText = node.getError();
      return TextUtils.isEmpty(errorText)
          ? ""
          : context.getString(R.string.template_node_error_with_error_message, errorText);
    }
    return "";
  }

  /** Returns the node error state text. */
  public static CharSequence notifyErrorStateText(
      @Nullable AccessibilityNodeInfoCompat node, Context context) {
    return (node == null || TextUtils.isEmpty(node.getError()))
        ? ""
        : context.getString(R.string.template_text_error, node.getError());
  }

  /** Represents a error message with the specific node hash code. */
  @AutoValue
  public abstract static class ErrorInfo {
    public abstract int nodeHashCode();

    public abstract CharSequence errorMessage();

    public static ErrorInfo create(int nodeHashCode, CharSequence errorMessage) {
      return new AutoValue_AccessibilityNodeFeedbackUtils_ErrorInfo(nodeHashCode, errorMessage);
    }
  }

  /**
   * Returns the error text if the last announced error message is different from the current one or
   * the error message is from a different node. Updates the provided lastAnnouncedErrorInfoRef.
   *
   * @param eventOptions The event options.
   * @param context The context.
   * @param lastAnnouncedErrorInfoRef An AtomicReference holding the ErrorInfo for the specific
   *     context (e.g., password or general input).
   * @return CharSequence The error text to be announced, or an empty string if no new error.
   */
  public static CharSequence updateAndGetErrorStateText(
      HandleEventOptions eventOptions,
      Context context,
      AtomicReference<ErrorInfo> lastAnnouncedErrorInfoRef) {

    AccessibilityNodeInfoCompat node = eventOptions.sourceNode;
    if (node == null || TextUtils.isEmpty(node.getError())) {
      lastAnnouncedErrorInfoRef.set(null);
      return "";
    }

    ErrorInfo currentAnnouncedError = lastAnnouncedErrorInfoRef.get();
    if (currentAnnouncedError != null
        && node.hashCode() == currentAnnouncedError.nodeHashCode()
        && TextUtils.equals(node.getError(), currentAnnouncedError.errorMessage())) {
      return "";
    }

    lastAnnouncedErrorInfoRef.set(ErrorInfo.create(node.hashCode(), node.getError()));
    return notifyErrorStateText(node, context);
  }

  /** Returns the max length reached state text. */
  public static CharSequence notifyMaxLengthReachedStateText(
      @Nullable AccessibilityNodeInfoCompat node, Context context) {
    if (node == null || TextUtils.isEmpty(node.getText())) {
      return "";
    }
    // Uses node.text to get correct text length because event.text would have a symbol character
    // transferred to a spoken descripgetNodeStateDescriptiontion.
    int maxTextLength = node.getMaxTextLength();
    int nodeTextLength = node.getText().length();
    return (maxTextLength > -1 && nodeTextLength >= maxTextLength)
        ? context.getString(R.string.value_text_max_length)
        : "";
  }

  @NonNull
  private static CharSequence prepareSpans(
      @Nullable CharSequence text,
      AccessibilityNodeInfoCompat node,
      Context context,
      Locale userPreferredLocale,
      boolean countRepeatedSymbols) {
    // Cleans up the edit text's text if it has just 1 symbol.
    // Do not double clean up the password.
    if (!node.isPassword()) {
      text =
          SpeechCleanupUtils.collapseRepeatedCharactersAndCleanUp(
              context, text, countRepeatedSymbols);
    }

    // Wrap the text with user preferred locale changed using language switcher, with an exception
    // for all talkback nodes. As talkback text is always in the system language.

    if (PackageManagerUtils.isTalkBackPackage(node.getPackageName())) {
      return text == null ? "" : text;
    }

    // UserPreferredLocale will take precedence over any LocaleSpan that is attached to the
    // text except in case of IMEs.
    if (!AccessibilityNodeInfoUtils.isKeyboard(node) && userPreferredLocale != null) {
      if (text instanceof Spannable) {
        Spannable ss = (Spannable) text;
        LocaleSpan[] spans = ss.getSpans(0, text.length(), LocaleSpan.class);
        for (LocaleSpan span : spans) {
          ss.removeSpan(span);
        }
      }
      return text == null ? "" : LocaleUtils.wrapWithLocaleSpan(text, userPreferredLocale);
    }
    return text == null ? "" : text;
  }
}
