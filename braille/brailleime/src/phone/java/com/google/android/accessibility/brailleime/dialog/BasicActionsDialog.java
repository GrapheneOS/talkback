/*
 * Copyright 2020 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.brailleime.dialog;

import static com.google.android.accessibility.brailleime.SupportedCommand.Category.BASIC;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BulletSpan;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.annotation.ColorInt;
import androidx.core.content.res.ResourcesCompat;
import com.google.android.accessibility.brailleime.R;
import com.google.android.accessibility.brailleime.SupportedCommand;
import com.google.android.accessibility.brailleime.settings.BrailleImeGestureActivity;
import com.google.android.accessibility.utils.BuildVersionUtils;
import com.google.android.accessibility.utils.material.A11yAlertDialogWrapper;
import com.google.android.material.color.MaterialColors;
import java.util.function.Function;

/** A dialog controller that presents basic actions for braille keyboard. */
public class BasicActionsDialog extends ViewAttachedDialog {
  /** Callback for basic gestures dialog. */
  public interface BasicActionsCallback {
    void onClickMoreGestures();
  }

  private final Context context;
  private final BasicActionsCallback callback;

  public BasicActionsDialog(Context context, BasicActionsCallback callback) {
    this.context = context;
    this.callback = callback;
  }

  @Override
  protected Dialog makeDialog() {
    // To sync with dialog in the Settings, the dialog will use v7 AlertDialog (Target to change to
    // material dialog on T). The other situation, the dialog will use material dialog.
    CharSequence[] list =
        SupportedCommand.getSupportedCommands(context).stream()
            .filter((SupportedCommand supportedCommand) -> supportedCommand.getCategory() == BASIC)
            .map(
                (Function<SupportedCommand, CharSequence>)
                    command -> {
                      SpannableString spannableString =
                          SpannableString.valueOf(
                              context.getString(
                                  R.string.review_gesture_format,
                                  command.getActionDescription(context),
                                  command.getGestureDescription(context)));
                      setBulletSpan(context, spannableString);
                      return spannableString;
                    })
            .toArray(CharSequence[]::new);
    Context dialogContext = Dialogs.getDialogContext(context);
    final ArrayAdapter<CharSequence> arrayAdapter =
        new ArrayAdapter<>(dialogContext, android.R.layout.simple_list_item_1, list) {
          @Override
          public boolean areAllItemsEnabled() {
            return false;
          }

          @Override
          public boolean isEnabled(int position) {
            return false;
          }
        };
    ListView listView = new ListView(context);
    listView.setDivider(null);
    listView.setAdapter(arrayAdapter);
    A11yAlertDialogWrapper.Builder builder =
        A11yAlertDialogWrapper.materialDialogBuilder(dialogContext)
            .setTitle(context.getString(R.string.braille_keyboard_basic_controls))
            .setView(listView)
            .setNegativeButton(
                context.getString(R.string.review_gestures_review_all_button),
                (dialog, which) -> {
                  Intent intent = new Intent(context, BrailleImeGestureActivity.class);
                  intent.addFlags(
                      Intent.FLAG_ACTIVITY_NEW_TASK
                          | Intent.FLAG_ACTIVITY_CLEAR_TOP
                          | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                  context.startActivity(intent);
                  callback.onClickMoreGestures();
                })
            .setPositiveButton(
                context.getString(R.string.review_gestures_close_button), /* listener= */ null);
    return builder.create().getDialog();
  }

  private static void setBulletSpan(Context context, SpannableString spannableString) {
    @ColorInt
    int textColor =
        MaterialColors.getColor(
            context, android.R.attr.textColorPrimary, "Unsupported color palette");
    if (BuildVersionUtils.isAtLeastP()) {
      spannableString.setSpan(
          new BulletSpan(
              (int) ResourcesCompat.getFloat(context.getResources(), R.dimen.gap_width),
              textColor,
              (int) ResourcesCompat.getFloat(context.getResources(), R.dimen.bullet_radius)),
          /* start= */ 0,
          /* end= */ 1,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    } else {
      spannableString.setSpan(
          new BulletSpan(
              (int) ResourcesCompat.getFloat(context.getResources(), R.dimen.gap_width), textColor),
          /* start= */ 0,
          /* end= */ 1,
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }
}
