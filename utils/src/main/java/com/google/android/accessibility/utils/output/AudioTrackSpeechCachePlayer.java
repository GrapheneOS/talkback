package com.google.android.accessibility.utils.output;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioFormat.ENCODING_PCM_32BIT;
import static android.media.AudioFormat.ENCODING_PCM_8BIT;
import static android.media.AudioFormat.ENCODING_PCM_FLOAT;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.PlaybackParams;
import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.google.android.accessibility.utils.Logger;
import com.google.android.accessibility.utils.output.WavFileLoader.WavData;
import com.google.android.libraries.accessibility.utils.log.LogUtils;
import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Loads WAV files into memory and plays them using AudioTrack. For now we don't support {@link
 * android.speech.tts.TextToSpeech.Engine#KEY_PARAM_PAN} because it is not used in accessibility
 * apps.
 */
public class AudioTrackSpeechCachePlayer extends SpeechCachePlayer {
  private static final String TAG = "AudioTrackSpeechCachePlayer";
  private static final long MAX_SLEEP_TIME_MS = 50;
  private static final long MIN_SLEEP_TIME_MS = 20;
  private static final long MAX_PROGRESS_WAIT_MS = 1000;

  /** Minimum size of the buffer, copied from {@code BlockingAudioTrack}. */
  private static final int MIN_BUFFER_SIZE_IN_BYTE = 8192;

  private final Map<SpeechInfo, byte[]> speechInfoPcmDataMap = new ConcurrentHashMap<>();
  private final LinkedBlockingQueue<SpeechPlaybackItem> playbackQueue = new LinkedBlockingQueue<>();
  private volatile boolean isPlayingCurrentSelection = false;
  private volatile int speechDataLength = 0;
  private volatile SpeechPlaybackItem speechPlaybackItem;

  // These parameters MUST be the same for all pre-loaded WAV files
  // They will be set when the first successfully parsed WAV is added.

  @GuardedBy("lock")
  private boolean audioTrackFormatDetermined = false;

  private int sampleRate;
  private int channelConfig;
  private int audioFormatEncoding;
  private int minBufferSize;
  private int bufferSize;
  private int bytesPerFrame;

  private float volume = 1.0f;

  @GuardedBy("lock")
  @Nullable
  private AudioTrack audioTrack;

  private final Thread thread;

  private final Object lock = new Object();

  /**
   * Creates an {@link AudioTrackSpeechCachePlayer} with the given maximum speech rate.
   *
   * @param callbackExecutor the executor to dispatch playback state callbacks.
   */
  public AudioTrackSpeechCachePlayer(Executor callbackExecutor) {
    super(callbackExecutor);
    thread = new Thread(new MessageLoop(), "speechCachePlaybackThread");
    thread.start();
  }

  @Override
  public boolean loadSpeechFileIntoMemory(SpeechInfo speechInfo) {
    File wavFile = speechInfo.speechFile;
    if (wavFile == null) {
      return false;
    }
    WavData wavData = WavFileLoader.loadWavFile(wavFile);
    if (wavData == null) {
      LogUtils.e(TAG, "Failed to load WAV file: %s", wavFile.getName());
      return false;
    }

    synchronized (lock) {
      if (initAudioTrackFormat(wavData)) {
        if (!isAudioTrackReady(audioTrack)) {
          // Release the existing AudioTrack if it's not ready before initializing a new one.
          releaseAudioTrack(/* resetAudioFormat= */ false);
        }
        AudioTrack newAudioTrack = initializeAudioTrack();
        if (newAudioTrack == null) {
          LogUtils.e(
              TAG, "Failed to initialize AudioTrack with format from %s ", wavFile.getName());
          return false;
        }
        audioTrack = newAudioTrack;
      } else if (!isAudioFormatCompatible(wavData, wavFile.getName())) {
        LogUtils.w(TAG, "incompatible audio format ", wavFile.getName());
        return false;
      }
    }

    byte[] pcmData = wavData.pcmData;
    speechInfoPcmDataMap.put(speechInfo, pcmData);
    int dataLength;
    synchronized (lock) {
      // Use temp variable to ensure atomic operation.
      dataLength = pcmData.length + speechDataLength;
      speechDataLength = dataLength;
    }
    LogUtils.i(
        TAG, "Successfully loaded PCM data for %s, stored data size=%s", speechInfo, dataLength);
    return true;
  }

