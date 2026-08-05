/*
 * Copyright (C) 2023 Google Inc.
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

import static com.google.android.accessibility.talkback.analytics.TalkBackAnalytics.TRAINING_BUTTON_TURN_OFF_TALKBACK;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.flags.FeatureFlagReader;
import com.google.android.accessibility.talkback.trainingcommon.TrainingIpcClient.ServiceData;
import com.google.android.accessibility.talkback.trainingcommon.TrainingMetricStore;
import com.google.android.accessibility.utils.monitor.InputDeviceMonitor;

/**
 * A {@link PageContentConfig}. It has a TalkBack exit banner UI that and a TalkBack-exit button for
 * TalkBack mis-triggering recovery. The user can turn off TalkBack settings by tapping the button.
 */
public class ExitBanner extends PageContentConfig {

  /** Interface for tutorial activity to request disabling TalkBack. */
  public interface RequestDisableTalkBack {
    void onRequestDisableTalkBack();
  }

  private RequestDisableTalkBack requestDisableTalkBack;

  private TrainingMetricStore metricStore;

  private boolean firstTapPerformed;
  private final InputDeviceMonitor inputDeviceMonitor;

  protected OnClickListener clickListener =
      (View exitButton) -> {
        // The first click changes the button label to remind the user to click again to turn off
        // TalkBack.
        if (firstTapPerformed) {
          performExit();
          firstTapPerformed = false;
        } else {
          firstTapPerformed = true;
          if (exitButton instanceof TextView textView) {
            textView.setText(R.string.tap_again_to_turn_off);
          }
        }
      };

  protected OnTouchListener mouseTouchListener =
      (View v, MotionEvent event) -> {
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_MOUSE
            && event.getAction() == MotionEvent.ACTION_UP) {
          performExit();
          return true;
        }
        return false;
      };

  public ExitBanner() {
    this.inputDeviceMonitor = null;
  }

  @VisibleForTesting
  ExitBanner(InputDeviceMonitor inputDeviceMonitor) {
    this.inputDeviceMonitor = inputDeviceMonitor;
  }

  @Override
  public View createView(
      LayoutInflater inflater, ViewGroup container, Context context, ServiceData data) {
    firstTapPerformed = false;
    View view = inflater.inflate(R.layout.training_exit_banner, container, false);
    Button exitButton = view.findViewById(R.id.training_exit_talkback_button);
    TextView descriptionText = view.findViewById(R.id.talkback_description_text);

    InputDeviceMonitor monitor =
        (this.inputDeviceMonitor != null)
            ? this.inputDeviceMonitor
            : new InputDeviceMonitor(context);

    if (FeatureFlagReader.enableShowTalkbackKeyboardTutorial(context)
        && monitor.hasPointingDevice()
        && monitor.hasTouchScreen()) {
      descriptionText.setText(R.string.talkback_turn_off_description_pointer_and_touch);
      exitButton.setOnTouchListener(mouseTouchListener);
      // TODO: b/442630049 Consider new string for non-touch devices.
    } else {
      descriptionText.setText(R.string.talkback_turn_off_description);
    }

    exitButton.setLongClickable(false);
    exitButton.setOnClickListener(clickListener);
    return view;
  }

  public void setRequestDisableTalkBack(RequestDisableTalkBack callback) {
    requestDisableTalkBack = callback;
  }

  public void setMetricStore(TrainingMetricStore metricStore) {
    this.metricStore = metricStore;
  }

  private void performExit() {
    if (metricStore != null) {
      metricStore.onTutorialEvent(TRAINING_BUTTON_TURN_OFF_TALKBACK);
    }
    if (requestDisableTalkBack != null) {
      requestDisableTalkBack.onRequestDisableTalkBack();
    }
  }
}
