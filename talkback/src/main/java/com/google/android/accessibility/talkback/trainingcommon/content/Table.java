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
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.GridLayout;
import android.widget.GridView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;
import com.google.common.collect.ImmutableList;

/** A {@link GridLayout} to show a table. */
public class Table extends PageContentConfig {

  private final int numColumns;

  /** A list of text resource ID of items in the table. */
  private final ImmutableList<Integer> textResIds;

  public Table(int numColumns, ImmutableList<Integer> textResIds) {
    this.numColumns = numColumns;
    this.textResIds = textResIds;
  }

  @Override
  public View createView(
      LayoutInflater inflater, ViewGroup container, Context context, ServiceData data) {
    View view = inflater.inflate(R.layout.training_table, container, false);
    GridView table = view.findViewById(R.id.training_grid_view);
    table.setLayoutParams(
        new AbsListView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            context.getResources().getDimensionPixelSize(R.dimen.training_table_cell_height)
                * (textResIds.size() / numColumns)));
    table.setAdapter(new GridViewAdapter(context, textResIds));
    table.setNumColumns(numColumns);
    return view;
  }

  /** A {@link BaseAdapter} that provides text for views that are displayed within a GridView. */
  private static final class GridViewAdapter extends BaseAdapter {
    private final Context context;
    private final ImmutableList<Integer> textResIds;
    private final int count;

    public GridViewAdapter(Context context, ImmutableList<Integer> textResIds) {
      this.context = context;
      this.textResIds = textResIds;
      this.count = textResIds.size();
    }

    @Override
    public int getCount() {
      return count;
    }

    @Nullable
    @Override
    public Object getItem(int i) {
      return null;
    }

    @Override
    public long getItemId(int i) {
      return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
      TextView textView = new TextView(context);
      textView.setText(textResIds.get(i));
      textView.setTextSize(
          TypedValue.COMPLEX_UNIT_PX,
          context.getResources().getDimension(R.dimen.training_text_size));
      textView.setTextColor(context.getColor(R.color.training_text_color));
      textView.setBackground(context.getDrawable(R.drawable.training_table_cell_border));
      textView.setLayoutParams(
          new AbsListView.LayoutParams(
              ViewGroup.LayoutParams.MATCH_PARENT,
              context.getResources().getDimensionPixelSize(R.dimen.training_table_cell_height)));
      textView.setPadding(
          context.getResources().getDimensionPixelSize(R.dimen.training_list_item_margin_start),
          /* top= */ 0,
          /* right= */ 0,
          /* bottom= */ 0);
      textView.setEllipsize(TruncateAt.END);
      textView.setSingleLine();
      return textView;
    }
  }
}
