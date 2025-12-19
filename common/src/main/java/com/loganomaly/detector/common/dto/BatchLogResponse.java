package com.loganomaly.detector.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchLogResponse {
    private int acceptedCount;
    private int failedCount;
    private List<String> acceptedIds;
    private Instant timestamp;
}

