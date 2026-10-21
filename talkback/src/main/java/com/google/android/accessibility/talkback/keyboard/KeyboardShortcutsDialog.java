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

package com.google.android.accessibility.talkback.keyboard;

import static com.google.android.accessibility.talkback.keyboard.KeyboardShortcutsRecyclerItemAdapter.COMMAND_ITEM_TYPE;
import static com.google.android.accessibility.talkback.keyboard.KeyboardShortcutsRecyclerItemAdapter.ItemData;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StyleSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.search.StringMatcher;
import com.google.android.accessibility.talkback.actor.search.StringMatcher.MatchResult;
import com.google.android.accessibility.talkback.compositor.KeyComboManagerUtils;
import com.google.android.accessibility.talkback.dialog.BaseDialog;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;

/** A dialog to show all keyboard commands and let users to search any command. */
public class KeyboardShortcutsDialog extends BaseDialog {
  private static final String TAG = "KeyboardShortcutsDialog";

  // TODO: b/404273230 - Update the category prefix.
  private static final String CATEGORY_GLOBAL_PREFIX = "keycombo_shortcut_global_";
  private static final String CATEGORY_OTHERS_PREFIX = "keycombo_shortcut_other_";

  private @Nullable KeyComboManager keyComboManager;

  /** The data list for the recycler view including categories and keyboard commands. */
  private List<ItemData> initialDataList;

  // UI elements.
  private EditText keywordEditText;
  private ImageButton clearInputButton;
  private RecyclerView recyclerView;
  private TextView noResultsView;

  /** Flag to ensure we set height only once after showing the dialog. */
  private boolean initialHeightSet = false;

  /** The adapter for the recycler view. */
  private KeyboardShortcutsRecyclerItemAdapter recyclerItemAdapter;

  /** The state for search result. */
  private SearchState searchState;

  public KeyboardShortcutsDialog(Context context) {
    super(context, R.string.title_show_keyboard_shortcuts, /* pipeline= */ null);
    setIncludePositiveButton(false);
  }

  @Override
  public void handleDialogClick(int buttonClicked) {
    initialHeightSet = false;
  }

  @Override
  public void handleDialogDismiss() {
    initialHeightSet = false;
  }

  @Override
  public String getMessageString() {
    return null;
  }

  @SuppressLint("InflateParams")
  @Override
  public View getCustomizedView(LayoutInflater inflater) {
    final LinearLayout root =
        (LinearLayout) inflater.inflate(R.layout.keyboard_shortcuts_dialog, /* root= */ null);

    keywordEditText = root.findViewById(R.id.keyword_edit);
    keywordEditText.requestFocus();
    clearInputButton = root.findViewById(R.id.clear_button);

    recyclerView = root.findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(context));

    initialDataList = getRecyclerItemDataList();
    recyclerItemAdapter = new KeyboardShortcutsRecyclerItemAdapter(initialDataList);
    recyclerView.setAdapter(recyclerItemAdapter);

    noResultsView = root.findViewById(R.id.no_results_view);

