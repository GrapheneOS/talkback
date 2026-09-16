/*
 * Copyright (C) 2012 Google Inc.
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

import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.getCollectionIsColumnTransition;
import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.getCollectionIsRowTransition;
import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.getCollectionTableItemColumnIndex;
import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.getCollectionTableItemHeadingType;
import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.getCollectionTableItemRowIndex;
import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.hasBothCount;
import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.hasColumnCount;
import static com.google.android.accessibility.utils.monitor.CollectionStateUtils.hasRowCount;

import android.content.Context;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.accessibility.AccessibilityEvent;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.RangeInfoCompat;
import com.google.android.accessibility.braille.brailledisplay.R;
import com.google.android.accessibility.braille.brailledisplay.controller.BdController.BehaviorNodeText;
import com.google.android.accessibility.braille.brailledisplay.controller.rule.BrailleRule;
import com.google.android.accessibility.braille.brailledisplay.controller.rule.DefaultBrailleRule;
import com.google.android.accessibility.braille.brailledisplay.controller.utils.StringUtils;
import com.google.android.accessibility.utils.AccessibilityEventUtils;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.Role.RoleName;
import com.google.android.accessibility.utils.monitor.CollectionState;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Turns a subset of the node tree into braille. */
public class NodeBrailler {
  private static final String BRAILLE_UNICODE_CLICKABLE = "⠿⠄";
  private static final String BRAILLE_UNICODE_LONG_CLICKABLE = "⠿⠤";
  private static final char NEW_LINE = '\n';
  private static final int MAX_HEADING_LEVEL = 7;
  private static final int RANGE_TYPE_UNKNOWN = -1;
  private final Context context;
  private final List<BrailleRule> rules = new ArrayList<>();
  private final BehaviorNodeText behaviorNodeText;

  public NodeBrailler(Context context, BehaviorNodeText behaviorNodeText) {
    this.context = context;
    this.behaviorNodeText = behaviorNodeText;
    rules.add(new DefaultBrailleRule(behaviorNodeText));
  }

  /**
   * Converts {@code AccessibilityNodeInfoCompat} to annotated text to put on the braille display.
   * Returns the new content, or {@code null} if the event doesn't have a source node.
   */
  public CellsContent brailleNode(AccessibilityNodeInfoCompat node) {
    return brailleNodeAndEvent(node, /* event= */ null);
  }

  /**
   * Converts {@code AccessibilityNodeInfoCompat} and {@code AccessibilityEvent} to annotated text
   * to put on the braille display.
   */
  public CellsContent brailleNodeAndEvent(
      AccessibilityNodeInfoCompat node, AccessibilityEvent event) {
    SpannableStringBuilder sb = new SpannableStringBuilder();
    sb.append(formatSubtree(node, event));
    StringUtils.appendWithSpaces(sb, formatSubtree(event));
    return new CellsContent(sb);
  }

  /**
   * Converts the source of {@code event} and its surroundings to annotated text to put on the
   * braille display. Returns the new content, or {@code null} if the event doesn't have a source
   * node.
   */
  public CellsContent brailleEvent(AccessibilityEvent event) {
    return new CellsContent(formatSubtree(event));
  }

  /** Formats {@code node} and its descendants or extract text and description of {@code event}. */
  private CharSequence formatSubtree(AccessibilityNodeInfoCompat node, AccessibilityEvent event) {
    if (!node.isVisibleToUser()) {
      return "";
    }
    SpannableStringBuilder result;
    if (shouldAppendChildrenText(node)) {
      List<AccessibilityNodeInfoCompat> nodeTree = obtainNodeTreePreorder(node);
      result = new SpannableStringBuilder(formatAndRemoveContinuousSameTextNode(nodeTree));
    } else {
      result = new SpannableStringBuilder();
      result.append(find(node).format(context, node));
    }
    if (TextUtils.isEmpty(result) && event != null) {
      result = new SpannableStringBuilder(AccessibilityEventUtils.getEventTextOrDescription(event));
    }
    if (node.isAccessibilityFocused() && !TextUtils.isEmpty(result)) {
      DisplaySpans.addSelection(result, /* start= */ 0, /* end= */ 0);
    }
    addInlineClickableLabel(node.getRoleDescription(), result);
    StringUtils.appendWithSpaces(result, getSuffixLabelForNode(context, node));
    addSpaceAfterNewLine(result);
    addAccessibilityNodeSpanForUncovered(node, result);
    return result;
  }

