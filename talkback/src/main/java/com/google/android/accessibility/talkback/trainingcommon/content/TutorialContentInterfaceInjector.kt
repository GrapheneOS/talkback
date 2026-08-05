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

package com.google.android.accessibility.talkback.trainingcommon.content

import android.content.Context
import android.view.View
import android.view.View.OnClickListener
import androidx.annotation.ArrayRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.google.android.accessibility.talkback.training.content.TutorialContentManagerImpl
import com.google.android.accessibility.talkback.trainingcommon.NavigationButtonBar
import com.google.android.accessibility.talkback.trainingcommon.PageConfig

class TutorialContentInterfaceInjector
private constructor(private val tutorialContentSupplier: TutorialContentSupplier) {
  interface TutorialContentSupplier {
    fun provideText(vararg paragraphs: Text.Paragraph): Text

    fun provideWebView(@StringRes textResId: Int): WebViewContentConfig

    fun provideTitle(pageConfig: PageConfig): Title

    fun providePageIconButton(
      @DrawableRes drawableResId: Int,
      @StringRes contentDescriptionResId: Int,
      clickListener: OnClickListener,
    ): PageContentConfig? // We would make it more specific after the form factors other than Wear

    // have their implementation

    fun provideTextList(@ArrayRes titlesResId: Int, @ArrayRes summariesResId: Int): TextList

    fun provideExitBanner(predicate: PageConfig.PageAndContentPredicate): ExitBanner

    fun provideNavigationButton(
      context: Context,
      @NavigationButtonBar.ButtonType buttonType: Int,
      @StringRes text: Int,
      onClickListener: OnClickListener,
      buttonCount: Int,
      currentPageNumber: Int,
    ): View?
  }

  interface TutorialContentManager {
    fun addTutorialContentView(tutorialContentView: View)

    fun addTutorialBanner(bannerView: View)

    fun hasTutorialBannerContainer(): Boolean

    fun addNavigationContentView(navigationContentView: View)
  }

  fun getTutorialContentSupplier(): TutorialContentSupplier {
    return tutorialContentSupplier
  }

  fun newTutorialContentManager(
    parentView: View,
    bannerLayout: View?,
    navigationBarContainer: View?,
  ): TutorialContentManager {
    return TutorialContentManagerImpl(parentView, bannerLayout, navigationBarContainer)
  }

  companion object {

    private lateinit var tutorialContentInterfaceInjector: TutorialContentInterfaceInjector

    @JvmStatic
    fun initialize(tutorialContentSupplier: TutorialContentSupplier) {
      tutorialContentInterfaceInjector = TutorialContentInterfaceInjector(tutorialContentSupplier)
    }

    @JvmStatic
    fun getInstance(): TutorialContentInterfaceInjector {
      if (!::tutorialContentInterfaceInjector.isInitialized) {
        throw IllegalStateException(
          "The instance of TutorialContentInterfaceInjector hasn't been initialized yet."
        )
      }
      return tutorialContentInterfaceInjector
    }
  }
}
