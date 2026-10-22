package com.paaduta.support;

import com.paaduta.constants.AudioConstants;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;

public final class Synth {

    public static final int RATE = AudioConstants.RATE;
    private static final int[] SCALE = {60, 62, 64, 65, 67, 69, 67, 65, 64, 62, 60};
    public static final int NOTES = SCALE.length;

    private Synth() {
    }

    public static double hz(double midi) {
        return 440 * Math.pow(2, (midi - 69) / 12);
    }

    public static double shift(double hz, double cents) {
        return hz * Math.pow(2, cents / 1200);
    }

    public static float[] notes(int rate, double totalSeconds, double[]... notes) {
        var out = new float[(int) (totalSeconds * rate)];
        for (double[] note : notes) {
            double depth = note.length > 3 ? note[3] : 0;
            double wobble = note.length > 4 ? note[4] : 0;
            double phase = 0;
            int from = (int) (note[1] * rate);
            int to = Math.min(out.length, (int) ((note[1] + note[2]) * rate));
            for (int i = from; i < to; i++) {
                double t = (double) (i - from) / rate;
                double envelope = Math.min(1, Math.min(t, (double) (to - i) / rate) / 0.02);
                phase += 2 * Math.PI * note[0] * Math.pow(2, depth * Math.sin(2 * Math.PI * wobble * t) / 1200) / rate;
                out[i] = (float) (0.3 * envelope * (Math.sin(phase) + 0.4 * Math.sin(2 * phase)) / 1.4);
            }
        }
        return out;
    }

    public static float[] notes(double totalSeconds, double[]... notes) {
        return notes(RATE, totalSeconds, notes);
    }

    public static float[] tone(double hz, double seconds) {
        return notes(seconds, new double[]{hz, 0, seconds});
    }

    public static float[] scale(double lead, double stretch, double[] cents, double[] late) {
        var notes = new double[NOTES][];
        for (int k = 0; k < SCALE.length; k++) {
            notes[k] = new double[]{shift(hz(SCALE[k]), cents[k]), lead + k * stretch + late[k], 0.7 * stretch - late[k]};
        }
        return notes(lead + SCALE.length * stretch + 0.5, notes);
    }

    public static float[] scale(double lead) {
        return scale(lead, 1, new double[NOTES], new double[NOTES]);
    }

    public static byte[] wav(float[] samples, int rate, int channels, int bits) {
        int width = bits / 8;
        var pcm = new byte[samples.length * channels * width];
        for (int i = 0; i < samples.length; i++) {
            int value = (int) (samples[i] * ((1 << (bits - 1)) - 1));
            for (int c = 0; c < channels; c++) {
                for (int k = 0; k < width; k++) {
                    pcm[(i * channels + c) * width + k] = (byte) (value >> (8 * k));
                }
            }
        }
        var format = new AudioFormat(rate, bits, channels, true, false);
        var out = new ByteArrayOutputStream();
        try {
            AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), format, samples.length), AudioFileFormat.Type.WAVE, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    public static byte[] wav(float[] samples) {
        return wav(samples, RATE, 1, 16);
    }
}
