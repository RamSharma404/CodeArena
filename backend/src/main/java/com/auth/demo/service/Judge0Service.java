package com.auth.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.HashMap;
import java.util.Map;

@Service
public class Judge0Service {

    @Value("${onlinecompiler.api.key}")
    private String apiKey;

    @Value("${onlinecompiler.api.url}")
    private String apiUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Map<String, String> COMPILER_MAP = new HashMap<>();
    static {
        COMPILER_MAP.put("python",     "python-3.14");
        COMPILER_MAP.put("java",       "openjdk-25");
        COMPILER_MAP.put("cpp",        "g++-15");
        COMPILER_MAP.put("c",          "gcc-15");
        COMPILER_MAP.put("javascript", "typescript-deno");
        COMPILER_MAP.put("typescript", "typescript-deno");
        COMPILER_MAP.put("go",         "go-1.26");
        COMPILER_MAP.put("rust",       "rust-1.93");
    }

    public PistonResult executeCode(String code, String language, String input) {
        try {
            String compiler = COMPILER_MAP.get(language.toLowerCase());
            if (compiler == null)
                throw new RuntimeException("Unsupported language: " + language);

            Map<String, Object> body = new HashMap<>();
            body.put("compiler", compiler);
            body.put("code",     code);
            body.put("input",    input != null ? input : "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<OnlineCompilerResponse> response = restTemplate.postForEntity(
                    apiUrl, request, OnlineCompilerResponse.class
            );

            OnlineCompilerResponse result = response.getBody();

            PistonResult pistonResult    = new PistonResult();
            PistonResult.RunResult run   = new PistonResult.RunResult();

            if (result != null) {
                run.setStdout(result.getOutput());
                run.setStderr(result.getError());
                run.setCode(result.getExitCode() != null ? result.getExitCode() : 0);
                if ("timeout".equalsIgnoreCase(result.getStatus())) {
                    run.setSignal("SIGKILL");
                }
            }

            pistonResult.setRun(run);
            return pistonResult;

        } catch (Exception e) {
            throw new RuntimeException("Code execution failed: " + e.getMessage());
        }
    }

    public String mapStatus(PistonResult result) {
        if (result == null || result.getRun() == null) return "RUNTIME_ERROR";
        PistonResult.RunResult run = result.getRun();
        if ("SIGKILL".equals(run.getSignal()))             return "TIME_LIMIT";
        if (run.getCode() == 0)                            return "ACCEPTED";
        if (run.getStderr() != null
                && !run.getStderr().isEmpty())                 return "RUNTIME_ERROR";
        return "WRONG_ANSWER";
    }

    @Data
    public static class OnlineCompilerResponse {
        private String output;
        private String error;
        private String status;

        @JsonProperty("exit_code")
        private Integer exitCode;

        private String time;
        private String memory;
    }

    @Data
    public static class PistonResult {
        private String language;
        private String version;
        private RunResult run;
        private RunResult compile;

        @Data
        public static class RunResult {
            private String stdout;
            private String stderr;
            private String output;
            private int    code;
            private String signal;
        }
    }
}