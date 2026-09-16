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

import android.content.Context;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ClickableSpan;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.DirectionNavigationActor;
import com.google.android.accessibility.talkback.actor.search.SearchState.MatchedNodeInfo;
import com.google.android.accessibility.talkback.actor.search.StringMatcher.MatchResult;
import com.google.android.accessibility.talkback.labeling.TalkBackLabelManager;
import com.google.android.accessibility.utils.AccessibilityNode;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityWindow;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.Role;
import com.google.android.accessibility.utils.Role.RoleName;
import java.util.ArrayList;
import java.util.List;

/** Searches keyword in screen nodes. */
public final class SearchScreenNodeStrategy {
  private final Context context;

  /** Observer instance which need to notify when search done. */
  @Nullable private SearchObserver observer;

  /** The custom label manager that is used for search screen node. */
  @Nullable private final TalkBackLabelManager labelManager;

  /** Stores last-searched keyword. */
  @Nullable private CharSequence lastKeyword;

  /** The cache for all searchable nodes on current screen. */
  private final ScreenNodesCache nodesCache;

  /** The type of list to show. */
  public enum ListType {
    HEADING,
    LANDMARK,
    LINK,
    CONTROL,
    TABLE,
  }

  /**
   * Creates a new SearchScreenNodeStrategy instance.
   *
   * @param observer The Observer which need to be notified when search done.
   * @param labelManager The custom label manager, or {@code null} if the API version does not
   */
  public SearchScreenNodeStrategy(
      @Nullable SearchObserver observer,
      @Nullable TalkBackLabelManager labelManager,
      Context context) {
    this.observer = observer;
    this.labelManager = labelManager;
    this.nodesCache = new ScreenNodesCache();
    this.context = context;
  }

  /** Returns last-searched keyword or null if no keyword was searched. */
  @Nullable
  public CharSequence getLastKeyword() {
    return lastKeyword;
  }

  /** Resets last-searched keyword. */
  public void resetLastKeyword() {
    lastKeyword = null;
  }

  /** Runs search, notifies observer of results. */
  public void searchByKeyword(CharSequence keyword) {
    SearchState searchState = search(keyword);
    if (observer != null) {
      observer.updateSearchState(searchState);
    }
  }

  /** Searches the list for the given {@code listType}, notifies observer of results. */
  public void searchByListType(ListType listType) {
    SearchState searchState = searchList(listType);
    if (observer != null) {
      observer.updateSearchState(searchState);
    }
  }

  /**
   * Searches the list for the given {@code listType} and {@code keyword}, notifies observer of
   * results.
   *
   * @param listType the type of list to search
   * @param keyword the keyword to search for
   * @param searchTrigger the action that triggered the search. For searches triggered by a user
   *     entering text into the search box, this should be {@link
   *     SearchState.SearchType.SEARCH_TYPE_KEYWORD}. For searches triggered by a user selecting a
   *     list type from the search overlay, this should be {@link
   *     SearchState.SearchType.SEARCH_TYPE_LIST}.
   */
  public void searchByListTypeAndKeyword(
      ListType listType, CharSequence keyword, SearchState.SearchType searchTrigger) {
    SearchState searchState = performSearchInternal(keyword, listType, searchTrigger);
    if (observer != null) {
      observer.updateSearchState(searchState);
    }
  }

