package com.loganomaly.detector.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    
    @NotBlank(message = "Log level is required")
    @Pattern(regexp = "^(TRACE|DEBUG|INFO|WARN|ERROR|FATAL)$", message = "Invalid log level")
    private String level;
    
    @NotBlank(message = "Message is required")
    @Size(max = 65536, message = "Message cannot exceed 64KB")
    private String message;
    
    @NotBlank(message = "Service name is required")
    @Size(max = 100, message = "Service name cannot exceed 100 characters")
    private String service;
    
    private Map<String, String> metadata;
}

