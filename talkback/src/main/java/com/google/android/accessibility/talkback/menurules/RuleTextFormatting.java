/*
 * Copyright (C) 2025 Google Inc.
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

import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.MENU_TYPE_TEXT_FORMATTING;
import static com.google.android.accessibility.talkback.contextmenu.TalkbackMenuProcessor.ORDER_TEXT_FORMATTING;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.analytics.TalkBackAnalytics;
import com.google.android.accessibility.talkback.contextmenu.ContextMenu;
import com.google.android.accessibility.talkback.contextmenu.ContextMenuItem;
import com.google.android.accessibility.talkback.contextmenu.OnContextMenuItemClickListener;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.utils.SpannableUtils.SpannableWithOffset;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.output.TextFormattingUtils;
import com.google.android.accessibility.utils.traversal.SpannableTraversalUtils;
import java.util.ArrayList;
import java.util.List;

/** Reads text formatting from menu. */
public class RuleTextFormatting extends NodeMenuRule {
  private static final String TAG = "RuleTextFormatting";
  private final Pipeline.FeedbackReturner pipeline;
  private final TalkBackAnalytics analytics;

  public RuleTextFormatting(Pipeline.FeedbackReturner pipeline, TalkBackAnalytics analytics) {
    super(
        R.string.pref_show_context_menu_text_formatting_setting_key,
        R.bool.pref_show_context_menu_text_formatting_default);
    this.pipeline = pipeline;
    this.analytics = analytics;
  }

  @Override
  public boolean accept(Context context, AccessibilityNodeInfoCompat node) {
    return FeatureFlagReader.enableTextFormattingSummary(context)
        && SpannableTraversalUtils.hasTargetCharcterStyleInNodeTree(
            node, TextFormattingUtils.getFormattingSpanClasses());
  }

  @Override
  public List<ContextMenuItem> getMenuItemsForNode(
      Context context, AccessibilityNodeInfoCompat node, boolean includeAncestors) {
    List<ContextMenuItem> menuList = new ArrayList<>();
    ContextMenuItem item =
        ContextMenu.createMenuItem(
            context,
            Menu.NONE,
            R.id.text_formatting,
            ORDER_TEXT_FORMATTING,
            context.getString(R.string.title_text_formatting_menu));
    item.setOnMenuItemClickListener(
        new TextFormattingMenuItemClickListener(context, node, analytics, pipeline));
    item.setSkipRefocusEvents(true);
    item.setSkipWindowEvents(true);
    menuList.add(item);

    return menuList;
  }

  @Override
  CharSequence getUserFriendlyMenuName(Context context) {
    return context.getString(R.string.title_text_formatting_menu);
  }

  @Override
  boolean isSubMenu() {
    return false;
  }

  /** Click listener for speaking the formatting of the text. */
  private static class TextFormattingMenuItemClickListener
      implements OnContextMenuItemClickListener {
    final Context context;
    final AccessibilityNodeInfoCompat node;
    final TalkBackAnalytics analytics;
    final Pipeline.FeedbackReturner pipeline;

    public TextFormattingMenuItemClickListener(
        Context context,
        AccessibilityNodeInfoCompat node,
        TalkBackAnalytics analytics,
        Pipeline.FeedbackReturner pipeline) {
      this.context = context;
      this.node = node;
      this.analytics = analytics;
      this.pipeline = pipeline;
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
      if (describeTextFormatting(context, node, pipeline)) {
        analytics.onLocalContextMenuAction(MENU_TYPE_TEXT_FORMATTING, item.getItemId());
      }
      return true;
    }
  }

  /** Returns true if the text formatting description is spoken. */
  public static boolean describeTextFormatting(
      Context context, AccessibilityNodeInfoCompat node, Pipeline.FeedbackReturner pipeline) {
    final List<SpannableWithOffset> spannableStrings = new ArrayList<>();
    SpannableTraversalUtils.getSpannableStringsWithTargetCharacterStyleInNodeTree(
        node, TextFormattingUtils.getFormattingSpanClasses(), spannableStrings);
    CharSequence textFormattingString =
        TextFormattingUtils.getTextFormattingSummary(context, spannableStrings);
    if (TextUtils.isEmpty(textFormattingString)) {
      return false;
    }
    // speak the text formatting description.
    return pipeline.returnFeedback(
        EVENT_ID_UNTRACKED,
        Feedback.speech(
            textFormattingString,
            SpeakOptions.create()
                .setQueueMode(
                    SpeechController.QUEUE_MODE_INTERRUPT_AND_UNINTERRUPTIBLE_BY_NEW_SPEECH)));
  }
}
