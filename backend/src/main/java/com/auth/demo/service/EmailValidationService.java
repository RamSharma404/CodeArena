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

            // Only block disposable emails
            // Removed deliverability check — it rejects valid college/work emails
            if (response != null
                    && response.getIsDisposableEmail() != null
                    && response.getIsDisposableEmail().getValue()) {
                throw new RuntimeException(
                        "Disposable/temporary email addresses are not allowed."
                );
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            // If API is down — allow the email
            System.out.println("Email validation API unavailable: " + e.getMessage());
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