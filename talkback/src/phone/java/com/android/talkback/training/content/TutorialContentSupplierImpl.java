/*
 * Copyright (C) 2024 Google Inc.
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

package com.google.android.accessibility.talkback.training.content;

import static com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_BACK;
import static com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_EXIT;
import static com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_FINISH;
import static com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.BUTTON_TYPE_NEXT;
import static com.google.android.accessibility.utils.material.MaterialComponentUtils.ButtonStyle.DEFAULT_BUTTON;
import static com.google.android.accessibility.utils.material.MaterialComponentUtils.ButtonStyle.FILLED_BUTON;
import static com.google.android.accessibility.utils.material.MaterialComponentUtils.ButtonStyle.OUTLINED_BUTTON;

import android.content.Context;
import android.graphics.Typeface;
import android.text.TextUtils.TruncateAt;
import android.util.TypedValue;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout.LayoutParams;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar;
import com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar.ButtonType;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig;
import com.google.android.accessibility.talkback.trainingcommon.content.ExitBanner;
import com.google.android.accessibility.talkback.trainingcommon.content.PageContentConfig;
import com.google.android.accessibility.talkback.trainingcommon.content.Text;
import com.google.android.accessibility.talkback.trainingcommon.content.Text.Paragraph;
import com.google.android.accessibility.talkback.trainingcommon.content.TextList;
import com.google.android.accessibility.talkback.trainingcommon.content.Title;
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector.TutorialContentSupplier;
import com.google.android.accessibility.talkback.trainingcommon.content.WebViewContentConfig;
import com.google.android.accessibility.talkback.trainingcommon.tv.TvNavigationButton;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.accessibility.utils.material.MaterialComponentUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Provides an implementation on tutorial content, like {@link Text}, etc., for the Handset and TV
 * form factor.
 */
public class TutorialContentSupplierImpl implements TutorialContentSupplier {

  @NonNull
  @Override
  public Text provideText(@NonNull Paragraph... paragraphs) {
    return new Text(paragraphs);
  }

  @NonNull
  @Override
  public WebViewContentConfig provideWebView(@StringRes int textResId) {
    return new WebViewContentConfig(textResId);
  }

  @NonNull
  @Override
  public Title provideTitle(@NonNull PageConfig pageConfig) {
    return new Title(pageConfig);
  }

  @Override
  public TextList provideTextList(int titlesResId, int summariesResId) {
    return new TextList(titlesResId, summariesResId);
  }

  @Override
  public PageContentConfig providePageIconButton(
      @DrawableRes int drawableResId,
      @StringRes int contentDescriptionResId,
      View.OnClickListener clickListener) {
    throw new UnsupportedOperationException("PageIconButton is not supported in Handset.");
  }

  @Override
  public ExitBanner provideExitBanner(@NotNull PageConfig.PageAndContentPredicate predicate) {
    ExitBanner exitBanner = new ExitBanner();
    exitBanner.setShowingPredicate(predicate);
    return exitBanner;
  }

  @Nullable
  @Override
  public View provideNavigationButton(
      @NonNull Context context,
      int buttonType,
      int text,
      @NonNull OnClickListener onClickListener,
      int buttonCount,
      int currentPageNumber) {
    if (FormFactorUtils.isAndroidTv()) {
      return createNavigationButtonTv(context, text, onClickListener);
    }

    return createNavigationButtonPhone(
        context, buttonType, text, onClickListener, buttonCount, currentPageNumber);
  }

  private View createNavigationButtonTv(
      Context context, @StringRes int text, OnClickListener clickListener) {
    TvNavigationButton button = new TvNavigationButton(context);
    button.setText(context.getString(text));
    button.setOnClickListener(clickListener);
    return button;
  }

  private View createNavigationButtonPhone(
      Context context,
      @ButtonType int buttonType,
      @StringRes int text,
      OnClickListener clickListener,
      int buttonCount,
      int currentPageNumber) {
    Button button;
    if (MaterialComponentUtils.supportMaterialComponent(context)) {
      if ((buttonType == BUTTON_TYPE_NEXT) || (buttonType == BUTTON_TYPE_FINISH)) {
        button = MaterialComponentUtils.createButton(context, FILLED_BUTON);
      } else if ((buttonType == BUTTON_TYPE_EXIT) || (buttonType == BUTTON_TYPE_BACK)) {
        button = MaterialComponentUtils.createButton(context, OUTLINED_BUTTON);
      } else {
        button = MaterialComponentUtils.createButton(context, DEFAULT_BUTTON);
      }
    } else {
      button = new Button(context);
      button.setBackgroundColor(
          context
              .getResources()
              .getColor(
                  com.google.android.accessibility.talkback.R.color
                      .training_navigation_button_bar_background_color,
                  /* theme= */ null));
      button.setTextColor(
          context
              .getResources()
              .getColor(
                  com.google.android.accessibility.talkback.R.color.training_button_text_color,
                  /* theme= */ null));
    }
    button.setText(text);
    button.setTypeface(
        Typeface.create(
            context.getString(
                com.google.android.accessibility.talkback.R.string.accessibility_font),
            Typeface.NORMAL));
    button.setTextSize(
        TypedValue.COMPLEX_UNIT_PX,
        context
            .getResources()
            .getDimensionPixelSize(
                com.google.android.accessibility.talkback.R.dimen.training_button_text_size));
    button.setPaddingRelative(
        0,
        0,
        0,
        context.getResources().getDimensionPixelSize(R.dimen.training_button_text_padding_bottom));
    button.setAllCaps(false);
    button.setEllipsize(TruncateAt.END);
    button.setLines(1);

    button.setId(NavigationButtonBar.getButtonId(buttonType, currentPageNumber));
    button.setLayoutParams(createLayoutParams(context, buttonType, buttonCount));
    button.setOnClickListener(clickListener);
    if (buttonType == BUTTON_TYPE_EXIT) {
      button.setContentDescription(context.getString(R.string.training_finish_tutorial));
    }

    return button;
  }

  private static LayoutParams createLayoutParams(
      Context context, @ButtonType int buttonType, int buttonCount) {
    LayoutParams layoutParams =
        new LayoutParams(
            /* width= */ 0,
            (int) context.getResources().getDimension(R.dimen.training_button_height),
            /* weight= */ 1);

    if (MaterialComponentUtils.supportMaterialComponent(context)) {
      // Default 3-button layout
      int leftMarginDimRes = R.dimen.training_button_margin_2dp;
      int rightMarginDimRes = R.dimen.training_button_margin_2dp;

      if (buttonCount == 2) {
        if ((buttonType == BUTTON_TYPE_NEXT) || (buttonType == BUTTON_TYPE_FINISH)) {
          // Sets left button layout
          leftMarginDimRes = R.dimen.training_button_margin_24dp;
          rightMarginDimRes = R.dimen.training_button_margin_8dp;
        } else if ((buttonType == BUTTON_TYPE_EXIT) || (buttonType == BUTTON_TYPE_BACK)) {
          // Sets right button layout
          leftMarginDimRes = R.dimen.training_button_margin_8dp;
          rightMarginDimRes = R.dimen.training_button_margin_24dp;
        }
      } else if (buttonCount == 1) {
        // Sets 1 button layout
        leftMarginDimRes = R.dimen.training_button_margin_24dp;
        rightMarginDimRes = R.dimen.training_button_margin_24dp;
      }
      layoutParams.leftMargin = (int) context.getResources().getDimension(leftMarginDimRes);
      layoutParams.rightMargin = (int) context.getResources().getDimension(rightMarginDimRes);
    }

    return layoutParams;
  }
}
