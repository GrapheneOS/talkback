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

import static android.view.View.VISIBLE;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK;
import static androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_LONG_CLICK;
import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Handler;
import androidx.recyclerview.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.constraintlayout.motion.widget.MotionLayout.TransitionListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat.AccessibilityActionCompat;
import androidx.core.view.accessibility.AccessibilityViewCommand;
import com.google.android.accessibility.gemineye.screenoverview.json.UiElement;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline.FeedbackReturner;
import com.google.android.accessibility.talkback.R;
import com.google.android.accessibility.talkback.actor.gemini.GeminiConfiguration;
import com.google.android.accessibility.talkback.utils.ClipboardUtils;
import com.google.android.accessibility.utils.Consumer;
import com.google.android.accessibility.utils.output.FeedbackItem;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.List;

/** The adapter for Image Q&A chatting messages. */
public class ImageQnaChatAdapter extends RecyclerView.Adapter<ImageQnaChatAdapter.ChatViewHolder> {
  private static final String TAG = "ImageQnaChatAdapter";

  /** The view type of messages from the user. */
  public static final int TYPE_USER_MESSAGE = 0;

  /** The view type of messages from the model. */
  public static final int TYPE_MODEL_MESSAGE = 1;

  // We don't export this because it's not part of the data model. It's only used in the ViewHolder
  private static final int TYPE_MODEL_MESSAGE_WITH_ACTIONS = 2;

  /** Image Q&A message type. */
  @IntDef({
    TYPE_USER_MESSAGE,
    TYPE_MODEL_MESSAGE,
  })
  @Retention(RetentionPolicy.SOURCE)
  @interface MessageType {}

  private static final ImageQnaMessage loadingMessage =
      new ImageQnaMessage(ImageQnaChatAdapter.TYPE_MODEL_MESSAGE);

  private final Context context;
  @NonNull private List<ImageQnaMessage> messages;
  private final Handler mainHandler;
  @NonNull private final FeedbackReturner pipeline;
  private final boolean isDimScreenEnabled;

  public ImageQnaChatAdapter(
      Context context, Handler mainHandler, FeedbackReturner pipeline, boolean isDimScreenEnabled) {
    this.context = context;
    this.mainHandler = mainHandler;
    this.pipeline = pipeline;
    this.messages = ImmutableList.of();
    this.isDimScreenEnabled = isDimScreenEnabled;
  }

  @NonNull
  @Override
  public ChatViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
    if (viewType == TYPE_USER_MESSAGE) {
      View view =
          LayoutInflater.from(viewGroup.getContext())
              .inflate(R.layout.image_qna_user_message, viewGroup, false);
      return new ChatViewHolder(view, viewType);
    }

    if (viewType == TYPE_MODEL_MESSAGE_WITH_ACTIONS) {
      View view =
          LayoutInflater.from(viewGroup.getContext())
              .inflate(R.layout.image_qna_model_message_with_actions, viewGroup, false);
      return new ChatViewHolderWithActions(view, viewType);
    }