    keywordEditText.addTextChangedListener(
        new TextWatcher() {
          String previousKeyword;

          @Override
          public void afterTextChanged(Editable s) {
            // Explicitly retrieve the keyword from `keywordEditText` instead of `s.toString()`
            // being passed to this function; the latter sometimes includes an unexpected prefix
            // (e.g. "Editing. "), which seems to be coming from the TalkBack speech feedback.
            String keyword = keywordEditText.getText().toString().trim();
            LogUtils.d(TAG, "afterTextChanged keyword: %s", keyword.toString());

            clearInputButton.setVisibility(keyword.length() > 0 ? View.VISIBLE : View.GONE);
            if (TextUtils.equals(keyword, previousKeyword)) {
              return;
            }

            if (!keyword.isEmpty()) {
              // Do keyword search.
              searchKeywordAndUpdateAdapter(keyword);
            } else {
              resetAdapter();
            }

            previousKeyword = keyword;
          }

          @Override
          public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            LogUtils.d(
                TAG,
                "beforeTextChanged s: %s, start: %d, count: %d, after: %d",
                s.toString(),
                start,
                count,
                after);
          }

          @Override
          public void onTextChanged(CharSequence s, int start, int before, int count) {
            LogUtils.d(
                TAG,
                "onTextChanged s: %s, start: %d, before: %d, count: %d",
                s.toString(),
                start,
                before,
                count);
          }
        });

    // Support the down arrow key to focus on the first item in the recycler view.
    keywordEditText.setOnKeyListener(
        (v, keyCode, event) -> {
          if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (recyclerView != null && recyclerView.getVisibility() == View.VISIBLE) {
              recyclerView.requestFocus();
              return true;
            } else if (noResultsView != null && noResultsView.getVisibility() == View.VISIBLE) {
              noResultsView.requestFocus();
              return true;
            }
          }
          return false;
        });

    keywordEditText.setOnEditorActionListener(
        (v, actionId, event) -> {
          if (actionId == EditorInfo.IME_ACTION_SEARCH) {
            InputMethodManager imm =
                (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
              imm.hideSoftInputFromWindow(keywordEditText.getWindowToken(), /* flags= */ 0);
            }
            return true;
          }
          return false;
        });

    clearInputButton.setOnClickListener(v -> keywordEditText.getText().clear());

    // While showing searching results, we want the height of the view to be as same as the height
    // of its initial drawn.
    recyclerView
        .getViewTreeObserver()
        .addOnGlobalLayoutListener(
            new OnGlobalLayoutListener() {
              @Override
              public void onGlobalLayout() {
                if (!initialHeightSet && recyclerView.getHeight() > 0) {
                  final int height = recyclerView.getHeight();
                  recyclerView.setMinimumHeight(height);
                  noResultsView.setMinimumHeight(height);
                  initialHeightSet = true;

                  // Remove the listener to avoid it running multiple times
                  recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
              }
            });

    return root;
  }

  public void setKeyComboManager(KeyComboManager keyComboManager) {
    this.keyComboManager = keyComboManager;
  }

  private Map<String, List<ItemData>> groupCommandsByCategory() {
    Map<String, List<ItemData>> categoryToCommandsMap = new HashMap<>();
    if (keyComboManager == null) {
      return categoryToCommandsMap;
    }

    // TODO: b/404273230 - Update the category to commands map.
    List<ItemData> globalCommandsList = new ArrayList<>();
    List<ItemData> othersCommandsList = new ArrayList<>();
    List<ItemData> navigationCommandsList = new ArrayList<>();

    ImmutableSet<String> browseModeOnlyCommands = keyComboManager.getBrowseModeOnlyCommands();
    String browseModeCommandTitleTemplate =
        browseModeOnlyCommands.isEmpty()
            ? ""
            : context.getString(R.string.template_keycombo_menu_browse_mode_command);
    for (String key : keyComboManager.getKeyComboModel().getKeyComboMap().keySet()) {
      TalkBackPhysicalKeyboardShortcut action =
          TalkBackPhysicalKeyboardShortcut.getActionFromKey(context.getResources(), key);
      if (action == null || action.getKeyStrRes() == -1) {
        continue;
      }

      String commandName =
          browseModeOnlyCommands.contains(key) && !browseModeCommandTitleTemplate.isEmpty()
              ? String.format(
                  browseModeCommandTitleTemplate, action.getDescription(context.getResources()))
              : action.getDescription(context.getResources());
      String keyComboString =
          KeyComboManagerUtils.getKeyComboStringRepresentation(
              context, keyComboManager, action.getKeyStrRes());
      if (key.startsWith(CATEGORY_GLOBAL_PREFIX)) {
        globalCommandsList.add(new ItemData(commandName, keyComboString));
      } else if (key.startsWith(CATEGORY_OTHERS_PREFIX)) {
        othersCommandsList.add(new ItemData(commandName, keyComboString));
      } else {
        navigationCommandsList.add(new ItemData(commandName, keyComboString));
      }
    }

    // TODO: b/404273230 - Update the categories names.
    categoryToCommandsMap.put(
        context.getString(R.string.keycombo_menu_category_global), globalCommandsList);
    categoryToCommandsMap.put(
        context.getString(R.string.keycombo_menu_category_other), othersCommandsList);
    categoryToCommandsMap.put(
        context.getString(R.string.keycombo_menu_category_navigation), navigationCommandsList);

    return categoryToCommandsMap;
  }

  private List<ItemData> getRecyclerItemDataList() {
    Map<String, List<ItemData>> categoryToCommandsMap = groupCommandsByCategory();
    List<ItemData> itemDataList = new ArrayList<>();

    for (String category : categoryToCommandsMap.keySet()) {
      itemDataList.add(new ItemData(category));
      itemDataList.addAll(categoryToCommandsMap.get(category));
    }

    return itemDataList;
  }

  private void searchKeywordAndUpdateAdapter(CharSequence keyword) {
    if (searchState != null) {
      searchState.clear();
    }
    searchState = search(keyword);

    // Show the initial list if user input is empty.
    if (searchState == null) {
      resetAdapter();
      return;
    }

    List<ItemData> searchResultList = new ArrayList<>();
    for (SearchState.MatchedItemInfo matchedItemInfo : searchState.getSearchResults()) {
      ItemData updatedItemData = getAllKeywordsBoldItemData(matchedItemInfo);
      if (updatedItemData != null) {
        searchResultList.add(updatedItemData);
      }
    }
    updateAdapter(searchResultList);
  }

  private @Nullable ItemData getAllKeywordsBoldItemData(
      SearchState.MatchedItemInfo matchedItemInfo) {
    if (!matchedItemInfo.hasMatchedResults()) {
      return null;
    }

    SpannableStringBuilder updatedTitle =
        new SpannableStringBuilder(matchedItemInfo.getItem().getTitle());
    for (MatchResult matchResult : matchedItemInfo.getMatchResults()) {
      updatedTitle.setSpan(
          new StyleSpan(Typeface.BOLD),
          matchResult.start(),
          matchResult.end(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return new ItemData(updatedTitle, matchedItemInfo.getItem().getKeyComboString());
  }

  private @Nullable SearchState search(@Nullable CharSequence userInput) {
    // Return null if the user input is empty.
    if (TextUtils.isEmpty(userInput)) {
      return null;
    }

    // Return null if userInput contains spaces only.
    String trimmedUserInput = userInput.toString().trim();
    if (trimmedUserInput.isEmpty()) {
      return null;
    }

    SearchState state = new SearchState();
    for (ItemData item : initialDataList) {
      if (item.getViewType() != COMMAND_ITEM_TYPE) {
        continue;
      }

      List<MatchResult> matchResults =
          StringMatcher.findMatches(item.getTitle().toString(), trimmedUserInput);
      if (!matchResults.isEmpty()) {
        state.addResult(new SearchState.MatchedItemInfo(item, matchResults));
      }
    }

    return state;
  }

  private void resetAdapter() {
    recyclerItemAdapter.setItemDataList(initialDataList);
    setVisibilityForNoResultsView(false);
  }

  private void updateAdapter(List<ItemData> itemDataList) {
    recyclerItemAdapter.setItemDataList(itemDataList);
    setVisibilityForNoResultsView(itemDataList.isEmpty());
  }

  private void setVisibilityForNoResultsView(boolean visibility) {
    noResultsView.setVisibility(visibility ? View.VISIBLE : View.GONE);
    recyclerView.setVisibility(visibility ? View.GONE : View.VISIBLE);
  }

  /** Define necessary data for dialog to display. */
  private class SearchState {
    static class MatchedItemInfo {
      /** The item from adapter for the matched info. */
      private final ItemData item;

      private ImmutableList<MatchResult> matchResults;

      public MatchedItemInfo(ItemData item, List<MatchResult> matchResults) {
        this.item = item;
        this.matchResults = ImmutableList.copyOf(matchResults);
      }

      public ItemData getItem() {
        return item;
      }

      public List<MatchResult> getMatchResults() {
        return new ArrayList<>(matchResults);
      }

      /** Returns the title of the item. If the item isn't a command item, returns null. */
      public @Nullable CharSequence getItemTitle() {
        if (item.getViewType() == COMMAND_ITEM_TYPE) {
          return item.getTitle();
        }
        return null;
      }

      boolean hasMatchedResults() {
        return item != null && !TextUtils.isEmpty(getItemTitle()) && !matchResults.isEmpty();
      }
    }

    private List<MatchedItemInfo> searchResults = new ArrayList<>();

    public SearchState() {}

    public void addResult(MatchedItemInfo matchedItemInfo) {
      if (matchedItemInfo == null) {
        return;
      }
      searchResults.add(matchedItemInfo);
    }

    public void clear() {
      searchResults.clear();
    }

    public MatchedItemInfo getMatchedItemInfo(int position) {
      MatchedItemInfo result = searchResults.get(position);
      return new MatchedItemInfo(result.getItem(), result.getMatchResults());
    }

    public List<MatchedItemInfo> getSearchResults() {
      return new ArrayList<>(searchResults);
    }
  }

  ///////////////////////////////////////////////////////////////////////////////////////
  // VisibleForTesting methods
  @VisibleForTesting
  List<ItemData> getInitialDataList() {
    return initialDataList;
  }

  @VisibleForTesting
  void setInitialDataList(List<ItemData> initialDataList) {
    this.initialDataList = initialDataList;
    resetAdapter();
  }

  @VisibleForTesting
  int getItemCountInAdapter() {
    return recyclerItemAdapter.getItemCount();
  }

  @VisibleForTesting
  EditText getKeywordEditText() {
    return keywordEditText;
  }
}
