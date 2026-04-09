package com.auth.demo.repository;

import com.auth.demo.model.ProblemTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProblemTemplateRepository extends JpaRepository<ProblemTemplate, Long> {

    // Get template for specific problem + language
    Optional<ProblemTemplate> findByProblemIdAndLanguage(Long problemId, String language);

    // Get all templates for a problem
    List<ProblemTemplate> findByProblemId(Long problemId);
}