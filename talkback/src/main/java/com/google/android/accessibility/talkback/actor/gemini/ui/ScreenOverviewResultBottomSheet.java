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

package com.google.android.accessibility.talkback.actor.gemini.ui;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

/** A class that owns the implementation of the screen overview result in a bottom sheet dialog. */
public final class ScreenOverviewResultBottomSheet {

  /**
   * Creates a bottom sheet dialog for the screen overview result.
   *
   * @param context The context of the dialog.
   * @param screenOverviewResult The result of screen overview.
   */
  public static BottomSheetDialog getBottomSheetDialog(
      Context context, String screenOverviewResult) {
    BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.ModalBottomSheetTheme);
    LayoutInflater layoutInflater =
        LayoutInflater.from(new ContextThemeWrapper(context, R.style.ModalBottomSheetTheme));
    LinearLayout dummyParent = new LinearLayout(context);
    CoordinatorLayout contentView =
        (CoordinatorLayout)
            layoutInflater.inflate(R.layout.screen_overview_bottomsheet_dialog, dummyParent, false);

    // View pager
    ViewPager2 viewPager = contentView.findViewById(R.id.screen_overview_view_pager);
    ScreenOverviewBottomSheetPagerAdapter pagerAdapter =
        new ScreenOverviewBottomSheetPagerAdapter(viewPager, screenOverviewResult);
    viewPager.setAdapter(pagerAdapter);

    FrameLayout standardBottomSheet = contentView.findViewById(R.id.standard_bottom_sheet);
    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(standardBottomSheet);
    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    behavior.setDraggable(false);

    // Set the max height for the bottom sheet.
    if (FeatureSupport.supportWindowMetrics()) {
      WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
      behavior.setMaxHeight(windowMetrics.getBounds().height() * 2 / 3);
    }

    // Drag handle for closing the dialog
    FrameLayout dragHandle = standardBottomSheet.findViewById(R.id.drag_handle);
    dragHandle.setOnClickListener(view -> dialog.dismiss());

    dialog.setContentView(contentView);
    dialog.setTitle(R.string.title_gemini_screen_overview_result_dialog);
    return dialog;
  }

  /**
   * Adapter for the image captioning result bottom sheet.
   *
   * <p>This adapter manages four different page types within the bottom sheet:
   *
   * <ol>
   *   <li>Image captioning result
   *   <li>Gemini listening request
   *   <li>Image Q&A with voice input
   *   <li>Image Q&A with typing input
   * </ol>
   */
  private static class ScreenOverviewBottomSheetPagerAdapter
      extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int SCREEN_OVERVIEW_SUMMARY = 0;
    static final int SCREEN_OVERVIEW_ACTIONS = 1;

    @NonNull final ViewPager2 viewPager;
    @Nullable final String screenOverviewResult;

    ScreenOverviewBottomSheetPagerAdapter(
        ViewPager2 viewPager, @Nullable String screenOverviewResult) {
      this.viewPager = viewPager;
      this.screenOverviewResult = screenOverviewResult;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
      View contentView;
      contentView =
          inflater.inflate(
              R.layout.screen_overview_summary_result_bottomsheet_dialog, viewGroup, false);
      return new ScreenOverviewResultViewHolder(contentView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
      // Page of the image captioning result.
      if (viewHolder instanceof ScreenOverviewResultViewHolder screenOverviewResultViewHolder) {
        screenOverviewResultViewHolder.setScreenOverviewResult(screenOverviewResult);
      } else if (viewHolder
          instanceof ScreenOverviewActionsResultViewHolder screenOverviewActionsResultViewHolder) {
        screenOverviewActionsResultViewHolder.setScreenOverviewResult("Template Only");
      }
    }

    @Override
    public int getItemCount() {
      return 1;
    }

    @Override
    public int getItemViewType(int position) {
      return switch (position) {
        case 1 -> SCREEN_OVERVIEW_ACTIONS;
        default -> SCREEN_OVERVIEW_SUMMARY;
      };
    }
  }

  /** The view holder for the page of the screen overview summary result. */
  private static class ScreenOverviewResultViewHolder extends RecyclerView.ViewHolder {
    final Context context;
    final TextView screenOverviewResultView;

    ScreenOverviewResultViewHolder(@NonNull View itemView) {
      super(itemView);
      context = itemView.getContext();
      screenOverviewResultView = itemView.findViewById(R.id.screen_overview_result_bottomsheet);
    }

    void setScreenOverviewResult(@Nullable String screenOverviewResult) {
      if (TextUtils.isEmpty(screenOverviewResult)) {
        screenOverviewResultView.setVisibility(View.GONE);
        return;
      }

      screenOverviewResultView.setVisibility(View.VISIBLE);
      screenOverviewResultView.setText(screenOverviewResult);
    }
  }

  /** The view holder for the page of the screen overview summary result. */
  private static class ScreenOverviewActionsResultViewHolder extends RecyclerView.ViewHolder {
    final Context context;
    final TextView screenOverviewResultView;

    ScreenOverviewActionsResultViewHolder(@NonNull View itemView) {
      super(itemView);
      context = itemView.getContext();
      screenOverviewResultView = itemView.findViewById(R.id.screen_overview_result_bottomsheet);
    }

    void setScreenOverviewResult(@Nullable String result) {
      if (TextUtils.isEmpty(result)) {
        screenOverviewResultView.setVisibility(View.GONE);
        return;
      }

      screenOverviewResultView.setVisibility(View.VISIBLE);
      screenOverviewResultView.setText(result);
    }
  }

  private ScreenOverviewResultBottomSheet() {}
}
