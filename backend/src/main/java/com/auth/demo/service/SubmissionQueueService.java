package com.auth.demo.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class SubmissionQueueService {

    // Redis queue name
    private static final String QUEUE_KEY = "submission_queue";

    private final RedisTemplate<String, String> redisTemplate;

    public SubmissionQueueService(
            RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // Push submissionId to left of queue
    public void pushToQueue(Long submissionId) {
        redisTemplate.opsForList()
                .leftPush(QUEUE_KEY, submissionId.toString());
        System.out.println("Pushed submission "
                + submissionId + " to queue");
    }

    // Pop submissionId from right of queue
    // Blocks for up to 10 seconds waiting for a job
    public String popFromQueue() {
        return redisTemplate.opsForList()
                .rightPop(QUEUE_KEY);
    }

    // Get queue length
    public Long getQueueLength() {
        return redisTemplate.opsForList().size(QUEUE_KEY);
    }
}