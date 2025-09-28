# Price Threshold Configuration

This document explains the new price threshold system that prevents excessive stock price growth when there is low trading activity.

## Overview

The price threshold system addresses the issue where stocks can grow exorbitantly (reaching millions in minutes) when there's no active trading. It applies progressive dampening to price movements when certain conditions are met, helping maintain market balance without completely stopping growth.

## Configuration Options

Add these settings to your `config.yml` under the `market.priceThreshold` section:

```yaml
market:
  priceThreshold:
    enabled: true
    maxChangePercent: 0.15
    priceMultiplierThreshold: 5.0
    dampeningFactor: 0.3
    minVolumeThreshold: 100
    volumeSensitivity: 0.5
```

### Setting Details

| Setting | Default | Description |
|---------|---------|-------------|
| `enabled` | `true` | Enable/disable the entire threshold system |
| `maxChangePercent` | `0.15` | Maximum price change per update before dampening (15%) |
| `priceMultiplierThreshold` | `5.0` | Price multiplier above which dampening starts (500% of initial price) |
| `dampeningFactor` | `0.3` | Dampening strength when applied (30% of original impact) |
| `minVolumeThreshold` | `100` | Minimum trading volume to ignore threshold |
| `volumeSensitivity` | `0.5` | How sensitive dampening is to volume changes |

## How It Works

### 1. Threshold Detection
The system monitors each stock's price relative to its initial price. When a stock exceeds the `priceMultiplierThreshold` (default: 5x initial price) AND the proposed price change exceeds `maxChangePercent`, dampening may be applied.

### 2. Trading Volume Consideration
Before applying dampening, the system checks recent trading activity:
- If trading volume ≥ `minVolumeThreshold`, dampening is reduced or eliminated
- High trading activity indicates genuine market interest, so less restriction is applied
- Low trading activity suggests artificial growth, triggering stronger dampening

### 3. Dampening Application
When conditions are met:
- Base dampening reduces price impact to `dampeningFactor` of original (default: 30%)
- Volume-based adjustments can reduce dampening strength
- The system never completely stops price movement, only slows it down

## Example Scenarios

### Scenario 1: Natural Growth with Trading
- Stock: $100 → $600 (6x growth)
- Trading volume: 150 shares this minute
- Proposed change: +20%
- **Result**: No dampening (high volume indicates real interest)

### Scenario 2: Artificial Growth without Trading
- Stock: $100 → $600 (6x growth)  
- Trading volume: 5 shares this minute
- Proposed change: +20%
- **Result**: Dampening applied (20% → 6% change)

### Scenario 3: Normal Growth
- Stock: $100 → $300 (3x growth)
- Any trading volume
- Proposed change: +20%
- **Result**: No dampening (below 5x threshold)

## Tuning Recommendations

### Conservative Settings (More Restriction)
```yaml
maxChangePercent: 0.10        # 10% max change
priceMultiplierThreshold: 3.0 # 300% threshold
dampeningFactor: 0.2          # 20% dampening
minVolumeThreshold: 50        # Lower volume requirement
```

### Liberal Settings (Less Restriction)
```yaml
maxChangePercent: 0.25        # 25% max change
priceMultiplierThreshold: 10.0 # 1000% threshold
dampeningFactor: 0.5          # 50% dampening
minVolumeThreshold: 200       # Higher volume requirement
```

### Disable System
```yaml
enabled: false
```

## Monitoring

The system logs when dampening is applied:
- Check server logs for "Applied threshold dampening" messages
- Monitor stock price multipliers to see which stocks are affected
- Track trading volumes to understand market activity

## Technical Implementation

- **PriceThresholdController**: Manages threshold logic and trading activity tracking
- **StockPriceCalculator**: Applies dampening during price calculations
- **TradingService**: Records trading activity for volume tracking
- **SimulationEngine**: Resets activity counters every 60 seconds

The system operates transparently within existing market simulation without requiring database changes or external dependencies.