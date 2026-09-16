package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;
import com.google.android.accessibility.braille.brailledisplay.R;
import java.util.List;

/** A list adapter shows the popup message history. */
public class HistoryListAdapter extends ArrayAdapter<PopUpMessageInfo> {

  private final LayoutInflater inflater;
  private final int resource;

  public HistoryListAdapter(
      Context context, int resource, @NonNull List<PopUpMessageInfo> objects) {
    super(context, resource, objects);
    this.inflater = LayoutInflater.from(context);
    this.resource = resource;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    if (convertView == null) {
      convertView = inflater.inflate(resource, parent, /* attachToRoot= */ false);
    }
    TextView itemTextView = convertView.findViewById(R.id.history_list_text);
    PopUpMessageInfo item = getItem(position);
    if (item != null) {
      SpannableStringBuilder text = item.getRecordOutput();
      itemTextView.setText(text);
    }
    return convertView;
  }
}
