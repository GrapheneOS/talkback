/*
 * Copyright (C) 2020 Google Inc.
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

package com.google.android.accessibility.talkback.preference.base;

import static com.google.android.accessibility.talkback.preference.base.GestureListPreference.TYPE_ACTION_ITEM;
import static com.google.android.accessibility.talkback.preference.base.GestureListPreference.TYPE_TITLE;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.LayoutManager;
import androidx.recyclerview.widget.RecyclerView.Recycler;
import androidx.recyclerview.widget.RecyclerView.State;
import android.text.TextUtils;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;
import com.google.android.accessibility.material.preference.AccessibilitySuitePreferenceCategory;
import com.google.android.accessibility.material.preference.AccessibilitySuiteRadioButtonPreference;
import com.google.android.accessibility.talkback.preference.base.GestureListPreference.ActionItem;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A fragment contains a customized list view for TalkBack supported actions.
 *
 * <p>Note: This class is only for Phone.
 */
public class GesturePreferenceFragmentCompat extends TalkbackBaseFragment {
  private static final String TAG = "GesturePreferenceFragmentCompat";

  private static final String GESTURE_TITLE = "GESTURE_TITLE";
  private static final String GESTURE_INITIAL_VALUE = "GESTURE_INITIAL_VALUE";
  private static final String GESTURE_ACTION_ITEMS = "GESTURE_ACTION_ITEMS";

  private Parcelable[] items;
  private CharSequence title;
  private String initialValue;
  private String preferenceKey;

  private final OnPreferenceClickListener onPreferenceClickListener =
      new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {

          ActionItem item = (ActionItem) items[preference.getOrder()];
          boolean checked = TextUtils.equals(item.value, initialValue);
          if (checked) {
            // The selected action is clicked again and it is going to be unchecked. We should
            // return false to ignore it and don't set the value to targetGestureListPreference.
            popBackStack();
            return false;
          }

          SharedPreferences prefs = SharedPreferencesUtils.getSharedPreferences(getContext());
          prefs.edit().putString(preferenceKey, item.value).apply();
          popBackStack();
          return true;
        }
      };

  private int selectedPosition;

  /** Creates the fragment from given {@link GestureListPreference}. */
  @Nullable
  public static GesturePreferenceFragmentCompat create(GestureListPreference preference) {
    // Do nothing.
    return null;
  }

  public static Bundle createBundleForFragmentArguments(GestureListPreference preference) {
    Bundle bundle = new Bundle();
    bundle.putString(ARG_PREFERENCE_ROOT, preference.getKey());
    bundle.putCharSequence(GESTURE_TITLE, preference.getTitle());
    bundle.putString(GESTURE_INITIAL_VALUE, preference.getCurrentValue());
    bundle.putParcelableArray(GESTURE_ACTION_ITEMS, preference.getActionItems());
    return bundle;
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (savedInstanceState == null) {
      // We post a runnable to scroll to the selected position in the 1st time.
      scrollToSelectedPosition();
    }
  }

  @NonNull
  @Override
  public LayoutManager onCreateLayoutManager() {
    return new LinearLayoutManager(requireContext()) {
      @Override
      public int getSelectionModeForAccessibility(
          @NonNull Recycler recycler, @NonNull State state) {
        return CollectionInfoCompat.SELECTION_MODE_SINGLE;
      }
    };
  }

  /** Pops back the fragment and restores the a11y importance attribute for the parent fragment. */
  private void popBackStack() {
    FragmentActivity activity = getActivity();
    if (activity == null) {
      return;
    }
    FragmentManager fragmentManager = getParentFragmentManager();
    fragmentManager.popBackStackImmediate();
    if (fragmentManager.getBackStackEntryCount() == 0) {
      activity.finish();
    }
  }

  @Override
  protected CharSequence getTitle() {
    return title;
  }

  @Override
  public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
    Bundle bundle = getArguments();
    if (bundle == null) {
      throw new IllegalArgumentException();
    }
    title = bundle.getCharSequence(GESTURE_TITLE);
    initialValue = bundle.getString(GESTURE_INITIAL_VALUE);
    items = bundle.getParcelableArray(GESTURE_ACTION_ITEMS);
    preferenceKey = rootKey;

    PreferenceSettingsUtils.setPreferenceScreen(this, createPreferenceScreen());
  }

  private PreferenceScreen createPreferenceScreen() {
    PreferenceScreen preferenceScreen = getPreferenceManager().createPreferenceScreen(getContext());
    Context context = getContext();

    AccessibilitySuitePreferenceCategory category = null;
    ActionItem item;
    for (int order = 0; order < items.length; order++) {
      item = (ActionItem) items[order];
      switch (item.viewType) {
        case TYPE_TITLE -> {
          category = new AccessibilitySuitePreferenceCategory(getContext());
          category.setTitle(item.text);
          preferenceScreen.addPreference(category);
        }
        case TYPE_ACTION_ITEM -> {
          AccessibilitySuiteRadioButtonPreference radioButtonPreference =
              new AccessibilitySuiteRadioButtonPreference(context);
          radioButtonPreference.setTitle(item.text);
          boolean checked = TextUtils.equals(item.value, initialValue);
          radioButtonPreference.setChecked(checked);
          radioButtonPreference.setOnPreferenceClickListener(onPreferenceClickListener);
          radioButtonPreference.setOrder(order);
          radioButtonPreference.setSingleLineTitle(false);
          if (category == null) {
            // We create a category without title to add the beginning preference (e.g., "Tap to
            // assign").
            category = new AccessibilitySuitePreferenceCategory(getContext());
            preferenceScreen.addPreference(category);
          }
          radioButtonPreference.setKey(item.text);
          category.addPreference(radioButtonPreference);
          if (checked) {
            selectedPosition = order;
          }
          radioButtonPreference.setRequestInitialAccessibilityFocus(checked);
        }
        default -> {}
      }
    }

    return preferenceScreen;
  }

  private int getSelectedPositionInAdapter() {
    // In SettingsBasePreferenceFragment, it has an unknown item in the beginning for the
    // RecyclerView. So, we offset one position for the selected preference.
    return selectedPosition + 1;
  }

  private void scrollToSelectedPosition() {
    RecyclerView recyclerView = getListView();
    if (recyclerView == null) {
      LogUtils.w(TAG, "RecyclerView has been cleared.");
      return;
    }

    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
    if (linearLayoutManager != null) {
      linearLayoutManager.scrollToPositionWithOffset(getSelectedPositionInAdapter(), 0);
    }
  }
}
