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
            long overallStart = System.currentTimeMillis();

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

            for (int i = 0; i < testCases.size(); i++) {
                TestCase tc = testCases.get(i);
                try {
                    Judge0Service.PistonResult result = judge0Service.executeCode(
                            fullCode,
                            request.getLanguage(),
                            tc.getInput());

                    String status = judge0Service.mapStatus(result);

                    // Handle compile/runtime errors — stop immediately
                    if ("COMPILE_ERROR".equals(status) ||
                            "TIME_LIMIT".equals(status) ||
                            "RUNTIME_ERROR".equals(status)) {

                        String error = result.getRun() != null
                                ? result.getRun().getStderr() : null;
                        if (error != null) error = error.trim();
                        if (error != null && error.isEmpty()) error = null;

                        overallStatus = status;
                        overallError  = error;

                        results.add(ExecutionDto.TestCaseResult.builder()
                                .testCaseIndex(i + 1)
                                .input(tc.getInput())
                                .expectedOutput(tc.getOutput() != null ? tc.getOutput().trim() : "")
                                .actualOutput("")
                                .passed(false)
                                .error(error)
                                .build());
                        break;
                    }

                    // Get actual output
                    String actual = result.getRun() != null
                            && result.getRun().getStdout() != null
                            ? result.getRun().getStdout().trim() : "";

                    String expected = tc.getOutput() != null
                            ? tc.getOutput().trim() : "";

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

                    // Small delay between API calls to avoid rate limiting
                    if (i < testCases.size() - 1) {
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    overallStatus = "RUNTIME_ERROR";
                    overallError  = e.getMessage();
                    results.add(ExecutionDto.TestCaseResult.builder()
                            .testCaseIndex(i + 1)
                            .input(tc.getInput())
                            .expectedOutput(tc.getOutput() != null ? tc.getOutput().trim() : "")
                            .actualOutput("")
                            .passed(false)
                            .error(e.getMessage())
                            .build());
                    break;
                }
            }

            long totalRuntime = System.currentTimeMillis() - overallStart;

            return ExecutionDto.RunResponse.builder()
                    .status(overallStatus)
                    .error(overallError)
                    .runtimeMs((int) totalRuntime)
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
     * Strips class wrappers like "class Solution { ... }" from user code
     * so it can be safely injected into the driver template.
     * Users can write code with or without the wrapper — both will work.
     */
    public static String sanitizeUserCode(String code, String language) {
        if (code == null) return "";
        String trimmed = code.trim();

        // Only apply to Java (other languages don't have this issue)
        if (!"JAVA".equalsIgnoreCase(language)) return trimmed;

        // Remove import statements (driver template already has imports)
        trimmed = trimmed.replaceAll("(?m)^\\s*import\\s+[\\w.*]+;\\s*$", "").trim();

        // Remove package declarations
        trimmed = trimmed.replaceAll("(?m)^\\s*package\\s+[\\w.]+;\\s*$", "").trim();

        // Match patterns like: class Solution { ... }
        // or: public class Solution { ... }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "^\\s*(public\\s+)?class\\s+\\w+\\s*\\{",
                java.util.regex.Pattern.MULTILINE
        );
        java.util.regex.Matcher matcher = pattern.matcher(trimmed);

        if (matcher.find()) {
            // Remove the class declaration line
            String withoutClassDecl = trimmed.substring(matcher.end());

            // Remove the matching closing brace (last '}' in the code)
            int lastBrace = withoutClassDecl.lastIndexOf('}');
            if (lastBrace >= 0) {
                withoutClassDecl = withoutClassDecl.substring(0, lastBrace);
            }

            return withoutClassDecl.trim();
        }

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