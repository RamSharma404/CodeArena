package com.auth.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problem_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false, unique = true)
    private Long problemId;

    @Column(name = "total_submissions")
    private Integer totalSubmissions = 0;

    @Column(name = "accepted_submissions")
    private Integer acceptedSubmissions = 0;

    @Column(name = "avg_runtime_ms")
    private Integer avgRuntimeMs = 0;

    @Column(name = "min_runtime_ms")
    private Integer minRuntimeMs = 0;

    @Column(name = "max_runtime_ms")
    private Integer maxRuntimeMs = 0;
}