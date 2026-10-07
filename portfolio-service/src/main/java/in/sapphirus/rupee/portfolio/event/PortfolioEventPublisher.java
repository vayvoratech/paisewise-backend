package in.sapphirus.rupee.portfolio.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Event publisher for real trading events:
 * - orders.created
 * - portfolio.recalc
 */
@Component
public class PortfolioEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PortfolioEventPublisher.class);

    public static final String TOPIC_ORDERS_CREATED = "orders.created";
    public static final String TOPIC_PORTFOLIO_RECALC = "portfolio.recalc";

    private final ApplicationEventPublisher applicationEventPublisher;

    public PortfolioEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Publish orders.created event.
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing event [topic: {}] for orderId={}, user={}, symbol={}",
                TOPIC_ORDERS_CREATED, event.orderId(), event.userId(), event.symbol());
        applicationEventPublisher.publishEvent(event);
    }

    /**
     * Publish portfolio.recalc event.
     */
    public void publishPortfolioRecalc(PortfolioRecalcEvent event) {
        log.info("Publishing event [topic: {}] for user={}, symbol={}, tradeId={}, trigger={}",
                TOPIC_PORTFOLIO_RECALC, event.userId(), event.symbol(), event.tradeId(), event.triggerReason());
        applicationEventPublisher.publishEvent(event);
    }
}
