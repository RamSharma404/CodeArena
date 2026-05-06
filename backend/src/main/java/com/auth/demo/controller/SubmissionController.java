package com.auth.demo.controller;

import com.auth.demo.dto.ExecutionDto;
import com.auth.demo.model.User;
import com.auth.demo.security.RoleGuard;
import com.auth.demo.service.RankingService;
import com.auth.demo.service.SubmissionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final RankingService    rankingService;
    private final RoleGuard         roleGuard;

    public SubmissionController(SubmissionService submissionService,
                                RankingService rankingService,
                                RoleGuard roleGuard) {
        this.submissionService = submissionService;
        this.rankingService    = rankingService;
        this.roleGuard         = roleGuard;
    }

    // POST /api/execute — Run code (not queued)
    @PostMapping("/execute")
    public ResponseEntity<?> executeCode(
            @RequestBody ExecutionDto.RunRequest request,
            HttpServletRequest httpRequest) {
        try {
            Long userId = null;
            try {
                userId = roleGuard.requireAuth(httpRequest).getId();
            } catch (Exception ignored) {}
            return ResponseEntity.ok(
                    submissionService.runCode(request, userId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /api/submit — Submit code (queued)
    @PostMapping("/submit")
    public ResponseEntity<?> submitCode(
            @RequestBody ExecutionDto.SubmitRequest request,
            HttpServletRequest httpRequest) {
        try {
            User user = roleGuard.requireAuth(httpRequest);
            return ResponseEntity.ok(
                    submissionService.submitSolution(
                            user.getId(),
                            request.getProblemId(),
                            request.getCode(),
                            request.getLanguage()
                    ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/submission/{id} — Poll for result
    @GetMapping("/submission/{id}")
    public ResponseEntity<?> getSubmissionResult(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        try {
            User user = roleGuard.requireAuth(httpRequest);
            return ResponseEntity.ok(
                    submissionService.getSubmissionResult(
                            id, user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/submissions — Get submission history
    @GetMapping("/submissions")
    public ResponseEntity<?> getSubmissions(
            @RequestParam(required = false) Long problemId,
            HttpServletRequest httpRequest) {
        try {
            User user = roleGuard.requireAuth(httpRequest);
            return ResponseEntity.ok(
                    submissionService.getSubmissions(
                            user.getId(), problemId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/submission/{id}/distribution — Runtime distribution for bar chart
    @GetMapping("/submission/{id}/distribution")
    public ResponseEntity<?> getRuntimeDistribution(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        try {
            User user = roleGuard.requireAuth(httpRequest);
            ExecutionDto.SubmissionResult result =
                    submissionService.getSubmissionResult(id, user.getId());
            return ResponseEntity.ok(
                    rankingService.getRuntimeDistribution(
                            result.getProblemId(),
                            result.getLanguage(),
                            result.getRuntimeMs()));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}