  @GuardedBy("lock")
  private boolean initAudioTrackFormat(WavData wavData) {
    if (audioTrackFormatDetermined) {
      return false;
    }
    // This is the first successfully parsed WAV file, use its format for AudioTrack
    this.sampleRate = wavData.sampleRate;
    this.channelConfig = wavData.channelConfig;
    this.audioFormatEncoding = wavData.audioFormatEncoding;
    this.audioTrackFormatDetermined = true;
    return true;
  }

  private boolean isAudioFormatCompatible(WavData wavData, String fileName) {
    // AudioTrack is already initialized, check if this file's format is compatible
    if (this.sampleRate != wavData.sampleRate
        || this.channelConfig != wavData.channelConfig
        || this.audioFormatEncoding != wavData.audioFormatEncoding) {
      LogUtils.e(
          TAG,
          "WAV file format mismatch: %s sample rate=%s, channels=%s, encoding"
              + " bytes=%s, audioTrack format: sample rate=%s, channel=%s, encoding bytes=%s",
          fileName,
          wavData.sampleRate,
          (wavData.channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2),
          getBytesPerSample(wavData.audioFormatEncoding),
          this.sampleRate,
          (this.channelConfig == AudioFormat.CHANNEL_OUT_MONO ? 1 : 2),
          getBytesPerSample(this.audioFormatEncoding));
      return false;
    }
    return true;
  }

