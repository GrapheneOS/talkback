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

import static com.google.android.accessibility.braille.brailledisplay.controller.TimedMessager.INDICATOR_START_OR_END;
import static com.google.android.accessibility.braille.common.BrailleUserPreferences.BRAILLE_SHARED_PREFS_FILENAME;
import static com.google.android.accessibility.braille.common.translate.EditBufferUtils.NO_CURSOR;
import static java.lang.Math.max;
import static java.lang.Math.min;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.text.style.ClickableSpan;
import android.util.Range;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.braille.brailledisplay.BrailleDisplayLog;
import com.google.android.accessibility.braille.brailledisplay.analytics.BrailleDisplayAnalytics;
import com.google.android.accessibility.braille.brailledisplay.controller.ContentHelper.WrapStrategyRetriever;
import com.google.android.accessibility.braille.brailledisplay.controller.DisplayInfo.Source;
import com.google.android.accessibility.braille.brailledisplay.controller.TranslatorManager.OutputCodeChangedListener;
import com.google.android.accessibility.braille.brailledisplay.controller.wrapping.EditorWordWrapStrategy;
import com.google.android.accessibility.braille.brailledisplay.controller.wrapping.WordWrapStrategy;
import com.google.android.accessibility.braille.brailledisplay.controller.wrapping.WrapStrategy;
import com.google.android.accessibility.braille.common.BrailleUserPreferences;
import com.google.android.accessibility.braille.common.R;
import com.google.android.accessibility.braille.interfaces.BrailleWord;
import com.google.android.accessibility.braille.interfaces.SelectionRange;
import com.google.android.accessibility.braille.translate.TranslationResult;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

/** Keeps track of the current display content and handles panning. */
public class CellsContentManager implements CellsContentConsumer {
  private static final String TAG = "CellsContentManager";

  /** Indicates the cursor is at which part of the text with a position. */
  @AutoValue
  abstract static class Cursor {
    enum Type {
      TEXT_FIELD,
      HOLDINGS,
      ACTION
    }

    static Cursor create(int position, Type cursorType) {
      return new AutoValue_CellsContentManager_Cursor(position, cursorType);
    }

    public abstract int position();

    public abstract Type type();
  }

  /** Provides the active status of the IME. */
  public interface ImeStatusProvider {
    boolean isImeOpen();
  }

  /** A callback used to send braille display dots toward the hardware. */
  public interface DotDisplayer {
    void displayDots(byte[] patterns, CharSequence text, int[] brailleToTextPositions);
  }

  private static final int WHAT_PULSE = 0;
  private final Context context;
  private final ImeStatusProvider imeStatusProvider;
  private final TranslatorManager translatorManager;
  private final CellContentUpdater cellContentUpdater;
  private final TimedMessager timedMessager;
  private final DotDisplayer inputEventListener;
  private WrapStrategy editingWrapStrategy;
  private WrapStrategy preferredWrapStrategy;
  private boolean panUpOverflow;
  private Range<Integer> holdingsRange;
  private List<Range<Integer>> onScreenRange;
  private Range<Integer> actionRange;
  private DisplayInfoWrapper commonDisplayInfoWrapper;
  private DisplayInfoWrapper timedMessagePopupWrapper;
  private DisplayInfoWrapper timedMessageAnnouncementWrapper;
  private DisplayInfoWrapper timedMessageCaptionWrapper;
  private final List<OnDisplayContentChangeListener> onDisplayContentChangeListeners =
      new ArrayList<>();
  private final CellContentOverlay pulseUpdate = new CellContentOverlay();

  /**
   * Creates an instance of this class and starts the internal thread to connect to the braille
   * display service. {@code contextArg} is used to connect to the display service. {@code
   * translator} is used for braille translation. The various listeners will be called as
   * appropriate and on the same thread that was used to create this object. The current thread must
   * have a prepared looper.
   */
  public CellsContentManager(
      Context context,
      ImeStatusProvider imeStatusProvider,
      TranslatorManager translatorManagerArg,
      DotDisplayer inputEventListenerArg) {
    this.context = context;
    this.imeStatusProvider = imeStatusProvider;
    translatorManager = translatorManagerArg;
    inputEventListener = inputEventListenerArg;
    cellContentUpdater = new CellContentUpdater();
    timedMessager = new TimedMessager(context, timedMessagerCallback);
  }

