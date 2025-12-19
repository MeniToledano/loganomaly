package com.loganomaly.detector.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEventRequest {
    private Instant timestamp;
    private String level;
    private String message;
    private String service;
    private Map<String, String> metadata;
}

