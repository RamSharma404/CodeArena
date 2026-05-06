package com.auth.demo.repository;

import com.auth.demo.model.ProblemStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ProblemStatsRepository
        extends JpaRepository<ProblemStats, Long> {

    Optional<ProblemStats> findByProblemId(Long problemId);

    // Count how many accepted submissions are slower than given runtime
    @Query("SELECT COUNT(s) FROM Submission s WHERE " +
            "s.problemId = :problemId AND " +
            "s.status = 'ACCEPTED' AND " +
            "s.runtimeMs > :runtimeMs AND " +
            "s.language = :language")
    Long countSlowerSubmissions(
            @Param("problemId") Long problemId,
            @Param("runtimeMs") Integer runtimeMs,
            @Param("language")  String language
    );

    // Count total accepted submissions for this problem and language
    @Query("SELECT COUNT(s) FROM Submission s WHERE " +
            "s.problemId = :problemId AND " +
            "s.status = 'ACCEPTED' AND " +
            "s.language = :language")
    Long countAcceptedByLanguage(
            @Param("problemId") Long problemId,
            @Param("language")  String language
    );
}