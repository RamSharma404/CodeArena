package com.auth.demo.service;

import com.auth.demo.dto.ExecutionDto;
import com.auth.demo.model.*;
import com.auth.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubmissionService {

    private final Judge0Service             judge0Service;
    private final SubmissionRepository      submissionRepository;
    private final TestCaseRepository        testCaseRepository;
    private final ProblemRepository         problemRepository;
    private final UserProblemRepository     userProblemRepository;
    private final ProblemTemplateRepository templateRepository;

    public SubmissionService(Judge0Service judge0Service,
                             SubmissionRepository submissionRepository,
                             TestCaseRepository testCaseRepository,
                             ProblemRepository problemRepository,
                             UserProblemRepository userProblemRepository,
                             ProblemTemplateRepository templateRepository) {
        this.judge0Service         = judge0Service;
        this.submissionRepository  = submissionRepository;
        this.testCaseRepository    = testCaseRepository;
        this.problemRepository     = problemRepository;
        this.userProblemRepository = userProblemRepository;
        this.templateRepository    = templateRepository;
    }

    private String buildFullCode(String userCode, String driverCode) {
        return driverCode.replace("{{USER_CODE}}", userCode);
    }

    private String normalizeOutput(String output) {
        if (output == null) return "";
        return output.trim()
                .replaceAll("\\r\\n", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n+$", "");
    }

    // ─── RUN CODE (custom input, not saved) ───────────────────
    public ExecutionDto.RunResponse runCode(
            ExecutionDto.RunRequest request, Long userId) {
        try {
            long startTime = System.currentTimeMillis();

            String fullCode = request.getCode();
            Optional<ProblemTemplate> template = templateRepository
                    .findByProblemIdAndLanguage(
                            request.getProblemId(),
                            request.getLanguage().toUpperCase());
            if (template.isPresent()) {
                fullCode = buildFullCode(
                        request.getCode(),
                        template.get().getDriverCode());
            }

            Judge0Service.PistonResult result;
            String input = request.getCustomInput();
            if (input == null || input.trim().isEmpty()) {
                List<TestCase> testCases = testCaseRepository.findByProblemId(request.getProblemId());
                if (!testCases.isEmpty()) {
                    input = testCases.get(0).getInput();
                }
            }

            result = judge0Service.executeCode(fullCode, request.getLanguage(), input);

            long   runtimeMs = System.currentTimeMillis() - startTime;
            String status    = judge0Service.mapStatus(result);

            String output = null;
            String error  = null;
            if (result.getRun() != null) {
                output = result.getRun().getStdout();
                error  = result.getRun().getStderr();
                if (output != null) output = output.trim();
                if (error  != null) error  = error.trim();
                if (error  != null && error.isEmpty()) error = null;
            }

            return ExecutionDto.RunResponse.builder()
                    .status(status)
                    .output(output)
                    .error(error)
                    .runtimeMs((int) runtimeMs)
                    .build();

        } catch (Exception e) {
            return ExecutionDto.RunResponse.builder()
                    .status("ERROR")
                    .error(e.getMessage())
                    .build();
        }
    }

    // ─── SUBMIT CODE ──────────────────────────────────────────
    // Returns status + test results + submission history
    // Does NOT call AI — user triggers that separately
    @Transactional
    public ExecutionDto.SubmitResponse submitSolution(
            Long userId, Long problemId,
            String code, String language) {

        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        List<TestCase> testCases = testCaseRepository
                .findByProblemId(problemId);
        if (testCases.isEmpty())
            throw new RuntimeException("No test cases found");

        Optional<ProblemTemplate> template = templateRepository
                .findByProblemIdAndLanguage(problemId, language.toUpperCase());

        int    total          = testCases.size();
        int    passed         = 0;
        String finalStatus    = "ACCEPTED";
        String failedInput    = null;
        String failedExpected = null;
        String failedActual   = null;
        String error          = null;
        long   totalRuntime   = 0;

        for (TestCase testCase : testCases) {
            try {
                long startTime = System.currentTimeMillis();

                String codeToRun = template.isPresent()
                        ? buildFullCode(code, template.get().getDriverCode())
                        : code;

                Judge0Service.PistonResult result = judge0Service.executeCode(
                        codeToRun, language, testCase.getInput());

                totalRuntime += System.currentTimeMillis() - startTime;
                String status = judge0Service.mapStatus(result);

                if ("COMPILE_ERROR".equals(status) ||
                        "TIME_LIMIT".equals(status) ||
                        "RUNTIME_ERROR".equals(status)) {
                    finalStatus = status;
                    if (result.getRun() != null)
                        error = result.getRun().getStderr();
                    break;
                }

                String actualOutput   = result.getRun() != null
                        && result.getRun().getStdout() != null
                        ? normalizeOutput(result.getRun().getStdout()) : "";
                String expectedOutput = normalizeOutput(testCase.getOutput());

                if (actualOutput.equals(expectedOutput)) {
                    passed++;
                } else if ("ACCEPTED".equals(finalStatus)) {
                    finalStatus    = "WRONG_ANSWER";
                    failedInput    = testCase.getInput();
                    failedExpected = expectedOutput;
                    failedActual   = actualOutput;
                }

            } catch (Exception e) {
                finalStatus = "RUNTIME_ERROR";
                error       = e.getMessage();
                break;
            }
        }

        Integer avgRuntime = total > 0 ? (int)(totalRuntime / total) : null;

        // Save submission
        Submission submission = Submission.builder()
                .userId(userId)
                .problemId(problemId)
                .code(code)
                .language(language.toUpperCase())
                .status(mapToEnum(finalStatus))
                .runtimeMs(avgRuntime)
                .memoryKb(0)
                .build();
        submissionRepository.save(submission);

        updateUserProblemStatus(userId, problemId, finalStatus);



        return ExecutionDto.SubmitResponse.builder()
                .submissionId(submission.getId())
                .status(finalStatus)
                .runtimeMs(avgRuntime)
                .totalTestCases(total)
                .passedTestCases(passed)
                .failedInput("ACCEPTED".equals(finalStatus) ? null : failedInput)
                .failedExpectedOutput("ACCEPTED".equals(finalStatus) ? null : failedExpected)
                .failedActualOutput("ACCEPTED".equals(finalStatus) ? null : failedActual)
                .error(error)
                .build();
    }

    // ─── GET SUBMISSIONS ──────────────────────────────────────
    public List<Submission> getSubmissions(Long userId, Long problemId) {
        if (problemId != null) {
            return submissionRepository
                    .findByUserIdAndProblemIdOrderBySubmittedAtDesc(
                            userId, problemId);
        }
        return submissionRepository
                .findByUserIdOrderBySubmittedAtDesc(userId);
    }

    private Submission.Status mapToEnum(String status) {
        return switch (status) {
            case "ACCEPTED"      -> Submission.Status.ACCEPTED;
            case "WRONG_ANSWER"  -> Submission.Status.WRONG_ANSWER;
            case "TIME_LIMIT"    -> Submission.Status.TIME_LIMIT;
            case "COMPILE_ERROR" -> Submission.Status.COMPILE_ERROR;
            default              -> Submission.Status.RUNTIME_ERROR;
        };
    }

    private void updateUserProblemStatus(
            Long userId, Long problemId, String status) {
        UserProblem.UserProblemId id =
                new UserProblem.UserProblemId(userId, problemId);
        Optional<UserProblem> existing = userProblemRepository
                .findByIdUserIdAndIdProblemId(userId, problemId);

        if ("ACCEPTED".equals(status)) {
            UserProblem up = existing.orElse(new UserProblem());
            up.setId(id);
            up.setStatus(UserProblem.Status.SOLVED);
            up.setSolvedAt(LocalDateTime.now());
            userProblemRepository.save(up);
        } else if (existing.isEmpty()) {
            UserProblem up = new UserProblem();
            up.setId(id);
            up.setStatus(UserProblem.Status.ATTEMPTED);
            userProblemRepository.save(up);
        }
    }
}