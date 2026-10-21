/*
 * Copyright (C) 2024 The Android Open Source Project
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

package com.google.android.accessibility.talkback.actor.gemini.ui;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.google.android.accessibility.talkback.Feedback.Focus.Action.MUTE_NEXT_FOCUS;
import static com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.TYPE_MODEL_MESSAGE;
import static com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.TYPE_USER_MESSAGE;
import static com.google.android.accessibility.talkback.utils.ClipboardUtils.copyToClipboard;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowMetrics;
import android.view.accessibility.AccessibilityEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Feedback.ShowToast;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.TalkBackService;
import com.google.android.accessibility.talkback.actor.DimScreenActor;
import com.google.android.accessibility.talkback.actor.gemini.GeminiActor.GeminiChatMetricProcessor;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.actor.gemini.ui.ImageQnaChatAdapter.ImageQnaMessage;
import com.google.android.accessibility.talkback.imagecaption.Result;
import com.google.android.accessibility.talkback.utils.SpeechRecognizerPerformer;
import com.google.android.accessibility.talkback.utils.SpeechRecognizerPerformer.SpeechRecognizerRequester;
import com.google.android.accessibility.utils.FeatureSupport;
import com.google.android.accessibility.utils.SharedPreferencesUtils;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import java.util.ArrayList;
import java.util.List;

/** A class that owns the implementation of the image captioning result in a bottom sheet dialog. */
public class ImageCaptionResultBottomSheet {
  private static final String TAG = "ImageCaptionResultBottomSheet";

  private final TalkBackService service;
  private final Context context;
  private final SpeechRecognizerPerformer speechRecognizerPerformer;

  private Pipeline.FeedbackReturner pipeline;
  private ImageCaptionBottomSheetPagerAdapter currentAdapter;
  private byte[] imageBytesForQna;
  private GeminiChatMetricProcessor chatMetrics;

  ImageCaptionResultBottomSheet(
      TalkBackService service,
      SpeechRecognizerPerformer speechRecognizerPerformer,
      GeminiChatMetricProcessor chatMetrics) {
    this.service = service;
    context = service.getBaseContext();
    this.speechRecognizerPerformer = speechRecognizerPerformer;
    this.chatMetrics = chatMetrics;
  }

  public void setPipeline(Pipeline.FeedbackReturner pipeline) {
    this.pipeline = pipeline;
  }

  /**
   * Creates a bottom sheet dialog for the image captioning result.
   *
   * @param controller contains the business logic and prompting logic to connect with the chat UI
   * @param imageBytes The image bytes to be used for image Q&A.
   * @param imageDescriptionResult The result of image description.
   * @param iconLabelResult The result of icon label.
   * @param ocrTextResult The result of OCR text.
   * @param isScreenDescription If the dialog is for screen description.
   */
  public BottomSheetDialog getBottomSheetDialog(
      QnAChatController controller,
      byte[] imageBytes,
      Result imageDescriptionResult,
      Result iconLabelResult,
      Result ocrTextResult,
      boolean isScreenDescription) {
    boolean isDimScreenEnabled =
        DimScreenActor.isDimScreenEnabled(
            context, SharedPreferencesUtils.getSharedPreferences(context));
    int themeId =
        isDimScreenEnabled ? R.style.BlackBottomSheetTheme : R.style.ModalBottomSheetTheme;

    imageBytesForQna = imageBytes;
    BottomSheetDialog dialog = new BottomSheetDialog(context, themeId);
    LayoutInflater layoutInflater = LayoutInflater.from(new ContextThemeWrapper(context, themeId));
    LinearLayout dummyParent = new LinearLayout(context);
    RelativeLayout contentView =
        (RelativeLayout)
            layoutInflater.inflate(R.layout.image_caption_bottomsheet_dialog, dummyParent, false);

    // View pager
    ViewPager2 viewPager = contentView.findViewById(R.id.image_caption_view_pager);
    currentAdapter =
        new ImageCaptionBottomSheetPagerAdapter(
            dialog,
            dialog.getLifecycle(),
            context,
            controller,
            viewPager,
            pipeline,
            speechRecognizerPerformer,
            imageDescriptionResult,
            iconLabelResult,
            ocrTextResult,
            imageBytesForQna,
            isScreenDescription,
            isDimScreenEnabled);
    viewPager.setAdapter(currentAdapter);
    viewPager.setUserInputEnabled(false);

    // Ignore the feedback of ViewPager scrolled.
    for (int i = 0; i < viewPager.getChildCount(); ++i) {
      // ViewPager2 handles the accessibility scroll behavior within a RecyclerView.
      View child = viewPager.getChildAt(i);
      if (child instanceof RecyclerView) {
        ViewCompat.setAccessibilityDelegate(
            child,
            new AccessibilityDelegateCompat() {
              @Override
              public void onInitializeAccessibilityEvent(View host, AccessibilityEvent event) {
                super.onInitializeAccessibilityEvent(host, event);
                // Ignore the page index information for scrolling.
                if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
                  event.setFromIndex(0);
                  event.setItemCount(0);
                }
              }
            });
        break;
      }
    }

