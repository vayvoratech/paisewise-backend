package in.sapphirus.rupee.practice.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import in.sapphirus.rupee.practice.domain.Order;
import in.sapphirus.rupee.practice.domain.PaperAccount;
import in.sapphirus.rupee.practice.domain.PaperPosition;
import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.repo.OrderRepository;
import in.sapphirus.rupee.practice.repo.PaperAccountRepository;
import in.sapphirus.rupee.practice.repo.PaperPositionRepository;
import in.sapphirus.rupee.practice.repo.StockRepository;
import in.sapphirus.rupee.practice.service.PaperOrderService;
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
    private final PaperOrderService paperOrderService;
    private final StockRepository stocks;
    private final OrderRepository orders;
    private final PaperAccountRepository paperAccounts;
    private final PaperPositionRepository paperPositions;

    public PracticeController(StockRepository stocks,
                              OrderRepository orders,
                              PaperOrderService paperOrderService,
                              PaperAccountRepository paperAccounts,
                              PaperPositionRepository paperPositions) {
        this.stocks = stocks;
        this.orders = orders;
        this.paperOrderService = paperOrderService;
        this.paperAccounts = paperAccounts;
        this.paperPositions = paperPositions;
    }

    // ── View records ──────────────────────────────────────────────────────────

    public record StockView(String symbol, String name, double price, double changePct,
                            String emoji, @JsonRawValue String trend) {}

    public record PositionView(String symbol, int quantity, int reservedQuantity) {}

    public record AccountView(BigDecimal balance, BigDecimal reservedBalance,
                              List<PositionView> positions) {}

    public record PlaceOrderRequest(
            @NotBlank String symbol,
            @NotBlank String side,
            @Min(1) int shares,
            @NotBlank String orderType,
            BigDecimal price,
            String clientOrderId   // optional idempotency key from the frontend
    ) {}

    public record OrderReceipt(UUID orderId, String symbol, String side, int shares,
                               BigDecimal pricePerShare, BigDecimal totalPaid,
                               String orderType, String status, int xpEarned) {}

    // ── Stock endpoints ───────────────────────────────────────────────────────

    @GetMapping("/stocks")
    public List<StockView> stocks() {
        return stocks.findAll().stream().map(this::view).toList();
    }

    @GetMapping("/stocks/{symbol}")
    public StockView stock(@PathVariable String symbol) {
        return stocks.findById(symbol).map(this::view)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));
    }

    // ── Account endpoint ──────────────────────────────────────────────────────

    @GetMapping("/account")
    public AccountView account() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        PaperAccount acct = paperAccounts.findById(userId)
                .orElseGet(() -> {
                    PaperAccount fresh = new PaperAccount(userId);
                    return paperAccounts.save(fresh);
                });

        List<PositionView> positions = paperPositions.findByUserId(userId).stream()
                .map(p -> new PositionView(p.getSymbol(), p.getQuantity(), p.getReservedQuantity()))
                .toList();

        return new AccountView(acct.getBalance(), acct.getReservedBalance(), positions);
    }

    // ── Order endpoints ───────────────────────────────────────────────────────

    @PostMapping("/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderReceipt placeOrder(@RequestBody PlaceOrderRequest req) {
        UUID userId = UUID.fromString(CurrentUser.requireId());

        Stock stock = stocks.findById(req.symbol())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Stock not found"));

        String orderType = req.orderType().toUpperCase();
        String side = req.side().toUpperCase();
        Order order;

        if ("MARKET".equals(orderType)) {
            if ("BUY".equals(side)) {
                order = paperOrderService.executeMarketBuy(userId, stock, req.shares(), req.clientOrderId());
            } else if ("SELL".equals(side)) {
                order = paperOrderService.executeMarketSell(userId, stock, req.shares(), req.clientOrderId());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order side");
            }
        } else if ("LIMIT".equals(orderType)) {
            if (req.price() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Price is required for LIMIT orders");
            }
            if ("BUY".equals(side)) {
                order = paperOrderService.placeLimitBuy(userId, stock, req.shares(), req.price(), req.clientOrderId());
            } else if ("SELL".equals(side)) {
                order = paperOrderService.placeLimitSell(userId, stock, req.shares(), req.price(), req.clientOrderId());
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid order side");
            }
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported order type");
        }

        BigDecimal price = order.getPrice() != null ? order.getPrice() : BigDecimal.ZERO;
        BigDecimal total = price.multiply(BigDecimal.valueOf(order.getQuantity()));

        return new OrderReceipt(order.getId(), order.getSymbol(), order.getSide(),
                order.getQuantity(), price, total, order.getOrderType(), order.getStatus(), XP_PER_TRADE);
    }

    @GetMapping("/orders")
    public List<OrderReceipt> myOrders() {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        return orders.findByUserIdOrderByPlacedAtDesc(userId).stream()
                .map(o -> {
                    BigDecimal price = o.getPrice() != null ? o.getPrice() : BigDecimal.ZERO;
                    BigDecimal total = price.multiply(BigDecimal.valueOf(o.getQuantity()));
                    return new OrderReceipt(o.getId(), o.getSymbol(), o.getSide(), o.getQuantity(),
                            price, total, o.getOrderType(), o.getStatus(), XP_PER_TRADE);
                })
                .toList();
    }

    @DeleteMapping("/orders/{orderId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelOrder(@PathVariable UUID orderId) {
        UUID userId = UUID.fromString(CurrentUser.requireId());
        paperOrderService.cancelOrder(userId, orderId);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private StockView view(Stock s) {
        return new StockView(s.getSymbol(), s.getName(), s.getPrice(), s.getChangePct(),
                s.getEmoji(), s.getTrendJson());
    }
}
