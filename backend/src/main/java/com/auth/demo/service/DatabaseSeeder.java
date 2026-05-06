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

        Problem problem = problemRepository.findBySlug(slug).orElse(null);

        if (problem == null) {
            // Save problem
            problem = Problem.builder()
                    .title((String) p.get("title"))
                    .slug(slug)
                    .description((String) p.get("description"))
                    .difficulty(Problem.Difficulty.valueOf(
                            (String) p.get("difficulty")))
                    .topicTags((String) p.get("topicTags"))
                    .companyTags((String) p.get("companyTags"))
                    .build();
            problemRepository.save(problem);

            // Save test cases ONLY for new problems
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
        }


        // Save Python template
        if (p.get("pythonTemplate") != null) {
            Optional<ProblemTemplate> ptOpt = templateRepository.findByProblemIdAndLanguage(problem.getId(), "PYTHON");
            ProblemTemplate pt = ptOpt.orElse(ProblemTemplate.builder()
                    .problemId(problem.getId())
                    .language("PYTHON")
                    .build());
            pt.setUserTemplate((String) p.get("pythonTemplate"));
            pt.setDriverCode((String) p.get("pythonDriver"));
            templateRepository.save(pt);
        }

        // Save Java template
        if (p.get("javaTemplate") != null) {
            Optional<ProblemTemplate> jtOpt = templateRepository.findByProblemIdAndLanguage(problem.getId(), "JAVA");
            ProblemTemplate jt = jtOpt.orElse(ProblemTemplate.builder()
                    .problemId(problem.getId())
                    .language("JAVA")
                    .build());
            // Force LeetCode style Java templates
            if ("two-sum".equals(slug)) {
                jt.setUserTemplate("class Solution {\n    public int[] twoSum(int[] nums, int target) {\n        // Write your solution here\n        return new int[]{};\n    }\n}");
                jt.setDriverCode("import java.util.*;\n\nclass Main {\n    public static void main(String[] args) {\n        Scanner sc = new Scanner(System.in);\n        String line = sc.nextLine().trim();\n        int[] nums = Arrays.stream(line.split(\" \")).mapToInt(Integer::parseInt).toArray();\n        int target = Integer.parseInt(sc.nextLine().trim());\n        Solution sol = new Solution();\n        int[] r = sol.twoSum(nums, target);\n        System.out.println(r[0] + \" \" + r[1]);\n    }\n}\n\n{{USER_CODE}}");
            } else {
                jt.setUserTemplate((String) p.get("javaTemplate"));
                jt.setDriverCode((String) p.get("javaDriver"));
            }
            templateRepository.save(jt);
        }

        // Save C++ template
        if (p.get("cppTemplate") != null) {
            Optional<ProblemTemplate> ctOpt = templateRepository.findByProblemIdAndLanguage(problem.getId(), "CPP");
            ProblemTemplate ct = ctOpt.orElse(ProblemTemplate.builder()
                    .problemId(problem.getId())
                    .language("CPP")
                    .build());
            ct.setUserTemplate((String) p.get("cppTemplate"));
            ct.setDriverCode((String) p.get("cppDriver"));
            templateRepository.save(ct);
        }

        System.out.println("Updated templates for: " + problem.getTitle());
    }
}