    BottomSheetBehavior<FrameLayout> behavior = dialog.getBehavior();
    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    behavior.setDraggable(false);
    behavior.setSkipCollapsed(true);

    // Set the max height for the bottom sheet.
    if (FeatureSupport.supportWindowMetrics()) {
      WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
      WindowMetrics windowMetrics = wm.getCurrentWindowMetrics();
      behavior.setMaxHeight(windowMetrics.getBounds().height() * 2 / 3);
      viewPager.setMinimumHeight(windowMetrics.getBounds().height() / 2);
    }

    MaterialButton closeButton = contentView.findViewById(R.id.close_button);
    closeButton.setOnClickListener(view -> dialog.dismiss());

    // For Hide screen feature
    if (isDimScreenEnabled) {
      contentView.setBackgroundColor(Color.BLACK);
      viewPager.setBackgroundColor(Color.BLACK);
      closeButton.setTextColor(Color.BLACK);
    }

    dialog.setContentView(contentView);
    // Customize title for screen description.
    if (isScreenDescription) {
      dialog.setTitle(R.string.title_gemini_screen_overview_result_dialog);
    } else {
      dialog.setTitle(R.string.title_gemini_result_bottom_sheet);
    }

    service.registerDialog(dialog, /* hasEditText= */ false);
    dialog.setOnDismissListener(
        dialogInterface -> {
          UiState state = controller.getUiState().getValue();
          if (state instanceof UiState.Chatting chatting) {
            List<ImageQnaMessage> messages = chatting.getMessages();
            List<ImageQnaMessage> collectionList = new ArrayList<>();
            boolean voiceInput = false;
            int questionLength = 0;
            for (ImageQnaMessage message : messages) {
              if (message.getViewType() == TYPE_USER_MESSAGE) {
                voiceInput = message.isVoiceInput();
                questionLength = message.getText().length();
              } else if (message.getViewType() == TYPE_MODEL_MESSAGE) {
                collectionList.add(
                    new ImageQnaMessage(TYPE_MODEL_MESSAGE, message.getText())
                        .setVoiceInput(voiceInput)
                        .setThumbUp(message.isThumbUp())
                        .setThumbDown(message.isThumbDown())
                        .setQuestionLength(questionLength));
                questionLength = 0;
              }
            }
            if (!collectionList.isEmpty()) {
              chatMetrics.sendLog(isScreenDescription, collectionList);
            }
          }
          imageBytesForQna = null;
          currentAdapter = null;
          service.unregisterDialog(dialog);
        });

