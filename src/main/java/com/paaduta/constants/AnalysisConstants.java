package com.paaduta.constants;

import static com.paaduta.constants.AudioConstants.RATE;

public final class AnalysisConstants {

    public static final int TAU_MIN = RATE / 1100;
    public static final int TAU_MAX = RATE / 65;
    public static final int WINDOW = 512;
    public static final int FRAME = WINDOW + TAU_MAX;
    public static final double YIN_THRESHOLD = 0.2;
    public static final double EPSILON = 1e-12;
    public static final int MIN_VOICED_RUN = 3;
    public static final int OCTAVE_CONTEXT = 10;
    public static final double OCTAVE_TOLERANCE = 1 / 6.0;

    public static final double CENTS_PER_SEMITONE = 100;
    public static final double CENTS_PER_OCTAVE = 1200;
    public static final int SEMITONES_PER_OCTAVE = 12;

    public static final double NEAR_TIE = 0.98;

    public static final int WARP_BAND = 37;
    public static final int WARP_WIDTH = 2 * WARP_BAND + 1;
    public static final double STEP_PENALTY = 0.15;
    public static final double MAX_STEP_SEMITONES = 3;
    public static final byte STEP_UP = 1;
    public static final byte STEP_LEFT = 2;

    public static final double OCTAVE_SUSPECT_CENTS = 1000;
    public static final double KEY_SHIFT_MIN_CENTS = 150;
    public static final double SILENT_PROBABILITY = 0.2;
    public static final int MAX_GAP = 3;
    public static final int MAX_STALL = 10;
    public static final int SMOOTHING = 4;
    public static final int CHUNK = 25;
    public static final double CHUNK_MIN_COVERAGE = 0.8;
    public static final double SPREAD_LOW_PERCENTILE = 0.2;
    public static final double SPREAD_HIGH_PERCENTILE = 0.8;
    public static final int ONSET_SILENCE = 8;
    public static final int ONSET_MIN_RUN = 10;
    public static final int ONSET_WINDOW = 37;
    public static final int ONSET_VOICING_FRAMES = 5;
    public static final int TIMING_TAIL = 15;

    public static final int MIN_VOICED_FRAMES = 15;
    public static final double TOO_SHORT_SECONDS = 1;
    public static final double MOSTLY_SILENT_FRACTION = 0.2;
    public static final double LOUD_PERCENTILE = 0.95;
    public static final double LOW_VOLUME_DB = -35;
    public static final double NOISY_PROBABILITY = 0.6;
    public static final int MIN_OCTAVE_SUSPECTS = 5;
    public static final double OCTAVE_SUSPECT_RATIO = 0.05;
    public static final double LOW_COVERAGE = 0.3;
    public static final double HIGH_COVERAGE = 0.6;
    public static final double HIGH_ALIGNMENT = 0.7;

    private AnalysisConstants() {
    }
}
