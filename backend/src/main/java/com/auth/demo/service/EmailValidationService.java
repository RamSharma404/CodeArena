package com.auth.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Service
public class EmailValidationService {

    @Value("${abstract.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public void validateEmail(String email) {
        try {
            String url = "https://emailvalidation.abstractapi.com/v1/?api_key="
                    + apiKey + "&email=" + email;

            EmailValidationResponse response = restTemplate.getForObject(
                    url, EmailValidationResponse.class
            );

            // Strict validation: Reject disposable AND undeliverable emails
            if (response != null) {
                if (response.getIsDisposableEmail() != null && response.getIsDisposableEmail().getValue()) {
                    throw new IllegalArgumentException("Disposable/temporary email addresses are not allowed.");
                }
                
                // Note: deliverability is a String, e.g., "DELIVERABLE", "UNDELIVERABLE", "UNKNOWN"
                if (response.getDeliverability() != null && "UNDELIVERABLE".equalsIgnoreCase(response.getDeliverability())) {
                    throw new IllegalArgumentException("Email address does not exist or is undeliverable.");
                }
            } else {
                throw new RuntimeException("Email validation failed. No response from provider.");
            }

        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e.getMessage());
        } catch (Exception e) {
            // Strictly block if API is down or key is invalid
            System.err.println("Email validation API error: " + e.getMessage());
            throw new RuntimeException("Email validation service is currently unavailable. Please try again later or use Google Login.");
        }
    }

    @Data
    public static class EmailValidationResponse {

        @JsonProperty("deliverability")
        private String deliverability;

        @JsonProperty("is_disposable_email")
        private BooleanField isDisposableEmail;

        @JsonProperty("is_valid_format")
        private BooleanField isValidFormat;
    }

    @Data
    public static class BooleanField {
        @JsonProperty("value")
        private Boolean value;
    }
}