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

import java.util.List;

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

    public record OrderReceipt(String symbol, int shares, double pricePerShare, double totalPaid,
                               int xpEarned, String orderType, String status) {}

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
        Order order = new Order(CurrentUser.requireId(), stock.getSymbol(), req.side().toUpperCase(),
                req.shares(), stock.getPrice(), req.orderType().toUpperCase(), XP_PER_TRADE);
        orders.save(order);
        return new OrderReceipt(order.getSymbol(), order.getShares(), order.getPricePerShare(),
                order.getTotalAmount(), order.getXpEarned(), order.getOrderType(), "FILLED");
    }

    @GetMapping("/orders")
    public List<OrderReceipt> myOrders() {
        return orders.findByUserIdOrderByCreatedAtDesc(CurrentUser.requireId()).stream()
                .map(o -> new OrderReceipt(o.getSymbol(), o.getShares(), o.getPricePerShare(),
                        o.getTotalAmount(), o.getXpEarned(), o.getOrderType(), "FILLED"))
                .toList();
    }

    private StockView view(Stock s) {
        return new StockView(s.getSymbol(), s.getName(), s.getPrice(), s.getChangePct(),
                s.getEmoji(), s.getTrendJson());
    }
}
