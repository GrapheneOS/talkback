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
package com.google.android.accessibility.talkback.compositor.roledescription;

import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
import static android.view.accessibility.AccessibilityNodeInfo.CollectionInfo.SELECTION_MODE_NONE;
import static androidx.core.view.ViewCompat.ACCESSIBILITY_LIVE_REGION_NONE;
import static com.google.android.accessibility.talkback.compositor.CompositorUtils.PRUNE_EMPTY;
import static com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DESC_ORDER_NAME_ROLE_STATE_POSITION;
import static com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DESC_ORDER_ROLE_NAME_STATE_POSITION;
import static com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor.DESC_ORDER_STATE_NAME_ROLE_POSITION;

import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.AccessibilityNodeFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.CompositorUtils;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.imagecaption.ImageContents;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.accessibility.utils.traversal.ReorderedChildrenIterator;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides tree nodes description.
 *
 * <p>Note: the tree nodes description, node tree description, would contain the description of the
 * descendant nodes of the event source node.
 *
 * <p>Note: if the source node has no content description, it should append the nodes description of
 * the child nodes.
 *
 * <p>Note: When the scroll item has content changed and the source node is accessibility live
 * region, it should append the nodes description of the child nodes.
 */
public class TreeNodesDescription {

  private static final String TAG = "TreeNodesDescription";

  private final Context context;
  private final ImageContents imageContents;
  private final GlobalVariables globalVariables;
  private final RoleDescriptionExtractor roleDescriptionExtractor;

  public TreeNodesDescription(
      Context context,
      ImageContents imageContents,
      GlobalVariables globalVariables,
      RoleDescriptionExtractor roleDescriptionExtractor) {
    this.context = context;
    this.imageContents = imageContents;
    this.globalVariables = globalVariables;
    this.roleDescriptionExtractor = roleDescriptionExtractor;
  }

  /**
   * Returns the aggregate node tree description text that has the node tree status and the
   * description information. By default, we iterate through the given node's children to aggregate
   * the subtree's description into one CharSequence.
   *
   * @param node the node for the description
   * @param event the event for the description
   */
  public CharSequence aggregateNodeTreeDescription(
      AccessibilityNodeInfoCompat node, AccessibilityEvent event) {
    return aggregateNodeTreeDescription(node, event, /*shouldIterateChildren*/ true);
  }

  /**
   * Returns the aggregate node tree description text that has the node tree status and the
   * description information. Callers may determine whether to recursively iterate through the given
   * node's children to aggregate the subtree's description into one CharSequence.
   *
   * <p>NOTE: callers dealing with Live Regions on Chrome should provide false for
   * shouldIterateChildren, to avoid double announcement.
   *
   * @param node the node for description
   * @param event the event for description
   * @param boolean whether we should iterate through node's children to form the description
   */
  public CharSequence aggregateNodeTreeDescription(
      AccessibilityNodeInfoCompat node, AccessibilityEvent event, boolean shouldIterateChildren) {
    if (node == null) {
      LogUtils.w(TAG, "aggregateNodeTreeDescription: node is null");
      return "";
    }
    int descriptionOrder = globalVariables.getDescriptionOrder();
    CharSequence treeDescription = treeDescriptionWithLabel(node, event, shouldIterateChildren);
    CharSequence disabledState = AccessibilityNodeFeedbackUtils.getDisabledStateText(node, context);
    CharSequence readOnlyState = AccessibilityNodeFeedbackUtils.getReadOnlyStateText(node, context);
    CharSequence disabledStateOrReadOnlyState =
        !TextUtils.isEmpty(disabledState) ? disabledState : readOnlyState;
    CharSequence selectedState =
        AccessibilityNodeFeedbackUtils.getSelectedStateText(node, context, globalVariables);

    LogUtils.v(
        TAG,
        "aggregateNodeTreeDescription: %s",
        String.format(" (%s)", node.hashCode())
            + String.format(", treeDescriptionWithLabel={%s}", treeDescription)
            + String.format(", disabledStateOrReadOnlyState=%s", disabledStateOrReadOnlyState)
            + String.format(", selectedState=%s", selectedState)
            + String.format(", descriptionOrder=%s", descriptionOrder));

    // Disabled and read only state announcement should always be a postfix.
    return switch (descriptionOrder) {
      case DESC_ORDER_NAME_ROLE_STATE_POSITION, DESC_ORDER_ROLE_NAME_STATE_POSITION ->
          CompositorUtils.joinCharSequences(
              treeDescription, selectedState, disabledStateOrReadOnlyState);
      case DESC_ORDER_STATE_NAME_ROLE_POSITION ->
          CompositorUtils.joinCharSequences(
              selectedState, treeDescription, disabledStateOrReadOnlyState);
      default -> "";
    };
  }

