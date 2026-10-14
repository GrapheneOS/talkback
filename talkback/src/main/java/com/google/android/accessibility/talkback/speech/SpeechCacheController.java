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

package com.google.android.accessibility.talkback.speech;

import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_ADDED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_REMOVED;
import static android.view.accessibility.AccessibilityEvent.WINDOWS_CHANGE_TITLE;
import static com.google.android.accessibility.talkback.compositor.Compositor.EVENT_SYNTHESIZE_SPEECH;

import android.accessibilityservice.AccessibilityService;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.text.Spannable;
import android.text.TextUtils;
import android.text.style.LocaleSpan;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import com.google.android.accessibility.talkback.Interpretation.CompositorID;
import com.google.android.accessibility.talkback.Pipeline.InterpretationReceiver;
import com.google.android.accessibility.talkback.monitor.RingerModeAndScreenMonitor.ScreenChangedListener;
import com.google.android.accessibility.talkback.speech.SpeechCacheController.WindowKey;
import com.google.android.accessibility.utils.AccessibilityEventListener;
import com.google.android.accessibility.utils.AccessibilityNodeInfoUtils;
import com.google.android.accessibility.utils.AccessibilityServiceCompatUtils;
import com.google.android.accessibility.utils.AccessibilityWindowInfoUtils;
import com.google.android.accessibility.utils.Filter;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.Performance.EventId;
import com.google.android.accessibility.utils.output.FailoverTextToSpeech.FailoverTtsListener;
import com.google.android.accessibility.utils.output.SpeechCacheManager.LoadSpeechResultNotifier;
import com.google.android.accessibility.utils.output.SpeechCachePlayer.SpeechInfo;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Manages caching synthesized speech for on-screen text, especially for IME windows. It processes
 * the tasks on a background thread since traverse and synthesis is a time-consuming operation.
 *
 * <p>It listens for window changes to detect IME windows. When an IME appears, it finds text entry
 * keys, synthesizes their content descriptions, and caches the speech.
 *
 * <p>The LRU cache uses {@link WindowKey} (window title and locale) for efficient retrieval.
 *
 * <p>It handles TTS engine changes by re-synthesizing cached items and retries failed syntheses
 * when screen is non-interactive.
 */
