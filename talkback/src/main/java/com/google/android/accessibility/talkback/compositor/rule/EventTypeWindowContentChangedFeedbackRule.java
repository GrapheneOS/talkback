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

import static android.view.View.ACCESSIBILITY_LIVE_REGION_ASSERTIVE;
import static android.view.View.ACCESSIBILITY_LIVE_REGION_NONE;
import static android.view.View.ACCESSIBILITY_LIVE_REGION_POLITE;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_DRAG_CANCELLED;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_DRAG_DROPPED;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_DRAG_STARTED;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_ENABLED;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_ERROR;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED;
import static com.google.android.accessibility.talkback.compositor.Compositor.EVENT_TYPE_WINDOW_CONTENT_CHANGED;
import static com.google.android.accessibility.talkback.compositor.TalkBackFeedbackProvider.EMPTY_FEEDBACK;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_QUEUE;
import static com.google.android.accessibility.utils.output.SpeechController.QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH;
import static com.google.android.accessibility.utils.output.SpeechController.UTTERANCE_GROUP_CONTENT_CHANGE;
import static com.google.android.accessibility.utils.output.SpeechController.UTTERANCE_GROUP_DEFAULT;
import static com.google.android.accessibility.utils.output.SpeechController.UTTERANCE_GROUP_PROGRESS_BAR_PROGRESS;
import static com.google.android.accessibility.utils.output.SpeechController.UTTERANCE_GROUP_SEEK_PROGRESS;

import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.compositor.AccessibilityEventFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.AccessibilityNodeFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.Compositor.HandleEventOptions;
import com.google.android.accessibility.talkback.compositor.EarconFeedbackUtils;
import com.google.android.accessibility.talkback.compositor.EventFeedback;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.compositor.WindowContentChangeAnnouncementFilter;
import com.google.android.accessibility.talkback.compositor.roledescription.RoleDescriptionExtractor;
import com.google.android.accessibility.talkback.compositor.roledescription.TreeNodesDescription;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Event feedback rules for {@link EVENT_TYPE_WINDOW_CONTENT_CHANGED} event. These rules will
 * provide the event feedback output function by inputting the {@link HandleEventOptions} and
 * outputting {@link EventFeedback}.
 */
public final class EventTypeWindowContentChangedFeedbackRule {

  private static final String TAG = "EventTypeWindowContentChangedFeedbackRule";

  /**
   * Adds the feedback rules to the provided event feedback rules map, {@code eventFeedbackRules}.
   * So {@link TalkBackFeedbackProvider} can populate the event feedback with the given event.
   *
   * @param eventFeedbackRules the event feedback rules
   * @param context the parent context
   * @param globalVariables the global compositor variables
   * @param roleDescriptionExtractor the node role description extractor
   * @param treeNodesDescription the node tree description extractor
   */
  public static void addFeedbackRule(
      Map<Integer, Function<HandleEventOptions, EventFeedback>> eventFeedbackRules,
      Context context,
      GlobalVariables globalVariables,
      RoleDescriptionExtractor roleDescriptionExtractor,
      TreeNodesDescription treeNodesDescription) {
    eventFeedbackRules.put(
        EVENT_TYPE_WINDOW_CONTENT_CHANGED,
        eventOptions ->
            windowContentChanged(
                eventOptions.eventObject,
                eventOptions.sourceNode,
                context,
                globalVariables,
                roleDescriptionExtractor,
                treeNodesDescription));
  }

