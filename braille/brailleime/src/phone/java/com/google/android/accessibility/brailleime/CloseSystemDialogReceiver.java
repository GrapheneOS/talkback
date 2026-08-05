package com.google.android.accessibility.brailleime;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.accessibility.braille.common.lib.ActionReceiver;

/** A receiver that receives the ACTION_CLOSE_SYSTEM_DIALOGS intent to close the system dialogs. */
public class CloseSystemDialogReceiver
    extends ActionReceiver<CloseSystemDialogReceiver, CloseSystemDialogReceiver.Callback> {
  private static final String SYSTEM_DIALOG_REASON_KEY = "reason";
  private static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";
  private static final String SYSTEM_DIALOG_REASON_HOME_KEY = "homekey";
  private static final String SYSTEM_DIALOG_REASON_VOICE_INTERACTION = "voiceinteraction";

  /** The callback associated with the actions of this receiver. */
  public interface Callback {
    void onSystemDialogClosed();
  }

  public CloseSystemDialogReceiver(Context context, Callback callback) {
    super(context, callback);
  }

  @Override
  protected void onReceive(Callback callback, String action, Bundle extras) {
    if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
      String reason = extras.getString(SYSTEM_DIALOG_REASON_KEY);
      if (reason != null) {
        if (reason.equals(SYSTEM_DIALOG_REASON_HOME_KEY)
            || reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)
            || reason.equals(SYSTEM_DIALOG_REASON_VOICE_INTERACTION)) {
          callback.onSystemDialogClosed();
        }
      }
    }
  }

  @Override
  protected String[] getActionsList() {
    return new String[] {Intent.ACTION_CLOSE_SYSTEM_DIALOGS};
  }
}
