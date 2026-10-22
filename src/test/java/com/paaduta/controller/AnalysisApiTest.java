package com.paaduta.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paaduta.support.Synth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalysisApiTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private static MockMultipartFile part(String name, float[] samples) {
        return new MockMultipartFile(name, name + ".wav", "audio/wav", Synth.wav(samples));
    }

    private JsonNode analyze(float[] reference, float[] user) throws Exception {
        var body = mvc.perform(multipart("/api/analyze").file(part("reference_audio", reference)).file(part("user_audio", user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }

    private static List<String> codes(JsonNode warnings) {
        var out = new ArrayList<String>();
        warnings.forEach(w -> out.add(w.get("code").asText()));
        return out;
    }

    @Test
    void healthCheck() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
    }

    @Test
    void allowsGitHubPagesAndLocalOriginsOnly() throws Exception {
        mvc.perform(get("/api/health").header("Origin", "https://someone.github.io"))
                .andExpect(header().string("Access-Control-Allow-Origin", "https://someone.github.io"));
        mvc.perform(get("/api/health").header("Origin", "http://localhost:5500"))
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5500"));
        mvc.perform(get("/api/health").header("Origin", "https://example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    void findsFlatNoteAndLateEntryInOffsetMelody() throws Exception {
        double[] cents = new double[Synth.NOTES];
        double[] late = new double[Synth.NOTES];
        cents[4] = -80;
        cents[9] = 25;
        late[7] = 0.35;

        var result = analyze(Synth.scale(0.3), Synth.scale(0.8, 1, cents, late));

        assertEquals("ok", result.get("status").asText());
        assertEquals(0.5, result.at("/alignment/offset_seconds").asDouble(), 0.04);
        assertEquals("high", result.at("/summary/pitch_comparison_confidence").asText());
        var issues = result.get("issues");
        assertEquals(2, issues.size());
        var flat = issues.get(0);
        assertEquals("pitch_deviation", flat.get("type").asText());
        assertEquals("flat", flat.get("direction").asText());
        assertEquals(-80, flat.get("median_deviation_cents").asDouble(), 10);
        assertEquals(4.3, flat.get("start_seconds").asDouble(), 0.15);
        var timing = issues.get(1);
        assertEquals("timing", timing.get("type").asText());
        assertEquals("late", timing.get("direction").asText());
        assertEquals(0.35, timing.get("onset_difference_seconds").asDouble(), 0.04);
        assertTrue(result.at("/contours/user_f0_hz/0").isNull());
        assertTrue(result.get("warnings").isEmpty());
    }

    @Test
    void analyzesShortLiveStyleClips() throws Exception {
        var reference = Synth.notes(1.6, new double[]{220, 0.3, 1.2});
        var attempt = Synth.notes(1.6, new double[]{Synth.shift(220, -80), 0.4, 1.1});

        var result = analyze(reference, attempt);

        assertEquals("ok", result.get("status").asText());
        assertEquals(0.1, result.at("/alignment/offset_seconds").asDouble(), 0.04);
        assertEquals(1, result.get("issues").size());
        assertEquals("flat", result.at("/issues/0/direction").asText());
        assertTrue(result.get("warnings").isEmpty());
    }

    @Test
    void warnsAboutShortAndSilentClips() throws Exception {
        var result = analyze(Synth.tone(220, 0.8), new float[Synth.RATE * 2]);

        assertEquals("insufficient", result.get("status").asText());
        assertTrue(result.get("issues").isEmpty());
        assertTrue(codes(result.get("warnings")).containsAll(List.of("too_short", "mostly_silent", "no_reliable_pitch")));
    }

    @Test
    void rejectsUnsupportedFormatWithProblemDetail() throws Exception {
        var notAudio = new MockMultipartFile("reference_audio", "notes.txt", "text/plain", "hello".getBytes());

        mvc.perform(multipart("/api/analyze").file(notAudio).file(part("user_audio", Synth.tone(220, 3))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("unsupported_format"))
                .andExpect(jsonPath("$.track").value("reference"));
    }

    @Test
    void validatesSingleClip() throws Exception {
        mvc.perform(multipart("/api/validate").file(part("audio", new float[Synth.RATE * 4])).param("track", "user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duration_seconds").value(4.0))
                .andExpect(jsonPath("$.warnings[*].code", hasItems("mostly_silent", "no_reliable_pitch")));
    }
}
