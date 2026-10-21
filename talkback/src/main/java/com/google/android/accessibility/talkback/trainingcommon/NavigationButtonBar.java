/*
 * Copyright (C) 2020 Google Inc.
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

package com.google.android.accessibility.talkback.trainingcommon;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** A navigation bar with four buttons for Back, Next, Exit and Turn off TalkBack. */
public class NavigationButtonBar extends LinearLayout {

  private static final String TAG = "NavigationButtonBar";

  /** A callback to be invoked when training navigation button has been clicked. */
  public interface NavigationListener {
    /** Called when Back button has been clicked. */
    void onBack();

    /** Called when Next button has been clicked. */
    void onNext();

    /** Called when Exit button has been clicked. */
    void onExit();
  }

  /** The function of buttons. */
  @IntDef({BUTTON_TYPE_BACK, BUTTON_TYPE_NEXT, BUTTON_TYPE_EXIT, BUTTON_TYPE_FINISH})
  public @interface ButtonType {}

  public static final int BUTTON_TYPE_BACK = 0;
  public static final int BUTTON_TYPE_NEXT = 1;
  public static final int BUTTON_TYPE_EXIT = 2;
  public static final int BUTTON_TYPE_FINISH = 3;

  public static final ImmutableList<Integer> DEFAULT_BUTTONS =
      ImmutableList.of(BUTTON_TYPE_BACK, BUTTON_TYPE_NEXT, BUTTON_TYPE_EXIT);

  private final LinearLayout navigationBarLayout;

  /**
   * A list of buttons which will be shown on the navigation button bar.
   *
   * <p>The type of the element in the list should be {@link ButtonType}.
   */
  private final List<Integer> navigationButtons;

  private final NavigationListener navigationListener;
  private final int currentPageNumber;
  private final boolean isFirstPage;
  private final boolean isLastPage;
  private final boolean isExitButtonOnlyShowOnLastPage;
  private final boolean isPrevButtonShownOnFirstPage;

  private static final TutorialContentInterfaceInjector trainingContent =
      TutorialContentInterfaceInjector.getInstance();

  public NavigationButtonBar(
      Context context,
      List<Integer> navigationButtons,
      NavigationListener navigationListener,
      int currentPageNumber,
      boolean isFirstPage,
      boolean isLastPage,
      boolean isExitButtonOnlyShowOnLastPage,
      boolean isPrevButtonShownOnFirstPage) {
    super(context);
    this.navigationBarLayout =
        inflate(context, R.layout.training_navigation_button_bar, this)
            .findViewById(R.id.training_navigation);
    this.navigationButtons = navigationButtons;
    this.navigationListener = navigationListener;
    this.currentPageNumber = currentPageNumber;
    this.isFirstPage = isFirstPage;
    this.isLastPage = isLastPage;
    this.isExitButtonOnlyShowOnLastPage = isExitButtonOnlyShowOnLastPage;
    this.isPrevButtonShownOnFirstPage = isPrevButtonShownOnFirstPage;

    if (FormFactorUtils.isAndroidTv()) {
      setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
      setClipChildren(false);
      setClipToPadding(false);
    }

    if (FormFactorUtils.isAndroidWear()) {
      createButtonsForWear();
    } else {
      createButtons();
    }
  }

  /**
   * Creates navigation buttons for the current page. The order of buttons is Next, Back and Close,
   * but the order of buttons on the last page is Finish and Back.
   */
  private void createButtons() {
    int buttonCount = getButtonsCount();
    if (isLastPage && hasButton(BUTTON_TYPE_EXIT)) {
      addButton(BUTTON_TYPE_FINISH, buttonCount);
    }

    // Add Next button if the current page is not the last page.
    if (!isLastPage && hasButton(BUTTON_TYPE_NEXT)) {
      addButton(BUTTON_TYPE_NEXT, buttonCount);
    }

    // Add back button if the current page is not the first page.
    if ((!isFirstPage || isPrevButtonShownOnFirstPage) && hasButton(BUTTON_TYPE_BACK)) {
      addButton(BUTTON_TYPE_BACK, buttonCount);
    }

    // If isExitButtonOnlyShowOnLastPage flag is true, exit button only shows on the last page.
    if (!isLastPage && !isExitButtonOnlyShowOnLastPage && hasButton(BUTTON_TYPE_EXIT)) {
      addButton(BUTTON_TYPE_EXIT, buttonCount);
    }
  }

