package in.sapphirus.rupee.market.repo;

import in.sapphirus.rupee.market.domain.Tick;
import in.sapphirus.rupee.market.domain.TickId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TickRepository extends JpaRepository<Tick, TickId> {

    @Query(value = "SELECT * FROM ticks WHERE symbol = :symbol ORDER BY time DESC LIMIT 1", nativeQuery = true)
    Optional<Tick> findLatestTick(@Param("symbol") String symbol);

    @Query(value = "SELECT ltp FROM (SELECT ltp, time FROM ticks WHERE symbol = :symbol ORDER BY time DESC LIMIT :lim) sub ORDER BY time ASC", nativeQuery = true)
    List<Double> findRecentTrend(@Param("symbol") String symbol, @Param("lim") int lim);

    @Query(value = "SELECT bucket, open, high, low, close, volume FROM candles_1min " +
                   "WHERE symbol = :symbol AND bucket >= :fromTime AND bucket <= :toTime ORDER BY bucket ASC", nativeQuery = true)
    List<Object[]> getCandles1m(@Param("symbol") String symbol, @Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = "SELECT bucket, open, high, low, close, volume FROM candles_5min " +
                   "WHERE symbol = :symbol AND bucket >= :fromTime AND bucket <= :toTime ORDER BY bucket ASC", nativeQuery = true)
    List<Object[]> getCandles5m(@Param("symbol") String symbol, @Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = "SELECT bucket, open, high, low, close, volume FROM candles_15min " +
                   "WHERE symbol = :symbol AND bucket >= :fromTime AND bucket <= :toTime ORDER BY bucket ASC", nativeQuery = true)
    List<Object[]> getCandles15m(@Param("symbol") String symbol, @Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = "SELECT bucket, open, high, low, close, volume FROM candles_1hr " +
                   "WHERE symbol = :symbol AND bucket >= :fromTime AND bucket <= :toTime ORDER BY bucket ASC", nativeQuery = true)
    List<Object[]> getCandles1h(@Param("symbol") String symbol, @Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);

    @Query(value = "SELECT bucket, open, high, low, close, volume FROM candles_1d " +
                   "WHERE symbol = :symbol AND bucket >= :fromTime AND bucket <= :toTime ORDER BY bucket ASC", nativeQuery = true)
    List<Object[]> getCandles1d(@Param("symbol") String symbol, @Param("fromTime") Instant fromTime, @Param("toTime") Instant toTime);
}
