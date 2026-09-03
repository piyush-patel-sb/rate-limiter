# API Rate Limiter

A thread-safe rate limiter for per-client request throttling.

```java
boolean allowRequest(String clientId);
```

Returns `true` if the request is within the client's configured limit, `false` if it has
been exceeded.

**Requirements:** Java 17, Maven. No runtime dependencies; JUnit 5 for tests.

---

## Quick start

```java
LimiterRegistry limiterRegistry = new InMemoryLimiterRegistry(100_000);
RateLimitRuleRegistry ruleRegistry = new InMemoryRateLimitRuleRegistry(limiterRegistry);

ruleRegistry.addRateLimitRule("customerA", RateLimitRule.of(100, Duration.ofMinutes(1)));
ruleRegistry.addRateLimitRule("customerB", RateLimitRule.of(1_000, Duration.ofMinutes(1)));
ruleRegistry.addRateLimitRule("customerC", RateLimitRule.of(10, Duration.ofSeconds(1)));

RateLimiter rateLimiter = RateLimiterService.builder()
        .ruleRegistry(ruleRegistry)
        .limiterRegistry(limiterRegistry)
        .algorithm(Algorithm.FIXED_WINDOW)
        .evictionIntervalInNanos(Duration.ofMinutes(10).toNanos())
        .build();

if (rateLimiter.allowRequest("customerA")) {
    // serve
} else {
    // reject with 429
}
```

A runnable example is in `com.piyush.ratelimiter.demo.Demo`.

---

## How a request flows

```
allowRequest(clientId)
        |
        v
  limiterRegistry.getLimiter(clientId)  ------- hit ------> Limiter
        |                                                     |
       miss                                                   |
        |                                                     |
        v                                                     |
  ruleRegistry.getRateLimitRule(clientId)                     |
        |                                                     |
        v                                                     |
  LimiterStrategyFactory.createLimiterStrategy(algorithm, rule)
        |                                                     |
        v                                                     |
  limiterRegistry.addLimiter(...)  -- returns the winner --> Limiter
                                                              |
                                                              v
                                                  strategy.tryAcquire(now)
```

## Thread safety

**The invariant:** for a given client id, exactly one `Limiter` is ever metered against,
and all mutation of its counters happens under that instance's lock.

Enforced in two places:

1. **Creation.** Two threads can both miss the registry for a new client and both build a
   `Limiter`. `InMemoryLimiterRegistry.addLimiter` resolves this with
   `ConcurrentHashMap.compute` and **returns the winner**, and `RateLimiterService` meters
   against that returned instance — never the one it constructed. A thread that loses the
   creation race discards its own limiter and uses the winner's.

2. **Counting.** Each strategy guards its own state. `FixedWindowStrategy` synchronises
   its counter and window id; `SlidingWindowLogStrategy` synchronises its timestamp deque.

Both strategies are verified under contention: 8 threads, 400 attempts each, against a
frozen clock, asserting that exactly `limit` of the 3,200 attempts succeed.

---

## Memory bounding

Two mechanisms, both on the limiter map only:

- **Idle eviction.** A single daemon thread sweeps on a fixed interval and drops any
  limiter whose last request is older than that interval. Last-seen is updated on every
  request including rejected ones, so a client actively being throttled is treated as
  active and is not evicted mid-window.