  private CharSequence formatSubtree(AccessibilityEvent event) {
    if (event == null) {
      return "";
    }
    AccessibilityNodeInfoCompat node = AccessibilityNodeInfoUtils.toCompat(event.getSource());
    @RoleName int role = Role.getRole(node);
    SpannableStringBuilder result = new SpannableStringBuilder();
    if (role == Role.ROLE_SEEK_CONTROL || role == Role.ROLE_PROGRESS_BAR) {
      CharSequence stateDescription = AccessibilityNodeInfoUtils.getState(node);
      if (TextUtils.isEmpty(stateDescription)) {
        StringUtils.appendWithSpaces(result, seekBarPercentText(event, node, context));
      } else if (!stateDescription
          .toString()
          .equals(context.getString(R.string.bd_state_description_in_progress))) {
        StringUtils.appendWithSpaces(result, stateDescription);
      }
    } else {
      // Extract the row count or column count of a table.
      CollectionState collectionState = behaviorNodeText.getCollectionState();
      if (collectionState.getCollectionTransition() == CollectionState.NAVIGATE_ENTER
          && collectionState.getCollectionRoleDescription() == null) {
        switch (collectionState.getCollectionRole()) {
          case Role.ROLE_GRID:
          case Role.ROLE_STAGGERED_GRID:
            StringBuilder sb = new StringBuilder();
            sb.append(context.getString(R.string.bd_affix_label_grid));
            StringUtils.appendWithSpaces(sb, getCollectionGridItemCount(collectionState, context));
            if (!TextUtils.isEmpty(sb)) {
              StringUtils.appendWithSpaces(
                  result, context.getString(R.string.bd_affix_label_grid_template, sb));
            }
            break;
          default: // fall out
        }
      }
    }
    return result;
  }

  private static String getCollectionGridItemCount(
      CollectionState collectionState, Context context) {
    if (hasBothCount(collectionState)) {
      return getStringForRow(collectionState, context)
          + " "
          + getStringForColumn(collectionState, context);
    } else if (hasRowCount(collectionState)) {
      return getStringForRow(collectionState, context);
    } else if (hasColumnCount(collectionState)) {
      return getStringForColumn(collectionState, context);
    }
    return "";
  }

  private static String getStringForRow(CollectionState collectionState, Context context) {
    return context
        .getResources()
        .getString(R.string.bd_affix_label_row_count, collectionState.getCollectionRowCount());
  }

  private static String getStringForColumn(CollectionState collectionState, Context context) {
    return context
        .getResources()
        .getString(
            R.string.bd_affix_label_column_count, collectionState.getCollectionColumnCount());
  }

  /** Returns the seekbar percent description text. */
  private static String seekBarPercentText(
      AccessibilityEvent event, AccessibilityNodeInfoCompat node, Context context) {
    @Nullable AccessibilityNodeInfoCompat.RangeInfoCompat rangeInfo = node.getRangeInfo();
    float current = rangeInfo == null ? 0 : rangeInfo.getCurrent();
    int type = rangeInfo == null ? RANGE_TYPE_UNKNOWN : rangeInfo.getType();
    return switch (type) {
      case RangeInfoCompat.RANGE_TYPE_PERCENT ->
          context.getString(R.string.bd_affix_label_percentage, current);
      case RangeInfoCompat.RANGE_TYPE_INT -> String.valueOf((int) current);
      case RangeInfoCompat.RANGE_TYPE_FLOAT -> String.valueOf(current);
      default ->
          event.getItemCount() > 0
              ? context.getString(
                  R.string.bd_affix_label_percentage,
                  AccessibilityNodeInfoUtils.roundForProgressPercent(
                      AccessibilityNodeInfoUtils.getProgressPercent(node)))
              : "";
    };
  }

  private boolean shouldAppendChildrenText(AccessibilityNodeInfoCompat node) {
    // Follow the same adding children rule with TalkBack compositor.
    int role = Role.getRole(node);
    return (role == Role.ROLE_GRID
            || role == Role.ROLE_LIST
            || role == Role.ROLE_PAGER
            || TextUtils.isEmpty(node.getContentDescription()))
        && role != Role.ROLE_WEB_VIEW;
  }

  private void addSpaceAfterNewLine(SpannableStringBuilder text) {
    for (int i = text.length() - 1; i >= 0; i--) {
      if (text.charAt(i) == NEW_LINE) {
        text.insert(i + 1, " ");
      }
    }
  }

