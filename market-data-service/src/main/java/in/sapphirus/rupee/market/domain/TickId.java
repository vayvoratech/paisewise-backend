package in.sapphirus.rupee.market.domain;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class TickId implements Serializable {
    private Instant time;
    private String symbol;

    public TickId() {}

    public TickId(Instant time, String symbol) {
        this.time = time;
        this.symbol = symbol;
    }

    public Instant getTime() { return time; }
    public String getSymbol() { return symbol; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TickId tickId = (TickId) o;
        return Objects.equals(time, tickId.time) && Objects.equals(symbol, tickId.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(time, symbol);
    }
}
