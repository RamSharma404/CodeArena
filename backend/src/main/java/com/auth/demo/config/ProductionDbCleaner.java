package com.auth.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class ProductionDbCleaner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public ProductionDbCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        System.out.println("Starting automatic production database cleanup...");
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            
            // Delete all unwanted problems and their associated records exactly as requested
            jdbcTemplate.execute("DELETE FROM problems WHERE id > 10");
            jdbcTemplate.execute("DELETE FROM problem_templates WHERE problem_id > 10");
            jdbcTemplate.execute("DELETE FROM test_cases WHERE problem_id > 10");
            jdbcTemplate.execute("DELETE FROM submissions WHERE problem_id > 10");
            jdbcTemplate.execute("DELETE FROM user_problems WHERE problem_id > 10");
            jdbcTemplate.execute("DELETE FROM problem_stats WHERE problem_id > 10");
            
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("✅ Production database cleanup successful!");
        } catch (Exception e) {
            System.err.println("❌ Failed to clean production DB: " + e.getMessage());
        }
    }
}
