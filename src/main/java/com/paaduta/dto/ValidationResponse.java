package com.paaduta.dto;

import java.util.List;

public record ValidationResponse(String status, double durationSeconds, List<Notice> warnings) {
}
