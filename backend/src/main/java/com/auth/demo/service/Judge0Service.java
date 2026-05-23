package com.auth.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
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

    private final RestTemplate restTemplate;

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

    public Judge0Service() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
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

            ResponseEntity<String> rawResponse = restTemplate.postForEntity(
                    apiUrl, request, String.class
            );
            
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            OnlineCompilerResponse result = mapper.readValue(rawResponse.getBody(), OnlineCompilerResponse.class);

            PistonResult pistonResult    = new PistonResult();
            PistonResult.RunResult run   = new PistonResult.RunResult();

            if (result != null) {
                String err = result.getError();
                // ─── LOCAL FALLBACK FOR SYNTAX ERRORS ───
                // The external API masks Java/Python compilation errors with a generic string.
                // We run a local syntax check to extract the real error.
                if (err != null && err.contains("Internal error: code execution failed")) {
                    String localErr = getLocalSyntaxError(code, language);
                    if (localErr != null && !localErr.trim().isEmpty()) {
                        err = localErr;
                        result.setExitCode(1); // Force compile error
                    }
                }

                run.setStdout(result.getOutput());
                run.setStderr(err);
                run.setCode(result.getExitCode() != null ? result.getExitCode() : 0);
                if ("timeout".equalsIgnoreCase(result.getStatus())) {
                    run.setSignal("SIGKILL");
                }
                // Parse actual execution time from API (in seconds, e.g. "1.8322")
                if (result.getTime() != null && !result.getTime().isEmpty()) {
                    try {
                        run.setExecutionTimeSeconds(Double.parseDouble(result.getTime()));
                    } catch (NumberFormatException ignored) {}
                }
            }

            pistonResult.setRun(run);
            return pistonResult;

        } catch (HttpStatusCodeException e) {
            String responseBody = e.getResponseBodyAsString();
            System.err.println("[Judge0Service] HTTP " + e.getStatusCode() + " for language=" + language
                    + " compiler=" + COMPILER_MAP.get(language.toLowerCase()) + " response=" + responseBody);
            System.err.println("[Judge0Service] Code snippet (first 500 chars): "
                    + (code != null ? code.substring(0, Math.min(code.length(), 500)) : "null"));
            PistonResult errorResult = new PistonResult();
            PistonResult.RunResult errorRun = new PistonResult.RunResult();
            String detail = responseBody != null && !responseBody.isEmpty() ? responseBody : e.getMessage();
            errorRun.setStderr("Code execution failed: " + e.getStatusCode() + " - " + detail);
            errorRun.setCode(1);
            errorResult.setRun(errorRun);
            return errorResult;

        } catch (Exception e) {
            System.err.println("[Judge0Service] Execution error for language=" + language + ": " + e.getMessage());
            e.printStackTrace();
            PistonResult errorResult = new PistonResult();
            PistonResult.RunResult errorRun = new PistonResult.RunResult();
            errorRun.setStderr("Code execution failed: " + e.getMessage());
            errorRun.setCode(1);
            errorResult.setRun(errorRun);
            return errorResult;
        }
    }

    public String mapStatus(PistonResult result) {
        if (result == null || result.getRun() == null) return "RUNTIME_ERROR";
        PistonResult.RunResult run = result.getRun();
        if ("SIGKILL".equals(run.getSignal()))             return "TIME_LIMIT";
        if (run.getCode() == 0)                            return "ACCEPTED";
        if (run.getStderr() != null
                && !run.getStderr().isEmpty())                 return "COMPILE_ERROR";
        return "RUNTIME_ERROR";
    }

    private String getLocalSyntaxError(String code, String language) {
        if (code == null) return null;
        language = language.toLowerCase();
        try {
            java.nio.file.Path tempDir = java.nio.file.Files.createTempDirectory("lc_compile");
            java.io.File dir = tempDir.toFile();
            
            if ("java".equals(language)) {
                java.io.File sourceFile = new java.io.File(dir, "Main.java");
                java.nio.file.Files.writeString(sourceFile.toPath(), code);
                Process p = new ProcessBuilder("javac", "Main.java")
                        .directory(dir)
                        .start();
                p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                String err = new String(p.getErrorStream().readAllBytes());
                // Clean up absolute paths from javac output
                err = err.replace(sourceFile.getAbsolutePath(), "Main.java");
                return err;
            } else if ("python".equals(language)) {
                java.io.File sourceFile = new java.io.File(dir, "solution.py");
                java.nio.file.Files.writeString(sourceFile.toPath(), code);
                Process p = new ProcessBuilder("python", "-m", "py_compile", "solution.py")
                        .directory(dir)
                        .start();
                p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                String err = new String(p.getErrorStream().readAllBytes());
                err = err.replace(sourceFile.getAbsolutePath(), "solution.py");
                return err;
            }
        } catch (Exception e) {
            System.err.println("[Judge0Service] Local syntax check failed: " + e.getMessage());
        }
        return null;
    }

    @Data
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
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
            private double executionTimeSeconds; // actual CPU time from the sandbox
        }
    }
}