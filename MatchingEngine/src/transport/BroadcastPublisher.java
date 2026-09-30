package transport;

import java.io.PrintWriter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;

import event.EngineResponse;
import event.EngineResponsePublisher;
import event.TradeExecutedEvent;

/**
 * A global publisher that broadcasts trade events to ALL connected TCP clients.
 * 
 * When internal agents (MarketMaker, RetailTrader) execute trades,
 * their events would normally go to NoOpPublisher and be lost.
 * This publisher ensures all connected clients (e.g., Node gateway)
 * receive TRADE_EXECUTED events in real-time, enabling the frontend
 * to display live market prices.
 */
public class BroadcastPublisher implements EngineResponsePublisher {
    
    private static final BroadcastPublisher INSTANCE = new BroadcastPublisher();
    
    private final List<PrintWriter> clients = new CopyOnWriteArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    
    private BroadcastPublisher() {}
    
    public static BroadcastPublisher getInstance() {
        return INSTANCE;
    }
    
    /**
     * Register a new TCP client to receive broadcast events.
     */
    public void addClient(PrintWriter writer) {
        clients.add(writer);
        System.out.println("BroadcastPublisher: client registered. Total clients: " + clients.size());
    }
    
    /**
     * Remove a disconnected client.
     */
    public void removeClient(PrintWriter writer) {
        clients.remove(writer);
        System.out.println("BroadcastPublisher: client removed. Total clients: " + clients.size());
    }
    
    @Override
    public void publish(EngineResponse response) {
        // Only broadcast trade events (to avoid flooding with ORDER_ACCEPTED etc.)
        if (!(response instanceof TradeExecutedEvent)) {
            return;
        }
        
        try {
            String json = mapper.writeValueAsString(response);
            
            for (PrintWriter writer : clients) {
                try {
                    writer.println(json);
                    writer.flush();
                } catch (Exception e) {
                    // Client disconnected — will be cleaned up
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