  public void start(int numTextCells) {
    preferredWrapStrategy = new WordWrapStrategy(numTextCells);
    editingWrapStrategy = new EditorWordWrapStrategy(numTextCells);
    commonDisplayInfoWrapper =
        new DisplayInfoWrapper(new ContentHelper(translatorManager, wrapStrategyRetriever));
    timedMessagePopupWrapper =
        new DisplayInfoWrapper(
            new ContentHelper(translatorManager, new WordWrapStrategy(numTextCells)));
    timedMessageAnnouncementWrapper =
        new DisplayInfoWrapper(
            new ContentHelper(translatorManager, new WordWrapStrategy(numTextCells)));
    timedMessageCaptionWrapper =
        new DisplayInfoWrapper(
            new ContentHelper(translatorManager, new WordWrapStrategy(numTextCells)));
    translatorManager.addOnOutputTablesChangedListener(outputCodeChangedListener);
    BrailleUserPreferences.getSharedPreferences(context, BRAILLE_SHARED_PREFS_FILENAME)
        .registerOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
  }

  public void shutdown() {
    cellContentUpdater.cancelAll();
    translatorManager.removeOnOutputTablesChangedListener(outputCodeChangedListener);
    BrailleUserPreferences.getSharedPreferences(context, BRAILLE_SHARED_PREFS_FILENAME)
        .unregisterOnSharedPreferenceChangeListener(onSharedPreferenceChangeListener);
    onDisplayContentChangeListeners.clear();
  }

  /**
   * Updates the display to reflect {@code content}. The {@code content} must not be modified after
   * this function is called.
   */
  @Override
  public void setContent(CellsContent content, Reason reason) {
    Preconditions.checkNotNull(content, "content can't be null");
    Preconditions.checkNotNull(content.getText(), "content text is null");
    commonDisplayInfoWrapper.renewDisplayInfo(
        content.getText(), content.getPanStrategy(), content.isSplitParagraphs());
    BrailleDisplayAnalytics.getInstance(context)
        .logReadingBrailleCharacter(
            commonDisplayInfoWrapper.getDisplayInfo().displayedBraille().array().length);
    if (reason == Reason.NAVIGATE_TO_NEW_NODE && panUpOverflow) {
      refreshToTail();
    } else {
      refresh();
    }
    cellOnDisplayContentChanged();
    panUpOverflow = false;
  }

  /** This is for BrailleIme to display the content. */
  public void setContent(
      List<Range<Integer>> onScreenRange,
      Range<Integer> holdingsRange,
      Range<Integer> actionRange,
      SelectionRange selection,
      TranslationResult overlayTranslationResult,
      boolean isMultiLine,
      boolean retranslate) {
    this.onScreenRange = onScreenRange;
    this.holdingsRange = holdingsRange;
    this.actionRange = actionRange;
    int beginningOfInput =
        onScreenRange.isEmpty()
            ? (holdingsRange.getLower() == NO_CURSOR
                ? min(selection.start, selection.end)
                : holdingsRange.getLower())
            : onScreenRange.get(0).getLower();
    int holdingsPosition =
        holdingsRange.getLower() == NO_CURSOR
            ? max(selection.start, selection.end)
            : holdingsRange.getUpper();
    int textCursorPosition =
        onScreenRange.isEmpty()
            ? max(selection.start, selection.end)
            : Iterables.getLast(onScreenRange).getUpper();
    commonDisplayInfoWrapper.renewDisplayInfo(
        retranslate ? CellsContent.PAN_KEEP : CellsContent.PAN_CURSOR,
        selection,
        beginningOfInput,
        max(holdingsPosition, textCursorPosition),
        isMultiLine,
        overlayTranslationResult,
        Source.IME);
    panUpOverflow = false;
    refresh();
    cellOnDisplayContentChanged();
  }

  @Override
  public void setTimedContent(
      TimedMessager.Type type, CellsContent content, int durationInMilliseconds) {
    Preconditions.checkNotNull(content, "content can't be null");
    Preconditions.checkNotNull(content.getText(), "content text is null");
    // Replace end indicator with new coming caption.
    if (isStartOrEndIndicatorShowing()) {
      timedMessagePopupWrapper.clear();
    }
    timedMessager.setTimedMessage(type, content, durationInMilliseconds);
    if (type == TimedMessager.Type.ANNOUNCEMENT) {
      BrailleDisplayAnalytics.getInstance(context).logPopupUsage();
    } else if (type == TimedMessager.Type.CAPTION) {
      BrailleDisplayAnalytics.getInstance(context).logCaptionUsage();
    }
  }

