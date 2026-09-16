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
package com.google.android.accessibility.utils.monitor;

/** Utility class for {@link CollectionState}. */
public class CollectionStateUtils {
  private CollectionStateUtils() {}

  /** Returns true if the collection is in row transition. */
  public static boolean getCollectionIsRowTransition(CollectionState collectionState) {
    return (collectionState.getRowColumnTransition() & CollectionState.TYPE_ROW) != 0;
  }

  /** Returns true if the collection is in column transition. */
  public static boolean getCollectionIsColumnTransition(CollectionState collectionState) {
    return (collectionState.getRowColumnTransition() & CollectionState.TYPE_COLUMN) != 0;
  }

  /** Returns the row index of the collection table item. Returns -1 if there is no row data. */
  public static int getCollectionTableItemRowIndex(CollectionState collectionState) {
    CollectionState.TableItemState itemState = collectionState.getTableItemState();
    return itemState != null ? itemState.getRowIndex() : -1;
  }

  /**
   * Returns the column index of the collection table item. Returns -1 if there is no column data.
   */
  public static int getCollectionTableItemColumnIndex(CollectionState collectionState) {
    CollectionState.TableItemState itemState = collectionState.getTableItemState();
    return itemState != null ? itemState.getColumnIndex() : -1;
  }

  /**
   * Returns the heading type of the collection table item. Returns 0 if there is no heading data.
   */
  public static int getCollectionTableItemHeadingType(CollectionState collectionState) {
    CollectionState.TableItemState itemState = collectionState.getTableItemState();
    return itemState != null ? itemState.getHeadingType() : 0;
  }

  /** Returns true if the collection has both row and column count. */
  public static boolean hasBothCount(CollectionState collectionState) {
    return hasRowCount(collectionState) && hasColumnCount(collectionState);
  }

  /** Returns true if the collection has row count. */
  public static boolean hasRowCount(CollectionState collectionState) {
    return collectionState.getCollectionRowCount() > -1;
  }

  /** Returns true if the collection has column count. */
  public static boolean hasColumnCount(CollectionState collectionState) {
    return collectionState.getCollectionColumnCount() > -1;
  }
}
