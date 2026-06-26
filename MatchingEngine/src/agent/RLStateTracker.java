package agent;

import java.util.ArrayList;
import java.util.List;

import model.OrderSide;

/**
 * Per-symbol mutable tracker for the RL agent's position.
 * 
 * Tracks:
 *  - inventory (net position from RL agent's fills: +long / -short)
 *  - realizedPnL (locked-in profit from closing positions)
 *  - unrealizedPnL (mark-to-market on open position)
 *  - recentTradeCount (number of fills since last reset)
 *  - lastTradePrice
 *  - activeOrderIds (for CANCEL_QUOTES action)
 *  - stepCount (for episode termination)
 * 
 * Episode termination conditions:
 *  - stepCount >= MAX_STEPS (500)
 *  - |inventory| >= INVENTORY_LIMIT (100)
 *  - realizedPnL <= PNL_FLOOR (-50000)
 */
public class RLStateTracker {

    private static final int MAX_STEPS = 500;
    private static final int INVENTORY_LIMIT = 100;
    private static final double PNL_FLOOR = -50000.0;

    private final String symbol;
    private int inventory = 0;
    private double realizedPnL = 0;
    private double totalCostBasis = 0;   // cumulative cost for avg price
    private int recentTradeCount = 0;
    private long lastTradePrice = 0;
    private int stepCount = 0;

    private final List<Long> activeOrderIds = new ArrayList<>();

    public RLStateTracker(String symbol) {
        this.symbol = symbol;
    }

    /**
     * Called when an RL agent order is filled.
     * Updates inventory, cost basis, and realized PnL.
     */
    public synchronized void recordFill(OrderSide side, long qty, long price) {
        recentTradeCount++;
        lastTradePrice = price;

        if (side == OrderSide.BUY) {
            // Buying: if we're short, this closes short position
            if (inventory < 0) {
                // Closing short position (partially or fully)
                long closeQty = Math.min(qty, Math.abs(inventory));
                double avgShortPrice = (inventory == 0) ? price : (totalCostBasis / Math.abs(inventory));
                // PnL from closing short: sold high (avgShortPrice), bought low (price)
                realizedPnL += closeQty * (avgShortPrice - price);

                long remainingQty = qty - closeQty;
                inventory += (int) qty;

                if (inventory > 0) {
                    // Flipped to long — remaining qty opens new long position
                    totalCostBasis = remainingQty * price;
                } else if (inventory == 0) {
                    totalCostBasis = 0;
                } else {
                    // Still short, reduce cost basis proportionally
                    totalCostBasis = Math.abs(inventory) * avgShortPrice;
                }
            } else {
                // Adding to long position
                inventory += (int) qty;
                totalCostBasis += qty * price;
            }
        } else {
            // Selling: if we're long, this closes long position
            if (inventory > 0) {
                long closeQty = Math.min(qty, inventory);
                double avgLongPrice = (inventory == 0) ? price : (totalCostBasis / inventory);
                // PnL from closing long: bought low (avgLongPrice), sold high (price)
                realizedPnL += closeQty * (price - avgLongPrice);

                long remainingQty = qty - closeQty;
                inventory -= (int) qty;

                if (inventory < 0) {
                    // Flipped to short
                    totalCostBasis = remainingQty * price;
                } else if (inventory == 0) {
                    totalCostBasis = 0;
                } else {
                    // Still long, reduce cost basis proportionally
                    totalCostBasis = inventory * avgLongPrice;
                }
            } else {
                // Adding to short position
                inventory -= (int) qty;
                totalCostBasis += qty * price;
            }
        }
    }

    /**
     * Compute unrealized PnL based on current mid price.
     */
    public double computeUnrealizedPnL(double midPrice) {
        if (inventory == 0) return 0;

        double avgPrice = totalCostBasis / Math.abs(inventory);

        if (inventory > 0) {
            // Long position: profit if midPrice > avgPrice
            return inventory * (midPrice - avgPrice);
        } else {
            // Short position: profit if midPrice < avgPrice
            return Math.abs(inventory) * (avgPrice - midPrice);
        }
    }

    /**
     * Increment step counter. Returns true if episode should terminate.
     */
    public synchronized boolean incrementStep() {
        stepCount++;
        return isEpisodeDone();
    }

    /**
     * Check episode termination conditions.
     */
    public boolean isEpisodeDone() {
        if (stepCount >= MAX_STEPS) return true;
        if (Math.abs(inventory) >= INVENTORY_LIMIT) return true;
        if (realizedPnL <= PNL_FLOOR) return true;
        return false;
    }

    /**
     * Reset tracker for a new episode.
     */
    public synchronized void resetEpisode() {
        inventory = 0;
        realizedPnL = 0;
        totalCostBasis = 0;
        recentTradeCount = 0;
        lastTradePrice = 0;
        stepCount = 0;
        activeOrderIds.clear();
    }

    // --- Active order tracking for CANCEL_QUOTES ---

    public synchronized void addActiveOrder(long orderId) {
        activeOrderIds.add(orderId);
    }

    public synchronized List<Long> getAndClearActiveOrders() {
        List<Long> copy = new ArrayList<>(activeOrderIds);
        activeOrderIds.clear();
        return copy;
    }

    public synchronized void removeActiveOrder(long orderId) {
        activeOrderIds.remove(Long.valueOf(orderId));
    }

    // --- Getters ---

    public String getSymbol() { return symbol; }
    public int getInventory() { return inventory; }
    public double getRealizedPnL() { return realizedPnL; }
    public int getRecentTradeCount() { return recentTradeCount; }
    public long getLastTradePrice() { return lastTradePrice; }
    public int getStepCount() { return stepCount; }
}
