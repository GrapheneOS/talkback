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

package com.google.android.accessibility.braille.brailledisplay.platform.connect.device;

import android.text.TextUtils;

/** Connectable device. */
public abstract class ConnectableDevice {
  private boolean useHid = false;

  /** The name of the connectable device. */
  public abstract String name();

  /** The truncated name of the connectable device. */
  public String truncatedName() {
    String name = name();
    if (!TextUtils.isEmpty(name)) {
      char[] masked = name().toCharArray();
      for (int i = 0; i < masked.length; i++) {
        if (masked[i] == ' ') {
          continue;
        }
        if (i >= masked.length / 2) {
          masked[i] = '*';
        }
      }
      name = new String(masked);
    }
    return name;
  }

  /** The vendor id of the connectable device. */
  public int vendorId() {
    return 0;
  }

  /** The product id of the connectable device. */
  public int productId() {
    return 0;
  }

  /** The address of the connectable device. */
  public abstract String address();

  /** Sets use HID protocol or not. */
  public void setUseHid(boolean useHid) {
    this.useHid = useHid;
  }

  /** Returns to use HID protocol or not. */
  public boolean useHid() {
    return useHid;
  }

  /** Returns a string with masked name and address. */
  @Override
  public String toString() {
    return truncatedName() + "(" + address() + ")";
  }
}
