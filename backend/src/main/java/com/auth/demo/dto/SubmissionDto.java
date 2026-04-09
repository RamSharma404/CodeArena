package com.auth.demo.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

public class SubmissionDto {

    // ── Run Code (POST /api/execute) ──

    @Data
    public static class ExecuteRequest {
        private Long problemId;
        private String code;
        private String language;  // JAVA, PYTHON, CPP, JAVASCRIPT
        private String input;     // custom test input
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecuteResponse {
        private String output;    // stdout
        private String error;     // stderr (compile errors, runtime errors)
        private int runtimeMs;
        private String status;    // SUCCESS, COMPILE_ERROR, RUNTIME_ERROR, TIME_LIMIT
    }

    // ── Submit Code (POST /api/submit) ──

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
    public static class SubmitResponse {
        private String status;         // ACCEPTED, WRONG_ANSWER, COMPILE_ERROR, RUNTIME_ERROR, TIME_LIMIT
        private int runtimeMs;
        private int memoryKb;
        private int testCasesPassed;
        private int totalTestCases;
        private Long submissionId;
    }

    // ── Submission History (GET /api/submissions) ──

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionHistory {
        private Long id;
        private Long problemId;
        private String status;
        private String language;
        private String code;
        private int runtimeMs;
        private int memoryKb;
        private LocalDateTime submittedAt;
    }
}
