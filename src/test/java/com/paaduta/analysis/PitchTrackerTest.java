package com.paaduta.analysis;

import com.paaduta.constants.AudioConstants;
import com.paaduta.support.Synth;
import com.paaduta.util.Stats;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PitchTrackerTest {

    @Test
    void tracks220HzTone() {
        var contour = PitchTracker.track(Synth.tone(220, 2));

        assertEquals(220, Stats.median(Stats.present(contour.f0())), 0.5);
        assertTrue(contour.voicedFrames() > 0.9 * contour.frames());
    }

    @Test
    void tracksToneOneSemitoneHigher() {
        var low = PitchTracker.track(Synth.tone(220, 2));
        var high = PitchTracker.track(Synth.tone(Synth.shift(220, 100), 2));

        double cents = 1200 * Math.log(Stats.median(Stats.present(high.f0())) / Stats.median(Stats.present(low.f0()))) / Math.log(2);
        assertEquals(100, cents, 5);
    }

    @Test
    void silenceIsNullNotZero() {
        var contour = PitchTracker.track(Synth.notes(3, new double[]{220, 0, 1}, new double[]{220, 2, 1}));
        int middle = (int) (1.5 / AudioConstants.HOP_SECONDS);

        assertNull(contour.f0()[middle]);
        assertEquals(0, contour.probability()[middle]);
        assertTrue(Arrays.stream(contour.f0()).noneMatch(f -> f != null && f <= 0));
    }

    @Test
    void silentClipHasNoVoicedFrames() {
        assertEquals(0, PitchTracker.track(new float[Synth.RATE * 2]).voicedFrames());
    }
}