  private static EventFeedback windowContentChanged(
      AccessibilityEvent event,
      AccessibilityNodeInfoCompat node,
      Context context,
      GlobalVariables globalVariables,
      RoleDescriptionExtractor roleDescriptionExtractor,
      TreeNodesDescription treeNodesDescription) {
    if (node == null) {
      LogUtils.e(TAG, "    windowContentChanged: error, node is null");
      return EMPTY_FEEDBACK;
    }

    int eventSrcRole = Role.getSourceRole(event);
    int nodeId = node.hashCode();
    int nodeRole = Role.getRole(node);
    int nodeLiveRegion = node.getLiveRegion();
    int queueMode = queueMode(nodeLiveRegion);
    boolean isChromeEvent = isEventFromChrome(node, event);

    CharSequence ttsOutput;
    // Do not handle the live region root node content change if the event comes from chrome. Chrome
    // will fire content change event for each live region child node that has changed.
    // Put this change behind the flag. e.g.:
    // 1) Flag off -> handle live region root node content change no matter if it's Chrome event or
    // not. No behavior change.
    // 2) Flag on + non-Chrome event -> handle live region root node content change. No behavior
    // change for native Android components.
    // 3) Flag on + Chrome event -> do not handle live region root node content change. Instead,
    // handle the live region descendant node content change later.
    if (nodeLiveRegion != ACCESSIBILITY_LIVE_REGION_NONE
        && (!FeatureFlagReader.enableOnlyAnnounceChangedLiveRegionNodes(context)
            || !isChromeEvent)) {
      ttsOutput = treeNodesDescription.aggregateNodeTreeDescription(node, event);
    } else {
      ttsOutput =
          computeWindowContentChangedStateText(
              node, event, context, roleDescriptionExtractor, globalVariables);
    }

    // Handle the live region node content change if the event comes from Chrome.
    // If the ttsOutput is not empty and already handled by specific content change type, we don't
    // need to handle it again even if the node is the descendant of a live region.
    // Put this change behind the flag. If the flag is off, no behavior change.
    //
    // NOTE: Chrome is responsible for firing the correct WINDOW_CONTENT_CHANGED event on all nodes
    // that should be announced by TalkBack. This may include the root node or any of its
    // descendants.
    if (TextUtils.isEmpty(ttsOutput)
        && FeatureFlagReader.enableOnlyAnnounceChangedLiveRegionNodes(context)
        && isChromeEvent) {
      // Check whether `node` is a live region root node. If not, see if any of its ancestors are a
      // live region root node.
      Optional<AccessibilityNodeInfoCompat> liveRegionRootNode =
          node.getLiveRegion() != ACCESSIBILITY_LIVE_REGION_NONE
              ? Optional.of(node)
              : getAncestorLiveRegionNode(node);
      if (liveRegionRootNode.isPresent()) {
        // If `node` has or is a live region root, we should set the ttsOutput
        // to match the changes made to `node`. For live region node changes made in Chrome, we
        // should not aggregate our description from the node's subtree, so we set
        // `shouldIterateChildren` to false.
        ttsOutput =
            treeNodesDescription.aggregateNodeTreeDescription(
                node, event, /* shouldIterateChildren= */ false);
        // Additionally, we must calculate the queueMode of this change based on whether the
        // the live region root is assertive or polite (since the child node does not store this
        // information).
        queueMode = queueMode(liveRegionRootNode.get().getLiveRegion());
      }
    }

    boolean nodeNotFrequentAnnounced = true;
    if (!TextUtils.isEmpty(ttsOutput)) {
      nodeNotFrequentAnnounced =
          WindowContentChangeAnnouncementFilter.shouldAnnounce(
              node,
              globalVariables.getTextChangeRateUnlimited(),
              globalVariables.getEnableShortAndLongDurationsForSpecificApps());
      if (!nodeNotFrequentAnnounced) {
        ttsOutput = "";
      }
    }
    boolean forcedFeedback = true;
    // List the event type that shouldn't be announced when microphone or SSB is active.
    if (TextUtils.isEmpty(ttsOutput)
        || nodeLiveRegion == ACCESSIBILITY_LIVE_REGION_POLITE
        || ((event.getContentChangeTypes()
                & (CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
                    | CONTENT_CHANGE_TYPE_TEXT
                    | CONTENT_CHANGE_TYPE_ENABLED))
            != 0)
        || ((event.getContentChangeTypes() & CONTENT_CHANGE_TYPE_STATE_DESCRIPTION) != 0
            && Role.getRole(node) == Role.ROLE_PROGRESS_BAR)) {
      forcedFeedback = false;
    }

    LogUtils.v(
        TAG,
        "windowContentChanged: %s",
        new StringBuilder()
            .append(String.format("(%s) ", nodeId))
            .append(String.format(", ttsOutput= {%s}", ttsOutput))
            .append(String.format(", eventSrcRole=%s", Role.roleToString(eventSrcRole)))
            .append(String.format(", nodeRole=%s", Role.roleToString(nodeRole)))
            .append(String.format(", nodeLiveRegion=%s", nodeLiveRegion))
            .append(String.format(", nodeNotFrequentAnnounced=%s", nodeNotFrequentAnnounced))
            .toString());

    return EventFeedback.builder()
        .setTtsOutput(Optional.of(ttsOutput))
        .setQueueMode(queueMode)
        .setTtsClearQueueGroup(ttsClearQueueGroup(eventSrcRole, event.getContentChangeTypes()))
        .setTtsAddToHistory(true)
        .setTtsSkipDuplicate(true)
        .setForceFeedbackEvenIfAudioPlaybackActive(true)
        .setForceFeedbackEvenIfMicrophoneActive(forcedFeedback)
        .setForceFeedbackEvenIfSsbActive(forcedFeedback)
        .setEarcon(
            EarconFeedbackUtils.getProgressBarChangeEarcon(
                event, node, globalVariables.getPreferredLocaleByNode(node)))
        .setEarconRate(EarconFeedbackUtils.getProgressBarChangeEarconRate(event, node))
        .setEarconVolume(EarconFeedbackUtils.getProgressBarChangeEarconVolume(event, node))
        .build();
  }

