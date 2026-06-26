package Book;
import java.util.*;

import event.CancelRejectedEvent;
import event.EngineResponsePublisher;
import event.OrderAcceptedEvent;
import event.OrderCancelledEvent;
import event.OrderFilledEvent;
import event.OrderPartiallyFilledEvent;
import event.OrderRejectedEvent;
import event.TradeExecutedEvent;
import model.Order;
import model.OrderSide;
import model.OrderType;

public class OrderBook {
	private TreeMap<Long, PriceLevel> bids = new TreeMap<>(Comparator.reverseOrder()); // descending
	private TreeMap<Long, PriceLevel> asks = new TreeMap<>(); // ascending
	
	private Map<Long, OrderReference> cancelMap = new HashMap<>();
	
	private String validateOrder(Order order) {

	    if (order == null) {
	        return "NULL_ORDER";
	    }

	    if (order.getOrderId() <= 0) {
	        return "INVALID_ORDER_ID";
	    }

	    if (order.getUserId() <= 0) {
	        return "INVALID_USER_ID";
	    }

	    if (order.getSide() == null) {
	        return "INVALID_SIDE";
	    }

	    if (order.getType() == null) {
	        return "INVALID_ORDER_TYPE";
	    }

	    if (order.getQuantity() <= 0) {
	        return "INVALID_QUANTITY";
	    }

	    if (order.getRemainingQuantity() != order.getQuantity()) {
	        return "INVALID_REMAINING_QUANTITY";
	    }

	    if (order.getType() == OrderType.LIMIT && order.getPrice() <= 0) {
	        return "INVALID_PRICE";
	    }

	    if (cancelMap.containsKey(order.getOrderId())) {
	        return "DUPLICATE_ORDER_ID";
	    }

	    return null;
	}
	
	// processNewOrder(Order order)
	public void processNewOrder(Order order, EngineResponsePublisher publisher) {
		if (validateOrder(order) != null) {
	        publisher.publish(
                new OrderRejectedEvent(
                    order.getSymbol(),
                    order.getOrderId(),
                    "INVALID_ORDER"
                )
	        );
	        return;
	    }

	    publisher.publish(
            new OrderAcceptedEvent(
                order.getSymbol(),
                order.getOrderId()
            )
	    );
		match(order, publisher);
		
		if(order.getRemainingQuantity() > 0 && order.getType() == OrderType.LIMIT) {
			addToBook(order);
		}
	}
	
	// match(Order order)
	public void match(Order order, EngineResponsePublisher publisher) {
		
		// for bid order
		while(order.getSide() == OrderSide.BUY && order.getRemainingQuantity() > 0) {
			PriceLevel bestLevel = getBestAsk();
			
			if(bestLevel == null) { // there is no ask 
				break; 
			}
			
			if(order.getType() == OrderType.LIMIT && bestLevel.getPrice() > order.getPrice()) {
				break;
			}
			
			Order restingOrder = bestLevel.getOrders().peekFirst();
			
			long tradeQty = Math.min(restingOrder.getRemainingQuantity(), order.getRemainingQuantity());
			
			executeTrade(order, restingOrder, tradeQty, publisher);
			
			order.setRemainingQuantity(order.getRemainingQuantity() - tradeQty);
			restingOrder.setRemainingQuantity(restingOrder.getRemainingQuantity() - tradeQty);
			
			if(restingOrder.getRemainingQuantity() == 0) {
				publisher.publish(
							new OrderFilledEvent(restingOrder.getSymbol(), order.getOrderId())
						);
				bestLevel.getOrders().pollFirst();
				removeFromCancelMap(restingOrder);
			} else {
				publisher.publish(
							new OrderPartiallyFilledEvent(order.getSymbol(), order.getOrderId(), order.getRemainingQuantity())
						);
			}
			
			if(bestLevel.getOrders().isEmpty()) {
				asks.remove(bestLevel.getPrice());
			}
		}
		
		while(order.getSide() == OrderSide.SELL && order.getRemainingQuantity() > 0) {
			PriceLevel bestLevel = getBestBid();
			
			if(bestLevel == null) {
				break;
			}
			
			if(order.getType() == OrderType.LIMIT && bestLevel.getPrice() < order.getPrice()) {
				break;
			}
			
			Order restingOrder = bestLevel.getOrders().peekFirst();
			
			long tradeQty = Math.min(restingOrder.getRemainingQuantity(), order.getRemainingQuantity());
			
			executeTrade(order, restingOrder, tradeQty, publisher);
			
			order.setRemainingQuantity(order.getRemainingQuantity() - tradeQty);
			restingOrder.setRemainingQuantity(restingOrder.getRemainingQuantity() - tradeQty);
			
			if(restingOrder.getRemainingQuantity() == 0) {
				bestLevel.getOrders().pollFirst();
				removeFromCancelMap(restingOrder);
			}
			
			if(bestLevel.getOrders().isEmpty()) {
				bids.remove(bestLevel.getPrice());
			}	
		}
	}
	
