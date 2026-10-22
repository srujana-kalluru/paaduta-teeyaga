package com.paaduta.model;

public record Issue(String id, double startSeconds, double endSeconds, double attemptStartSeconds, double attemptEndSeconds,
                    String type, String direction, Double medianDeviationCents, Double onsetDifferenceSeconds,
                    double confidence, String evidence) {

    public Issue withId(String newId) {
        return new Issue(newId, startSeconds, endSeconds, attemptStartSeconds, attemptEndSeconds, type, direction,
                medianDeviationCents, onsetDifferenceSeconds, confidence, evidence);
    }
}
