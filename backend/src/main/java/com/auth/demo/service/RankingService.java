package com.auth.demo.service;

import com.auth.demo.model.ProblemStats;
import com.auth.demo.repository.ProblemStatsRepository;
import com.auth.demo.repository.SubmissionRepository;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class RankingService {

    private final ProblemStatsRepository statsRepository;
    private final SubmissionRepository   submissionRepository;

    public RankingService(ProblemStatsRepository statsRepository,
                          SubmissionRepository submissionRepository) {
        this.statsRepository   = statsRepository;
        this.submissionRepository = submissionRepository;
    }

    // ─── Update stats after each submission ───────────────────
    public void updateStats(Long problemId, String status,
                            Integer runtimeMs) {
        ProblemStats stats = statsRepository
                .findByProblemId(problemId)
                .orElse(ProblemStats.builder()
                        .problemId(problemId)
                        .totalSubmissions(0)
                        .acceptedSubmissions(0)
                        .avgRuntimeMs(0)
                        .minRuntimeMs(Integer.MAX_VALUE)
                        .maxRuntimeMs(0)
                        .build());

        // Increment total
        stats.setTotalSubmissions(stats.getTotalSubmissions() + 1);

        // Update accepted stats
        if ("ACCEPTED".equals(status) && runtimeMs != null) {
            stats.setAcceptedSubmissions(
                    stats.getAcceptedSubmissions() + 1);

            // Update min runtime
            if (runtimeMs < stats.getMinRuntimeMs()) {
                stats.setMinRuntimeMs(runtimeMs);
            }

            // Update max runtime
            if (runtimeMs > stats.getMaxRuntimeMs()) {
                stats.setMaxRuntimeMs(runtimeMs);
            }

            // Recalculate average
            // Simple running average
            int accepted = stats.getAcceptedSubmissions();
            int newAvg   = ((stats.getAvgRuntimeMs() * (accepted - 1))
                    + runtimeMs) / accepted;
            stats.setAvgRuntimeMs(newAvg);
        }

        statsRepository.save(stats);
    }

    // ─── Calculate percentile ranking ─────────────────────────
    // Returns: "Faster than 87.5% of Python submissions"
    public Map<String, Object> getRanking(Long problemId,
                                          Integer runtimeMs,
                                          String language) {
        Map<String, Object> result = new HashMap<>();

        if (runtimeMs == null) {
            result.put("percentile", 0.0);
            result.put("message", "Runtime not available");
            return result;
        }

        // Count submissions slower than this one
        Long slowerCount = statsRepository.countSlowerSubmissions(
                problemId, runtimeMs, language.toUpperCase());

        // Count total accepted for this language
        Long totalAccepted = statsRepository.countAcceptedByLanguage(
                problemId, language.toUpperCase());

        if (totalAccepted == 0) {
            result.put("percentile", 100.0);
            result.put("message",
                    "Faster than 100.00% of " + language + " submissions");
            return result;
        }

        // Calculate percentile
        double percentile = ((double) slowerCount / totalAccepted) * 100;

        // Round to 2 decimal places
        percentile = Math.round(percentile * 100.0) / 100.0;

        result.put("percentile",  percentile);
        result.put("runtimeMs",   runtimeMs);
        result.put("language",    language);
        result.put("totalAccepted", totalAccepted);
        result.put("message",
                String.format("Faster than %.2f%% of %s submissions",
                        percentile, language));

        return result;
    }

    // ─── Get problem stats for problems list page ─────────────
    public Map<String, Object> getProblemStats(Long problemId) {
        ProblemStats stats = statsRepository
                .findByProblemId(problemId)
                .orElse(null);

        Map<String, Object> result = new HashMap<>();

        if (stats == null) {
            result.put("totalSubmissions",    0);
            result.put("acceptedSubmissions", 0);
            result.put("acceptanceRate",      0.0);
            result.put("avgRuntimeMs",        0);
            return result;
        }

        double acceptanceRate = stats.getTotalSubmissions() > 0
                ? ((double) stats.getAcceptedSubmissions()
                / stats.getTotalSubmissions()) * 100
                : 0.0;

        result.put("totalSubmissions",
                stats.getTotalSubmissions());
        result.put("acceptedSubmissions",
                stats.getAcceptedSubmissions());
        result.put("acceptanceRate",
                Math.round(acceptanceRate * 10.0) / 10.0);
        result.put("avgRuntimeMs",
                stats.getAvgRuntimeMs());
        result.put("minRuntimeMs",
                stats.getMinRuntimeMs() == Integer.MAX_VALUE
                        ? 0 : stats.getMinRuntimeMs());

        return result;
    }

    // ─── Runtime distribution for bar chart ───────────────────
    public Map<String, Object> getRuntimeDistribution(
            Long problemId, String language, Integer userRuntimeMs) {

        List<Integer> runtimes = submissionRepository
                .findRuntimesByProblemIdAndStatusAndLanguage(
                        problemId, language.toUpperCase());

        Map<String, Object> result = new HashMap<>();

        if (runtimes == null || runtimes.isEmpty()) {
            result.put("buckets", new java.util.ArrayList<>());
            result.put("userBucketIndex", -1);
            return result;
        }

        // Find min/max
        int min = runtimes.stream().mapToInt(Integer::intValue).min().orElse(0);
        int max = runtimes.stream().mapToInt(Integer::intValue).max().orElse(0);

        // Create ~10 buckets
        int numBuckets = Math.min(12, Math.max(5, runtimes.size() / 3));
        if (max == min) {
            max = min + 1;
            numBuckets = 1;
        }

        double bucketWidth = (double)(max - min) / numBuckets;

        java.util.List<Map<String, Object>> buckets = new java.util.ArrayList<>();
        int[] counts = new int[numBuckets];
        int userBucketIndex = -1;

        // Count runtimes in each bucket
        for (int rt : runtimes) {
            int idx = (int)((rt - min) / bucketWidth);
            if (idx >= numBuckets) idx = numBuckets - 1;
            counts[idx]++;
        }

        // Find user's bucket
        if (userRuntimeMs != null) {
            userBucketIndex = (int)((userRuntimeMs - min) / bucketWidth);
            if (userBucketIndex >= numBuckets) userBucketIndex = numBuckets - 1;
            if (userBucketIndex < 0) userBucketIndex = 0;
        }

        // Build bucket objects
        for (int i = 0; i < numBuckets; i++) {
            int lo = min + (int)(i * bucketWidth);
            int hi = min + (int)((i + 1) * bucketWidth);
            if (i == numBuckets - 1) hi = max;

            Map<String, Object> bucket = new HashMap<>();
            bucket.put("rangeLabel", lo + "-" + hi + "ms");
            bucket.put("lo", lo);
            bucket.put("hi", hi);
            bucket.put("count", counts[i]);
            bucket.put("isUser", i == userBucketIndex);
            buckets.add(bucket);
        }

        result.put("buckets", buckets);
        result.put("userBucketIndex", userBucketIndex);
        result.put("userRuntimeMs", userRuntimeMs);
        result.put("totalAccepted", runtimes.size());

        return result;
    }
}