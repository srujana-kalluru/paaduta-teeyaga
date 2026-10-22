package com.paaduta.constants;

import java.util.Locale;

public enum ErrorCode {
    UNSUPPORTED_FORMAT("This is not a WAV file the service can read."),
    CORRUPT_AUDIO("This file looks damaged or incomplete."),
    EMPTY_AUDIO("This file contains no usable audio."),
    TOO_LONG("Clips must be 3 minutes or shorter.");

    private final String message;

    ErrorCode(String message) {
        this.message = message;
    }

    public String code() {
        return name().toLowerCase(Locale.ROOT);
    }

    public String message() {
        return message;
    }
}