  /**
   * Returns the node tree description appended label information if it has the label information.
   */
  private CharSequence treeDescriptionWithLabel(
      AccessibilityNodeInfoCompat node, AccessibilityEvent event, boolean shouldIterateChildren) {
    boolean shouldAppendChildNode = shouldAppendChildNode(context, event, shouldIterateChildren);
    CharSequence appendedTreeDescription =
        getAppendedTreeDescription(node, event, shouldIterateChildren, shouldAppendChildNode);
    CharSequence labelDescription =
        AccessibilityNodeFeedbackUtils.getDescriptionFromLabelNode(
            node, context, imageContents, globalVariables);

    LogUtils.v(
        TAG,
        "  treeDescriptionWithLabel: %s",
        new StringBuilder()
            .append(String.format(", appendedTreeDescription={%s}", appendedTreeDescription))
            .append(String.format(", labelDescription=%s", labelDescription))
            .append(String.format(", shouldAppendChildNode=%s", shouldAppendChildNode))
            .toString());

    return TextUtils.isEmpty(labelDescription)
        ? appendedTreeDescription
        : context.getString(
            R.string.template_labeled_item, appendedTreeDescription, labelDescription);
  }

  /**
   * Returns {@code true} if it should append the child node tree description by the event. Live
   * region changes coming from Chrome set shouldIterateChildren=false when calculating a node's
   * description. This avoids reading the entire subtree when receiving a change to just one node in
   * a live region. However, for live region changes coming from native Android applications, we
   * need to preserve this behavior for now. Therefore, shouldIterateChildren=true and
   * shouldAppendChildNode should return true.
   *
   * <p>Additionally, shouldIterateChildren cannot be overridden by any other tree properties, while
   * shouldAppendChildNode can be overridden by the node being unfocusable, which will cause it to
   * be added to the description regardless.
   */
  private static boolean shouldAppendChildNode(
      Context context, AccessibilityEvent event, boolean shouldIterateChildren) {
    if (!shouldIterateChildren) {
      return false;
    }
    AccessibilityNodeInfoCompat srcNode = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    boolean sourceIsLiveRegion =
        (srcNode != null) && (srcNode.getLiveRegion() != ACCESSIBILITY_LIVE_REGION_NONE);
    return (event.getEventType() == TYPE_WINDOW_CONTENT_CHANGED && sourceIsLiveRegion);
  }

  /**
   * Returns the appended tree description text that contains nodes description and status.
   *
   * <p>Note: the text is composed of tree nodes, tree status description in description order and
   * appends some accessibility information, error text, hint and tooltip.
   */
  private CharSequence getAppendedTreeDescription(
      AccessibilityNodeInfoCompat node,
      AccessibilityEvent event,
      boolean shouldIterateChildren,
      boolean shouldAppendChildNode) {
    int descriptionOrder = globalVariables.getDescriptionOrder();
    CharSequence treeDescription =
        switch (descriptionOrder) {
          case DESC_ORDER_NAME_ROLE_STATE_POSITION, DESC_ORDER_ROLE_NAME_STATE_POSITION ->
              CompositorUtils.conditionalAppend(
                  treeNodesDescription(node, event, shouldIterateChildren, shouldAppendChildNode),
                  nodeStatusDescription(node),
                  CompositorUtils.getSeparator());
          case DESC_ORDER_STATE_NAME_ROLE_POSITION ->
              CompositorUtils.conditionalPrepend(
                  nodeStatusDescription(node),
                  treeNodesDescription(node, event, shouldIterateChildren, shouldAppendChildNode),
                  CompositorUtils.getSeparator());
          default -> "";
        };

    CharSequence accessibilityNodeError =
        AccessibilityNodeFeedbackUtils.getAccessibilityNodeErrorText(node, context);
    CharSequence accessibilityNodeHint = AccessibilityNodeFeedbackUtils.getHintDescription(node);
    CharSequence tooltip =
        AccessibilityNodeFeedbackUtils.getUniqueTooltipText(node, context, globalVariables);

    LogUtils.v(
        TAG,
        StringBuilderUtils.joinFields(
            String.format("    getAppendedTreeDescription: (%s)  ", node.hashCode()),
            StringBuilderUtils.optionalText("accessibilityNodeError", accessibilityNodeError),
            String.format(", treeDescription={%s} ,", treeDescription),
            StringBuilderUtils.optionalText("accessibilityNodeHint", accessibilityNodeHint),
            StringBuilderUtils.optionalText("tooltip", tooltip)));

    return CompositorUtils.joinCharSequences(
        accessibilityNodeError, accessibilityNodeHint, treeDescription, tooltip);
  }

