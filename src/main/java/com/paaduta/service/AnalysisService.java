package com.paaduta.service;

import com.paaduta.analysis.Aligner;
import com.paaduta.analysis.AudioDecoder;
import com.paaduta.analysis.Comparison;
import com.paaduta.analysis.PitchTracker;
import com.paaduta.config.AnalysisProperties;
import com.paaduta.dto.AnalysisResponse;
import com.paaduta.dto.Contours;
import com.paaduta.dto.Notice;
import com.paaduta.dto.Summary;
import com.paaduta.dto.ValidationResponse;
import com.paaduta.model.Alignment;
import com.paaduta.model.ComparisonResult;
import com.paaduta.model.Contour;
import com.paaduta.model.Issue;
import com.paaduta.util.Stats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static com.paaduta.constants.AnalysisConstants.HIGH_ALIGNMENT;
import static com.paaduta.constants.AnalysisConstants.HIGH_COVERAGE;
import static com.paaduta.constants.AnalysisConstants.LOUD_PERCENTILE;
import static com.paaduta.constants.AnalysisConstants.LOW_COVERAGE;
import static com.paaduta.constants.AnalysisConstants.LOW_VOLUME_DB;
import static com.paaduta.constants.AnalysisConstants.MIN_OCTAVE_SUSPECTS;
import static com.paaduta.constants.AnalysisConstants.MIN_VOICED_FRAMES;
import static com.paaduta.constants.AnalysisConstants.MOSTLY_SILENT_FRACTION;
import static com.paaduta.constants.AnalysisConstants.NOISY_PROBABILITY;
import static com.paaduta.constants.AnalysisConstants.OCTAVE_SUSPECT_RATIO;
import static com.paaduta.constants.AnalysisConstants.SEMITONES_PER_OCTAVE;
import static com.paaduta.constants.AnalysisConstants.TOO_SHORT_SECONDS;
import static com.paaduta.constants.ApiConstants.CENTS_PLACES;
import static com.paaduta.constants.ApiConstants.CONFIDENCE_HIGH;
import static com.paaduta.constants.ApiConstants.CONFIDENCE_LOW;
import static com.paaduta.constants.ApiConstants.CONFIDENCE_MEDIUM;
import static com.paaduta.constants.ApiConstants.HZ_PLACES;
import static com.paaduta.constants.ApiConstants.PROBABILITY_PLACES;
import static com.paaduta.constants.ApiConstants.REFERENCE_TRACK;
import static com.paaduta.constants.ApiConstants.SECONDS_PLACES;
import static com.paaduta.constants.ApiConstants.STATUS_INSUFFICIENT;
import static com.paaduta.constants.ApiConstants.STATUS_OK;
import static com.paaduta.constants.ApiConstants.SUMMARY_PLACES;
import static com.paaduta.constants.ApiConstants.USER_TRACK;
import static com.paaduta.constants.AudioConstants.HOP_SECONDS;
import static com.paaduta.constants.AudioConstants.SILENCE_DB;
import static com.paaduta.constants.Warning.LIKELY_OCTAVE_ERROR;
import static com.paaduta.constants.Warning.LOW_VOLUME;
import static com.paaduta.constants.Warning.MOSTLY_SILENT;
import static com.paaduta.constants.Warning.NOISY_AUDIO;
import static com.paaduta.constants.Warning.NO_RELIABLE_PITCH;
import static com.paaduta.constants.Warning.POSSIBLE_DENSE_MIX;
import static com.paaduta.constants.Warning.POSSIBLE_KEY_MISMATCH;
import static com.paaduta.constants.Warning.TOO_SHORT;
import static com.paaduta.constants.Warning.WEAK_ALIGNMENT;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AnalysisProperties properties;

    public AnalysisService(AnalysisProperties properties) {
        this.properties = properties;
    }

    public ValidationResponse validate(byte[] audio, String track) {
        var contour = PitchTracker.track(AudioDecoder.decode(audio, track));
        return new ValidationResponse(STATUS_OK, Stats.round(contour.seconds(), SECONDS_PLACES), quality(contour, track));
    }

    public AnalysisResponse analyze(byte[] reference, byte[] user, Integer keyShift) {
        long started = System.nanoTime();
        var ref = PitchTracker.track(AudioDecoder.decode(reference, REFERENCE_TRACK));
        var usr = PitchTracker.track(AudioDecoder.decode(user, USER_TRACK));
        var alignment = Aligner.estimate(ref, usr);
        var cmp = Comparison.compare(ref, usr, alignment, keyShift, properties);
        boolean insufficient = ref.voicedFrames() < MIN_VOICED_FRAMES || usr.voicedFrames() < MIN_VOICED_FRAMES;
        boolean weak = alignment.confidence() < properties.minAlignmentConfidence();
        var warnings = warnings(ref, usr, cmp, insufficient, weak);
        var issues = insufficient || weak ? List.<Issue>of() : cmp.issues();
        var summary = new Summary(Stats.round(cmp.comparedFrames() * HOP_SECONDS, SUMMARY_PLACES),
                level(ref, cmp, alignment, insufficient || weak), issues.size());

        log.atInfo()
                .addKeyValue("referenceSeconds", Stats.round(ref.seconds(), SECONDS_PLACES))
                .addKeyValue("userSeconds", Stats.round(usr.seconds(), SECONDS_PLACES))
                .addKeyValue("alignmentMethod", alignment.method())
                .addKeyValue("alignmentConfidence", alignment.confidence())
                .addKeyValue("issues", issues.size())
                .addKeyValue("warnings", warnings.stream().map(Notice::code).toList())
                .addKeyValue("elapsedMs", (System.nanoTime() - started) / 1_000_000)
                .log("Analysis complete");

        return new AnalysisResponse(insufficient ? STATUS_INSUFFICIENT : STATUS_OK, Stats.round(ref.seconds(), SECONDS_PLACES), alignment,
                summary, contours(ref, cmp), issues, warnings, keyShift == null ? 0 : keyShift, cmp.detectedKeyShift());
    }

    private static List<Notice> warnings(Contour ref, Contour usr, ComparisonResult cmp, boolean insufficient, boolean weak) {
        var out = new ArrayList<>(quality(ref, REFERENCE_TRACK));
        out.addAll(quality(usr, USER_TRACK));
        if (!insufficient && weak) {
            out.add(WEAK_ALIGNMENT.notice(null));
        }
        if (cmp.octaveSuspects() >= MIN_OCTAVE_SUSPECTS
                && cmp.octaveSuspects() > OCTAVE_SUSPECT_RATIO * (cmp.comparedFrames() + cmp.octaveSuspects())) {
            out.add(LIKELY_OCTAVE_ERROR.notice(null));
        }
        if (!insufficient && cmp.pitchHeld()) {
            out.add(POSSIBLE_KEY_MISMATCH.notice(null, describeShift(cmp.detectedKeyShift())));
        }
        return out;
    }

    private static String level(Contour ref, ComparisonResult cmp, Alignment alignment, boolean unusable) {
        double coverage = ref.voicedFrames() == 0 ? 0 : (double) cmp.comparedFrames() / ref.voicedFrames();
        if (unusable || coverage < LOW_COVERAGE) {
            return CONFIDENCE_LOW;
        }
        return alignment.confidence() >= HIGH_ALIGNMENT && coverage >= HIGH_COVERAGE ? CONFIDENCE_HIGH : CONFIDENCE_MEDIUM;
    }

    private static Contours contours(Contour ref, ComparisonResult cmp) {
        return new Contours(
                IntStream.range(0, ref.frames()).mapToObj(i -> Stats.round(i * HOP_SECONDS, SECONDS_PLACES)).toList(),
                cmp.attemptTime(),
                Stats.round(Arrays.asList(ref.f0()), HZ_PLACES),
                Stats.round(cmp.userF0(), HZ_PLACES),
                Stats.round(Stats.boxed(ref.probability()), PROBABILITY_PLACES),
                Stats.round(cmp.userProbability(), PROBABILITY_PLACES),
                Stats.round(cmp.deviationCents(), CENTS_PLACES));
    }

    private static List<Notice> quality(Contour c, String track) {
        var out = new ArrayList<Notice>();
        double[] activeProbability = IntStream.range(0, c.frames()).filter(i -> c.db()[i] > SILENCE_DB)
                .mapToDouble(i -> c.probability()[i]).toArray();
        if (c.seconds() < TOO_SHORT_SECONDS) {
            out.add(TOO_SHORT.notice(track));
        }
        if (activeProbability.length < MOSTLY_SILENT_FRACTION * c.frames()) {
            out.add(MOSTLY_SILENT.notice(track));
        } else if (Stats.percentile(c.db(), LOUD_PERCENTILE) < LOW_VOLUME_DB) {
            out.add(LOW_VOLUME.notice(track));
        }
        if (activeProbability.length >= MIN_VOICED_FRAMES && Stats.median(activeProbability) < NOISY_PROBABILITY) {
            out.add((REFERENCE_TRACK.equals(track) ? POSSIBLE_DENSE_MIX : NOISY_AUDIO).notice(track));
        }
        if (c.voicedFrames() < MIN_VOICED_FRAMES) {
            out.add(NO_RELIABLE_PITCH.notice(track));
        }
        return out;
    }

    private static String describeShift(int semitones) {
        int size = Math.abs(semitones);
        String amount = size == SEMITONES_PER_OCTAVE ? "an octave" : size + (size == 1 ? " semitone" : " semitones");
        return amount + (semitones < 0 ? " below" : " above");
    }
}
