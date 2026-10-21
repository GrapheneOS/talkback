/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.google.android.accessibility.utils;

import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import androidx.annotation.IntDef;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.CollectionInfoCompat;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.checkerframework.checker.nullness.qual.Nullable;

/** Utility methods for managing AccessibilityNodeInfo Roles. */
public class Role {

  /**
   * Ids of user-interface element roles, which are flexibly mapped from specific UI classes. This
   * mapping allows us to abstract similar UI elements to the same role, and to isolate UI element
   * interpretation logic.
   *
   * <p>For definitions of MUST, SHALL, SHOULD, MAY, MUST NOT, SHALL NOT, SHOULD NOT, and MAY NOT
   * see <a href="https://www.ietf.org/rfc/rfc2119.txt"></a>.
   *
   * <p>tl;dr:
   * <ul>
   *   <li>MUST, MUST NOT, SHALL, and SHALL NOT are used for absolute requirements
   *   <li>SHOULD, SHOULD NOT are used for recommendations for which there are <i>occasional</i>
   *   exceptions
   *   <li>MAY, MAY NOT are used for truly optional items.
   * </ul>
   *
   * <p>
   * Consider the following definitions when interpreting the docs for individual roles.
   * <table>
   *   <tr>
   *     <td><i>focusable</i></td>
   *     <td>{@link AccessibilityNodeInfo#isFocusable()} is {@code true} or a node’s action list
   *     contains {@link AccessibilityNodeInfo.AccessibilityAction#ACTION_FOCUS}. Note that this
   *     refers to input focus, such as being focusable with keyboard or directional pad input, as
   *     opposed to accessibility focus.</td>
   *   </tr>
   *   <tr>
   *     <td><i>clickable</i></td>
   *     <td>{@link AccessibilityNodeInfo#isClickable()} is {@code true} or a node’s action list
   *     contains {@link AccessibilityNodeInfo.AccessibilityAction#ACTION_CLICK}</td>
   *   </tr>
   *   <tr>
   *     <td><i>long-clickable</i></td>
   *     <td>{@link AccessibilityNodeInfo#isLongClickable()} is {@code true} or a node’s action list
   *     contains {@link AccessibilityNodeInfo.AccessibilityAction#ACTION_LONG_CLICK}</td>
   *   </tr>
   *   <tr>
   *     <td><i>context-clickable</i></td>
   *     <td>{@link AccessibilityNodeInfo#isContextClickable()} is {@code true} or a node’s action
   *     list contains {@link AccessibilityNodeInfo.AccessibilityAction#ACTION_CONTEXT_CLICK}</td>
   *   </tr>
   *   <tr>
   *     <td><i>actionable</i></td>
   *     <td>The user can interact with the node and execute one or more accessibility actions
   *     (for example, {@link AccessibilityNodeInfo.AccessibilityAction#ACTION_CLICK} to perform a
   *     click, {@link AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_FORWARD} to perform a
   *     scroll, {@link AccessibilityNodeInfo.AccessibilityAction#ACTION_DISMISS} to dismiss an
   *     element, etc.).</td>
   *   </tr>
   *   <tr>
   *     <td><i>checkable</i></td>
   *     <td>{@link AccessibilityNodeInfo#isCheckable()} is always {@code true}, and <i>node</i> has
   *     checked and unchecked states that can be obtained by calling
   *     {@link AccessibilityNodeInfo#isChecked()}. Accessibility services may associate
   *     descriptive labels - "on/off", "checked/unchecked", "selected/unselected", etc. - with the
   *     checked/unchecked state of <i>node</i>. These labels may be customized by calling
   *     {@link AccessibilityNodeInfo#setStateDescription(CharSequence)}.</td>
   *   </tr>
   *   <tr>
   *     <td><i>enabled</i></td>
   *     <td>{@link AccessibilityNodeInfo#isEnabled()} returns {@code true}.</td>
   *   </tr>
   *   <tr>
   *     <td><i>accessibility label</i></td>
   *     <td>Text, usually in the form of a localized string, that describes the node. This is
   *     usually computed by an accessibility service based on a variety of attributes (example:
   *     {@link AccessibilityNodeInfo#getText()},
   *     {@link AccessibilityNodeInfo#getContentDescription()}, or
   *     {@link AccessibilityNodeInfo#getStateDescription()}), or
   *     {@link AccessibilityNodeInfo#getContainerTitle()}), or via another text node referenced
   *     by {@link AccessibilityNodeInfo#getLabeledBy()}.</td>
   *   </tr>
   *   <tr>
   *     <td><i>collection</i></td>
   *     <td>A grouping of logically related <i>collection item</i> nodes. The following constraints
   *     apply:
   *     <ul>
   *       <li>A <i>collection</i> MUST expose {@link CollectionInfo}.</li>
   *       <li>For an empty <i>collection</i>, {@link CollectionInfo#getRowCount()} and
   *       {@link CollectionInfo#getColumnCount()} SHOULD both return 0, and for a non-empty
   *       collection, {@link CollectionInfo#getRowCount()} and {@link
   *       CollectionInfo#getColumnCount()} SHOULD both return values >= 1.</li>
   *       <li>{@link CollectionInfo#getItemCount()} should return the total number of items in the
   *       collection and {@link CollectionInfo#getImportantForAccessibilityItemCount()} should
   *       return the number of items that are considered important for accessibility. The latter
   *       usually excludes items such as decorative headers, dividers, etc. Example: a collection
   *       contains 10 items, which includes one decorative header and one divider. In this case,
   *       {@link CollectionInfo#getItemCount()} should return 10 (the total number of items), and
   *       {@link CollectionInfo#getImportantForAccessibilityItemCount()} should return 8.
   *       </li>
   *       <li>If <i>node</i> is enabled and all items in a <i>collection</i> are not fully
   *       visible, {@link AccessibilityNodeInfo#getActionList()} SHOULD contain one or more scroll
   *       actions: if the last <i>collection item</i> is not fully visible, {@link
   *       AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_FORWARD} SHOULD be present; if
   *       the first <i>collection item</i> is not fully visible, {@link
   *       AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_BACKWARD} SHOULD be present.
   *       </li>
   *       <li>In the case of a nested <i>collection</i>, the inner <i>collection</i> SHOULD expose
   *       both {@link CollectionItemInfo} (since it is part of the outer <i>collection</i>) and
   *       <i>{@link CollectionInfo}.</li>
   *     </td>
   *   </tr>
   *   <tr>
   *     <td><i>collection item</i></td>
   *     <td>An item in a <i>collection</i>. The following constraints apply:
   *       <ul>
   *         <li>{@link CollectionItemInfo} MUST be exposed. </li>
   *     </td>
   *   </tr>
   */
  @IntDef({
    ROLE_NONE,
    ROLE_BUTTON,
    ROLE_CHECK_BOX,
    ROLE_CHECKED_TEXT_VIEW,
    ROLE_DROP_DOWN_LIST,
    ROLE_EDIT_TEXT,
    ROLE_GRID,
    ROLE_IMAGE,
    ROLE_IMAGE_BUTTON,
    ROLE_LIST,
    ROLE_PAGER,
    ROLE_RADIO_BUTTON,
    ROLE_SEEK_CONTROL,
    ROLE_SWITCH,
    ROLE_TAB_BAR,
    ROLE_TOGGLE_BUTTON,
    ROLE_VIEW_GROUP,
    ROLE_WEB_VIEW,
    ROLE_PROGRESS_BAR,
    ROLE_ACTION_BAR_TAB,
    ROLE_DRAWER_LAYOUT,
    ROLE_SLIDING_DRAWER,
    ROLE_ICON_MENU,
    ROLE_TOAST,
    ROLE_ALERT_DIALOG,
    ROLE_DATE_PICKER_DIALOG,
    ROLE_TIME_PICKER_DIALOG,
    ROLE_DATE_PICKER,
    ROLE_TIME_PICKER,
    ROLE_NUMBER_PICKER,
    ROLE_SCROLL_VIEW,
    ROLE_HORIZONTAL_SCROLL_VIEW,
    ROLE_KEYBOARD_KEY,
    ROLE_TALKBACK_EDIT_TEXT_OVERLAY,
    ROLE_TEXT_ENTRY_KEY,
    ROLE_STAGGERED_GRID,
    ROLE_FLOATING_ACTION_BUTTON,
    ROLE_NON_MODAL_ALERT,
    ROLE_SNACKBAR,
    ROLE_AUDIO_CAPTION,
    ROLE_DIALOG,
    ROLE_NAVIGATION,
    ROLE_SEARCH,
    ROLE_VOICE_DICTATION_BUTTON,
  })
  @Retention(RetentionPolicy.SOURCE)
  public @interface RoleName {}

