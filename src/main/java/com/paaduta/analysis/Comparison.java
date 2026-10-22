package com.paaduta.analysis;

import com.paaduta.config.AnalysisProperties;
import com.paaduta.model.Alignment;
import com.paaduta.model.ComparisonResult;
import com.paaduta.model.Contour;
import com.paaduta.model.Issue;
import com.paaduta.util.Stats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

import static com.paaduta.constants.AudioConstants.HOP_SECONDS;

public final class Comparison {

    private static final double CENTS_PER_SEMITONE = 100;
    private static final double CENTS_PER_OCTAVE = 1200;
    private static final double OCTAVE_SUSPECT_CENTS = 1000;
    private static final double KEY_SHIFT_MIN_CENTS = 150;
    private static final double SILENT_PROBABILITY = 0.2;
    private static final int MAX_GAP = 3;
    private static final int MAX_STALL = 10;
    private static final int SMOOTHING = 4;
    private static final int CHUNK = 25;
    private static final double CHUNK_MIN_COVERAGE = 0.8;
    private static final double SPREAD_LOW_PERCENTILE = 0.2;
    private static final double SPREAD_HIGH_PERCENTILE = 0.8;
    private static final int ONSET_SILENCE = 8;
    private static final int ONSET_MIN_RUN = 10;
    private static final int ONSET_WINDOW = 37;
    private static final int ONSET_VOICING_FRAMES = 5;
    private static final int TIMING_TAIL = 15;
    private static final String ISSUE_ID_PREFIX = "issue-";
    private static final String PITCH_DEVIATION = "pitch_deviation";
    private static final String TIMING = "timing";
    private static final String MISSED = "missed";
    private static final String EXTRA = "extra";
    private static final String UNSTEADY = "unsteady";
    private static final String FLAT = "flat";
    private static final String SHARP = "sharp";
    private static final String LATE = "late";
    private static final String EARLY = "early";
    private static final String SUSTAINED_DEVIATION = "sustained_deviation";
    private static final String ONSET_OFFSET = "onset_offset";
    private static final String REFERENCE_ONLY = "reference_only";
    private static final String ATTEMPT_ONLY = "attempt_only";
    private static final String PITCH_WAVERING = "pitch_wavering";
    private static final int SECONDS_PLACES = 2;
    private static final int PROBABILITY_PLACES = 2;

    private record Window(int from, int to, double attemptFrom, double attemptTo) {
    }

    private final Contour ref;
    private final Contour user;
    private final int[] map;
    private final int lag;
    private final AnalysisProperties properties;
    private final int n;
    private final int minFrames;
    private final boolean[] matched;
    private final boolean[] excluded;
    private final double[] userProbability;
    private final double[] attemptTime;
    private final List<Issue> candidates = new ArrayList<>();

    private Comparison(Contour ref, Contour user, int[] map, int lag, AnalysisProperties properties) {
        this.ref = ref;
        this.user = user;
        this.map = map;
        this.lag = lag;
        this.properties = properties;
        this.n = ref.frames();
        this.minFrames = (int) Math.ceil(properties.minIssueSeconds() / HOP_SECONDS);
        this.matched = new boolean[n];
        this.excluded = new boolean[n];
        this.userProbability = new double[n];
        this.attemptTime = new double[n];
        boolean[] stalled = stalls(map);
        for (int i = 0; i < n; i++) {
            attemptTime[i] = Stats.round(Math.clamp(map[i], 0, user.frames() - 1) * HOP_SECONDS, SECONDS_PLACES);
            matched[i] = inside(map[i]) && !stalled[i];
            if (matched[i]) {
                userProbability[i] = user.probability()[map[i]];
            }
        }
    }

    public static double cents(double hz, double referenceHz) {
        return CENTS_PER_OCTAVE * Math.log(hz / referenceHz) / Math.log(2);
    }