- **Hard cap.** `InMemoryLimiterRegistry` is constructed with a `maxSize` ceiling, so the
  map cannot outgrow the heap when new client ids arrive faster than the sweep reclaims
  old ones. The ceiling is a property of *this* implementation rather than of the
  contract — `LimiterRegistry` is the seam where an off-heap store such as Redis would
  replace it. See [Scalability](#scalability) for why that swap has to move the counting
  as well as the storage.

Per-client cost depends on the algorithm:

- `FixedWindowStrategy` — a fixed handful of fields, tens of bytes, independent of limit.
- `SlidingWindowLogStrategy` — roughly 24 bytes per retained request once boxing and the
  deque slot are counted. A 1,000 req/min client therefore costs ~24KB at saturation,
  which is why that strategy caps `limit` at 5,000 (~120KB/client worst case).

That asymmetry is the practical reason to pick fixed window at high limits and
sliding-window log where burst smoothness at a boundary matters more than memory.

---

## Extensibility

The seams are deliberate. Each of these is an implementation of an existing interface, and
no other file changes:

| To add | Implement | Notes |
|---|---|---|
| A new algorithm (token bucket, sliding-window counter) | `LimiterStrategy` | Plus one `Algorithm` constant and one `switch` arm in `LimiterStrategyFactory`. |
| Persistent or shared rule storage | `RateLimitRuleRegistry` | Rules are read on the miss path only, so a slower store is tolerable. |
| A different eviction policy or backing store | `LimiterRegistry` | E.g. a Caffeine-backed registry with `maximumSize` and `expireAfterAccess`. |
| Rules loaded from file or a control plane | `RateLimitRuleRegistry` | Configuration is programmatic today; nothing reads from disk. |

`LimiterStrategy.tryAcquire(long currentNanos)` takes time as a parameter rather than
reading the clock itself. That is what makes every strategy test deterministic.

---

## Scalability

Single node, in process. What breaks first, in order:

1. **Eviction sweep is O(n) in the number of live clients**, on one thread. Fine at 10⁴–10⁵
   clients on a multi-minute interval; at 10⁶ it wants a Caffeine-backed `LimiterRegistry`
   with `maximumSize` + `expireAfterAccess`, which amortises both the cap and the TTL and
   removes the sweep thread entirely.
2. **Horizontal scale.** N nodes each enforce the full limit, so a client behind a load
   balancer gets up to N × limit. Two ways out: sticky routing by client id (cheap, but
   rebalancing loses state and limits go soft during a deploy), or a shared store — Redis
   with a Lua script for atomic check-and-increment, trading one network hop per request
   for a correct global count.
3. **Clock.** `System.nanoTime()` has no meaning across JVMs, so any shared-store design
   needs either a single authoritative clock (the Redis server's) or an explicit skew
   tolerance at the window boundary.
4. **Lock granularity.** Each `Limiter` has its own lock, so clients never contend with
   each other; contention is only ever within one hot client. A per-client CAS loop would
   remove even that, at the cost of a more delicate implementation.

---

## Design decisions and trade-offs

| Decision | Alternative considered | Why this way                                                                                                                                                                         |
|---|---|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Algorithm behind a `LimiterStrategy` interface, selected by an `Algorithm` enum through a factory | Hard-code one algorithm | Fixed window and sliding-window log have genuinely different burst/memory profiles; the right choice is deployment-specific. Adding a third is one enum constant and one factory arm. |
| Algorithm chosen once per service | Algorithm as a field on `RateLimitRule` | Keeps `RateLimitRule` a pure configuration value with no behavioural coupling. Making it per-rule later is a few-line change.                                |
| Limiter rebuilt when a rule is written | Pass the rule into `tryAcquire` on every call | Rebuilding keeps strategies free of configuration state. Cost: the client's consumed quota resets — see *Known limitations*.                                                         |
| `System.nanoTime()` | `System.currentTimeMillis()` | Monotonic; immune to NTP steps and DST. Windows must not move backwards.                                                                                                             |

---

## Known limitations and next steps

- **No clock abstraction.** `RateLimiterService` calls `System.nanoTime()` directly, so
  service-level time behaviour cannot be tested without real sleeps — the eviction test
  spends two seconds in `Thread.sleep`. An injected `TimeSource` (defaulting to
  `System::nanoTime`) would make window expiry and eviction deterministic. First thing I'd add.
- **A rule write resets the client's consumed quota.** Rebuilding the limiter is what makes
  a new limit take effect, but it is currently unconditional, so re-pushing an *unchanged*
  rule also refills the client. The fix is to compare `limit`/`period` and only rebuild
  when enforcement actually changed.
- **A full registry rejects rather than evicts.** At `maxSize`, `addLimiter` throws instead
  of dropping the least recently used client. Since a limiter is derived state that is
  rebuilt for free, a full registry should degrade by forgetting, never by refusing a
  configured client.
- **No observability.** Allow/deny rates, eviction counts and registry size are what you
  would alert on; there is no hook for them yet.
- **One interval knob.** The eviction sweep period and the idle TTL are the same value.
  They should be independently configurable.

---