  private void addInlineClickableLabel(@Nullable CharSequence roleDescription, Editable editable) {
    final ClickableSpan[] spans = editable.getSpans(0, editable.length(), ClickableSpan.class);
    for (int i = spans.length - 1; i >= 0; i--) {
      int start = editable.getSpanStart(spans[i]);
      int end = editable.getSpanEnd(spans[i]);
      SpannableString label =
          new SpannableString(
              spans[i] instanceof URLSpan
                  ? getLinkAffix(roleDescription)
                  : BRAILLE_UNICODE_CLICKABLE);
      StringUtils.insertWithSpaces(editable, end, label);
      editable.setSpan(spans[i], start, end + label.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
  }

  private String getLinkAffix(CharSequence roleDescription) {
    if (TextUtils.equals(
        roleDescription, context.getString(R.string.bd_role_description_visited_link))) {
      return context.getString(R.string.bd_affix_label_visited_link);
    }
    return context.getString(R.string.bd_affix_label_link);
  }

  private List<AccessibilityNodeInfoCompat> obtainNodeTreePreorder(
      AccessibilityNodeInfoCompat root) {
    List<AccessibilityNodeInfoCompat> result = new ArrayList<>();
    result.add(root);
    for (int i = 0; i < root.getChildCount(); i++) {
      AccessibilityNodeInfoCompat child = root.getChild(i);
      if (AccessibilityNodeInfoUtils.FILTER_NON_FOCUSABLE_VISIBLE_NODE.accept(child)
          || AccessibilityNodeInfoUtils.FILTER_NON_FOCUSABLE_NON_VISIBLE_HAS_TEXT_NODE.accept(
              child)) {
        result.addAll(obtainNodeTreePreorder(child));
      }
    }
    return result;
  }

  /**
   * Traverses and compares {@link AccessibilityNodeInfoCompat} in the list in order and remove the
   * continuous {@link AccessibilityNodeInfoCompat} with absolutely same text.
   */
  private CharSequence formatAndRemoveContinuousSameTextNode(
      List<AccessibilityNodeInfoCompat> list) {
    if (list.isEmpty()) {
      return "";
    }
    SpannableStringBuilder result = new SpannableStringBuilder();
    CharSequence previous = find(list.get(0)).format(context, list.get(0));
    CharSequence current;
    StringUtils.appendWithSpaces(result, previous);
    for (int i = 1; i < list.size(); i++) {
      current = find(list.get(i)).format(context, list.get(i));
      if (!previous.toString().contentEquals(current)) {
        StringUtils.appendWithSpaces(result, current);
      }
      previous = current;
    }
    return result;
  }

  /** Returns a user-facing, possibly-empty suffix label for the node, such as "btn" for button. */
  private CharSequence getSuffixLabelForNode(Context context, AccessibilityNodeInfoCompat node) {
    boolean shouldCheckClickable = false;
    @RoleName int role = Role.getRole(node);
    StringBuilder result = new StringBuilder();

    if (node.isSelected()) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_selected));
    }

    String heading = getHeadingString(node);
    if (!TextUtils.isEmpty(heading)) {
      StringUtils.appendWithSpaces(result, heading);
    }

    String roleAffix = getRoleAffix(node.getRoleDescription());
    if (!TextUtils.isEmpty(roleAffix)) {
      StringUtils.appendWithSpaces(result, roleAffix);
    }

