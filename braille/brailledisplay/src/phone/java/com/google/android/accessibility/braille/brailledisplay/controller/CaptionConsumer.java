package com.google.android.accessibility.braille.brailledisplay.controller;

import static java.lang.Math.min;

import android.content.Context;
import android.graphics.Typeface;
import androidx.appcompat.app.AlertDialog;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brltty.BrailleInputEvent;
import com.google.android.accessibility.braille.common.BraillePreferenceUtils;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import java.time.Duration;
import org.apache.commons.text.similarity.LevenshteinDistance;

/** An event consumer catches captions on the screen. */
public class CaptionConsumer implements EventConsumer {
  private static final String TAG = "CaptionConsumer";
  private static final float SENTENCE_SIMILARITY_THRESHOLD = 0.5f;
  private final Context context;
  private final CellsContentConsumer cellsContentConsumer;
  private AlertDialog tutorialDialog;
  private String captions;

  public CaptionConsumer(Context context, CellsContentConsumer cellsContentConsumer) {
    this.context = context;
    this.cellsContentConsumer = cellsContentConsumer;
  }

  @Override
  public void onActivate() {}

  @Override
  public void onDeactivate() {}

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event) {
    BrailleDisplayLog.v(TAG, "onAccessibilityEvent");
    if (showTutorial()) {
      if (tutorialDialog != null && tutorialDialog.isShowing()) {
        return;
      }
      String dialogMessage = context.getString(R.string.bd_caption_tutorial_dialog_message);
      String preferenceTitle = context.getString(R.string.bd_preference_caption_title);
      int start = dialogMessage.indexOf(preferenceTitle);
      int end = start + preferenceTitle.length();
      SpannableString spannableMessage = new SpannableString(dialogMessage);
      spannableMessage.setSpan(
          new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      tutorialDialog =
          BraillePreferenceUtils.createDialogWithCheckbox(
              context,
              context.getString(R.string.bd_caption_tutorial_dialog_title),
              spannableMessage,
              context.getString(R.string.bd_caption_tutorial_dialog_checkbox),
              BrailleUserPreferences::writeShowCaptionTutorialDialogAgain);
      tutorialDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY);
      tutorialDialog.show();
      BrailleUserPreferences.writeShowCaptionTime(context, System.currentTimeMillis());
      return;
    }
    CharSequence text = event.getContentDescription();
    String old = captions;
    if (TextUtils.isEmpty(text)) {
      AccessibilityNodeInfoCompat sourceNode = AccessibilityEventUtils.sourceCompat(event);
      text = AccessibilityNodeInfoUtils.getText(sourceNode);
    }
    if (!TextUtils.isEmpty(text) && !TextUtils.equals(captions, text)) {
      captions = text.toString();
      int strategy =
          TextUtils.isEmpty(old)
              ? CellsContent.PAN_RESET
              : (sameCaption(old, captions) ? CellsContent.PAN_KEEP : CellsContent.PAN_RESET);
      BrailleDisplayLog.v(TAG, "strategy: " + strategy);
      CellsContent content = new CellsContent(captions);
      content.setPanStrategy(strategy);
      cellsContentConsumer.setTimedContent(
          TimedMessager.Type.CAPTION, content, TimedMessager.UNLIMITED_DURATION);
    }
  }

  /**
   * Returns true if two sentences are essentially the same, allowing for minor differences like
   * typos and an appended sentence at the end, provided the differences do not exceed a certain
   * threshold.
   */
  private boolean sameCaption(String oldStr, String newStr) {
    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    // If the new string is longer than the old string, we only compare the portion of the new
    // string that has the same length as the old string, because the new string could be very long.
    // For example, if the old string is "Hello world" and the new string is "Hello world. This is a
    // test.", the new string is considered the same caption. If the new string is shorter than the
    // old string, we compare the portion of the old string that has the same length as the new
    // string. For example, if the old string is "Hello world" and the new string is "Hello word",
    // the new string is considered the same caption.
    String prefix = newStr.substring(0, min(newStr.length(), oldStr.length()));
    int distance = levenshteinDistance.apply(oldStr, prefix);
    BrailleDisplayLog.v(
        TAG,
        "oldStr.length: "
            + oldStr.length()
            + ", newStr.length: "
            + newStr.length()
            + ", distance: "
            + distance);
    return distance <= newStr.length() * SENTENCE_SIMILARITY_THRESHOLD;
  }

  @Override
  public boolean onMappedInputEvent(BrailleInputEvent event) {
    return false;
  }

  private boolean showTutorial() {
    if (!BrailleUserPreferences.readShowCaptionTutorialDialogAgain(context)) {
      return false;
    }
    long lastDialogTime = BrailleUserPreferences.readShowCaptionTime(context).toEpochMilli();
    return lastDialogTime == 0
        || System.currentTimeMillis() - lastDialogTime > Duration.ofDays(1).toMillis(); // One day.
  }
}
