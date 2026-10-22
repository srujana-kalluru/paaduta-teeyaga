package com.paaduta.analysis;

import com.paaduta.config.AnalysisProperties;
import com.paaduta.model.ComparisonResult;
import com.paaduta.model.Issue;
import com.paaduta.support.Synth;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComparisonTest {

    private static final AnalysisProperties DEFAULTS = new AnalysisProperties(50, 0.2, 0.6, 0.2, 0.5, 5, 70);
    private static final double[] STEADY = {220, 0.2, 2.5};

    private static ComparisonResult compare(float[] reference, float[] user, Integer keyShift, AnalysisProperties properties) {
        var ref = PitchTracker.track(reference);
        var usr = PitchTracker.track(user);
        return Comparison.compare(ref, usr, Aligner.estimate(ref, usr), keyShift, properties);
    }

    private static List<String> types(ComparisonResult result) {
        return result.issues().stream().map(Issue::type).toList();
    }

    @Test
    void convertsFrequencyRatiosToCents() {
        assertEquals(1200, Comparison.cents(440, 220), 1e-9);
        assertEquals(100, Comparison.cents(Synth.hz(70), Synth.hz(69)), 1e-9);
        assertEquals(-100, Comparison.cents(Synth.shift(220, -100), 220), 1e-9);
        assertEquals(0, Comparison.cents(330, 330), 1e-9);
    }

    @Test
    void flagsToneOneSemitoneSharpWhenSameKeyConfirmed() {
        var result = compare(Synth.tone(220, 3), Synth.tone(Synth.shift(220, 100), 3), 0, DEFAULTS);

        assertEquals(1, result.issues().size());
        var issue = result.issues().getFirst();
        assertEquals("pitch_deviation", issue.type());
        assertEquals("sharp", issue.direction());
        assertEquals(100, issue.medianDeviationCents(), 5);
        assertTrue(issue.confidence() >= DEFAULTS.minIssueConfidence());
    }

    @Test
    void holdsConstantShiftAsPossibleKeyMismatch() {
        var octaveBelow = compare(Synth.tone(440, 3), Synth.tone(220, 3), null, DEFAULTS);
        assertEquals(-12, octaveBelow.detectedKeyShift());
        assertTrue(octaveBelow.pitchHeld());
        assertTrue(octaveBelow.issues().isEmpty());
        assertEquals(0, octaveBelow.octaveSuspects());

        var inUserKey = compare(Synth.tone(440, 3), Synth.tone(220, 3), -12, DEFAULTS);
        assertFalse(inUserKey.pitchHeld());
        assertTrue(inUserKey.issues().isEmpty());
        assertTrue(inUserKey.comparedFrames() > 100);

        var inSameKey = compare(Synth.tone(440, 3), Synth.tone(220, 3), 0, DEFAULTS);
        assertEquals(List.of("pitch_deviation"), types(inSameKey));
        assertEquals(-1200, inSameKey.issues().getFirst().medianDeviationCents(), 5);
    }

    @Test
    void flagsSingleFlatNoteInShortClipInsteadOfAskingAboutKey() {
        var result = compare(Synth.tone(220, 1.5), Synth.tone(Synth.shift(220, -80), 1.5), null, DEFAULTS);

        assertFalse(result.pitchHeld());
        assertEquals(List.of("pitch_deviation"), types(result));
        assertEquals("flat", result.issues().getFirst().direction());
    }

    @Test
    void ignoresDeviationBelowCentsThreshold() {
        assertTrue(compare(Synth.tone(220, 3), Synth.tone(Synth.shift(220, 30), 3), 0, DEFAULTS).issues().isEmpty());
    }

    @Test
    void ignoresDeviationShorterThanMinimumDuration() {
        var user = Synth.notes(3, new double[]{220, 0, 1}, new double[]{Synth.shift(220, -90), 1, 0.12}, new double[]{220, 1.12, 1.88});

        assertTrue(compare(Synth.tone(220, 3), user, 0, DEFAULTS).issues().isEmpty());
    }

    @Test
    void dropsIssuesBelowConfidenceThreshold() {
        var strict = new AnalysisProperties(50, 0.2, 1.01, 0.2, 0.5, 5, 70);

        assertTrue(compare(Synth.tone(220, 3), Synth.tone(Synth.shift(220, 100), 3), 0, strict).issues().isEmpty());
    }

    @Test
    void reportsLateEntryAsTimingNotPitch() {
        var reference = Synth.notes(4, new double[]{220, 0.5, 1}, new double[]{247, 2, 1.5});
        var user = Synth.notes(4, new double[]{220, 0.5, 1}, new double[]{247, 2.35, 1.15});

        var issues = compare(reference, user, null, DEFAULTS).issues();

        assertEquals(1, issues.size());
        assertEquals("timing", issues.getFirst().type());
        assertEquals("late", issues.getFirst().direction());
        assertEquals(0.35, issues.getFirst().onsetDifferenceSeconds(), 0.04);
    }

    @Test
    void absorbsLateStartOfTheAttempt() {
        var reference = Synth.notes(5, new double[]{220, 0.5, 1}, new double[]{247, 2, 1}, new double[]{262, 3.5, 1});
        var attempt = Synth.notes(5, new double[]{220, 0.9, 1}, new double[]{247, 2, 1}, new double[]{262, 3.5, 1});

        assertEquals(List.of(), types(compare(reference, attempt, null, DEFAULTS)));
    }

    @Test
    void followsTempoDriftAndFlagsOnlyTheFlatNote() {
        double[] cents = new double[Synth.NOTES];
        cents[4] = -80;

        var result = compare(Synth.scale(0.3), Synth.scale(0.8, 1.08, cents, new double[Synth.NOTES]), null, DEFAULTS);

        assertEquals(List.of("pitch_deviation"), types(result));
        assertEquals("flat", result.issues().getFirst().direction());
        assertEquals(4.3, result.issues().getFirst().startSeconds(), 0.2);
    }

    @Test
    void toleratesVibratoAroundTheRightPitch() {
        var user = Synth.notes(3, new double[]{220, 0.2, 2.5, 60, 5.5});

        assertEquals(List.of(), types(compare(Synth.notes(3, STEADY), user, 0, DEFAULTS)));
    }

    @Test
    void flagsFlatNoteSungWithVibrato() {
        var user = Synth.notes(3, new double[]{Synth.shift(220, -70), 0.2, 2.5, 60, 5.5});

        var result = compare(Synth.notes(3, STEADY), user, 0, DEFAULTS);

        assertEquals(List.of("pitch_deviation"), types(result));
        assertEquals(-70, result.issues().getFirst().medianDeviationCents(), 15);
    }

    @Test
    void flagsSlowWaveringAsUnsteady() {
        var user = Synth.notes(3, new double[]{220, 0.2, 2.5, 70, 1.5});

        var found = types(compare(Synth.notes(3, STEADY), user, 0, DEFAULTS));

        assertFalse(found.isEmpty());
        assertTrue(found.stream().allMatch("unsteady"::equals));
    }

    @Test
    void flagsMissedNote() {
        var reference = Synth.notes(3, new double[]{220, 0.3, 0.6}, new double[]{247, 1.2, 0.6}, new double[]{262, 2.1, 0.6});
        var attempt = Synth.notes(3, new double[]{220, 0.3, 0.6}, new double[]{262, 2.1, 0.6});

        var result = compare(reference, attempt, 0, DEFAULTS);

        assertEquals(List.of("missed"), types(result));
        assertEquals(1.2, result.issues().getFirst().startSeconds(), 0.1);
    }

    @Test
    void flagsExtraNote() {
        var reference = Synth.notes(3, new double[]{220, 0.3, 0.6}, new double[]{247, 2.1, 0.6});
        var attempt = Synth.notes(3, new double[]{220, 0.3, 0.6}, new double[]{330, 1.2, 0.5}, new double[]{247, 2.1, 0.6});

        var result = compare(reference, attempt, 0, DEFAULTS);

        assertEquals(List.of("extra"), types(result));
        assertEquals(1.2, result.issues().getFirst().attemptStartSeconds(), 0.1);
    }
}
