package com.paaduta.analysis;

import com.paaduta.exception.AnalysisException;
import com.paaduta.support.Synth;
import com.paaduta.util.Stats;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AudioDecoderTest {

    @Test
    void convertsStereo24Bit44kHzToMono16kHz() {
        var wav = Synth.wav(Synth.notes(44_100, 2, new double[]{220, 0, 2}), 44_100, 2, 24);

        var samples = AudioDecoder.decode(wav, "reference");

        assertEquals(2 * Synth.RATE, samples.length, 20);
        assertEquals(220, Stats.median(Stats.present(PitchTracker.track(samples).f0())), 1);
    }

    static Stream<Arguments> invalidAudio() {
        return Stream.of(
                Arguments.of("definitely not audio".getBytes(StandardCharsets.UTF_8), "unsupported_format"),
                Arguments.of(Synth.wav(new float[Synth.RATE * 181]), "too_long"),
                Arguments.of(Synth.wav(new float[100]), "empty_audio"));
    }

    @ParameterizedTest
    @MethodSource("invalidAudio")
    void rejectsInvalidAudioWithACode(byte[] bytes, String code) {
        var error = assertThrows(AnalysisException.class, () -> AudioDecoder.decode(bytes, "user"));

        assertEquals(code, error.code());
        assertEquals("user", error.track());
    }
}
