/*
 * Copyright (C) 2018 The Android Open Source Project
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

package com.google.android.accessibility.talkback.actor.search;

import android.text.TextUtils;
import com.google.android.accessibility.talkback.actor.search.StringMatcher.MatchResult;
import com.google.android.accessibility.utils.AccessibilityNode;
import java.util.ArrayList;
import java.util.List;

/** Define necessary data for screen search UI to display. */
final class SearchState {
  /**
   * Data class to hold the matched {@code AccessibilityNode} and the {@code MatchResult} containing
   * the match index information.
   */
  static class MatchedNodeInfo {
    // TODO: [Screen Search] Enables AutoValue for screen search when AOSP verified to
    // allow using AutoValue

    /** Node containing the matched info. */
    private AccessibilityNode node;

    private List<MatchResult> matchResults;

    /** The text to be displayed for this node in the search results. */
    private CharSequence displayText;

    /**
     * Creates a {@code MatchedNodeInfo}.
     *
     * @param node node containing the matched info.
     * @param matchResults containing the {@code MatchResult} to indicate the keyword match info
     *     presented in the {@code node.getNodeText()}
     */
    public MatchedNodeInfo(AccessibilityNode node, List<MatchResult> matchResults) {
      this.node = node;
      this.matchResults = new ArrayList<>(matchResults);
      // By default, display the text of the node. This may be overridden for list-based searches.
      this.displayText = node.getNodeText();
    }

    /**
     * Creates a {@code MatchedNodeInfo} with a specific display text.
     *
     * @param node node containing the matched info.
     * @param matchResults containing the {@code MatchResult} to indicate the keyword match info
     *     presented in the {@code node.getNodeText()}
     * @param displayText the text to be displayed for this node in the search results.
     */
    public MatchedNodeInfo(
        AccessibilityNode node, List<MatchResult> matchResults, CharSequence displayText) {
      this.node = node;
      this.matchResults = new ArrayList<>(matchResults);
      this.displayText = displayText;
    }

    /** Gets the {@code AccessibilityNode} that containing the matched content. */
    public AccessibilityNode node() {
      return node;
    }

    public List<MatchResult> matchResults() {
      return new ArrayList<>(matchResults);
    }

    /** Gets the text to be displayed for this node in the search results. */
    public CharSequence getNodeText() {
      return displayText;
    }

    /**
     * Checks if the {@code MatchedNodeInfo} has valid {@code AccessibilityNode} and {@code
     * MatchResult}, and that the display text is not empty.
     */
    boolean hasMatchedResult() {
      return node != null && !TextUtils.isEmpty(displayText) && !matchResults.isEmpty();
    }
  }

  private List<MatchedNodeInfo> result = new ArrayList<>();

  /** The types of queries that can be used to trigger a search. */
  public enum SearchType {
    /* Used when a user enters a keyword into the search box. */
    SEARCH_TYPE_KEYWORD,
    /**
     * Used when a user selects a list type (e.g. headings, links, etc.) from the search overlay.
     * See {@link SearchScreenNodeStrategy.ListType} for a full list of list types.
     */
    SEARCH_TYPE_LIST,
  }

  /** The type of query that triggered this search. */
  private SearchType searchType;

  public SearchState(SearchType searchType) {
    this.searchType = searchType;
  }

  public void addResult(MatchedNodeInfo matchedNodeInfo) {
    if (matchedNodeInfo == null) {
      return;
    }
    result.add(matchedNodeInfo);
  }

  public void clear() {
    result.clear();
  }

  public MatchedNodeInfo getResult(int index) {
    return result.get(index);
  }

  public List<MatchedNodeInfo> getResults() {
    // Do not return copied list to make it immutable since we need to purify the list while
    // extracting to adapter.
    return result;
  }

  public SearchType getSearchType() {
    return searchType;
  }
}
