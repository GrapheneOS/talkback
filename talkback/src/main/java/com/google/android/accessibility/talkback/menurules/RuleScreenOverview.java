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

package com.google.android.accessibility.talkback.menurules;

import static com.google.android.accessibility.talkback.contextmenu.TalkbackMenuProcessor.ORDER_SUMMARIZE_VIEW;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.actor.gemini.GeminiFunctionUtils;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.contextmenu.AbstractOnContextMenuItemClickListener;
import com.google.android.accessibility.talkback.contextmenu.ContextMenu;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem.DeferredType;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.SettingsUtils;
import com.google.android.accessibility.utils.monitor.ScreenMonitor;
import java.util.ArrayList;
import java.util.List;

/** Performs screen overview from menu. */
public class RuleScreenOverview extends NodeMenuRule {

  private final Pipeline.FeedbackReturner pipeline;
  private final TalkBackAnalytics analytics;

  public RuleScreenOverview(Pipeline.FeedbackReturner pipeline, TalkBackAnalytics analytics) {
    super(
        R.string.pref_show_context_menu_summarize_view_setting_key,
        R.bool.pref_show_context_menu_summarize_view_default);
    this.pipeline = pipeline;
    this.analytics = analytics;
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return !ScreenMonitor.isDeviceLocked(context)
        && SettingsUtils.allowLinksOutOfSettings(context)
        && FeatureSupport.canTakeScreenShotByAccessibilityService()
        && GeminiConfiguration.screenOverviewEnabled(context);
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {

    List<ContextMenuItem> items = new ArrayList<>();

    final ScreenOverviewMenuItemOnClickListener menuItemOnClickListener =
        new ScreenOverviewMenuItemOnClickListener(context, node, pipeline, analytics);

    ContextMenuItem item =
        ContextMenu.createMenuItem(
            context,
            Menu.NONE,
            R.id.summarize_view_menu,
            ORDER_SUMMARIZE_VIEW,
            context.getString(R.string.title_summarize_view));
    item.setOnMenuItemClickListener(menuItemOnClickListener);
    item.setSkipRefocusEvents(true);
    item.setSkipWindowEvents(true);
    item.setDeferredType(DeferredType.WINDOWS_STABLE);
    items.add(item);

    return items;
  }

  @Override
  CharSequence getUserFriendlyMenuName(Context context) {
    return context.getString(R.string.title_summarize_view);
  }

  @Override
  boolean isSubMenu() {
    return false;
  }

  private static class ScreenOverviewMenuItemOnClickListener
      extends AbstractOnContextMenuItemClickListener {

    private final Context context;

    public ScreenOverviewMenuItemOnClickListener(
        Context context,
        AccessibilityNodeInfoCompat node,
        Pipeline.FeedbackReturner pipeline,
        TalkBackAnalytics analytics) {
      super(node, pipeline, analytics);
      this.context = context;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
      Feedback.Part.Builder feedback =
          GeminiFunctionUtils.getPreferredScreenOverviewFeedback(context, node);
      return feedback != null && pipeline.returnFeedback(EVENT_ID_UNTRACKED, feedback);
    }
  }
}
