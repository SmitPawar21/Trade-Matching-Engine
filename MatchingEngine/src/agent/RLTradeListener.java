package agent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import event.EngineResponse;
import event.EngineResponsePublisher;
import event.TradeExecutedEvent;
import model.OrderSide;

/**
 * A publisher wrapper that intercepts TradeExecutedEvent and updates
 * the RLStateTracker when an RL agent order is involved in a trade.
 * 
 * RL agent orders are identified by their order IDs being registered
 * in the tracker's active order set.
 * 
 * This publisher delegates all events to the original socket publisher
 * so that Node.js still receives trade notifications.
 */
public class RLTradeListener implements EngineResponsePublisher {

    private final EngineResponsePublisher delegate;
    private final RLStateTracker tracker;
    private final Set<Long> rlOrderIds;

    public RLTradeListener(
            EngineResponsePublisher delegate,
            RLStateTracker tracker) {

        this.delegate = delegate;
        this.tracker = tracker;
        this.rlOrderIds = ConcurrentHashMap.newKeySet();
    }

    /**
     * Register an RL agent order ID so we can detect its fills.
     */
    public void trackOrderId(long orderId, OrderSide side) {
        rlOrderIds.add(orderId);
    }

    @Override
    public void publish(EngineResponse response) {
        // Always forward to delegate (socket publisher)
        if (delegate != null) {
            delegate.publish(response);
        }

        // Intercept trade events to update RL state
        if (response instanceof TradeExecutedEvent) {
            TradeExecutedEvent trade = (TradeExecutedEvent) response;

            long buyOrderId = trade.getBuyOrderId();
            long sellOrderId = trade.getSellOrderId();
            long qty = trade.getTradeQty();
            long price = trade.getTradePrice();

            // Check if the RL agent is the buyer
            if (rlOrderIds.contains(buyOrderId)) {
                tracker.recordFill(OrderSide.BUY, qty, price);
                System.out.println(
                    "RL FILL -> BUY " + qty + " @ " + price +
                    " | Inventory: " + tracker.getInventory() +
                    " | RealizedPnL: " + tracker.getRealizedPnL()
                );
            }

            // Check if the RL agent is the seller
            if (rlOrderIds.contains(sellOrderId)) {
                tracker.recordFill(OrderSide.SELL, qty, price);
                System.out.println(
                    "RL FILL -> SELL " + qty + " @ " + price +
                    " | Inventory: " + tracker.getInventory() +
                    " | RealizedPnL: " + tracker.getRealizedPnL()
                );
            }
        }
    }
}
