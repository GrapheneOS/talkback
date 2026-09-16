/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.accessibility.talkback.actor.gemini.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.dialog.BaseDialog;
import com.google.android.accessibility.talkback.imagecaption.Result;
import java.util.Optional;

/** Dialog for showing the detailed image description result. */
public class CaptionResultDialog extends BaseDialog {

  private final Optional<Result> imageDescriptionResult;
  private final Optional<Result> iconLabelResult;
  private final Optional<Result> ocrTextResult;
  private final Optional<Result> screenOverviewResult;

  public CaptionResultDialog(
      Context context,
      Result imageDescriptionResult,
      Result iconLabelResult,
      Result ocrTextResult) {
    super(context, R.string.title_gemini_result_dialog, /* pipeline= */ null);
    this.imageDescriptionResult = Optional.ofNullable(imageDescriptionResult);
    this.iconLabelResult = Optional.ofNullable(iconLabelResult);
    this.ocrTextResult = Optional.ofNullable(ocrTextResult);
    screenOverviewResult = Optional.empty();
    setIncludeNegativeButton(false);
    setPositiveButtonStringRes(R.string.positive_button_gemini_result_dialog);
  }

  /** The dialog result for screen description. */
  public CaptionResultDialog(Context context, Result screenOverviewResult) {
    super(context, R.string.title_gemini_screen_overview_result_dialog, /* pipeline= */ null);
    this.screenOverviewResult = Optional.ofNullable(screenOverviewResult);
    imageDescriptionResult = Optional.empty();
    iconLabelResult = Optional.empty();
    ocrTextResult = Optional.empty();
    setIncludeNegativeButton(false);
    setPositiveButtonStringRes(R.string.positive_button_gemini_result_dialog);
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    // Do nothing.
  }

  @Override
  public void handleDialogDismiss() {
    // Do nothing.
  }

  @Override
  public String getMessageString() {
    return null;
  }

  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    FrameLayout tempParent = new FrameLayout(context);
    ScrollView dialogView =
        (ScrollView) inflater.inflate(R.layout.caption_result_dialog, tempParent, false);
    if (screenOverviewResult.isPresent()) {
      CharSequence screenDescriptionResultText = getTextFromResult(screenOverviewResult);
      TextView imageDescriptionResultView = dialogView.findViewById(R.id.image_description_result);
      imageDescriptionResultView.setText(screenDescriptionResultText);
    } else {
      CharSequence imageDescriptionResultText = getTextFromResult(imageDescriptionResult);
      CharSequence iconDetectionResultText = getTextFromResult(iconLabelResult);
      CharSequence ocrResultText = getTextFromResult(ocrTextResult);

      TextView imageDescriptionResultView = dialogView.findViewById(R.id.image_description_result);
      imageDescriptionResultView.setText(imageDescriptionResultText);

      if (!TextUtils.isEmpty(iconDetectionResultText)) {
        TextView iconDetectionResultView = dialogView.findViewById(R.id.icon_detection_result);
        iconDetectionResultView.setVisibility(View.VISIBLE);
        iconDetectionResultView.setText(
            context.getString(
                R.string.detailed_image_description_icon_result, iconDetectionResultText));
      }

      if (!TextUtils.isEmpty(ocrResultText)) {
        TextView ocrResulView = dialogView.findViewById(R.id.ocr_result);
        ocrResulView.setVisibility(View.VISIBLE);
        ocrResulView.setText(
            context.getString(R.string.detailed_image_description_ocr_result, ocrResultText));
      }
    }

    return dialogView;
  }

  private static CharSequence getTextFromResult(Optional<Result> result) {
    return result.map(Result::text).orElse("");
  }
}
