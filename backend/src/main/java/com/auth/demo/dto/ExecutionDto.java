package com.auth.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;

public class ExecutionDto {

    @Data
    public static class RunRequest {
        private Long problemId;
        private String code;
        private String language;
        private String customInput;
    }

    @Data
    public static class SubmitRequest {
        private Long problemId;
        private String code;
        private String language;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunResponse {
        private String status;
        private String output;
        private String expectedOutput;
        private String error;
        private Integer runtimeMs;
        private Integer memoryKb;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitResponse {
        private Long submissionId;
        private String status;
        private Integer runtimeMs;
        private Integer memoryKb;
        private Integer totalTestCases;
        private Integer passedTestCases;
        private String failedInput;
        private String failedExpectedOutput;
        private String failedActualOutput;
        private String error;
    }
}