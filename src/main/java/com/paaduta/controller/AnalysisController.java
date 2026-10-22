package com.paaduta.controller;

import com.paaduta.dto.AnalysisResponse;
import com.paaduta.dto.ValidationResponse;
import com.paaduta.service.AnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

import static com.paaduta.constants.ApiConstants.STATUS_OK;
import static com.paaduta.constants.ApiConstants.USER_TRACK;

@RestController
@RequestMapping("/api")
class AnalysisController {

    private final AnalysisService service;

    AnalysisController(AnalysisService service) {
        this.service = service;
    }

    @GetMapping("/health")
    Map<String, String> health() {
        return Map.of("status", STATUS_OK);
    }

    @Operation(summary = "Compare a sung attempt with a reference passage",
            description = "Both files are WAV. Audio is processed in memory and never stored. Alignment is automatic; "
                    + "key_shift_semitones confirms the key (0 = same key) and releases pitch moments held back by a detected key mismatch.")
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    AnalysisResponse analyze(@RequestPart("reference_audio") MultipartFile reference,
                             @RequestPart("user_audio") MultipartFile user,
                             @RequestParam(name = "key_shift_semitones", required = false) Integer keyShift) throws IOException {
        return service.analyze(reference.getBytes(), user.getBytes(), keyShift);
    }

    @Operation(summary = "Preflight check of one clip's format, duration and audio quality")
    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ValidationResponse validate(@RequestPart("audio") MultipartFile audio,
                                @RequestParam(defaultValue = USER_TRACK) String track) throws IOException {
        return service.validate(audio.getBytes(), track);
    }
}
