package com.auth.demo.service;

import com.auth.demo.dto.ExecutionDto;
import com.auth.demo.model.*;
import com.auth.demo.repository.*;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private final SubmissionRepository      submissionRepository;
    private final ProblemRepository         problemRepository;
    private final SubmissionQueueService    queueService;
    private final Judge0Service             judge0Service;
    private final TestCaseRepository        testCaseRepository;
    private final ProblemTemplateRepository templateRepository;
    private final UserProblemRepository     userProblemRepository;
    private final RankingService            rankingService;

    public SubmissionService(
            SubmissionRepository submissionRepository,
            ProblemRepository problemRepository,
            SubmissionQueueService queueService,
            Judge0Service judge0Service,
            TestCaseRepository testCaseRepository,
            ProblemTemplateRepository templateRepository,
            UserProblemRepository userProblemRepository,
            RankingService rankingService) {
        this.submissionRepository  = submissionRepository;
        this.problemRepository     = problemRepository;
        this.queueService          = queueService;
        this.judge0Service         = judge0Service;
        this.testCaseRepository    = testCaseRepository;
        this.templateRepository    = templateRepository;
        this.userProblemRepository = userProblemRepository;
        this.rankingService        = rankingService;
    }

    // ─── SUBMIT — saves as PENDING and queues ─────────────────
    public ExecutionDto.SubmitResponse submitSolution(
            Long userId, Long problemId,
            String code, String language) {

        problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        Submission submission = Submission.builder()
                .userId(userId)
                .problemId(problemId)
                .code(code)
                .language(language.toUpperCase())
                .status(Submission.Status.PENDING)
                .build();
        submissionRepository.save(submission);

        queueService.pushToQueue(submission.getId());

        return ExecutionDto.SubmitResponse.builder()
                .submissionId(submission.getId())
                .status("PENDING")
                .message("Submission received. Processing...")
                .build();
    }

    // ─── GET RESULT — frontend polls this ─────────────────────
    public ExecutionDto.SubmissionResult getSubmissionResult(
            Long submissionId, Long userId) {

        Submission submission = submissionRepository
                .findById(submissionId)
                .orElseThrow(() -> new RuntimeException("Submission not found"));

        // Get ranking only if ACCEPTED
        Map<String, Object> ranking = null;
        if (submission.getStatus() == Submission.Status.ACCEPTED) {
            ranking = rankingService.getRanking(
                    submission.getProblemId(),
                    submission.getRuntimeMs(),
                    submission.getLanguage());
        }

        // Build submission history
        List<ExecutionDto.SubmissionHistoryItem> history = submissionRepository
                .findByUserIdAndProblemIdOrderBySubmittedAtDesc(
                        userId, submission.getProblemId())
                .stream()
                .map(s -> ExecutionDto.SubmissionHistoryItem.builder()
                        .id(s.getId())
                        .status(s.getStatus().name())
                        .language(s.getLanguage())
                        .runtimeMs(s.getRuntimeMs())
                        .submittedAt(s.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());

        return ExecutionDto.SubmissionResult.builder()
                .submissionId(submission.getId())
                .problemId(submission.getProblemId())
                .status(submission.getStatus().name())
                .code(submission.getCode())
                .language(submission.getLanguage())
                .runtimeMs(submission.getRuntimeMs())
                .totalTestCases(submission.getTotalTestCases())
                .passedTestCases(submission.getPassedTestCases())
                .failedInput(submission.getFailedInput())
                .failedExpectedOutput(submission.getFailedExpected())
                .failedActualOutput(submission.getFailedActual())
                .error(submission.getErrorMessage())
                .ranking(ranking)
                .submissionHistory(history)
                .build();
    }

    // ─── RUN CODE — runs against all visible test cases ───────
    public ExecutionDto.RunResponse runCode(
            ExecutionDto.RunRequest request, Long userId) {
        try {
            double execTimeSecs = 0.0; // actual execution time from sandbox

            // Get the code template
            String userCode = request.getCode();
            Optional<ProblemTemplate> template = templateRepository
                    .findByProblemIdAndLanguage(
                            request.getProblemId(),
                            request.getLanguage().toUpperCase());

            String fullCode = userCode;
            if (template.isPresent()) {
                fullCode = template.get().getDriverCode()
                        .replace("{{USER_CODE}}", sanitizeUserCode(userCode, request.getLanguage()));
            }

            // Get visible test cases
            List<TestCase> testCases = testCaseRepository
                    .findByProblemIdAndIsHidden(request.getProblemId(), false);

            if (testCases.isEmpty()) {
                return ExecutionDto.RunResponse.builder()
                        .status("ERROR")
                        .error("No test cases available")
                        .build();
            }

            List<ExecutionDto.TestCaseResult> results = new ArrayList<>();
            String overallStatus = "ACCEPTED";
            String overallError  = null;
            int passed = 0;

            // Transform single-testcase driver to batch driver
            fullCode = SubmissionWorker.wrapDriverForBatch(fullCode, request.getLanguage().toLowerCase());

            // Build batched input
            StringBuilder batchedInput = new StringBuilder();
            batchedInput.append(testCases.size()).append("\n");
            for (TestCase tc : testCases) {
                String in = tc.getInput() != null ? tc.getInput() : "";
                batchedInput.append(in);
                if (!in.endsWith("\n")) batchedInput.append("\n");
                batchedInput.append(SubmissionWorker.TC_SENTINEL).append("\n");
            }

            try {
                Judge0Service.PistonResult result = judge0Service.executeCode(
                        fullCode,
                        request.getLanguage(),
                        batchedInput.toString());

                // Capture actual execution time from sandbox
                if (result.getRun() != null) {
                    execTimeSecs = result.getRun().getExecutionTimeSeconds();
                }

                String status = judge0Service.mapStatus(result);

                if ("COMPILE_ERROR".equals(status) ||
                    "TIME_LIMIT".equals(status) ||
                    "RUNTIME_ERROR".equals(status)) {
                    
                    overallStatus = status;
                    String error = null;
                    if (result.getRun() != null && result.getRun().getStderr() != null) {
                        error = result.getRun().getStderr();
                    } else if (result.getRun() != null && result.getRun().getStdout() != null) {
                        error = result.getRun().getStdout();
                    }
                    if (error != null) error = error.trim();
                    if (error != null && error.isEmpty()) error = null;
                    overallError = error;
                    
                    // Fallback to first testcase
                    TestCase tc = testCases.get(0);
                    results.add(ExecutionDto.TestCaseResult.builder()
                            .testCaseIndex(1)
                            .input(tc.getInput())
                            .expectedOutput(tc.getOutput() != null ? tc.getOutput().trim() : "")
                            .actualOutput("")
                            .passed(false)
                            .error(error)
                            .build());
                } else {
                    String rawOut = result.getRun() != null && result.getRun().getStdout() != null
                            ? result.getRun().getStdout() : "";
                            
                    // Check if we captured an error in stdout
                    String error = null;
                    int errIdx = rawOut.indexOf("\n---ERROR---\n");
                    if (errIdx != -1) {
                        error = rawOut.substring(errIdx + 13).trim();
                        rawOut = rawOut.substring(0, errIdx);
                    }
                    
                    String[] outputs = rawOut.split("\\n?" + SubmissionWorker.TC_SENTINEL + "\\n?");
                    
                    for (int i = 0; i < testCases.size(); i++) {
                        TestCase tc = testCases.get(i);
                        String expected = tc.getOutput() != null ? tc.getOutput().trim() : "";
                        
                        if (i < outputs.length && (error == null || i < outputs.length - 1)) {
                            String actual = outputs[i].trim();
                            boolean tcPassed = normalize(actual).equals(normalize(expected));
                            
                            if (tcPassed) {
                                passed++;
                            } else if ("ACCEPTED".equals(overallStatus)) {
                                overallStatus = "WRONG_ANSWER";
                            }
                            
                            results.add(ExecutionDto.TestCaseResult.builder()
                                    .testCaseIndex(i + 1)
                                    .input(tc.getInput())
                                    .expectedOutput(expected)
                                    .actualOutput(actual)
                                    .passed(tcPassed)
                                    .error(null)
                                    .build());
                                    
                            if (!tcPassed) break; // Stop at first failure
                        } else {
                            if (error != null && error.contains("Time Limit Exceeded")) {
                                overallStatus = "TIME_LIMIT_EXCEEDED";
                            } else {
                                overallStatus = "RUNTIME_ERROR";
                            }
                            if (error == null) {
                                error = result.getRun() != null ? result.getRun().getStderr() : "Program crashed mid-execution.";
                            }
                            if (error != null) error = error.trim();
                            overallError = error;
                            
                            results.add(ExecutionDto.TestCaseResult.builder()
                                    .testCaseIndex(i + 1)
                                    .input(tc.getInput())
                                    .expectedOutput(expected)
                                    .actualOutput("")
                                    .passed(false)
                                    .error(error != null && !error.isEmpty() ? error : "Execution crashed.")
                                    .build());
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                overallStatus = "RUNTIME_ERROR";
                overallError  = e.getMessage();
                results.add(ExecutionDto.TestCaseResult.builder()
                        .testCaseIndex(1)
                        .input(testCases.get(0).getInput())
                        .expectedOutput(testCases.get(0).getOutput() != null ? testCases.get(0).getOutput().trim() : "")
                        .actualOutput("")
                        .passed(false)
                        .error(e.getMessage())
                        .build());
            }

            // ─── Calculate LeetCode-style runtime ─────────────────────
            double startupOverhead;
            switch (request.getLanguage().toLowerCase()) {
                case "java":       startupOverhead = 1.6; break;
                case "cpp":        startupOverhead = 0.3; break;
                case "python":     startupOverhead = 0.2; break;
                case "javascript": startupOverhead = 0.2; break;
                default:           startupOverhead = 0.3; break;
            }
            
            double userCodeTimeSecs = execTimeSecs - startupOverhead;
            if (userCodeTimeSecs < 0) userCodeTimeSecs = 0;
            
            // Convert actual execution time to milliseconds (like LeetCode)
            int runtimeMs = (int) Math.round(userCodeTimeSecs * 1000);

            return ExecutionDto.RunResponse.builder()
                    .status(overallStatus)
                    .error(overallError)
                    .runtimeMs(runtimeMs)
                    .passedCount(passed)
                    .totalCount(testCases.size())
                    .testCaseResults(results)
                    .build();

        } catch (Exception e) {
            return ExecutionDto.RunResponse.builder()
                    .status("ERROR")
                    .error(e.getMessage())
                    .build();
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim()
                .replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n+$", "");
    }

    /**
     * Cleans up user code before injection into the driver template.
     * For Java: removes import/package statements (driver already has them)
     *           but KEEPS the class wrapper intact (driver expects full class).
     */
    public static String sanitizeUserCode(String code, String language) {
        if (code == null) return "";
        String trimmed = code.trim();

        // Only special handling for Java
        if (!"JAVA".equalsIgnoreCase(language)) return trimmed;

        // Remove import statements (driver template already has imports)
        trimmed = trimmed.replaceAll("(?m)^\\s*import\\s+[\\w.*]+;\\s*$", "").trim();

        // Remove package declarations
        trimmed = trimmed.replaceAll("(?m)^\\s*package\\s+[\\w.]+;\\s*$", "").trim();

        return trimmed;
    }

    // ─── GET SUBMISSIONS LIST ─────────────────────────────────
    public List<Submission> getSubmissions(
            Long userId, Long problemId) {
        if (problemId != null) {
            return submissionRepository
                    .findByUserIdAndProblemIdOrderBySubmittedAtDesc(
                            userId, problemId);
        }
        return submissionRepository
                .findByUserIdOrderBySubmittedAtDesc(userId);
    }
}