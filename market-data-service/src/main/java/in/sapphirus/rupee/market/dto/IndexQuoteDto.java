package in.sapphirus.rupee.market.dto;

import java.io.Serializable;

public class IndexQuoteDto implements Serializable {
    private static final long serialVersionUID = 1L;

    private String symbol;
    private Double value;
    private Double changePct;

    public IndexQuoteDto() {}

    public IndexQuoteDto(String symbol, Double value, Double changePct) {
        this.symbol = symbol;
        this.value = value;
        this.changePct = changePct;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }

    public Double getChangePct() { return changePct; }
    public void setChangePct(Double changePct) { this.changePct = changePct; }
}