  @Nullable
  private AudioTrack initializeAudioTrack() {
    LogUtils.i(
        TAG,
        "Initializing AudioTrack, sampleRate==%s, ChannelConfig==%s, encoding format==%s",
        sampleRate,
        channelConfig,
        audioFormatEncoding);

    minBufferSize = AudioTrack.getMinBufferSize(sampleRate, channelConfig, audioFormatEncoding);
    if (minBufferSize <= 0) {
      LogUtils.e(TAG, "AudioTrack.getMinBufferSize failed: " + minBufferSize);
      return null;
    }
    int bytesPerSample = getBytesPerSample(audioFormatEncoding);

    bufferSize = Math.max(minBufferSize, MIN_BUFFER_SIZE_IN_BYTE);
    LogUtils.d(
        TAG,
        "AudioTrack parameters and bufferSize: "
            + bufferSize
            + " (SampleRate: "
            + sampleRate
            + ", Channels: "
            + channelConfig
            + ", Encoding: "
            + audioFormatEncoding
            + ")");
    AudioTrack.Builder builder =
        new AudioTrack.Builder()
            .setAudioAttributes(
                new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .build())
            .setAudioFormat(
                new AudioFormat.Builder()
                    .setEncoding(audioFormatEncoding)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelConfig)
                    .build())
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY);
    AudioTrack newAudioTrack;
    try {
      newAudioTrack = builder.build();
      PlaybackParams playbackParams = new PlaybackParams();
      playbackParams.setSpeed(1.0f);
      playbackParams.setPitch(1.0f);
      playbackParams.allowDefaults();
      newAudioTrack.setPlaybackParams(playbackParams);
      newAudioTrack.setVolume(volume);
      bytesPerFrame = bytesPerSample * newAudioTrack.getChannelCount();
    } catch (IllegalArgumentException e) {
      LogUtils.e(TAG, "IllegalArgumentException during AudioTrack creation: " + e.getMessage());
      return null;
    }

    if (newAudioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
      LogUtils.e(TAG, "AudioTrack initialization failed. State: " + newAudioTrack.getState());
      newAudioTrack.release();
      return null;
    }
    LogUtils.i(
        TAG,
        "AudioTrack initialized/re-initialized successfully with sampleRate=%s , channelConfig=%s,"
            + " audioFormatEncoding=%s",
        sampleRate,
        channelConfig,
        audioFormatEncoding);
    return newAudioTrack;
  }

  @Override
  public void stopSpeech() {
    LogUtils.d(TAG, "stopSpeech");
    stopSpeechInternal(getAudioTrack());
    LogUtils.d(TAG, "stopSpeech done");
  }

  private void stopSpeechInternal(@Nullable AudioTrack audioTrack) {
    LogUtils.d(TAG, "stopSpeechInternal queue size=" + playbackQueue.size());
    Iterator<SpeechPlaybackItem> it = playbackQueue.iterator();
    while (it.hasNext()) {
      final SpeechPlaybackItem item = it.next();
      it.remove();
      dispatchPlaybackState(item, STATE_STOPPED);
    }
    SpeechPlaybackItem item = speechPlaybackItem;
    if (item != null) {
      stopPlaybackItem(item, audioTrack);
    }
  }

  private void stopPlaybackItem(
      SpeechPlaybackItem currentSpeechPlaybackItem, @Nullable AudioTrack track) {
    LogUtils.d(TAG, "stopPlaybackItem speechInfo= %s", currentSpeechPlaybackItem.speechInfo);
    isPlayingCurrentSelection = false;
    if (track != null && track.getState() == AudioTrack.STATE_INITIALIZED) {
      if (track.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
        try {
          track.pause();
          track.flush();
        } catch (IllegalStateException e) {
          LogUtils.w(TAG, "AudioTrack operation failed during stopPlaybackItem", e);
          // Release the AudioTrack because it's in an illegal state.
          releaseAudioTrack(/* resetAudioFormat= */ false);
          dispatchPlaybackState(currentSpeechPlaybackItem, STATE_STOPPED);
          return;
        }
      }
      if (!currentSpeechPlaybackItem.isInterrupted()) {
        dispatchPlaybackState(currentSpeechPlaybackItem, STATE_STOPPED);
      }
    }
  }

  @Override
  @SuppressWarnings("Interruption")
  public void release() {
    stopSpeech();
    speechInfoPcmDataMap.clear();
    // Release the AudioTrack and reset the audio format information.
    releaseAudioTrack(/* resetAudioFormat= */ true);
    thread.interrupt();
  }

  private void releaseAudioTrack(boolean resetAudioFormat) {
    AudioTrack track;
    synchronized (lock) {
      track = audioTrack;
      audioTrack = null;
      if (resetAudioFormat) {
        audioTrackFormatDetermined = false;
      }
      volume = 1.0f;
    }
    if (track != null) {
      try {
        track.release();
      } catch (IllegalStateException e) {
        LogUtils.w(TAG, "IllegalStateException during track.release().", e);
      }
    }
  }

  @Override
  public boolean playSpeech(SpeechInfo speechInfo, PlaybackStateListener playStateCallback) {
    AudioTrack track = getAudioTrack();
    if (!isAudioTrackReady(track)) {
      LogUtils.w(TAG, "playSpeech --AudioTrack is not ready, try to reinitialize");
      track = initializeAudioTrack();
      if (track == null) {
        return false;
      }
    }
    synchronized (lock) {
      audioTrack = track;
    }

    stopSpeechInternal(track);
    byte[] audioData = speechInfoPcmDataMap.get(speechInfo);
    if (audioData == null || audioData.length == 0) {
      LogUtils.w(TAG, "playSpeech -- %s of audio data is not found.", speechInfo);
      return false;
    }

    track.setVolume(speechInfo.volume);
    SpeechPlaybackItem currentItem =
        new AudioTrackPlaybackItem(
            speechInfo.utteranceId, speechInfo, audioData, track, playStateCallback);
    try {
      playbackQueue.put(currentItem);
    } catch (InterruptedException e) {
      LogUtils.w(TAG, "InterruptedException during playbackQueue.put: " + e.getMessage());
      return false;
    }
    return true;
  }

  @Override
  public boolean isPlaying() {
    return !playbackQueue.isEmpty() || isPlayingCurrentSelection;
  }

  private void playSpeechInternal(
      AudioTrack audioTrack,
      int internalBufferSize,
      AudioTrackPlaybackItem speechPlaybackItem,
      float volume,
      byte[] audioData) {
    LogUtils.d(TAG, "playSpeechInternal=" + speechPlaybackItem);
    // Use this to allow breaking the loop
    isPlayingCurrentSelection = true;
    int offset = 0;
    try {
      if (audioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING) {
        audioTrack.play();
      }
      audioTrack.setVolume(volume);
      int frameStartPosition = audioTrack.getPlaybackHeadPosition();

      byte[] buffer = new byte[internalBufferSize]; // Re-use a buffer for writing

      int bytesToWrite;
      LogUtils.d(
          TAG,
          "playSpeechInternal, internalBufferSize=%s bytes, " + " audioData size=%s bytes",
          internalBufferSize,
          audioData.length);

      dispatchPlaybackState(speechPlaybackItem, STATE_READY);
      boolean played = false;
      while (isPlayingCurrentSelection && offset < audioData.length) {
        // Simulate reading from the larger audioData buffer into a smaller write buffer
        bytesToWrite = Math.min(buffer.length, audioData.length - offset);
        if (bytesToWrite <= 0) {
          break;
        }

        System.arraycopy(audioData, offset, buffer, 0, bytesToWrite);
        int written = audioTrack.write(buffer, 0, bytesToWrite);

        if (written < 0) {
          LogUtils.e(TAG, "AudioTrack.write() failed, release audioTrack: " + written);
          // Release the AudioTrack due to a write error.
          releaseAudioTrack(/* resetAudioFormat= */ false);
          dispatchPlaybackState(speechPlaybackItem, STATE_ERROR);
          break;
        }

        if (written == 0) { // Should not usually happen if data is provided
          LogUtils.w(TAG, "AudioTrack.write() wrote 0 bytes");
          // Potentially add a small sleep to avoid tight loop if this happens unexpectedly
          try {
            Thread.sleep(5);
          } catch (InterruptedException e) {
            LogUtils.w(TAG, "Interrupted while waiting for AudioTrack.write()");
            break;
          }
        } else {
          if (!played) {
            played = true;
            LogUtils.d(TAG, "start to play:%s", speechPlaybackItem.utteranceId);
            if (!speechPlaybackItem.isInterrupted()) {
              dispatchPlaybackState(speechPlaybackItem, STATE_PLAYED);
            }
          }
        }
        offset += written;
      }
      if (offset != 0 && offset == audioData.length) {
        LogUtils.d(TAG, "%s write %s completed", speechPlaybackItem.utteranceId, offset);
        int totalFrameLength = audioData.length / bytesPerFrame + frameStartPosition;
        waitForComplete(audioTrack, offset, totalFrameLength);
        if (!speechPlaybackItem.isInterrupted()) {
          dispatchPlaybackState(speechPlaybackItem, STATE_COMPLETED);
        }
      }
    } catch (IllegalStateException e) {
      // Release the AudioTrack because it's in an illegal state during playback.
      releaseAudioTrack(/* resetAudioFormat= */ false);
      dispatchPlaybackState(speechPlaybackItem, STATE_ERROR);
      LogUtils.e(
          TAG, e, "IllegalStateException during playback: %s", speechPlaybackItem.utteranceId);
    } finally {
      if (!speechPlaybackItem.isInterrupted()) {
        dispatchPlaybackState(speechPlaybackItem, STATE_STOPPED);
      }
      isPlayingCurrentSelection = false;
    }
  }

  private boolean isAudioTrackReady(AudioTrack audioTrack) {
    return audioTrack != null && audioTrack.getState() == AudioTrack.STATE_INITIALIZED;
  }

  @Override
  public long getCacheSizeMb() {
    return speechDataLength / 1024 / 1024;
  }

  @Override
  public void dumpCachedSpeechInfo(Logger logger) {
    Iterator<SpeechInfo> speechInfoIterator = speechInfoPcmDataMap.keySet().iterator();
    logger.log("Cached SpeechInfo:");
    while (speechInfoIterator.hasNext()) {
      logger.log(speechInfoIterator.next().dump());
    }
    logger.log(
        "%s caches, audio data size in memory is %s MB",
        speechInfoPcmDataMap.size(), getCacheSizeMb());
  }

  @Override
  public void cleanCaches() {
    speechInfoPcmDataMap.clear();
    speechDataLength = 0;
    // Release the audio track and reset the audio format information to be re-initialized by the
    // first new WAV file.
    releaseAudioTrack(/* resetAudioFormat= */ true);
  }

  private static long clip(long value, long min, long max) {
    return value < min ? min : Math.min(value, max);
  }

  private void waitForComplete(AudioTrack audioTrack, int bytesWritten, int writtenFrames) {

    if (audioTrack == null) {
      return;
    }
    // For "small" audio tracks, we have to stop() them to make them mixable,
    // else the audio subsystem will wait indefinitely for us to fill the buffer
    // before rendering the track mixable.
    boolean isShortUtterance = false;
    if (bytesWritten < bufferSize && isPlayingCurrentSelection) {
      LogUtils.d(
          TAG,
          "Stopping small audio track to flush audio, state was : " + audioTrack.getPlayState());
      isShortUtterance = true;
      audioTrack.stop();
    }

    // Block until the audio track is done only if we haven't stopped yet.
    if (isPlayingCurrentSelection) {
      LogUtils.d(TAG, "Waiting for audio track to complete : ");
      if (isShortUtterance) {
        // In this case we would have called AudioTrack#stop() to flush
        // buffers to the mixer. This makes the playback head position
        // unobservable and notification markers do not work reliably. We
        // have no option but to wait until we think the track would finish
        // playing and release it after.
        //
        // This isn't as bad as it looks because (a) We won't end up waiting
        // for much longer than we should because even at 4khz mono, a short
        // utterance weighs in at about 2 seconds, and (b) such short utterances
        // are expected to be relatively infrequent and in a stream of utterances
        // this shows up as a slightly longer pause.
        blockUntilEstimatedCompletion(audioTrack, writtenFrames);
      } else {
        blockUntilCompletion(audioTrack, writtenFrames);
      }
    }
  }

  private void blockUntilEstimatedCompletion(@Nullable AudioTrack audioTrack, int writtenFrames) {
    if (audioTrack == null) {
      LogUtils.w(TAG, "AudioTrack is null, skip blockUntilEstimatedCompletion");
      return;
    }
    final long estimatedTimeMs = writtenFrames * 1000L / audioTrack.getSampleRate();
    LogUtils.d(TAG, "About to sleep for: " + estimatedTimeMs);

    try {
      Thread.sleep(estimatedTimeMs);
    } catch (InterruptedException ie) {
      // Do nothing.
    }
  }

  private void blockUntilCompletion(AudioTrack audioTrack, int writtenFrames) {
    if (audioTrack == null) {
      LogUtils.w(TAG, "AudioTrack is null, skip blockUntilCompletion");
      return;
    }

    int previousPosition = -1;
    int currentPosition;
    long blockedTimeMs = 0;

    while (isPlayingCurrentSelection
        && audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING
        && ((currentPosition = audioTrack.getPlaybackHeadPosition()) < writtenFrames)) {
      final long estimatedTimeMs =
          ((writtenFrames - currentPosition) * 1000L) / audioTrack.getSampleRate();
      final long sleepTimeMs = clip(estimatedTimeMs, MIN_SLEEP_TIME_MS, MAX_SLEEP_TIME_MS);

      // Check if the audio track has made progress since the last loop
      // iteration. We should then add in the amount of time that was
      // spent sleeping in the last iteration.
      if (currentPosition == previousPosition) {
        // This works only because the sleep time that would have been calculated
        // would be the same in the previous iteration too.
        blockedTimeMs += sleepTimeMs;
        // If we've taken too long to make progress, bail.
        if (blockedTimeMs > MAX_PROGRESS_WAIT_MS) {
          LogUtils.w(
              TAG,
              "Waited unsuccessfully for "
                  + MAX_PROGRESS_WAIT_MS
                  + "ms "
                  + "for AudioTrack to make progress, Aborting");
          break;
        }
      } else {
        blockedTimeMs = 0;
      }
      previousPosition = currentPosition;

      LogUtils.d(
          TAG,
          "About to sleep for : "
              + sleepTimeMs
              + " ms,"
              + " Playback position : "
              + currentPosition
              + ", Length in frames : "
              + writtenFrames);
      try {
        Thread.sleep(sleepTimeMs);
      } catch (InterruptedException ie) {
        break;
      }
    }
    if (audioTrack.getPlayState() == AudioTrack.PLAYSTATE_PLAYING) {
      audioTrack.stop();
      LogUtils.d(TAG, "AudioTrack is played and completed, stop it");
    }
  }

  static int getBytesPerSample(int audioFormat) {
    return switch (audioFormat) {
      case ENCODING_PCM_8BIT -> 1;
      case ENCODING_PCM_16BIT -> 2;
      case ENCODING_PCM_FLOAT, ENCODING_PCM_32BIT -> 4;
      default -> throw new IllegalArgumentException("Bad audio format " + audioFormat);
    };
  }

  @Override
  public boolean hasCache(SpeechInfo speechInfo) {
    return speechInfoPcmDataMap.get(speechInfo) != null;
  }

  @Override
  public void removeCache(SpeechInfo speechInfo) {
    byte[] audioBytes = speechInfoPcmDataMap.remove(speechInfo);
    if (audioBytes != null) {
      synchronized (lock) {
        int currentLength = speechDataLength - audioBytes.length;
        speechDataLength = currentLength;
      }
    } else {
      LogUtils.d(TAG, "removeCache -- not found cache with %s", speechInfo);
    }
  }

  @VisibleForTesting
  @Nullable
  AudioTrack getAudioTrack() {
    synchronized (lock) {
      return audioTrack;
    }
  }

  private final class MessageLoop implements Runnable {
    @Override
    public void run() {
      while (true) {
        SpeechPlaybackItem item;
        try {
          item = playbackQueue.take();
        } catch (InterruptedException ie) {
          LogUtils.d(TAG, "MessageLoop : Shutting down (interrupted)");
          return;
        }
        // If stopSpeech() is called between playbackQueue.take()
        // returning and speechPlaybackItem being set, the current item
        // will be run anyway.
        speechPlaybackItem = item;
        item.run();
        speechPlaybackItem = null;
      }
    }
  }

  private class AudioTrackPlaybackItem extends SpeechPlaybackItem {
    private final byte[] audioData;
    private final AudioTrack audioTrack;

    private AudioTrackPlaybackItem(
        String utteranceId,
        SpeechInfo speechInfo,
        byte[] audioData,
        AudioTrack audioTrack,
        PlaybackStateListener callback) {
      super(utteranceId, callback, speechInfo);
      this.audioData = audioData;
      this.audioTrack = audioTrack;
    }

    @Override
    public void run() {
      if (this.isInterrupted()) {
        LogUtils.d(TAG, "utterance %s is stopped before playing", speechInfo.utteranceId);
        return;
      }

      if (!isAudioTrackReady(audioTrack)) {
        dispatchPlaybackState(this, STATE_ERROR);
        return;
      }
      dispatchPlaybackState(this, STATE_START);
      playSpeechInternal(audioTrack, minBufferSize, this, speechInfo.volume, audioData);
      LogUtils.i(TAG, "Finished or stopped playback for %s", speechInfo);
    }
  }
}
