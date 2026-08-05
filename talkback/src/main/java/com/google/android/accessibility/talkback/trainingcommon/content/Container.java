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

package com.google.android.accessibility.talkback.trainingcommon.content;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;
import com.google.android.accessibility.utils.FeatureSupport;

/** Container view has title, subtext and a container title. */
public class Container extends PageContentConfig {

  private final boolean isKeyboardTutorial;

  public Container() {
    this(/* isKeyboardTutorial= */ false);
  }

  public Container(boolean isKeyboardTutorial) {
    this.isKeyboardTutorial = isKeyboardTutorial;
  }

  @Override
  public View createView(
      LayoutInflater inflater, ViewGroup container, Context context, ServiceData data) {
    final View view = inflater.inflate(R.layout.training_container, container, false);
    View containerLayout = view.findViewById(R.id.container_layout);
    ViewCompat.setAccessibilityDelegate(
        containerLayout,
        new AccessibilityDelegateCompat() {
          @Override
          public void onInitializeAccessibilityNodeInfo(
              @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
            super.onInitializeAccessibilityNodeInfo(host, info);
            info.setContainerTitle(context.getString(R.string.container_tem_title));
          }
        });

    final int containerItemSubText;
    final int containerItemExitSubText;
    if (isKeyboardTutorial) {
      containerItemSubText = R.string.keyboard_tutorial_go_to_next_container;
      containerItemExitSubText = R.string.keyboard_tutorial_exit_container;
    } else {
      containerItemSubText =
          FeatureSupport.isMultiFingerGestureSupported()
              ? R.string.container_item_subtext
              : R.string.container_item_subtext_pre_r;
      containerItemExitSubText =
          FeatureSupport.isMultiFingerGestureSupported()
              ? R.string.container_item_exit_subtext
              : R.string.container_item_exit_subtext_pre_r;
    }

    addContainerItem(
        view,
        R.id.training_container_title1,
        context.getString(R.string.container_tem_title) + " " + 1,
        R.id.training_container_subtext1,
        containerItemSubText);
    addContainerItem(
        view,
        R.id.training_container_title2,
        context.getString(R.string.container_tem_title) + " " + 2,
        R.id.training_container_subtext2,
        containerItemSubText);
    addContainerItem(
        view,
        R.id.training_container_title3,
        context.getString(R.string.container_tem_title) + " " + 3,
        R.id.training_container_subtext3,
        containerItemSubText);
    addContainerItem(
        view,
        R.id.training_container_title4,
        context.getString(R.string.container_tem_title) + " " + 4,
        R.id.training_container_subtext4,
        containerItemSubText);
    addContainerItem(
        view,
        R.id.training_container_title5,
        context.getString(R.string.container_tem_title) + " " + 5,
        R.id.training_container_subtext5,
        containerItemSubText);
    addContainerItem(
        view,
        R.id.training_container_title6,
        context.getString(R.string.container_tem_title) + " " + 6,
        R.id.training_container_subtext6,
        containerItemExitSubText);
    return view;
  }

  private void addContainerItem(
      View view, int titleResId, String titleString, int subTextResId, int subTextStringRes) {
    TextView title = view.findViewById(titleResId);
    if (title != null) {
      title.setText(titleString);
    }
    TextView subText = view.findViewById(subTextResId);
    if (subText != null) {
      subText.setText(subTextStringRes);
    }
  }
}