    public static ComparisonResult compare(Contour ref, Contour user, Alignment alignment, Integer keyShift, AnalysisProperties properties) {
        int lag = (int) Math.round(alignment.offsetSeconds() / HOP_SECONDS);
        int[] diagonal = IntStream.range(0, ref.frames()).map(i -> i + lag).toArray();
        int detected = detectKeyShift(ref, user, diagonal);
        return new Comparison(ref, user, Warp.path(ref, user, lag, detected), lag, properties).run(keyShift, detected);
    }

    private ComparisonResult run(Integer keyShift, int detected) {
        int applied = keyShift == null ? 0 : keyShift;
        boolean pitchHeld = keyShift == null && detected != 0;
        var userF0 = new Double[n];
        var deviation = new Double[n];
        int compared = 0;
        int octave = 0;
        for (int i = 0; i < n; i++) {
            userF0[i] = matched[i] ? user.f0()[map[i]] : null;
            if (userF0[i] != null && ref.f0()[i] != null) {
                double cents = cents(userF0[i], ref.f0()[i]);
                if (Math.abs(cents - CENTS_PER_SEMITONE * detected) > OCTAVE_SUSPECT_CENTS) {
                    octave++;
                } else {
                    deviation[i] = cents - CENTS_PER_SEMITONE * applied;
                    compared++;
                }
            }
        }
        timingIssues();
        missedIssues();
        extraIssues();
        if (!pitchHeld) {
            var smooth = smooth(deviation);
            pitchIssues(smooth, deviation);
            unsteadyIssues(smooth);
        }
        return new ComparisonResult(Arrays.asList(userF0), Stats.boxed(userProbability), Arrays.asList(deviation), Stats.boxed(attemptTime),
                kept(), detected, octave, compared, pitchHeld);
    }

    private List<Issue> kept() {
        var kept = candidates.stream()
                .filter(c -> c.confidence() >= properties.minIssueConfidence())
                .sorted(Comparator.comparingDouble(Issue::confidence).reversed())
                .limit(properties.maxIssues())
                .sorted(Comparator.comparingDouble(Issue::startSeconds))
                .toList();
        return IntStream.range(0, kept.size()).mapToObj(k -> kept.get(k).withId(ISSUE_ID_PREFIX + (k + 1))).toList();
    }

    private static int detectKeyShift(Contour ref, Contour user, int[] map) {
        double median = Stats.median(IntStream.range(0, map.length)
                .filter(i -> map[i] >= 0 && map[i] < user.frames() && ref.f0()[i] != null && user.f0()[map[i]] != null)
                .mapToDouble(i -> cents(user.f0()[map[i]], ref.f0()[i])).toArray());
        return Math.abs(median) >= KEY_SHIFT_MIN_CENTS ? (int) Math.round(median / CENTS_PER_SEMITONE) : 0;
    }

    private static boolean[] stalls(int[] map) {
        var stalled = new boolean[map.length];
        int i = 0;
        while (i < map.length) {
            int end = i;
            while (end + 1 < map.length && map[end + 1] == map[i]) {
                end++;
            }
            if (end - i >= MAX_STALL) {
                Arrays.fill(stalled, i, end + 1, true);
            }
            i = end + 1;
        }
        return stalled;
    }

    private void timingIssues() {
        var pairs = pairEntries();
        for (int k = 0; k < pairs.size(); k++) {
            int r = pairs.get(k)[0];
            int u = pairs.get(k)[1];
            int shift = u - r - baseline(pairs, k);
            if (Math.abs(shift * HOP_SECONDS) >= properties.timingSeconds()) {
                int from = Math.max(0, Math.min(r, r + shift));
                int to = Math.min(n, Math.max(r, r + shift) + TIMING_TAIL);
                Arrays.fill(excluded, from, to, true);
                if (k > 0) {
                    var window = new Window(from, to, Math.max(0, Math.min(u, u - shift)) * HOP_SECONDS, (Math.max(u, u - shift) + TIMING_TAIL) * HOP_SECONDS);
                    double confidence = Math.min(mean(ref.probability(), r, ONSET_VOICING_FRAMES), mean(user.probability(), u, ONSET_VOICING_FRAMES));
                    candidates.add(issue(window, TIMING, shift > 0 ? LATE : EARLY, null, shift * HOP_SECONDS, confidence, ONSET_OFFSET));
                }
            }
        }
    }

