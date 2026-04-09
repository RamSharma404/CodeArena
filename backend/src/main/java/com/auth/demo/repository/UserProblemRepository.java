package com.auth.demo.repository;

import com.auth.demo.model.UserProblem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserProblemRepository extends JpaRepository<UserProblem, UserProblem.UserProblemId> {

    // Get all problems attempted/solved by a user
    List<UserProblem> findByIdUserId(Long userId);

    // Check if user solved a specific problem
    Optional<UserProblem> findByIdUserIdAndIdProblemId(Long userId, Long problemId);
}