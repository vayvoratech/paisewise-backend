package in.sapphirus.rupee.market;

import in.sapphirus.rupee.market.domain.Watchlist;
import in.sapphirus.rupee.market.repo.SymbolRepository;
import in.sapphirus.rupee.market.repo.WatchlistRepository;
import in.sapphirus.rupee.market.service.WatchlistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class WatchlistServiceTest {

    @Mock
    private WatchlistRepository watchlistRepository;

    @Mock
    private SymbolRepository symbolRepository;

    private WatchlistService watchlistService;
    private final UUID testUserId = UUID.randomUUID();

    @BeforeEach
    public void setUp() {
        watchlistService = new WatchlistService(watchlistRepository, symbolRepository);
    }

    @Test
    public void testGetWatchlist() {
        List<Watchlist> mockList = new ArrayList<>();
        mockList.add(new Watchlist(testUserId, "NSE:INFY", 0));
        mockList.add(new Watchlist(testUserId, "NSE:TCS", 1));

        when(watchlistRepository.findByUserIdOrderBySortOrderAsc(testUserId)).thenReturn(mockList);

        List<Watchlist> result = watchlistService.getWatchlist(testUserId);
        assertEquals(2, result.size());
        assertEquals("NSE:INFY", result.get(0).getSymbol());
        assertEquals("NSE:TCS", result.get(1).getSymbol());
    }

    @Test
    public void testAddToWatchlist_Success() {
        when(symbolRepository.existsById("NSE:RELIANCE")).thenReturn(true);
        when(watchlistRepository.findByUserIdAndSymbol(testUserId, "NSE:RELIANCE")).thenReturn(Optional.empty());
        when(watchlistRepository.countByUserId(testUserId)).thenReturn(5);
        when(watchlistRepository.save(any(Watchlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Watchlist saved = watchlistService.addToWatchlist(testUserId, "NSE:RELIANCE");
        assertNotNull(saved);
        assertEquals(testUserId, saved.getUserId());
        assertEquals("NSE:RELIANCE", saved.getSymbol());
    }

    @Test
    public void testAddToWatchlist_SuccessResolvePrefix() {
        // Resolve prefix "RELIANCE" -> "NSE:RELIANCE"
        when(symbolRepository.existsById("NSE:RELIANCE")).thenReturn(true);
        when(watchlistRepository.findByUserIdAndSymbol(testUserId, "NSE:RELIANCE")).thenReturn(Optional.empty());
        when(watchlistRepository.countByUserId(testUserId)).thenReturn(0);
        when(watchlistRepository.save(any(Watchlist.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Watchlist saved = watchlistService.addToWatchlist(testUserId, "RELIANCE");
        assertEquals("NSE:RELIANCE", saved.getSymbol());
    }

    @Test
    public void testAddToWatchlist_SymbolNotFound() {
        when(symbolRepository.existsById("NSE:INVALID")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            watchlistService.addToWatchlist(testUserId, "NSE:INVALID");
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Stock symbol not found"));
    }

    @Test
    public void testAddToWatchlist_DuplicateSymbol() {
        when(symbolRepository.existsById("NSE:RELIANCE")).thenReturn(true);
        when(watchlistRepository.findByUserIdAndSymbol(testUserId, "NSE:RELIANCE"))
                .thenReturn(Optional.of(new Watchlist(testUserId, "NSE:RELIANCE", 0)));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            watchlistService.addToWatchlist(testUserId, "NSE:RELIANCE");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
    }

    @Test
    public void testAddToWatchlist_LimitExceeded() {
        when(symbolRepository.existsById("NSE:RELIANCE")).thenReturn(true);
        when(watchlistRepository.findByUserIdAndSymbol(testUserId, "NSE:RELIANCE")).thenReturn(Optional.empty());
        when(watchlistRepository.countByUserId(testUserId)).thenReturn(50); // Max limit reached

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            watchlistService.addToWatchlist(testUserId, "NSE:RELIANCE");
        });

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("limit of 50 items reached"));
    }

    @Test
    public void testRemoveFromWatchlist() {
        watchlistService.removeFromWatchlist(testUserId, "NSE:RELIANCE");
        verify(watchlistRepository, times(1)).deleteByUserIdAndSymbol(testUserId, "NSE:RELIANCE");
    }

    @Test
    public void testReorderWatchlist() {
        Watchlist w1 = new Watchlist(testUserId, "NSE:RELIANCE", 0);
        Watchlist w2 = new Watchlist(testUserId, "NSE:TCS", 1);

        when(watchlistRepository.findByUserIdAndSymbol(testUserId, "NSE:RELIANCE")).thenReturn(Optional.of(w1));
        when(watchlistRepository.findByUserIdAndSymbol(testUserId, "NSE:TCS")).thenReturn(Optional.of(w2));

        watchlistService.reorderWatchlist(testUserId, List.of("NSE:TCS", "NSE:RELIANCE"));

        assertEquals(1, w1.getSortOrder()); // Rel reordered to 2nd position (index 1)
        assertEquals(0, w2.getSortOrder()); // TCS reordered to 1st position (index 0)
        verify(watchlistRepository, times(2)).save(any(Watchlist.class));
    }
}
