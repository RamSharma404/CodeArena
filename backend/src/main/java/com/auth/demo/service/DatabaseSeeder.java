package com.auth.demo.service;

import com.auth.demo.model.*;
import com.auth.demo.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

@Service
public class DatabaseSeeder {

    @Value("${seed.problems:false}")
    private boolean seedProblems;

    private final ProblemRepository         problemRepository;
    private final TestCaseRepository        testCaseRepository;
    private final ProblemTemplateRepository templateRepository;
    private final ObjectMapper              objectMapper;

    public DatabaseSeeder(ProblemRepository problemRepository,
                          TestCaseRepository testCaseRepository,
                          ProblemTemplateRepository templateRepository) {
        this.problemRepository  = problemRepository;
        this.testCaseRepository = testCaseRepository;
        this.templateRepository = templateRepository;
        this.objectMapper       = new ObjectMapper();
    }

    @PostConstruct
    public void seed() {
        // Only seed if enabled in properties
        // AND no problems exist yet
        if (!seedProblems) return;
        if (problemRepository.count() > 0) {
            System.out.println("Problems already seeded — skipping");
            return;
        }

        try {
            System.out.println("Seeding problems from JSON...");

            ClassPathResource resource =
                    new ClassPathResource("problems.json");

            List<Map<String, Object>> problems =
                    objectMapper.readValue(
                            resource.getInputStream(),
                            objectMapper.getTypeFactory()
                                    .constructCollectionType(
                                            List.class, Map.class));

            for (Map<String, Object> p : problems) {
                seedOneProblem(p);
            }

            System.out.println("Seeded "
                    + problems.size() + " problems successfully!");

        } catch (Exception e) {
            System.err.println("Seeding failed: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void seedOneProblem(Map<String, Object> p) {
        String slug = (String) p.get("slug");

        // Skip if already exists
        if (problemRepository.findBySlug(slug).isPresent()) {
            System.out.println("Skipping " + slug + " — already exists");
            return;
        }

        // Save problem
        Problem problem = Problem.builder()
                .title((String) p.get("title"))
                .slug(slug)
                .description((String) p.get("description"))
                .difficulty(Problem.Difficulty.valueOf(
                        (String) p.get("difficulty")))
                .topicTags((String) p.get("topicTags"))
                .companyTags((String) p.get("companyTags"))
                .build();
        problemRepository.save(problem);

        // Save test cases
        List<Map<String, Object>> testCases =
                (List<Map<String, Object>>) p.get("testCases");
        if (testCases != null) {
            for (Map<String, Object> tc : testCases) {
                TestCase testCase = TestCase.builder()
                        .problemId(problem.getId())
                        .input((String) tc.get("input"))
                        .output((String) tc.get("output"))
                        .isHidden((Boolean) tc.get("hidden"))
                        .build();
                testCaseRepository.save(testCase);
            }
        }

        // Save Python template
        if (p.get("pythonTemplate") != null) {
            templateRepository.save(ProblemTemplate.builder()
                    .problemId(problem.getId())
                    .language("PYTHON")
                    .userTemplate((String) p.get("pythonTemplate"))
                    .driverCode((String) p.get("pythonDriver"))
                    .build());
        }

        // Save Java template
        if (p.get("javaTemplate") != null) {
            templateRepository.save(ProblemTemplate.builder()
                    .problemId(problem.getId())
                    .language("JAVA")
                    .userTemplate((String) p.get("javaTemplate"))
                    .driverCode((String) p.get("javaDriver"))
                    .build());
        }

        // Save C++ template
        if (p.get("cppTemplate") != null) {
            templateRepository.save(ProblemTemplate.builder()
                    .problemId(problem.getId())
                    .language("CPP")
                    .userTemplate((String) p.get("cppTemplate"))
                    .driverCode((String) p.get("cppDriver"))
                    .build());
        }

        System.out.println("Seeded: " + problem.getTitle());
    }
}