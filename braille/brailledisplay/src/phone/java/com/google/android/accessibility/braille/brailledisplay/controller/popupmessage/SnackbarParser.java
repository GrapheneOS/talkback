package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;


import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;

/**
 * A class parses snackbar events.
 *
 * <p>This object retrieves alert data, such as snackbar messages, including the title, category,
 * and content as strings.
 */
public class SnackbarParser extends Parser {

  public SnackbarParser(Context context) {
    super(context);
  }

  /** Returns whether the node is a non-modal alert. */
  public static boolean isAlert(AccessibilityNodeInfoCompat node) {
    return node != null
        && node.getPaneTitle() != null
        && node.getPaneTitle().toString().equals("Alert");
  }

  @Override
  void init(AccessibilityEvent event) {}

  @Override
  public String getOutput() {
    String content = getContent();
    if (TextUtils.isEmpty(content)) {
      return "";
    }
    return String.join(SEPARATOR_PERIOD, "[" + getCategory() + "]", getContent());
  }

  @Override
  String getTitle() {
    return "";
  }

  /** Returns the string with the format "[snackbar]. XXX." */
  @Override
  String getContent() {
    StringBuilder stringBuilder = new StringBuilder();
    AccessibilityNodeInfoCompat node = AccessibilityEventUtils.sourceCompat(event);
    if (node != null) {
      for (int i = 0; i < node.getChildCount(); i++) {
        AccessibilityNodeInfoCompat child = node.getChild(i);
        if (child != null) {
          stringBuilder.append(AccessibilityNodeInfoUtils.getText(child));
        }
      }
    }
    return stringBuilder.toString();
  }

  @Override
  String getCategory() {
    return context.getString(R.string.bd_non_modal_alert_category);
  }
}
