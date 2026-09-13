package com.mm_mk.Messaging.util;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiter {

    private static final int CHARACTERS_PER_WINDOW = 1000;
    private static final Duration WINDOW_SIZE = Duration.ofSeconds(10);

    private final ConcurrentHashMap<String, UserBucket> buckets = new ConcurrentHashMap<>();

    private static class UserBucket {
        int charactersRemaining;
        Instant windowStart;

        UserBucket() {
            this.charactersRemaining = CHARACTERS_PER_WINDOW;
            this.windowStart = Instant.now();
        }
    }

    public boolean isAllowed(String userId, int messageLength) {
        Instant now = Instant.now();
        UserBucket bucket = buckets.computeIfAbsent(userId, k -> new UserBucket());

        if (Duration.between(bucket.windowStart, now).compareTo(WINDOW_SIZE) > 0) {
            bucket.windowStart = now;
            bucket.charactersRemaining = CHARACTERS_PER_WINDOW;
        }

        if (messageLength <= bucket.charactersRemaining) {
            bucket.charactersRemaining -= messageLength;
            return true;
        }

        return false;
    }

    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(300);
        buckets.entrySet().removeIf(entry ->
            Duration.between(entry.getValue().windowStart, Instant.now()).getSeconds() > 300
        );
    }
}
