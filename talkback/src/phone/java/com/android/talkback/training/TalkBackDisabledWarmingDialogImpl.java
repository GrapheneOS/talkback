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

package com.google.android.accessibility.talkback.training;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.trainingcommon.TrainingActivityInterfaceInjector.TalkBackDisabledWarmingDialog;
import com.google.android.accessibility.talkback.trainingcommon.TrainingUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import java.util.List;
import java.util.function.Consumer;

/** Provides the warming dialog shown in the tutorial page when TalkBack is disabled. */
public class TalkBackDisabledWarmingDialogImpl implements TalkBackDisabledWarmingDialog {

  @Override
  public void show(FragmentActivity fragmentActivity, Consumer<Boolean> checkBoxConsumer) {
    LayoutInflater inflater = LayoutInflater.from(fragmentActivity);
    final View root =
        inflater.inflate(R.layout.do_not_show_again_checkbox_dialog, /* root= */ null);
    CheckBox doNotShowAgainCheckBox = root.findViewById(R.id.dont_show_again);
    doNotShowAgainCheckBox.setText(R.string.do_not_show_again_check_box);
    doNotShowAgainCheckBox.setOnCheckedChangeListener(
        (buttonView, isChecked) -> checkBoxConsumer.accept(isChecked));
    TextView contentTextView = root.findViewById(R.id.dialog_content);
    contentTextView.setText(R.string.talkback_inactive_warning_message);

    A11yAlertDialogWrapper.materialDialogBuilder(
            fragmentActivity, fragmentActivity.getSupportFragmentManager())
        .setView(root)
        .setTitle(R.string.talkback_inactive_title)
        .setCancelable(true)
        .setNegativeButton(
            R.string.talkback_inactive_go_to_settings_button,
            (dialog, which) -> lunchTalkBackSettingsIfAvailable(fragmentActivity))
        .setPositiveButton(R.string.talkback_inactive_warning_positive_button, null)
        .create()
        .show();
  }

  private void lunchTalkBackSettingsIfAvailable(FragmentActivity fragmentActivity) {
    Intent intent = TrainingUtils.getAccessibilitySettingsAndHighLightTalkBackIntent();
    PackageManager pm = fragmentActivity.getPackageManager();
    List<ResolveInfo> activities = pm.queryIntentActivities(intent, 0);
    if (!activities.isEmpty()) {
      fragmentActivity.startActivity(intent);
    }
  }
}
