
// A rate limiter controls how many requests a client can make to an API within a specific time window.
// When a request comes in, the rate limiter checks if the client has exceeded their quota.
// If they're under the limit, the request proceeds.
// If they've hit the cap, the request gets rejected.
// This protects APIs from abuse and ensures fair resource allocation across clients.

// User should be allowed to choose algorithm
// User should get a correct message about 429 or next refill time.
//  If they're under the limit, the request proceeds.
//  If they've hit the cap, the request gets rejected.

//   {
//        "endpoint": "/search",
//        "algorithm": "TokenBucket",
//        "algoConfig": {
//        "capacity": 1000,
//        "refillRatePerSecond": 10
//        }
//   }

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class RateLimiter {
    public static void main(String[] args) throws InterruptedException {

        Map<RateLimitKey, RateLimitConfig> configs = new HashMap<>();

        configs.put(new RateLimitKey("user1", "/search"), new TokenBucketConfig(3, 1));

        RateLimiterService service = new RateLimiterService(configs, new TokenBucketConfig(5, 1));

        for (int i = 0; i < 8; i++) {
            RateLimiterResult result = service.allow("user1", "/search");

            System.out.println("Request " + i + ": " + result);
            Thread.sleep(200);
        }

    }
}

class RateLimiterService {
    Map<RateLimitKey, IRateLimiter> limiters;
    IRateLimiter defaultLimiter;

    public RateLimiterService(Map<RateLimitKey, RateLimitConfig> configs, RateLimitConfig defaultRateLimitConfig) {
        Map<RateLimitKey, RateLimitConfig> configuration = RateLimitConfigLoader.load(configs);
        limiters = new ConcurrentHashMap<>();
        for (Map.Entry<RateLimitKey, RateLimitConfig> entry : configuration.entrySet()) {
            RateLimitKey key = entry.getKey();
            RateLimitConfig rateLimitConfig = entry.getValue();
            limiters.put(entry.getKey(), RateLimiterFactory.create(rateLimitConfig));
        }
        this.defaultLimiter = RateLimiterFactory.create(defaultRateLimitConfig);
    }

    public RateLimiterResult allow(String clientId, String endPoint) {
        RateLimitKey key = new RateLimitKey(clientId, endPoint);
        IRateLimiter rateLimiter = limiters.get(key);
        if (rateLimiter == null) rateLimiter = defaultLimiter;
        return rateLimiter.allowRequests();
    }
}

class RateLimitConfigLoader {
    public static Map<RateLimitKey, RateLimitConfig> load(Map<RateLimitKey, RateLimitConfig> configs) {

        // load from configs
        // Testing purpose , i am adding data hardcoded.
        configs.put(new RateLimitKey("user1", "/api/payments"), new TokenBucketConfig(5, 60));

        configs.put(new RateLimitKey("user1", "/api/orders"), new FixedWindowConfig(10, 60));

        return configs;
    }
}

interface RateLimitConfig {
    AlgorithmType getType();
}

class FixedWindowConfig implements RateLimitConfig {
    int capacity;
    int windowSizeSec;

    public FixedWindowConfig(int capacity, int windowSizeSec) {
        this.capacity = capacity;
        this.windowSizeSec = windowSizeSec;
    }

    public AlgorithmType getType() {
        return AlgorithmType.FIXED_WINDOW;
    }
}

class SlidingWindowConfig implements RateLimitConfig {
    int capacity;
    int windowSizeSec;

    public SlidingWindowConfig(int capacity, int windowSizeSec) {
        this.capacity = capacity;
        this.windowSizeSec = windowSizeSec;
    }

    public AlgorithmType getType() {
        return AlgorithmType.SLIDING_WINDOW;
    }
}

class TokenBucketConfig implements RateLimitConfig {

    int capacity;
    int refillRate;

    public TokenBucketConfig(int capacity, int refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
    }

    @Override
    public AlgorithmType getType() {
        return AlgorithmType.TOKEN_BUCKET;
    }
}

class RateLimitKey {
    String userId;
    String endpoint;

    public RateLimitKey(String userId, String endpoint) {
        this.userId = userId;
        this.endpoint = endpoint;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RateLimitKey)) return false;
        RateLimitKey that = (RateLimitKey) o;
        return userId.equals(that.userId) && endpoint.equals(that.endpoint);
    }

    @Override
    public int hashCode() {
        return userId.hashCode() * 31 + endpoint.hashCode();
    }
}

interface IRateLimiter {
    RateLimiterResult allowRequests();
}