  // Please keep the constants in this list sorted by constant index order, and not by
  // alphabetical order. If you add a new constant, it must also be added to the RoleName
  // annotation interface.
  public static final int ROLE_NONE = 0;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.widget.Button} or equivalent.
   *
   * <ul>
   *   <li>If <i>node</i> has on/off semantics, use {@link Role#ROLE_SWITCH} instead.
   *   <li>If <i>node</i> has checked/unchecked (or partially checked) semantics, use {@link
   *       Role#ROLE_CHECK_BOX} instead.
   *   <li>If <i>node</i> lets the user pick one option from a set of options, use {@link
   *       Role#ROLE_RADIO_BUTTON} instead.
   * </ul>
   *
   * <p>The following constraints apply:
   *
   * <ul>
   *   <li><i>node</i> MUST have an <i>accessibility label</i>.
   *   <li>When <i>enabled</i>, <i>node</i> SHOULD be <i>focusable</i> and <i>actionable</i>(
   *       ({@link AccessibilityNodeInfo.AccessibilityAction#ACTION_CLICK}, {@link
   *       AccessibilityNodeInfo.AccessibilityAction#ACTION_LONG_CLICK}, and {@link
   *       AccessibilityNodeInfo.AccessibilityAction#ACTION_CONTEXT_CLICK} are the most common
   *       actions associated with this role). Example:
   *       <pre>
   *      node: {
   *        text: “...”,
   *        clickable: true,
   *        focusable: true,
   *        childNodes: []
   *        }
   *       </pre>
   *       Or, when <i>node</i> is neither <i>focusable</i> nor <i>actionable</i>, the nearest
   *       <i>focusable</i> ancestor MUST be <i>actionable</i>. Example:
   *       <pre>
   *     node: {
   *       text: “...”,
   *       clickable: true,
   *       focusable: true,
   *       childNodes: [
   *          node: {
   *            role: ROLE_BUTTON,
   *            clickable: false,
   *            focusable: false, … }]
   *            }
   *       </pre>
   *       Note: this hierarchy occurs commonly when a {@link android.widget.Button} is wrapped in a
   *       parent and click and other actions are registered on the parent and not directly on the
   *       button.
   *   <li>When <i>not enabled</i>, <i>node</i> MUST be <i>focusable</i> but not <i>actionable</i>.
   *       This allows a user to place input focus on the element but not perform, say, a click on
   *       it.
   * </ul>
   */
  public static final int ROLE_BUTTON = 1;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.widget.CheckBox} or equivalent.
   *
   * <p>All constraints for {@link Role#ROLE_BUTTON} apply with the following modifications:
   *
   * <ul>
   *   <li><i>node</i> MUST be <i>checkable</i>
   * </ul>
   */
  public static final int ROLE_CHECK_BOX = 2;

  public static final int ROLE_DROP_DOWN_LIST = 3;
  public static final int ROLE_EDIT_TEXT = 4;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.widget.GridView} or an equivalent element that represents a grid of nodes with clear
   * row and column semantics.
   *
   * <p>All constraints associated with a <i>collection</i> apply with the following modifications:
   *
   * <ul>
   *   <li>Both {@link CollectionInfo#getRowCount()} and {@link CollectionInfo#getColumnCount()}
   *       MUST return values >=1 for non-empty grids.
   * </ul>
   */
  public static final int ROLE_GRID = 5;

  public static final int ROLE_IMAGE = 6;
  public static final int ROLE_IMAGE_BUTTON = 7;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.widget.ListView} or an equivalent element that represents a one-dimensional collection
   * of nodes.
   *
   * <p>All constraints associated with a <i>collection</i> apply with the following modifications:
   *
   * <ul>
   *   <li>If child nodes are arranged vertically (in a column) {@link
   *       CollectionInfo#getColumnCount()} MUST return 1. If child nodes are arranged horizontally
   *       (in a row) {@link CollectionInfo#getRowCount()} MUST return 1.
   * </ul>
   */
  public static final int ROLE_LIST = 8;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.widget.RadioButton} or equivalent.
   *
   * <p>All constraints for {@link Role#ROLE_BUTTON} apply with the following modifications:
   *
   * <ul>
   *   <li><i>node</i> MUST be <i>checkable</i>
   *   <li>If {@link AccessibilityNodeInfo#isChecked()} return {@code true}, <i>node</i> need not be
   *       <i>actionable</i>. This is because the checked state of <i>node</i> may be toggled by
   *       some other means, such as interacting with a sibling {@link Role#ROLE_RADIO_BUTTON} node
   *   <li><i>node</i> MUST have an ancestor with the className {@link android.widget.RadioGroup}
   *   <li><i>node</i> MUST expose {@link
   *       android.view.accessibility.AccessibilityNodeInfo.CollectionItemInfo}
   * </ul>
   */
  // TODO: add ROLE_RADIO_GROUP.
  public static final int ROLE_RADIO_BUTTON = 9;

  /**
   * Role of an {@link AccessibilityNodeInfo} <i>node</i> corresponding to a {@link
   * android.widget.SeekBar} or equivalent.
   *
   * <p>Note: if the UI element corresponding to <i>node</i> does not permit the user to set the
   * current progress value, use {@link Role#ROLE_PROGRESS_BAR} instead.
   *
   * <p>All constraints for {@link Role#ROLE_PROGRESS_BAR} apply with the following modifications:
   *
   * <ul>
   *   <li>If <i>node</i> is enabled:
   *       <ul>
   *         <li><i>node</i> MUST be actionable.
   *         <li>If progress < {@link RangeInfo#getMax()}, {@link
   *             AccessibilityNodeInfo#getActionList()} MUST contain {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_FORWARD}.
   *         <li>If progress == {@link RangeInfo#getMax()}, {@link
   *             AccessibilityNodeInfo#getActionList()} MUST NOT contain {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_FORWARD}.
   *         <li>If progress > {@link RangeInfo#getMin()}, {@link
   *             AccessibilityNodeInfo#getActionList()} MUST contain {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_BACKWARD}.
   *         <li>If progress == {@link RangeInfo#getMin()}, {@link
   *             AccessibilityNodeInfo#getActionList()} MUST NOT contain {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_BACKWARD}.
   *         <li><i>node</i> MUST be able to perform {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SET_PROGRESS} and set progress
   *             between {@link RangeInfo#getMin()} and {@link RangeInfo#getMax()}.
   *       </ul>
   *   <li>If <i>node</i> is not enabled:
   *       <ul>
   *         <li>{@link AccessibilityNodeInfo#getActionList()} MUST NOT contain {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_FORWARD} or {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SCROLL_BACKWARD} or {@link
   *             AccessibilityNodeInfo.AccessibilityAction#ACTION_SET_PROGRESS}.
   *       </ul>
   * </ul>
   */
  public static final int ROLE_SEEK_CONTROL = 10;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.widget.Switch} or equivalent.
   *
   * <p>All constraints for {@link Role#ROLE_BUTTON} apply with the following modifications:
   *
   * <ul>
   *   <li><i>node</i> MUST be <i>checkable</i>
   * </ul>
   */
  public static final int ROLE_SWITCH = 11;

  public static final int ROLE_TAB_BAR = 12;
  public static final int ROLE_TOGGLE_BUTTON = 13;
  public static final int ROLE_VIEW_GROUP = 14;
  public static final int ROLE_WEB_VIEW = 15;
  public static final int ROLE_PAGER = 16;
  public static final int ROLE_CHECKED_TEXT_VIEW = 17;

  // TODO: define specific expectations for indeterminate ProgressBar.
  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.widget.ProgressBar} or equivalent.
   *
   * <p>Note: if progress is determinate and can be set by the user on the UI element corresponding
   * to <i>node</i>, use {@link Role#ROLE_SEEK_CONTROL} instead.
   *
   * <p>The following constraints apply:
   *
   * <ul>
   *   <li>Progress MAY be determinate or indeterminate. If progress is determinate, {@link
   *       android.view.accessibility.AccessibilityNodeInfo.RangeInfo} MUST be set and {@link
   *       RangeInfo#getCurrent()} values MUST return the updated value as progress changes.
   *   <li>Generally, <i>node</i> SHOULD NOT be <i>actionable</i>.
   * </ul>
   */
  public static final int ROLE_PROGRESS_BAR = 18;

  public static final int ROLE_ACTION_BAR_TAB = 19;
  public static final int ROLE_DRAWER_LAYOUT = 20;
  public static final int ROLE_SLIDING_DRAWER = 21;
  public static final int ROLE_ICON_MENU = 22;
  public static final int ROLE_TOAST = 23;
  public static final int ROLE_ALERT_DIALOG = 24;
  public static final int ROLE_DATE_PICKER_DIALOG = 25;
  public static final int ROLE_TIME_PICKER_DIALOG = 26;
  public static final int ROLE_DATE_PICKER = 27;
  public static final int ROLE_TIME_PICKER = 28;
  public static final int ROLE_NUMBER_PICKER = 29;
  public static final int ROLE_SCROLL_VIEW = 30;
  public static final int ROLE_HORIZONTAL_SCROLL_VIEW = 31;
  public static final int ROLE_KEYBOARD_KEY = 32;
  public static final int ROLE_TALKBACK_EDIT_TEXT_OVERLAY = 33;
  public static final int ROLE_TEXT_ENTRY_KEY = 34;

  // TODO: add expectations of CollectionItemInfo (here and for ROLE_GRID)
  // TODO: Talkback should announce the correct collection size for staggered grids.
  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to {@code
   * androidx.recyclerview.widget.StaggeredGridLayoutManager} or an equivalent element that
   * represents a grid with <b>either</b> a defined number of rows or or a defined number of
   * columns.
   *
   * <p>All constraints associated with a <i>collection</i> apply with the following modifications:
   *
   * <ul>
   *   <li>If <i>node</i> has a defined number of columns and a variable number of rows per column,
   *       {@link CollectionInfo#getRowCount()} MUST return -1 and {@link
   *       CollectionInfo#getColumnCount()} MUST return the number of columns; or if <i>node</i> has
   *       a defined number of rows and a variable number of columns per row, {@link
   *       CollectionInfo#getRowCount()} MUST return the number of rows and {@link
   *       CollectionInfo#getColumnCount()} MUST return -1.
   * </ul>
   */
  public static final int ROLE_STAGGERED_GRID = 35;

  public static final int ROLE_FLOATING_ACTION_BUTTON = 36;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which contains content that the user
   * should be informed of in a time-sensitive manner without blocking interaction with the rest of
   * the screen.
   *
   * <ul>
   *   <li>If <i>node</i> maps to a <a href="https://m3.material.io/components/snackbar">Material
   *       Snackbar</a>, use {@link Role#ROLE_SNACKBAR} instead.
   * </ul>
   *
   * <p>The following constraints apply:
   *
   * <ul>
   *   <li><i>node</i> MUST contain at least one child, usually representing the content that the
   *       user should be informed of.
   *   <li><i>node</i> MUST be either an accessibility pane or it must be the root of a titled
   *       window: i.e., either {@link AccessibilityNodeInfo#getPaneTitle()} must return the
   *       <i>node</i> title or <i>node</i> must equal {@link AccessibilityWindowInfo#getRoot()} and
   *       {@link AccessibilityWindowInfo#getTitle()} must return the <i>node</i> title.
   * </ul>
   */
  public static final int ROLE_NON_MODAL_ALERT = 37;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which aligns with a <a
   * href="https://m3.material.io/components/snackbar">Material Snackbar</a>.
   *
   * <ul>
   *   <li>For generic non-modal containers with alert semantics, use {@link
   *       Role#ROLE_NON_MODAL_ALERT}.
   * </ul>
   *
   * <p>All constraints for {@link Role#ROLE_NON_MODAL_ALERT} apply, with the following
   * modifications:
   *
   * <ul>
   *   <li>A "polite" live region SHOULD be used; i.e., {@link
   *       AccessibilityNodeInfo#getLiveRegion()} SHOULD return {@link
   *       android.view.View#ACCESSIBILITY_LIVE_REGION_POLITE}
   *   <li>If <i>node</i> allows a user to perform an action, a child node with role {@link
   *       Role#ROLE_BUTTON} should be provided.
   * </ul>
   */
  public static final int ROLE_SNACKBAR = 38;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@code
   * androidx.media3.ui.SubtitleView} or an equivalent element which periodically updates captions
   * of realtime audio and relays those updates to users of refreshable Braille displays.
   *
   * <p>The following constraints apply:
   *
   * <ul>
   *   <li><i>node</i> SHOULD be a leaf node.
   *   <li>When the value of {@link AccessibilityNodeInfo#getText()} changes, <i>node</i> MUST
   *       dispatch an {@link AccessibilityEvent} of type {@link
   *       AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED} with a {@link
   *       AccessibilityEvent.CONTENT_CHANGE_TYPE_TEXT} subtype.
   *   <li><i>node</i> MAY declare itself an {@code accessibilityLiveRegion} if content updates
   *       should also be conveyed to users of screen readers.
   * </ul>
   */
  public static final int ROLE_AUDIO_CAPTION = 39;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i> which corresponds to a {@link
   * android.app.Dialog} or equivalent.
   */
  public static final int ROLE_DIALOG = 40;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i>, which corresponds to a landmark that
   * groups <i>actionable</i> elements. Accessibility services can provide means to allow a user to
   * easily navigate to <i>node</i>.
   *
   * <p>The following constraints apply:
   *
   * <ul>
   *   <li><i>node</i> MUST provide an {@link AccessibilityNodeInfo#getContainerTitle}, which acts
   *       as its <i>accessibility label</i>.
   *   <li>The <i>accessibility label</i> MUST be unique in the <i>node</i>'s hierarchy.
   * </ul>
   *
   * Example of an {@link AccessibilityNodeInfo} representing a navigation landmark and its
   * children:
   *
   * <pre>
   *      node: {
   *        role: ROLE_NAVIGATION,
   *        containerTitle: "Table of Contents",
   *        childNodes: [
   *          {
   *            role: ROLE_TEXT,
   *            text: "...",
   *          },
   *          {
   *            role: ROLE_BUTTON,
   *            text: "...",
   *            actionable: true,
   *            focusable: true
   *          },
   *        ]
   *      }
   * </pre>
   */
  public static final int ROLE_NAVIGATION = 41;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i>, which corresponds to a landmark that
   * contains all the elements that comprise an application's search functionality. Accessibility
   * services can provide means to allow a user to easily navigate to <i>node</i>.
   *
   * <p>The following constraints apply:
   *
   * <ul>
   *   <li><i>node</i> MUST have an <i>accessibility label</i>.
   *   <li><i>node</i> MUST contain, as a descendant, an input field that allows users to enter
   *       search terms.
   * </ul>
   *
   * Example of an {@link AccessibilityNodeInfo} representing a search landmark and its children:
   *
   * <pre>
   *      node: {
   *        role: ROLE_SEARCH,
   *        containerTitle: "Search Recipes",
   *        childNodes: [
   *          {
   *            role: ROLE_EDIT_TEXT,
   *            ...
   *          },
   *          {
   *            role: ROLE_BUTTON,
   *            text: "Search Now",
   *            actionable: true,
   *            focusable: true
   *          },
   *        ]
   *      }
   * </pre>
   */
  public static final int ROLE_SEARCH = 42;

  /**
   * Role for an {@link AccessibilityNodeInfo} <i>node</i>, which corresponds to a voice dictation
   * input button with class name {@link #VOICE_DICTATION_CLASSNAME}.
   *
   * <p>All constraints for {@link Role#ROLE_BUTTON} apply with the following modifications:
   *
   * <ul>
   *   <li><i>node</i> MUST on the input method editor(IME) window.
   * </ul>
   */
  public static final int ROLE_VOICE_DICTATION_BUTTON = 43;

  /** Used to identify the voice dictation node. */
  public static final String VOICE_DICTATION_CLASSNAME =
      "android.speech.SpeechRecognizer.VoiceDictationButton";

  /** Used to identify and ignore a11y overlay windows created by Talkback. */
  public static final String TALKBACK_EDIT_TEXT_OVERLAY_CLASSNAME = "TalkbackEditTextOverlay";

  /** Used to identify lists. */
  public static final String TALKBACK_LIST_CLASSNAME = "android.widget.listview";

  /** Used to identify grids. */
  public static final String TALKBACK_GRID_CLASSNAME = "android.widget.gridview";

  /** Used to identify staggered grids. */
  public static final String TALKBACK_STAGGERED_GRID_CLASSNAME =
      "androidx.recyclerview.widget.StaggeredGridLayoutManager";

  /** Used to identify floating action buttons. */
  public static final String TALKBACK_FLOATING_ACTION_BUTTON_CLASSNAME =
      "com.google.android.material.floatingactionbutton.FloatingActionButton";

  /** Used to identify non-modal alerts */
  public static final String NON_MODAL_ALERT_CLASSNAME =
      "com.google.android.material.snackbar.BaseTransientBottomBar";

  /** Used to identify Material Snackbars. */
  public static final String SNACKBAR_CLASSNAME = "com.google.android.material.snackbar.SnackBar";

  /** Used to identify nodes that represent audio captions. */
  public static final String AUDIO_CAPTION_CLASSNAME = "androidx.media3.ui.SubtitleView";

  /** Used to identify dialogs. */
  public static final String DIALOG_CLASSNAME = "android.app.Dialog";

  /** Used to identify alert dialogs. */
  public static final String ALERT_DIALOG_CLASSNAME = "android.app.AlertDialog";

  /** Used to identify Material NavigationView. */
  public static final String MATERIAL_NAVIGATION_VIEW_CLASSNAME =
      "com.google.android.material.navigation.NavigationView";

  /** Used to identify Material NavigationBarView. */
  public static final String MATERIAL_NAVIGATION_BAR_CLASSNAME =
      "com.google.android.material.navigation.NavigationBarView";

  /** Used to identify Material NavigationRailView. */
  public static final String MATERIAL_NAVIGATION_RAIL_CLASSNAME =
      "com.google.android.material.navigationrail.NavigationRailView";

  /** Used to identify a search view. */
  public static final String SEARCH_CLASSNAME = "android.widget.SearchView";

  /**
   * Gets the source {@link Role} from the {@link AccessibilityEvent}.
   *
   * <p>It checks the role with {@link AccessibilityEvent#getClassName()}. If it returns {@link
   * #ROLE_NONE}, fallback to check {@link AccessibilityNodeInfoCompat#getClassName()} of the source
   * node.
   */
  @RoleName
  public static int getSourceRole(AccessibilityEvent event) {
    if (event == null) {
      return ROLE_NONE;
    }

    // Try to get role from event's class name.
    @RoleName int role = sourceClassNameToRole(event);
    if (role != ROLE_NONE) {
      return role;
    }

    // Extract event's source node, and map source node class to role.
    return getRole(event.getSource());
  }

  /** Find role from source event's class name string. */
  @RoleName
  private static int sourceClassNameToRole(AccessibilityEvent event) {
    if (event == null) {
      return ROLE_NONE;
    }

    // Event TYPE_NOTIFICATION_STATE_CHANGED always has null source node.
    CharSequence eventClassName = event.getClassName();

    // When comparing event.getClassName() to class name of standard widgets, we should take care of
    // the order of the "if" statements: check subclasses before checking superclasses.

    // Toast.TN is a private class, thus we have to hard code the class name.
    // "$TN" is only in the class-name before android-R.
    if (ClassLoadingCache.checkInstanceOf(eventClassName, "android.widget.Toast$TN")
        || ClassLoadingCache.checkInstanceOf(eventClassName, "android.widget.Toast")) {
      return ROLE_TOAST;
    }

    // Some events have different value for getClassName() and getSource().getClass()
    if (ClassLoadingCache.checkInstanceOf(eventClassName, android.app.ActionBar.Tab.class)) {
      return ROLE_ACTION_BAR_TAB;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // Subclasses of ViewGroup.

    // Inheritance: View->ViewGroup->DrawerLayout
    if (ClassLoadingCache.checkInstanceOf(
            eventClassName, androidx.drawerlayout.widget.DrawerLayout.class)
        || ClassLoadingCache.checkInstanceOf(eventClassName, "androidx.core.widget.DrawerLayout")) {
      return ROLE_DRAWER_LAYOUT;
    }

    // Inheritance: View->ViewGroup->SlidingDrawer
    if (ClassLoadingCache.checkInstanceOf(eventClassName, android.widget.SlidingDrawer.class)) {
      return ROLE_SLIDING_DRAWER;
    }

    // Inheritance: View->ViewGroup->IconMenuView
    // IconMenuView is a hidden class, thus we have to hard code the class name.
    if (ClassLoadingCache.checkInstanceOf(
        eventClassName, "com.android.internal.view.menu.IconMenuView")) {
      return ROLE_ICON_MENU;
    }

    // Inheritance: View->ViewGroup->FrameLayout->DatePicker
    if (ClassLoadingCache.checkInstanceOf(eventClassName, android.widget.DatePicker.class)) {
      return ROLE_DATE_PICKER;
    }

    // Inheritance: View->ViewGroup->FrameLayout->TimePicker
    if (ClassLoadingCache.checkInstanceOf(eventClassName, android.widget.TimePicker.class)) {
      return ROLE_TIME_PICKER;
    }

    // Inheritance: View->ViewGroup->LinearLayout->NumberPicker
    if (ClassLoadingCache.checkInstanceOf(eventClassName, android.widget.NumberPicker.class)) {
      return ROLE_NUMBER_PICKER;
    }

    // Inheritance: View->ViewGroup->LinearLayout->SearchView
    if (ClassLoadingCache.checkInstanceOf(eventClassName, SEARCH_CLASSNAME)) {
      return ROLE_SEARCH;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // Subclasses of Dialog.
    // Inheritance: Dialog->AlertDialog->DatePickerDialog
    if (ClassLoadingCache.checkInstanceOf(eventClassName, android.app.DatePickerDialog.class)) {
      return ROLE_DATE_PICKER_DIALOG;
    }

    // Inheritance: Dialog->AlertDialog->TimePickerDialog
    if (ClassLoadingCache.checkInstanceOf(eventClassName, android.app.TimePickerDialog.class)) {
      return ROLE_TIME_PICKER_DIALOG;
    }

    // Inheritance: Dialog->AlertDialog
    if (ClassLoadingCache.checkInstanceOf(eventClassName, ALERT_DIALOG_CLASSNAME)
        || ClassLoadingCache.checkInstanceOf(
            eventClassName, "androidx.appcompat.app.AlertDialog")) {
      return ROLE_ALERT_DIALOG;
    }

    if (ClassLoadingCache.checkInstanceOf(eventClassName, DIALOG_CLASSNAME)) {
      return ROLE_DIALOG;
    }

    if (ClassLoadingCache.checkInstanceOf(eventClassName, AUDIO_CAPTION_CLASSNAME)) {
      return ROLE_AUDIO_CAPTION;
    }

    // Inheritance: View->ViewGroup->FrameLayout->NavigationView
    if (ClassLoadingCache.checkInstanceOf(eventClassName, MATERIAL_NAVIGATION_VIEW_CLASSNAME)
        // Inheritance: View->ViewGroup->FrameLayout->NavigationBarView->NavigationRailView
        || ClassLoadingCache.checkInstanceOf(eventClassName, MATERIAL_NAVIGATION_RAIL_CLASSNAME)
        // Inheritance: View->ViewGroup->FrameLayout->NavigationBarView
        || ClassLoadingCache.checkInstanceOf(eventClassName, MATERIAL_NAVIGATION_BAR_CLASSNAME)) {
      return ROLE_NAVIGATION;
    }

    return ROLE_NONE;
  }

  /** Gets {@link Role} for {@link AccessibilityNodeInfoCompat}. */
  @RoleName
  public static int getRole(@Nullable AccessibilityNodeInfoCompat node) {
    if (node == null) {
      return ROLE_NONE;
    }

    // We check Text entry key from property instead of class, so it needs to be in the beginning.
    if (node.isTextEntryKey()) {
      return ROLE_TEXT_ENTRY_KEY;
    }
    CharSequence className = node.getClassName();

    // When comparing node.getClassName() to class name of standard widgets, we should take care of
    // the order of the "if" statements: check subclasses before checking superclasses.
    // e.g. RadioButton is a subclass of Button, we should check Role RadioButton first and fall
    // down to check Role Button.

    // Identifies a11y overlay added by Talkback on edit texts.
    if (ClassLoadingCache.checkInstanceOf(className, TALKBACK_EDIT_TEXT_OVERLAY_CLASSNAME)) {
      return ROLE_TALKBACK_EDIT_TEXT_OVERLAY;
    }

    // Inheritance: View->ImageView->ImageButton->FloatingActionButton
    if (ClassLoadingCache.checkInstanceOf(className, TALKBACK_FLOATING_ACTION_BUTTON_CLASSNAME)) {
      return ROLE_FLOATING_ACTION_BUTTON;
    }

    if (className != null && className.toString().equals(VOICE_DICTATION_CLASSNAME)) {
      return ROLE_VOICE_DICTATION_BUTTON;
    }

    // Inheritance: View->ImageView
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.ImageView.class)) {
      return node.isClickable() ? ROLE_IMAGE_BUTTON : ROLE_IMAGE;
    }

    if (className != null && className.toString().equals("android.widget.Image")) {
      // "android.widget.Image" is used in some WebView to play as an image.
      return node.isClickable() ? ROLE_IMAGE_BUTTON : ROLE_IMAGE;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // Subclasses of TextView.

    // Inheritance: View->TextView->Button->CompoundButton->Switch
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.Switch.class)) {
      return ROLE_SWITCH;
    }

    // Inheritance: View->TextView->Button->CompoundButton->ToggleButton
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.ToggleButton.class)) {
      return ROLE_TOGGLE_BUTTON;
    }

    // Inheritance: View->TextView->Button->CompoundButton->RadioButton
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.RadioButton.class)) {
      return ROLE_RADIO_BUTTON;
    }

    // Inheritance: View->TextView->Button->CompoundButton
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.CompoundButton.class)) {
      return ROLE_CHECK_BOX;
    }

    // Inheritance: View->TextView->Button
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.Button.class)) {
      return ROLE_BUTTON;
    }

    // Inheritance: View->TextView->CheckedTextView
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.CheckedTextView.class)) {
      return ROLE_CHECKED_TEXT_VIEW;
    }

    // Inheritance: View->TextView->EditText
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.EditText.class)) {
      return ROLE_EDIT_TEXT;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // Subclasses of ProgressBar.

    // Inheritance: View->ProgressBar->AbsSeekBar->SeekBar
    if (ClassLoadingCache.checkInstanceOf(className, SeekBar.class)
        || (AccessibilityNodeInfoUtils.hasValidRangeInfo(node)
            && AccessibilityNodeInfoUtils.supportsAction(
                node, android.R.id.accessibilityActionSetProgress))) {
      return ROLE_SEEK_CONTROL;
    }

    // Inheritance: View->ProgressBar
    if (ClassLoadingCache.checkInstanceOf(className, ProgressBar.class)
        || (AccessibilityNodeInfoUtils.hasValidRangeInfo(node)
            && !AccessibilityNodeInfoUtils.supportsAction(
                node, android.R.id.accessibilityActionSetProgress))) {
      // ProgressBar check must come after SeekBar, because SeekBar specializes ProgressBar.
      return ROLE_PROGRESS_BAR;
    }

    if (ClassLoadingCache.checkInstanceOf(
        className, android.inputmethodservice.Keyboard.Key.class)) {
      return ROLE_KEYBOARD_KEY;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // Subclasses of ViewGroup.

    // Inheritance: View->ViewGroup->AbsoluteLayout->WebView
    if (ClassLoadingCache.checkInstanceOf(className, android.webkit.WebView.class)) {
      return ROLE_WEB_VIEW;
    }

    // Inheritance: View->ViewGroup->LinearLayout->TabWidget
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.TabWidget.class)) {
      return ROLE_TAB_BAR;
    }

    // Inheritance: View->ViewGroup->FrameLayout->HorizontalScrollView
    // If there is a CollectionInfo, fall into a ROLE_LIST/ROLE_GRID
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.HorizontalScrollView.class)
        && node.getCollectionInfo() == null) {
      return ROLE_HORIZONTAL_SCROLL_VIEW;
    }

    // Inheritance: View->ViewGroup->FrameLayout->ScrollView
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.ScrollView.class)) {
      return ROLE_SCROLL_VIEW;
    }

    // Inheritance: View->ViewGroup->ViewPager
    if (ClassLoadingCache.checkInstanceOf(className, androidx.viewpager.widget.ViewPager.class)
        || ClassLoadingCache.checkInstanceOf(className, "android.support.v4.view.ViewPager")
        || ClassLoadingCache.checkInstanceOf(className, "androidx.core.view.ViewPager")
        || ClassLoadingCache.checkInstanceOf(className, "com.android.internal.widget.ViewPager")) {
      return ROLE_PAGER;
    }

    // Inheritance: View->ViewGroup->LinearLayout->NumberPicker
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.NumberPicker.class)) {
      return ROLE_NUMBER_PICKER;
    }

    // Inheritance: View->ViewGroup->AdapterView->AbsSpinner->Spinner
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.Spinner.class)) {
      return ROLE_DROP_DOWN_LIST;
    }

    // Inheritance: View->ViewGroup->AdapterView->AbsListView->GridView
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.GridView.class)) {
      return ROLE_GRID;
    }

    // Inheritance: View->ViewGroup->AdapterView->AbsListView
    if (ClassLoadingCache.checkInstanceOf(className, android.widget.AbsListView.class)) {
      return ROLE_LIST;
    }

    // Inheritance: View->ViewGroup->ViewPager2
    if (AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_UP.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_DOWN.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_LEFT.getId())
        || AccessibilityNodeInfoUtils.supportsAction(
            node, AccessibilityActionCompat.ACTION_PAGE_RIGHT.getId())) {
      return ROLE_PAGER;
    }

    CollectionInfoCompat collection = node.getCollectionInfo();
    if (collection != null) {
      // Grids, staggered grids, and lists can be empty or have only a single row or a single
      // column. For these collections, we rely on the classname being set explicitly when
      // assigning a role.
      if (ClassLoadingCache.checkInstanceOf(className, TALKBACK_LIST_CLASSNAME)) {
        return ROLE_LIST;
      }
      if (ClassLoadingCache.checkInstanceOf(className, TALKBACK_GRID_CLASSNAME)) {
        return ROLE_GRID;
      }
      // Staggered grids don't always map neatly to row and column semantics (vertical staggered
      // grids only have clear columns and horizontal staggered grids only have clear rows). We
      // distinguish staggered grids from regular grids, which have both well defined rows and
      // columns.
      if (ClassLoadingCache.checkInstanceOf(className, TALKBACK_STAGGERED_GRID_CLASSNAME)) {
        return ROLE_STAGGERED_GRID;
      }

      // Any collection that does not explicitly set a classname has its role assigned based on the
      // collection count.
      if (collection.getRowCount() > 1 && collection.getColumnCount() > 1) {
        return ROLE_GRID;
      } else {
        return ROLE_LIST;
      }
    }

    if (ClassLoadingCache.checkInstanceOf(className, NON_MODAL_ALERT_CLASSNAME)) {
      return ROLE_NON_MODAL_ALERT;
    }

    if (ClassLoadingCache.checkInstanceOf(className, SNACKBAR_CLASSNAME)) {
      return ROLE_SNACKBAR;
    }

    if (ClassLoadingCache.checkInstanceOf(className, AUDIO_CAPTION_CLASSNAME)) {
      return ROLE_AUDIO_CAPTION;
    }

    // Inheritance: View->ViewGroup->FrameLayout->NavigationView
    if (ClassLoadingCache.checkInstanceOf(className, MATERIAL_NAVIGATION_VIEW_CLASSNAME)
        // Inheritance: View->ViewGroup->FrameLayout->NavigationBarView->NavigationRailView
        || ClassLoadingCache.checkInstanceOf(className, MATERIAL_NAVIGATION_RAIL_CLASSNAME)
        // Inheritance: View->ViewGroup->FrameLayout->NavigationBarView
        || ClassLoadingCache.checkInstanceOf(className, MATERIAL_NAVIGATION_BAR_CLASSNAME)) {
      return ROLE_NAVIGATION;
    }

    // Inheritance: View->ViewGroup->LinearLayout->SearchView
    if (ClassLoadingCache.checkInstanceOf(className, SEARCH_CLASSNAME)) {
      return ROLE_SEARCH;
    }

    if (ClassLoadingCache.checkInstanceOf(className, android.view.ViewGroup.class)) {
      return ROLE_VIEW_GROUP;
    }

    // //////////////////////////////////////////////////////////////////////////////////////////
    // Subclasses of Dialog.

    // Inheritance: Dialog->AlertDialog
    if (ClassLoadingCache.checkInstanceOf(className, ALERT_DIALOG_CLASSNAME)) {
      return ROLE_ALERT_DIALOG;
    }

    if (ClassLoadingCache.checkInstanceOf(className, DIALOG_CLASSNAME)) {
      return ROLE_DIALOG;
    }

    return ROLE_NONE;
  }

  /**
   * Gets {@link Role} for {@link AccessibilityNodeInfo}. @See {@link
   * #getRole(AccessibilityNodeInfoCompat)}
   */
  @RoleName
  public static int getRole(@Nullable AccessibilityNodeInfo node) {
    if (node == null) {
      return Role.ROLE_NONE;
    }

    AccessibilityNodeInfoCompat nodeCompat = AccessibilityNodeInfoUtils.toCompat(node);
    return getRole(nodeCompat);
  }

  /** For use in logging. */
  public static String roleToString(@RoleName int role) {
    return switch (role) {
      case ROLE_NONE -> "ROLE_NONE";
      case ROLE_BUTTON -> "ROLE_BUTTON";
      case ROLE_CHECK_BOX -> "ROLE_CHECK_BOX";
      case ROLE_DROP_DOWN_LIST -> "ROLE_DROP_DOWN_LIST";
      case ROLE_EDIT_TEXT -> "ROLE_EDIT_TEXT";
      case ROLE_GRID -> "ROLE_GRID";
      case ROLE_IMAGE -> "ROLE_IMAGE";
      case ROLE_IMAGE_BUTTON -> "ROLE_IMAGE_BUTTON";
      case ROLE_LIST -> "ROLE_LIST";
      case ROLE_RADIO_BUTTON -> "ROLE_RADIO_BUTTON";
      case ROLE_SEEK_CONTROL -> "ROLE_SEEK_CONTROL";
      case ROLE_SWITCH -> "ROLE_SWITCH";
      case ROLE_TAB_BAR -> "ROLE_TAB_BAR";
      case ROLE_TOGGLE_BUTTON -> "ROLE_TOGGLE_BUTTON";
      case ROLE_VIEW_GROUP -> "ROLE_VIEW_GROUP";
      case ROLE_WEB_VIEW -> "ROLE_WEB_VIEW";
      case ROLE_PAGER -> "ROLE_PAGER";
      case ROLE_CHECKED_TEXT_VIEW -> "ROLE_CHECKED_TEXT_VIEW";
      case ROLE_PROGRESS_BAR -> "ROLE_PROGRESS_BAR";
      case ROLE_ACTION_BAR_TAB -> "ROLE_ACTION_BAR_TAB";
      case ROLE_DRAWER_LAYOUT -> "ROLE_DRAWER_LAYOUT";
      case ROLE_SLIDING_DRAWER -> "ROLE_SLIDING_DRAWER";
      case ROLE_ICON_MENU -> "ROLE_ICON_MENU";
      case ROLE_TOAST -> "ROLE_TOAST";
      case ROLE_ALERT_DIALOG -> "ROLE_ALERT_DIALOG";
      case ROLE_DATE_PICKER_DIALOG -> "ROLE_DATE_PICKER_DIALOG";
      case ROLE_TIME_PICKER_DIALOG -> "ROLE_TIME_PICKER_DIALOG";
      case ROLE_DATE_PICKER -> "ROLE_DATE_PICKER";
      case ROLE_TIME_PICKER -> "ROLE_TIME_PICKER";
      case ROLE_NUMBER_PICKER -> "ROLE_NUMBER_PICKER";
      case ROLE_SCROLL_VIEW -> "ROLE_SCROLL_VIEW";
      case ROLE_HORIZONTAL_SCROLL_VIEW -> "ROLE_HORIZONTAL_SCROLL_VIEW";
      case ROLE_KEYBOARD_KEY -> "ROLE_KEYBOARD_KEY";
      case ROLE_TALKBACK_EDIT_TEXT_OVERLAY -> "ROLE_TALKBACK_EDIT_TEXT_OVERLAY";
      case ROLE_TEXT_ENTRY_KEY -> "ROLE_TEXT_ENTRY_KEY";
      case ROLE_STAGGERED_GRID -> "ROLE_STAGGERED_GRID";
      case ROLE_FLOATING_ACTION_BUTTON -> "ROLE_FLOATING_ACTION_BUTTON";
      case ROLE_NON_MODAL_ALERT -> "ROLE_NON_MODAL_ALERT";
      case ROLE_SNACKBAR -> "ROLE_SNACKBAR";
      case ROLE_AUDIO_CAPTION -> "ROLE_AUDIO_CAPTION";
      case ROLE_DIALOG -> "ROLE_DIALOG";
      case ROLE_NAVIGATION -> "ROLE_NAVIGATION";
      case ROLE_SEARCH -> "ROLE_SEARCH";
      case ROLE_VOICE_DICTATION_BUTTON -> "ROLE_VOICE_DICTATION_BUTTON";
      default -> "(unknown role " + role + ")";
    };
  }

  /**
   * Whether the role is an adjustable role. Need to check the ancestor of the node to see if it is
   * a number picker.
   */
  public static boolean isAdjustableRole(@RoleName int role) {
    return role == Role.ROLE_SEEK_CONTROL
        || role == Role.ROLE_DATE_PICKER
        || role == Role.ROLE_TIME_PICKER
        || role == Role.ROLE_NUMBER_PICKER;
  }
}
