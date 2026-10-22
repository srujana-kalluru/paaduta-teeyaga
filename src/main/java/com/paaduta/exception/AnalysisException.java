package com.paaduta.exception;

import com.paaduta.constants.ErrorCode;

public class AnalysisException extends RuntimeException {

    private final ErrorCode error;
    private final String track;

    public AnalysisException(ErrorCode error, String track, Throwable cause) {
        super(error.message(), cause);
        this.error = error;
        this.track = track;
    }

    public AnalysisException(ErrorCode error, String track) {
        this(error, track, null);
    }

    public String code() {
        return error.code();
    }

    public String track() {
        return track;
    }
}
