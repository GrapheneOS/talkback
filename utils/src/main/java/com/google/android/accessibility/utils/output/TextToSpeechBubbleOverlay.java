/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.android.accessibility.utils.output;

import static java.lang.Math.max;
import static java.lang.Math.min;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Message;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.view.WindowManager.BadTokenException;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.StringRes;
import com.google.android.accessibility.utils.R;
import com.google.android.accessibility.utils.WeakReferenceHandler;
import com.google.android.accessibility.utils.widget.DialogUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.android.libraries.accessibility.widgets.simple.SimpleOverlay;

/**
 * Displays text on the screen using an inflatable layout. This class is used by {@link
 * SpeechControllerImpl} and AccessibilityMenuService. The overlay is draggable by the user.
 */
public class TextToSpeechBubbleOverlay extends SimpleOverlay implements TextToSpeechOverlay {
  private static final String LOG_TAG = "TextToSpeechBubbleOverlay";
  public static final int DEFAULT_EVENT_TYPE = -1;
  private static final int MSG_SET_TEXT = 1;
  private static final int MSG_CLEAR_TEXT = 2;

  private TextView bubbleText;
  private ImageButton closeButton;
  private ImageButton menuButton;
  private View rootView;

  private final Context context;
  private final OverlayHandler handler;

  // Dragging related fields
  private final int touchSlop;
  private boolean isDragging = false;
  private float initialTouchX;
  private float initialTouchY;
  private int initialWindowX;
  private int initialWindowY;

  private final OnClickListener closeButtonOnClickListener;
  private final OnClickListener menuButtonOnClickListener;
  @StringRes private final int labelResourceId;
  @StringRes private final int menuDescriptionResourceId;

  public TextToSpeechBubbleOverlay(
      Context context,
      @StringRes int labelResourceId,
      @StringRes int menuDescriptionResourceId,
      OnClickListener closeButtonOnClickListener,
      OnClickListener menuButtonOnClickListener) {
    this(
        context,
        /* id= */ 0,
        /* sendsAccessibilityEvents= */ true,
        labelResourceId,
        menuDescriptionResourceId,
        closeButtonOnClickListener,
        menuButtonOnClickListener);
  }

  public TextToSpeechBubbleOverlay(
      Context context,
      int id,
      final boolean sendsAccessibilityEvents,
      @StringRes int labelResourceId,
      @StringRes int menuDescriptionResourceId,
      OnClickListener closeButtonOnClickListener,
      OnClickListener menuButtonOnClickListener) {
    super(context, id, sendsAccessibilityEvents);
    this.context = context;

    this.labelResourceId = labelResourceId;
    this.menuDescriptionResourceId = menuDescriptionResourceId;
    this.closeButtonOnClickListener = closeButtonOnClickListener;
    this.menuButtonOnClickListener = menuButtonOnClickListener;

    this.handler = new OverlayHandler(this);

    ViewConfiguration viewConfiguration = ViewConfiguration.get(context);
    this.touchSlop = viewConfiguration.getScaledTouchSlop();

    setupWindowParams();
    reInflateView();
  }

  public boolean isDragging() {
    return isDragging;
  }

  public void setCloseButtonOnClickListener(OnClickListener onClickListener) {
    closeButton.setOnClickListener(onClickListener);
  }

  public void setMenuButtonOnClickListener(OnClickListener onClickListener) {
    menuButton.setOnClickListener(onClickListener);
  }

  @Override
  public void displayText(CharSequence text) {
    displayText(text, /* eventType= */ DEFAULT_EVENT_TYPE);
  }

  @Override
  // TODO: b/404563630 - Remove this suppression once the lint checker is fixed.
  @SuppressWarnings("FlaggedApi")
  public void displayText(CharSequence text, int eventType) {
    final String preparedText = (text == null) ? "" : text.toString().trim();
    if (TextUtils.isEmpty(preparedText)) {
      return;
    }

    handler.removeMessages(MSG_CLEAR_TEXT);
    handler.sendMessage(Message.obtain(handler, MSG_SET_TEXT, eventType, 0, preparedText));
  }

