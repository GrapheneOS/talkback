/*
 * Copyright (C) 2017 The Android Open Source Project
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

import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK;
import static com.google.android.accessibility.talkback.Feedback.EditText.Action.SELECT_SEGMENT;
import static com.google.android.accessibility.talkback.contextmenu.ListMenuManager.MenuId.CUSTOM_ACTION;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.accessibilityservice.GestureDescription.StrokeDescription;
import android.accessibilityservice.TouchInteractionController;
import android.graphics.Path;
import android.graphics.Rect;
import android.text.style.URLSpan;
import android.view.ViewConfiguration;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityWindowInfoCompat;
import com.google.android.accessibility.talkback.ActorStateWritable;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Interpretation;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService.GestureDetectionState;
import com.google.android.accessibility.talkback.UserInterface.UserInputEventListener;
import com.google.android.accessibility.talkback.WebActor;
import com.google.android.accessibility.talkback.actor.helper.FocusActorHelper;
import com.google.android.accessibility.talkback.compositor.GlobalVariables;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.talkback.focusmanagement.AccessibilityFocusMonitor;
import com.google.android.accessibility.talkback.focusmanagement.interpreter.ScreenStateMonitor;
import com.google.android.accessibility.talkback.focusmanagement.record.AccessibilityFocusActionHistory;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionInfo;
import com.google.android.accessibility.talkback.focusmanagement.record.FocusActionRecord;
import com.google.android.accessibility.talkback.utils.LinkUtils;
import com.google.android.accessibility.talkback.utils.LinkUtils.LinkSpan;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.android.accessibility.utils.PerformActionUtils;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.WebInterfaceUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/** FocusActor executes focus-feedback, using FocusManagerInternal. */
// TODO: Merge FocusActor with FocusManagerInternal.
public class FocusActor implements UserInputEventListener {

  private static final String TAG = "FocusActor";
  private static final int STROKE_TIME_GAP_MS = 40;

  /** The only class in TalkBack which has direct access to accessibility focus from framework. */
  private final FocusManagerInternal focusManagerInternal;

  private final AccessibilityService service;
  private final AccessibilityFocusActionHistory history;
  private final AccessibilityFocusMonitor accessibilityFocusMonitor;

  /** Actor-state passed in from pipeline, which encapsulates {@code history}. */
  private ActorStateWritable actorState;

  private final WebActor webActor;
  private final GestureDetectionState gestureDetectionState;
  private Pipeline.FeedbackReturner pipeline;
  private ListMenuManager menuManager;
  private GlobalVariables globalVariables;

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Construction

  public FocusActor(
      AccessibilityService service,
      FocusFinder focusFinder,
      ScreenStateMonitor.State screenState,
      AccessibilityFocusActionHistory accessibilityFocusActionHistory,
      AccessibilityFocusMonitor accessibilityFocusMonitor,
      GestureDetectionState gestureDetectionState,
      GlobalVariables globalVariables) {
    this.service = service;
    this.history = accessibilityFocusActionHistory;
    this.accessibilityFocusMonitor = accessibilityFocusMonitor;
    this.gestureDetectionState = gestureDetectionState;
    this.globalVariables = globalVariables;
    focusManagerInternal =
        new FocusManagerInternal(
            service, focusFinder, screenState, history, accessibilityFocusMonitor);
    webActor = new WebActor(service, this::updateFocusHistory);
  }

  public void setActorState(ActorStateWritable actorState) {
    this.actorState = actorState;
    focusManagerInternal.setActorState(actorState);
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
    focusManagerInternal.setPipeline(pipeline);
    webActor.setPipeline(pipeline);
  }

  public void setMenuManager(ListMenuManager menuManager) {
    this.menuManager = menuManager;
  }

  public AccessibilityFocusActionHistory.Reader getHistory() {
    return history.reader;
  }

  //////////////////////////////////////////////////////////////////////////////////////////////////
  // Methods

