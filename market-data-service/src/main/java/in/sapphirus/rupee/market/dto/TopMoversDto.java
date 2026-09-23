package in.sapphirus.rupee.market.dto;

import java.util.List;

public record TopMoversDto(List<StockQuoteDto> gainers, List<StockQuoteDto> losers) {}
