package com.paaduta.constants;

import com.paaduta.dto.Notice;

import java.util.Locale;

import static com.paaduta.constants.ApiConstants.REFERENCE_TRACK;

public enum Warning {
    TOO_SHORT("%s is under 1 second."),
    LOW_VOLUME("%s is very quiet. Move closer to the microphone."),
    MOSTLY_SILENT("%s is mostly silence."),
    NOISY_AUDIO("%s is noisy, so pitch estimates are less reliable."),
    POSSIBLE_DENSE_MIX("The reference has background music or several voices, so pitch estimates are less reliable."),
    NO_RELIABLE_PITCH("%s has too little clear singing to compare."),
    WEAK_ALIGNMENT("The recordings didn't line up reliably, so nothing is flagged. Record both again, singing the same phrase."),
    LIKELY_OCTAVE_ERROR("Some octave jumps were ignored as likely tracking errors."),
    POSSIBLE_KEY_MISMATCH("The attempt is about %2$s the reference.");

    private final String template;

    Warning(String template) {
        this.template = template;
    }

    public Notice notice(String track) {
        return notice(track, null);
    }

    public Notice notice(String track, Object detail) {
        String subject = "";
        if (track != null) {
            subject = REFERENCE_TRACK.equals(track) ? "The reference" : "The attempt";
        }
        return new Notice(name().toLowerCase(Locale.ROOT), track, template.formatted(subject, detail));
    }
}
