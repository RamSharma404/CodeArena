package com.auth.demo.dto;

import java.util.List;
import java.util.Map;

public class ProfileStatsDto {
    private String username;
    private String email;
    private String avatarUrl; // currently we don't have avatars, but we can return default
    private int totalSubmissions;
    private int acceptedSubmissions;
    private double acceptanceRate;
    
    // Overall completion
    private int totalSolved;
    private int totalProblems;
    
    // Difficulty breakdown
    private int easySolved;
    private int totalEasy;
    private int mediumSolved;
    private int totalMedium;
    private int hardSolved;
    private int totalHard;
    
    // Language breakdown
    private Map<String, Integer> languageStats;
    
    // Recent submissions
    private List<SubmissionSummaryDto> recentSubmissions;

    public ProfileStatsDto() {}

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public int getTotalSubmissions() { return totalSubmissions; }
    public void setTotalSubmissions(int totalSubmissions) { this.totalSubmissions = totalSubmissions; }
    public int getAcceptedSubmissions() { return acceptedSubmissions; }
    public void setAcceptedSubmissions(int acceptedSubmissions) { this.acceptedSubmissions = acceptedSubmissions; }
    public double getAcceptanceRate() { return acceptanceRate; }
    public void setAcceptanceRate(double acceptanceRate) { this.acceptanceRate = acceptanceRate; }
    public int getTotalSolved() { return totalSolved; }
    public void setTotalSolved(int totalSolved) { this.totalSolved = totalSolved; }
    public int getTotalProblems() { return totalProblems; }
    public void setTotalProblems(int totalProblems) { this.totalProblems = totalProblems; }
    public int getEasySolved() { return easySolved; }
    public void setEasySolved(int easySolved) { this.easySolved = easySolved; }
    public int getTotalEasy() { return totalEasy; }
    public void setTotalEasy(int totalEasy) { this.totalEasy = totalEasy; }
    public int getMediumSolved() { return mediumSolved; }
    public void setMediumSolved(int mediumSolved) { this.mediumSolved = mediumSolved; }
    public int getTotalMedium() { return totalMedium; }
    public void setTotalMedium(int totalMedium) { this.totalMedium = totalMedium; }
    public int getHardSolved() { return hardSolved; }
    public void setHardSolved(int hardSolved) { this.hardSolved = hardSolved; }
    public int getTotalHard() { return totalHard; }
    public void setTotalHard(int totalHard) { this.totalHard = totalHard; }
    public Map<String, Integer> getLanguageStats() { return languageStats; }
    public void setLanguageStats(Map<String, Integer> languageStats) { this.languageStats = languageStats; }
    public List<SubmissionSummaryDto> getRecentSubmissions() { return recentSubmissions; }
    public void setRecentSubmissions(List<SubmissionSummaryDto> recentSubmissions) { this.recentSubmissions = recentSubmissions; }
    
    public static class SubmissionSummaryDto {
        private Long id;
        private Long problemId;
        private String problemTitle;
        private String language;
        private String status;
        private java.time.LocalDateTime submittedAt;
        private Integer runtimeMs;
        private String difficulty;

        public SubmissionSummaryDto() {}

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getProblemId() { return problemId; }
        public void setProblemId(Long problemId) { this.problemId = problemId; }
        public String getProblemTitle() { return problemTitle; }
        public void setProblemTitle(String problemTitle) { this.problemTitle = problemTitle; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public java.time.LocalDateTime getSubmittedAt() { return submittedAt; }
        public void setSubmittedAt(java.time.LocalDateTime submittedAt) { this.submittedAt = submittedAt; }
        public Integer getRuntimeMs() { return runtimeMs; }
        public void setRuntimeMs(Integer runtimeMs) { this.runtimeMs = runtimeMs; }
        public String getDifficulty() { return difficulty; }
        public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    }
}
