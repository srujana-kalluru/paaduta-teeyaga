package com.paaduta.model;

import java.util.List;

public record ComparisonResult(List<Double> userF0, List<Double> userProbability, List<Double> deviationCents, List<Double> attemptTime,
                               List<Issue> issues, int detectedKeyShift, int octaveSuspects, int comparedFrames, boolean pitchHeld) {
}