  // This function determines whether our AccessibilityEvent was issued by a version of Chrome
  // (Production, Clankium, Chromium, or WebView). We rely on the information provided by Chrome's
  // Accessibility Tree structure to properly announce our live regions.
  // TODO: In the future, we have plans to improve live region support for native
  // events as well.
  private static boolean isEventFromChrome(
      AccessibilityNodeInfoCompat node, AccessibilityEvent event) {
    // There are two ways to determine an event is from Chrome.
    // (1) The name of the app package matches a version of Chrome:
    String packageName = event.getPackageName() == null ? "" : event.getPackageName().toString();
    if (packageName.equals("com.android.chrome") // Production Chrome
        || packageName.equals("com.chrome.canary") // Production Chrome Canary
        || packageName.equals("com.chrome.dev") // Production Chrome Dev
        || packageName.equals("com.chrome.beta") // Production Chrome Beta
        || packageName.equals("com.google.android.apps.chrome") // Clankium
        || packageName.equals("org.chromium.chrome")) { // Chromium
      // If we can verify the event is from Chrome just from the package name, return early.
      return true;
    }

    // (2) Our source node has an ancestor node whose role is ROLE_WEB_VIEW:
    node = node.getParent();
    // Walk up the tree (beginning at our source node) until we reach the root.
    while (node != null) {
      if (Role.getRole(node) == Role.ROLE_WEB_VIEW) {
        return true;
      }
      node = node.getParent();
    }

    // If the node is not from a Chrome application or in a WebView, return false.
    return false;
  }

  // This function optionally returns a given node's Live Region ancestor if present. In Chrome,
  // only the root node of a Live Region subtree is marked as being a Live Region. We need to obtain
  // the root node in order to determine how our changed subtree node should be announced.
  private static Optional<AccessibilityNodeInfoCompat> getAncestorLiveRegionNode(
      AccessibilityNodeInfoCompat node) {
    node = node.getParent();
    // Walk up the subtree (beginning at our changed node) until we reach the root.
    while (node != null) {
      if (node.getLiveRegion() != ACCESSIBILITY_LIVE_REGION_NONE) {
        return Optional.of(node);
      }
      node = node.getParent();
    }
    return Optional.empty();
  }

