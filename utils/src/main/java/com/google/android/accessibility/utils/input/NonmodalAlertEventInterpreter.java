/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.accessibility.utils.input;

import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_APPEARED;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_PANE_DISAPPEARED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOWS_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_ADDED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_REMOVED;
import static com.google.android.accessibility.utils.StringBuilderUtils.optionalTag;

import android.accessibilityservice.AccessibilityService;
import android.view.Display;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.Consumer;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.StringBuilderUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.util.ArrayList;
import java.util.List;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Handles appearances and disappearances of nodes with the {@link Role.ROLE_NONMODAL_ALERT} role.
 * These nodes should belong to a pane or window.
 */
public class NonmodalAlertEventInterpreter implements AccessibilityEventListener {
  private static final String TAG = "NonmodalAlertEventInterpreter";

  /** Event types that are handled by NonmodalAlertEventInterpreter. */
  private static final int MASK_EVENTS = TYPE_WINDOW_STATE_CHANGED | TYPE_WINDOWS_CHANGED;

  /** This is {@code true} if we are actively tracking an alert. */
  private boolean tracking = false;

  private final TrackedAlert trackedAlert = new TrackedAlert();
  private final List<Consumer<Interpretation>> listeners = new ArrayList<>();
  private final AccessibilityService service;

  public NonmodalAlertEventInterpreter(AccessibilityService service) {
    this.service = service;
  }

  @Override
  public int getEventTypes() {
    return MASK_EVENTS;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    boolean notifyListeners = false;
    if (event.getEventType() == TYPE_WINDOW_STATE_CHANGED) {
      if ((event.getContentChangeTypes() & CONTENT_CHANGE_TYPE_PANE_APPEARED) != 0) {
        if (tracking) {
          return;
        }
        AccessibilityNodeInfoCompat eventSource = AccessibilityEventUtils.sourceCompat(event);
        if (eventSource == null || !isNonmodalAlert(eventSource)) {
          return;
        }
        trackedAlert.reset();
        trackedAlert.setAlert(eventSource);
        trackedAlert.setTitle(eventSource.getPaneTitle());
        tracking = true;
        notifyListeners = true;
      } else if ((event.getContentChangeTypes() & CONTENT_CHANGE_TYPE_PANE_DISAPPEARED) != 0) {
        // Don't reset state if we're not tracking or we're actively tracking a window alert.
        if (!tracking
            || trackedAlert.getWindowId() != AccessibilityWindowInfoUtils.WINDOW_ID_NONE) {
          return;
        }
        trackedAlert.reset();
        tracking = false;
        notifyListeners = true;
        LogUtils.v(TAG, "reset tracked nonmodal alert");
      }
    } else if (event.getEventType() == TYPE_WINDOWS_CHANGED) {
      if (!BuildVersionUtils.isAtLeastP()) {
        return;
      }
      if ((event.getWindowChanges() & WINDOWS_CHANGE_ADDED) != 0) {
        if (tracking) {
          return;
        }
        int addedDisplayId = AccessibilityEventUtils.getDisplayId(event);
        List<AccessibilityWindowInfo> windows =
            AccessibilityServiceCompatUtils.getWindowsOnAllDisplays(service).get(addedDisplayId);
        if (windows == null) {
          return;
        }
        int addedWindowId = AccessibilityEventUtils.getWindowId(event);
        AccessibilityWindowInfo addedWindow = null;
        // Find the added window from the event's window id.
        for (AccessibilityWindowInfo window : windows) {
          if (addedWindowId == window.getId()) {
            addedWindow = window;
            break;
          }
        }
        if (addedWindow == null) {
          return;
        }
        AccessibilityNodeInfoCompat addedWindowRoot =
            AccessibilityWindowInfoUtils.getRootCompat(addedWindow);
        if (addedWindowRoot == null || !isNonmodalAlert(addedWindowRoot)) {
          return;
        }
        trackedAlert.reset();
        trackedAlert.setAlert(addedWindowRoot);
        trackedAlert.setTitle(AccessibilityWindowInfoUtils.getTitle(addedWindow));
        trackedAlert.setWindowId(addedWindowId);
        trackedAlert.setDisplayId(addedDisplayId);
        tracking = true;
        notifyListeners = true;
      } else if ((event.getWindowChanges() & WINDOWS_CHANGE_REMOVED) != 0) {
        // Don't reset state if we're not tracking or we're actively tracking a pane alert.
        if (!tracking
            || trackedAlert.getWindowId() == AccessibilityWindowInfoUtils.WINDOW_ID_NONE
            || trackedAlert.getDisplayId() == Display.INVALID_DISPLAY
            || trackedAlert.getWindowId() != AccessibilityEventUtils.getWindowId(event)
            || trackedAlert.getDisplayId() != AccessibilityEventUtils.getDisplayId(event)) {
          return;
        }
        trackedAlert.reset();
        tracking = false;
        notifyListeners = true;
        LogUtils.v(TAG, "reset tracked nonmodal alert");
      }
    }
    if (notifyListeners) {
      notifyListeners(event, eventId);
    }
  }

