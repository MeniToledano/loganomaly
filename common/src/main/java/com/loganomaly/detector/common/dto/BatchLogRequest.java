package com.loganomaly.detector.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchLogRequest {
    
    @NotEmpty(message = "Logs list cannot be empty")
    @Size(max = 1000, message = "Batch cannot exceed 1000 log events")
    @Valid
    private List<LogEventRequest> logs;
}

