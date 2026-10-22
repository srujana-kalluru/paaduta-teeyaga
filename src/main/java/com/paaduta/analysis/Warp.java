package com.paaduta.analysis;

import com.paaduta.model.Contour;

final class Warp {

    private static final double CENTS_PER_SEMITONE = 100;
    private static final int WARP_BAND = 37;
    private static final int WARP_WIDTH = 2 * WARP_BAND + 1;
    private static final double STEP_PENALTY = 0.15;
    private static final double MAX_STEP_SEMITONES = 3;
    private static final byte STEP_UP = 1;
    private static final byte STEP_LEFT = 2;

    private Warp() {
    }

    static int[] path(Contour ref, Contour user, int lag, int keyShift) {
        int n = ref.frames();
        var total = new double[n][WARP_WIDTH];
        var from = new byte[n][WARP_WIDTH];
        for (int i = 0; i < n; i++) {
            for (int k = 0; k < WARP_WIDTH; k++) {
                double cost = cost(ref, user, i, i + lag - WARP_BAND + k, keyShift);
                total[i][k] = i == 0 ? cost : cost + cheapestStep(total, from, i, k);
            }
        }
        return backtrack(from, argmin(total[n - 1]), lag);
    }

    private static double cheapestStep(double[][] total, byte[][] from, int i, int k) {
        double best = total[i - 1][k];
        if (k + 1 < WARP_WIDTH && total[i - 1][k + 1] + STEP_PENALTY < best) {
            best = total[i - 1][k + 1] + STEP_PENALTY;
            from[i][k] = STEP_UP;
        }
        if (k > 0 && total[i][k - 1] + STEP_PENALTY < best) {
            best = total[i][k - 1] + STEP_PENALTY;
            from[i][k] = STEP_LEFT;
        }
        return best;
    }

    private static int argmin(double[] row) {
        int best = 0;
        for (int k = 1; k < row.length; k++) {
            if (row[k] < row[best]) {
                best = k;
            }
        }
        return best;
    }

    private static int[] backtrack(byte[][] from, int end, int lag) {
        var map = new int[from.length];
        int i = from.length - 1;
        int k = end;
        map[i] = i + lag - WARP_BAND + k;
        while (i > 0) {
            byte move = from[i][k];
            if (move == STEP_LEFT) {
                k--;
            } else {
                i--;
                k += move == STEP_UP ? 1 : 0;
            }
            map[i] = i + lag - WARP_BAND + k;
        }
        return map;
    }

    private static double cost(Contour ref, Contour user, int i, int j, int keyShift) {
        boolean inside = j >= 0 && j < user.frames();
        Double a = ref.f0()[i];
        Double b = inside ? user.f0()[j] : null;
        if (a != null && b != null) {
            return Math.min(MAX_STEP_SEMITONES, Math.abs(Comparison.cents(b, a) / CENTS_PER_SEMITONE - keyShift)) / MAX_STEP_SEMITONES;
        }
        return Math.abs(ref.probability()[i] - (inside ? user.probability()[j] : 0));
    }
}