	// addToBook(Order order)
	public void addToBook(Order order) {
		TreeMap<Long, PriceLevel> side = (order.getSide() == OrderSide.BUY) ? bids : asks;
		PriceLevel level = side.get(order.getPrice());
		
		if(level == null) {
			level = new PriceLevel(order.getPrice());
			side.put(order.getPrice(), level);
		}
		
		level.getOrders().addLast(order);
		
		cancelMap.put(order.getOrderId(), new OrderReference(order));
	}
	
	// Execute Trade
	private void executeTrade(Order incoming, Order resting, long tradeQty, EngineResponsePublisher publisher) {

	    long tradePrice = resting.getPrice();

	    Order buyOrder;
	    Order sellOrder;

	    if (incoming.getSide() == OrderSide.BUY) {
	        buyOrder = incoming;
	        sellOrder = resting;
	    } else {
	        buyOrder = resting;
	        sellOrder = incoming;
	    }

	    // String symbol, long buyOrderId, long sellOrderId, long tradeQty, long tradePrice
	    publisher.publish(
	    			new TradeExecutedEvent(buyOrder.getSymbol(), buyOrder.getOrderId(), sellOrder.getOrderId(), tradeQty, tradePrice)
	    		);
	    
	    System.out.println("TRADE EXECUTED -> " +
	    		"Symbol: "+buyOrder.getSymbol()+
	            " | Price: " + tradePrice +
	            " | Quantity: " + tradeQty +
	            " | BuyOrderId: " + buyOrder.getOrderId() +
	            " | SellOrderId: " + sellOrder.getOrderId());
	}
	
	// cancelOrder(String symbol, long orderId)
	public void cancelOrder(String symbol, long orderId, EngineResponsePublisher publisher) {
		OrderReference ref = cancelMap.get(orderId);
		
		if (ref == null) {
	        publisher.publish(
	                new CancelRejectedEvent(
	                        symbol,
	                        orderId,
	                        "ORDER NOT FOUND"
	                )
	        );
	        return;
	    }
		
		PriceLevel level = getLevel(ref);
		
		level.getOrders().remove(ref.getOrder());
//		cancel is not truly O(1) yet.
//		The above line has O(n) Time Complexity
//		Real exchanges solve this with:
//			- intrusive linked list
//			- doubly linked nodes
//			- node pointer in cancelMap
//		Then cancel becomes true O(1).
		
		
		if(level.getOrders().isEmpty()) {
			if(ref.getSide() == OrderSide.BUY) {
				bids.remove(ref.getPrice());
			} 
			if(ref.getSide() == OrderSide.SELL) {
				asks.remove(ref.getPrice());
			}
		}
		
		cancelMap.remove(orderId);
		
		publisher.publish(
	            new OrderCancelledEvent(
	                    symbol,
	                    orderId
	            )
	    );
	}
	
	// getLevel for OrderReference
	private PriceLevel getLevel(OrderReference ref) {

	    if (ref.getSide() == OrderSide.BUY) {
	        return bids.get(ref.getPrice());
	    } else {
	        return asks.get(ref.getPrice());
	    }
	}
	
	// getBestBid()
	public PriceLevel getBestBid() {
		if(bids.isEmpty()) {
			return null;
		}
		
		return bids.firstEntry().getValue();
	}
	
	// getBestAsk()
	public PriceLevel getBestAsk() {
		if(asks.isEmpty()) {
			return null;
		}
		return asks.firstEntry().getValue();
	}
	
	// remove order from cancel map
	public void removeFromCancelMap(Order order) {
		cancelMap.remove(order.getOrderId());
	}
	
	public void printMaps() {
		System.out.println("BIDS MAP  ===");
		for(Long key : bids.keySet()) {
			System.out.println(key+" -> "+bids.get(key));
		}
		System.out.println("ASKS MAP  ===");
		for(Long key : asks.keySet()) {
			System.out.println(key+" -> "+asks.get(key));
		}
	}
	
	public TreeMap<Long, PriceLevel> getBids() {
		return bids;
	}

	public TreeMap<Long, PriceLevel> getAsks() {
		return asks;
	}
	
}
