package com.auth.demo.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "code_snippets",
       uniqueConstraints = @UniqueConstraint(columnNames = {"problem_id", "language"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSnippet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "problem_id", nullable = false)
    private Long problemId;

    @Column(nullable = false, length = 20)
    private String language;  // JAVA, PYTHON, CPP, JAVASCRIPT

    @Column(name = "solution_template", nullable = false, columnDefinition = "TEXT")
    private String solutionTemplate;  // starter code the user sees

    @Column(name = "driver_code", nullable = false, columnDefinition = "TEXT")
    private String driverCode;  // wrapper with {{USER_CODE}} placeholder
}