  @Override
  public AccessibilityNodeInfoCompat getAccessibilityNode(int byteIndex) {
    return getCurrentDisplayInfoWrapper().getContentHelper().getAccessibilityNodeInfo(byteIndex);
  }

  @Override
  public int getTextIndexInWhole(int routingKeyIndex) {
    return getCurrentDisplayInfoWrapper().getContentHelper().getTextCursorPosition(routingKeyIndex);
  }

  @Override
  public Optional<ClickableSpan[]> getClickableSpans(int routingKeyIndex) {
    return getCurrentDisplayInfoWrapper().getContentHelper().getClickableSpans(routingKeyIndex);
  }

  /** Dismisses current timed message and shows next type of timed message if possible. */
  @Override
  public void dismissTimedMessage() {
    TimedMessager.Type type = getCurrentTimedMessageType();
    if (type != null) {
      timedMessager.clearAllTimedMessage(type);
    }
  }

  /** Checks whether split point exists. */
  public boolean hasSplitPoints() {
    return editingWrapStrategy.hasSplitPoints();
  }

  /** Returns the length of current show content. */
  public int getCurrentShowContentLength() {
    return getCurrentDisplayInfoWrapper().getDisplayInfo().displayedBraille().array().length;
  }

  /** Checks whether the timed message is showing. */
  @Override
  public boolean isTimedMessageDisplaying() {
    if (isPopupShowing() || isAnnouncementShowing() || isCaptionShowing()) {
      BrailleDisplayLog.v(TAG, "timedMessage is displaying");
      return true;
    }
    return false;
  }

  @Override
  public boolean isCaptionShowing() {
    return getCurrentDisplayInfoWrapper() == timedMessageCaptionWrapper;
  }

  @Override
  public boolean showLatestCaption() {
    boolean result = timedMessager.showLatestCaption();
    if (result || isCaptionShowing()) {
      refreshToTail();
      result = true;
    }
    return result;
  }

  /** Maps the click index on the braille display to its index in the whole content. */
  public Cursor map(int positionOnBrailleDisplay) throws ExecutionException {
    ContentHelper contentHelper = getCurrentDisplayInfoWrapper().getContentHelper();
    int bytePositionInWhole = contentHelper.toWholeContentIndex(positionOnBrailleDisplay);
    if (bytePositionInWhole == NO_CURSOR) {
      throw new ExecutionException("Can't move cursor to " + positionOnBrailleDisplay, null);
    } else if (actionRange.contains(bytePositionInWhole)) {
      return Cursor.create(NO_CURSOR, Cursor.Type.ACTION);
    } else if (holdingsRange.contains(bytePositionInWhole)) {
      return Cursor.create(bytePositionInWhole - holdingsRange.getLower(), Cursor.Type.HOLDINGS);
    } else if (holdingsRange.getLower() != NO_CURSOR) {
      return Cursor.create(
          bytePositionInWhole > holdingsRange.getLower()
              ? holdingsRange.getUpper() - holdingsRange.getLower() + 1
              : NO_CURSOR,
          Cursor.Type.HOLDINGS);
    }
    int indexOnTextFieldText = NO_CURSOR;
    for (Range<Integer> range : onScreenRange) {
      if (range.contains(bytePositionInWhole)) {
        indexOnTextFieldText +=
            contentHelper.transferByteIndexToTextIndex(bytePositionInWhole)
                - contentHelper.transferByteIndexToTextIndex(range.getLower())
                + 1;
        return Cursor.create(indexOnTextFieldText, Cursor.Type.TEXT_FIELD);
      }
      if (bytePositionInWhole > range.getUpper()) {
        int upperTextIndex = contentHelper.transferByteIndexToTextIndex(range.getUpper());
        int lowerTextIndex = contentHelper.transferByteIndexToTextIndex(range.getLower());
        indexOnTextFieldText += upperTextIndex - lowerTextIndex + 1;
      }
    }
    // The routing button user clicks on cannot move any cursor.
    throw new ExecutionException("Can't move cursor to " + positionOnBrailleDisplay, null);
  }

  /**
   * Returns {@code true} if panning the displayed info up successfully, {@code false} if reach to
   * the end.
   */
  @CanIgnoreReturnValue
  public boolean panUp() {
    DisplayInfoWrapper displayInfoWrapper = getCurrentDisplayInfoWrapper();
    if (displayInfoWrapper.panUp()) {
      refresh();
      cellOnDisplayContentChanged();
    } else {
      TimedMessager.Type type = getCurrentTimedMessageType();
      if (type != null) {
        timedMessager.onReachedBeginningOrEnd(type);
      } else {
        panUpOverflow = true;
        return false;
      }
    }
    return true;
  }

