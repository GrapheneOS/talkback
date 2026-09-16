package com.google.android.accessibility.talkback.training.content;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector.TutorialContentManager;
import org.jetbrains.annotations.NotNull;

/** Provides an implementation on adding contents into scrollable view. */
public class TutorialContentManagerImpl implements TutorialContentManager {

  private final LinearLayout pageLayout;
  @Nullable private final LinearLayout pageBannerLayout;
  private final ViewGroup navBarContainer;

  public TutorialContentManagerImpl(
      View parentView, @Nullable View bannerLayout, @Nullable View navigationBarContainer) {
    if (!(parentView instanceof LinearLayout parentLinearLayout)) {
      throw new IllegalArgumentException(
          "parentView should be LinearLayout for tutorial pages. parentView=" + parentView);
    }
    if (bannerLayout != null && !(bannerLayout instanceof LinearLayout)) {
      throw new IllegalArgumentException(
          "bannerLayout should be LinearLayout for tutorial pages. bannerLayout=" + bannerLayout);
    }
    if (navigationBarContainer != null && !(navigationBarContainer instanceof ViewGroup)) {
      throw new IllegalArgumentException(
          "navigationBarContainer should be ViewGroup for tutorial pages. navigationBarContainer="
              + navigationBarContainer);
    }

    pageLayout = parentLinearLayout;
    pageBannerLayout = (LinearLayout) bannerLayout;
    navBarContainer = (ViewGroup) navigationBarContainer;
  }

  @Override
  public void addTutorialContentView(View tutorialContentView) {
    pageLayout.addView(tutorialContentView);
  }

  @Override
  public void addTutorialBanner(@NotNull View bannerView) {
    if (pageBannerLayout != null) {
      pageBannerLayout.addView(bannerView);
    }
  }

  @Override
  public boolean hasTutorialBannerContainer() {
    return pageBannerLayout != null;
  }

  @Override
  public void addNavigationContentView(View navigationContentView) {
    if (navBarContainer != null) {
      navBarContainer.addView(navigationContentView);
    }
  }
}
