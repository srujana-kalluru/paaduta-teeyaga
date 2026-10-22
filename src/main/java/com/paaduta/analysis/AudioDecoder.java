package com.paaduta.analysis;

import com.paaduta.exception.AnalysisException;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.paaduta.constants.AnalysisConstants.FRAME;
import static com.paaduta.constants.AudioConstants.ANALYSIS_FORMAT;
import static com.paaduta.constants.AudioConstants.MAX_SECONDS;
import static com.paaduta.constants.AudioConstants.PCM_SCALE;
import static com.paaduta.constants.AudioConstants.RATE;
import static com.paaduta.constants.ErrorCode.CORRUPT_AUDIO;
import static com.paaduta.constants.ErrorCode.EMPTY_AUDIO;
import static com.paaduta.constants.ErrorCode.TOO_LONG;
import static com.paaduta.constants.ErrorCode.UNSUPPORTED_FORMAT;

public final class AudioDecoder {

    private AudioDecoder() {
    }

    public static float[] decode(byte[] bytes, String track) {
        byte[] pcm;
        try (var source = AudioSystem.getAudioInputStream(new ByteArrayInputStream(bytes));
             var converted = AudioSystem.getAudioInputStream(ANALYSIS_FORMAT, source)) {
            pcm = converted.readAllBytes();
        } catch (UnsupportedAudioFileException | IllegalArgumentException e) {
            throw new AnalysisException(UNSUPPORTED_FORMAT, track, e);
        } catch (IOException e) {
            throw new AnalysisException(CORRUPT_AUDIO, track, e);
        }
        var samples = new float[pcm.length / 2];
        for (int i = 0; i < samples.length; i++) {
            samples[i] = (short) ((pcm[2 * i] & 0xff) | (pcm[2 * i + 1] << 8)) / PCM_SCALE;
        }
        if (samples.length < FRAME) {
            throw new AnalysisException(EMPTY_AUDIO, track);
        }
        if (samples.length > MAX_SECONDS * RATE) {
            throw new AnalysisException(TOO_LONG, track);
        }
        return samples;
    }
}
