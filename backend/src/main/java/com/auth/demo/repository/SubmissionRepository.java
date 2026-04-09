package com.auth.demo.repository;

import com.auth.demo.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // Get all submissions by a user
    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    // Get submissions for a specific problem by a user
    List<Submission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(Long userId, Long problemId);
}