public class SpeechCacheController
    implements AccessibilityEventListener,
        FailoverTtsListener,
        RemovalListener<WindowKey, List<SpeechInfo>>,
        Handler.Callback,
        ScreenChangedListener {

  /**
   * Each keyboard also has emoji and digital keyboard. Assume users use 3 keyboards, so we choose
   * 9;
   */
  public static final int DEFAULT_MAX_CACHED_WINDOWS_SIZE = 9;

  /** The constant copied from {@code AccessibilityWindowInfo#UNDEFINED_WINDOW_ID}} */
  private static final int UNDEFINED_WINDOW_ID = -1;

  private static final int MSG_REFILL_CACHE = 1;

  private static final int MSG_RETRY_SYNTHESIS = 2;

  private static final int DELAY_REFILL_CACHE_MS = 1000;

  private static final String TAG = "SpeechCacheController";

  public static final String DEFAULT_CACHE_SUPPORT_TTS_ENGINE = "com.google.android.tts";

  private final AccessibilityService accessibilityService;

  /**
   * An LRU cache. When the {@link SpeechInfo} list is evicted, we will delete its file. The key is
   * {@link WindowKey}
   */
  private final Cache<WindowKey, List<SpeechInfo>> successCache;

  /** A map to store the failed synthesis speech for retry. */
  private final Map<WindowKey, List<SpeechInfo>> synthesisFailureMap;

  private final Handler workerHandler;
  private final InterpretationReceiver pipeline;
  private final SpeechController speechController;
  private final AccessibilityEventHelper accessibilityEventHelper;
  private final String cacheSupportTtsEngine;

  private String ttsPackageName = "";
  private int ttsWindowId = UNDEFINED_WINDOW_ID;

  private final SpeechParametsObserver speechParametsObserver = new SpeechParametsObserver();
  private volatile boolean enabled = true;
  private volatile boolean isEngineSupported = false;

  public SpeechCacheController(
      AccessibilityService accessibilityService,
      SpeechController speechController,
      InterpretationReceiver pipeline,
      int maxCachedWindowSize,
      String cacheSupportTtsEngine) {
    this.accessibilityService = accessibilityService;
    this.speechController = speechController;
    this.pipeline = pipeline;
    HandlerThread handlerThread =
        new HandlerThread("SpeechCacheController", Process.THREAD_PRIORITY_BACKGROUND);
    handlerThread.start();
    workerHandler = new Handler(handlerThread.getLooper(), this);
    successCache =
        CacheBuilder.newBuilder().removalListener(this).maximumSize(maxCachedWindowSize).build();
    synthesisFailureMap = new ConcurrentHashMap<>();
    speechController.getFailoverTts().addListener(this);
    speechController.addObserver(speechParametsObserver);
    accessibilityEventHelper =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.P
            ? new LegacyAccessibilityEventHelper()
            : new AccessibilityEventApi29Helper();
    this.cacheSupportTtsEngine = cacheSupportTtsEngine;
  }

  @VisibleForTesting
  Looper getWorkerHandlerLooper() {
    return workerHandler.getLooper();
  }

  @Override
  public int getEventTypes() {
    if (!isActivated()) {
      return 0;
    }
    return AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
        | AccessibilityEvent.TYPE_WINDOWS_CHANGED
        | AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
  }

  @Override
  public void onAccessibilityEvent(AccessibilityEvent event, EventId eventId) {
    int windowChange =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? event.getWindowChanges() : 0;
    SimpleAccessibilityEvent simpleEvent =
        new SimpleAccessibilityEvent(event.getEventType(), event.getWindowId(), windowChange);
    workerHandler.post(() -> onAccessibilityEventInternal(simpleEvent, eventId));
  }

  @WorkerThread
  private void onAccessibilityEventInternal(SimpleAccessibilityEvent event, EventId eventId) {
    LogUtils.d(TAG, "onAccessibilityEventInternal:" + event);
    AccessibilityWindowInfo actionWindow = accessibilityEventHelper.getActionWindow(event, eventId);
    if (actionWindow != null && AccessibilityWindowInfoUtils.isImeWindow(actionWindow)) {
      handleImeWindowChangedIfNeeded(actionWindow, eventId);
    }

    if (isTTsEngineWindow(actionWindow) && ttsWindowId == UNDEFINED_WINDOW_ID) {
      ttsWindowId = actionWindow.getId();
      LogUtils.d(TAG, "onA11yEventInternal users open the window relevant to current TTS");
    }

    if (ttsWindowId != UNDEFINED_WINDOW_ID
        && accessibilityEventHelper.isTtsWindowRemoved(event, ttsWindowId)) {
      ttsWindowId = UNDEFINED_WINDOW_ID;
      refillCacheWithDelay();
    }
  }

  /** Returns the counts of windows that have been traversed and cached. */
  public long getCacheWindowCounts() {
    return successCache.size();
  }

  /** Returns the size of speech cache in MB. */
  public long getCacheSizeMb() {
    if (isActivated()) {
      return speechController.getCacheSizeMb();
    }
    return -1;
  }

  private interface AccessibilityEventHelper {
    @Nullable
    AccessibilityWindowInfo getActionWindow(SimpleAccessibilityEvent event, EventId eventId);

    boolean isTtsWindowRemoved(SimpleAccessibilityEvent event, int ttsWindowId);
  }

  private class LegacyAccessibilityEventHelper implements AccessibilityEventHelper {

    @Nullable
    @Override
    public AccessibilityWindowInfo getActionWindow(
        SimpleAccessibilityEvent event, EventId eventId) {
      return AccessibilityServiceCompatUtils.getWindowInfo(accessibilityService, event.windowId());
    }

    @Override
    public boolean isTtsWindowRemoved(SimpleAccessibilityEvent event, int ttsWindowId) {
      AccessibilityWindowInfo activeWindow =
          AccessibilityServiceCompatUtils.getActiveWidow(accessibilityService);
      return activeWindow != null && (activeWindow.getId() != ttsWindowId);
    }
  }

  private class AccessibilityEventApi29Helper implements AccessibilityEventHelper {

    @Override
    @Nullable
    public AccessibilityWindowInfo getActionWindow(
        SimpleAccessibilityEvent event, EventId eventId) {
      if (windowsChangedOrWindowTitleChanged(event)
          || event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
        return AccessibilityServiceCompatUtils.getWindowInfo(
            accessibilityService, event.windowId());
      }
      return null;
    }

    @SuppressWarnings("NewApi")
    private boolean windowsChangedOrWindowTitleChanged(SimpleAccessibilityEvent event) {
      return (event.windowChange() & (WINDOWS_CHANGE_ADDED | WINDOWS_CHANGE_TITLE)) != 0;
    }

    @Override
    public boolean isTtsWindowRemoved(SimpleAccessibilityEvent event, int ttsWindowId) {
      if ((event.windowChange() & WINDOWS_CHANGE_REMOVED) != 0) {
        return ttsWindowId == event.windowId();
      }
      return false;
    }
  }

  private boolean isTTsEngineWindow(@Nullable AccessibilityWindowInfo window) {
    if (window == null) {
      return false;
    }
    AccessibilityNodeInfo root = window.getRoot();
    return root != null && TextUtils.equals(ttsPackageName, root.getPackageName());
  }

  @WorkerThread
  private void refillSpeechCache() {
    LogUtils.d(TAG, "refillSpeechCache");
    speechController.cleanCache();
    successCache
        .asMap()
        .forEach(
            (windowKey, speechInfos) -> {
              LogUtils.v(TAG, "refillSpeechCache, windowkey=%s", windowKey);
              final ImmutableList<SpeechInfo> speechInfosCopy = ImmutableList.copyOf(speechInfos);
              final List<SpeechInfo> synthesisFailureSpeeches =
                  getSynthesisFailuresSpeeches(windowKey);
              Consumer<SpeechInfo> synthesisTask =
                  speechInfo ->
                      speechController.addSpeech(
                          speechInfo.charsequence,
                          new SynthesisCallback(
                              speechInfos, synthesisFailureSpeeches, workerHandler),
                          /* speechParams= */ null,
                          /* eventId= */ null);
              speechInfos.clear();
              synthesisFailureSpeeches.forEach(synthesisTask);
              speechInfosCopy.forEach(synthesisTask);
            });
  }

  public void dump(Logger logger) {
    logger.log("SpeechCacheController enabled:" + enabled);
    logger.log("SpeechCacheController tts package name:" + ttsPackageName);
    logger.log("SpeechCacheController cached ime window title:");
    logger.log(successCache.asMap().keySet().toString());
  }

  /**
   * Enables or disables the SpeechCacheController. The controller is only truly activated when both
   * {@link #enabled} is {@code true} and {@link #isEngineSupported} is {@code true}. When disabled,
   * all cached speeches are cleared and any pending tasks are removed.
   *
   * @param enabled {@code true} to enable the controller, {@code false} to disable it.
   */
  public void setEnabled(boolean enabled) {
    if (enabled == this.enabled) {
      return;
    }
    this.enabled = enabled;
    if (!enabled) {
      reset();
      speechController.removeObserver(speechParametsObserver);
    } else {
      speechController.addObserver(speechParametsObserver);
    }
  }

  private void reset() {
    speechController.cleanCache();
    this.successCache.invalidateAll();
    this.synthesisFailureMap.clear();
    this.ttsWindowId = UNDEFINED_WINDOW_ID;
    workerHandler.removeCallbacksAndMessages(/* token= */ null);
  }

  private boolean isActivated() {
    return enabled && isEngineSupported;
  }

  public void destroy() {
    workerHandler.getLooper().quit();
  }

  private List<SpeechInfo> getSynthesisFailuresSpeeches(WindowKey windowKey) {
    return synthesisFailureMap.computeIfAbsent(windowKey, k -> new ArrayList<>());
  }

  @WorkerThread
  private void handleImeWindowChangedIfNeeded(AccessibilityWindowInfo imeWindow, EventId eventId) {
    WindowKey windowKey = buildWindowKey(imeWindow);
    LogUtils.d(TAG, "build ime windowKey:,%s ", windowKey);
    AccessibilityNodeInfo rootNode = imeWindow.getRoot();
    if (rootNode == null) {
      LogUtils.d(TAG, "root node is null");
      return;
    }
    cacheSpeechesForTextEntryKeyIfNeeded(
        eventId, AccessibilityNodeInfoCompat.wrap(rootNode), windowKey);
  }

  @Nullable
  private WindowKey buildWindowKey(AccessibilityWindowInfo windowInfo) {
    String nodeIdentifier = createIdentifierOfFirstTextEntryKey(windowInfo);
    if (TextUtils.isEmpty(nodeIdentifier)) {
      return null;
    }

    CharSequence windowTitle = windowInfo.getTitle();
    if (windowTitle == null) {
      return createWindowKey(/* windowTitle= */ null, nodeIdentifier, /* locale= */ null);
    }
    Locale locale = null;
    if (windowTitle instanceof Spannable spannable) {
      LocaleSpan[] spans = spannable.getSpans(0, windowTitle.length(), LocaleSpan.class);
      locale = spans.length == 1 ? spans[0].getLocale() : null;
    }
    return createWindowKey(windowTitle, nodeIdentifier, locale);
  }

  private WindowKey createWindowKey(
      @Nullable CharSequence windowTitle, String nodeIdentifier, @Nullable Locale locale) {
    return new WindowKey(windowTitle + "_" + nodeIdentifier, locale);
  }

  @WorkerThread
  private void cacheSpeechesForTextEntryKeyIfNeeded(
      EventId eventId, AccessibilityNodeInfoCompat windowRoot, WindowKey windowKey) {
    if (windowKey == null) {
      return;
    }

    final List<SpeechInfo> speechInfoCaches = successCache.getIfPresent(windowKey);
    final List<SpeechInfo> synthesizeFailureSpeech = getSynthesisFailuresSpeeches(windowKey);
    final ImmutableList<SpeechInfo> synthesizeFailureSpeechCopy =
        ImmutableList.copyOf(synthesizeFailureSpeech);
    synthesizeFailureSpeech.clear();
    if (speechInfoCaches != null) {
      LogUtils.d(TAG, "Nodes are traversed:" + windowKey);
      synthesizeFailureSpeechCopy.forEach(
          speechInfo ->
              speechController.addSpeech(
                  speechInfo.charsequence,
                  (speechInfo1, success) -> {
                    if (success) {
                      speechInfoCaches.add(speechInfo1);
                    } else {
                      synthesizeFailureSpeech.add(speechInfo1);
                    }
                  },
                  /* speechParams= */ null,
                  /* eventId= */ null));
    } else {
      // First Time, traverse
      LogUtils.d(TAG, "First Time, traverse:" + windowKey);
      final List<SpeechInfo> availableSpeechInfoCaches = new ArrayList<>();
      successCache.put(windowKey, availableSpeechInfoCaches);
      traverseTextEntryKeyToAddSpeech(
          eventId,
          windowRoot,
          new SynthesisCallback(availableSpeechInfoCaches, synthesizeFailureSpeech, workerHandler));
    }
  }

  @WorkerThread
  private void traverseTextEntryKeyToAddSpeech(
      EventId eventId,
      AccessibilityNodeInfoCompat windowRoot,
      LoadSpeechResultNotifier synthesisCallback) {
    long startTime = SystemClock.uptimeMillis();
    List<AccessibilityNodeInfoCompat> textEntryKeys = findTextEntryKeys(windowRoot);
    LogUtils.d(
        TAG,
        "traverseTextEntryKeyToAddSpeech traverse  latency= :%d",
        (SystemClock.uptimeMillis() - startTime));
    if (textEntryKeys == null || textEntryKeys.isEmpty()) {
      LogUtils.d(TAG, "no textEntryKey within the window");
      return;
    }
    startTime = SystemClock.uptimeMillis();
    textEntryKeys.forEach(
        accessibilityNodeInfoCompat ->
            synthesizeCache(accessibilityNodeInfoCompat, eventId, synthesisCallback));
    LogUtils.d(
        TAG,
        "traverseTextEntryKeyToAddSpeech add cache latency= :%d",
        (SystemClock.uptimeMillis() - startTime));
  }

  private List<AccessibilityNodeInfoCompat> findTextEntryKeys(
      AccessibilityNodeInfoCompat windowRoot) {
    return AccessibilityNodeInfoUtils.getMatchingDescendantsOrRoot(
        windowRoot,
        new Filter<>() {
          @Override
          public boolean accept(AccessibilityNodeInfoCompat node) {
            if (node == null) {
              return false;
            }
            if (node.getText() == null && node.getContentDescription() == null) {
              return false;
            }
            return node.isTextEntryKey() && node.getViewIdResourceName() != null;
          }
        });
  }

  private String createIdentifierOfFirstTextEntryKey(AccessibilityWindowInfo window) {
    AccessibilityNodeInfo rootNode = window.getRoot();
    if (rootNode == null) {
      return "";
    }
    AccessibilityNodeInfoCompat rootNodeCompat = AccessibilityNodeInfoCompat.wrap(rootNode);

    AccessibilityNodeInfoCompat firstTextEntryKey =
        AccessibilityNodeInfoUtils.getSelfOrMatchingDescendant(
            rootNodeCompat,
            new Filter<>() {
              @Override
              public boolean accept(AccessibilityNodeInfoCompat node) {
                if (node == null) {
                  return false;
                }
                if (!node.isTextEntryKey() || node.getViewIdResourceName() == null) {
                  return false;
                }
                return node.getText() != null || node.getContentDescription() != null;
              }
            });
    if (firstTextEntryKey == null) {
      return "";
    }
    if (!TextUtils.isEmpty(firstTextEntryKey.getContentDescription())) {
      return firstTextEntryKey.getContentDescription().toString();
    }

    if (!TextUtils.isEmpty(firstTextEntryKey.getText())) {
      return firstTextEntryKey.getText().toString();
    }
    return "";
  }

  @WorkerThread
  private void synthesizeCache(
      AccessibilityNodeInfoCompat textEntryKeyNode,
      EventId eventId,
      LoadSpeechResultNotifier statusNotifier) {
    pipeline.input(
        eventId,
        /* event= */ null,
        new CompositorID(EVENT_SYNTHESIZE_SPEECH, statusNotifier),
        textEntryKeyNode);
  }

  @Override
  public void onTtsInitialized(boolean wasSwitchingEngines, String enginePackageName) {
    LogUtils.d(TAG, "onTtsInitialized new Engine=%s", enginePackageName);
    ttsPackageName = enginePackageName;
    isEngineSupported =
        enginePackageName != null && TextUtils.equals(cacheSupportTtsEngine, enginePackageName);
    if (!isEngineSupported) {
      reset();
      return;
    }
    if (wasSwitchingEngines) {
      // TTS engine might be changed frequently when users are interacting with the voice page.
      refillCacheWithDelay();
    }
  }

  @Override
  public void onUtteranceRangeStarted(String utteranceId, int start, int end) {}

  @Override
  public void onUtteranceCompleted(String utteranceId, boolean success) {}

  @Override
  public void onRemoval(RemovalNotification<WindowKey, List<SpeechInfo>> notification) {
    LogUtils.d(TAG, "onRemoval notification:" + notification);
    List<SpeechInfo> speechInfos = notification.getValue();
    if (speechInfos == null || speechInfos.isEmpty()) {
      return;
    }
    LogUtils.d(TAG, notification.getKey() + " is evicted");
    workerHandler.post(() -> speechInfos.forEach(speechController::removeSpeech));
  }

  private void refillCacheWithDelay() {
    if (!isActivated()) {
      return;
    }
    workerHandler.removeMessages(MSG_REFILL_CACHE);
    workerHandler.sendEmptyMessageDelayed(MSG_REFILL_CACHE, DELAY_REFILL_CACHE_MS);
  }

  @Override
  public boolean handleMessage(@NonNull Message msg) {
    LogUtils.d(TAG, "handleMessage: %s", messageIdToString(msg.what));
    if (msg.what == MSG_REFILL_CACHE) {
      refillSpeechCache();
      return true;
    }
    if (msg.what == MSG_RETRY_SYNTHESIS) {
      EventId eventId = (EventId) msg.obj;
      synthesisFailureMap.forEach(
          (windowKey, speechInfos) -> {
            List<SpeechInfo> successSpeeches = successCache.getIfPresent(windowKey);
            if (successSpeeches == null) {
              successSpeeches = new ArrayList<>();
              successCache.put(windowKey, successSpeeches);
            }

            SynthesisCallback callback =
                new SynthesisCallback(successSpeeches, speechInfos, workerHandler);
            speechInfos.forEach(
                speechInfo ->
                    speechController.addSpeech(
                        speechInfo.charsequence, callback, /* speechParams= */ null, eventId));
          });
    }
    return false;
  }

  @Override
  public void onScreenChanged(boolean isInteractive, EventId eventId) {
    if (!isActivated()) {
      return;
    }
    LogUtils.d(TAG, "onScreenChanged: %s", isInteractive);
    if (!isInteractive) {
      Message message = workerHandler.obtainMessage(MSG_RETRY_SYNTHESIS, eventId);
      workerHandler.sendMessageDelayed(message, 2000);
    } else {
      workerHandler.removeMessages(MSG_RETRY_SYNTHESIS);
    }
  }

  private class SpeechParametsObserver implements SpeechController.Observer {

    @Override
    public void onSpeechStarting() {}

    @Override
    public void onSpeechCompleted() {}

    @Override
    public void onSpeechPaused() {}

    @Override
    public void onSpeechRateChanged(float rate) {
      refillCacheWithDelay();
    }

    @Override
    public void onSpeechPitchChanged(float pitch) {
      refillCacheWithDelay();
    }

    @Override
    public void onDefaultPitchChanged(float pitch) {
      refillCacheWithDelay();
    }

    @Override
    public void onDefaultRateChanged(float rate) {
      refillCacheWithDelay();
    }
  }

  record WindowKey(String windowTitle, Locale locale) {}

  record SimpleAccessibilityEvent(int eventType, int windowId, int windowChange) {}

  private static String messageIdToString(int message) {
    if (message == MSG_REFILL_CACHE) {
      return "MSG_REFILL_CACHE";
    }
    if (message == MSG_RETRY_SYNTHESIS) {
      return "MSG_RETRY_SYNTHESIS";
    }
    return Integer.toString(message);
  }

  private record SynthesisCallback(
      List<SpeechInfo> speechInfosCaches,
      List<SpeechInfo> synthesizeFailureSpeechInfos,
      Handler workerHandler)
      implements LoadSpeechResultNotifier {

    @Override
    public void onFinished(SpeechInfo speechInfo, Boolean success) {
      workerHandler.post(() -> handleFinished(speechInfo, success));
    }

    private void handleFinished(SpeechInfo speechInfo, Boolean success) {
      LogUtils.d(TAG, "handleFinished %s: %s", success, speechInfo);
      if (success) {
        speechInfosCaches.add(speechInfo);
      } else {
        LogUtils.w(TAG, "add speech fail: %s", speechInfo);
        synthesizeFailureSpeechInfos.add(speechInfo);
      }
    }
  }
}
