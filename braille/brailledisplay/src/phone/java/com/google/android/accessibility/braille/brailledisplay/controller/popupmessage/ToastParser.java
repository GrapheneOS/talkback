package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;

import android.content.Context;
import com.google.android.accessibility.braille.brailledisplay.R;

/**
 * A class parses toasts events.
 *
 * <p>This object gets the data of a toast, including the title, category, and content as {@link
 * String}.
 */
public class ToastParser extends DefaultParser {

  public ToastParser(Context context) {
    super(context);
  }

  @Override
  String getCategory() {
    return context.getString(R.string.bd_toast_category);
  }
}