  /**
   * Returns {@code true} if panning the displayed info down successfully, {@code false} if reach to
   * the end.
   */
  @CanIgnoreReturnValue
  public boolean panDown() {
    DisplayInfoWrapper displayInfoWrapper = getCurrentDisplayInfoWrapper();
    if (displayInfoWrapper.panDown()) {
      refresh();
      cellOnDisplayContentChanged();
    } else {
      TimedMessager.Type type = getCurrentTimedMessageType();
      if (type != null) {
        timedMessager.onReachedBeginningOrEnd(type);
      } else {
        return false;
      }
    }
    return true;
  }

  private void refreshToTail() {
    DisplayInfoWrapper displayInfoWrapper = getCurrentDisplayInfoWrapper();
    while (displayInfoWrapper.panDown()) {
      // Empty.
    }
    refresh();
  }

  private void refresh() {
    DisplayInfo displayInfoTarget = getCurrentDisplayInfoWrapper().getDisplayInfo();
    if (displayInfoTarget == null) {
      BrailleDisplayLog.v(TAG, "no displayInfoTarget");
      getCurrentDisplayInfoWrapper().clear();
      return;
    }
    cellContentUpdater.cancelAll();
    pulseUpdate.setOverlay(
        Arrays.asList(
            new BrailleWord(displayInfoTarget.displayedBraille().array()),
            new BrailleWord(displayInfoTarget.displayedOverlaidBraille().array())));
    if (displayInfoTarget.blink()) {
      cellContentUpdater.schedule(
          WHAT_PULSE, pulser, BrailleUserPreferences.readBlinkingIntervalMs(context));
    }
    refreshSelf();
  }

  private void refreshSelf() {
    DisplayInfo displayInfoTarget = getCurrentDisplayInfoWrapper().getDisplayInfo();
    if (displayInfoTarget == null) {
      getCurrentDisplayInfoWrapper().clear();
      return;
    }
    byte[] toDisplay = pulseUpdate.getOverlay().toByteArray();
    inputEventListener.displayDots(
        toDisplay,
        displayInfoTarget.displayedText(),
        displayInfoTarget.displayedBrailleToTextPositions().stream()
            .mapToInt(Integer::intValue)
            .toArray());
  }

  @Nullable
  private TimedMessager.Type getCurrentTimedMessageType() {
    TimedMessager.Type type = null;
    if (isPopupShowing()) {
      type = TimedMessager.Type.POPUP;
    } else if (isAnnouncementShowing()) {
      type = TimedMessager.Type.ANNOUNCEMENT;
    } else if (isCaptionShowing()) {
      type = TimedMessager.Type.CAPTION;
    }
    return type;
  }

  private DisplayInfoWrapper getCurrentDisplayInfoWrapper() {
    // Timed message has higher priority than common message.
    if (timedMessagePopupWrapper.hasDisplayInfo()) {
      BrailleDisplayLog.v(TAG, "popup wrapper");
      return timedMessagePopupWrapper;
    } else if (timedMessageAnnouncementWrapper.hasDisplayInfo()) {
      BrailleDisplayLog.v(TAG, "announcement wrapper");
      return timedMessageAnnouncementWrapper;
    } else if (timedMessageCaptionWrapper.hasDisplayInfo()) {
      BrailleDisplayLog.v(TAG, "caption wrapper");
      return timedMessageCaptionWrapper;
    }
    BrailleDisplayLog.v(TAG, "common wrapper");
    return commonDisplayInfoWrapper;
  }

  private boolean isPopupShowing() {
    return getCurrentDisplayInfoWrapper() == timedMessagePopupWrapper;
  }

  private boolean isAnnouncementShowing() {
    return getCurrentDisplayInfoWrapper() == timedMessageAnnouncementWrapper;
  }

  private boolean isStartOrEndIndicatorShowing() {
    return isPopupShowing()
        && timedMessagePopupWrapper
            .getDisplayInfo()
            .displayedText()
            .toString()
            .equals(INDICATOR_START_OR_END);
  }

  private final Runnable pulser =
      () -> {
        pulseUpdate.update();
        refreshSelf();
      };