  /** Returns true if the given {@code node} is of the given {@code listType}. */
  private boolean isTargetListType(AccessibilityNode node, ListType listType) {
    AccessibilityNodeInfoCompat nodeCompat = node.getCompat();
    switch (listType) {
      case HEADING -> {
        // Screen search should not return collection items (e.g. table headers) as headings.
        if (nodeCompat.getCollectionItemInfo() != null) {
          return false;
        }
        return AccessibilityNodeInfoUtils.isHeading(nodeCompat);
      }
      case LINK -> {
        if (AccessibilityNodeInfoUtils.isChromeRoleLink(nodeCompat)) {
          return true;
        }

        CharSequence text = AccessibilityNodeInfoUtils.getNodeText(nodeCompat);
        if (TextUtils.isEmpty(text) || !(text instanceof Spanned)) {
          return false;
        }

        Spanned spannedText = (Spanned) text;
        return spannedText.getSpans(0, spannedText.length(), ClickableSpan.class).length > 0;
      }
      case TABLE -> {
        return Role.getRole(nodeCompat) == Role.ROLE_GRID;
      }
      case LANDMARK -> {
        @RoleName int role = Role.getRole(nodeCompat);
        // Navigation and search are considered landmarks as they are key sections of a page that
        // users may want to quickly navigate to.
        return role == Role.ROLE_NAVIGATION || role == Role.ROLE_SEARCH;
      }
      case CONTROL -> {
        return AccessibilityNodeInfoUtils.FILTER_CONTROL.accept(nodeCompat);
      }
      default -> {
        return false;
      }
    }
  }

  /**
   * Searches the list of {@code listType}.
   *
   * @param listType the type of list to search
   * @return SearchState containing the nodes in {@code SearchState.result}, the {@code
   *     SearchState.result} will be empty if {@code listType} is invalid.
   */
  @NonNull
  protected SearchState searchList(ListType listType) {
    return performSearchInternal(
        /* query= */ null, listType, /* searchTrigger= */ SearchState.SearchType.SEARCH_TYPE_LIST);
  }

  /**
   * Searches {@code userInput} from the nodes cached in {@link
   * #cacheNodeTree(AccessibilityWindow)}. The search is case-insensitive and the leading/tailing
   * whitespaces in {@code userInput} will be trimmed before searching.
   *
   * @param userInput the input to be used for searching
   * @return SearchState containing the nodes in {@code SearchState.result}, the {@code
   *     SearchState.result} will be null if {@code userInput} is empty or contains only spaces.
   */
  @NonNull
  protected SearchState search(@Nullable CharSequence userInput) {
    return performSearchInternal(
        userInput,
        /* listTypeFilter= */ null,
        /* searchTrigger= */ SearchState.SearchType.SEARCH_TYPE_KEYWORD);
  }