  private static boolean isNonmodalAlert(AccessibilityNodeInfoCompat node) {
    return Role.getRole(node) == Role.ROLE_NON_MODAL_ALERT;
  }

  public void addListener(Consumer<Interpretation> listener) {
    listeners.add(listener);
  }

  private void notifyListeners(AccessibilityEvent event, EventId eventId) {
    LogUtils.v(
        TAG,
        "alert %s event=%s appeared=%b title=%s actionable=%b",
        trackedAlert.getAlert(),
        event,
        trackedAlert.getAlert() != null,
        trackedAlert.getTitle(),
        trackedAlert.isActionable());
    Interpretation interpretation =
        new Interpretation(
            event,
            eventId,
            trackedAlert.getAlert() != null,
            trackedAlert.getAlert(),
            trackedAlert.getTitle(),
            trackedAlert.isActionable());
    for (Consumer<Interpretation> listener : listeners) {
      listener.accept(interpretation);
    }
  }

  /** Data-structure containing raw-event and interpretation, sent to listeners. */
  public static class Interpretation {
    public final @NonNull AccessibilityEvent event;
    public final @NonNull EventId eventId;
    public final boolean alertAppeared;
    public final @Nullable AccessibilityNodeInfoCompat alert;
    public final @Nullable CharSequence title;
    public final boolean actionable;

    public Interpretation(
        @NonNull AccessibilityEvent event,
        @NonNull EventId eventId,
        boolean alertAppeared,
        @Nullable AccessibilityNodeInfoCompat alert,
        @Nullable CharSequence title,
        boolean actionable) {
      this.event = event;
      this.eventId = eventId;
      this.alertAppeared = alertAppeared;
      this.alert = alert;
      this.title = title;
      this.actionable = actionable;
    }

    @Override
    public String toString() {
      return StringBuilderUtils.joinFields(
          eventId.toString(),
          String.valueOf(title),
          optionalTag("alert appeared", alertAppeared),
          optionalTag("actionable", actionable));
    }
  }

  /** Data-structure containing the tracked alert. */
  private static class TrackedAlert {
    private @Nullable AccessibilityNodeInfoCompat alert;
    private @Nullable CharSequence title = null;
    private int windowId = AccessibilityWindowInfoUtils.WINDOW_ID_NONE;
    private int displayId = Display.INVALID_DISPLAY;

    public TrackedAlert() {}

    public void reset() {
      alert = null;
      title = null;
      windowId = AccessibilityWindowInfoUtils.WINDOW_ID_NONE;
      displayId = Display.INVALID_DISPLAY;
    }

    public void setAlert(@Nullable AccessibilityNodeInfoCompat alert) {
      this.alert = alert;
    }

    public @Nullable AccessibilityNodeInfoCompat getAlert() {
      return alert;
    }

    public void setTitle(@Nullable CharSequence title) {
      this.title = title;
    }

    public @Nullable CharSequence getTitle() {
      return title;
    }

    public void setWindowId(int windowId) {
      this.windowId = windowId;
    }

    public int getWindowId() {
      return windowId;
    }

    public void setDisplayId(int displayId) {
      this.displayId = displayId;
    }

    public int getDisplayId() {
      return displayId;
    }

    public boolean isActionable() {
      return Role.getRole(alert) == Role.ROLE_NON_MODAL_ALERT
          && AccessibilityNodeInfoUtils.isOrHasMatchingDescendant(
              alert, AccessibilityNodeInfoUtils.FILTER_CLICKABLE);
    }
  }
}
