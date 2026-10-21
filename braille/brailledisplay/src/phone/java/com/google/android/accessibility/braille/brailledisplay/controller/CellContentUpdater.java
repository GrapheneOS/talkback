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

package com.google.android.accessibility.braille.brailledisplay.controller;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/** Triggers the giving runnable at giving time in millis. */
public class CellContentUpdater {
  private final Handler handler = new MainHandler();

  /** Schedule specified updates at a specified frequency. */
  public void schedule(int what, Runnable runnable, int frequencyMillis) {
    handler.removeMessages(what);
    // If runnable is set as callback, handleMessage won't be called.
    Message message = Message.obtain();
    message.what = what;
    message.arg1 = frequencyMillis;
    message.obj = runnable;
    handler.sendMessageDelayed(message, frequencyMillis);
  }

  /** Schedule specified updates. */
  public void cancel(int what) {
    handler.removeMessages(what);
  }

  /** Cancels all updates. */
  public void cancelAll() {
    handler.removeCallbacksAndMessages(null);
  }

  private static class MainHandler extends Handler {
    public MainHandler() {
      super(Looper.getMainLooper());
    }

    @Override
    public void handleMessage(Message msg) {
      ((Runnable) msg.obj).run();
      Message message = Message.obtain();
      message.what = msg.what;
      message.arg1 = msg.arg1;
      message.obj = msg.obj;
      sendMessageDelayed(message, msg.arg1);
    }
  }
}
