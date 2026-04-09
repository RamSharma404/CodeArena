package com.auth.demo.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_problems")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProblem {

    @EmbeddedId
    private UserProblemId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "solved_at")
    private LocalDateTime solvedAt;

    public enum Status {
        SOLVED, ATTEMPTED
    }

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserProblemId implements java.io.Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Column(name = "problem_id")
        private Long problemId;
    }
}