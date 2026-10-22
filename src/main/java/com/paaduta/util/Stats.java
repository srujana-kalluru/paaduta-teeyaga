package com.paaduta.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public final class Stats {

    private Stats() {
    }

    public static double round(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    public static List<Double> round(List<Double> values, int places) {
        return values.stream().map(v -> v == null ? null : round(v, places)).toList();
    }

    public static List<Double> boxed(double[] values) {
        return Arrays.stream(values).boxed().toList();
    }

    public static double percentile(double[] values, double p) {
        if (values.length == 0) {
            return Double.NaN;
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        return sorted[(int) Math.min(sorted.length - 1, Math.floor(p * sorted.length))];
    }

    public static double median(double[] values) {
        return percentile(values, 0.5);
    }

    public static double median(Collection<Double> values) {
        return median(values.stream().mapToDouble(Double::doubleValue).toArray());
    }

    public static double[] present(Double[] values) {
        return Arrays.stream(values).filter(Objects::nonNull).mapToDouble(Double::doubleValue).toArray();
    }
}
