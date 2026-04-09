package com.auth.demo.repository;

import com.auth.demo.model.CodeSnippet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CodeSnippetRepository extends JpaRepository<CodeSnippet, Long> {

    // Get all code snippets for a problem (all languages)
    List<CodeSnippet> findByProblemId(Long problemId);

    // Get code snippet for a specific problem + language
    Optional<CodeSnippet> findByProblemIdAndLanguage(Long problemId, String language);

    // Delete all snippets for a problem (used when updating/deleting problem)
    void deleteByProblemId(Long problemId);
}
