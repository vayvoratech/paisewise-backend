package in.sapphirus.rupee.practice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record Tick(
    String symbol,
    Double ltp
) {}
