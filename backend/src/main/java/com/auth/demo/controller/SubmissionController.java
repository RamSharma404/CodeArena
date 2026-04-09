package com.auth.demo.controller;

import com.auth.demo.dto.ExecutionDto;
import com.auth.demo.model.User;
import com.auth.demo.security.RoleGuard;
import com.auth.demo.service.SubmissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final RoleGuard         roleGuard;

    public SubmissionController(SubmissionService submissionService,
                                RoleGuard roleGuard) {
        this.submissionService = submissionService;
        this.roleGuard         = roleGuard;
    }

    // POST /api/execute
    @PostMapping("/execute")
    public ResponseEntity<?> executeCode(
            @RequestBody ExecutionDto.RunRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            if (request.getProblemId() == null)
                return ResponseEntity.badRequest().body("problemId is required");
            if (request.getCode() == null || request.getCode().isEmpty())
                return ResponseEntity.badRequest().body("code is required");
            if (request.getLanguage() == null || request.getLanguage().isEmpty())
                return ResponseEntity.badRequest().body("language is required");

            // userId is optional for run — null if not logged in
            Long userId = null;
            if (authHeader != null) {
                try {
                    User user = roleGuard.requireAuth(authHeader);
                    userId = user.getId();
                } catch (Exception ignored) {}
            }

            return ResponseEntity.ok(
                    submissionService.runCode(request, userId)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // POST /api/submit
    @PostMapping("/submit")
    public ResponseEntity<?> submitCode(
            @RequestBody ExecutionDto.SubmitRequest request,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = roleGuard.requireAuth(authHeader);

            if (request.getProblemId() == null)
                return ResponseEntity.badRequest().body("problemId is required");
            if (request.getCode() == null || request.getCode().isEmpty())
                return ResponseEntity.badRequest().body("code is required");
            if (request.getLanguage() == null || request.getLanguage().isEmpty())
                return ResponseEntity.badRequest().body("language is required");

            return ResponseEntity.ok(
                    submissionService.submitSolution(
                            user.getId(),
                            request.getProblemId(),
                            request.getCode(),
                            request.getLanguage()
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/submissions
    // GET /api/submissions?problemId=1
    @GetMapping("/submissions")
    public ResponseEntity<?> getSubmissions(
            @RequestParam(required = false) Long problemId,
            @RequestHeader("Authorization") String authHeader) {
        try {
            User user = roleGuard.requireAuth(authHeader);
            return ResponseEntity.ok(
                    submissionService.getSubmissions(user.getId(), problemId)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}