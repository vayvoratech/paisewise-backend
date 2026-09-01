package in.sapphirus.rupee.market;

import in.sapphirus.rupee.market.domain.Symbol;
import in.sapphirus.rupee.market.domain.Tick;
import in.sapphirus.rupee.market.dto.TopMoversDto;
import in.sapphirus.rupee.market.repo.SymbolRepository;
import in.sapphirus.rupee.market.repo.TickRepository;
import in.sapphirus.rupee.market.service.MarketDataService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MarketDataServiceTest {

    @Mock
    private SymbolRepository symbolRepository;

    @Mock
    private TickRepository tickRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MarketDataService service;

    @BeforeEach
    public void setUp() {
        service = new MarketDataService(symbolRepository, tickRepository, redisTemplate, objectMapper);
    }

    @Test
    public void testCalculateTopMovers_Fallback() {
        when(tickRepository.findLatestTicksForAllSymbols()).thenReturn(Collections.emptyList());

        TopMoversDto movers = service.calculateTopMovers();

        assertNotNull(movers);
        assertEquals(5, movers.gainers().size());
        assertEquals(5, movers.losers().size());
        assertEquals("NSE:ICICIBANK", movers.gainers().get(0).getSymbol()); // Highest mock gainer: 2.10
        assertEquals("NSE:HDFCBANK", movers.losers().get(0).getSymbol()); // Lowest mock loser: -1.15
    }

    @Test
    public void testCalculateTopMovers_SortedCorrectly() {
        List<Tick> mockTicks = new ArrayList<>();
        
        Tick tickA = new Tick(Instant.now(), "NSE:RELIANCE", 2950.0);
        tickA.setChangePct(1.5);
        mockTicks.add(tickA);

        Tick tickB = new Tick(Instant.now(), "NSE:TCS", 3800.0);
        tickB.setChangePct(-2.5);
        mockTicks.add(tickB);

        Tick tickC = new Tick(Instant.now(), "NSE:INFY", 1450.0);
        tickC.setChangePct(4.0);
        mockTicks.add(tickC);

        when(tickRepository.findLatestTicksForAllSymbols()).thenReturn(mockTicks);
        when(symbolRepository.findById("NSE:RELIANCE")).thenReturn(Optional.of(new Symbol("NSE:RELIANCE", "Reliance Industries Ltd.", "RELIANCE", "NSE", "EQ")));
        when(symbolRepository.findById("NSE:TCS")).thenReturn(Optional.of(new Symbol("NSE:TCS", "Tata Consultancy Services Ltd.", "TCS", "NSE", "EQ")));
        when(symbolRepository.findById("NSE:INFY")).thenReturn(Optional.of(new Symbol("NSE:INFY", "Infosys Limited", "INFY", "NSE", "EQ")));

        TopMoversDto movers = service.calculateTopMovers();

        assertNotNull(movers);
        // Gainers list has INFY (4.0) first, then RELIANCE (1.5)
        assertEquals("NSE:INFY", movers.gainers().get(0).getSymbol());
        assertEquals("NSE:RELIANCE", movers.gainers().get(1).getSymbol());

        // Losers list has TCS (-2.5) first
        assertEquals("NSE:TCS", movers.losers().get(0).getSymbol());
    }
}
