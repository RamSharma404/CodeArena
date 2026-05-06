package com.auth.demo.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ExecutionDto {

    // ─── Run Code Request ─────────────────────────────────────
    @Data
    public static class RunRequest {
        private Long   problemId;
        private String code;
        private String language;
        private String customInput;
    }

    // ─── Submit Request ───────────────────────────────────────
    @Data
    public static class SubmitRequest {
        private Long   problemId;
        private String code;
        private String language;
    }

    // ─── Per-Test-Case Result (used in RunResponse) ───────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseResult {
        private int     testCaseIndex;
        private String  input;
        private String  expectedOutput;
        private String  actualOutput;
        private boolean passed;
        private String  error;
    }

    // ─── Run Response ─────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RunResponse {
        private String  status;          // overall: ACCEPTED, WRONG_ANSWER, etc.
        private String  error;           // compile error or general error
        private Integer runtimeMs;
        private Integer passedCount;
        private Integer totalCount;
        private List<TestCaseResult> testCaseResults;
    }

    // ─── Submit Response (returned immediately) ───────────────
    // Just tells frontend the submission was queued
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitResponse {
        private Long   submissionId;
        private String status;      // always PENDING initially
        private String message;
    }

    // ─── Submission Result (returned when polling) ────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionResult {
        private Long    submissionId;
        private Long    problemId;
        private String  status;
        private String  code;
        private String  language;
        private Integer runtimeMs;
        private Integer totalTestCases;
        private Integer passedTestCases;
        private String  failedInput;
        private String  failedExpectedOutput;
        private String  failedActualOutput;
        private String  error;
        private AIReview aiReview;
        private List<SubmissionHistoryItem> submissionHistory;
        private Map<String, Object> ranking;
    }

    // ─── AI Review ────────────────────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AIReview {
        private String timeComplexity;
        private String spaceComplexity;
        private String whatYouDidWell;
        private String improvements;
        private String alternativeApproach;
        private String overallRating;
    }

    // ─── Submission History Item ──────────────────────────────
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmissionHistoryItem {
        private Long          id;
        private String        status;
        private String        language;
        private Integer       runtimeMs;
        private LocalDateTime submittedAt;
    }
}