    // For TYPE_MODEL_MESSAGE
    View view =
        LayoutInflater.from(viewGroup.getContext())
            .inflate(R.layout.image_qna_model_message, viewGroup, false);
    return new ChatViewHolder(view, viewType);
  }

  @Override
  public void onBindViewHolder(@NonNull ChatViewHolder chatViewHolder, int position) {
    ImageQnaMessage message = messages.get(position);
    chatViewHolder.setMessageText(message);
    chatViewHolder.setGeminiMotion(message);
    if (message.getViewType() == TYPE_MODEL_MESSAGE) {
      // Setup feedback buttons for thumb up/down.
      if (!TextUtils.isEmpty(message.text)) {
        chatViewHolder.setCopyAction(
            (view, arguments) -> {
              if (ClipboardUtils.copyToClipboard(context, message.text)) {
                pipeline.returnFeedback(
                    EVENT_ID_UNTRACKED,
                    Feedback.speech(
                        context.getString(R.string.template_text_copied, message.text),
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
        chatViewHolder.setThumbUpAction(
            (view, arguments) -> {
              LogUtils.d(TAG, "User toggles the thumb up.");
              message.thumbUp = !message.thumbUp;
              if (message.thumbUp) {
                message.thumbDown = false;
              }
              chatViewHolder.fillThumbState(message.thumbUp, message.thumbDown);
              onUserFeedBack(position);
              return true;
            });
        chatViewHolder.setThumbDownAction(
            (view, arguments) -> {
              LogUtils.d(TAG, "User toggles the thumb down.");
              message.thumbDown = !message.thumbDown;
              if (message.thumbDown) {
                message.thumbUp = false;
              }

              chatViewHolder.fillThumbState(message.thumbUp, message.thumbDown);
              onUserFeedBack(position);
              return true;
            });
      }
      chatViewHolder.fillThumbState(message.thumbUp, message.thumbDown);
    }
    if (isDimScreenEnabled) {
      chatViewHolder.setDimScreenEnabled();
    }

    if (chatViewHolder instanceof ChatViewHolderWithActions withActions) {
      withActions.setUiElements(messages.get(position).getUiElements());
      withActions.setOnUiElementActionListener(
          messages.get(position).getOnUiElementActionListener());
    }
  }

  @Override
  public int getItemViewType(int position) {
    var message = messages.get(position);
    if (message.getUiElements() != null && GeminiConfiguration.isScreenQnaActionsEnabled(context)) {
      return TYPE_MODEL_MESSAGE_WITH_ACTIONS;
    }

    return message.viewType;
  }

  @Override
  public int getItemCount() {
    return messages.size();
  }

  void onUserFeedBack(int position) {
    mainHandler.post(() -> notifyItemChanged(position));
  }

  /** The view holder for Image Q&A chatting messages. */
  public static class ChatViewHolder extends RecyclerView.ViewHolder {
    private final TextView messageText;
    @Nullable private MotionLayout motionLayout;
    @Nullable private AccessibilityViewCommand thumbsUpCommand;
    @Nullable private AccessibilityViewCommand thumbsDownCommand;
    @MessageType int viewType;
    @Nullable private ImageView geminiIcon;
    private final LoopAnimationTransitionListener loopAnimationTransitionListener;

    public ChatViewHolder(View itemView, @MessageType int viewType) {
      super(itemView);
      this.viewType = viewType;
      if (viewType == TYPE_USER_MESSAGE) {
        messageText = itemView.findViewById(R.id.user_message);
      } else {
        messageText = itemView.findViewById(R.id.model_message);
        motionLayout = itemView.findViewById(R.id.generating_motionLayout);
        geminiIcon = itemView.findViewById(R.id.gemini_spark);
      }
      loopAnimationTransitionListener = new LoopAnimationTransitionListener();
    }

    public @MessageType int getViewType() {
      return viewType;
    }

    void setMessageText(ImageQnaMessage message) {
      if (TextUtils.isEmpty(message.text)) {
        messageText.setText("");
        if (message.getViewType() == TYPE_MODEL_MESSAGE) {
          messageText.setVisibility(View.GONE);
        }
        return;
      }

      String text = message.text;
      List<String> topImages = message.getTopImages();
      if (topImages != null && !topImages.isEmpty()) {
        text += "\n" + itemView.getContext().getString(R.string.screen_overview_top_images_header);
        for (int i = 0; i < topImages.size(); i++) {
          String image = topImages.get(i);
          text +=
              "\n" + itemView.getContext().getString(R.string.overviews_list_item, (i + 1), image);
        }
      }

      // Remove the newline at the end of the result from Gemini, as it causes forward line
      // granularity to not work correctly.
      messageText.setText(TextUtils.isEmpty(text) ? "" : text.trim());
      messageText.setVisibility(VISIBLE);
    }

    void setGeminiMotion(ImageQnaMessage message) {
      // Only for the model message(Gemini loading motion).
      if (message.viewType == TYPE_MODEL_MESSAGE && motionLayout != null) {
        if (TextUtils.isEmpty(message.text)) {
          motionLayout.setVisibility(View.VISIBLE);
          motionLayout.setTransitionListener(loopAnimationTransitionListener);
        } else {
          motionLayout.removeTransitionListener(loopAnimationTransitionListener);
          motionLayout.setVisibility(View.GONE);
        }
      }
    }

    void setCopyAction(AccessibilityViewCommand command) {
      ViewCompat.addAccessibilityAction(
          itemView, itemView.getContext().getString(R.string.title_action_copy), command);
    }

    void setThumbUpAction(AccessibilityViewCommand command) {
      thumbsUpCommand = command;
    }

    void setThumbDownAction(AccessibilityViewCommand command) {
      thumbsDownCommand = command;
    }

    void fillThumbState(boolean thumbUp, boolean thumbDown) {
      LogUtils.d(TAG, "image Q&A thumb_up=%b, thumb_down=%b", thumbUp, thumbDown);
      if (thumbsUpCommand == null) {
        LogUtils.e(TAG, "No AccessibilityViewCommand for thumbs-up action");
      } else {
        int likeActionId =
            ViewCompat.addAccessibilityAction(
                itemView,
                itemView.getContext().getString(R.string.action_text_like_button),
                thumbsUpCommand);
        int clearLikeActionId =
            ViewCompat.addAccessibilityAction(
                itemView,
                itemView.getContext().getString(R.string.action_text_clear_like_button),
                thumbsUpCommand);
        ViewCompat.removeAccessibilityAction(itemView, thumbUp ? likeActionId : clearLikeActionId);
      }

      if (thumbsDownCommand == null) {
        LogUtils.e(TAG, "No AccessibilityViewCommand for thumbs-down action");
      } else {
        int dislikeActionId =
            ViewCompat.addAccessibilityAction(
                itemView,
                itemView.getContext().getString(R.string.action_text_dislike_button),
                thumbsDownCommand);
        int clearDislikeActionId =
            ViewCompat.addAccessibilityAction(
                itemView,
                itemView.getContext().getString(R.string.action_text_clear_dislike_button),
                thumbsDownCommand);
        ViewCompat.removeAccessibilityAction(
            itemView, thumbDown ? dislikeActionId : clearDislikeActionId);
      }
    }

    void setDimScreenEnabled() {
      messageText.setBackgroundColor(Color.BLACK);
      if (motionLayout != null) {
        motionLayout.removeTransitionListener(loopAnimationTransitionListener);
        motionLayout.setVisibility(View.GONE);
      }

      if (geminiIcon != null) {
        geminiIcon.setImageTintList(ColorStateList.valueOf(Color.BLACK));
      }
    }
  }

  /**
   * Sets the messages shown in the adapter
   *
   * <p>It's assumed that messages are never removed or changed, only added.
   *
   * @param messages the new messages to show.
   * @param isLoading true to show a loading indicator, false otherwise
   */
  public void setMessages(@NonNull List<ImageQnaMessage> messages, boolean isLoading) {
    var newMessages = new ArrayList<>(messages);
    if (isLoading) {
      newMessages.add(loadingMessage);
    }

    this.messages = newMessages;

    // This could be more efficient, but it's not a very heavy list so we won't lose that much
    notifyDataSetChanged();
  }

  /** The data model for Image Q&A chatting messages. */
  public static class ImageQnaMessage {
    @Nullable private List<UiElement> uiElements;
    @Nullable private List<String> topImages;
    @Nullable private Consumer<OverviewIntent> onUiElementActionListener;
    @MessageType private final int viewType;
    private boolean thumbUp = false;
    private boolean thumbDown = false;
    private boolean isVoiceInput = false;
    private String text;
    private int questionLength;

    ImageQnaMessage(@MessageType int viewType) {
      this(viewType, /* text= */ "");
    }

    ImageQnaMessage(@MessageType int viewType, String text) {
      this.viewType = viewType;
      this.text = text;
    }

    @Nullable
    public List<UiElement> getUiElements() {
      return uiElements;
    }

    public void setUiElements(@Nullable List<UiElement> uiElements) {
      this.uiElements = uiElements;
    }

    @Nullable
    public Consumer<OverviewIntent> getOnUiElementActionListener() {
      return onUiElementActionListener;
    }

    public void setOnUiElementActionListener(
        @Nullable Consumer<OverviewIntent> onUiElementActionListener) {
      this.onUiElementActionListener = onUiElementActionListener;
    }

    @Nullable
    public List<String> getTopImages() {
      return topImages;
    }

    public void setTopImages(@Nullable List<String> topImages) {
      this.topImages = topImages;
    }

    public @MessageType int getViewType() {
      return viewType;
    }

    public String getText() {
      return text;
    }

    void setText(String text) {
      this.text = text;
    }

    public int getQuestionLength() {
      return questionLength;
    }

    public boolean isVoiceInput() {
      return isVoiceInput;
    }

    public boolean isThumbUp() {
      return thumbUp;
    }

    public boolean isThumbDown() {
      return thumbDown;
    }

    @CanIgnoreReturnValue
    public ImageQnaMessage setVoiceInput(boolean voiceInput) {
      isVoiceInput = voiceInput;
      return this;
    }

    @CanIgnoreReturnValue
    public ImageQnaMessage setThumbUp(boolean set) {
      thumbUp = set;
      if (set) {
        thumbDown = false;
      }
      return this;
    }

    @CanIgnoreReturnValue
    public ImageQnaMessage setThumbDown(boolean set) {
      thumbDown = set;
      if (set) {
        thumbUp = false;
      }
      return this;
    }

    @CanIgnoreReturnValue
    public ImageQnaMessage setQuestionLength(int questionLength) {
      this.questionLength = questionLength;
      return this;
    }
  }

  /** A {@link TransitionListener} that makes the animation looped. */
  private static class LoopAnimationTransitionListener implements TransitionListener {
    int startId;
    int endId;

    @Override
    public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {
      this.startId = startId;
      this.endId = endId;
    }

    @Override
    public void onTransitionChange(
        MotionLayout motionLayout, int startId, int endId, float progress) {
      // Do nothing.
    }

    @Override
    public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
      // Loop the animation.
      if (currentId == startId) {
        motionLayout.transitionToEnd();
      } else if (currentId == endId) {
        motionLayout.transitionToStart();
      }
    }

    @Override
    public void onTransitionTrigger(
        MotionLayout motionLayout, int triggerId, boolean positive, float progress) {
      // Do nothing.
    }
  }

  private static class ChatViewHolderWithActions extends ChatViewHolder {
    private final ViewGroup uiElementsContainer;
    private final View header;
    private Consumer<OverviewIntent> onUiElementActionListener;

    public ChatViewHolderWithActions(View view, int viewType) {
      super(view, viewType);
      uiElementsContainer = itemView.findViewById(R.id.ui_elements);
      header = itemView.findViewById(R.id.ui_elements_header);
    }

    public void setUiElements(List<UiElement> uiElements) {
      header.setVisibility(uiElements.isEmpty() ? View.GONE : View.VISIBLE);
      // This could be optimized by re-using the buttons already in there, and hiding the others
      uiElementsContainer.removeAllViews();

      for (UiElement uiElement : uiElements) {
        Button uiElementView =
            (Button)
                LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.image_qna_ui_element, uiElementsContainer, false);
        CharSequence label;
        CharSequence contentDescription;
        if (TextUtils.isEmpty(uiElement.getLabel())) {
          label = uiElement.getDescription();
          contentDescription = label;
        } else {
          if (TextUtils.isEmpty(uiElement.getDescription())) {
            label = uiElement.getLabel();
            contentDescription = label;
          } else {
            label = uiElement.getLabel();
            contentDescription = label + ": " + uiElement.getDescription();
          }
        }
        uiElementView.setText(label);
        uiElementView.setContentDescription(contentDescription);

        // We have to set the listener otherwise the call to replaceAccessibilityAction() won't work
        uiElementView.setOnLongClickListener(
            v -> {
              if (onUiElementActionListener != null) {
                onUiElementActionListener.accept(
                    new OverviewIntent.ChooseAction(uiElement, ACTION_CLICK));
                return true;
              }

              return false;
            });
        // Add accessibility actions
        ViewCompat.replaceAccessibilityAction(
            uiElementView,
            ACTION_LONG_CLICK,
            itemView.getContext().getString(R.string.action_activate),
            (view, commandArguments) -> {
              if (onUiElementActionListener != null) {
                onUiElementActionListener.accept(
                    new OverviewIntent.ChooseAction(uiElement, ACTION_CLICK));
                return true;
              }
              return false;
            });
        ViewCompat.replaceAccessibilityAction(
            uiElementView,
            ACTION_CLICK,
            itemView.getContext().getString(R.string.action_jump_focus),
            (view, commandArguments) -> {
              if (onUiElementActionListener != null) {
                onUiElementActionListener.accept(
                    new OverviewIntent.ChooseAction(
                        uiElement, AccessibilityActionCompat.ACTION_ACCESSIBILITY_FOCUS));
                return true;
              }
              return false;
            });

        uiElementsContainer.addView(uiElementView);
      }
    }

    public void setOnUiElementActionListener(Consumer<OverviewIntent> onUiElementActionListener) {
      this.onUiElementActionListener = onUiElementActionListener;
    }

    public Consumer<OverviewIntent> getOnUiElementActionListener() {
      return onUiElementActionListener;
    }
  }
}
