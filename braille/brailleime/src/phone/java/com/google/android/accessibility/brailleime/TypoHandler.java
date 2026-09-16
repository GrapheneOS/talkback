/*
 * Copyright (C) 2023 Google Inc.
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

package com.google.android.accessibility.brailleime;

import static android.text.style.SuggestionSpan.FLAG_GRAMMAR_ERROR;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.TtsSpan;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputConnection;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.braille.common.BrailleTypoFinder;
import com.google.android.accessibility.braille.common.BrailleTypoFinder.NoTypoFocusFoundException;
import com.google.android.accessibility.braille.common.TalkBackSpeaker;
import com.google.android.accessibility.braille.interfaces.ScreenReaderActionPerformer.ScreenReaderAction;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils.SpellingSuggestion;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/** Class handles all the typo correction functionalities in Braille keyboard. */
public class TypoHandler {
  private static final String TAG = "TypoHandler";
  private static final String SEPARATOR_PERIOD = ". ";
  private static final String SPACE = " ";
  private static final int NON_INITIALIZED = -1;
  private final TalkBackSpeaker talkBackSpeaker;
  private final Context context;
  private BrailleTypoFinder typoFinder;
  private CharSequence lastCorrectedTypo;
  private int lastCorrectedTypoHeadIndex;
  private int lastCorrectedSuggestionTailIndex;
  private final Callback callback;

  /** Callback for {@link TypoHandler}. */
  interface Callback {
    /** Returns the {@link InputConnection} of the current focused edit text. */
    InputConnection getInputConnection();

    /** Returns the {@link FocusFinder} to find the focused node. */
    FocusFinder getFocusFinder();

    /** Performs the {@link ScreenReaderAction} with the given arguments. */
    boolean performAction(ScreenReaderAction action, Object... arg);
  }

  public TypoHandler(Context context, Callback callback, TalkBackSpeaker talkBackSpeaker) {
    this.context = context;
    this.callback = callback;
    this.typoFinder = new BrailleTypoFinder(callback.getFocusFinder());
    this.talkBackSpeaker = talkBackSpeaker;
    clearCorrectionCache();
  }

  /** Refreshes focused typo. */
  @CanIgnoreReturnValue
  public boolean updateTypoTarget() {
    clearCorrectionCache();
    return typoFinder.updateTypoCorrectionFrom(context, AccessibilityNodeInfo.FOCUS_INPUT);
  }

  /** Navigates to next suggestion for current typo. */
  @CanIgnoreReturnValue
  public boolean nextSuggestion() {
    String suggestion;
    String indexString;
    try {
      suggestion = typoFinder.nextSuggestion();
      ImmutableList<String> suggestionList = typoFinder.getSuggestionCandidates();
      indexString =
          context.getString(
              R.string.index_of_spelling_suggestion,
              suggestionList.indexOf(suggestion) + 1,
              suggestionList.size());
    } catch (NoTypoFocusFoundException e) {
      if (updateTypoTarget()) {
        return nextSuggestion();
      }
      return false;
    }
    talkBackSpeaker.speak(
        getSuggestionSpannableString(suggestion, getSuggestionTypeString(), indexString));
    return true;
  }

  /** Navigates to previous suggestion for current typo. */
  @CanIgnoreReturnValue
  public boolean previousSuggestion() {
    String suggestion;
    String indexString;
    try {
      suggestion = typoFinder.previousSuggestion();
      ImmutableList<String> suggestionList = typoFinder.getSuggestionCandidates();
      indexString =
          context.getString(
              R.string.index_of_spelling_suggestion,
              suggestionList.indexOf(suggestion) + 1,
              suggestionList.size());
    } catch (NoTypoFocusFoundException e) {
      if (updateTypoTarget()) {
        return previousSuggestion();
      }
      return false;
    }
    talkBackSpeaker.speak(
        getSuggestionSpannableString(suggestion, getSuggestionTypeString(), indexString));
    return true;
  }

  /** Confirms current suggestion as final. */
  public boolean confirmSuggestion() {
    String suggestionCandidate;
    try {
      suggestionCandidate = typoFinder.getCurrentSuggestionCandidate();
      if (TextUtils.isEmpty(suggestionCandidate)) {
        // There are 2 possible causes: 1. This typo doesn't have suggestions. 2. User hasn't chosen
        // a suggestion.
        BrailleImeLog.e(TAG, "suggestion is empty");
        return false;
      }
    } catch (NoTypoFocusFoundException e) {
      BrailleImeLog.e(TAG, "no typo found", e);
      return false;
    }
    SpellingSuggestion spellingSuggestion = typoFinder.getSpellingSuggestion();
    AccessibilityNodeInfoCompat node = typoFinder.getTargetNode();
    lastCorrectedTypoHeadIndex = spellingSuggestion.start();
    lastCorrectedTypo =
        spellingSuggestion
            .misspelledWord()
            .subSequence(0, spellingSuggestion.misspelledWord().length());
    lastCorrectedSuggestionTailIndex =
        lastCorrectedTypoHeadIndex + suggestionCandidate.length() - 1;
    typoFinder.clear();
    return callback.performAction(
        ScreenReaderAction.TYPO_CORRECT, node, suggestionCandidate, spellingSuggestion);
  }

  /** Undoes last confirmed final to original typo. */
  public boolean undoConfirmSuggestion() {
    InputConnection inputConnection = callback.getInputConnection();
    if (inputConnection == null) {
      BrailleImeLog.w(TAG, "inputConnection is null");
      return false;
    }
    if (lastCorrectedTypo == null) {
      BrailleImeLog.w(TAG, "lastCorrectedTypo is null");
      return false;
    }
    // Replace replaced suggestion with original typo.
    inputConnection.beginBatchEdit();
    inputConnection.setComposingRegion(
        lastCorrectedTypoHeadIndex, lastCorrectedSuggestionTailIndex + 1);
    inputConnection.setComposingText(lastCorrectedTypo, 0);
    inputConnection.finishComposingText();
    inputConnection.endBatchEdit();
    clearCorrectionCache();
    return true;
  }

  /** Whether grammar mistake or not. */
  public boolean isGrammarMistake() {
    return (typoFinder.getSuggestionSpanFlag() & FLAG_GRAMMAR_ERROR) != 0;
  }

  private void clearCorrectionCache() {
    lastCorrectedTypoHeadIndex = NON_INITIALIZED;
    lastCorrectedTypo = null;
    lastCorrectedSuggestionTailIndex = NON_INITIALIZED;
  }

  /**
   * The suggestion will first be read aloud as a whole word, and then each of its characters will
   * be read out individually. The index string will be read out at the end.
   */
  private Spanned getSuggestionSpannableString(String suggestion, String type, String indexSting) {
    return new SpannableStringBuilder()
        .append(suggestion)
        .append(SEPARATOR_PERIOD)
        .append(type)
        .append(SEPARATOR_PERIOD)
        .append(SPACE, new TtsSpan.VerbatimBuilder(suggestion).build(), 0)
        .append(SEPARATOR_PERIOD)
        .append(indexSting);
  }

  private String getSuggestionTypeString() {
    return context.getString(
        isGrammarMistake() ? R.string.grammar_suggestion : R.string.spelling_suggestion);
  }

  @VisibleForTesting
  void testing_setTypoFinder(BrailleTypoFinder typoFinder) {
    this.typoFinder = typoFinder;
  }
}
