package in.sapphirus.rupee.market.service;

import in.sapphirus.rupee.market.domain.Watchlist;
import in.sapphirus.rupee.market.repo.SymbolRepository;
import in.sapphirus.rupee.market.repo.WatchlistRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.UUID;

@Service
public class WatchlistService {
    private final WatchlistRepository repository;
    private final SymbolRepository symbolRepository;

    public WatchlistService(WatchlistRepository repository, SymbolRepository symbolRepository) {
        this.repository = repository;
        this.symbolRepository = symbolRepository;
    }

    public List<Watchlist> getWatchlist(UUID userId) {
        return repository.findByUserIdOrderBySortOrderAsc(userId);
    }

    @Transactional
    public Watchlist addToWatchlist(UUID userId, String symbol) {
        String resolvedSymbol = symbol;
        if (!symbol.contains(":")) {
            resolvedSymbol = "NSE:" + symbol;
        }
        if (!symbolRepository.existsById(resolvedSymbol)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock symbol not found: " + symbol);
        }
        if (repository.findByUserIdAndSymbol(userId, resolvedSymbol).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Symbol already exists in watchlist.");
        }
        if (repository.countByUserId(userId) >= 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Watchlist limit of 50 items reached.");
        }
        
        int nextOrder = repository.findByUserIdOrderBySortOrderAsc(userId)
                .stream()
                .mapToInt(Watchlist::getSortOrder)
                .max()
                .orElse(-1) + 1;

        return repository.save(new Watchlist(userId, resolvedSymbol, nextOrder));
    }

    @Transactional
    public void removeFromWatchlist(UUID userId, String symbol) {
        String resolvedSymbol = symbol;
        if (!symbol.contains(":")) {
            resolvedSymbol = "NSE:" + symbol;
        }
        repository.deleteByUserIdAndSymbol(userId, resolvedSymbol);
    }

    @Transactional
    public void reorderWatchlist(UUID userId, List<String> symbols) {
        for (int i = 0; i < symbols.size(); i++) {
            String symbol = symbols.get(i);
            String resolvedSymbol = symbol;
            if (!symbol.contains(":")) {
                resolvedSymbol = "NSE:" + symbol;
            }
            int order = i;
            repository.findByUserIdAndSymbol(userId, resolvedSymbol)
                    .ifPresent(watchlist -> {
                        watchlist.setSortOrder(order);
                        repository.save(watchlist);
                    });
        }
    }
}
