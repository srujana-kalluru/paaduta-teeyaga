package com.paaduta.analysis;

import com.paaduta.model.Alignment;
import com.paaduta.model.Contour;
import com.paaduta.util.Stats;

import java.util.Arrays;
import java.util.stream.IntStream;

import static com.paaduta.constants.AudioConstants.HOP_SECONDS;

public final class Aligner {

    private static final double NEAR_TIE = 0.98;
    private static final String ALIGNMENT_METHOD = "voicing_overlap";
    private static final int SECONDS_PLACES = 2;
    private static final int PROBABILITY_PLACES = 2;

    private Aligner() {
    }

    public static Alignment estimate(Contour reference, Contour user) {
        double[] a = reference.probability();
        double[] b = user.probability();
        var overlap = new double[a.length + b.length - 1];
        for (int lag = 1 - a.length; lag < b.length; lag++) {
            double sum = 0;
            for (int i = Math.max(0, -lag); i < Math.min(a.length, b.length - lag); i++) {
                sum += Math.min(a[i], b[i + lag]);
            }
            overlap[lag + a.length - 1] = sum;
        }
        double best = Arrays.stream(overlap).max().orElse(0);
        int start = firstVoiced(user) - firstVoiced(reference);
        int chosen = start;
        double distance = Double.MAX_VALUE;
        for (int lag = 1 - a.length; lag < b.length; lag++) {
            if (overlap[lag + a.length - 1] >= NEAR_TIE * best && Math.abs(lag - start) < distance) {
                distance = Math.abs(lag - start);
                chosen = lag;
            }
        }
        double mass = overlap[chosen + a.length - 1];
        double union = Arrays.stream(a).sum() + Arrays.stream(b).sum() - mass;
        return new Alignment(Stats.round(chosen * HOP_SECONDS, SECONDS_PLACES),
                Stats.round(union <= 0 ? 0 : mass / union, PROBABILITY_PLACES), ALIGNMENT_METHOD);
    }

    private static int firstVoiced(Contour contour) {
        return IntStream.range(0, contour.frames()).filter(i -> contour.f0()[i] != null).findFirst().orElse(0);
    }
}
