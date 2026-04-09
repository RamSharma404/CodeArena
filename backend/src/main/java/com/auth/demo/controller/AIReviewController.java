package com.auth.demo.controller;

import com.auth.demo.model.Problem;
import com.auth.demo.model.Submission;
import com.auth.demo.model.User;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.SubmissionRepository;
import com.auth.demo.security.RoleGuard;
import com.auth.demo.service.AIReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.Data;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AIReviewController {

    private final AIReviewService      aiReviewService;
    private final RoleGuard            roleGuard;
    private final ProblemRepository    problemRepository;
    private final SubmissionRepository submissionRepository;

    public AIReviewController(AIReviewService aiReviewService,
                              RoleGuard roleGuard,
                              ProblemRepository problemRepository,
                              SubmissionRepository submissionRepository) {
        this.aiReviewService      = aiReviewService;
        this.roleGuard            = roleGuard;
        this.problemRepository    = problemRepository;
        this.submissionRepository = submissionRepository;
    }

    // POST /api/problems/{id}/review
    // Only works if user has ACCEPTED submission
    // Called when user clicks "Get AI Review" button
    @PostMapping("/problems/{id}/review")
    public ResponseEntity<?> reviewCode(
            @PathVariable Long id,
            @RequestBody ReviewRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = roleGuard.requireAuth(authHeader);

            // Check user has an accepted submission for this problem
            List<Submission> submissions = submissionRepository
                    .findByUserIdAndProblemIdOrderBySubmittedAtDesc(
                            user.getId(), id);

            boolean hasAccepted = submissions.stream()
                    .anyMatch(s -> s.getStatus() == Submission.Status.ACCEPTED);

            if (!hasAccepted) {
                return ResponseEntity.badRequest().body(
                        "You must solve the problem first before requesting AI review"
                );
            }

            Problem problem = problemRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Problem not found"));

            AIReviewService.CodeReviewResponse review =
                    aiReviewService.reviewCode(
                            request.getCode(),
                            request.getLanguage(),
                            problem.getTitle(),
                            problem.getDescription()
                    );

            return ResponseEntity.ok(review);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @Data
    public static class ReviewRequest {
        private String code;
        private String language;
    }
}