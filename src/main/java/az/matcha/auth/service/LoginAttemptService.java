package az.matcha.auth.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private record AttemptInfo(AtomicInteger count, Instant firstAttempt) {}

    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    public void loginSucceeded(String email) {
        attempts.remove(email);
    }

    public void loginFailed(String email) {
        attempts.compute(email, (key, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new AttemptInfo(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public boolean isBlocked(String email) {
        AttemptInfo info = attempts.get(email);
        if (info == null) return false;
        if (isExpired(info)) {
            attempts.remove(email);
            return false;
        }
        return info.count().get() >= MAX_ATTEMPTS;
    }

    private boolean isExpired(AttemptInfo info) {
        return Instant.now().isAfter(info.firstAttempt().plus(LOCKOUT_DURATION));
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000) // every 5 minutes
    public void evictExpired() {
        attempts.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }
}
