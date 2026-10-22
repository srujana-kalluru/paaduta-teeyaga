package com.paaduta.constants;

import javax.sound.sampled.AudioFormat;

public final class AudioConstants {

    public static final int RATE = 16_000;
    public static final int HOP = 320;
    public static final double HOP_SECONDS = (double) HOP / RATE;
    public static final double SILENCE_DB = -50;
    public static final double MAX_SECONDS = 180;
    public static final float PCM_SCALE = 32768f;
    public static final AudioFormat ANALYSIS_FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    private AudioConstants() {
    }
}
