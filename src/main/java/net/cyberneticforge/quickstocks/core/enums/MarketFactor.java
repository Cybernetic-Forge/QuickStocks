package net.cyberneticforge.quickstocks.core.enums;

import lombok.Getter;

/**
 * Represents various market factors that influence stock prices in the simulation.
 * Each factor has a weight indicating its impact strength and volatility representing
 * how much it can fluctuate.
 */
@SuppressWarnings("unused")
@Getter
public enum MarketFactor {
    // Economic Indicators
    INFLATION_RATE(0.15, 0.3, "Changes in inflation affect currency value and purchasing power"),
    INTEREST_RATES(0.20, 0.25, "Central bank interest rates impact borrowing costs and investment flows"),
    GDP_GROWTH(0.18, 0.2, "Economic growth indicators influence overall market confidence"),
    UNEMPLOYMENT_RATE(0.12, 0.35, "Employment levels affect consumer spending and economic stability"),
    
    // Market Sentiment
    INVESTOR_CONFIDENCE(0.25, 0.8, "Overall market sentiment and investor psychology"),
    FEAR_GREED_INDEX(0.20, 0.9, "Market emotion index ranging from extreme fear to extreme greed"),
    MEDIA_SENTIMENT(0.08, 0.6, "News coverage and media perception impact"),
    
    // Industry Specific
    SECTOR_PERFORMANCE(0.30, 0.4, "Performance of the specific industry sector"),
    COMMODITY_PRICES(0.15, 0.5, "Raw material and commodity cost fluctuations"),
    REGULATORY_CHANGES(0.10, 0.7, "Government regulations and policy changes"),
    
    // Global Events
    GEOPOLITICAL_EVENTS(0.12, 1.0, "International conflicts, trade wars, political instability"),
    NATURAL_DISASTERS(0.08, 0.9, "Environmental events affecting markets and supply chains"),
    PANDEMIC_IMPACT(0.20, 0.8, "Health crises affecting global economy"),
    
    // Technical Factors
    TRADING_VOLUME(0.15, 0.3, "Amount of shares being traded"),
    MARKET_LIQUIDITY(0.10, 0.4, "Ease of buying/selling without affecting price"),
    ALGORITHMIC_TRADING(0.12, 0.6, "Impact of automated trading systems"),
    
    // Company Specific (for individual stocks)
    EARNINGS_REPORTS(0.35, 0.5, "Company financial performance and forecasts"),
    DIVIDEND_ANNOUNCEMENTS(0.08, 0.3, "Dividend policy changes"),
    MANAGEMENT_CHANGES(0.10, 0.4, "Leadership and strategic direction changes"),
    PRODUCT_LAUNCHES(0.15, 0.6, "New products or services introduction"),
    
    // Seasonal/Cyclical
    SEASONAL_TRENDS(0.12, 0.3, "Predictable seasonal market patterns"),
    HOLIDAY_EFFECTS(0.05, 0.4, "Market behavior around holidays"),
    QUARTERLY_CYCLES(0.08, 0.2, "Regular quarterly business cycles"),
    
    // Random Events
    MARKET_MANIPULATION(0.05, 1.0, "Artificial price movements"),
    FLASH_CRASHES(0.02, 1.5, "Sudden, severe market drops"),
    SOCIAL_MEDIA_BUZZ(0.10, 0.8, "Viral social media trends affecting stocks");

    private final double baseWeight;
    private final double volatility;
    private final String description;
    
    MarketFactor(double baseWeight, double volatility, String description) {
        this.baseWeight = baseWeight;
        this.volatility = volatility;
        this.description = description;
    }

    /**
     * @return True if this is a high-impact factor (weight > 0.2)
     */
    public boolean isHighImpact() {
        return baseWeight > 0.2;
    }
    
    /**
     * @return True if this is a highly volatile factor (volatility > 0.7)
     */
    public boolean isHighVolatility() {
        return volatility > 0.7;
    }
}