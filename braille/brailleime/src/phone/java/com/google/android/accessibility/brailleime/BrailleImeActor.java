package com.google.android.accessibility.brailleime;

import static com.google.android.accessibility.braille.common.BrailleImeAction.ADD_NEWLINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.ADD_SPACE_OR_NEXT_ITEM;
import static com.google.android.accessibility.braille.common.BrailleImeAction.BEGINNING_OF_PAGE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.COPY;
import static com.google.android.accessibility.braille.common.BrailleImeAction.CUT;
import static com.google.android.accessibility.braille.common.BrailleImeAction.DELETE_CHARACTER_OR_PREVIOUS_ITEM;
import static com.google.android.accessibility.braille.common.BrailleImeAction.DELETE_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.END_OF_PAGE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.HELP_AND_OTHER_ACTIONS;
import static com.google.android.accessibility.braille.common.BrailleImeAction.HIDE_KEYBOARD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.MOVE_CURSOR_BACKWARD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.MOVE_CURSOR_FORWARD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_GRANULARITY;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.NEXT_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PASTE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_GRANULARITY;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.PREVIOUS_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_ALL;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_CURRENT_TO_END;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_CURRENT_TO_START;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_NEXT_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_NEXT_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_NEXT_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_PREVIOUS_CHARACTER;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_PREVIOUS_LINE;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SELECT_PREVIOUS_WORD;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SUBMIT_TEXT;
import static com.google.android.accessibility.braille.common.BrailleImeAction.SWITCH_KEYBOARD;

import android.content.Context;
import android.os.Handler;
import android.view.inputmethod.InputConnection;
import androidx.annotation.Nullable;
import com.google.android.accessibility.braille.common.BrailleCommonTalkBackSpeaker;
import com.google.android.accessibility.braille.common.BrailleImeAction;
import com.google.android.accessibility.braille.common.FeedbackManager;
import com.google.android.accessibility.braille.common.ImeConnection;
import com.google.android.accessibility.braille.common.translate.EditBuffer;
import com.google.android.accessibility.braille.common.translate.EditBufferUtils;
import com.google.android.accessibility.braille.interfaces.BrailleCharacter;
import com.google.android.accessibility.braille.interfaces.ScreenReaderActionPerformer.ScreenReaderAction;
import com.google.android.accessibility.braille.interfaces.TalkBackForBrailleIme;
import com.google.android.accessibility.utils.FocusFinder;
import com.google.common.annotations.VisibleForTesting;

/**
 * Handles user input and provides feedback within the Braille IME. This class utilizes {@link
 * SupportedCommand} to perform its functions.
 */
public class BrailleImeActor {
  private final Context context;
  private final TalkBackForBrailleIme talkBackForBrailleIme;
  private final TypoHandler typoHandler;
  private final FeedbackManager feedbackManager;
  private final Callback callback;
  private final Handler handler;

  /** Callback for actions which request InputMethodService. */
  public interface Callback {
    void hideBrailleKeyboard();

    void switchToNextInputMethod();

    void showContextMenu();

    void performEditorAction();

    boolean isConnectionValid();

    ImeConnection getImeConnection();

    EditBuffer getEditBuffer();
  }

  public BrailleImeActor(
      Context context,
      Callback callback,
      TalkBackForBrailleIme talkBackForBrailleIme,
      FeedbackManager feedbackManager) {
    this.context = context;
    // Do not recreate the TypoHandler every time keyboard is up because TalkBack performs typo
    // correction makes IME restart views but user won't aware. If we recreate, the data will all
    // lost. So making the TypoHandler keep as-it but only renew its InputConnection.
    this.typoHandler =
        new TypoHandler(
            context,
            new TypoHandler.Callback() {
              @Override
              public InputConnection getInputConnection() {
                return callback.getImeConnection().inputConnection;
              }

              @Override
              public FocusFinder getFocusFinder() {
                return talkBackForBrailleIme.createFocusFinder();
              }

              @Override
              public boolean performAction(ScreenReaderAction action, Object... arg) {
                return talkBackForBrailleIme.performAction(action, arg);
              }
            },
            BrailleCommonTalkBackSpeaker.getInstance());
    this.callback = callback;
    this.feedbackManager = feedbackManager;
    this.talkBackForBrailleIme = talkBackForBrailleIme;
    this.handler = new Handler();
  }

  @Nullable
  public String performTyping(BrailleCharacter brailleChar) {
    if (!callback.isConnectionValid()) {
      return null;
    }
    talkBackForBrailleIme.interruptSpeak();
    if (talkBackForBrailleIme.isCurrentGranularityTypoCorrection()) {
      talkBackForBrailleIme.resetGranularity();
    }
    return callback.getEditBuffer().appendBraille(callback.getImeConnection(), brailleChar);
  }