  public boolean clickCurrentFocus(EventId eventId) {
    AccessibilityNodeInfoCompat currentFocus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    return clickNode(currentFocus, eventId);
  }

  public boolean clickNode(AccessibilityNodeInfoCompat node, EventId eventId) {
    if (node == null || pipeline == null) {
      return false;
    }

    if (globalVariables.supportClickableLinks() && tryClickLinks(node, eventId)) {
      return true;
    }

    if (PerformActionUtils.isNodeSupportAction(node, AccessibilityNodeInfoCompat.ACTION_CLICK)
        && pipeline.returnFeedback(eventId, Feedback.nodeAction(node, ACTION_CLICK.getId()))) {
      return true;
    }
    if (gestureDetectionState.gestureDetector()) {
      TouchInteractionController controller =
          service.getTouchInteractionController(
              AccessibilityWindowInfoUtils.getDisplayId(node.getWindow()));
      if (controller != null) {
        controller.performClick();
        return true;
      }
    }
    // When none of the preceding conditions are met, the last resort is simulating click event.
    return simulateClickOnNode(service, node);
  }

  public boolean doubleClickCurrentFocus(EventId eventId) {
    AccessibilityNodeInfoCompat currentFocus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    return doubleClickNode(currentFocus, eventId);
  }

  public boolean doubleClickNode(AccessibilityNodeInfoCompat node, EventId eventId) {
    return pipeline.returnFeedback(eventId, Feedback.edit(node, SELECT_SEGMENT))
        || simulateDoubleClickOnNode(service, node);
  }

  public boolean longClickCurrentFocus(EventId eventId) {
    AccessibilityNodeInfoCompat currentFocus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    return longClickNode(currentFocus, eventId);
  }

  public boolean longClickNode(AccessibilityNodeInfoCompat node, EventId eventId) {
    if (node == null || pipeline == null) {
      return false;
    }
    return pipeline.returnFeedback(eventId, Feedback.nodeAction(node, ACTION_LONG_CLICK.getId()));
  }

  public boolean clickCurrentHierarchical(@Nullable EventId eventId) {
    AccessibilityNodeInfoCompat currentFocus =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (currentFocus == null) {
      return false;
    }
    AccessibilityNodeInfoCompat nodeToClick =
        AccessibilityNodeInfoUtils.getSelfOrMatchingAncestor(
            currentFocus, AccessibilityNodeInfoUtils.FILTER_CLICKABLE);
    return pipeline.returnFeedback(eventId, Feedback.nodeAction(nodeToClick, ACTION_CLICK.getId()));
  }

  public void clearAccessibilityFocus(EventId eventId) {
    focusManagerInternal.clearAccessibilityFocus(eventId);
  }

  /** Allows menus to prevent announcing content-focus event after edit-menu dismissed. */
  public void setMuteNextFocus() {
    focusManagerInternal.setMuteNextFocus();
  }

  public void renewEnsureFocus() {
    focusManagerInternal.renewEnsureFocus();
  }

  /** Passes through to FocusManagerInternal.setAccessibilityFocus() */
  public boolean setAccessibilityFocus(
      @NonNull AccessibilityNodeInfoCompat node,
      boolean forceRefocusIfAlreadyFocused,
      @NonNull FocusActionInfo focusActionInfo,
      EventId eventId) {
    return focusManagerInternal.setAccessibilityFocus(
        node, forceRefocusIfAlreadyFocused, focusActionInfo, eventId);
  }

  public WebActor getWebActor() {
    return webActor;
  }

  /** Passes through to FocusManagerInternal.updateFocusHistory() */
  private void updateFocusHistory(
      @NonNull AccessibilityNodeInfoCompat pivot, @NonNull FocusActionInfo focusActionInfo) {
    focusManagerInternal.updateFocusHistory(pivot, focusActionInfo);
  }

