package com.google.android.accessibility.utils.material;

import android.os.Build;
import android.view.WindowManager.BadTokenException;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/** An utils that handles alert dialog's operation. */
public class AlertDialogUtils {

  public static final String TAG = "DialogUtils";

  private static final boolean IS_DEBUGGABLE =
      Build.TYPE.equals("eng") || Build.TYPE.equals("userdebug");

  @CanIgnoreReturnValue
  public static boolean safeShow(A11yAlertDialogWrapper dialogWrapper) {
    try {
      dialogWrapper.show();
    } catch (BadTokenException e) {
      LogUtils.e(TAG, e, "BadTokenException is detected.");
      if (IS_DEBUGGABLE) {
        throw e;
      }
      return false;
    }
    return true;
  }

  private AlertDialogUtils() {}
}
