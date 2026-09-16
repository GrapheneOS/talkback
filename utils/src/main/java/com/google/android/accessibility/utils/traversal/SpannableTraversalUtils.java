/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.accessibility.utils.traversal;

import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.CharacterStyle;
import android.text.style.ClickableSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.SpannableUtils;
import com.google.android.accessibility.utils.SpannableUtils.SpannableWithOffset;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Utility methods for traversing a tree with spannable objects. */
public final class SpannableTraversalUtils {

  private static final String TAG = "SpannableTraversalUtils";

  /**
   * Returns whether the node hierarchy contains target {@link ClickableSpan}.
   *
   * <p><b>Note: {@code targetClickableSpanClass} should be able to be parcelable and transmitted by
   * IPC which depends on the implementation of {@link AccessibilityNodeInfo#setText(CharSequence)}
   * in the framework side.</b>
   */
  public static boolean hasTargetClickableSpanInNodeTree(
      AccessibilityNodeInfoCompat node, Class<? extends ClickableSpan> targetClickableSpanClass) {
    if (node == null) {
      return false;
    }
    Set<AccessibilityNodeInfoCompat> visitedNode = new HashSet<>();
    return searchSpannableStringsForCharacterStyleInNodeTree(
        node,
        visitedNode,
        /* currentOffset= */ null,
        /* result= */ null,
        Arrays.asList(targetClickableSpanClass));
  }

  /** Returns whether the node hierarchy contains target {@link CharacterStyle} subclasses. */
  public static boolean hasTargetCharcterStyleInNodeTree(
      AccessibilityNodeInfoCompat node, List<Class<? extends CharacterStyle>> spanClasses) {
    if (node == null) {
      return false;
    }
    Set<AccessibilityNodeInfoCompat> visitedNode = new HashSet<>();
    return searchSpannableStringsForCharacterStyleInNodeTree(
        node, visitedNode, /* currentOffset= */ null, /* result= */ null, spanClasses);
  }

  /**
   * Gets {@link SpannableWithOffset} with target {@link ClickableSpan} within the node tree.
   *
   * <p><b>Note: {@code targetClickableSpanClass} should be able to be parcelable and transmitted by
   * IPC which depends on the implementation of {@link AccessibilityNodeInfo#setText(CharSequence)}
   * in the framework side.</b>
   */
  public static void getSpannableStringsWithTargetClickableSpanInNodeTree(
      AccessibilityNodeInfoCompat node,
      Class<? extends ClickableSpan> targetClickableSpanClass,
      @NonNull List<SpannableWithOffset> result) {
    if (node == null) {
      return;
    }
    Set<AccessibilityNodeInfoCompat> visitedNodes = new HashSet<>();
    searchSpannableStringsForCharacterStyleInNodeTree(
        node, visitedNodes, new AtomicInteger(0), result, Arrays.asList(targetClickableSpanClass));
  }

  /**
   * Gets {@link SpannableWithOffset} with target {@link CharacterStyle} subclasses within the node
   * tree.
   */
  public static void getSpannableStringsWithTargetCharacterStyleInNodeTree(
      AccessibilityNodeInfoCompat node,
      List<Class<? extends CharacterStyle>> spanClasses,
      @NonNull List<SpannableWithOffset> result) {
    if (node == null) {
      return;
    }
    Set<AccessibilityNodeInfoCompat> visitedNodes = new HashSet<>();
    searchSpannableStringsForCharacterStyleInNodeTree(
        node, visitedNodes, new AtomicInteger(0), result, spanClasses);
  }

  /**
   * Search for {@link SpannableWithOffset} under <strong>node tree</strong> from {@code root}.
   *
   * <p><b>Note: {@code root} will be added to {@code visitedNodes} if it's not null.</b>
   *
   * <p><b>Note: {@code spanClasses} should be able to be parcelable and transmitted by IPC which
   * depends on the implementation of {@link AccessibilityNodeInfo#setText(CharSequence)} in the
   * framework side.</b>
   *
   * @param root root of node tree
   * @param visitedNodes a set of {@link AccessibilityNodeInfoCompat} to record visited nodes, used
   *     to avoid loops.
   * @param currentOffset the offset of the spannable string in the node tree.
   * @param result a list of {@link SpannableWithOffset} collected from node tree
   * @param spanClasses the class of target CharacterStyle subclasses.
   * @return {@code true} if any SpannableString is found in the node tree
   */
  @CanIgnoreReturnValue
  private static boolean searchSpannableStringsForCharacterStyleInNodeTree(
      AccessibilityNodeInfoCompat root,
      @NonNull Set<AccessibilityNodeInfoCompat> visitedNodes,
      @Nullable AtomicInteger currentOffset,
      @Nullable List<SpannableWithOffset> result,
      List<Class<? extends CharacterStyle>> spanClasses) {
    if (root == null) {
      return false;
    }
    if (!visitedNodes.add(root)) {
      // Root already visited. Stop searching.
      return false;
    }
    SpannableString string = SpannableUtils.getSpannableStringWithCharacterStyle(root, spanClasses);
    boolean hasSpannableString = !TextUtils.isEmpty(string);
    if (hasSpannableString) {
      if (result == null) {
        // If we don't need to collect result and we found a Spannable String, return true.
        return true;
      } else {
        result.add(
            new SpannableWithOffset(string, currentOffset == null ? 0 : currentOffset.get()));
      }
    }

    if (currentOffset != null && root.getText() != null) {
      currentOffset.addAndGet(root.getText().length());
    }

    // TODO: Check if we should search descendants of web content node.
    if (!TextUtils.isEmpty(root.getContentDescription())) {
      // If root has content description, do not search the children nodes.
      LogUtils.v(TAG, "Root has content description, skipping searching the children nodes.");
      return hasSpannableString;
    }
    ReorderedChildrenIterator iterator = ReorderedChildrenIterator.createAscendingIterator(root);
    boolean containsSpannableDescendants = false;
    while (iterator.hasNext()) {
      AccessibilityNodeInfoCompat child = iterator.next();
      if (AccessibilityNodeInfoUtils.FILTER_NON_FOCUSABLE_VISIBLE_NODE.accept(child)) {
        containsSpannableDescendants |=
            searchSpannableStringsForCharacterStyleInNodeTree(
                child, visitedNodes, currentOffset, result, spanClasses);
      }
      if (containsSpannableDescendants && result == null) {
        return true;
      }
    }
    return hasSpannableString || containsSpannableDescendants;
  }

  private SpannableTraversalUtils() {}
}