    private List<int[]> pairEntries() {
        var userOnsets = onsets(user.f0());
        var pairs = new ArrayList<int[]>();
        for (int r : onsets(ref.f0())) {
            int target = map[r];
            userOnsets.stream()
                    .filter(u -> Math.abs(u - target) <= ONSET_WINDOW)
                    .min(Comparator.comparingInt(u -> Math.abs(u - target)))
                    .ifPresent(u -> addPair(pairs, r, u));
        }
        return pairs;
    }

    private void addPair(List<int[]> pairs, int r, int u) {
        if (!pairs.isEmpty() && pairs.getLast()[1] == u) {
            if (Math.abs(u - map[pairs.getLast()[0]]) <= Math.abs(u - map[r])) {
                return;
            }
            pairs.removeLast();
        }
        pairs.add(new int[]{r, u});
    }

    private int baseline(List<int[]> pairs, int k) {
        int offset = pairs.get(k)[1] - pairs.get(k)[0];
        int baseline = lag;
        for (int q : new int[]{k - 1, k + 1}) {
            if (q >= 0 && q < pairs.size()) {
                int neighbour = pairs.get(q)[1] - pairs.get(q)[0];
                if (Math.abs(offset - neighbour) < Math.abs(offset - baseline)) {
                    baseline = neighbour;
                }
            }
        }
        return baseline;
    }

    private void missedIssues() {
        IntPredicate silent = i -> !excluded[i] && ref.f0()[i] != null && userProbability[i] < SILENT_PROBABILITY;
        for (int[] run : runs(n, silent)) {
            double confidence = mean(ref.probability(), run[0], span(run)) * coverage(run);
            candidates.add(issue(window(run), MISSED, null, null, null, confidence, REFERENCE_ONLY));
        }
    }

    private void extraIssues() {
        int m = user.frames();
        var alignedToPitch = new boolean[m];
        for (int i = 0; i < n; i++) {
            if (ref.f0()[i] != null && inside(map[i])) {
                int next = i + 1 < n && map[i + 1] > map[i] ? map[i + 1] : map[i] + 1;
                Arrays.fill(alignedToPitch, map[i], Math.min(next, m), true);
            }
        }
        for (int[] run : runs(m, j -> user.f0()[j] != null && !alignedToPitch[j])) {
            int at = referenceFrameAt(run[0] * HOP_SECONDS);
            double confidence = mean(user.probability(), run[0], span(run)) * coverage(run);
            var window = new Window(at, at + 1, run[0] * HOP_SECONDS, (run[1] + 1) * HOP_SECONDS);
            candidates.add(issue(window, EXTRA, null, null, null, confidence, ATTEMPT_ONLY));
        }
    }

    private void pitchIssues(Double[] smooth, Double[] deviation) {
        for (int sign : new int[]{-1, 1}) {
            for (int[] run : runs(n, i -> smooth[i] != null && smooth[i] * sign >= properties.pitchCents())) {
                var values = IntStream.rangeClosed(run[0], run[1])
                        .filter(i -> deviation[i] != null && !excluded[i])
                        .mapToObj(i -> deviation[i])
                        .toList();
                double cents = Math.round(Stats.median(values));
                candidates.add(issue(window(run), PITCH_DEVIATION, sign < 0 ? FLAT : SHARP, cents, null,
                        voicing(run) * coverage(run), SUSTAINED_DEVIATION));
            }
        }
    }

    private void unsteadyIssues(Double[] smooth) {
        var wavering = new boolean[n];
        for (int start = 0; start + CHUNK <= n; start += CHUNK) {
            if (wavers(Stats.present(Arrays.copyOfRange(smooth, start, start + CHUNK)))) {
                Arrays.fill(wavering, start, start + CHUNK, true);
            }
        }
        for (int[] run : runs(n, i -> wavering[i] && smooth[i] != null)) {
            candidates.add(issue(window(run), UNSTEADY, null, null, null, voicing(run), PITCH_WAVERING));
        }
    }

