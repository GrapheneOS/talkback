/*
 * Copyright (C) 2025 Google Inc.
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
package com.google.android.accessibility.braille.brltty;

import com.google.android.apps.common.proguard.UsedByNative;

/** Usb device entry. */
@UsedByNative("BrlttyWrapper.c")
public class UsbDeviceEntry {
  private final int vendorId;
  private final int productId;
  private final String driver;

  @UsedByNative("BrlttyWrapper.c")
  public UsbDeviceEntry(String driver, int vendorId, int productId) {
    this.driver = driver;
    this.vendorId = vendorId;
    this.productId = productId;
  }

  /** Returns the vendor id of the USB device. */
  public int getVendorId() {
    return vendorId;
  }

  /** Returns the product id of the USB device. */
  public int getProductId() {
    return productId;
  }

  /** Returns the driver of the bluetooth device. */
  public String getDriver() {
    return driver;
  }
}