  private final WrapStrategyRetriever wrapStrategyRetriever =
      new WrapStrategyRetriever() {
        @Override
        public WrapStrategy getWrapStrategy() {
          return imeStatusProvider.isImeOpen() ? editingWrapStrategy : preferredWrapStrategy;
        }
      };

  private final OutputCodeChangedListener outputCodeChangedListener =
      new OutputCodeChangedListener() {
        @Override
        public void onOutputCodeChanged() {
          commonDisplayInfoWrapper.retranslate();
          timedMessagePopupWrapper.retranslate();
          timedMessageAnnouncementWrapper.retranslate();
          timedMessageCaptionWrapper.retranslate();
          refresh();
          cellOnDisplayContentChanged();
        }
      };

  private final OnSharedPreferenceChangeListener onSharedPreferenceChangeListener =
      new OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (context.getString(R.string.pref_bd_blinking_interval_key).equals(key)) {
            cellContentUpdater.schedule(
                WHAT_PULSE, pulser, BrailleUserPreferences.readBlinkingIntervalMs(context));
          } else if (context.getString(R.string.pref_bd_caption_key).equals(key)) {
            if (!BrailleUserPreferences.readCaptionEnabled(context)) {
              timedMessager.clearAllTimedMessage(TimedMessager.Type.CAPTION);
            }
          } else if (context.getString(R.string.pref_bd_popup_announcement_key).equals(key)) {
            if (!BrailleUserPreferences.readPopupMessageEnabled(context)) {
              timedMessager.clearAllTimedMessage(TimedMessager.Type.POPUP);
            }
          }
        }
      };
  private final TimedMessager.Callback timedMessagerCallback =
      new TimedMessager.Callback() {
        @Override
        public void onTimedMessageDisplayed(TimedMessager.Type type, CellsContent content) {
          BrailleDisplayLog.v(TAG, "onTimedMessageDisplayed: " + type);
          DisplayInfoWrapper wrapper = null;
          boolean refresh = false;
          switch (type) {
            case POPUP -> {
              wrapper = timedMessagePopupWrapper;
              refresh = true;
            }
            case ANNOUNCEMENT -> {
              wrapper = timedMessageAnnouncementWrapper;
              refresh = !isPopupShowing();
            }
            case CAPTION -> {
              wrapper = timedMessageCaptionWrapper;
              refresh = !isPopupShowing() && !isAnnouncementShowing();
            }
          }
          BrailleDisplayLog.v(TAG, "refresh: " + refresh);
          if (wrapper != null) {
            wrapper.renewDisplayInfo(
                content.getText(), content.getPanStrategy(), content.isSplitParagraphs());
          }
          if (refresh) {
            refresh();
            cellOnDisplayContentChanged();
          }
        }

        @Override
        public void onTimedMessageCleared(TimedMessager.Type type) {
          BrailleDisplayLog.v(TAG, "onTimedMessageCleared: " + type);
          DisplayInfoWrapper wrapper =
              switch (type) {
                case POPUP -> timedMessagePopupWrapper;
                case ANNOUNCEMENT -> timedMessageAnnouncementWrapper;
                case CAPTION -> timedMessageCaptionWrapper;
              };
          if (wrapper != null) {
            wrapper.clear();
            refresh();
            cellOnDisplayContentChanged();
          }
        }
      };

  /** Interface definition for a callback to be invoked when the display content is changed. */
  interface OnDisplayContentChangeListener {
    void onDisplayContentChanged();
  }

  /** Adds a listener to be called when display content have changed. */
  public void addOnDisplayContentChangeListener(OnDisplayContentChangeListener listener) {
    onDisplayContentChangeListeners.add(listener);
  }

  /** Removes a display content change listener. */
  public void removeOnDisplayContentChangeListener(OnDisplayContentChangeListener listener) {
    onDisplayContentChangeListeners.remove(listener);
  }

  private void cellOnDisplayContentChanged() {
    for (OnDisplayContentChangeListener listener : onDisplayContentChangeListeners) {
      listener.onDisplayContentChanged();
    }
  }

  @VisibleForTesting
  WrapStrategyRetriever testing_getWrapStrategyRetriever() {
    return wrapStrategyRetriever;
  }

  @SuppressWarnings("VisibleForTestingUsed")
  @VisibleForTesting
  void testing_setContentHelper(ContentHelper contentHelper) {
    commonDisplayInfoWrapper.testing_setContentHelper(contentHelper);
  }
}