    if (AccessibilityNodeInfoUtils.isExpandable(node)) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_collapsed));
    } else if (AccessibilityNodeInfoUtils.isCollapsible(node)) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_expanded));
    }

    if (role == Role.ROLE_BUTTON
        || role == Role.ROLE_IMAGE_BUTTON
        || role == Role.ROLE_FLOATING_ACTION_BUTTON
        || role == Role.ROLE_VOICE_DICTATION_BUTTON) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_button));
    } else if (role == Role.ROLE_EDIT_TEXT) {
      if (node.isMultiLine()) {
        StringUtils.appendWithSpaces(
            result, context.getString(R.string.bd_affix_label_multiple_line));
      }
      StringUtils.appendWithSpaces(
          result, context.getString(R.string.bd_affix_label_editable_text));
    } else if (role == Role.ROLE_DROP_DOWN_LIST) {
      StringUtils.appendWithSpaces(
          result, context.getString(R.string.bd_affix_label_drop_down_list));
    } else if (role == Role.ROLE_CHECK_BOX) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_checkbox));
    } else if (role == Role.ROLE_RADIO_BUTTON) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_radiobutton));
    } else if (role == Role.ROLE_LIST) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_list));
    } else {
      shouldCheckClickable = true;
    }

    if (!node.isEnabled()) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_disabled));
    }

    if (AccessibilityNodeInfoUtils.nodeMatchesClassByType(node, ImageView.class)) {
      StringUtils.appendWithSpaces(result, context.getString(R.string.bd_affix_label_graphic));
    }

    if (shouldCheckClickable) {
      if (AccessibilityNodeInfoUtils.isLongClickable(node) && node.isEnabled()) {
        StringUtils.appendWithSpaces(result, BRAILLE_UNICODE_LONG_CLICKABLE);
      } else if (AccessibilityNodeInfoUtils.isClickable(node) && node.isEnabled()) {
        StringUtils.appendWithSpaces(result, BRAILLE_UNICODE_CLICKABLE);
      }
    }

    // Extract the row index or column index changed when the collection is transitioned.
    CollectionState collectionState = behaviorNodeText.getCollectionState();
    boolean isRowTransition = getCollectionIsRowTransition(collectionState);
    boolean isColumnTransition = getCollectionIsColumnTransition(collectionState);
    int headingType = getCollectionTableItemHeadingType(behaviorNodeText.getCollectionState());
    StringBuilder sb = new StringBuilder();
    if (isRowTransition || isColumnTransition) {
      int tableItemRowIndex = getCollectionTableItemRowIndex(collectionState);
      if (isRowTransition && tableItemRowIndex != -1 && headingType != CollectionState.TYPE_ROW) {
        sb.append(context.getString(R.string.bd_affix_label_row, tableItemRowIndex + 1));
      }
      int tableItemColumnIndex = getCollectionTableItemColumnIndex(collectionState);
      if (isColumnTransition
          && tableItemColumnIndex != -1
          && headingType != CollectionState.TYPE_COLUMN) {
        StringUtils.appendWithSpaces(
            sb, context.getString(R.string.bd_affix_label_col, tableItemColumnIndex + 1));
      }
      if (!TextUtils.isEmpty(sb)) {
        StringUtils.appendWithSpaces(
            result, context.getString(R.string.bd_affix_label_grid_template, sb));
      }
    }

    return result;
  }

  private String getHeadingString(AccessibilityNodeInfoCompat node) {
    CharSequence roleDescription = node.getRoleDescription();
    if (TextUtils.isEmpty(roleDescription)) {
      if (AccessibilityNodeInfoUtils.isHeading(node)) {
        return context.getString(R.string.bd_affix_label_heading_no_level);
      }
    } else {
      for (int i = 1; i < MAX_HEADING_LEVEL; i++) {
        if (Pattern.matches(
            ".*" + context.getString(R.string.bd_role_description_heading, i) + ".*",
            roleDescription)) {
          return context.getString(R.string.bd_affix_label_heading_with_level, i);
        }
      }
    }
    return "";
  }

  @Nullable
  private BrailleRule find(AccessibilityNodeInfoCompat node) {
    return rules.stream().filter(r -> r.accept(node)).findFirst().orElse(null);
  }

  /**
   * Adds {@code node} as a span on {@code content} if not already fully covered by an accessibility
   * node info span.
   */
  private void addAccessibilityNodeSpanForUncovered(
      AccessibilityNodeInfoCompat node, Spannable spannable) {
    AccessibilityNodeInfoCompat[] spans =
        spannable.getSpans(0, spannable.length(), AccessibilityNodeInfoCompat.class);
    for (AccessibilityNodeInfoCompat span : spans) {
      if (spannable.getSpanStart(span) == 0 && spannable.getSpanEnd(span) == spannable.length()) {
        return;
      }
    }
    DisplaySpans.setAccessibilityNode(spannable, node);
  }

  private String getRoleAffix(@Nullable CharSequence roleDescription) {
    if (TextUtils.equals(roleDescription, context.getString(R.string.bd_role_description_table))) {
      return context.getString(R.string.bd_affix_label_table);
    } else if (TextUtils.equals(
        roleDescription, context.getString(R.string.bd_role_description_graphic))) {
      return context.getString(R.string.bd_affix_label_graphic);
    }
    return "";
  }
}
