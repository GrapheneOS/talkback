package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;

import android.app.Notification;
import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.utils.AccessibilityEventUtils;

/**
 * A class parses notification events.
 *
 * <p>This object gets the data of a notification type accessibility event, including the title,
 * category, and content as {@link String}.
 */
public class NotificationParser extends Parser {
  private Notification notification;

  public NotificationParser(Context context) {
    super(context);
  }

  @Override
  void init(AccessibilityEvent event) {
    this.notification = AccessibilityEventUtils.extractNotification(event);
  }

  @Override
  String getTitle() {
    return getNotificationAttribute("android.title");
  }

  @Override
  String getContent() {
    if (TextUtils.isEmpty(getCategory())) {
      return "";
    }
    return getNotificationAttribute("android.text");
  }

  @Override
  String getCategory() {
    if (notification == null || notification.category == null) {
      return "";
    }
    return switch (notification.category) {
      case Notification.CATEGORY_CALL -> context.getString(R.string.bd_notification_category_call);
      case Notification.CATEGORY_MESSAGE ->
          context.getString(R.string.bd_notification_category_msg);
      case Notification.CATEGORY_EMAIL ->
          context.getString(R.string.bd_notification_category_email);
      case Notification.CATEGORY_EVENT ->
          context.getString(R.string.bd_notification_category_event);
      case Notification.CATEGORY_PROMO ->
          context.getString(R.string.bd_notification_category_promo);
      case Notification.CATEGORY_ALARM ->
          context.getString(R.string.bd_notification_category_alarm);
      case Notification.CATEGORY_PROGRESS ->
          context.getString(R.string.bd_notification_category_progress);
      case Notification.CATEGORY_SOCIAL -> context.getString(R.string.bd_notification_cat_social);
      case Notification.CATEGORY_ERROR -> context.getString(R.string.bd_notification_category_err);
      case Notification.CATEGORY_SYSTEM -> context.getString(R.string.bd_notification_category_sys);
      case Notification.CATEGORY_SERVICE ->
          context.getString(R.string.bd_notification_category_service);
      default -> "";
    };
  }

  private String getNotificationAttribute(String attributeTag) {
    if (notification == null) {
      return "";
    }
    CharSequence notificationArttribute = null;
    if (notification.extras != null) {
      // Get notification arttribute from the Notification Extras bundle.
      notificationArttribute = notification.extras.getCharSequence(attributeTag);
    }
    return (notificationArttribute == null) ? "" : notificationArttribute.toString();
  }
}
