package com.google.android.accessibility.brailleime;

import static com.google.android.accessibility.brailleime.BrailleImeGestureAction.isValid;

import android.content.Context;
import android.content.res.Configuration;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.Size;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnAttachStateChangeListener;
import android.view.animation.AlphaAnimation;
import android.widget.FrameLayout;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.Constants.BrailleType;
import com.google.android.accessibility.braille.common.TouchDots;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.brailleime.BrailleIme.OrientationSensitive;
import com.google.android.accessibility.brailleime.LayoutOrientator.LayoutOrientatorCallback;
import com.google.android.accessibility.brailleime.input.BrailleInputView;
import com.google.android.accessibility.brailleime.input.DotHoldSwipe;
import com.google.android.accessibility.brailleime.input.Swipe;
import com.google.android.accessibility.brailleime.tutorial.VerticalTextView;
import com.google.android.material.color.MaterialColors;
import com.google.common.annotations.VisibleForTesting;
import java.util.Optional;

/** A view for showing custom gesture hint. */
public class CustomGestureView extends FrameLayout
    implements OnAttachStateChangeListener, OrientationSensitive {
  private static final int HINT_ANIMATION_DURATION_MS = 300;
  private static final double HINT_HEIGHT_FRACTION = 2.5;
  private static final double HINT_WIDTH_FRACTION = 4.5;
  private final BrailleInputView inputView;
  private final AlphaAnimation hintToastAnimation;
  private final LayoutOrientator layoutOrientator;
  private final CustomGestureCallback customGestureCallback;
  private final VerticalTextView verticalTextView;
  private Size screenSize;
  private boolean tableTopMode;

  /** A callback for custom gesture event. */
  public interface CustomGestureCallback {
    /** Called when an invalid gesture is detected. */
    boolean onInvalidGesture();

    /** Called when a swipe gesture is detected. */
    boolean onSwipeProduced(Swipe swipe);

    /** Called when a dot hold and dot swipe gesture is detected. */
    boolean onDotHoldAndDotSwipe(DotHoldSwipe dotHoldSwipe);

    /** Called when a hold gesture is detected. */
    boolean onHoldProduced(int pointersHeldCount);

    /** Called when the detection of the layout is changed. */
    void onDetectionChanged(boolean isTabletop);
  }

  public CustomGestureView(
      Context context, CustomGestureCallback customGestureCallback, Size screenSize) {
    super(context);
    this.screenSize = screenSize;
    this.customGestureCallback = customGestureCallback;
    layoutOrientator = new LayoutOrientator(getContext(), layoutOrientatorCallback);
    layoutOrientator.startIfNeeded();
    BrailleInputOptions options =
        BrailleInputOptions.builder()
            .setTutorialMode(true)
            .setBrailleType(BrailleType.SIX_DOT)
            .build();
    boolean tableTopMode = isCurrentTableTopMode();
    inputView =
        new BrailleInputView(context, brailleInputViewCallback, screenSize, options, tableTopMode);
    hintToastAnimation = new AlphaAnimation(/* fromAlpha= */ 0.0f, /* toAlpha= */ 1.0f);
    hintToastAnimation.setDuration(HINT_ANIMATION_DURATION_MS);
    verticalTextView =
        new VerticalTextView(new ContextThemeWrapper(getContext(), R.style.CustomGestureHint));
    addView(inputView);
    addView(verticalTextView, getTextViewLayoutParams());
    addOnAttachStateChangeListener(this);
  }

  @Override
  public void onViewAttachedToWindow(@NonNull View v) {}

  @Override
  public void onViewDetachedFromWindow(@NonNull View v) {
    layoutOrientator.stop();
    removeOnAttachStateChangeListener(this);
  }

  @Override
  public void onOrientationChanged(int orientation, Size screenSize) {
    this.screenSize = screenSize;
    verticalTextView.onOrientationChanged(orientation, screenSize);
    updateViewLayout(verticalTextView, getTextViewLayoutParams());
    inputView.onOrientationChanged(orientation, screenSize);
  }

  /** Sets keyboard to table layout. */
  public void setTableTopMode(boolean tableTopMode) {
    this.tableTopMode = tableTopMode;
    updateViewLayout(verticalTextView, getTextViewLayoutParams());
    inputView.setTabletopMode(tableTopMode);
  }

  /** Shows the hint. */
  public void showHint(SpannableStringBuilder sb, String title, String hint, boolean warning) {
    colorSubstring(sb, title, warning);
    resizeSubstring(sb, hint);
    verticalTextView.setText(sb);
    verticalTextView.setVisibility(VISIBLE);
    verticalTextView.startAnimation(hintToastAnimation);
  }

  /** Returns true if the current layout is tabletop. */
  public boolean isCurrentTableTopMode() {
    Optional<TouchDots> layoutOptional = layoutOrientator.getDetectedLayout();
    TouchDots mode = BrailleUserPreferences.readLayoutMode(getContext());
    return mode == TouchDots.TABLETOP
        || layoutOptional
            .map(touchDots -> touchDots == TouchDots.TABLETOP)
            .orElseGet(
                () ->
                    getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE);
  }

  private LayoutParams getTextViewLayoutParams() {
    LayoutParams layoutParams =
        new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    int margin = (int) ResourcesCompat.getFloat(getResources(), R.dimen.custom_gesture_hint_margin);
    layoutParams.setMargins(
        /* left= */ margin, /* top= */ margin, /* right= */ margin, /* bottom= */ margin);
    if (isPortrait()) {
      layoutParams.height =
          tableTopMode
              ? (int) (screenSize.getHeight() / HINT_HEIGHT_FRACTION)
              : (int) (screenSize.getHeight() / HINT_WIDTH_FRACTION);
    } else {
      layoutParams.height =
          tableTopMode
              ? (int) (screenSize.getWidth() / HINT_HEIGHT_FRACTION)
              : (int) (screenSize.getWidth() / HINT_WIDTH_FRACTION);
    }
    layoutParams.gravity = getGravity();
    return layoutParams;
  }

  private int getGravity() {
    return tableTopMode
        ? (isPortrait() ? Gravity.START | Gravity.CENTER : Gravity.TOP | Gravity.CENTER)
        : Gravity.CENTER;
  }

  private boolean isPortrait() {
    return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
  }

  private void resizeSubstring(SpannableStringBuilder allString, CharSequence substring) {
    int indexStart = allString.toString().indexOf(substring.toString());
    int indexEnd = indexStart + substring.length();
    if (indexStart == -1) {
      return;
    }
    allString.setSpan(
        new RelativeSizeSpan(0.9f), indexStart, indexEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  private void colorSubstring(
      SpannableStringBuilder allString, CharSequence substring, boolean warning) {
    int indexStart = allString.toString().indexOf(substring.toString());
    int indexEnd = indexStart + substring.length();
    if (indexStart == -1) {
      return;
    }
    allString.setSpan(
        new ForegroundColorSpan(getTextColor(warning)),
        indexStart,
        indexEnd,
        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
  }

  @ColorInt
  private int getTextColor(boolean warning) {
    Context themeContext = new ContextThemeWrapper(getContext(), R.style.CustomGestureView);
    return MaterialColors.getColor(
        themeContext,
        warning ? R.attr.colorErrorContainer : R.attr.colorTertiaryFixed,
        "Unsupported color palette");
  }

  @VisibleForTesting
  public BrailleInputView testing_getBrailleInputView() {
    return inputView;
  }

  private final BrailleInputView.Callback brailleInputViewCallback =
      new BrailleInputView.Callback() {
        @Override
        public boolean onInvalidGesture() {
          return customGestureCallback.onInvalidGesture();
        }

        @Override
        public String onBrailleProduced(BrailleCharacter brailleCharacter) {
          var unused = onInvalidGesture();
          return "";
        }

        @Override
        public boolean onSwipeProduced(Swipe swipe) {
          if (!isValid(swipe)) {
            return onInvalidGesture();
          }
          return customGestureCallback.onSwipeProduced(swipe);
        }

        @Override
        public boolean onDotHoldAndDotSwipe(DotHoldSwipe dotHoldSwipe) {
          if (!isValid(dotHoldSwipe)) {
            return onInvalidGesture();
          }
          return customGestureCallback.onDotHoldAndDotSwipe(dotHoldSwipe);
        }

        @Override
        public boolean onHoldProduced(int pointersHeldCount) {
          return customGestureCallback.onHoldProduced(pointersHeldCount);
        }
      };

  private final LayoutOrientatorCallback layoutOrientatorCallback =
      new LayoutOrientatorCallback() {

        @Override
        public boolean useSensorsToDetectLayout() {
          return BrailleUserPreferences.readLayoutMode(getContext()) == TouchDots.AUTO_DETECT;
        }

        @Override
        public void onDetectionChanged(boolean isTabletop, boolean firstChangedEvent) {
          customGestureCallback.onDetectionChanged(isTabletop);
        }
      };
}
