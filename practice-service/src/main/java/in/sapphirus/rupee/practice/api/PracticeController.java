package in.sapphirus.rupee.practice.api;

import com.fasterxml.jackson.annotation.JsonRawValue;
import in.sapphirus.rupee.practice.domain.Order;
import in.sapphirus.rupee.practice.domain.Stock;
import in.sapphirus.rupee.practice.repo.OrderRepository;
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

    public PracticeController(StockRepository stocks,
                              OrderRepository orders,
                              PaperOrderService paperOrderService) {
        this.stocks = stocks;
        this.orders = orders;
        this.paperOrderService = paperOrderService;
    }

    public record StockView(String symbol, String name, double price, double changePct,
                            String emoji, @JsonRawValue String trend) {}

    public record PlaceOrderRequest(
            @NotBlank String symbol,
            @NotBlank String side,
            @Min(1) int shares,
            @NotBlank String orderType,
            BigDecimal price
    ) {}

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
        UUID userId = UUID.fromString(CurrentUser.requireId());

        Stock stock = stocks.findById(req.symbol())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Stock not found"
                        ));

        String orderType = req.orderType().toUpperCase();
        String side = req.side().toUpperCase();

        Order order;

        if ("MARKET".equals(orderType)) {

            if ("BUY".equals(side)) {
                order = paperOrderService.executeMarketBuy(
                        userId,
                        stock,
                        req.shares()
                );
            } else if ("SELL".equals(side)) {
                order = paperOrderService.executeMarketSell(
                        userId,
                        stock,
                        req.shares()
                );
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid order side"
                );
            }

        } else if ("LIMIT".equals(orderType)) {

        if (req.price() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Price is required for LIMIT orders"
            );
        }

        if ("BUY".equals(side)) {

            order = paperOrderService.placeLimitBuy(
                    userId,
                    stock,
                    req.shares(),
                    req.price()
            );

        } else if ("SELL".equals(side)) {

            order = paperOrderService.placeLimitSell(
                    userId,
                    stock,
                    req.shares(),
                    req.price()
            );

        } else {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid order side"
            );
        }

        } else {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported order type"
            );
        }

        BigDecimal price = order.getPrice();

        BigDecimal total = price.multiply(
                BigDecimal.valueOf(order.getQuantity())
        );

        return new OrderReceipt(
                order.getSymbol(),
                order.getQuantity(),
                price,
                total,
                order.getOrderType(),
                order.getStatus(),
                XP_PER_TRADE
        );
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