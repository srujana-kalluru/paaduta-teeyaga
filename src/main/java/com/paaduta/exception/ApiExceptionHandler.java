package com.paaduta.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
class ApiExceptionHandler {

    @ExceptionHandler(AnalysisException.class)
    ProblemDetail handle(AnalysisException exception) {
        var problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        problem.setProperty("code", exception.code());
        problem.setProperty("track", exception.track());
        return problem;
    }
}
