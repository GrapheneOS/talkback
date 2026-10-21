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
package com.google.android.accessibility.talkback.trainingcommon.content;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/** A {@link TabLayout} that shows different images in each tab. */
public class Tabs extends PageContentConfig {

  private final int[] tabNameResIds;
  private final int[] imageInTabResources;
  private final int[] imageInTabContentDescriptionResIds;

  public Tabs(
      int[] tabNameResIds, int[] imageInTabResources, int[] imageInTabContentDescriptionResIds) {
    this.tabNameResIds = tabNameResIds;
    this.imageInTabResources = imageInTabResources;
    this.imageInTabContentDescriptionResIds = imageInTabContentDescriptionResIds;
  }

  @Override
  public View createView(
      LayoutInflater inflater, ViewGroup container, Context context, ServiceData data) {
    final View view = inflater.inflate(R.layout.training_tabs, container, false);
    ViewPager2 tabPager = view.findViewById(R.id.training_tab_pager);
    tabPager.setAdapter(
        new TabAdapter(context, imageInTabResources, imageInTabContentDescriptionResIds));
    new TabLayoutMediator(
            view.findViewById(R.id.training_tabs),
            tabPager,
            (tab, position) -> {
              View tabView =
                  LayoutInflater.from(context)
                      .inflate(R.layout.training_tab_item, container, false);
              TextView tabText = tabView.findViewById(R.id.training_tab_text);
              tabText.setText(tabNameResIds[position]);
              tab.setCustomView(tabView);
            })
        .attach();
    return view;
  }

  private static class TabAdapter extends Adapter<TabAdapter.ViewHolder> {

    private final Context context;
    private final int[] imageInTabResources;
    private final int[] imageInTabContentDescriptionResIds;

    private TabAdapter(
        Context context, int[] imageInTabResources, int[] imageInTabContentDescriptionResIds) {
      this.context = context;
      this.imageInTabResources = imageInTabResources;
      this.imageInTabContentDescriptionResIds = imageInTabContentDescriptionResIds;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int position) {
      return new ViewHolder(
          LayoutInflater.from(context).inflate(R.layout.training_tab_page, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
      ImageView imageView = viewHolder.getImageView();
      imageView.setImageResource(imageInTabResources[position]);
      imageView.setContentDescription(
          context.getString(imageInTabContentDescriptionResIds[position]));
    }

    @Override
    public int getItemCount() {
      return imageInTabResources.length;
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
      private final ImageView imageView;

      public ViewHolder(@NonNull View itemView) {
        super(itemView);
        imageView = itemView.findViewById(R.id.tab_page_image);
      }

      private ImageView getImageView() {
        return imageView;
      }
    }
  }
}
