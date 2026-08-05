package com.google.android.accessibility.braille.brailledisplay.controller.popupmessage;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface.OnClickListener;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import android.view.WindowManager;
import android.widget.ListView;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorDisplayer;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A class stores a history of pop-up messages, retaining up to 25 records within the past hour. */
public class PopUpHistory {
  private static final int POPUP_HISTORY_SIZE = 25;
  private static final Instant ONE_HOUR = Instant.ofEpochSecond(60 * 60);
  private final Context context;
  private final BehaviorDisplayer behaviorDisplayer;
  private final List<PopUpMessageInfo> announcementHistoryList;
  private Dialog historyDialog;

  public PopUpHistory(Context context, BehaviorDisplayer behaviorDisplayer) {
    this.context = context;
    this.behaviorDisplayer = behaviorDisplayer;
    this.announcementHistoryList = new ArrayList<>();
  }

  /** Add the new pop up message to the history list. */
  public void addMessage(PopUpMessageInfo info) {
    if (info != null && !TextUtils.isEmpty(info.output())) {
      announcementHistoryList.add(info);
      if (announcementHistoryList.size() > POPUP_HISTORY_SIZE) {
        announcementHistoryList.remove(0);
      }
    }
  }

  /**
   * If the history is empty, display 'Empty' in a timed message. Otherwise, display the history as
   * a list in a dialog."
   */
  public void showHistory() {
    refreshAnnouncementHistoryList();
    if (announcementHistoryList.isEmpty()) {
      behaviorDisplayer.displayTimedMessage(
          context.getString(R.string.popup_message_history_empty));
      return;
    }
    if (historyDialog != null) {
      historyDialog.dismiss();
    }
    List<PopUpMessageInfo> data = new ArrayList<>(announcementHistoryList);
    // data are presented from recent ones to older ones
    Collections.reverse(data);
    historyDialog =
        createListAlertDialog(context, data, (dialog, which) -> announcementHistoryList.clear());
    historyDialog.show();
  }

  private void refreshAnnouncementHistoryList() {
    for (int i = announcementHistoryList.size() - 1; i >= 0; i--) {
      if (Instant.now().minusMillis(announcementHistoryList.get(i).timestamp()).isAfter(ONE_HOUR)) {
        announcementHistoryList.remove(i);
      }
    }
  }

  private Dialog createListAlertDialog(
      Context context, List<PopUpMessageInfo> list, @Nullable OnClickListener listener) {
    ListView listView = new ListView(context);
    listView.setAdapter(new HistoryListAdapter(context, R.layout.list_item_popup_history, list));
    Dialog dialog =
        A11yAlertDialogWrapper.materialDialogBuilder(context)
            .setTitle(R.string.popup_message_history_title)
            .setView(listView)
            .setPositiveButton(R.string.popup_message_history_clear, listener)
            .setNegativeButton(R.string.bd_bt_cancel_button, null)
            .create()
            .getDialog();
    dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
    return dialog;
  }
}
