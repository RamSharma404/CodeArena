package com.auth.demo.repository;

import com.auth.demo.model.Problem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

    // Find by slug (for /problems/:slug)
    Optional<Problem> findBySlug(String slug);

    // Filter by difficulty
    List<Problem> findByDifficulty(Problem.Difficulty difficulty);

    // Search by title
    List<Problem> findByTitleContainingIgnoreCase(String title);

    // Filter + search combined
    @Query("SELECT p FROM Problem p WHERE " +
            "(:difficulty IS NULL OR p.difficulty = :difficulty) AND " +
            "(:search IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<Problem> findByFilters(
            @Param("difficulty") Problem.Difficulty difficulty,
            @Param("search") String search
    );
}