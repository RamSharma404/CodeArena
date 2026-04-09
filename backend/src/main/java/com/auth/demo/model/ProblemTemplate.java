package com.auth.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "problem_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProblemTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(nullable = false, length = 20)
    private String language;

    // What user sees in the editor
    @Column(name = "user_template", nullable = false, columnDefinition = "TEXT")
    private String userTemplate;

    // Hidden driver code backend wraps around user code
    @Column(name = "driver_code", nullable = false, columnDefinition = "TEXT")
    private String driverCode;
}