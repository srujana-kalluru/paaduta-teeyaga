package com.paaduta.constants;

public final class ApiConstants {

    public static final String REFERENCE_TRACK = "reference";
    public static final String USER_TRACK = "user";

    public static final String STATUS_OK = "ok";
    public static final String STATUS_INSUFFICIENT = "insufficient";

    public static final String CONFIDENCE_LOW = "low";
    public static final String CONFIDENCE_MEDIUM = "medium";
    public static final String CONFIDENCE_HIGH = "high";

    public static final String ALIGNMENT_METHOD = "voicing_overlap";
    public static final String ISSUE_ID_PREFIX = "issue-";

    public static final String PITCH_DEVIATION = "pitch_deviation";
    public static final String TIMING = "timing";
    public static final String MISSED = "missed";
    public static final String EXTRA = "extra";
    public static final String UNSTEADY = "unsteady";

    public static final String FLAT = "flat";
    public static final String SHARP = "sharp";
    public static final String LATE = "late";
    public static final String EARLY = "early";

    public static final String SUSTAINED_DEVIATION = "sustained_deviation";
    public static final String ONSET_OFFSET = "onset_offset";
    public static final String REFERENCE_ONLY = "reference_only";
    public static final String ATTEMPT_ONLY = "attempt_only";
    public static final String PITCH_WAVERING = "pitch_wavering";

    public static final int SECONDS_PLACES = 2;
    public static final int SUMMARY_PLACES = 1;
    public static final int HZ_PLACES = 1;
    public static final int PROBABILITY_PLACES = 2;
    public static final int CENTS_PLACES = 1;

    private ApiConstants() {
    }
}