  /**
   * Creates navigation buttons for the current page on wear. The order of buttons is negative and
   * then positive buttons.
   */
  private void createButtonsForWear() {
    int buttonCount = getButtonsCount();
    if (isLastPage && hasButton(BUTTON_TYPE_EXIT)) {
      addButton(BUTTON_TYPE_FINISH, buttonCount);
    }

    // If isExitButtonOnlyShowOnLastPage flag is true, exit button only shows on the last page.
    if (!isLastPage && !isExitButtonOnlyShowOnLastPage && hasButton(BUTTON_TYPE_EXIT)) {
      addButton(BUTTON_TYPE_EXIT, buttonCount);
    }

    // Add Next button if the current page is not the last page.
    if (!isLastPage && hasButton(BUTTON_TYPE_NEXT)) {
      addButton(BUTTON_TYPE_NEXT, buttonCount);
    }
  }

  private int getButtonsCount() {
    int count = 0;
    if (isLastPage && hasButton(BUTTON_TYPE_EXIT)) {
      count++;
    }

    // Add Next button if the current page is not the last page.
    if (!isLastPage && hasButton(BUTTON_TYPE_NEXT)) {
      count++;
    }

    // Add back button if the current page is not the first page.
    if (!isFirstPage && hasButton(BUTTON_TYPE_BACK)) {
      count++;
    }

    // If isExitButtonOnlyShowOnLastPage flag is true, exit button only shows on the last page.
    if (!isLastPage && !isExitButtonOnlyShowOnLastPage && hasButton(BUTTON_TYPE_EXIT)) {
      count++;
    }

    return count;
  }

  /** Creates a navigation button, the ID of which depends on page number. */
  private void addButton(@ButtonType int buttonType, int buttonCount) {
    switch (buttonType) {
      case BUTTON_TYPE_BACK -> {
        View backButton =
            createButton(
                getContext(),
                buttonType,
                R.string.training_back_button,
                view -> navigationListener.onBack(),
                buttonCount);
        navigationBarLayout.addView(backButton);
        return;
      }
      case BUTTON_TYPE_NEXT -> {
        View nextButton =
            createButton(
                getContext(),
                buttonType,
                R.string.training_next_button,
                view -> navigationListener.onNext(),
                buttonCount);
        navigationBarLayout.addView(nextButton);
        return;
      }
      case BUTTON_TYPE_EXIT, BUTTON_TYPE_FINISH -> {
        View exitButton =
            createButton(
                getContext(),
                buttonType,
                R.string.training_finish_button,
                view -> navigationListener.onExit(),
                buttonCount);
        navigationBarLayout.addView(exitButton);
        return;
      }
      default -> throw new IllegalArgumentException("Unsupported button type.");
    }
  }

  private View createButton(
      Context context,
      @ButtonType int buttonType,
      @StringRes int text,
      OnClickListener clickListener,
      int buttonCount) {
    return trainingContent
        .getTutorialContentSupplier()
        .provideNavigationButton(
            context, buttonType, text, clickListener, buttonCount, currentPageNumber);
  }

  /**
   * Returns an alternate resource ID for the specified button to avoid the focus still keeping on
   * the last focused node when the page is changed.
   */
  @IdRes
  public static int getButtonId(@ButtonType int type, int currentPageNumber) {
    return switch (type) {
      case BUTTON_TYPE_BACK ->
          (currentPageNumber % 2 == 0) ? R.id.training_back_button_0 : R.id.training_back_button_1;
      case BUTTON_TYPE_NEXT ->
          (currentPageNumber % 2 == 0) ? R.id.training_next_button_0 : R.id.training_next_button_1;
      case BUTTON_TYPE_EXIT ->
          (currentPageNumber % 2 == 0) ? R.id.training_exit_button_0 : R.id.training_exit_button_1;
      case BUTTON_TYPE_FINISH ->
          (currentPageNumber % 2 == 0)
              ? R.id.training_finish_button_0
              : R.id.training_finish_button_1;
      default -> throw new IllegalArgumentException("Unsupported button type.");
    };
  }

  private boolean hasButton(@ButtonType int buttonType) {
    return navigationButtons.contains(buttonType);
  }

  @VisibleForTesting
  @Nullable
  View getButton(@ButtonType int type) {
    return navigationBarLayout.findViewById(getButtonId(type, currentPageNumber));
  }
}
