/*
 * Copyright (C) 2012 Google Inc.
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

package com.google.android.accessibility.talkback.monitor;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.os.PowerManager;
import com.google.android.accessibility.talkback.compositor.Compositor;
import com.google.android.accessibility.talkback.contextmenu.ListMenuManager;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.Performance;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.util.ArrayList;
import java.util.List;

/** Watches changes in device orientation & bold font style. */
public class DeviceConfigurationMonitor {

  /** A callback when receiving orientation & bold fond style changed. */
  public interface OnConfigurationChangedListener {
    void onConfigurationChanged(Configuration newConfig);
  }

  private final Compositor compositor;
  private final PowerManager powerManager;
  private ListMenuManager menuManager;
  private A11yAlertDialogWrapper dialog;

  /** The orientation of the most recently received configuration. */
  private int lastOrientation;

  /** The screen layout of the most recently received configuration. */
  private int lastScreenLayout;

  /** The bold text style of the most recently received configuration. */
  private int lastFontWeightAdjustment;

  private final List<OnConfigurationChangedListener> listeners;

  @SuppressLint("NewApi")
  public DeviceConfigurationMonitor(Compositor compositor, Context context) {
    this.compositor = compositor;
    powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    listeners = new ArrayList<>();
    lastOrientation = context.getResources().getConfiguration().orientation;
    if (FeatureSupport.supportBoldFont()) {
      lastFontWeightAdjustment = context.getResources().getConfiguration().fontWeightAdjustment;
    }
    lastScreenLayout = context.getResources().getConfiguration().screenLayout;
  }

  public void addConfigurationChangedListener(OnConfigurationChangedListener listener) {
    listeners.add(listener);
  }

  private void notifyOnConfigurationChanged(Configuration newConfig) {
    for (OnConfigurationChangedListener listener : listeners) {
      listener.onConfigurationChanged(newConfig);
    }
  }

  /**
   * Called by {@link com.google.android.accessibility.talkback.TalkBackService} when the
   * configuration changes.
   *
   * @param newConfig The new configuration.
   */
  @SuppressLint("NewApi")
  public void onConfigurationChanged(Configuration newConfig) {
    boolean isBoldFontStyleChanged = false;
    boolean isScreenLayoutChanged = lastScreenLayout != newConfig.screenLayout;
    final int orientation = newConfig.orientation;
    boolean isOrientationChanged = orientation != lastOrientation;

    if (FeatureSupport.supportBoldFont()) {
      isBoldFontStyleChanged = lastFontWeightAdjustment != newConfig.fontWeightAdjustment;
      if (isBoldFontStyleChanged) {
        lastFontWeightAdjustment = newConfig.fontWeightAdjustment;
      }
    }

    // For foldable devices that open or fold.
    if (isScreenLayoutChanged) {
      lastScreenLayout = newConfig.screenLayout;
      // Dismiss TalkBack menu
      if (menuManager != null && menuManager.isMenuShowing()) {
        menuManager.dismissAll();
      }
    }

    if (!(isOrientationChanged || isBoldFontStyleChanged || isScreenLayoutChanged)) {
      return;
    }

    lastOrientation = orientation;
    EventId eventId = EVENT_ID_UNTRACKED;
    if (isOrientationChanged) {
      eventId = Performance.getInstance().onRotateEventReceived(orientation);
    }

    notifyOnConfigurationChanged(newConfig);

    if (!isOrientationChanged) {
      return;
    }
    // noinspection deprecation
    if (!powerManager.isScreenOn()) {
      // Don't announce rotation when the screen is off.
      Performance.getInstance().onHandlerDone(eventId);
      return;
    }

    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      compositor.handleEvent(Compositor.EVENT_ORIENTATION_PORTRAIT, eventId);
    } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      compositor.handleEvent(Compositor.EVENT_ORIENTATION_LANDSCAPE, eventId);

      // Recreates the dialog when device rotates to ensure the keyboard can be shown in the
      // fullscreen overlay above the dialog in landscape mode.
      if (dialog != null && dialog.isShowing()) {
        dialog.cancel();
        dialog.show();
      }
    }
    if (menuManager != null && menuManager.isMenuShowing()) {
      menuManager.dismissForOrientationChange();
    }

    Performance.getInstance().onHandlerDone(eventId);
  }

  /** Sets the dialog which will be recreated when device rotates. */
  public void setDialogWithEditText(A11yAlertDialogWrapper dialog) {
    this.dialog = dialog;
  }

  public void setMenuManager(ListMenuManager menuManager) {
    this.menuManager = menuManager;
  }
}
