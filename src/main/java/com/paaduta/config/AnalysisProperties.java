package com.paaduta.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("analysis")
public record AnalysisProperties(
        @DefaultValue("50") double pitchCents,
        @DefaultValue("0.2") double minIssueSeconds,
        @DefaultValue("0.6") double minIssueConfidence,
        @DefaultValue("0.2") double timingSeconds,
        @DefaultValue("0.3") double minAlignmentConfidence,
        @DefaultValue("10") double secondsPerIssue,
        @DefaultValue("5") int minIssues,
        @DefaultValue("20") int maxIssues,
        @DefaultValue("70") double unsteadyCents) {
}
