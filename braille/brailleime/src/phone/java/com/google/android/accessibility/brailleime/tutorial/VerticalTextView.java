package com.google.android.accessibility.brailleime.tutorial;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.Size;
import android.widget.TextView;
import com.google.android.accessibility.brailleime.BrailleIme.OrientationSensitive;

/** A {@code TextView} that display texts vertically. */
public class VerticalTextView extends TextView implements OrientationSensitive {
  private int orientation = getResources().getConfiguration().orientation;

  public VerticalTextView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public VerticalTextView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public VerticalTextView(Context context) {
    super(context);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    super.onMeasure(heightMeasureSpec, widthMeasureSpec);
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    } else {
      setMeasuredDimension(getMeasuredWidth(), getMeasuredHeight());
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    TextPaint textPaint = getPaint();
    textPaint.setColor(getCurrentTextColor());
    textPaint.drawableState = getDrawableState();

    canvas.save();
    if (orientation == Configuration.ORIENTATION_PORTRAIT) {
      canvas.translate(/* dx= */ 0, /* dy= */ getHeight());
      canvas.rotate(/* degree= */ -90);
    } else {
      canvas.translate(/* dx= */ 0, /* dy= */ 0);
      canvas.rotate(/* degree= */ 0);
    }

    canvas.translate(getCompoundPaddingLeft(), getExtendedPaddingTop());

    getLayout().draw(canvas);
    canvas.restore();
  }

  @Override
  public void onOrientationChanged(int orientation, Size screenSize) {
    this.orientation = orientation;
    invalidate();
    requestLayout();
  }
}