  /**
   * Caches the current focused node especially for context menu and dialogs, which is used to
   * restore focus when context menu or dialog closes. It can work by calling {@code
   * overrideNextFocusRestorationForWindowTransition} before next window transition to invoke {@code
   * restoreFocus} when screen state changes to restore the cached focus.
   *
   * <p>If the cached focused node is null, the current focused node will be the target node for
   * restore focus.
   *
   * <p>This is a workaround to restore focus when returning from special windows or web containers,
   * other cases will fallback to standard flow to assign focus. And it is used for below cases:
   * <li>non-active window: REFERTO, restore focus on non-active window after popup
   *     window close.
   * <li>system window: REFERTO, restore focus on system window after popup window
   *     close.
   * <li>WebView: REFERTO, restore focus on a view in a web container after popup
   *     window close.
   *
   * @return true if cached node successfully, otherwise false
   */
  public boolean cacheNodeToRestoreFocus(@Nullable AccessibilityNodeInfoCompat targetNode) {
    if (targetNode != null) {
      history.cacheNodeToRestoreFocus(targetNode);
      return true;
    } else {
      targetNode =
          accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    }
    if (targetNode == null) {
      return false;
    }

    if (WebInterfaceUtils.isWebContainer(targetNode)) {
      history.cacheNodeToRestoreFocus(targetNode);
      return true;
    }

    AccessibilityWindowInfoCompat windowInfoCompat =
        AccessibilityNodeInfoUtils.getWindow(targetNode);

    if (windowInfoCompat != null
        && (!windowInfoCompat.isActive()
            || windowInfoCompat.getType() == AccessibilityWindowInfo.TYPE_SYSTEM)) {
      history.cacheNodeToRestoreFocus(targetNode);
      return true;
    }

    return false;
  }

  /**
   * Clears node to restore focus after context menu/dialog closes, and returns whether node
   * existed.
   */
  public boolean popCachedNodeToRestoreFocus() {
    return (history.popCachedNodeToRestoreFocus() != null);
  }

  /** Restore focus with the node cached before context menu or dialog appeared. */
  public boolean restoreFocus(@Nullable EventId eventId) {
    AccessibilityNodeInfoCompat nodeToRestoreFocus = history.popCachedNodeToRestoreFocus();
    if (nodeToRestoreFocus == null) {
      return false;
    }

    if (!nodeToRestoreFocus.refresh() || !nodeToRestoreFocus.isVisibleToUser()) {
      return false;
    }
    // Checks if the node is in the window for pane changes.
    // b/365017291: Only check native nodes because sometimes the WebView is gone in the node tree.
    if (!WebInterfaceUtils.isWebContainer(nodeToRestoreFocus)
        && !AccessibilityNodeInfoUtils.isInWindow(
            nodeToRestoreFocus, AccessibilityNodeInfoUtils.getWindow(nodeToRestoreFocus))) {
      LogUtils.e(TAG, "Do not restore focus from the invalid node " + nodeToRestoreFocus);
      return false;
    }
    return focusManagerInternal.setAccessibilityFocus(
        nodeToRestoreFocus,
        /* forceRefocusIfAlreadyFocused= */ false,
        FocusActionInfo.builder()
            .setSourceAction(FocusActionInfo.SCREEN_STATE_CHANGE)
            .setInitialFocusType(FocusActionInfo.RESTORED_LAST_FOCUS)
            .build(),
        eventId);
  }

  /** Restores focus precisely at the next {@link ScreenState} change. */
  public void overrideNextFocusRestorationForWindowTransition() {
    actorState.setOverrideFocusRestore();
  }

  /**
   * Checks the accessibility focused node on the current screen, and requests an initial focus if
   * no focused node is found.
   */
  public boolean ensureAccessibilityFocusOnScreen(EventId eventId) {
    return focusManagerInternal.ensureAccessibilityFocusOnScreen(eventId);
  }

