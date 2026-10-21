package com.google.android.accessibility.utils;

import android.content.Context;
import androidx.core.i18n.MessageFormat;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** A MessageFormat compatible class to support different variant. */
public final class AccessibilityMessageFormat {

  public static String formatNamedArgs(Context context, String message, Object... nameValuePairs) {
    Map<String, Object> nameValueMap = new HashMap<>();
    for(int i = 0; i < nameValuePairs.length; i+=2) {
      nameValueMap.put(String.valueOf(nameValuePairs[i]), nameValuePairs[i+1]);
    }
    return MessageFormat.format(context, Locale.getDefault(), message, nameValueMap);
  }

  private AccessibilityMessageFormat() {}
}
