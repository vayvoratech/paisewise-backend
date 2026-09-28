package in.sapphirus.rupee.practice.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public class PaperPositionId implements Serializable {
    private UUID userId;
    private String symbol;

    public PaperPositionId() {
    }

    public PaperPositionId(UUID userId, String symbol) {
        this.userId = userId;
        this.symbol = symbol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof PaperPositionId that)) {
            return false;
        }

        return Objects.equals(userId, that.userId)
                && Objects.equals(symbol, that.symbol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, symbol);
    }
}