class RateLimiterFactory {
    public static IRateLimiter create(RateLimitConfig rateLimitConfig) {

        switch (rateLimitConfig.getType()) {
            case AlgorithmType.TOKEN_BUCKET:
                TokenBucketConfig tokenBucketConfig = (TokenBucketConfig) rateLimitConfig;
                return new TokenRateLimiter(tokenBucketConfig.capacity, tokenBucketConfig.refillRate);

            case AlgorithmType.SLIDING_WINDOW:
                SlidingWindowConfig slidingWindowConfig = (SlidingWindowConfig) rateLimitConfig;
                return new SlidingWindowLimiter(slidingWindowConfig.capacity, slidingWindowConfig.windowSizeSec);

            case AlgorithmType.FIXED_WINDOW:
                FixedWindowConfig fixedWindowConfig = (FixedWindowConfig) rateLimitConfig;
                return new FixedWindowLimiter(fixedWindowConfig.windowSizeSec, fixedWindowConfig.capacity);

            default:
                throw new IllegalArgumentException("Unsupported algorithm");
        }
    }
}

class FixedWindowLimiter implements IRateLimiter {

    long maxWindowSize;
    int capacity;
    long windowStartTime;
    int usedToken;

    public FixedWindowLimiter(int maxWindowSize, int capacity) {
        this.maxWindowSize = maxWindowSize;
        this.capacity = capacity;
        this.windowStartTime = System.currentTimeMillis();
        this.usedToken = 0;
    }

    @Override
    public synchronized RateLimiterResult allowRequests() {
        long now = System.currentTimeMillis();

        if (now - windowStartTime >= this.maxWindowSize) {
            windowStartTime = now;
            usedToken = 0;
        }

        if (usedToken < capacity) {
            usedToken++;
            return new RateLimiterResult(true, 0);
        }

        long retryAfterMs = maxWindowSize - (now - windowStartTime);

        return new RateLimiterResult(false, retryAfterMs);
    }
}

class SlidingWindowLimiter implements IRateLimiter {

    int capacity;
    long maxWindowSizeMillis;

    Deque<Long> timestampsDeque = new ArrayDeque<>();

    public SlidingWindowLimiter(int capacity, long windowSizeMillis) {
        this.capacity = capacity;
        this.maxWindowSizeMillis = windowSizeMillis;
    }

    @Override
    public synchronized RateLimiterResult allowRequests() {

        long now = System.currentTimeMillis();

        while (!timestampsDeque.isEmpty() && now - timestampsDeque.pollFirst() >= this.maxWindowSizeMillis) {
            timestampsDeque.pop();
        }

        if (timestampsDeque.size() < capacity) {
            timestampsDeque.add(now);
            return new RateLimiterResult(true, 0);
        }

        long retryAfterMs = !timestampsDeque.isEmpty() ? maxWindowSizeMillis - (now - timestampsDeque.peekFirst()) : 0;

        return new RateLimiterResult(false, retryAfterMs);
    }
}

class TokenRateLimiter implements IRateLimiter {

    int capacity;
    int refillRatePerSecond;

    double tokens;
    long lastRefillTime;

    public TokenRateLimiter(int capacity, int refillRatePerSecond) {
        this.capacity = capacity;
        // refill in seconds.
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = capacity;
        this.lastRefillTime = System.currentTimeMillis();
    }

    @Override
    public synchronized RateLimiterResult allowRequests() {
        refill();

        if (tokens >= 1) {
            tokens--;
            return new RateLimiterResult(true,  0);
        }

        long retryAfter = (long) (1000 * Math.ceil(tokens) / refillRatePerSecond);
        return new RateLimiterResult(false,  retryAfter);

    }

    private void refill() {
        long now = System.currentTimeMillis();
        double tokensToAdd = (now - lastRefillTime) / 1000.0 * refillRatePerSecond;

        if (tokensToAdd > 0) {
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }
    }


}

enum AlgorithmType {
    TOKEN_BUCKET, FIXED_WINDOW, SLIDING_WINDOW
}

class RateLimiterResult {

    boolean allowed;
    long retryAfterMs;

    public RateLimiterResult(boolean allowed, long retryAfterMs) {
        this.allowed = allowed;
        this.retryAfterMs = retryAfterMs;
    }

    @Override
    public String toString() {
        return "RateLimiterResult{" +
                "allowed=" + allowed +
                ", retryAfterMs=" + retryAfterMs +
                '}';
    }
}

