package com.paaduta.analysis;

import com.paaduta.support.Synth;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlignerTest {

    private static float[] phrase(double lead) {
        double[] durations = {0.6, 0.9, 0.4, 1.2, 0.7, 0.5};
        double[] gaps = {0.3, 0.5, 0.25, 0.4, 0.6, 0};
        var notes = new double[durations.length][];
        double t = lead;
        for (int k = 0; k < durations.length; k++) {
            notes[k] = new double[]{Synth.hz(60 + 2 * k), t, durations[k]};
            t += durations[k] + gaps[k];
        }
        return Synth.notes(t + 0.5, notes);
    }

    @Test
    void recoversKnownOffsetWithinTwoFrames() {
        var alignment = Aligner.estimate(PitchTracker.track(phrase(0.3)), PitchTracker.track(phrase(1.0)));

        assertEquals(0.7, alignment.offsetSeconds(), 0.04);
        assertTrue(alignment.confidence() > 0.8);
        assertEquals("voicing_overlap", alignment.method());
    }

    @Test
    void alignsShortUnbrokenPhraseOnSingingStart() {
        var reference = Synth.notes(2.5, new double[]{220, 0.5, 1.5});
        var attempt = Synth.notes(2.5, new double[]{220, 0.8, 1.2});

        var alignment = Aligner.estimate(PitchTracker.track(reference), PitchTracker.track(attempt));

        assertEquals(0.3, alignment.offsetSeconds(), 0.04);
        assertTrue(alignment.confidence() > 0.7);
    }

    @Test
    void recoversNegativeOffset() {
        var alignment = Aligner.estimate(PitchTracker.track(phrase(1.2)), PitchTracker.track(phrase(0.2)));

        assertEquals(-1.0, alignment.offsetSeconds(), 0.04);
    }

    @Test
    void silentClipHasNoConfidence() {
        var alignment = Aligner.estimate(PitchTracker.track(phrase(0.3)), PitchTracker.track(new float[Synth.RATE * 4]));

        assertEquals(0, alignment.confidence());
    }
}
