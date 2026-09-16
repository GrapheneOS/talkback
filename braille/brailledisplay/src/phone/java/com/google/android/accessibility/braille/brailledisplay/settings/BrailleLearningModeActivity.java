package com.google.android.accessibility.braille.brailledisplay.settings;

import static com.google.android.accessibility.braille.common.BrailleUserPreferences.BRAILLE_SHARED_PREFS_FILENAME;

import android.os.Bundle;
import android.view.KeyEvent;
import androidx.preference.PreferenceFragmentCompat;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.platform.Connectioneer;
import com.google.android.accessibility.braille.brailledisplay.platform.Connectioneer.AspectDisplayer;
import com.google.android.accessibility.braille.brltty.BrailleDisplayProperties;
import com.google.android.accessibility.braille.common.BrailleCommonTalkBackSpeaker;
import com.google.android.accessibility.braille.common.TalkBackSpeaker.AnnounceType;
import com.google.android.accessibility.utils.PreferenceSettingsUtils;
import com.google.android.accessibility.utils.preference.PreferencesActivity;

/**
 * Shows the braille learning mode page. Overrides the default Braille Display commands and instead
 * reports the commands usage.
 */
public final class BrailleLearningModeActivity extends PreferencesActivity {
  @Override
  protected PreferenceFragmentCompat createPreferenceFragment() {
    return new BrailleLearningModeFragment();
  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (isExitCommand(keyCode, event)) {
      finish();
      return true;
    }
    return super.onKeyDown(keyCode, event);
  }

  /** Returns true if the key event should exit the learning mode. */
  private boolean isExitCommand(int keyCode, KeyEvent event) {
    // CTRL + W.
    if (keyCode == KeyEvent.KEYCODE_W
        && (event.getMetaState() & KeyEvent.META_CTRL_ON) == KeyEvent.META_CTRL_ON) {
      return true;
    }
    // ESC.
    if (keyCode == KeyEvent.KEYCODE_ESCAPE && event.getMetaState() == 0) {
      return true;
    }
    return false;
  }

  /** Fragment that holds the braille learning mode preference. */
    public static class BrailleLearningModeFragment extends PreferenceFragmentCompat {

    private Connectioneer connectioneer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      connectioneer = Connectioneer.getInstance(getContext());
    }

    @Override
    public void onResume() {
      super.onResume();
      connectioneer.aspectDisplayer.attach(displayerCallback);
    }

    @Override
    public void onPause() {
      super.onPause();
      connectioneer.aspectDisplayer.detach(displayerCallback);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
      getPreferenceManager().setSharedPreferencesName(BRAILLE_SHARED_PREFS_FILENAME);
      PreferenceSettingsUtils.addPreferencesFromResource(this, R.xml.bd_learning_mode);
    }

    /** Handles behavior when the display is disconnected. */
    private void handleOnDisplayStopped() {
      BrailleCommonTalkBackSpeaker.getInstance()
          .speak(
              getContext().getString(R.string.bd_learning_mode_no_display_found),
              AnnounceType.INTERRUPT);
      // TODO: Enable auto-launch learning mode.
      getActivity().finish();
    }

    private final AspectDisplayer.Callback displayerCallback =
        new AspectDisplayer.Callback() {
          @Override
          public void onDisplayStopped() {
            handleOnDisplayStopped();
          }

          @Override
          public void onDisplayStarted(BrailleDisplayProperties brailleDisplayProperties) {}
        };
  }
}
