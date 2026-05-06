package com.auth.demo.controller;

import com.auth.demo.dto.ProblemDto;
import com.auth.demo.model.User;
import com.auth.demo.security.RoleGuard;
import com.auth.demo.service.ProblemService;
import com.auth.demo.service.RankingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/problems")
@CrossOrigin(origins = "${cors.allowed-origins}", allowCredentials = "true")
public class ProblemController {

    private final ProblemService problemService;
    private final RoleGuard roleGuard;
    private final RankingService rankingService;

    public ProblemController(ProblemService problemService,
                             RoleGuard roleGuard, RankingService rankingService) {
        this.problemService = problemService;
        this.roleGuard = roleGuard;
        this.rankingService = rankingService;

    }

    // ─── PUBLIC — anyone can view problems ───────────────────

    // GET /api/problems
    // GET /api/problems?difficulty=EASY
    // GET /api/problems?search=two
    // GET /api/problems?difficulty=MEDIUM&search=sum
    @GetMapping
    public ResponseEntity<?> getProblems(
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String search,
            HttpServletRequest httpRequest) {
        try {
            // userId is optional — used to show solved status
            Long userId = null;
            try {
                User user = roleGuard.requireAuth(httpRequest);
                userId = user.getId();
            } catch (Exception ignored) {}
            return ResponseEntity.ok(problemService.getProblems(difficulty, search, userId));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/problems/1
    @GetMapping("/{id}")
    public ResponseEntity<?> getProblemById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(problemService.getProblemById(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // GET /api/problems/slug/two-sum
    @GetMapping("/slug/{slug}")
    public ResponseEntity<?> getProblemBySlug(@PathVariable String slug) {
        try {
            return ResponseEntity.ok(problemService.getProblemBySlug(slug));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // GET /api/problems/{id}/template?language=python
    @GetMapping("/{id}/template")
    public ResponseEntity<?> getTemplate(
            @PathVariable Long id,
            @RequestParam String language) {
        try {
            return ResponseEntity.ok(problemService.getTemplate(id, language));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ─── ADMIN ONLY — create, update, delete ─────────────────

    // POST /api/problems
    @PostMapping
    public ResponseEntity<?> createProblem(
            @RequestBody ProblemDto.CreateProblemRequest request,
            HttpServletRequest httpRequest) {
        try {
            roleGuard.requireAdmin(httpRequest);
            return ResponseEntity.ok(problemService.createProblem(request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // PUT /api/problems/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProblem(
            @PathVariable Long id,
            @RequestBody ProblemDto.CreateProblemRequest request,
            HttpServletRequest httpRequest) {
        try {
            roleGuard.requireAdmin(httpRequest);  // blocks non-admins
            return ResponseEntity.ok(problemService.updateProblem(id, request));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // GET /api/problems/{id}/stats
    @GetMapping("/{id}/stats")
    public ResponseEntity<?> getProblemStats(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(rankingService.getProblemStats(id));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // DELETE /api/problems/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProblem(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        try {
            roleGuard.requireAdmin(httpRequest);  // blocks non-admins
            problemService.deleteProblem(id);
            return ResponseEntity.ok("Problem deleted successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}