package com.paaduta.dto;

import com.paaduta.model.Alignment;
import com.paaduta.model.Issue;

import java.util.List;

public record AnalysisResponse(String status, double durationSeconds, Alignment alignment, Summary summary, Contours contours,
                               List<Issue> issues, List<Notice> warnings, int keyShiftSemitones, int detectedKeyShiftSemitones) {
}
