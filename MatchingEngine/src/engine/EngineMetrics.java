package engine;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;
import io.prometheus.client.Histogram;

public class EngineMetrics {
    // 1. Order Throughput (Counter)
    public static final Counter incomingOrders = Counter.build()
            .name("engine_orders_total")
            .help("Total incoming orders.")
            .labelNames("symbol", "side", "type")
            .register();

    // 2. Trade Execution Rate (Counter)
    public static final Counter tradesExecuted = Counter.build()
            .name("engine_trades_total")
            .help("Total executed trades.")
            .labelNames("symbol")
            .register();

    // 3. Order Book Depth / Liquidity (Gauge)
    public static final Gauge orderBookDepth = Gauge.build()
            .name("engine_order_book_depth_quantity")
            .help("Current total quantity of resting orders in the book.")
            .labelNames("symbol", "side")
            .register();

    // 4. Matching Latency (Histogram)
    // Buckets can be tuned based on typical latency, here we use default + some microsecond buckets
    public static final Histogram matchingLatency = Histogram.build()
            .name("engine_matching_latency_seconds")
            .help("Time taken to process an order in seconds.")
            .labelNames("symbol")
            .buckets(0.0001, 0.0005, 0.001, 0.005, 0.01, 0.05, 0.1, 0.5) // From 0.1ms to 500ms
            .register();

    // 5. Order Rejection / Error Rate (Counter)
    public static final Counter orderRejections = Counter.build()
            .name("engine_order_rejections_total")
            .help("Total number of rejected orders.")
            .labelNames("symbol", "reason")
            .register();

    // 6. Traded Volume (Counter)
    public static final Counter volumeExecuted = Counter.build()
            .name("engine_trade_volume_total")
            .help("Total volume traded.")
            .labelNames("symbol")
            .register();
}
