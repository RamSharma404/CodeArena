package com.auth.demo.repository;

import com.auth.demo.model.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {

    // Get all test cases for a problem
    List<TestCase> findByProblemId(Long problemId);

    // Get only visible test cases (shown to user)
    List<TestCase> findByProblemIdAndIsHidden(Long problemId, Boolean isHidden);
}