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

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_AUTO;
import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_YES;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig.IdleAnnouncementConfig;
import com.google.android.accessibility.talkback.trainingcommon.PageConfig.PageId;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;
import com.google.android.accessibility.talkback.trainingcommon.content.ExitBanner;
import com.google.android.accessibility.talkback.trainingcommon.content.Link;
import com.google.android.accessibility.talkback.trainingcommon.content.Link.LinkHandler;
import com.google.android.accessibility.talkback.trainingcommon.content.PageButton;
import com.google.android.accessibility.talkback.trainingcommon.content.PageContentConfig;
import com.google.android.accessibility.talkback.trainingcommon.content.PageNumber;
import com.google.android.accessibility.talkback.trainingcommon.content.Title;
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector;
import com.google.android.accessibility.talkback.trainingcommon.content.TutorialContentInterfaceInjector.TutorialContentManager;
import com.google.android.accessibility.utils.FormFactorUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.base.VerifyException;
import com.google.common.collect.ImmutableList;
import java.util.function.Function;

/** A fragment to show one of the training page that parsers from {@link #EXTRA_PAGE} argument. */
public class TrainingFragment extends Fragment {

  private static final String TAG = "TrainingFragment";
  public static final String EXTRA_PAGE = "page";
  public static final String EXTRA_PAGE_NUMBER = "page_number";
  public static final String EXTRA_TOTAL_NUMBER = "total_number";
  public static final String EXTRA_VENDOR_PAGE_INDEX = "vendor_page_index";

  @Nullable private PageConfig page;
  private View pageLayout;
  private LinearLayout pageBannerLayout;
  private LinkHandler linkHandler;
  private ServiceData data;

  @Nullable NavigationButtonBar navigationButtonBar;
  private TutorialContentManager tutorialContentManager;

  // We only have supplier if this page has navigation bar and it is belong to some form factors.
  @Nullable private Function<Context, NavigationButtonBar> navigationButtonBarSupplier;

  private TrainingMetricStore metricStore;
  @Nullable private RepeatedAnnouncingHandler repeatedAnnouncingHandler;

  void setNavigationButtonBarSupplier(
      Function<Context, NavigationButtonBar> navigationButtonBarSupplier) {
    this.navigationButtonBarSupplier = navigationButtonBarSupplier;
  }

  void setMetricStore(TrainingMetricStore metricStore) {
    this.metricStore = metricStore;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedState) {
    View view = inflater.inflate(R.layout.training_fragment_name, container, false);
    pageLayout = view.findViewById(R.id.training_page);
    pageBannerLayout = view.findViewById(R.id.training_page_banner);

    @Nullable Bundle arguments = getArguments();
    if (arguments == null) {
      LogUtils.e(TAG, "Cannot create view because fragment was created without arguments.");
      return view;
    }

    @Nullable PageId pageId = (PageId) arguments.get(EXTRA_PAGE);

    if (pageId == null) {
      LogUtils.e(TAG, "Cannot create view because no page ID.");
      return view;
    }

    FragmentActivity activity = getActivity();
    if (activity == null) {
      LogUtils.e(TAG, "Cannot create view because fragment is not attached to activity.");
      return view;
    }

    // Remind there is a potential issue: The supplier may be null when the fragment is restored
    // while the activity is recreated. Ideally we should reassign it when the window is recreated.
    // However we don't need to worried about it now because we don't recreate the trainingActivity
    // in configuration changes.
    ViewGroup navBarContainer = view.findViewById(R.id.nav_container);
    tutorialContentManager =
        TutorialContentInterfaceInjector.getInstance()
            .newTutorialContentManager(pageLayout, pageBannerLayout, navBarContainer);
    if (navigationButtonBarSupplier != null) {
      navigationButtonBar = navigationButtonBarSupplier.apply(view.getContext());
      tutorialContentManager.addNavigationContentView(navigationButtonBar);
    }

    int vendorPageIndex = arguments.getInt(EXTRA_VENDOR_PAGE_INDEX, PageConfig.UNKNOWN_PAGE_INDEX);
    page = PageConfig.getPage(pageId, activity, vendorPageIndex);

    if (page == null) {
      LogUtils.e(TAG, "Cannot create view because unknown PageId. [%s]", pageId.name());
      return view;
    }

    pageLayout.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_AUTO);
    addView(inflater, container);

    if (page.getIdleAnnouncementConfig() != null) {
      IdleAnnouncementConfig config = page.getIdleAnnouncementConfig();
      repeatedAnnouncingHandler =
          new RepeatedAnnouncingHandler(
              getContext(),
              getContext().getString(config.announcement()),
              config.initialDelay(),
              config.repeatedDelay());
      LogUtils.v(TAG, "Idle announcement prepared.");
    }

    if (FormFactorUtils.isAndroidWear()) {
      // Setting a pane title for fragment will trigger TYPE_WINDOW_STATE_CHANGED. It will drop an
      // immediate TYPE_VIEW_FOCUS event sent after the last window state changed event. As a
      // result, the dropped TYPE_VIEW_FOCUS event will not set a11y focus on the fragment.
      ViewCompat.setAccessibilityPaneTitle(view, getString(page.getPageNameResId()));

      // Support rotary input.
      view.requestFocus();

      // The page supports to swipe right with 2-fingers to go back to the previous page.
      if (getActivity() instanceof TrainingActivity) {
        view = ((TrainingActivity) getActivity()).wrapWithSwipeHandler(view);
      }
    }

