package com.google.android.libraries.accessibility.utils.url;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.text.style.URLSpan;
import android.view.View;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.auto.value.AutoValue;


/**
 * Represents a URL from a {@link android.text.SpannableString} with a URL path and its associated
 * text from the {@link android.text.SpannableString}.
 */
@AutoValue
public abstract class SpannableUrl {

  private static final String TAG = "SpannableUrl";

  public static SpannableUrl create(String string, URLSpan urlSpan) {
    return new AutoValue_SpannableUrl(urlSpan, string);
  }

  /**
   * A {@link URLSpan} associated with this SpannableUrl. Access to the original URLSpan is needed,
   * as subclasses may use {@link URLSpan#onClick(View)} instead of {@link URLSpan#getURL()}.
   */
  public abstract URLSpan urlSpan();

  /**
   * The text from the {@link android.text.SpannableString} associated with this SpannableUrl. For
   * example, if a spannable string "Click here" has a {@link android.text.style.URLSpan} on the
   * text "here," the text value would be "here." It is possible for this to be the same value as
   * the URL path.
   */
  public abstract String text();

  /** A URL path from a {@link android.text.style.URLSpan}. */
  public String path() {
    return urlSpan().getURL();
  }

  /** Returns {@code true} if the URL text and path are the same. */
  public boolean isTextAndPathEquivalent() {
    return text().equals(path());
  }

  // URLSpan.onClick is fine with a null parameter when called from an AccessibilityService.
  @SuppressWarnings("nullness:argument")
  public void onClick(Context context) {
    try {
      // Calling urlSpan() may return either a UrlSpan or an AccessibilityUrlSpan (or some other
      // subclass of UrlSpan). UrlSpan#onClick(null) throws, but AccessibilityUrlSpan#onClick(null)
      // does not; and we handle both scenarios in the code below.
      urlSpan().onClick(null);
    } catch (RuntimeException e) {
      LogUtils.i(TAG, "Clicking urlSpan failed: %s\n%s", urlSpan(), e);
      try {
        LogUtils.i(TAG, "Attempting to open %s using Intent",  path());
        UrlUtils.openUrlWithIntent(context, path());
      } catch (ActivityNotFoundException anfe) {
        // Sometimes a malformed link can cause an ActivityNotFound exception when a link is
        // clicked.
        LogUtils.i(TAG, "Activity not found when opening url: %s/n%s", path(), anfe);
      }
    }
  }
}