    private boolean wavers(double[] chunk) {
        return chunk.length >= CHUNK_MIN_COVERAGE * CHUNK
                && Math.abs(Stats.median(chunk)) < properties.pitchCents()
                && Stats.percentile(chunk, SPREAD_HIGH_PERCENTILE) - Stats.percentile(chunk, SPREAD_LOW_PERCENTILE) >= properties.unsteadyCents();
    }

    private Double[] smooth(Double[] deviation) {
        var out = new Double[n];
        for (int i = 0; i < n; i++) {
            if (deviation[i] != null && !excluded[i]) {
                var window = new ArrayList<Double>();
                for (int k = Math.max(0, i - SMOOTHING); k <= Math.min(n - 1, i + SMOOTHING); k++) {
                    if (deviation[k] != null && !excluded[k]) {
                        window.add(deviation[k]);
                    }
                }
                out[i] = Stats.median(window);
            }
        }
        return out;
    }

    private List<int[]> runs(int length, IntPredicate member) {
        var out = new ArrayList<int[]>();
        int i = 0;
        while (i < length) {
            if (member.test(i)) {
                int[] run = extend(length, member, i);
                if (span(run) >= minFrames) {
                    out.add(run);
                }
                i = run[1] + 1;
            } else {
                i++;
            }
        }
        return out;
    }

    private static int[] extend(int length, IntPredicate member, int start) {
        int end = start;
        int count = 1;
        int gap = 0;
        for (int k = start + 1; k < length && gap <= MAX_GAP; k++) {
            if (member.test(k)) {
                end = k;
                count++;
                gap = 0;
            } else {
                gap++;
            }
        }
        return new int[]{start, end, count};
    }

    private static List<Integer> onsets(Double[] f0) {
        var out = new ArrayList<Integer>();
        int silent = 0;
        for (int i = 0; i < f0.length; i++) {
            if (f0[i] == null) {
                silent++;
            } else {
                if (silent >= ONSET_SILENCE && voicedRun(f0, i) >= ONSET_MIN_RUN) {
                    out.add(i);
                }
                silent = 0;
            }
        }
        return out;
    }

    private static int voicedRun(Double[] f0, int start) {
        int end = start;
        while (end < f0.length && f0[end] != null) {
            end++;
        }
        return end - start;
    }

    private Window window(int[] run) {
        return new Window(run[0], run[1] + 1, attemptTime[run[0]], attemptTime[run[1]] + HOP_SECONDS);
    }

    private static Issue issue(Window w, String type, String direction, Double cents, Double onset, double confidence, String evidence) {
        return new Issue(null, Stats.round(w.from() * HOP_SECONDS, SECONDS_PLACES), Stats.round(w.to() * HOP_SECONDS, SECONDS_PLACES),
                Stats.round(w.attemptFrom(), SECONDS_PLACES), Stats.round(w.attemptTo(), SECONDS_PLACES), type, direction, cents,
                onset == null ? null : Stats.round(onset, SECONDS_PLACES), Stats.round(confidence, PROBABILITY_PLACES), evidence);
    }

    private boolean inside(int j) {
        return j >= 0 && j < user.frames();
    }

    private int referenceFrameAt(double attemptSeconds) {
        return IntStream.range(0, n).filter(i -> attemptTime[i] >= attemptSeconds).findFirst().orElse(n - 1);
    }

    private double voicing(int[] run) {
        return IntStream.rangeClosed(run[0], run[1]).mapToDouble(i -> Math.min(ref.probability()[i], userProbability[i])).average().orElse(0);
    }

    private static int span(int[] run) {
        return run[1] - run[0] + 1;
    }

    private static double coverage(int[] run) {
        return (double) run[2] / span(run);
    }

    private static double mean(double[] values, int from, int count) {
        int to = Math.min(values.length, from + count);
        return to <= from ? 0 : Arrays.stream(values, from, to).average().orElse(0);
    }
}
