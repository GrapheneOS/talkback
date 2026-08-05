package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;

import android.content.Context;
import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.utils.AccessibilityEventUtils;

/**
 * A class parses announcement events.
 *
 * <p>This object gets the data of an announcement type accessibility event, including the title,
 * category, and content as {@link String}.
 */
public class DefaultParser extends Parser {

  public DefaultParser(Context context) {
    super(context);
  }

  @Override
  void init(AccessibilityEvent event) {}

  @Override
  String getTitle() {
    return "";
  }

  @Override
  String getContent() {
    return getEventContentDescriptionOrEventAggregateText(event);
  }

  @Override
  String getCategory() {
    return context.getString(R.string.bd_announcement_category);
  }

  private String getEventContentDescriptionOrEventAggregateText(AccessibilityEvent event) {
    CharSequence contentDescription = event.getContentDescription();
    CharSequence aggregateText = AccessibilityEventUtils.getEventAggregateText(event);
    if (contentDescription != null) {
      return contentDescription.toString();
    } else if (aggregateText != null) {
      return aggregateText.toString();
    } else {
      return "";
    }
  }
}
