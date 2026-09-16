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

package com.google.android.accessibility.talkback.keyboard;

import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.IntDef;
import com.google.android.accessibility.talkback.R;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * The adapter for listing keyboard categories, keyboard commands with their shortcuts and dividers.
 */
public class KeyboardShortcutsRecyclerItemAdapter
    extends RecyclerView.Adapter<KeyboardShortcutsRecyclerItemAdapter.ViewHolder> {
  /** The view type of item for a command name and its keyboard shortcut. */
  public static final int COMMAND_ITEM_TYPE = 0;

  /** The view type of item which is a category. */
  public static final int CATEGORY_ITEM_TYPE = 1;

  /** Defines types of items in the list. */
  @IntDef({
    COMMAND_ITEM_TYPE,
    CATEGORY_ITEM_TYPE,
  })
  @Retention(RetentionPolicy.SOURCE)
  private @interface ItemType {}

  private List<ItemData> itemDataList;

  /** The data model for an item shown in the list of keyboard commands. */
  public static class ItemData {
    @ItemType private final int viewType;
    private final CharSequence title; // either category name or command name
    private String keyComboString;

    public ItemData(CharSequence title) {
      this.viewType = CATEGORY_ITEM_TYPE;
      this.title = title;
    }

    public ItemData(CharSequence title, String keyComboString) {
      this.viewType = COMMAND_ITEM_TYPE;
      this.title = title;
      this.keyComboString = keyComboString;
    }

    @ItemType
    public int getViewType() {
      return viewType;
    }

    public CharSequence getTitle() {
      return title;
    }

    public String getKeyComboString() {
      return keyComboString;
    }
  }

  /**
   * The view holder for holding a category name with a divider above it or a command item which is
   * constructed by a command name and a keyboard shortcut.
   */
  public static class ViewHolder extends RecyclerView.ViewHolder {
    private final LinearLayout categoryContainer;
    private final View divider;
    private final TextView categoryTitle;
    private final LinearLayout keyboardShortcutContainer;
    private final TextView keyboardShortcutTitle;
    private final TextView keyboardShortcutSubtitle;

    @ItemType int viewType;

    public ViewHolder(View itemView, @ItemType int viewType) {
      super(itemView);
      this.viewType = viewType;
      keyboardShortcutContainer = itemView.findViewById(R.id.keyboard_shortcut_container);
      keyboardShortcutTitle = itemView.findViewById(R.id.keyboard_shortcut_title);
      keyboardShortcutSubtitle = itemView.findViewById(R.id.keyboard_shortcut_subtitle);
      categoryContainer = itemView.findViewById(R.id.category_container);
      categoryTitle = itemView.findViewById(R.id.category_title);
      divider = itemView.findViewById(R.id.divider);
    }

    public void bindViewHolderWithData(ItemData itemData, final int position) {
      viewType = itemData.getViewType();

      if (viewType == COMMAND_ITEM_TYPE) {
        keyboardShortcutContainer.setVisibility(View.VISIBLE);
        categoryContainer.setVisibility(View.GONE);

        keyboardShortcutTitle.setText(itemData.getTitle());
        keyboardShortcutSubtitle.setText(itemData.getKeyComboString());
      } else {
        keyboardShortcutContainer.setVisibility(View.GONE);
        categoryContainer.setVisibility(View.VISIBLE);

        categoryTitle.setText(itemData.getTitle());
        divider.setVisibility(position > 0 ? View.VISIBLE : View.GONE);
      }
    }
  }

  public KeyboardShortcutsRecyclerItemAdapter(List<ItemData> itemDataList) {
    this.itemDataList = itemDataList;
  }

  @Override
  public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
    View view =
        LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.keyboard_shortcuts_recycler_item, viewGroup, false);
    return new ViewHolder(view, viewType);
  }

  @Override
  public void onBindViewHolder(ViewHolder viewHolder, final int position) {
    viewHolder.bindViewHolderWithData(itemDataList.get(position), position);
  }

  @Override
  public int getItemCount() {
    return itemDataList.size();
  }

  public void setItemDataList(List<ItemData> itemDataList) {
    this.itemDataList = itemDataList;
    notifyDataSetChanged();
  }
}
