package com.google.android.accessibility.brailleime.dialog;

import android.app.Dialog;

/** Common dialog for Braille IME. */
public abstract class CommonDialog {
  private Dialog dialog;

  /** Shows the dialog attach on the specific View. */
  public void show() {
    dialog = makeDialog();
    dialog.show();
  }

  /** Gets the dialog is showing or not. */
  public boolean isShowing() {
    return dialog != null && dialog.isShowing();
  }

  /** Dismisses the dialog. */
  public void dismiss() {
    if (dialog != null) {
      dialog.dismiss();
    }
  }

  protected abstract Dialog makeDialog();
}
