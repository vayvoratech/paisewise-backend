package in.sapphirus.rupee.market.dto;

public record MarketStatusResponse(boolean isMarketOpen, String session, String nextSessionTime) {}
