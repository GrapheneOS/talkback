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
package com.google.android.accessibility.talkback.compositor.rule;

import static com.google.android.accessibility.talkback.compositor.CompositorUtils.PRUNE_EMPTY;

import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.AccessibilityEventFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.AccessibilityNodeFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.CompositorUtils;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.compositor.roledescription.TreeNodesDescription;
import com.google.android.accessibility.talkback.eventprocessor.ProcessorPhoneticLetters;
import com.google.android.accessibility.talkback.imagecaption.ImageContents;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Event feedback rules for {@link EVENT_TYPE_VIEW_ACCESSIBILITY_FOCUSED} event only for TV Surface.
 * These rules will provide the event feedback output function by inputting the {@link
 * HandleEventOptions} and outputting {@link EventFeedback}.
 */
public final class EventTypeViewAccessibilityFocusedFeedbackRuleForTV {

  private static final String TAG = "EventTypeViewAccessibilityFocusedFeedbackRuleForTV";

  /**
   * Returns TTS feedback text for {@link EVENT_TYPE_VIEW_ACCESSIBILITY_FOCUSED} event for TV
   * Surface.
   *
   * <ul>
   *   The feedback text is composed of below sequential elements. AGUA requirement. b/334050530:
   *   <li>1. Window transition,
   *   <li>2. Collection transition, or Container transition,
   *   <li>3. Collection item transition or Node role/heading description,
   *   <li>4. Unlabelled description or Node tree description or Event description,
   * </ul>
   */
  public static CharSequence viewAccessibilityFocusedDescriptionForTv(
      AccessibilityEvent event,
      AccessibilityNodeInfoCompat node,
      boolean isEventNavigateByUser,
      Context context,
      ImageContents imageContents,
      GlobalVariables globalVariables,
      ProcessorPhoneticLetters processorPhoneticLetters,
      TreeNodesDescription treeNodesDescription) {
    StringBuilder logString = new StringBuilder();
    List<CharSequence> outputJoinList = new ArrayList<>();
    Locale preferredLocale = globalVariables.getPreferredLocaleByNode(node);

    // Prepare window transition state if the window transition happened for feedback.
    CharSequence windowTransition =
        EventTypeViewAccessibilityFocusedFeedbackRule.windowTransitionState(
            isEventNavigateByUser, node, context, globalVariables);
    if (!TextUtils.isEmpty(windowTransition)) {
      outputJoinList.add(windowTransition);
      logString.append(String.format("\n    windowTransition={%s}", windowTransition));
    }

    // Prepare Collection item transition state or Node role/heading description for feedback.
    boolean speakCollectionInfo = globalVariables.getSpeakCollectionInfo();
    boolean speakRoles = globalVariables.getSpeakRoles();
    logString
        .append(String.format("\n Verbosity speakCollectionInfo=%s", speakCollectionInfo))
        .append(String.format(", speakRoles=%s", speakRoles));

    // Prepare collection transition state if the collection transition happened for feedback.
    if (speakCollectionInfo) {
      CharSequence collectionTransition = globalVariables.getCollectionTransitionDescription();
      if (!TextUtils.isEmpty(collectionTransition)) {
        outputJoinList.add(collectionTransition);
        logString.append(String.format("\n    collectionTransition={%s}", collectionTransition));
      } else {
        // Prepare container transition state for feedback.
        CharSequence containerTransition =
            EventTypeViewAccessibilityFocusedFeedbackRule.containerTransitionState(node, context);
        if (!TextUtils.isEmpty(containerTransition)) {
          outputJoinList.add(containerTransition);
          logString.append(String.format("\n    containerTransition={%s}", containerTransition));
        }
      }
    }

    CharSequence collectionItemTransition =
        speakCollectionInfo ? globalVariables.getCollectionItemTransitionDescription(node) : "";
    if (!TextUtils.isEmpty(collectionItemTransition)) {
      outputJoinList.add(collectionItemTransition);
      logString.append(
          String.format("\n    collectionItemTransition={%s}", collectionItemTransition));
    } else if (speakRoles
        && !WebInterfaceUtils.isWebContainer(node)
        && AccessibilityNodeInfoUtils.isHeading(node)) {
      // If the source node has collection item transition, collectionItemTransition text would
      // not be empty. And TalkBack should announce the collection item transition information or it
      // should fallback to announce the role/heading description.
      CharSequence nodeRoleDescription =
          AccessibilityNodeFeedbackUtils.getNodeRoleDescription(node, context, globalVariables);
      if (!TextUtils.isEmpty(nodeRoleDescription)) {
        outputJoinList.add(nodeRoleDescription);
        logString.append(String.format("\n    nodeRoleDescription={%s}", nodeRoleDescription));
      } else {
        outputJoinList.add(context.getString(R.string.heading_template));
        logString.append("\n    heading");
      }
    }

    // Prepare Unlabelled description or Node tree description or Event description for feedback.
    CharSequence nodeUnlabelledState =
        AccessibilityNodeFeedbackUtils.getUnlabelledNodeDescription(
            Role.getRole(node), node, context, imageContents, globalVariables);
    CharSequence eventDescription =
        AccessibilityEventFeedbackUtils.getEventContentDescriptionOrEventAggregateText(
            event, preferredLocale);
    if (!TextUtils.isEmpty(nodeUnlabelledState)) {
      CharSequence unlabelledDescription =
          TextUtils.isEmpty(eventDescription) ? nodeUnlabelledState : eventDescription;
      outputJoinList.add(unlabelledDescription);
      logString
          .append(String.format("\n    unlabelledDescription={%s}", unlabelledDescription))
          .append(String.format(", eventDescription={%s}", eventDescription));
    } else {
      CharSequence nodeTreeDescription =
          treeNodesDescription.aggregateNodeTreeDescription(node, event);
      if (!TextUtils.isEmpty(nodeTreeDescription)) {
        outputJoinList.add(nodeTreeDescription);
        logString.append(String.format("\n    nodeTreeDescription={%s}", nodeTreeDescription));
      } else {
        outputJoinList.add(eventDescription);
        logString.append(String.format("\n    eventDescription={%s}", eventDescription));
      }
    }

    // Add phonetic spelling if necessary.
    Optional<CharSequence> phoneticExample =
        processorPhoneticLetters.getPhoneticLetterForKeyboardFocusEvent(event);
    phoneticExample.ifPresent(outputJoinList::add);
    logString.append(String.format("\n    phoneticExample={%s}", phoneticExample));

    LogUtils.v(TAG, "viewAccessibilityFocusedDescription: %s", logString.toString());

    return CompositorUtils.joinCharSequences(
        outputJoinList, CompositorUtils.getSeparator(), PRUNE_EMPTY);
  }

  private EventTypeViewAccessibilityFocusedFeedbackRuleForTV() {}
}
