package com.auth.demo.service;

import com.auth.demo.dto.ProblemDto;
import com.auth.demo.model.Problem;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.ProblemTemplateRepository;
import com.auth.demo.repository.TestCaseRepository;
import com.auth.demo.repository.UserProblemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ProblemService — handles fetching, creating, and updating problems.
 */
@ExtendWith(MockitoExtension.class)
class ProblemServiceTest {

    @Mock private ProblemRepository problemRepository;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private UserProblemRepository userProblemRepository;
    @Mock private ProblemTemplateRepository templateRepository;

    @InjectMocks private ProblemService problemService;

    private Problem testProblem;

    @BeforeEach
    void setUp() {
        testProblem = Problem.builder()
                .id(1L)
                .title("Two Sum")
                .slug("two-sum")
                .description("Find two numbers that add up to target.")
                .difficulty(Problem.Difficulty.EASY)
                .topicTags("Array,Hash Table")
                .build();
    }

    // ═══════════════════════════════════════════════════
    // TEST 1: Fetch problem by valid slug
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Searches for a problem by its URL slug.
    // WHY IT MATTERS: The frontend uses the slug to load the problem page.
    @Test
    @DisplayName("Should return ProblemDetail when given valid slug")
    void shouldReturnProblemBySlug() {
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(testProblem));
        when(testCaseRepository.findByProblemIdAndIsHidden(1L, false)).thenReturn(List.of());


        ProblemDto.ProblemDetail result = problemService.getProblemBySlug("two-sum");

        assertEquals("Two Sum", result.getTitle());
        assertEquals(Problem.Difficulty.EASY, result.getDifficulty());
    }

    // ═══════════════════════════════════════════════════
    // TEST 2: Problem not found → throws exception
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Passes an invalid slug.
    // WHY IT MATTERS: API should return 404/throw error, not crash.
    @Test
    @DisplayName("Should throw exception when slug not found")
    void shouldThrowWhenSlugNotFound() {
        when(problemRepository.findBySlug("fake-problem")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> problemService.getProblemBySlug("fake-problem"));

        assertEquals("Problem not found", ex.getMessage());
    }

    // ═══════════════════════════════════════════════════
    // TEST 3: Create Problem checks for duplicate slug
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Tries to create a new problem but the slug is already in use.
    // WHY IT MATTERS: Slugs must be unique for routing.
    @Test
    @DisplayName("Should block problem creation if slug already exists")
    void shouldBlockDuplicateSlugCreation() {
        ProblemDto.CreateProblemRequest request = new ProblemDto.CreateProblemRequest();
        request.setTitle("Two Sum");
        request.setSlug("two-sum");

        // DB says slug already exists
        when(problemRepository.findBySlug("two-sum")).thenReturn(Optional.of(testProblem));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> problemService.createProblem(request));

        assertEquals("Problem with this slug already exists", ex.getMessage());
    }
}
