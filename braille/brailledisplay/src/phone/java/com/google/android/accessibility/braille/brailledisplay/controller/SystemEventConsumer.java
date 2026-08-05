package com.google.android.accessibility.braille.brailledisplay.controller;

import static android.view.accessibility.AccessibilityEvent.TYPE_ANNOUNCEMENT;
import static android.view.accessibility.AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED;
import static android.view.accessibility.AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityEvent;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorDisplayer;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.DefaultParser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.NotificationParser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.Parser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.PopUpHistory;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.PopUpMessageInfo;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.SnackbarParser;
import com.google.android.accessibility.braille.brailledisplay.controller.popupmessage.ToastParser;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.Role;

/**
 * An event-consumer handles events from the phone.
 *
 * <p>Devices generates events such as notifications, toasts, and phone calls. This consumer would
 * handle those actions that are automatically triggered by specific events , and also ensuring that
 * users are reminded.
 */
public class SystemEventConsumer implements EventConsumer {
  private static final String TAG = "SystemEventConsumer";
  private static final int SKIP_EVENT_MS = 200;
  private final Context context;
  private final BehaviorDisplayer behaviorDisplayer;
  private final CellsContentConsumer cellsContentConsumer;
  private final DefaultParser defaultParser;
  private final ToastParser toastParser;
  private final NotificationParser notificationParser;
  private final SnackbarParser snackbarParser;
  private final PopUpHistory popUpHistory;
  private PopUpMessageInfo cachePopupMessageInfo;

  public SystemEventConsumer(
      Context context,
      BehaviorDisplayer behaviorDisplayer,
      CellsContentConsumer cellsContentConsumer,
      PopUpHistory popUpHistory) {
    this.context = context;
    this.behaviorDisplayer = behaviorDisplayer;
    this.cellsContentConsumer = cellsContentConsumer;
    this.defaultParser = new DefaultParser(context);
    this.toastParser = new ToastParser(context);
    this.snackbarParser = new SnackbarParser(context);
    this.notificationParser = new NotificationParser(context);
    this.popUpHistory = popUpHistory;
  }

  @Override
  public void onActivate() {}

  @Override
  public void onDeactivate() {}

  @Override
  public boolean onMappedInputEvent(BrailleInputEvent event) {
    BrailleDisplayLog.v(TAG, "onMappedInputEvent: " + event.getCommand());
    return false;
  }

  @Override
  @SuppressLint("SwitchIntDef") // pre-existing logic
  public void onAccessibilityEvent(AccessibilityEvent event) {
    BrailleDisplayLog.v(TAG, "onAccessibilityEvent: " + event.getEventType());
    Parser parser;
    switch (event.getEventType()) {
      case TYPE_WINDOW_CONTENT_CHANGED -> {
        if (SnackbarParser.isAlert(AccessibilityEventUtils.sourceCompat(event))) {
          parser = snackbarParser;
        } else {
          return;
        }
      }
      case TYPE_NOTIFICATION_STATE_CHANGED ->
          parser = isToast(event) ? toastParser : notificationParser;
      case TYPE_ANNOUNCEMENT -> parser = defaultParser;
      default -> {
        // do not parse and display
        return;
      }
    }
    PopUpMessageInfo info = parser.parse(event);
    if (!TextUtils.isEmpty(info.output()) && !skip(info)) {
      cachePopupMessageInfo = info;
      // show the notification text output on braille display
      displayPopMessage(info.output());
      // Add the new notificationNode to notificationHistoryList
      if (BrailleUserPreferences.readPopupMessageHistoryEnabled(context)) {
        popUpHistory.addMessage(info);
      }
    }
  }

  private boolean skip(PopUpMessageInfo info) {
    if (cachePopupMessageInfo == null || !cachePopupMessageInfo.output().equals(info.output())) {
      return false;
    }
    return info.eventTime() - cachePopupMessageInfo.eventTime() < SKIP_EVENT_MS;
  }

  private boolean isToast(AccessibilityEvent event) {
    return (Role.getSourceRole(event) == Role.ROLE_TOAST);
  }

  private void displayPopMessage(String popMessage) {
    if (behaviorDisplayer.isBrailleDisplayConnected()) {
      cellsContentConsumer.setTimedContent(
          TimedMessager.Type.ANNOUNCEMENT,
          new CellsContent(popMessage),
          TimedMessager.UNLIMITED_DURATION);
    }
  }
}
