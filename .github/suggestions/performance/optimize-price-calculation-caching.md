# Optimize Price Calculation with Caching

**Generated**: 2026-01-28  
**Category**: Performance  
**Priority**: Medium  
**Status**: Proposed

## Problem Statement

The `StockPriceCalculator` recalculates prices every 5 seconds for all instruments. For servers with many instruments (100+), this can become CPU-intensive as each calculation involves:
- Multi-factor analysis (25+ factors)
- Technical indicator calculations
- Historical data queries
- Random number generation

Currently, each calculation is performed from scratch without caching intermediate results.

## Proposed Solution

Implement a multi-level caching strategy for price calculations:

### 1. Factor Value Caching

Cache market factor values since they change infrequently:

```java
public class MarketFactorCache {
    private final Map<MarketFactor, CachedValue> factorCache = new ConcurrentHashMap<>();
    private static final long CACHE_DURATION_MS = 30_000; // 30 seconds
    
    public double getFactorValue(MarketFactor factor) {
        CachedValue cached = factorCache.get(factor);
        if (cached != null && !cached.isExpired()) {
            return cached.value;
        }
        
        double value = calculateFactorValue(factor);
        factorCache.put(factor, new CachedValue(value, System.currentTimeMillis()));
        return value;
    }
    
    private static class CachedValue {
        final double value;
        final long timestamp;
        
        CachedValue(double value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
        
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CACHE_DURATION_MS;
        }
    }
}
```

### 2. Technical Indicator Caching

Cache calculated technical indicators (moving averages, etc.):

```java
public class TechnicalIndicatorCache {
    private final Map<String, MovingAverageData> maCache = new ConcurrentHashMap<>();
    
    public MovingAverageData getMovingAverages(String symbol) {
        MovingAverageData cached = maCache.get(symbol);
        if (cached != null && cached.isValid()) {
            return cached;
        }
        
        MovingAverageData calculated = calculateMovingAverages(symbol);
        maCache.put(symbol, calculated);
        return calculated;
    }
}
```

### 3. Incremental Updates

Instead of recalculating from scratch, calculate price deltas:

```java
public double calculatePriceUpdate(InstrumentState currentState) {
    // Only calculate delta from current price
    double priceDelta = calculateDelta(currentState);
    return currentState.getCurrentPrice() + priceDelta;
}
```

## Benefits

- **Reduced CPU Usage**: Estimated 40-60% reduction in price calculation overhead
- **Improved Scalability**: Support more instruments without performance degradation
- **Faster Updates**: Price updates can complete quicker, allowing for more frequent updates
- **Lower Latency**: Reduced lag spikes during price update cycles

## Considerations

- **Memory Usage**: Caching adds memory overhead (estimated ~1-2 MB for 100 instruments)
- **Cache Invalidation**: Need to properly invalidate cache on market events
- **Staleness**: Must ensure cached values don't become too stale
- **Complexity**: Adds caching layer that needs maintenance
- **Thread Safety**: Requires proper synchronization in multi-threaded environment

## Implementation Notes

### Phase 1: Market Factor Caching
1. Create `MarketFactorCache` class
2. Integrate with `StockPriceCalculator`
3. Add configuration for cache duration
4. Test with various cache durations

### Phase 2: Technical Indicator Caching
1. Create `TechnicalIndicatorCache` class
2. Cache moving averages and other expensive calculations
3. Invalidate cache when historical data updates

### Phase 3: Incremental Updates
1. Refactor price calculation to use deltas
2. Maintain state between updates
3. Benchmark performance improvements

### Configuration

Add to `market.yml`:

```yaml
price-calculation:
  caching:
    enabled: true
    factor-cache-duration: 30  # seconds
    indicator-cache-duration: 10  # seconds
```

## Testing Strategy

1. **Performance Benchmarks**: Measure CPU usage before and after
2. **Load Testing**: Test with 100, 500, 1000 instruments
3. **Cache Hit Rates**: Monitor cache effectiveness
4. **Accuracy Testing**: Ensure cached calculations match non-cached
5. **Memory Profiling**: Monitor memory usage increase

## Related Files

- `src/main/java/net/cyberneticforge/quickstocks/core/algorithms/StockPriceCalculator.java`
- `src/main/java/net/cyberneticforge/quickstocks/core/services/StockMarketService.java`
- `src/main/resources/market.yml`

## Metrics to Track

- Average price calculation time (before/after)
- CPU usage during price updates
- Memory usage
- Cache hit rate
- Number of calculations per second

## Alternative Approaches

1. **Lazy Evaluation**: Only calculate prices when requested
2. **Sampling**: Update only subset of instruments each cycle
3. **Pre-computation**: Pre-calculate common scenarios
4. **Distributed Calculation**: Use multiple threads for calculation
