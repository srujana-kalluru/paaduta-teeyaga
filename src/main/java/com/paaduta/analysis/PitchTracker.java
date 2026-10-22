package com.paaduta.analysis;

import com.paaduta.model.Contour;

import java.util.Arrays;

import static com.paaduta.constants.AudioConstants.HOP;
import static com.paaduta.constants.AudioConstants.RATE;
import static com.paaduta.constants.AudioConstants.SILENCE_DB;

public final class PitchTracker {

    private static final int TAU_MIN = RATE / 1100;
    private static final int TAU_MAX = RATE / 65;
    private static final int WINDOW = 512;
    static final int FRAME = WINDOW + TAU_MAX;
    private static final double YIN_THRESHOLD = 0.2;
    private static final double EPSILON = 1e-12;
    private static final int MIN_VOICED_RUN = 3;

    private PitchTracker() {
    }

    public static Contour track(float[] x) {
        int n = Math.max(0, (x.length - FRAME) / HOP + 1);
        var f0 = new Double[n];
        var probability = new double[n];
        var db = new double[n];
        var d = new double[TAU_MAX + 1];
        for (int i = 0; i < n; i++) {
            int start = i * HOP;
            db[i] = levelDb(x, start);
            if (db[i] >= SILENCE_DB) {
                normalisedDifference(x, start, d);
                int best = bestLag(d);
                probability[i] = Math.max(0, 1 - d[best]);
                if (d[best] < YIN_THRESHOLD) {
                    f0[i] = RATE / refine(d, best);
                }
            }
        }
        dropShortRuns(f0);
        return new Contour((double) x.length / RATE, f0, probability, db);
    }

    private static double levelDb(float[] x, int start) {
        double energy = 0;
        for (int j = start; j < start + WINDOW; j++) {
            energy += x[j] * x[j];
        }
        return 10 * Math.log10(energy / WINDOW + EPSILON);
    }

    private static void normalisedDifference(float[] x, int start, double[] d) {
        double running = 0;
        d[0] = 1;
        for (int tau = 1; tau <= TAU_MAX; tau++) {
            double diff = 0;
            for (int j = start; j < start + WINDOW; j++) {
                double delta = x[j] - x[j + tau];
                diff += delta * delta;
            }
            running += diff;
            d[tau] = running <= 0 ? 1 : diff * tau / running;
        }
    }

    private static int bestLag(double[] d) {
        int best = TAU_MIN;
        int tau = TAU_MIN;
        while (tau <= TAU_MAX && d[tau] >= YIN_THRESHOLD) {
            if (d[tau] < d[best]) {
                best = tau;
            }
            tau++;
        }
        if (tau > TAU_MAX) {
            return best;
        }
        while (tau < TAU_MAX && d[tau + 1] < d[tau]) {
            tau++;
        }
        return tau;
    }

    private static double refine(double[] d, int tau) {
        if (tau >= TAU_MAX) {
            return tau;
        }
        double before = d[tau - 1];
        double after = d[tau + 1];
        double curvature = before - 2 * d[tau] + after;
        return Math.abs(curvature) < EPSILON ? tau : tau + (before - after) / (2 * curvature);
    }

    private static void dropShortRuns(Double[] f0) {
        int i = 0;
        while (i < f0.length) {
            int end = i;
            while (end < f0.length && f0[end] != null) {
                end++;
            }
            if (end - i < MIN_VOICED_RUN) {
                Arrays.fill(f0, i, end, null);
            }
            i = Math.max(end, i + 1);
        }
    }
}