  /** Manages showing/hiding the overlay and updating its text content based on messages. */
  @Override
  public void hide() {
    handler.removeMessages(MSG_CLEAR_TEXT);
    handler.removeMessages(MSG_SET_TEXT);

    WindowManager.LayoutParams params = getParams();
    params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
    setParams(params);

    super.hide();
  }

  @Override
  public void onConfigurationChanged() {
    final Point lastPosition = new Point(getParams().x, getParams().y);

    reInflateView();
    setupWindowParams();

    // Restore the position, ensuring it's valid for the new screen bounds.
    restorePositionWithinNewBounds(lastPosition);
  }

  /**
   * Recalculates the overlay's position to ensure it remains visible on screen after a
   * configuration change, such as screen rotation or resizing.
   *
   * @param lastPosition The (x, y) coordinates of the overlay before the config change.
   */
  private void restorePositionWithinNewBounds(Point lastPosition) {
    final WindowManager.LayoutParams params = getParams();
    final Resources resources = context.getResources();
    final int screenWidth = resources.getDisplayMetrics().widthPixels;
    final int screenHeight = resources.getDisplayMetrics().heightPixels;

    rootView.measure(
        View.MeasureSpec.makeMeasureSpec(screenWidth, View.MeasureSpec.AT_MOST),
        View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST));
    final int bubbleWidth = rootView.getMeasuredWidth();
    final int bubbleHeight = rootView.getMeasuredHeight();

    // Clamp the previous coordinates to keep the view within the new screen bounds.
    params.x = max(0, min(lastPosition.x, screenWidth - bubbleWidth));
    params.y = max(0, min(lastPosition.y, screenHeight - bubbleHeight));