    // On TV, we apply the TalkBack focus always first on the text, then after clicking the center
    // button we move the input focus to the navigation buttons. Note that on TV, the TalkBack focus
    // automatically follows the input focus.
    if (FormFactorUtils.isAndroidTv()) {
      view.findViewById(R.id.training_page_wrapper)
          .setOnClickListener(
              clickedView -> {
                ((TrainingActivity) getActivity()).moveInputFocusToNavButtons();
              });

      pageLayout.requestFocus();
    }
    return view;
  }

  @Override
  public void onResume() {
    super.onResume();
    if (repeatedAnnouncingHandler != null) {
      LogUtils.v(TAG, "Idle announcement registered.");
      repeatedAnnouncingHandler.start();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (repeatedAnnouncingHandler != null) {
      LogUtils.v(TAG, "Idle announcement unregistered.");
      repeatedAnnouncingHandler.stop();
    }
  }

  public void setLinkHandler(LinkHandler linkHandler) {
    this.linkHandler = linkHandler;
  }

  public void setData(ServiceData data) {
    this.data = data;
  }

  /** Creates and adds all contents to the fragment. */
  private void addView(LayoutInflater inflater, ViewGroup container) {
    if (page == null) {
      LogUtils.e(TAG, "Cannot add view to fragment because no page.");
      return;
    }

    // Sets title.
    Title title =
        TutorialContentInterfaceInjector.getInstance()
            .getTutorialContentSupplier()
            .provideTitle(page);
    View titleView = title.createView(inflater, container, getContext(), data);
    addView(titleView);

    // Sets page number.
    int pageNumber = getArguments().getInt(EXTRA_PAGE_NUMBER);
    int totalNumber = getArguments().getInt(EXTRA_TOTAL_NUMBER);
    View pageNumberView = null;
    if (pageNumber > 0 && totalNumber > 0) {
      pageNumberView =
          new PageNumber(pageNumber, totalNumber)
              .createView(inflater, container, getContext(), data);
      addView(pageNumberView);
    }

    ImmutableList<PageContentConfig> contents = page.getContents();
    for (PageContentConfig content : contents) {
      // Adds Page banner if needed.
      if (tutorialContentManager.hasTutorialBannerContainer()
          && content instanceof ExitBanner exitBanner) {
        addPageBanner(exitBanner, inflater, container);
        continue;
      }
      addView(createPageContentView(content, inflater, container));
    }

    // Make container screenReaderFocusable.
    if (page.isOnlyOneFocus()) {
      ViewCompat.setScreenReaderFocusable(pageLayout, true);
    }

    // Make initial focus on pageNumber or training page layout but not on training banner.
    if (pageBannerLayout != null && pageBannerLayout.getChildCount() > 0) {
      setTrainingPageInitialFocus(
          (pageNumberView == null || page.isOnlyOneFocus()) ? pageLayout : pageNumberView);
    }
  }

  private void addPageBanner(ExitBanner content, LayoutInflater inflater, ViewGroup container) {
    if (data == null || !content.isNeedToShow(data)) {
      return;
    }

    View view = content.createView(inflater, container, getContext(), data);
    content.setRequestDisableTalkBack(
        () -> {
          try {
            ((TrainingActivity) getActivity()).onRequestDisableTalkBack();
          } catch (InterruptedException e) {
            throw new VerifyException(e);
          }
        });
    content.setMetricStore(metricStore);
    tutorialContentManager.addTutorialBanner(view);
  }

  @Nullable
  private View createPageContentView(
      PageContentConfig content, LayoutInflater inflater, ViewGroup container) {
    if (data != null && content.isNeedToShow(data)) {
      // Sets a click listener to send message to TalkBack for a PageButton.
      if (content instanceof PageButton pageButton) {
        View view = content.createView(inflater, container, getContext(), data);
        @Nullable Button button = pageButton.getButton();
        @Nullable Message message = pageButton.getMessage();
        if (button != null && message != null) {
          button.setOnClickListener(
              v -> {
                LogUtils.v(TAG, "Sends a message to service. what=%d", message.what);
                ((TrainingActivity) getActivity()).checkAndSendMessageToService(message);
              });
        }
        return view;
      }

      // For the navigation contents, like Link and button.
      if (content instanceof Link link) {
        link.setLinkHandler(linkHandler);
      }
      return content.createView(inflater, container, getContext(), data);
    } else {
      PageContentConfig substitute = content.getSubstitute();
      if (substitute == null) {
        return null;
      }
      return createPageContentView(substitute, inflater, container);
    }
  }

  /** Adds the view for the given content to the page. */
  private void addView(@Nullable View view) {
    if (view == null) {
      // No view has to be shown on the screen.
      return;
    }

    if (page == null) {
      LogUtils.e(TAG, "Cannot add view to fragment because no page.");
      return;
    }

    if (page.isOnlyOneFocus()) {
      // Entire page is spoken continuously. The focus is on the first child (pageLayout) of
      // ViewPager, so the content view and its descendant views are not focusable.
      view.setFocusable(false);
    }
    tutorialContentManager.addTutorialContentView(view);
  }

  private void setTrainingPageInitialFocus(View view) {
    if (view != null) {
      view.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
      ViewCompat.setAccessibilityDelegate(
          view,
          new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                View host, AccessibilityNodeInfoCompat info) {
              super.onInitializeAccessibilityNodeInfo(host, info);
              info.setRequestInitialAccessibilityFocus(true);
            }
          });
    }
  }
}
