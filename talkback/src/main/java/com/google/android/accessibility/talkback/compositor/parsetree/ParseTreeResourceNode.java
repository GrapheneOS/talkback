/*
 * Copyright (C) 2016 The Android Open Source Project
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

package com.google.android.accessibility.talkback.compositor.parsetree;

import android.content.res.Resources;
import androidx.annotation.IntDef;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParseTreeResourceNode extends ParseTreeNode {

  private static final String TAG = "ParseTreeResourceNode";

  private static final Pattern RESOURCE_PATTERN =
      Pattern.compile("@(string|plurals|raw|array)/(\\w+)");

  @IntDef({TYPE_STRING, TYPE_PLURALS, TYPE_RESOURCE_ID})
  @Retention(RetentionPolicy.SOURCE)
  @interface Type {}

  static final int TYPE_STRING = 0;
  static final int TYPE_PLURALS = 1;
  static final int TYPE_RESOURCE_ID = 2;

  private final Resources mResources;
  private final int mResourceId;
  @Type private final int mType;
  private final List<ParseTreeNode> mParams = new ArrayList<>();

  ParseTreeResourceNode(Resources resources, String resourceName, String packageName) {
    mResources = resources;

    Matcher matcher = RESOURCE_PATTERN.matcher(resourceName);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("Resource parameter is malformed: " + resourceName);
    }

    String type = matcher.group(1);
    String name = matcher.group(2);
    if (type == null || name == null) {
      throw new IllegalArgumentException("Resource parameter is malformed: " + resourceName);
    }

    mType =
        switch (type) {
          case "string" -> TYPE_STRING;
          case "plurals" -> TYPE_PLURALS;
          case "raw", "array" -> TYPE_RESOURCE_ID;
          default -> throw new IllegalArgumentException("Unknown resource type: " + type);
        };

    mResourceId = mResources.getIdentifier(name, type, packageName);

    if (mResourceId == 0) {
      throw new IllegalStateException("Missing resource: " + resourceName);
    }
  }

  void addParams(List<ParseTreeNode> params) {
    mParams.addAll(params);
  }

  @Override
  public int getType() {
    switch (mType) {
      case TYPE_STRING:
      case TYPE_PLURALS:
        return ParseTree.VARIABLE_STRING;

      case TYPE_RESOURCE_ID:
      default:
        return ParseTree.VARIABLE_INTEGER;
    }
  }

  @Override
  public int resolveToInteger(ParseTree.VariableDelegate delegate, String logIndent) {
    return mResourceId;
  }

  @Override
  public CharSequence resolveToString(ParseTree.VariableDelegate delegate, String logIndent) {
    switch (mType) {
      case TYPE_STRING -> {
        Object[] stringParamList = getParamList(mParams, 0, delegate, logIndent);
        String templateString = mResources.getString(mResourceId);
        return SpannableUtils.getSpannedFormattedString(templateString, stringParamList);
      }
      case TYPE_PLURALS -> {
        if (mParams.isEmpty() || mParams.get(0).getType() != ParseTree.VARIABLE_INTEGER) {
          LogUtils.e(TAG, "First parameter for plurals must be the count");
          return "";
        }

        Object[] pluralParamList = getParamList(mParams, 1, delegate, logIndent);
        String templatePlural =
            mResources.getQuantityString(
                mResourceId, mParams.get(0).resolveToInteger(delegate, logIndent));
        return SpannableUtils.getSpannedFormattedString(templatePlural, pluralParamList);
      }
      case TYPE_RESOURCE_ID -> {
        LogUtils.e(TAG, "Cannot resolve resource ID to string");
        return "";
      }
      default -> {
        LogUtils.e(TAG, "Unknown resource type: " + mType);
        return "";
      }
    }
  }

  private static Object[] getParamList(
      List<ParseTreeNode> params,
      int start,
      ParseTree.VariableDelegate delegate,
      String logIndent) {
    List<Object> result = new ArrayList<>();
    for (ParseTreeNode node : params.subList(start, params.size())) {
      int type = node.getType();
      if (type == ParseTree.VARIABLE_BOOL) {
        result.add(node.resolveToBoolean(delegate, logIndent));
      } else if (type == ParseTree.VARIABLE_STRING) {
        result.add(node.resolveToString(delegate, logIndent));
      } else if (type == ParseTree.VARIABLE_INTEGER) {
        result.add(node.resolveToInteger(delegate, logIndent));
      } else if (type == ParseTree.VARIABLE_NUMBER) {
        result.add(node.resolveToNumber(delegate, logIndent));
      } else if (type == ParseTree.VARIABLE_ENUM || type == ParseTree.VARIABLE_ARRAY
          || type == ParseTree.VARIABLE_CHILD_ARRAY) {
        LogUtils.e(TAG, "Cannot format string with type: " + node.getType());
      }
    }
    return result.toArray();
  }
}
