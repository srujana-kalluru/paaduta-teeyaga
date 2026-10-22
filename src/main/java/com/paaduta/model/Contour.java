package com.paaduta.model;

import java.util.Arrays;
import java.util.Objects;

public record Contour(double seconds, Double[] f0, double[] probability, double[] db) {

    public int frames() {
        return f0.length;
    }

    public long voicedFrames() {
        return Arrays.stream(f0).filter(Objects::nonNull).count();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Contour c && Double.compare(seconds, c.seconds) == 0 && Arrays.equals(f0, c.f0)
                && Arrays.equals(probability, c.probability) && Arrays.equals(db, c.db);
    }

    @Override
    public int hashCode() {
        return Objects.hash(seconds, Arrays.hashCode(f0), Arrays.hashCode(probability), Arrays.hashCode(db));
    }

    @Override
    public String toString() {
        return "Contour[seconds=" + seconds + ", frames=" + f0.length + "]";
    }
}
