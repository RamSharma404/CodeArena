package com.auth.demo.dto;

import com.auth.demo.model.Problem;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;

public class ProblemDto {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemSummary {
        private Long               id;
        private String             title;
        private String             slug;
        private Problem.Difficulty difficulty;
        private String             topicTags;
        private String             companyTags;
        private String             solvedStatus;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProblemDetail {
        private Long                  id;
        private String                title;
        private String                slug;
        private String                description;
        private Problem.Difficulty    difficulty;
        private String                topicTags;
        private String                companyTags;
        private List<TestCaseDto>     visibleTestCases;
        private LocalDateTime         createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseDto {
        private Long   id;
        private String input;
        private String output;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TemplateResponse {
        private String language;
        private String userTemplate;
    }

    @Data
    public static class TemplateInput {
        private String language;
        private String userTemplate;
        private String driverCode;
    }

    @Data
    public static class TestCaseInput {
        private String  input;
        private String  output;
        private boolean hidden;
    }

    @Data
    public static class CreateProblemRequest {
        private String                title;
        private String                slug;
        private String                description;
        private Problem.Difficulty    difficulty;
        private String                topicTags;
        private String                companyTags;
        private List<TestCaseInput>   testCases;
        private List<TemplateInput>   templates;
    }
}