    setParams(params);
  }

  /**
   * Re-inflates the view hierarchy and restores its state. This is necessary to apply resource
   * changes (e.g., dimensions, layouts, font sizes) from a new configuration.
   */
  private void reInflateView() {
    CharSequence currentText = (bubbleText != null) ? bubbleText.getText() : "";

    rootView = setupUi(context, labelResourceId);
    setContentView(rootView);

    bubbleText = rootView.findViewById(R.id.bubble_text);
    closeButton = rootView.findViewById(R.id.close_button);
    menuButton = rootView.findViewById(R.id.menu_button);

    setCloseButtonOnClickListener(closeButtonOnClickListener);
    menuButton.setContentDescription(context.getString(menuDescriptionResourceId));
    setMenuButtonOnClickListener(menuButtonOnClickListener);
    rootView.setOnTouchListener(new DragTouchListener());

    bubbleText.setMovementMethod(ScrollingMovementMethod.getInstance());
    bubbleText.setText(currentText);
  }

  /**
   * Configures the WindowManager parameters for this overlay window. These parameters control the
   * window's type, behavior, size, and position on the screen, it is independent of the content
   * view's layout.
   */
  private void setupWindowParams() {
    final WindowManager.LayoutParams params = getParams();
    final Resources resources = context.getResources();

    configureWindowFlags(params);
    configureAdaptiveLayout(params, resources);
    configureVerticalPosition(params, resources);

    setParams(params);
  }

  private void configureWindowFlags(WindowManager.LayoutParams params) {
    params.type = DialogUtils.getDialogType();
    params.height = WindowManager.LayoutParams.WRAP_CONTENT;

    params.flags =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
            | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
  }

  /** Configures the overlay's width and horizontal position based on screen size. */
  private void configureAdaptiveLayout(WindowManager.LayoutParams params, Resources resources) {
    if (resources.getBoolean(R.bool.is_large_screen)) {
      params.width = resources.getDimensionPixelSize(R.dimen.tts_bubble_overlay_max_width);
      params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
      params.x = 0;
    } else {
      final int screenWidth = resources.getDisplayMetrics().widthPixels;
      final int margin =
          resources.getDimensionPixelSize(R.dimen.tts_bubble_overlay_horizontal_margin);

      params.width = screenWidth - (2 * margin);
      params.gravity = Gravity.TOP | Gravity.START;
      params.x = margin;
    }
  }

  /** Calculates and sets the vertical position of the overlay at the bottom of the screen. */
  private void configureVerticalPosition(WindowManager.LayoutParams params, Resources resources) {
    final int screenHeight = resources.getDisplayMetrics().heightPixels;
    final int bottomOffset =
        resources.getDimensionPixelSize(R.dimen.tts_bubble_overlay_window_bottom_offset);

    params.y = screenHeight - params.height - bottomOffset;
  }

  /**
   * Inflates the XML layout for the overlay content.
   *
   * @param context The context used for inflation.
   * @return The root View of the inflated layout.
   */
  private View setupUi(Context context, @StringRes int labelResourceId) {
    LayoutInflater inflater = LayoutInflater.from(context);

    @SuppressLint("InflateParams")
    View layout = inflater.inflate(R.layout.tts_bubble_overlay, null, false);

    TextView labelView = layout.findViewById(R.id.tts_bubble_label);
    if (labelView != null) {
      labelView.setText(context.getString(labelResourceId));
    }
    return layout;
  }

  /** Touch listener implementation for handling drag gestures and clicks to child views. */
  private class DragTouchListener implements View.OnTouchListener {

    private final Rect viewBounds = new Rect();

    /** Checks if the raw screen coordinates of a MotionEvent are within a view's bounds. */
    private boolean isEventInView(MotionEvent event, View view) {
      if (view == null || !view.isShown()) {
        return false;
      }
      view.getGlobalVisibleRect(viewBounds);
      return viewBounds.contains((int) event.getRawX(), (int) event.getRawY());
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
      WindowManager.LayoutParams params = getParams();

      switch (event.getActionMasked()) {
        case MotionEvent.ACTION_DOWN -> {
          // TODO: b/436851306 - Confirm with UX about dragging behavior on the bubble text.
          if (isEventInView(event, bubbleText)) {
            return false;
          }

          params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
          setParams(params);

          initialTouchX = event.getRawX();
          initialTouchY = event.getRawY();
          initialWindowX = params.x;
          initialWindowY = params.y;

          isDragging = false;
          return true;
        }
        case MotionEvent.ACTION_MOVE -> {
          float deltaX = event.getRawX() - initialTouchX;
          float deltaY = event.getRawY() - initialTouchY;

          // Check if movement exceeds touch slop threshold to initiate drag.
          if (!isDragging && Math.hypot(deltaX, deltaY) > touchSlop) {
            isDragging = true;
          }

          if (isDragging) {
            params.x = initialWindowX + (int) deltaX;
            params.y = initialWindowY + (int) deltaY;
            setParams(params);
          }
          return true;
        }
        case MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
          if (!isDragging) {
            // This was a tap, not a drag. Check which button it hit.
            if (isEventInView(event, closeButton)) {
              closeButton.performClick();
            } else if (isEventInView(event, menuButton)) {
              menuButton.performClick();
            }
          }

          params.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
          setParams(params);

          // Reset state
          isDragging = false;
          return true;
        }
        default -> {}
      }
      return false;
    }
  }

  private static class OverlayHandler extends WeakReferenceHandler<TextToSpeechBubbleOverlay> {
    public OverlayHandler(TextToSpeechBubbleOverlay parent) {
      super(parent);
    }

    @Override
    protected void handleMessage(Message msg, TextToSpeechBubbleOverlay parent) {
      switch (msg.what) {
        case MSG_SET_TEXT -> {
          try {
            WindowManager.LayoutParams params = parent.getParams();
            params.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            parent.setParams(params);
            parent.show();
          } catch (BadTokenException e) {
            LogUtils.e(
                LOG_TAG, e, "Caught WindowManager.BadTokenException while trying to show overlay.");
            return;
          }
          parent.bubbleText.setText((CharSequence) msg.obj);
          parent.bubbleText.scrollTo(0, 0);
        }
        case MSG_CLEAR_TEXT -> parent.bubbleText.setText("");
        default ->
            LogUtils.w(LOG_TAG, "OverlayHandler received unhandled message type: %d", msg.what);
      }
    }
  }
}
