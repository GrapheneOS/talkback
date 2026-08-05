/*
 * Copyright (C) 2024 Google Inc.
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
package com.google.android.accessibility.utils.traversal;

import com.google.auto.value.AutoValue;

/** Config class for {@link OrderedTraversalStrategy} */
@AutoValue
public abstract class OrderedTraversalStrategyConfig {

  public abstract int searchDirection();

  public abstract boolean includeChildrenOfNodesWithWebActions();

  public abstract boolean makeFabFirst();

  public static OrderedTraversalStrategyConfig.Builder builder() {
    return new AutoValue_OrderedTraversalStrategyConfig.Builder()
        .setSearchDirection(0)
        .setIncludeChildrenOfNodesWithWebActions(false)
        .setMakeFabFirst(false);
  }

  /** Builder for TraversalStrategy config data. */
  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setSearchDirection(int searchDirection);

    public abstract Builder setIncludeChildrenOfNodesWithWebActions(boolean value);

    public abstract Builder setMakeFabFirst(boolean value);

    public abstract OrderedTraversalStrategyConfig build();
  }
}
