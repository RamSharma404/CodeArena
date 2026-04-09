package com.auth.demo.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class AIReviewService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper  = new ObjectMapper();

    public CodeReviewResponse reviewCode(String code,
                                         String language,
                                         String problemTitle,
                                         String problemDescription) {
        try {
            String prompt = buildPrompt(code, language,
                    problemTitle, problemDescription);
            String rawResponse = callGroq(prompt);
            return parseResponse(rawResponse);

        } catch (Exception e) {
            return CodeReviewResponse.builder()
                    .timeComplexity("Unable to analyze")
                    .spaceComplexity("Unable to analyze")
                    .whatYouDidWell("Code was accepted successfully")
                    .improvements("AI review temporarily unavailable: "
                            + e.getMessage())
                    .alternativeApproach("N/A")
                    .overallRating("Accepted")
                    .build();
        }
    }

    private String buildPrompt(String code, String language,
                               String problemTitle,
                               String problemDescription) {
        return """
            You are a senior software engineer reviewing a coding solution.

            Problem: %s
            Description: %s
            Language: %s

            User's Solution:
            %s
            Analyze this solution and respond ONLY with a valid JSON object
            (no markdown, no extra text) in exactly this format:
            {
                "timeComplexity": "O(?) - brief explanation",
                "spaceComplexity": "O(?) - brief explanation",
                "whatYouDidWell": "specific positive feedback",
                "improvements": "specific suggestions",
                "alternativeApproach": "brief different approach",
                "overallRating": "Excellent / Good / Acceptable / Needs Improvement"
            }

            Be specific to the actual code. Keep each field under 100 words.
            """.formatted(problemTitle, problemDescription, language, code);
    }

    private String callGroq(String prompt) {
        // Build messages array
        Map<String, String> message = new HashMap<>();
        message.put("role",    "user");
        message.put("content", prompt);

        // Build request body
        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.1-8b-instant");  // free model on Groq
        body.put("messages", List.of(message));
        body.put("temperature", 0.3);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = restTemplate.postForEntity(
                apiUrl, request, Map.class
        );

        // Extract text from Groq response
        // Groq follows OpenAI format
        Map responseBody = response.getBody();
        List choices    = (List) responseBody.get("choices");
        Map firstChoice = (Map) choices.get(0);
        Map messageMap  = (Map) firstChoice.get("message");

        return (String) messageMap.get("content");
    }

    private CodeReviewResponse parseResponse(String rawResponse) {
        try {
            String cleaned = rawResponse
                    .replace("```json", "")
                    .replace("```",     "")
                    .trim();

            // Find JSON object in response
            int start = cleaned.indexOf("{");
            int end   = cleaned.lastIndexOf("}");
            if (start != -1 && end != -1) {
                cleaned = cleaned.substring(start, end + 1);
            }

            return objectMapper.readValue(cleaned, CodeReviewResponse.class);

        } catch (Exception e) {
            return CodeReviewResponse.builder()
                    .timeComplexity("See review")
                    .spaceComplexity("See review")
                    .whatYouDidWell(rawResponse)
                    .improvements("N/A")
                    .alternativeApproach("N/A")
                    .overallRating("Reviewed")
                    .build();
        }
    }

    @Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class CodeReviewResponse {

        @JsonProperty("timeComplexity")
        private String timeComplexity;

        @JsonProperty("spaceComplexity")
        private String spaceComplexity;

        @JsonProperty("whatYouDidWell")
        private String whatYouDidWell;

        @JsonProperty("improvements")
        private String improvements;

        @JsonProperty("alternativeApproach")
        private String alternativeApproach;

        @JsonProperty("overallRating")
        private String overallRating;
    }
}