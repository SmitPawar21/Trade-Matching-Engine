package engine;

import java.util.Date;

import agent.MarketAgent;
import agent.MarketStateProvider;
import agent.OrderBookStateProvider;
import agent.RuleBasedMarketMaker;
import model.Order;
import model.OrderSide;
import model.OrderStatus;
import model.OrderType;
import simulation.MarketMakerRunner;
import transport.EngineSocketServer;

public class MatchingEngineApp {
	public static void main(String[] args) throws InterruptedException {
		try {
			 // 1. Create engine manager
            EngineManager manager =
                    new EngineManager();

            // 2. Register supported symbols
            manager.addSymbol("BTC");
            manager.addSymbol("ETH");

            // 3. Seed market BEFORE starting matching threads
            //    This is synchronous — orders go directly into the OrderBook
            //    on the main thread. No race condition.
            MarketStateProvider stateProvider = new OrderBookStateProvider(manager);
            seedMarket(manager, stateProvider);

            // 4. Start symbol engines (matching threads)
            manager.startAll();

            System.out.println(
                    "Matching Engine Started..."
            );

            // 5. Start TCP socket server
            EngineSocketServer server = new EngineSocketServer(manager);
            
            Thread socketThread =
                    new Thread(() -> {
                        try {
                            server.start();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

            socketThread.start();
            
            
            MarketAgent agent = new RuleBasedMarketMaker();

            MarketMakerRunner btcRunner = new MarketMakerRunner(manager, agent, stateProvider, "BTC");
            MarketMakerRunner ethRunner = new MarketMakerRunner(manager, agent, stateProvider, "ETH");
            
        //    btcRunner.start();
        //    ethRunner.start();
            
            
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static void seedMarket(EngineManager manager, MarketStateProvider stateProvider) {

	    long orderId = 1;
	    
	    long userId1 = 1;
	    long userId2 = 2;
	    long userId3 = 3;
	    long userId4 = 4;
	    long userId5 = 5;
	    long userId6 = 6;
	    long userId7 = 7;
	    long userId8 = 8;
	    long userId9 = 9;
	    long userId10 = 10;
	    long userId11 = 11;
	    long userId12 = 12;

	    // =========================
	    // BTC BUY SIDE
	    // =========================

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "BTC",
	                    userId1,
	                    OrderSide.BUY,
	                    OrderType.LIMIT,
	                    99900,
	                    50,
	                    50,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("BTC State = "
	            + stateProvider.getState("BTC").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "BTC",
	                    userId2,
	                    OrderSide.BUY,
	                    OrderType.LIMIT,
	                    99800,
	                    40,
	                    40,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("BTC State = "
	            + stateProvider.getState("BTC").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "BTC",
	                    userId3,
	                    OrderSide.BUY,
	                    OrderType.LIMIT,
	                    99700,
	                    30,
	                    30,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("BTC State = "
	            + stateProvider.getState("BTC").print());

	    // =========================
	    // BTC SELL SIDE
	    // =========================

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "BTC",
	                    userId4,
	                    OrderSide.SELL,
	                    OrderType.LIMIT,
	                    100100,
	                    50,
	                    50,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("BTC State = "
	            + stateProvider.getState("BTC").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "BTC",
	                    userId5,
	                    OrderSide.SELL,
	                    OrderType.LIMIT,
	                    100200,
	                    40,
	                    40,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("BTC State = "
	            + stateProvider.getState("BTC").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "BTC",
	                    userId6,
	                    OrderSide.SELL,
	                    OrderType.LIMIT,
	                    100300,
	                    30,
	                    30,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("BTC State = "
	            + stateProvider.getState("BTC").print());

	    // =========================
	    // ETH BUY SIDE
	    // =========================

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "ETH",
	                    userId7,
	                    OrderSide.BUY,
	                    OrderType.LIMIT,
	                    1990,
	                    80,
	                    80,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("ETH State = "
	            + stateProvider.getState("ETH").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "ETH",
	                    userId8,
	                    OrderSide.BUY,
	                    OrderType.LIMIT,
	                    1985,
	                    60,
	                    60,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("ETH State = "
	            + stateProvider.getState("ETH").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "ETH",
	                    userId9,
	                    OrderSide.BUY,
	                    OrderType.LIMIT,
	                    1980,
	                    40,
	                    40,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("ETH State = "
	            + stateProvider.getState("ETH").print());

	    // =========================
	    // ETH SELL SIDE
	    // =========================

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "ETH",
	                    userId10,
	                    OrderSide.SELL,
	                    OrderType.LIMIT,
	                    2010,
	                    80,
	                    80,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("ETH State = "
	            + stateProvider.getState("ETH").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "ETH",
	                    userId11,
	                    OrderSide.SELL,
	                    OrderType.LIMIT,
	                    2015,
	                    60,
	                    60,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("ETH State = "
	            + stateProvider.getState("ETH").print());

	    manager.seedOrder(
	            new Order(
	                    orderId++,
	                    "ETH",
	                    userId12,
	                    OrderSide.SELL,
	                    OrderType.LIMIT,
	                    2020,
	                    40,
	                    40,
	                    OrderStatus.NEW,
	                    new Date()
	            )
	    );
	    
	    System.out.println("ETH State = "
	            + stateProvider.getState("ETH").print());

	    System.out.println("Initial market liquidity seeded.");
	}
}
