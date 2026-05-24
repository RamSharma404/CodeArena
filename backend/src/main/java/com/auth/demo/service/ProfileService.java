package com.auth.demo.service;

import com.auth.demo.dto.ProfileStatsDto;
import com.auth.demo.model.Problem;
import com.auth.demo.model.Submission;
import com.auth.demo.model.User;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.SubmissionRepository;
import com.auth.demo.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileService {

    private final UserRepository userRepository;
    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;

    public ProfileService(UserRepository userRepository,
                          SubmissionRepository submissionRepository,
                          ProblemRepository problemRepository) {
        this.userRepository = userRepository;
        this.submissionRepository = submissionRepository;
        this.problemRepository = problemRepository;
    }

    public ProfileStatsDto getUserProfileStats(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Submission> submissions = submissionRepository.findByUserIdOrderBySubmittedAtDesc(userId);
        List<Problem> problems = problemRepository.findAll();

        Map<Long, Problem> problemMap = problems.stream()
                .collect(Collectors.toMap(Problem::getId, p -> p));

        ProfileStatsDto dto = new ProfileStatsDto();
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=" + user.getUsername());

        // Overall Submission Stats
        dto.setTotalSubmissions(submissions.size());
        int acceptedCount = 0;
        Map<String, Integer> langCount = new HashMap<>();
        Set<Long> solvedProblemIds = new HashSet<>();

        for (Submission sub : submissions) {
            if ("ACCEPTED".equalsIgnoreCase(sub.getStatus().name())) {
                acceptedCount++;
                solvedProblemIds.add(sub.getProblemId());
            }
            langCount.put(sub.getLanguage(), langCount.getOrDefault(sub.getLanguage(), 0) + 1);
        }

        dto.setAcceptedSubmissions(acceptedCount);
        if (submissions.size() > 0) {
            double rate = ((double) acceptedCount / submissions.size()) * 100.0;
            // Round to 1 decimal place
            dto.setAcceptanceRate(Math.round(rate * 10.0) / 10.0);
        } else {
            dto.setAcceptanceRate(0.0);
        }
        dto.setLanguageStats(langCount);

        // Problem Solved Stats by Difficulty
        int totalEasy = 0, totalMedium = 0, totalHard = 0;
        for (Problem p : problems) {
            if (p.getDifficulty() == Problem.Difficulty.EASY) totalEasy++;
            else if (p.getDifficulty() == Problem.Difficulty.MEDIUM) totalMedium++;
            else if (p.getDifficulty() == Problem.Difficulty.HARD) totalHard++;
        }

        int easySolved = 0, mediumSolved = 0, hardSolved = 0;
        for (Long pid : solvedProblemIds) {
            Problem p = problemMap.get(pid);
            if (p != null) {
                if (p.getDifficulty() == Problem.Difficulty.EASY) easySolved++;
                else if (p.getDifficulty() == Problem.Difficulty.MEDIUM) mediumSolved++;
                else if (p.getDifficulty() == Problem.Difficulty.HARD) hardSolved++;
            }
        }

        dto.setTotalProblems(problems.size());
        dto.setTotalEasy(totalEasy);
        dto.setTotalMedium(totalMedium);
        dto.setTotalHard(totalHard);

        dto.setTotalSolved(solvedProblemIds.size());
        dto.setEasySolved(easySolved);
        dto.setMediumSolved(mediumSolved);
        dto.setHardSolved(hardSolved);

        // Recent Submissions (Top 15)
        List<ProfileStatsDto.SubmissionSummaryDto> recent = new ArrayList<>();
        int limit = Math.min(submissions.size(), 15);
        for (int i = 0; i < limit; i++) {
            Submission sub = submissions.get(i);
            Problem p = problemMap.get(sub.getProblemId());
            ProfileStatsDto.SubmissionSummaryDto sum = new ProfileStatsDto.SubmissionSummaryDto();
            sum.setId(sub.getId());
            sum.setProblemId(sub.getProblemId());
            sum.setProblemTitle(p != null ? p.getTitle() : "Unknown");
            sum.setDifficulty(p != null ? p.getDifficulty().name() : "EASY");
            sum.setLanguage(sub.getLanguage());
            sum.setStatus(sub.getStatus() != null ? sub.getStatus().name() : null);
            sum.setSubmittedAt(sub.getSubmittedAt());
            sum.setRuntimeMs(sub.getRuntimeMs());
            recent.add(sum);
        }
        dto.setRecentSubmissions(recent);

        return dto;
    }
}