  /**
   * Attempts to announce the URL of the focused content or notify if content contains any URLs. If
   * no URL is found, returns false.
   */
  public boolean readFocusedContentLinkUrl(EventId eventId) {
    String warningMessage = service.getString(R.string.read_link_url_no_url_found);
    AccessibilityNodeInfoCompat focusedNode =
        accessibilityFocusMonitor.getAccessibilityFocus(/* useInputFocusIfEmpty= */ false);
    if (focusedNode == null) {
      pipeline.returnFeedback(eventId, Feedback.speech(warningMessage));
      return false;
    }
    if (!readUrlFromExtraBundle(focusedNode, eventId)
        && !readUrlFromLinkSpan(focusedNode, eventId)) {
      pipeline.returnFeedback(eventId, Feedback.speech(warningMessage));
      return false;
    }
    return true;
  }

  /**
   * Reads the URL from the node's extras bundle. Returns false if extras bundle does not contain
   * key or value is empty string.
   */
  private boolean readUrlFromExtraBundle(
      @NonNull AccessibilityNodeInfoCompat focusedNode, EventId eventId) {
    // Target URL string comes from AccessibilityNodeInfoBuilder#EXTRAS_KEY_TARGET_URL defined at
    // go/a11y_node_info_builder_extra_target_url_key.
    final String urlKey = "AccessibilityNodeInfo.targetUrl";
    Object urlValue = focusedNode.getExtras().get(urlKey);
    if (urlValue != null && urlValue.toString().length() > 0) {
      return pipeline.returnFeedback(
          eventId, Feedback.speech(service.getString(R.string.read_link_url_url_found, urlValue)));
    }
    return false;
  }

  /**
   * Reads the URL from the node's LinkSpan or notifies if node contains more than one LinkSpan.
   * Returns false if node does not contain LinkSpans or focused index is out-of-range.
   */
  private boolean readUrlFromLinkSpan(
      @NonNull AccessibilityNodeInfoCompat focusedNode, EventId eventId) {
    final List<LinkSpan> linkSpans = LinkUtils.getLinkSpansInNodeGroup(focusedNode);
    if (linkSpans.isEmpty()) {
      return false;
    }
    int index = getFocusedLinkIndex(focusedNode);
    // Node contains a single link then read available link.
    if (linkSpans.size() == 1) {
      index = 0;
    }
    // Index is within valid range then read URL.
    if (0 <= index && index < linkSpans.size()) {
      // Nodes that provide the URL in the span matching the focused link index.
      LinkSpan linkSpan = linkSpans.get(index);
      if (linkSpan.span instanceof URLSpan span) {
        return pipeline.returnFeedback(
            eventId,
            Feedback.speech(service.getString(R.string.read_link_url_url_found, span.getURL())));
      } else {
        return pipeline.returnFeedback(
            eventId,
            Feedback.speech(service.getString(R.string.read_link_url_url_found, linkSpan.label)));
      }
    }
    // Node contains multiple links but no focused link.
    return pipeline.returnFeedback(
        eventId, Feedback.speech(service.getString(R.string.read_link_url_multiple_links_found)));
  }

  /** Simulates a click on the center of a view. */
  private boolean simulateClickOnNode(
      AccessibilityService accessibilityService, AccessibilityNodeInfoCompat node) {
    Rect rect = new Rect();
    node.getBoundsInScreen(rect);
    Path path = new Path();
    path.moveTo(rect.centerX(), rect.centerY());
    int durationMs = ViewConfiguration.getTapTimeout();
    GestureDescription gestureDescription =
        new GestureDescription.Builder()
            .addStroke(new StrokeDescription(path, /* startTime= */ 0, durationMs))
            .addStroke(new StrokeDescription(path, durationMs + STROKE_TIME_GAP_MS, durationMs))
            .build();
    return accessibilityService.dispatchGesture(
        gestureDescription, /* callback= */ null, /* handler= */ null);
  }

