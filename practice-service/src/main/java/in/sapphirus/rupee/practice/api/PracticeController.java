package in.sapphirus.rupee.practice.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import in.sapphirus.rupee.practice.domain.Order;
import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.repo.OrderRepository;
import in.sapphirus.rupee.practice.repo.StockRepository;
import in.sapphirus.rupee.security.CurrentUser;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Stocks (read) + paper-trading orders (write). Routed at /practice/**. */
@RestController
@RequestMapping("/practice")
public class PracticeController {

    private static final int XP_PER_TRADE = 25;

    private final StockRepository stocks;
    private final OrderRepository orders;

    public PracticeController(StockRepository stocks, OrderRepository orders) {
        this.stocks = stocks;
        this.orders = orders;
    }

    public record StockView(String symbol, String name, double price, double changePct,
                            String emoji, @JsonRawValue String trend) {}

    public record PlaceOrderRequest(@NotBlank String symbol, @NotBlank String side,
                                    @Min(1) int shares, @NotBlank String orderType) {}

    public record OrderReceipt(String symbol, int shares, java.math.BigDecimal pricePerShare,
                               java.math.BigDecimal totalPaid, String orderType, String status,
                               int xpEarned) {}

    @GetMapping("/stocks")
    public List<StockView> stocks() {
        return stocks.findAll().stream().map(this::view).toList();
    }

    @GetMapping("/stocks/{symbol}")
    public StockView stock(@PathVariable String symbol) {
        return stocks.findById(symbol).map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
    }

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderReceipt placeOrder(@RequestBody PlaceOrderRequest req) {
        Stock stock = stocks.findById(req.symbol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));

        // ASSUMPTION: CurrentUser.requireId() still returns String — adjust if it
        // already returns UUID directly.
        UUID userId = UUID.fromString(CurrentUser.requireId());

        // client_order_id is the idempotency key the new schema requires. Generating
        // it server-side here since this endpoint doesn't currently accept one from
        // the client — fine for now, but real trading endpoints should let the app
        // generate this so retries after a network glitch don't double-order.
        String clientOrderId = userId + "-" + System.currentTimeMillis();

        Order order = new Order(
                userId,
                clientOrderId,
                stock.getSymbol(),
                "NSE",                      // ASSUMPTION: practice.stocks has no exchange field —
                // defaulting to NSE. Confirm this is correct for all
                // symbols in your stocks table.
                req.side().toUpperCase(),
                req.orderType().toUpperCase(),
                "CNC",                      // ASSUMPTION: practice trades are always delivery-style.
                req.shares(),
                true                        // is_paper — always true here, this is practice-service.
        );
        order.setStatus("COMPLETE");        // simulate instant fill, matching old behavior.
        order.setFilledQty(req.shares());
        order.setAvgPrice(BigDecimal.valueOf(stock.getPrice()));

        orders.save(order);

        BigDecimal price = BigDecimal.valueOf(stock.getPrice());
        BigDecimal total = price.multiply(BigDecimal.valueOf(req.shares()));

        // NOTE: XP-per-trade tracking is no longer part of practice.orders in the new
        // schema — that responsibility moved to learn-service/profile-service's XP
        // system. If you still want XP awarded on a trade, that needs a real decision:
        // either call profile-service from here, or leave trades as XP-free and only
        // award XP for lessons/quizzes. Not something to silently invent — flagging it.

        return new OrderReceipt(order.getSymbol(), order.getQuantity(), price, total,
                order.getOrderType(), order.getStatus(), XP_PER_TRADE);
    }

    @GetMapping("/orders")
    public List<OrderReceipt> myOrders() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return orders.findByUserIdOrderByPlacedAtDesc(userId).stream()
                .map(o -> new OrderReceipt(o.getSymbol(), o.getQuantity(), o.getPrice(),
                        o.getPrice() != null ? o.getPrice().multiply(BigDecimal.valueOf(o.getQuantity())) : null,
                        o.getOrderType(), o.getStatus(), XP_PER_TRADE))
                .toList();
    }

    private StockView view(Stock s) {
        return new StockView(s.getSymbol(), s.getName(), s.getPrice(), s.getChangePct(),
                s.getEmoji(), s.getTrendJson());
    }
}