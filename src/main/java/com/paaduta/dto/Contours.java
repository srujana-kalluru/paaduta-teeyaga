package com.paaduta.dto;

import java.util.List;

public record Contours(List<Double> timeSeconds, List<Double> attemptTimeSeconds, List<Double> referenceF0Hz, List<Double> userF0Hz,
                       List<Double> referenceVoicedProbability, List<Double> userVoicedProbability, List<Double> deviationCents) {
}