    return dialog;
  }

  public void onImageQnaResponse(String message) {
    if (currentAdapter == null) {
      LogUtils.d(TAG, "Can't find the adapter.");
      return;
    }

    currentAdapter.onImageQnaResponse(message);
  }

  /**
   * Adapter for the image captioning result bottom sheet.
   *
   * <p>This adapter manages four different page types within the bottom sheet:
   *
   * <ol>
   *   <li>Image captioning result
   *   <li>Image Q&A voice input listening
   *   <li>Image Q&A chat UI with voice input
   *   <li>Image Q&A chat UI with typing input
   * </ol>
   */
  private static class ImageCaptionBottomSheetPagerAdapter
      extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    static final int IMAGE_CAPTION_RESULT = 0;
    static final int ASK_GEMINI_LISTENING = 1;
    static final int IMAGE_QA_CHAT_VOICE_INPUT = 2;
    static final int IMAGE_QA_CHAT_TYPING = 3;

    @NonNull Context context;
    @NonNull final ViewPager2 viewPager;
    @NonNull final Pipeline.FeedbackReturner pipeline;
    @NonNull final SpeechRecognizerPerformer speechRecognizerPerformer;
    @Nullable final Result imageDescriptionResult;
    @Nullable final Result iconLabelResult;
    @Nullable final Result ocrTextResult;

    final ImageQnaChatAdapter imageQnaChatAdapter;

    final byte[] imageBytesForQna;

    final boolean isScreenDescription;

    final Handler mainHandler;
    final boolean isDimScreenEnabled;
    BottomSheetDialog mainDialog;
    ViewHolder viewHolderAttachedToWindow;

    // Skip voice input result when interrupting the voice input and switching to tying.
    boolean skipVoiceInputResult;

    final QnAChatController controller;

    ImageCaptionBottomSheetPagerAdapter(
        BottomSheetDialog mainDialog,
        Lifecycle lifecycle,
        @NonNull Context context,
        QnAChatController controller,
        @NonNull ViewPager2 viewPager,
        @NonNull FeedbackReturner pipeline,
        @NonNull SpeechRecognizerPerformer speechRecognizerPerformer,
        @Nullable Result imageDescriptionResult,
        @Nullable Result iconLabelResult,
        @Nullable Result ocrTextResult,
        byte[] imageBytes,
        boolean isScreenDescription,
        boolean isDimScreenEnabled) {
      this.mainDialog = mainDialog;
      this.context = context;
      this.viewPager = viewPager;
      this.controller = controller;
      this.pipeline = pipeline;
      this.speechRecognizerPerformer = speechRecognizerPerformer;
      this.imageDescriptionResult = imageDescriptionResult;
      this.iconLabelResult = iconLabelResult;
      this.ocrTextResult = ocrTextResult;
      this.isScreenDescription = isScreenDescription;
      this.isDimScreenEnabled = isDimScreenEnabled;
      imageBytesForQna = imageBytes;
      mainHandler = new Handler(context.getMainLooper());
      imageQnaChatAdapter =
          new ImageQnaChatAdapter(context, mainHandler, pipeline, isDimScreenEnabled);

      // Listen for changes to the messages and UiState from the controller.
      CoroutineHelper.INSTANCE.collect(
          lifecycle,
          controller.getUiState(),
          uiState -> {
            if (uiState instanceof UiState.Chatting chat) {
              imageQnaChatAdapter.setMessages(chat.getMessages(), chat.isLoading());

              notifyMessageRangeChanged(viewHolderAttachedToWindow);
            } else if (uiState instanceof UiState.FinishedAndExit) {
              mainDialog.dismiss();
            }
          });
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
      View contentView;
      switch (viewType) {
        case IMAGE_CAPTION_RESULT -> {
          contentView =
              inflater.inflate(R.layout.caption_result_bottomsheet_dialog, viewGroup, false);
          return new ImageCaptionResultViewHolder(contentView);
        }
        case ASK_GEMINI_LISTENING -> {
          contentView =
              inflater.inflate(R.layout.image_qna_voice_input_bottomsheet_dialog, viewGroup, false);
          return new ImageQnaVoiceInputViewHolder(contentView, speechRecognizerPerformer);
        }
        case IMAGE_QA_CHAT_VOICE_INPUT -> {
          contentView = inflater.inflate(R.layout.image_qna_chat_ui_voice_input, viewGroup, false);
          return new ImageQnAVoiceInputChatViewHolder(contentView);
        }
        case IMAGE_QA_CHAT_TYPING -> {
          contentView = inflater.inflate(R.layout.image_qna_chat_ui_typing, viewGroup, false);
          return new ImageQnATypingChatViewHolder(contentView);
        }
        default -> {}
      }

      contentView = inflater.inflate(R.layout.caption_result_bottomsheet_dialog, viewGroup, false);
      return new ImageCaptionResultViewHolder(contentView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
      if (viewHolder instanceof ImageCaptionResultViewHolder imageCaptionResultViewHolder) {
        // Page of the image captioning result.
        imageCaptionResultViewHolder.setIsScreenDescription(isScreenDescription);
        imageCaptionResultViewHolder.setImageDescriptionResult(imageDescriptionResult);
        imageCaptionResultViewHolder.setIconDetectionResult(iconLabelResult);
        imageCaptionResultViewHolder.setOcrResult(ocrTextResult);

        imageCaptionResultViewHolder.setCopyAction(
            (view, arguments) -> {
              ImageQnaMessage description = getImageQnaMessage();
              if (description != null && copyToClipboard(context, description.getText())) {
                pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.speech(
                        context.getString(R.string.template_text_copied, description.getText()),
                        SpeakOptions.create()
                            .setFlags(
                                FeedbackItem.FLAG_NO_HISTORY
                                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_AUDIO_PLAYBACK_ACTIVE
                                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_MICROPHONE_ACTIVE
                                    | FeedbackItem.FLAG_FORCE_FEEDBACK_EVEN_IF_SSB_ACTIVE)));
                return true;
              }
              return false;
            });
        imageCaptionResultViewHolder.setThumbUpAction(
            (view, arguments) -> {
              LogUtils.d(TAG, "User toggles the thumb up.");
              ImageQnaMessage description = getImageQnaMessage();
              if (description == null) {
                return false;
              }
              description.setThumbUp(!description.isThumbUp());
              if (description.isThumbUp()) {
                description.setThumbDown(false);
              }
              mainHandler.post(
                  () ->
                      imageCaptionResultViewHolder.fillThumbState(
                          description.isThumbUp(), description.isThumbDown()));
              return true;
            });
        imageCaptionResultViewHolder.setThumbDownAction(
            (view, arguments) -> {
              LogUtils.d(TAG, "User toggles the thumb down.");
              ImageQnaMessage description = getImageQnaMessage();
              if (description == null) {
                return false;
              }
              description.setThumbDown(!description.isThumbDown());
              if (description.isThumbDown()) {
                description.setThumbUp(false);
              }
              mainHandler.post(
                  () ->
                      imageCaptionResultViewHolder.fillThumbState(
                          description.isThumbUp(), description.isThumbDown()));
              return true;
            });

        imageCaptionResultViewHolder.setAskGeminiButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Ask Gemini button clicked - voice input.");
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
              viewPager.setCurrentItem(ASK_GEMINI_LISTENING, /* smoothScroll= */ false);
            });
        imageCaptionResultViewHolder.setAskGeminiTypingButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Ask Gemini button clicked - typing.");
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
              viewPager.setCurrentItem(IMAGE_QA_CHAT_TYPING, /* smoothScroll= */ false);
            });

        if (isDimScreenEnabled) {
          imageCaptionResultViewHolder.setDimScreenEnabled();
        }
      } else if (viewHolder instanceof ImageQnaVoiceInputViewHolder imageQnAVoiceInputViewHolder) {
        // The page of voice input listening.
        imageQnAVoiceInputViewHolder.setSpeechRecognizerListener(
            new SpeechRecognizerRequester() {
              @Override
              public boolean onResult(String command, boolean isPartialResult) {
                LogUtils.d(TAG, "Recognized text : %s, isPartialResult", command, isPartialResult);
                if (skipVoiceInputResult) {
                  LogUtils.d(TAG, "Skip voice input result.");
                  skipVoiceInputResult = false;
                  return true;
                }
                pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
                requestImageQna(command, /* isVoiceInput= */ true);
                viewPager.setCurrentItem(IMAGE_QA_CHAT_VOICE_INPUT, /* smoothScroll= */ false);
                return true;
              }

              @Override
              public void onFeedbackEvent(@FeedbackEvent int eventId) {
                LogUtils.d(TAG, "onFeedbackEvent eventId: %d", eventId);
                switch (eventId) {
                  case EVENT_MIC_PERMISSION_REQUESTED -> mainDialog.hide();
                  case EVENT_MIC_PERMISSION_NOT_GRANTED ->
                      pipeline.returnFeedback(
                          EVENT_ID_UNTRACKED,
                          Feedback.showToast(
                              ShowToast.Action.SHOW,
                              context.getString(R.string.voice_commands_no_mic_permissions),
                              true));
                  case EVENT_DIALOG_CONFIRM -> mainDialog.show();
                  default -> {}
                }
              }

              @Override
              public void onError(int error) {
                LogUtils.d(TAG, "Recognition failed with error code: %d", error);
                if (skipVoiceInputResult) {
                  LogUtils.d(TAG, "Skip voice input result.");
                  skipVoiceInputResult = false;
                  return;
                }
                pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));

                controller.onVoiceError(error);

                viewPager.setCurrentItem(IMAGE_QA_CHAT_VOICE_INPUT, false);
              }
            });
        speechRecognizerPerformer.doListening();
        skipVoiceInputResult = false;
        imageQnAVoiceInputViewHolder.setAskGeminiTypingButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Ask Gemini listening to typing.");
              skipVoiceInputResult = true;
              speechRecognizerPerformer.stopListening();
              viewPager.setCurrentItem(IMAGE_QA_CHAT_TYPING, /* smoothScroll= */ false);
            });
        if (isDimScreenEnabled) {
          imageQnAVoiceInputViewHolder.setDimScreenEnabled();
        }
      } else if (viewHolder
          instanceof ImageQnAVoiceInputChatViewHolder imageQnAVoiceInputChatViewHolder) {
        // The page of the screen Q&A UI with voice input.
        imageQnAVoiceInputChatViewHolder.setChatAdapter(context, imageQnaChatAdapter);
        imageQnAVoiceInputChatViewHolder.setAskGeminiButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Ask Gemini button from voice input page - voice input");
              startVoiceInput();
            });
        imageQnAVoiceInputChatViewHolder.setAskGeminiTypingButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Ask Gemini typing button from voice input page - typing.");
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
              viewPager.setCurrentItem(IMAGE_QA_CHAT_TYPING, /* smoothScroll= */ false);
            });

        if (isDimScreenEnabled) {
          imageQnAVoiceInputChatViewHolder.setDimScreenEnabled();
        }
      } else if (viewHolder instanceof ImageQnATypingChatViewHolder imageQnATypingChatViewHolder) {
        // The page of the screen Q&A UI with typing.
        imageQnATypingChatViewHolder.setChatAdapter(context, imageQnaChatAdapter);
        imageQnATypingChatViewHolder.setEditTextOnEditorActionListener(
            (textView, actionId, keyEvent) -> {
              if (actionId == EditorInfo.IME_ACTION_DONE) {
                pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
                imageQnATypingChatViewHolder.setEditingMode(/* enableEditing= */ false);
                String enteredText = textView.getText().toString();
                LogUtils.d(TAG, "Enter text : %s", enteredText);
                if (!TextUtils.isEmpty(enteredText)) {
                  requestImageQna(enteredText, /* isVoiceInput= */ false);
                  notifyMessageRangeChanged(imageQnATypingChatViewHolder);
                  return true;
                }
              }
              return false;
            });
        imageQnATypingChatViewHolder.setSendTextButtonOnClickListener(
            view -> {
              pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
              imageQnATypingChatViewHolder.setEditingMode(/* enableEditing= */ false);
              String question = imageQnATypingChatViewHolder.getEditText();
              LogUtils.d(TAG, "Send text : %s", question);
              if (!TextUtils.isEmpty(question)) {
                requestImageQna(question, /* isVoiceInput= */ false);
                notifyMessageRangeChanged(imageQnATypingChatViewHolder);
              }
            });
        imageQnATypingChatViewHolder.setAskGeminiTypingButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Ask Gemini button from typing page - typing.");
              imageQnATypingChatViewHolder.setEditingMode(/* enableEditing= */ true);
            });
        imageQnATypingChatViewHolder.setAskGeminiVoiceInputButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Ask Gemini button from typing page - voice input.");
              startVoiceInput();
            });
        imageQnATypingChatViewHolder.setSpeakQuestionButtonOnClickListener(
            view -> {
              LogUtils.d(TAG, "Speak question button from typing page - voice input.");
              startVoiceInput();
            });

        if (isDimScreenEnabled) {
          imageQnATypingChatViewHolder.setDimScreenEnabled();
        }
      }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull ViewHolder viewHolder) {
      viewHolderAttachedToWindow = viewHolder;
      // For long conversations, scroll the chatting UI to the latest user message position.
      if (viewHolder instanceof ImageQnAVoiceInputChatViewHolder imageQnAVoiceInputChatViewHolder) {
        if (imageQnaChatAdapter.getItemCount() > 2) {
          imageQnAVoiceInputChatViewHolder.scrollToPosition(imageQnaChatAdapter.getItemCount() - 1);
        }
      } else if (viewHolder instanceof ImageQnATypingChatViewHolder imageQnATypingChatViewHolder) {
        if (imageQnaChatAdapter.getItemCount() > 2) {
          imageQnATypingChatViewHolder.scrollToPosition(imageQnaChatAdapter.getItemCount() - 1);
        }

        imageQnATypingChatViewHolder.setEditingMode(/* enableEditing= */ true);
      }
    }

    @Override
    public int getItemCount() {
      return 4;
    }

    @Override
    public int getItemViewType(int position) {
      return switch (position) {
        case 1 -> ASK_GEMINI_LISTENING;
        case 2 -> IMAGE_QA_CHAT_VOICE_INPUT;
        case 3 -> IMAGE_QA_CHAT_TYPING;
        default -> IMAGE_CAPTION_RESULT;
      };
    }

    void requestImageQna(String question, boolean isVoiceInput) {
      controller.onNewCommand(question, imageBytesForQna, isVoiceInput);
    }

    void onImageQnaResponse(String message) {
      controller.onImageQnaResponse(message);
    }

    void notifyMessageRangeChanged(ViewHolder viewHolder) {
      mainHandler.post(
          () -> {
            var position = imageQnaChatAdapter.getItemCount() - 1;
            // Scroll the Q&A UI
            if (viewHolder
                instanceof ImageQnAVoiceInputChatViewHolder imageQnAVoiceInputChatViewHolder) {
              if (position > 0) {
                imageQnAVoiceInputChatViewHolder.scrollToPosition(position);
              }
            } else if (viewHolder
                instanceof ImageQnATypingChatViewHolder imageQnATypingChatViewHolder) {
              if (position > 0) {
                imageQnATypingChatViewHolder.scrollToPosition(position);
              }
            }
          });
    }

    void startVoiceInput() {
      pipeline.returnFeedback(EVENT_ID_UNTRACKED, Feedback.focus(MUTE_NEXT_FOCUS));
      viewPager.setCurrentItem(ASK_GEMINI_LISTENING, /* smoothScroll= */ false);
      skipVoiceInputResult = false;
      speechRecognizerPerformer.doListening();
    }

    @Nullable
    private ImageQnaMessage getImageQnaMessage() {
      UiState state = controller.getUiState().getValue();
      if (!(state instanceof UiState.Chatting chatting)) {
        return null;
      }
      List<ImageQnaMessage> messages = chatting.getMessages();
      if (messages.size() != 1) {
        LogUtils.w(TAG, "Contains more than 1 message on the first page.");
      }
      return messages.get(0);
    }
  }

  /** The view holder for the page of the image captioning result. */
  private static class ImageCaptionResultViewHolder extends RecyclerView.ViewHolder {
    final Context context;

    final TextView bottomSheetDialogTitle;
    final TextView imageDescriptionResultView;
    final TextView iconDetectionResultView;
    final TextView ocrResulView;
    @Nullable private AccessibilityViewCommand thumbsUpCommand;
    @Nullable private AccessibilityViewCommand thumbsDownCommand;
    final MaterialButton askGeminiButton;
    final MaterialButton askGeminiTypingButton;

    ImageCaptionResultViewHolder(@NonNull View itemView) {
      super(itemView);
      context = itemView.getContext();
      bottomSheetDialogTitle = itemView.findViewById(R.id.bottomsheet_dialog_title);
      imageDescriptionResultView = itemView.findViewById(R.id.image_description_result_bottomsheet);
      iconDetectionResultView = itemView.findViewById(R.id.icon_detection_result_bottomsheet);
      ocrResulView = itemView.findViewById(R.id.ocr_result_bottomsheet);
      askGeminiButton = itemView.findViewById(R.id.ask_gemini);
      askGeminiTypingButton = itemView.findViewById(R.id.ask_gemini_typing);
    }

    void setIsScreenDescription(boolean isScreenDescription) {
      // Customize title for screen description.
      if (isScreenDescription) {
        bottomSheetDialogTitle.setText(R.string.title_gemini_screen_overview_result_dialog);
      } else {
        bottomSheetDialogTitle.setText(R.string.title_gemini_result_bottom_sheet);
      }
    }

    void setImageDescriptionResult(@Nullable Result imageDescriptionResult) {
      if (imageDescriptionResult == null || TextUtils.isEmpty(imageDescriptionResult.text())) {
        imageDescriptionResultView.setVisibility(GONE);
        return;
      }

      imageDescriptionResultView.setVisibility(VISIBLE);
      // Remove the newline at the end of the result from Gemini, as it causes forward line
      // granularity to not work correctly.
      imageDescriptionResultView.setText(imageDescriptionResult.text().toString().trim());
    }

    void setIconDetectionResult(@Nullable Result iconLabelResult) {
      if (iconLabelResult == null || TextUtils.isEmpty(iconLabelResult.text())) {
        iconDetectionResultView.setVisibility(GONE);
        return;
      }

      iconDetectionResultView.setVisibility(VISIBLE);
      iconDetectionResultView.setText(
          context.getString(
              R.string.detailed_image_description_icon_result, iconLabelResult.text()));
    }

    void setOcrResult(@Nullable Result ocrTextResult) {
      if (ocrTextResult == null || TextUtils.isEmpty(ocrTextResult.text())) {
        ocrResulView.setVisibility(GONE);
        return;
      }

      ocrResulView.setVisibility(VISIBLE);
      ocrResulView.setText(
          context.getString(R.string.detailed_image_description_ocr_result, ocrTextResult.text()));
    }

    void setThumbUpAction(AccessibilityViewCommand command) {
      thumbsUpCommand = command;
      ViewCompat.addAccessibilityAction(
          imageDescriptionResultView, context.getString(R.string.action_text_like_button), command);
    }

    void setThumbDownAction(AccessibilityViewCommand command) {
      thumbsDownCommand = command;
      ViewCompat.addAccessibilityAction(
          imageDescriptionResultView,
          context.getString(R.string.action_text_dislike_button),
          command);
    }

    void fillThumbState(boolean thumbUp, boolean thumbDown) {
      LogUtils.d(TAG, "image caption thumb_up=%b, thumb_down=%b", thumbUp, thumbDown);
      if (thumbsUpCommand == null) {
        LogUtils.e(TAG, "No AccessibilityViewCommand for thumbs-up action");
      } else {
        int likeActionId =
            ViewCompat.addAccessibilityAction(
                imageDescriptionResultView,
                context.getString(R.string.action_text_like_button),
                thumbsUpCommand);
        int clearLikeActionId =
            ViewCompat.addAccessibilityAction(
                imageDescriptionResultView,
                context.getString(R.string.action_text_clear_like_button),
                thumbsUpCommand);
        ViewCompat.removeAccessibilityAction(
            imageDescriptionResultView, thumbUp ? likeActionId : clearLikeActionId);
      }

      if (thumbsDownCommand == null) {
        LogUtils.e(TAG, "No AccessibilityViewCommand for thumbs-down action");
      } else {
        int dislikeActionId =
            ViewCompat.addAccessibilityAction(
                imageDescriptionResultView,
                context.getString(R.string.action_text_dislike_button),
                thumbsDownCommand);
        int clearDislikeActionId =
            ViewCompat.addAccessibilityAction(
                imageDescriptionResultView,
                context.getString(R.string.action_text_clear_dislike_button),
                thumbsDownCommand);
        ViewCompat.removeAccessibilityAction(
            imageDescriptionResultView, thumbDown ? dislikeActionId : clearDislikeActionId);
      }
    }

    void setAskGeminiButtonOnClickListener(OnClickListener listener) {
      askGeminiButton.setOnClickListener(listener);
    }

    void setAskGeminiTypingButtonOnClickListener(OnClickListener listener) {
      askGeminiTypingButton.setOnClickListener(listener);
    }

    void setDimScreenEnabled() {
      setDimScreenButton(askGeminiButton);
      setDimScreenButton(askGeminiTypingButton);
    }

    void setCopyAction(AccessibilityViewCommand command) {
      ViewCompat.addAccessibilityAction(
          imageDescriptionResultView, context.getString(R.string.title_action_copy), command);
    }
  }

  /** The view holder for the image Q&A voice input listening page. */
  private static class ImageQnaVoiceInputViewHolder extends RecyclerView.ViewHolder {
    final SpeechRecognizerPerformer speechRecognizerPerformer;
    final MaterialButton askGeminiTypingButton;
    final ImageView geminiSpark;
    final ImageView stopListening;

    public ImageQnaVoiceInputViewHolder(
        @NonNull View itemView, SpeechRecognizerPerformer speechRecognizerPerformer) {
      super(itemView);
      this.speechRecognizerPerformer = speechRecognizerPerformer;
      askGeminiTypingButton = itemView.findViewById(R.id.btn_type_question);
      geminiSpark = itemView.findViewById(R.id.gemini_spark);
      stopListening = itemView.findViewById(R.id.stop_listening);
    }

    void setSpeechRecognizerListener(SpeechRecognizerRequester requester) {
      speechRecognizerPerformer.setListener(requester);
    }

    void setAskGeminiTypingButtonOnClickListener(OnClickListener listener) {
      askGeminiTypingButton.setOnClickListener(listener);
    }

    void setDimScreenEnabled() {
      geminiSpark.setImageTintList(ColorStateList.valueOf(Color.BLACK));
      stopListening.setImageTintList(ColorStateList.valueOf(Color.BLACK));
      setDimScreenButton(askGeminiTypingButton);
    }
  }

  /** The view holder for the image Q&A chat UI with voice input. */
  private static class ImageQnAVoiceInputChatViewHolder extends RecyclerView.ViewHolder {
    final RecyclerView chatRecyclerView;
    final MaterialButton askGeminiButton;
    final MaterialButton askGeminiTypingButton;

    public ImageQnAVoiceInputChatViewHolder(@NonNull View itemView) {
      super(itemView);
      chatRecyclerView = itemView.findViewById(R.id.image_qna_chat_ui);
      askGeminiButton = itemView.findViewById(R.id.ask_gemini);
      askGeminiTypingButton = itemView.findViewById(R.id.ask_gemini_typing);

      // Set the initial focus position.
      ViewCompat.setAccessibilityDelegate(
          askGeminiButton,
          new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
              super.onInitializeAccessibilityNodeInfo(host, info);
              info.setRequestInitialAccessibilityFocus(true);
            }
          });
    }

    void setChatAdapter(Context context, ImageQnaChatAdapter imageQnaChatAdapter) {
      chatRecyclerView.setLayoutManager(new LinearLayoutManager(context));
      chatRecyclerView.setAdapter(imageQnaChatAdapter);
    }

    void scrollToPosition(int position) {
      chatRecyclerView.scrollToPosition(position);
    }

    void setAskGeminiButtonOnClickListener(OnClickListener listener) {
      askGeminiButton.setOnClickListener(listener);
    }

    void setAskGeminiTypingButtonOnClickListener(OnClickListener listener) {
      askGeminiTypingButton.setOnClickListener(listener);
    }

    void setDimScreenEnabled() {
      setDimScreenButton(askGeminiButton);
      setDimScreenButton(askGeminiTypingButton);
    }
  }

  /** The view holder for the image Q&A chat UI with typing. */
  private static class ImageQnATypingChatViewHolder extends RecyclerView.ViewHolder {
    final Context context;
    final RecyclerView chatRecyclerView;
    final MaterialButton askGeminiTyping;
    final MaterialButton askGeminiVoiceInput;

    final ViewGroup editingBar;
    final ViewGroup buttonBar;
    final MaterialButton speakQuestion;
    final EditText editText;
    final MaterialButton sendText;

    public ImageQnATypingChatViewHolder(@NonNull View itemView) {
      super(itemView);
      context = itemView.getContext();
      chatRecyclerView = itemView.findViewById(R.id.image_qna_chat_ui);
      askGeminiTyping = itemView.findViewById(R.id.ask_gemini_typing);
      askGeminiVoiceInput = itemView.findViewById(R.id.ask_gemini_voice_input);
      editingBar = itemView.findViewById(R.id.edit_bar);
      buttonBar = itemView.findViewById(R.id.button_bar);
      speakQuestion = itemView.findViewById(R.id.speak_question);
      editText = itemView.findViewById(R.id.question_edit_filed);
      sendText = itemView.findViewById(R.id.send_button);

      // Specify the size limit of keyboard input
      InputFilter[] filters = new InputFilter[1];
      filters[0] =
          new InputFilter.LengthFilter(GeminiConfiguration.imageQnaTypingSizeLimit(context));
      editText.setFilters(filters);

      // Set the initial focus position.
      ViewCompat.setAccessibilityDelegate(
          askGeminiTyping,
          new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(
                @NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
              super.onInitializeAccessibilityNodeInfo(host, info);
              info.setRequestInitialAccessibilityFocus(true);
            }
          });
    }

    void setChatAdapter(Context context, ImageQnaChatAdapter imageQnaChatAdapter) {
      chatRecyclerView.setLayoutManager(new LinearLayoutManager(context));
      chatRecyclerView.setAdapter(imageQnaChatAdapter);
    }

    void scrollToPosition(int position) {
      chatRecyclerView.scrollToPosition(position);
    }

    void setAskGeminiTypingButtonOnClickListener(OnClickListener listener) {
      askGeminiTyping.setOnClickListener(listener);
    }

    void setAskGeminiVoiceInputButtonOnClickListener(OnClickListener listener) {
      askGeminiVoiceInput.setOnClickListener(listener);
    }

    void setSpeakQuestionButtonOnClickListener(OnClickListener listener) {
      speakQuestion.setOnClickListener(listener);
    }

    void setEditTextOnEditorActionListener(OnEditorActionListener listener) {
      editText.setOnEditorActionListener(listener);
    }

    void setSendTextButtonOnClickListener(OnClickListener listener) {
      sendText.setOnClickListener(listener);
    }

    String getEditText() {
      return editText.getText().toString();
    }

    void setEditingMode(boolean enableEditing) {
      InputMethodManager imm =
          (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);

      if (enableEditing) {
        editingBar.setVisibility(VISIBLE);
        buttonBar.setVisibility(GONE);
        editText.setText(""); // Clear the previous text.
        editText.requestFocus();
        imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT);
      } else {
        if (editText.isFocused()) {
          imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
        editingBar.setVisibility(GONE);
        buttonBar.setVisibility(VISIBLE);
      }
    }

    void setDimScreenEnabled() {
      editText.setTextColor(Color.BLACK);
      editText.setHintTextColor(Color.BLACK);
      editText.setBackgroundColor(Color.BLACK);
      setDimScreenButton(sendText);
      setDimScreenButton(askGeminiTyping);
      setDimScreenButton(askGeminiVoiceInput);
      setDimScreenButton(speakQuestion);
    }
  }

  private static void setDimScreenButton(MaterialButton button) {
    button.setTextColor(Color.BLACK);
    button.setIconTint(ColorStateList.valueOf(Color.BLACK));
    button.setStrokeColor(ColorStateList.valueOf(Color.BLACK));
  }
}