  /** Caches all the searchable nodes in currentWindow. */
  void cacheNodeTree(@Nullable AccessibilityWindow currentWindow) {
    clearCachedNodes();

    nodesCache.cacheCurrentWindow(
        currentWindow,
        new Filter<AccessibilityNodeInfoCompat>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            // Only keep the visible nodes.
            if (!AccessibilityNodeInfoUtils.isVisible(node)) {
              return false;
            }

            @RoleName int role = Role.getRole(node);

            // Search nodes and table nodes may not have text, but they should always be included in
            // the search results so users can navigate to them.
            if (role == Role.ROLE_SEARCH || role == Role.ROLE_GRID) {
              return true;
            }

            CharSequence nodeText = TalkBackLabelManager.getNodeText(node, labelManager);
            boolean hasText = !TextUtils.isEmpty(nodeText);

            // Navigation nodes may have a container title instead of (or in addition to) text that
            // may be displayed in the list search results.
            if (role == Role.ROLE_NAVIGATION) {
              return hasText || !TextUtils.isEmpty(node.getContainerTitle());
            }

            // Keep the nodes with texts.
            return hasText;
          }
        });
  }

  void clearCachedNodes() {
    nodesCache.clearCachedNodes();
  }

  /**
   * Moves focus to next node after current focused-node, which matches target-keyword. Returns
   * success flag.
   */
  public boolean searchAndFocus(
      boolean startAtRoot,
      @Nullable final CharSequence target,
      DirectionNavigationActor directionNavigator) {

    // Clean and check target keyword.
    if (TextUtils.isEmpty(target)) {
      return false;
    }
    final String trimmedUserInput = target.toString().trim();
    if (trimmedUserInput.isEmpty()) {
      return false;
    }

    lastKeyword = trimmedUserInput;

    // Find node matching target keyword, and focus that node.
    return directionNavigator.searchAndFocus(
        startAtRoot,
        new Filter<AccessibilityNodeInfoCompat>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            if (node == null) {
              return false;
            }

            // Only keep the visible nodes.
            if (!AccessibilityNodeInfoUtils.isVisible(node)) {
              return false;
            }
            // Keep the nodes with texts.
            @Nullable CharSequence nodeText = TalkBackLabelManager.getNodeText(node, labelManager);
            if (TextUtils.isEmpty(nodeText)) {
              return false;
            }

            // Check for target-text match.
            List<MatchResult> matches =
                StringMatcher.findMatches(nodeText.toString(), trimmedUserInput);
            return (matches != null) && (!matches.isEmpty());
          }
        });
  }

  @Nullable
  private CharSequence getDisplayTextForNode(AccessibilityNode node) {
    @Nullable CharSequence displayText = null;
    AccessibilityNodeInfoCompat nodeCompat = node.getCompat();

    switch (Role.getRole(nodeCompat)) {
      case Role.ROLE_NAVIGATION -> {
        displayText = nodeCompat.getContainerTitle();
        if (TextUtils.isEmpty(displayText)) {
          displayText = AccessibilityNodeInfoUtils.getNodeText(nodeCompat);
        }
      }
      case Role.ROLE_SEARCH -> {
        displayText = nodeCompat.getContainerTitle();
        if (TextUtils.isEmpty(displayText)) {
          // Search nodes may not have any text content, so in that case, display the literal
          // "Search" so it shows up in the search results.
          displayText = context.getString(R.string.label_search_result_landmark_search);
        }
      }
      case Role.ROLE_GRID -> {
        displayText = nodeCompat.getContainerTitle();
        if (TextUtils.isEmpty(displayText)) {
          // Table nodes may not have any text content, so in that case, display the literal
          // "Table" so it shows up in the search results.
          displayText = context.getString(R.string.label_search_result_table);
        }
      }
      default -> displayText = AccessibilityNodeInfoUtils.getNodeText(nodeCompat);
    }

    return displayText;
  }

  @Nullable
  private MatchedNodeInfo getMatchedNodeInfoForNodeWithQuery(
      AccessibilityNode node, CharSequence query) {
    @Nullable CharSequence displayText = getDisplayTextForNode(node);
    if (TextUtils.isEmpty(displayText)) {
      return null;
    }
    List<MatchResult> matchResults =
        StringMatcher.findMatches(displayText.toString(), query.toString());

    if (matchResults.isEmpty()) {
      return null;
    }

    return new MatchedNodeInfo(node, matchResults, displayText);
  }

  /**
   * Internal method to perform search via list type and/or text query.
   *
   * @param query the query to search. If null, search will be performed by list type only.
   * @param listTypeFilter the type of list to search. If null, search will be performed by text
   *     query only.
   * @param searchTrigger the action that triggered the search. This doesn't affect the results, but
   *     is used to populate the {@code SearchState}.
   */
  private SearchState performSearchInternal(
      @Nullable CharSequence query,
      @Nullable ListType listTypeFilter,
      SearchState.SearchType searchTrigger) {
    SearchState state = new SearchState(searchTrigger);

    @Nullable String trimmedQuery = !TextUtils.isEmpty(query) ? query.toString().trim() : null;
    lastKeyword = trimmedQuery;

    for (AccessibilityNode node : nodesCache.getCachedNodes()) {
      if (listTypeFilter != null && !isTargetListType(node, listTypeFilter)) {
        continue;
      }

      if (trimmedQuery != null) {
        @Nullable
        MatchedNodeInfo matchedNodeInfo = getMatchedNodeInfoForNodeWithQuery(node, trimmedQuery);
        if (matchedNodeInfo != null) {
          state.addResult(matchedNodeInfo);
        }
      } else if (listTypeFilter != null) {
        @Nullable CharSequence displayText = getDisplayTextForNode(node);
        if (!TextUtils.isEmpty(displayText)) {
          List<MatchResult> result = new ArrayList<>();
          result.add(new MatchResult(displayText));
          state.addResult(new MatchedNodeInfo(node, result, displayText));
        }
      }
    }
    return state;
  }
}
