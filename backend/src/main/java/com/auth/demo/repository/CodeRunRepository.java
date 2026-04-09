package com.auth.demo.repository;

import com.auth.demo.model.CodeRun;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CodeRunRepository extends JpaRepository<CodeRun, Long> {
    List<CodeRun> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<CodeRun> findByUserIdAndProblemIdOrderByCreatedAtDesc(Long userId, Long problemId);
}