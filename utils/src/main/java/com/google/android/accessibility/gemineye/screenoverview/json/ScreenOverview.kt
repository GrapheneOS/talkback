/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.android.accessibility.gemineye.screenoverview.json

import com.google.android.accessibility.gemineye.api.NodeId
import com.google.android.accessibility.gemineye.screenoverview.gsonadapters.StringEnumTypeAdapter
import com.google.gson.annotations.JsonAdapter
import com.google.gson.annotations.SerializedName

/** The type of UI element. */
enum class UiElementType(val value: String) {
  Button("button"),
  FloatingActionButton("floating_action_button"),
  TextField("text_field"),
  Header("header"),
  Checkbox("checkbox"),
  Menu("menu"),
  Slider("slider"),
  Tab("tab"),
  Link("link");

  override fun toString() = value
}

/** The type of container that the UI element is in. */
enum class ParentType(val value: String) {
  TabBar("tab_bar"),
  TopNavigation("top_navigation"),
  BottomNavigation("bottom_navigation"),
  SideMenu("side_menu"),
  Filters("filters"),
  Post("post"),
  ListItem("list_item"),
  MainContent("main_content");

  override fun toString() = value
}

data class Rect(
  val left: Int? = null,
  val top: Int? = null,
  val right: Int? = null,
  val bottom: Int? = null,
) {
  val width: Int
    get() = right!! - left!!

  val height: Int
    get() = bottom!! - top!!

  val area: Int
    get() = width * height

  val centerX
    get() = (left!! + right!!) / 2f

  val centerY
    get() = (top!! + bottom!!) / 2f

  /** Returns true if all the bounds are set. */
  fun isValid(): Boolean =
    left != null && top != null && right != null && bottom != null && width > 0 && height > 0
}

/** JSON representation of a UI element like a button or text field. */
data class UiElement(
  val label: String? = null,
  val description: String? = null,

  /**
   * The list of nodes in the accessibility tree that could possibly match this UI element.
   *
   * Sorted by descending likelihood of match.
   */
  val potentiallyMatchingNodes: List<NodeId>? = null,

  /** The bounds of the node in the screen, in pixels */
  val bounds: Rect? = null,
  @SerializedName("parent_container")
  @JsonAdapter(StringEnumTypeAdapter.Factory::class)
  val parentContainer: ParentType? = null,
  @JsonAdapter(StringEnumTypeAdapter.Factory::class) val type: UiElementType? = null,
) {
  /**
   * Returns a unique key for the UiElement that can be used for grouping.
   *
   * Each key is the same for UiElements whose contents are semantically the same, even if the
   * potentiallyMatchingNodes are different.
   */
  @delegate:Transient
  val contentEqualityKey: Any by lazy { listOf(label, description, bounds, parentContainer, type) }
}

/** JSON representation of a ScreenOverview. */
data class ScreenOverview(
  val summary: String? = null,
  @SerializedName("top_images") val topImages: List<String>? = null,
  @SerializedName("top_ui_elements") val topUiElements: List<UiElement>? = null,
)