  private static int getContentChangeType(int contentChangeTypes) {
    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION) != 0) {
      return CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION;
    }
    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT) != 0) {
      return CONTENT_CHANGE_TYPE_TEXT;
    }
    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_STATE_DESCRIPTION) != 0) {
      return CONTENT_CHANGE_TYPE_STATE_DESCRIPTION;
    }
    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_DRAG_STARTED) != 0) {
      return CONTENT_CHANGE_TYPE_DRAG_STARTED;
    }

    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_DRAG_DROPPED) != 0) {
      return CONTENT_CHANGE_TYPE_DRAG_DROPPED;
    }

    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_DRAG_CANCELLED) != 0) {
      return CONTENT_CHANGE_TYPE_DRAG_CANCELLED;
    }
    if (contentChangeTypes == AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED) {
      return CONTENT_CHANGE_TYPE_UNDEFINED;
    }
    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_ERROR) != 0) {
      return CONTENT_CHANGE_TYPE_ERROR;
    }
    if ((contentChangeTypes & AccessibilityEvent.CONTENT_CHANGE_TYPE_ENABLED) != 0) {
      return CONTENT_CHANGE_TYPE_ENABLED;
    }

    return -1;
  }

  private static CharSequence computeWindowContentChangedStateText(
      AccessibilityNodeInfoCompat node,
      AccessibilityEvent event,
      Context context,
      RoleDescriptionExtractor roleDescriptionExtractor,
      GlobalVariables globalVariables) {
    int nodeRole = Role.getRole(node);
    int contentChangeType = getContentChangeType(event.getContentChangeTypes());
    boolean isSelfOrAncestorFocused = AccessibilityNodeInfoUtils.isSelfOrAncestorFocused(node);

    LogUtils.v(
        TAG,
        "  computeWindowContentChangedStateText: %s",
        new StringBuilder()
            .append(
                String.format(
                    "  eventContentChangeTypes=%s",
                    AccessibilityEventUtils.contentChangeTypesToString(
                        event.getContentChangeTypes())))
            .append(
                String.format(
                    "  contentChangeType=%s",
                    (contentChangeType != -1)
                        ? AccessibilityEventUtils.contentChangeTypesToString(contentChangeType)
                        : AccessibilityEventUtils.contentChangeTypesToString(
                            event.getContentChangeTypes())))
            .append(String.format(", isSelfOrAncestorFocused=%s", isSelfOrAncestorFocused))
            .toString());

    switch (contentChangeType) {
      case CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION -> {
        return (isSelfOrAncestorFocused)
            ? AccessibilityEventFeedbackUtils.getEventContentDescription(
                event, globalVariables.getPreferredLocaleByNode(node))
            : "";
      }
      case CONTENT_CHANGE_TYPE_TEXT -> {
        return isSelfOrAncestorFocused && nodeRole != Role.ROLE_EDIT_TEXT
            ? AccessibilityNodeFeedbackUtils.getNodeText(node, context, globalVariables)
            : "";
      }
      case CONTENT_CHANGE_TYPE_STATE_DESCRIPTION -> {
        if (!isSelfOrAncestorFocused) {
          return "";
        }
        if (nodeRole == Role.ROLE_SEEK_CONTROL) {
          return roleDescriptionExtractor.getSeekBarStateDescription(node, event);
        }
        // Fallback to aggregate text.
        CharSequence aggregateText =
            AccessibilityEventFeedbackUtils.getEventAggregateText(
                event, globalVariables.getPreferredLocaleByNode(node));
        if (!TextUtils.isEmpty(aggregateText)) {
          return aggregateText;
        }
        // Fallback to seekBar description for progressBar role.
        // Android widget Progressbar used to send TYPE_VIEW_SELECTED event when
        // progress changes. It then adopted StateDescription. However, if a new
        // widget wants to provide the RangeInfo data only without state
        // description, we can use state change event to announce state changes
        // TODO: moves seekBarStateDescription together for seekBar and progressBar
        if (nodeRole == Role.ROLE_PROGRESS_BAR) {
          return roleDescriptionExtractor.getSeekBarStateDescription(node, event);
        }
        // Fallback to state description.
        return AccessibilityNodeFeedbackUtils.getNodeStateDescription(
            node, context, globalVariables);
      }
      case CONTENT_CHANGE_TYPE_DRAG_STARTED -> {
        return context.getString(R.string.drag_started);
      }
      case CONTENT_CHANGE_TYPE_DRAG_DROPPED -> {
        return context.getString(R.string.drag_dropped);
      }
      case CONTENT_CHANGE_TYPE_DRAG_CANCELLED -> {
        return context.getString(R.string.drag_cancelled);
      }
      case CONTENT_CHANGE_TYPE_UNDEFINED, CONTENT_CHANGE_TYPE_ERROR -> {
        // When an error should be displayed, send
        // AccessibilityEvent#CONTENT_CHANGE_TYPE_INVALID. At this point talkback should
        // indicate the presence of an error to the user. REFERTO.
        return AccessibilityNodeFeedbackUtils.getAccessibilityNodeErrorText(node, context);
      }
      case CONTENT_CHANGE_TYPE_ENABLED -> {
        return AccessibilityNodeFeedbackUtils.getAccessibilityEnabledState(node, context);
      }
      default -> {
        return "";
      }
    }
  }

  private static int queueMode(int nodeLiveRegion) {
    if (nodeLiveRegion == ACCESSIBILITY_LIVE_REGION_ASSERTIVE) {
      return QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH;
    } else if (nodeLiveRegion == ACCESSIBILITY_LIVE_REGION_POLITE) {
      return QUEUE_MODE_UNINTERRUPTIBLE_BY_NEW_SPEECH;
    }
    return QUEUE_MODE_QUEUE;
  }

  private static int ttsClearQueueGroup(int role, int contentChangeType) {
    switch (role) {
      case Role.ROLE_PROGRESS_BAR -> {
        return UTTERANCE_GROUP_PROGRESS_BAR_PROGRESS;
      }
      case Role.ROLE_SEEK_CONTROL -> {
        return UTTERANCE_GROUP_SEEK_PROGRESS;
      }
      default -> {
        if (contentChangeType == CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION
            || contentChangeType == CONTENT_CHANGE_TYPE_TEXT
            || contentChangeType == CONTENT_CHANGE_TYPE_STATE_DESCRIPTION) {
          return UTTERANCE_GROUP_CONTENT_CHANGE;
        } else {
          return UTTERANCE_GROUP_DEFAULT;
        }
      }
    }
  }

  private EventTypeWindowContentChangedFeedbackRule() {}
}
