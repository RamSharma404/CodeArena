package com.auth.demo.service;

import com.auth.demo.dto.ExecutionDto;
import com.auth.demo.model.Problem;
import com.auth.demo.model.Submission;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.ProblemTemplateRepository;
import com.auth.demo.repository.SubmissionRepository;
import com.auth.demo.repository.TestCaseRepository;
import com.auth.demo.repository.UserProblemRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for SubmissionService — handles code execution queueing and logic.
 */
@ExtendWith(MockitoExtension.class)
class SubmissionServiceTest {

    @Mock private SubmissionRepository submissionRepository;
    @Mock private ProblemRepository problemRepository;
    @Mock private SubmissionQueueService queueService;
    @Mock private Judge0Service judge0Service;
    @Mock private TestCaseRepository testCaseRepository;
    @Mock private ProblemTemplateRepository templateRepository;
    @Mock private UserProblemRepository userProblemRepository;
    @Mock private RankingService rankingService;

    @InjectMocks private SubmissionService submissionService;

    private Problem testProblem;

    @BeforeEach
    void setUp() {
        testProblem = Problem.builder()
                .id(1L)
                .title("Two Sum")
                .slug("two-sum")
                .build();
    }

    // ═══════════════════════════════════════════════════
    // TEST 1: Submitting code queues it successfully
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Simulates a user clicking "Submit".
    // WHY IT MATTERS: The system must create a PENDING submission, save it
    //   to the database, and push the ID to the Redis queue for the workers.
    @Test
    @DisplayName("Should create PENDING submission and push to Redis queue")
    void shouldQueueSubmissionSuccessfully() {
        when(problemRepository.findById(1L)).thenReturn(Optional.of(testProblem));
        
        // When save is called, return a submission with an ID
        when(submissionRepository.save(any(Submission.class))).thenAnswer(invocation -> {
            Submission sub = invocation.getArgument(0);
            sub.setId(100L); // Fake DB auto-increment ID
            return sub;
        });

        ExecutionDto.SubmitResponse response = submissionService.submitSolution(10L, 1L, "class Solution {}", "java");

        // Verify it returned the assigned ID and PENDING status
        assertEquals(100L, response.getSubmissionId());
        assertEquals("PENDING", response.getStatus());

        // Verify that the queue service was called to push this ID
        verify(queueService).pushToQueue(100L);
    }

    // ═══════════════════════════════════════════════════
    // TEST 2: Submitting to invalid problem → throws error
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Tries to submit code for a problem ID that doesn't exist.
    // WHY IT MATTERS: Prevents database foreign key constraint errors and
    //   provides a clean 404/error to the client.
    @Test
    @DisplayName("Should throw exception when submitting to non-existent problem")
    void shouldThrowOnInvalidProblemSubmit() {
        when(problemRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> submissionService.submitSolution(10L, 999L, "code", "java"));

        assertEquals("Problem not found", ex.getMessage());
        
        // Verify queue was NEVER called
        verify(queueService, never()).pushToQueue(anyLong());
    }
}
