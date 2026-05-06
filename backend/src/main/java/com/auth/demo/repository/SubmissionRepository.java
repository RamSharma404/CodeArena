package com.auth.demo.repository;

import com.auth.demo.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubmissionRepository
        extends JpaRepository<Submission, Long> {

    List<Submission> findByUserIdOrderBySubmittedAtDesc(Long userId);

    List<Submission> findByUserIdAndProblemIdOrderBySubmittedAtDesc(
            Long userId, Long problemId);

    @org.springframework.data.jpa.repository.Query("SELECT s.runtimeMs FROM Submission s WHERE s.problemId = :problemId AND s.status = 'ACCEPTED' AND s.runtimeMs IS NOT NULL")
    List<Integer> findRuntimesByProblemIdAndStatus(@org.springframework.data.repository.query.Param("problemId") Long problemId);

    @org.springframework.data.jpa.repository.Query("SELECT s.runtimeMs FROM Submission s WHERE s.problemId = :problemId AND s.status = 'ACCEPTED' AND s.runtimeMs IS NOT NULL AND s.language = :language")
    List<Integer> findRuntimesByProblemIdAndStatusAndLanguage(
            @org.springframework.data.repository.query.Param("problemId") Long problemId,
            @org.springframework.data.repository.query.Param("language") String language);
}