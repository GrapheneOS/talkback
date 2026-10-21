/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.google.android.accessibility.talkback.monitor;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.PowerManager;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.Interpretation;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.Pipeline.InterpretationReceiver;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.utils.broadcast.SameThreadBroadcastReceiver;
import org.checkerframework.checker.nullness.qual.NonNull;

/** Monitor battery charging status changes. Start charging Stop changing */
public class BatteryMonitor extends SameThreadBroadcastReceiver {
  private final Context context;

  private Pipeline.InterpretationReceiver pipeline;

  public static final int UNKNOWN_LEVEL = -1;
  private int batteryLevel = UNKNOWN_LEVEL;
  private boolean powerConnected;
  private boolean powerSaveMode;

  public BatteryMonitor(Context context) {
    this.context = context;
    powerSaveMode = getPowerSaveModeState();
    BatteryManager batteryManager =
        (BatteryManager) this.context.getSystemService(Context.BATTERY_SERVICE);
    powerConnected = batteryManager.isCharging();
  }

  public void setPipeline(@NonNull InterpretationReceiver pipeline) {
    if (pipeline == null) {
      throw new IllegalStateException();
    }
    this.pipeline = pipeline;
  }

  public IntentFilter getFilter() {
    final IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
    intentFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
    intentFilter.addAction(Intent.ACTION_POWER_CONNECTED);
    intentFilter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
    return intentFilter;
  }

  @Override
  public void onReceiveIntent(Intent intent) {
    final String action = intent.getAction();
    if (action == null) {
      return;
    }

    switch (action) {
      case Intent.ACTION_BATTERY_CHANGED -> {
        int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
        int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
        batteryLevel = getBatteryLevel(scale, level);
      }
      case Intent.ACTION_POWER_DISCONNECTED -> {
        powerConnected = false;
        pipeline.input(
            EVENT_ID_UNTRACKED,
            new Interpretation.Power(
                /* connected= */ powerConnected, batteryLevel, /* powerSaveMode= */ powerSaveMode));
      }
      case Intent.ACTION_POWER_CONNECTED -> {
        powerConnected = true;
        pipeline.input(
            EVENT_ID_UNTRACKED,
            new Interpretation.Power(
                /* connected= */ powerConnected, batteryLevel, /* powerSaveMode= */ powerSaveMode));
      }
      case PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> powerSaveMode = getPowerSaveModeState();
      default -> {
        // Do nothing.
      }
    }
  }

  @VisibleForTesting
  public static int getBatteryLevel(int scale, int level) {
    return (scale > 0 ? Math.round((level / (float) scale) * 100) : UNKNOWN_LEVEL);
  }

  public String getBatteryStateDescription() {
    return context.getString(
        R.string.template_battery_state,
        powerConnected ? context.getString(R.string.notification_type_status_connected) : "",
        batteryLevel != UNKNOWN_LEVEL
            ? String.valueOf(batteryLevel)
            : context.getString(R.string.notification_battery_level_unknown),
        powerSaveMode ? context.getString(R.string.notification_battery_saver_mode) : "");
  }

  private boolean getPowerSaveModeState() {
    PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    return powerManager.isPowerSaveMode();
  }
}
