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
package com.google.android.accessibility.talkback.selector;

import static com.google.android.accessibility.talkback.selector.SelectorController.Setting.GRANULARITY_ROW_COLUMN;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.ActorState;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.selector.SelectorController.ContextualSetting;
import com.google.android.accessibility.talkback.selector.SelectorController.Setting;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.monitor.CollectionState;

/**
 * Contextual setting for Row & Column granularity. Row & Column granularity needs the rule because
 * it overrides the left and right gestures, it should automatically leave the granularity and
 * restore the gestures when out of the table.
 */
public class RowColumnGranularity implements ContextualSetting {
  private final CollectionState collectionState;
  private final ActorState actorState;

  public RowColumnGranularity(CollectionState collectionState, ActorState actorState) {
    this.collectionState = collectionState;
    this.actorState = actorState;
  }

  @Override
  public Setting getSetting() {
    return GRANULARITY_ROW_COLUMN;
  }

  @Override
  public boolean isNodeSupportSetting(Context context, AccessibilityNodeInfoCompat node) {
    // Leaves the setting automatically if not supported.
    return actorState.getDirectionNavigation().hasNavigableTableContent();
  }

  @Override
  public boolean shouldActivateSetting(Context context, AccessibilityNodeInfoCompat node) {
    // This is controlled by the preference.
    SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(context);
    return FeatureFlagReader.enableRowAndColumnOneGranularity(context)
        && collectionState.getCollectionTransition() == CollectionState.NAVIGATE_ENTER
        && prefs.getBoolean(
            context.getString(R.string.pref_auto_enter_table_navigation_key),
            context.getResources().getBoolean(R.bool.pref_auto_enter_table_navigation_default));
  }
}
