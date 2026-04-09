package com.auth.demo.service;

import com.auth.demo.dto.ProblemDto;
import com.auth.demo.model.Problem;
import com.auth.demo.model.ProblemTemplate;
import com.auth.demo.model.TestCase;
import com.auth.demo.model.UserProblem;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.ProblemTemplateRepository;
import com.auth.demo.repository.TestCaseRepository;
import com.auth.demo.repository.UserProblemRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ProblemService {

    private final ProblemRepository         problemRepository;
    private final TestCaseRepository        testCaseRepository;
    private final UserProblemRepository     userProblemRepository;
    private final ProblemTemplateRepository templateRepository;

    public ProblemService(ProblemRepository problemRepository,
                          TestCaseRepository testCaseRepository,
                          UserProblemRepository userProblemRepository,
                          ProblemTemplateRepository templateRepository) {
        this.problemRepository     = problemRepository;
        this.testCaseRepository    = testCaseRepository;
        this.userProblemRepository = userProblemRepository;
        this.templateRepository    = templateRepository;
    }

    // ─── GET ALL PROBLEMS ─────────────────────────────────────
    public List<ProblemDto.ProblemSummary> getProblems(
            String difficulty, String search, Long userId) {

        Problem.Difficulty diffEnum = null;
        if (difficulty != null && !difficulty.isEmpty()) {
            diffEnum = Problem.Difficulty.valueOf(difficulty.toUpperCase());
        }

        List<Problem> problems = problemRepository.findByFilters(diffEnum, search);

        return problems.stream().map(problem -> {
            String solvedStatus = null;
            if (userId != null) {
                Optional<UserProblem> up = userProblemRepository
                        .findByIdUserIdAndIdProblemId(userId, problem.getId());
                if (up.isPresent())
                    solvedStatus = up.get().getStatus().name();
            }
            return ProblemDto.ProblemSummary.builder()
                    .id(problem.getId())
                    .title(problem.getTitle())
                    .slug(problem.getSlug())
                    .difficulty(problem.getDifficulty())
                    .topicTags(problem.getTopicTags())
                    .companyTags(problem.getCompanyTags())
                    .solvedStatus(solvedStatus)
                    .build();
        }).collect(Collectors.toList());
    }

    // ─── GET SINGLE PROBLEM BY SLUG ───────────────────────────
    public ProblemDto.ProblemDetail getProblemBySlug(String slug) {
        Problem problem = problemRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        List<TestCase> visible = testCaseRepository
                .findByProblemIdAndIsHidden(problem.getId(), false);

        List<ProblemDto.TestCaseDto> testCaseDtos = visible.stream()
                .map(tc -> ProblemDto.TestCaseDto.builder()
                        .id(tc.getId())
                        .input(tc.getInput())
                        .output(tc.getOutput())
                        .build())
                .collect(Collectors.toList());

        return ProblemDto.ProblemDetail.builder()
                .id(problem.getId())
                .title(problem.getTitle())
                .slug(problem.getSlug())
                .description(problem.getDescription())
                .difficulty(problem.getDifficulty())
                .topicTags(problem.getTopicTags())
                .companyTags(problem.getCompanyTags())
                .visibleTestCases(testCaseDtos)
                .createdAt(problem.getCreatedAt())
                .build();
    }

    // ─── GET SINGLE PROBLEM BY ID ─────────────────────────────
    public ProblemDto.ProblemDetail getProblemById(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));
        return getProblemBySlug(problem.getSlug());
    }

    // ─── GET TEMPLATE ─────────────────────────────────────────
    public ProblemDto.TemplateResponse getTemplate(Long problemId, String language) {
        ProblemTemplate template = templateRepository
                .findByProblemIdAndLanguage(problemId, language.toUpperCase())
                .orElseThrow(() -> new RuntimeException(
                        "No template found for language: " + language));

        return ProblemDto.TemplateResponse.builder()
                .language(template.getLanguage())
                .userTemplate(template.getUserTemplate())
                .build();
        // driverCode is NOT returned — stays hidden from user
    }

    // ─── CREATE PROBLEM ───────────────────────────────────────
    public ProblemDto.ProblemDetail createProblem(
            ProblemDto.CreateProblemRequest request) {

        if (problemRepository.findBySlug(request.getSlug()).isPresent())
            throw new RuntimeException("Problem with this slug already exists");

        Problem problem = Problem.builder()
                .title(request.getTitle())
                .slug(request.getSlug())
                .description(request.getDescription())
                .difficulty(request.getDifficulty())
                .topicTags(request.getTopicTags())
                .companyTags(request.getCompanyTags())
                .build();
        problemRepository.save(problem);

        // Save test cases
        if (request.getTestCases() != null) {
            request.getTestCases().forEach(tc -> {
                TestCase testCase = TestCase.builder()
                        .problemId(problem.getId())
                        .input(tc.getInput())
                        .output(tc.getOutput())
                        .isHidden(tc.isHidden())
                        .build();
                testCaseRepository.save(testCase);
            });
        }

        // Save templates
        if (request.getTemplates() != null) {
            request.getTemplates().forEach(t -> {
                ProblemTemplate template = ProblemTemplate.builder()
                        .problemId(problem.getId())
                        .language(t.getLanguage().toUpperCase())
                        .userTemplate(t.getUserTemplate())
                        .driverCode(t.getDriverCode())
                        .build();
                templateRepository.save(template);
            });
        }

        return getProblemBySlug(problem.getSlug());
    }

    // ─── UPDATE PROBLEM ───────────────────────────────────────
    public ProblemDto.ProblemDetail updateProblem(
            Long id, ProblemDto.CreateProblemRequest request) {

        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        problem.setTitle(request.getTitle());
        problem.setSlug(request.getSlug());
        problem.setDescription(request.getDescription());
        problem.setDifficulty(request.getDifficulty());
        problem.setTopicTags(request.getTopicTags());
        problem.setCompanyTags(request.getCompanyTags());
        problemRepository.save(problem);

        // Replace test cases
        if (request.getTestCases() != null) {
            testCaseRepository.deleteAll(
                    testCaseRepository.findByProblemId(id));
            request.getTestCases().forEach(tc -> {
                TestCase testCase = TestCase.builder()
                        .problemId(problem.getId())
                        .input(tc.getInput())
                        .output(tc.getOutput())
                        .isHidden(tc.isHidden())
                        .build();
                testCaseRepository.save(testCase);
            });
        }

        // Replace templates
        if (request.getTemplates() != null) {
            templateRepository.deleteAll(
                    templateRepository.findByProblemId(id));
            request.getTemplates().forEach(t -> {
                ProblemTemplate template = ProblemTemplate.builder()
                        .problemId(problem.getId())
                        .language(t.getLanguage().toUpperCase())
                        .userTemplate(t.getUserTemplate())
                        .driverCode(t.getDriverCode())
                        .build();
                templateRepository.save(template);
            });
        }

        return getProblemBySlug(problem.getSlug());
    }

    // ─── DELETE PROBLEM ───────────────────────────────────────
    public void deleteProblem(Long id) {
        Problem problem = problemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Problem not found"));

        testCaseRepository.deleteAll(testCaseRepository.findByProblemId(id));
        templateRepository.deleteAll(templateRepository.findByProblemId(id));
        problemRepository.delete(problem);
    }
}