  /** Simulates a double-click on the center of a view. */
  private boolean simulateDoubleClickOnNode(
      AccessibilityService accessibilityService, AccessibilityNodeInfoCompat node) {
    Rect rect = new Rect();
    node.getBoundsInScreen(rect);
    Path path = new Path();
    path.moveTo(rect.centerX(), rect.centerY());
    int durationMs = ViewConfiguration.getTapTimeout();
    GestureDescription gestureDescription =
        new GestureDescription.Builder()
            .addStroke(new StrokeDescription(path, /* startTime= */ 0, durationMs))
            .addStroke(new StrokeDescription(path, durationMs + STROKE_TIME_GAP_MS, durationMs))
            .addStroke(
                new StrokeDescription(path, 2L * (durationMs + STROKE_TIME_GAP_MS), durationMs))
            .addStroke(
                new StrokeDescription(path, 3L * (durationMs + STROKE_TIME_GAP_MS), durationMs))
            .build();
    return accessibilityService.dispatchGesture(
        gestureDescription, /* callback= */ null, /* handler= */ null);
  }

  /**
   * Clicks the native links in the node by priorities:
   * <li>If a link inside the node is focused, click the current focused link.
   * <li>If the node is clickable, return false to continue to click the node.
   * <li>If the node has single link, click the single link.
   * <li>If the node has multiple links, show the context menu.
   *
   * @return true if any link is clicked, otherwise false.
   */
  private boolean tryClickLinks(AccessibilityNodeInfoCompat node, EventId eventId) {
    int focusedLinkIndex = getFocusedLinkIndex(node);

    if (focusedLinkIndex == FocusActionInfo.LINK_INDEX_NOT_SET
        && AccessibilityNodeInfoUtils.isClickable(node)) {
      return false;
    }

    final List<LinkSpan> linkSpans = LinkUtils.getLinkSpansInNodeGroup(node);
    if (linkSpans.isEmpty()) {
      return false;
    }

    int index = -1;
    if (linkSpans.size() == 1) {
      index = 0;
    } else if (focusedLinkIndex >= 0 && focusedLinkIndex < linkSpans.size()) {
      index = focusedLinkIndex;
    }

    LogUtils.d(TAG, "tryClickLinks with index=%s", index);
    if (index >= 0) {
      LinkSpan linkSpan = linkSpans.get(index);
      return LinkUtils.activateLinkSpan(service, linkSpan);
    } else {
      return menuManager.showMenu(CUSTOM_ACTION, eventId);
    }
  }

  private int getFocusedLinkIndex(AccessibilityNodeInfoCompat node) {
    FocusActionRecord focusActionRecord = history.getLastFocusActionRecord();
    if (focusActionRecord == null) {
      return FocusActionInfo.LINK_INDEX_NOT_SET;
    }
    int curLinkIndex = focusActionRecord.getExtraInfo().getFocusedLinkIndex();
    if (node.equals(focusActionRecord.getFocusedNode()) && (curLinkIndex >= 0)) {
      return curLinkIndex;
    }
    return FocusActionInfo.LINK_INDEX_NOT_SET;
  }

  @Override
  public void newItemFocused(
      AccessibilityNodeInfo nodeInfo, Interpretation.@NonNull AccessibilityFocused axFocused) {
    AccessibilityNodeInfoCompat node = AccessibilityNodeInfoUtils.toCompat(nodeInfo);
    if (node == null || axFocused.focusActionInfo() == null) {
      return;
    }
    if (FocusActorHelper.shouldSyncInputFocusToAccessibilityFocus(
        service, node, axFocused.focusActionInfo())) {
      focusManagerInternal.setFocus(EVENT_ID_UNTRACKED, node);
    }
    if (axFocused.focusActionInfo().sourceAction != FocusActionInfo.LOGICAL_NAVIGATION
        && axFocused.focusActionInfo().sourceAction != FocusActionInfo.TOUCH_EXPLORATION) {
      pipeline.returnFeedback(
          EVENT_ID_UNTRACKED, Feedback.speech(Feedback.Speech.Action.RESET_FORMATTING_HISTORY));
    }
  }
}