  public boolean performAction(BrailleImeAction action) {
    if (action == null || !callback.isConnectionValid()) {
      return false;
    }
    if (SupportedCommand.getSupportedCommands(context).stream()
        .noneMatch(supportedCommand -> supportedCommand.getBrailleImeAction().equals(action))) {
      return false;
    }
    talkBackForBrailleIme.interruptSpeak();
    ImeConnection imeConnection = callback.getImeConnection();
    boolean result = true;
    switch (action) {
      case HIDE_KEYBOARD -> performTextAction(imeConnection, callback::hideBrailleKeyboard);
      case SWITCH_KEYBOARD -> performTextAction(imeConnection, callback::switchToNextInputMethod);
      case HELP_AND_OTHER_ACTIONS -> performTextAction(imeConnection, callback::showContextMenu);
      case NEXT_GRANULARITY ->
          performTextAction(imeConnection, ScreenReaderAction.NEXT_READING_CONTROL);
      case PREVIOUS_GRANULARITY ->
          performTextAction(imeConnection, ScreenReaderAction.PREVIOUS_READING_CONTROL);
      case PASTE -> performTextAction(imeConnection, ScreenReaderAction.PASTE);
      case CUT -> performTextAction(imeConnection, ScreenReaderAction.CUT);
      case COPY -> performTextAction(imeConnection, ScreenReaderAction.COPY);
      case SUBMIT_TEXT ->
          performTextAction(
              imeConnection,
              () -> {
                callback
                    .hideBrailleKeyboard(); // Restore EBT so a11y focus could jump to next field.
                callback.performEditorAction();
              });
      case ADD_SPACE_OR_NEXT_ITEM -> {
        if (talkBackForBrailleIme.isCurrentGranularityTypoCorrection()) {
          if (!typoHandler.nextSuggestion()) {
            BrailleCommonTalkBackSpeaker.getInstance()
                .speak(
                    context.getString(
                        typoHandler.isGrammarMistake()
                            ? R.string.no_grammar_suggestion_available_announcement
                            : R.string.no_spelling_suggestion_available_announcement));
          }
        } else {
          callback.getEditBuffer().appendSpace(imeConnection);
        }
      }
      case ADD_NEWLINE -> {
        if (talkBackForBrailleIme.isCurrentGranularityTypoCorrection()) {
          result = typoHandler.confirmSuggestion();
          if (!result) {
            feedbackManager.emitFeedback(FeedbackManager.Type.NAVIGATE_OUT_OF_BOUNDS);
          }
        } else {
          callback.getEditBuffer().appendNewline(imeConnection);
        }
      }
      case DELETE_CHARACTER_OR_PREVIOUS_ITEM -> {
        if (talkBackForBrailleIme.isCurrentGranularityTypoCorrection()) {
          if (!typoHandler.previousSuggestion()) {
            BrailleCommonTalkBackSpeaker.getInstance()
                .speak(
                    context.getString(
                        typoHandler.isGrammarMistake()
                            ? R.string.no_grammar_suggestion_available_announcement
                            : R.string.no_spelling_suggestion_available_announcement));
          }
        } else {
          callback.getEditBuffer().deleteCharacterBackward(imeConnection);
        }
      }
      case DELETE_WORD -> {
        if (talkBackForBrailleIme.isCurrentGranularityTypoCorrection()) {
          result = typoHandler.undoConfirmSuggestion();
          if (!result) {
            feedbackManager.emitFeedback(FeedbackManager.Type.NAVIGATE_OUT_OF_BOUNDS);
          }
        } else {
          callback.getEditBuffer().deleteWord(imeConnection);
        }
      }
      case MOVE_CURSOR_FORWARD -> {
        if (talkBackForBrailleIme.shouldUseCharacterGranularity()) {
          if (!callback.getEditBuffer().moveCursorForward(imeConnection)) {
            performTextAction(imeConnection, ScreenReaderAction.FOCUS_NEXT_CHARACTER);
          }
        } else {
          performTextAction(
              imeConnection,
              ScreenReaderAction.NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_FORWARD);
          handler.post(
              () -> {
                if (talkBackForBrailleIme.isCurrentGranularityTypoCorrection()) {
                  typoHandler.updateTypoTarget();
                }
              });
        }
      }
      case MOVE_CURSOR_BACKWARD -> {
        if (talkBackForBrailleIme.shouldUseCharacterGranularity()) {
          if (!callback.getEditBuffer().moveCursorBackward(imeConnection)) {
            performTextAction(imeConnection, ScreenReaderAction.FOCUS_PREVIOUS_CHARACTER);
          }
        } else {
          performTextAction(
              imeConnection,
              ScreenReaderAction
                  .NAVIGATE_BY_READING_GRANULARITY_OR_ADJUST_READING_CONTROL_BACKWARD);
          handler.post(
              () -> {
                if (talkBackForBrailleIme.isCurrentGranularityTypoCorrection()) {
                  typoHandler.updateTypoTarget();
                }
              });
        }
      }
      case NEXT_CHARACTER -> {
        if (!callback.getEditBuffer().moveCursorForward(imeConnection)) {
          performTextAction(imeConnection, ScreenReaderAction.FOCUS_NEXT_CHARACTER);
        }
      }
      case PREVIOUS_CHARACTER -> {
        if (!callback.getEditBuffer().moveCursorBackward(imeConnection)) {
          performTextAction(imeConnection, ScreenReaderAction.FOCUS_PREVIOUS_CHARACTER);
        }
      }
      case NEXT_WORD -> performTextAction(imeConnection, ScreenReaderAction.FOCUS_NEXT_WORD);
      case PREVIOUS_WORD ->
          performTextAction(imeConnection, ScreenReaderAction.FOCUS_PREVIOUS_WORD);
      case NEXT_LINE -> {
        if (!EditBufferUtils.isMultiLineField(imeConnection.editorInfo.inputType)) {
          return false;
        }
        performTextAction(imeConnection, ScreenReaderAction.FOCUS_NEXT_LINE);
      }
      case PREVIOUS_LINE -> {
        if (!EditBufferUtils.isMultiLineField(imeConnection.editorInfo.inputType)) {
          return false;
        }
        performTextAction(imeConnection, ScreenReaderAction.FOCUS_PREVIOUS_LINE);
      }
      case BEGINNING_OF_PAGE ->
          performTextAction(imeConnection, ScreenReaderAction.CURSOR_TO_BEGINNING);
      case END_OF_PAGE -> performTextAction(imeConnection, ScreenReaderAction.CURSOR_TO_END);
      case SELECT_NEXT_CHARACTER ->
          performTextAction(imeConnection, ScreenReaderAction.SELECT_NEXT_CHARACTER);
      case SELECT_PREVIOUS_CHARACTER ->
          performTextAction(imeConnection, ScreenReaderAction.SELECT_PREVIOUS_CHARACTER);
      case SELECT_NEXT_WORD ->
          performTextAction(imeConnection, ScreenReaderAction.SELECT_NEXT_WORD);
      case SELECT_PREVIOUS_WORD ->
          performTextAction(imeConnection, ScreenReaderAction.SELECT_PREVIOUS_WORD);
      case SELECT_NEXT_LINE -> {
        // TODO: As the text selection for line granularity movement does not work, we
        // mask off the action of selecting text by line.
        // performTextAction(imeConnection, ScreenReaderAction.SELECT_NEXT_LINE);
        return false;
      }
      case SELECT_PREVIOUS_LINE -> {
        // TODO: As the text selection for line granularity movement does not work, we
        // mask off the action of selecting text by line.
        // performTextAction(imeConnection, ScreenReaderAction.SELECT_PREVIOUS_LINE);
        return false;
      }
      case SELECT_ALL -> performTextAction(imeConnection, ScreenReaderAction.SELECT_ALL);
      case SELECT_CURRENT_TO_START ->
          performTextAction(imeConnection, ScreenReaderAction.SELECT_CURRENT_TO_START);
      case SELECT_CURRENT_TO_END ->
          performTextAction(imeConnection, ScreenReaderAction.SELECT_CURRENT_TO_END);
      default -> {
        return false;
      }
    }
    return result;
  }

  private void performTextAction(
      ImeConnection imeConnection, ScreenReaderAction screenReaderAction) {
    performTextAction(imeConnection, () -> talkBackForBrailleIme.performAction(screenReaderAction));
  }

  private void performTextAction(ImeConnection imeConnection, Runnable runnable) {
    EditBuffer editBuffer = callback.getEditBuffer();
    if (editBuffer != null
        && editBuffer.getHoldingsInfo(imeConnection).holdings().array().length != 0) {
      editBuffer.commit(imeConnection);
      handler.post(runnable);
    } else {
      runnable.run();
    }
  }

  @VisibleForTesting
  public TypoHandler testing_getTypoHandler() {
    return typoHandler;
  }
}
