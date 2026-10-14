package com.google.android.accessibility.utils.output;

import static android.media.AudioFormat.ENCODING_PCM_16BIT;
import static android.media.AudioFormat.ENCODING_PCM_8BIT;
import static android.media.AudioFormat.ENCODING_PCM_FLOAT;

import android.media.AudioFormat;
import android.util.Log;
import androidx.annotation.Nullable;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** A utility class to load and parse WAV files. */
public class WavFileLoader {

  private static final String TAG = "WavFileLoader";

  // WAV header constants
  private static final int RIFF_CHUNK_ID_OFFSET = 0;
  private static final int RIFF_CHUNK_ID_LENGTH = 4;
  private static final String RIFF_CHUNK_ID = "RIFF";
  private static final int WAVE_FORMAT_OFFSET = 8;
  private static final int WAVE_FORMAT_LENGTH = 4;
  private static final String WAVE_FORMAT = "WAVE";
  private static final int FMT_SUBCHUNK_ID_OFFSET = 12;
  private static final int FMT_SUBCHUNK_ID_LENGTH = 4;
  private static final String FMT_SUBCHUNK_ID = "fmt ";
  private static final int AUDIO_FORMAT_OFFSET = 20;
  private static final short PCM_AUDIO_FORMAT = 1;
  private static final int NUM_CHANNELS_OFFSET = 22;
  private static final int SAMPLE_RATE_OFFSET = 24;
  private static final int BITS_PER_SAMPLE_OFFSET = 34;
  private static final int DATA_SUBCHUNK_SIZE_OFFSET = 40;

  public static final int HEADER_SIZE = 44;

  /** Holds the parsed data from a WAV file. */
  public static class WavData {
    public final byte[] pcmData;
    public final int sampleRate;
    public final int channelConfig;
    public final int audioFormatEncoding;

    WavData(byte[] pcmData, int sampleRate, int channelConfig, int audioFormatEncoding) {
      this.pcmData = pcmData;
      this.sampleRate = sampleRate;
      this.channelConfig = channelConfig;
      this.audioFormatEncoding = audioFormatEncoding;
    }
  }

  private WavFileLoader() {}

  /**
   * Loads a WAV file, parses its header, and extracts the PCM data.
   *
   * @param wavFile The WAV file to load.
   * @return A {@link WavData} object containing the PCM data and format info, or {@code null} if
   *     loading fails.
   */
  @Nullable
  public static WavData loadWavFile(File wavFile) {
    if (wavFile == null || !wavFile.exists()) {
      Log.e(TAG, "Invalid WAV file or file does not exist.");
      return null;
    }

    try (InputStream inputStream = new FileInputStream(wavFile);
        ByteArrayOutputStream pcmOutputStream = new ByteArrayOutputStream()) {

      // --- WAV Header Parsing ---
      byte[] header = new byte[HEADER_SIZE];
      int bytesReadInHeader = inputStream.read(header, 0, header.length);
      if (bytesReadInHeader < HEADER_SIZE) {
        Log.e(TAG, "Could not read full WAV header from: " + wavFile.getName());
        return null;
      }

      ByteBuffer buffer = ByteBuffer.wrap(header);
      buffer.order(ByteOrder.LITTLE_ENDIAN);

      if (!(new String(header, RIFF_CHUNK_ID_OFFSET, RIFF_CHUNK_ID_LENGTH).equals(RIFF_CHUNK_ID)
          && new String(header, WAVE_FORMAT_OFFSET, WAVE_FORMAT_LENGTH).equals(WAVE_FORMAT))) {
        Log.e(TAG, wavFile.getName() + " is not a valid WAV file (RIFF/WAVE tags missing).");
        return null;
      }
      if (!new String(header, FMT_SUBCHUNK_ID_OFFSET, FMT_SUBCHUNK_ID_LENGTH)
          .equals(FMT_SUBCHUNK_ID)) {
        Log.e(TAG, wavFile.getName() + ": WAV 'fmt ' chunk not found at expected offset.");
        return null;
      }

      short audioFormatCode = buffer.getShort(AUDIO_FORMAT_OFFSET);
      if (audioFormatCode != PCM_AUDIO_FORMAT) {
        Log.e(
            TAG,
            "Unsupported WAV audio format code: "
                + audioFormatCode
                + " in "
                + wavFile.getName()
                + ". Only PCM is supported.");
        return null;
      }

      short numChannels = buffer.getShort(NUM_CHANNELS_OFFSET);
      int currentSampleRate = buffer.getInt(SAMPLE_RATE_OFFSET);
      short bitsPerSample = buffer.getShort(BITS_PER_SAMPLE_OFFSET);
      int dataChunkSize = buffer.getInt(DATA_SUBCHUNK_SIZE_OFFSET);

      int currentChannelConfig;
      switch (numChannels) {
        case 1 -> currentChannelConfig = AudioFormat.CHANNEL_OUT_MONO;
        case 2 -> currentChannelConfig = AudioFormat.CHANNEL_OUT_STEREO;
        default -> {
          Log.e(TAG, "Unsupported number of channels: " + numChannels + " in " + wavFile.getName());
          return null;
        }
      }

      int currentAudioFormatEncoding;
      switch (bitsPerSample) {
        case 8 -> currentAudioFormatEncoding = ENCODING_PCM_8BIT;
        case 16 -> currentAudioFormatEncoding = ENCODING_PCM_16BIT;
        case 32 -> currentAudioFormatEncoding = ENCODING_PCM_FLOAT;
        default -> {
          Log.e(TAG, "Unsupported bits per sample: " + bitsPerSample + " in " + wavFile.getName());
          return null;
        }
      }
      // --- End WAV Header Parsing ---

      // --- Read PCM Data into ByteArrayOutputStream ---
      byte[] readBuffer = new byte[4096];
      int bytesReadFromPcm;
      int totalPcmBytesRead = 0;

      int limit = (dataChunkSize > 0) ? dataChunkSize : Integer.MAX_VALUE;

      while (totalPcmBytesRead < limit
          && (bytesReadFromPcm =
                  inputStream.read(
                      readBuffer, 0, Math.min(readBuffer.length, limit - totalPcmBytesRead)))
              != -1) {
        pcmOutputStream.write(readBuffer, 0, bytesReadFromPcm);
        totalPcmBytesRead += bytesReadFromPcm;
      }
      // --- End PCM Data Reading ---

      if (pcmOutputStream.size() == 0) {
        return null;
      }
      byte[] pcmData = pcmOutputStream.toByteArray();
      return new WavData(
          pcmData, currentSampleRate, currentChannelConfig, currentAudioFormatEncoding);

    } catch (FileNotFoundException e) {
      Log.e(TAG, "WAV File not found: " + wavFile.getAbsolutePath(), e);
    } catch (IOException e) {
      Log.e(TAG, "IOException during WAV loading: " + wavFile.getAbsolutePath(), e);
    } catch (RuntimeException e) { // Catch any other unexpected exceptions.
      Log.e(TAG, "Unexpected error loading WAV: " + wavFile.getAbsolutePath(), e);
    }
    return null;
  }
}
