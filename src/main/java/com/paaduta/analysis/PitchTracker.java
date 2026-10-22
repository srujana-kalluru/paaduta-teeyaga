package com.paaduta.analysis;

import com.paaduta.model.Contour;
import com.paaduta.util.Stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.paaduta.constants.AnalysisConstants.EPSILON;
import static com.paaduta.constants.AnalysisConstants.FRAME;
import static com.paaduta.constants.AnalysisConstants.MIN_VOICED_RUN;
import static com.paaduta.constants.AnalysisConstants.OCTAVE_CONTEXT;
import static com.paaduta.constants.AnalysisConstants.OCTAVE_TOLERANCE;
import static com.paaduta.constants.AnalysisConstants.TAU_MAX;
import static com.paaduta.constants.AnalysisConstants.TAU_MIN;
import static com.paaduta.constants.AnalysisConstants.WINDOW;
import static com.paaduta.constants.AnalysisConstants.YIN_THRESHOLD;
import static com.paaduta.constants.AudioConstants.HOP;
import static com.paaduta.constants.AudioConstants.RATE;
import static com.paaduta.constants.AudioConstants.SILENCE_DB;

public final class PitchTracker {

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
        foldOctaveErrors(f0);
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

    private static void foldOctaveErrors(Double[] f0) {
        var original = f0.clone();
        for (int i = 0; i < f0.length; i++) {
            if (original[i] != null) {
                var around = neighbourhood(original, i);
                if (around.size() >= OCTAVE_CONTEXT) {
                    double octaves = Math.log(original[i] / Stats.median(around)) / Math.log(2);
                    if (Math.abs(Math.abs(octaves) - 1) < OCTAVE_TOLERANCE) {
                        f0[i] = original[i] * (octaves > 0 ? 0.5 : 2);
                    }
                }
            }
        }
    }

    private static List<Double> neighbourhood(Double[] f0, int centre) {
        var around = new ArrayList<Double>();
        for (int k = Math.max(0, centre - OCTAVE_CONTEXT); k <= Math.min(f0.length - 1, centre + OCTAVE_CONTEXT); k++) {
            if (f0[k] != null) {
                around.add(f0[k]);
            }
        }
        return around;
    }
}