  /**
   * Returns the node status description text.
   *
   * <p>Note: the status description provides collapsed/expanded, and checked/unchecked state of the
   * node.
   */
  private CharSequence nodeStatusDescription(AccessibilityNodeInfoCompat node) {
    CharSequence collapsedOrExpandedState =
        AccessibilityNodeFeedbackUtils.getCollapsedOrExpandedStateText(node, context);
    boolean stateDescriptionIsEmpty =
        TextUtils.isEmpty(
            AccessibilityNodeFeedbackUtils.getNodeStateDescription(node, context, globalVariables));
    int role = Role.getRole(node);
    boolean srcIsCheckable = node.isCheckable();
    boolean srcIsChecked = node.isChecked();
    int collectionSelectionMode = globalVariables.getCollectionSelectionMode();

    LogUtils.v(
        TAG,
        "      nodeStatusDescription: %s",
        new StringBuilder()
            .append(String.format("role=%s", Role.roleToString(role)))
            .append(String.format(", collapsedOrExpandedState=%s", collapsedOrExpandedState))
            .append(String.format(", stateDescriptionIsEmpty=%s", stateDescriptionIsEmpty))
            .append(String.format(", srcIsCheckable=%b", srcIsCheckable))
            .append(String.format(", srcIsChecked=%b", srcIsChecked))
            .append(String.format(", collectionSelectionMode=%d", collectionSelectionMode))
            .toString());

    // Appends checked state for checkable view if it is in collection selection or checked.
    // If the node has set stateDescription, node checked state description in tree nodes
    // description text will be redundant for switch and toggle button to be always announced in
    // tree nodes description text.
    if ((stateDescriptionIsEmpty || node.isFieldRequired())
        && srcIsCheckable
        && (collectionSelectionMode != SELECTION_MODE_NONE || srcIsChecked)) {
      CharSequence checkedState = AccessibilityNodeFeedbackUtils.getCheckedStateText(node, context);
      return CompositorUtils.joinCharSequences(collapsedOrExpandedState, checkedState);
    }
    return collapsedOrExpandedState;
  }

  /**
   * Returns the tree nodes description text.
   *
   * <p>Note: this function will recursively iterate through the node's subtree if the source node
   * has no content description and shouldIterateChildren is true.
   *
   * <p>Note: this function appends a child node's tree description if the node role is a top-level
   * scroll item, such as {@link Role.ROLE_LIST}, {@link Role.ROLE_GRID} and {@link
   * Role.ROLE_PAGER}, and shouldAppendChildNode is true. This typically narrowly scoped to live
   * region changes not coming from Chrome.
   */
  private CharSequence treeNodesDescription(
      AccessibilityNodeInfoCompat node,
      AccessibilityEvent event,
      boolean shouldIterateChildren,
      boolean shouldAppendChildNode) {
    int role = Role.getRole(node);
    List<CharSequence> joinList = new ArrayList<>();
    // Join the role description text.
    joinList.add(roleDescriptionExtractor.nodeRoleDescriptionText(node, event));

    boolean isContentDescriptionEmpty =
        TextUtils.isEmpty(
            AccessibilityNodeFeedbackUtils.getNodeContentDescription(
                node, context, globalVariables));
    StringBuilder logString = new StringBuilder();
    logString
        .append(String.format(" (%s)", node.hashCode()))
        .append(String.format(", role=%s", Role.roleToString(role)))
        .append(String.format(", isContentDescriptionEmpty=%b", isContentDescriptionEmpty))
        .append(String.format(", shouldAppendChildNode=%b", shouldAppendChildNode));

    if (shouldIterateChildren
        && role != Role.ROLE_WEB_VIEW
        && (role == Role.ROLE_GRID
            || role == Role.ROLE_LIST
            || role == Role.ROLE_PAGER
            || isContentDescriptionEmpty)) {
      // Append the node description if needed. It recurse on all visible & un-focusable children,
      // ascending.
      ReorderedChildrenIterator childIterator =
          ReorderedChildrenIterator.createAscendingIterator(node);
      if (!childIterator.hasNext()) {
        logString.append(", hasNoNextChildNode");
      }
      while (childIterator.hasNext()) {
        AccessibilityNodeInfoCompat childNode = childIterator.next();
        if (childNode == null) {
          logString.append(
              String.format("error: sourceNode (%s) has a null child.", node.hashCode()));
        } else {
          boolean isVisible = AccessibilityNodeInfoUtils.isVisible(childNode);
          boolean isAccessibilityFocusable =
              AccessibilityNodeInfoUtils.isAccessibilityFocusable(childNode);
          logString
              .append(String.format("\n        childNode:(%s)", childNode.hashCode()))
              .append(String.format(", isVisible=%b", isVisible))
              .append(String.format(", isAccessibilityFocusable=%b", isAccessibilityFocusable));

          if (isVisible && (!isAccessibilityFocusable || shouldAppendChildNode)) {
            // Join the tree description of child node.
            CharSequence description =
                getAppendedTreeDescription(
                    childNode, event, shouldIterateChildren, shouldAppendChildNode);
            logString.append(
                String.format("\n        > appendChildNodeDescription= {%s}", description));
            joinList.add(description);
          }
        }
      }
    }

    LogUtils.v(TAG, "      treeNodesDescription:  %s", logString.toString());

    return CompositorUtils.joinCharSequences(joinList, CompositorUtils.getSeparator(), PRUNE_EMPTY);
  }
}
