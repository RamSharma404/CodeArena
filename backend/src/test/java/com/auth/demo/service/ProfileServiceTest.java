package com.auth.demo.service;

import com.auth.demo.dto.ProfileStatsDto;
import com.auth.demo.model.Problem;
import com.auth.demo.model.Submission;
import com.auth.demo.model.User;
import com.auth.demo.repository.ProblemRepository;
import com.auth.demo.repository.SubmissionRepository;
import com.auth.demo.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests for ProfileService — the service that builds the user's profile
 * statistics page (solved counts, acceptance rate, difficulty breakdown, etc.)
 */
@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    // ─── Fake (mock) versions of the repositories ───
    @Mock private UserRepository userRepository;
    @Mock private SubmissionRepository submissionRepository;
    @Mock private ProblemRepository problemRepository;

    // ─── The real service, with fakes injected into it ───
    @InjectMocks private ProfileService profileService;

    // Reusable test data
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("ram")
                .email("ram@test.com")
                .password("hashed")
                .name("Ram Sharma")
                .bio("I love DSA")
                .build();
    }

    // ═══════════════════════════════════════════════════
    // TEST 1: Basic profile fields are returned correctly
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Calls getUserProfileStats and checks that the username,
    //   email, name, and bio from the database are correctly mapped to the DTO.
    // WHY IT MATTERS: If someone refactors the mapping code and accidentally
    //   swaps email and username, this test will catch it immediately.
    @Test
    @DisplayName("Should return correct user info (username, email, name, bio)")
    void shouldReturnCorrectUserInfo() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of());
        when(problemRepository.findAll()).thenReturn(List.of());

        ProfileStatsDto result = profileService.getUserProfileStats(1L);

        assertEquals("ram", result.getUsername());
        assertEquals("ram@test.com", result.getEmail());
        assertEquals("Ram Sharma", result.getName());
        assertEquals("I love DSA", result.getBio());
    }

    // ═══════════════════════════════════════════════════
    // TEST 2: Acceptance rate is calculated correctly
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Creates 10 submissions (7 ACCEPTED, 3 WRONG_ANSWER),
    //   calls the service, and verifies acceptance rate = 70.0%
    // WHY IT MATTERS: The acceptance rate formula (accepted/total * 100)
    //   involves floating-point math. If rounding changes, this catches it.
    @Test
    @DisplayName("Should calculate acceptance rate as 70% for 7/10 accepted")
    void shouldCalculateAcceptanceRateCorrectly() {
        List<Submission> submissions = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            submissions.add(Submission.builder()
                    .status(Submission.Status.ACCEPTED)
                    .problemId((long) (i % 3 + 1))
                    .language("java")
                    .build());
        }
        for (int i = 0; i < 3; i++) {
            submissions.add(Submission.builder()
                    .status(Submission.Status.WRONG_ANSWER)
                    .problemId(4L)
                    .language("java")
                    .build());
        }

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(submissions);
        when(problemRepository.findAll()).thenReturn(List.of());

        ProfileStatsDto result = profileService.getUserProfileStats(1L);

        assertEquals(10, result.getTotalSubmissions());
        assertEquals(7, result.getAcceptedSubmissions());
        assertEquals(70.0, result.getAcceptanceRate());
    }

    // ═══════════════════════════════════════════════════
    // TEST 3: Zero submissions → 0% acceptance rate (no division by zero!)
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Tests the edge case where a brand new user has 0 submissions.
    // WHY IT MATTERS: Without proper guarding, 0/0 would cause ArithmeticException
    //   or return NaN. This verifies we safely return 0.0%.
    @Test
    @DisplayName("Should return 0% acceptance rate when user has no submissions")
    void shouldHandleZeroSubmissions() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(List.of());
        when(problemRepository.findAll()).thenReturn(List.of());

        ProfileStatsDto result = profileService.getUserProfileStats(1L);

        assertEquals(0, result.getTotalSubmissions());
        assertEquals(0, result.getAcceptedSubmissions());
        assertEquals(0.0, result.getAcceptanceRate());
    }

    // ═══════════════════════════════════════════════════
    // TEST 4: Difficulty breakdown counts are correct
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Creates 3 Easy, 2 Medium, 1 Hard problem in the database,
    //   then the user solves 2 Easy and 1 Hard. Verifies the counts match.
    // WHY IT MATTERS: The profile page shows "Easy 2/3, Medium 0/2, Hard 1/1".
    //   If the grouping logic breaks, the UI would show wrong numbers.
    @Test
    @DisplayName("Should correctly count solved problems by difficulty")
    void shouldCountSolvedByDifficulty() {
        // 3 Easy, 2 Medium, 1 Hard problem in DB
        List<Problem> problems = List.of(
                Problem.builder().id(1L).difficulty(Problem.Difficulty.EASY).title("p1").slug("p1").description("d").build(),
                Problem.builder().id(2L).difficulty(Problem.Difficulty.EASY).title("p2").slug("p2").description("d").build(),
                Problem.builder().id(3L).difficulty(Problem.Difficulty.EASY).title("p3").slug("p3").description("d").build(),
                Problem.builder().id(4L).difficulty(Problem.Difficulty.MEDIUM).title("p4").slug("p4").description("d").build(),
                Problem.builder().id(5L).difficulty(Problem.Difficulty.MEDIUM).title("p5").slug("p5").description("d").build(),
                Problem.builder().id(6L).difficulty(Problem.Difficulty.HARD).title("p6").slug("p6").description("d").build()
        );

        // User solved problems 1 (Easy), 2 (Easy), 6 (Hard)
        List<Submission> submissions = List.of(
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(1L).language("java").build(),
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(2L).language("java").build(),
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(6L).language("java").build()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(submissions);
        when(problemRepository.findAll()).thenReturn(problems);

        ProfileStatsDto result = profileService.getUserProfileStats(1L);

        assertEquals(3, result.getTotalSolved());
        assertEquals(6, result.getTotalProblems());
        // Easy: solved 2 out of 3
        assertEquals(2, result.getEasySolved());
        assertEquals(3, result.getTotalEasy());
        // Medium: solved 0 out of 2
        assertEquals(0, result.getMediumSolved());
        assertEquals(2, result.getTotalMedium());
        // Hard: solved 1 out of 1
        assertEquals(1, result.getHardSolved());
        assertEquals(1, result.getTotalHard());
    }

    // ═══════════════════════════════════════════════════
    // TEST 5: Language stats are tracked correctly
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Creates submissions in java (3) and python (2), then
    //   checks the languageStats map has the right counts.
    // WHY IT MATTERS: The profile page shows "Languages Used: Java 3 subs,
    //   Python 2 subs". Wrong counting = misleading user stats.
    @Test
    @DisplayName("Should track language usage counts correctly")
    void shouldTrackLanguageStats() {
        List<Submission> submissions = List.of(
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(1L).language("java").build(),
                Submission.builder().status(Submission.Status.WRONG_ANSWER).problemId(1L).language("java").build(),
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(2L).language("java").build(),
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(3L).language("python").build(),
                Submission.builder().status(Submission.Status.WRONG_ANSWER).problemId(3L).language("python").build()
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(submissions);
        when(problemRepository.findAll()).thenReturn(List.of());

        ProfileStatsDto result = profileService.getUserProfileStats(1L);

        assertEquals(3, result.getLanguageStats().get("java"));
        assertEquals(2, result.getLanguageStats().get("python"));
    }

    // ═══════════════════════════════════════════════════
    // TEST 6: Duplicate accepted submissions on same problem count as 1 solve
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: User submits ACCEPTED on problem #1 three times.
    //   Verifies totalSolved = 1, not 3.
    // WHY IT MATTERS: Without deduplication, a user who retries a solved
    //   problem would see inflated "solved" counts.
    @Test
    @DisplayName("Should not double-count the same problem solved multiple times")
    void shouldDeduplicateSolvedProblems() {
        List<Submission> submissions = List.of(
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(1L).language("java").build(),
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(1L).language("java").build(),
                Submission.builder().status(Submission.Status.ACCEPTED).problemId(1L).language("java").build()
        );

        Problem p = Problem.builder().id(1L).difficulty(Problem.Difficulty.EASY).title("p1").slug("p1").description("d").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(submissionRepository.findByUserIdOrderBySubmittedAtDesc(1L)).thenReturn(submissions);
        when(problemRepository.findAll()).thenReturn(List.of(p));

        ProfileStatsDto result = profileService.getUserProfileStats(1L);

        assertEquals(1, result.getTotalSolved());   // Not 3!
        assertEquals(1, result.getEasySolved());
    }

    // ═══════════════════════════════════════════════════
    // TEST 7: User not found → throws RuntimeException
    // ═══════════════════════════════════════════════════
    // WHAT IT DOES: Calls getUserProfileStats with a non-existent user ID.
    // WHY IT MATTERS: The API should throw a clear error, not return null
    //   or cause a NullPointerException somewhere downstream.
    @Test
    @DisplayName("Should throw exception when user not found")
    void shouldThrowWhenUserNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> profileService.getUserProfileStats(999L));

        assertEquals("User not found", ex.getMessage());
    }
}
