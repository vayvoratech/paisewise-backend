package in.sapphirus.rupee.market.api;

import in.sapphirus.rupee.market.domain.Watchlist;
import in.sapphirus.rupee.market.service.WatchlistService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/market/watchlist")
public class WatchlistController {
    private final WatchlistService service;

    public WatchlistController(WatchlistService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Watchlist>> getWatchlist(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(service.getWatchlist(UUID.fromString(userId)));
    }

    @PostMapping
    public ResponseEntity<Watchlist> add(@RequestHeader("X-User-Id") String userId, @RequestParam String symbol) {
        return ResponseEntity.ok(service.addToWatchlist(UUID.fromString(userId), symbol));
    }

    @DeleteMapping("/{symbol}")
    public ResponseEntity<Void> remove(@RequestHeader("X-User-Id") String userId, @PathVariable String symbol) {
        service.removeFromWatchlist(UUID.fromString(userId), symbol);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/reorder")
    public ResponseEntity<Void> reorder(@RequestHeader("X-User-Id") String userId, @RequestBody List<String> symbols) {
        service.reorderWatchlist(UUID.fromString(userId), symbols);
        return ResponseEntity.ok().build();
    }
}
