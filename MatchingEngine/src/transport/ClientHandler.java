package transport;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;

import com.fasterxml.jackson.databind.ObjectMapper;

import engine.EngineManager;
import event.EngineResponsePublisher;
import model.Order;
import model.OrderSide;
import model.OrderStatus;
import model.OrderType;
import transport.dto.EngineRequest;
import transport.dto.Envelope;

// Exact flow
//		JSON
//		↓
//		EngineRequest
//		↓
//		Order
//		↓
//		EngineManager
//		↓
//		BTC queue
//		↓
//		Matching thread

public class ClientHandler implements Runnable {
	private final Socket socket;
	private final EngineManager engineManager;
    private final ObjectMapper mapper =
            new ObjectMapper();
    
    public ClientHandler(
            Socket socket,
            EngineManager engineManager) {

        this.socket = socket;
        this.engineManager = engineManager;
    }
    
    @Override
    public void run() {
    	System.out.println(
    	        "ClientHandler started"
    	);
    	try (
                BufferedReader reader =
                    new BufferedReader(
                        new InputStreamReader(
                            socket.getInputStream()
                        )
                    )
        ) {
    		PrintWriter writer =
    		        new PrintWriter(
    		                socket.getOutputStream(),
    		                true
    		        );

    		EngineResponsePublisher publisher =
    		        new SocketResponsePublisher(
    		                writer
    		        );
    		
    		String line;

            while ((line = reader.readLine()) != null) {
            	System.out.println("RAW FROM NODE: " + line);
            	Envelope envelope =
            		    mapper.readValue(
            		        line,
            		        Envelope.class
            		    );

            		processRequest(
            		    envelope,
            		    publisher
            		);
            }
    	} catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void processRequest(
            Envelope envelope,
            EngineResponsePublisher publisher) {
    	
    	System.out.println("TYPE: " + envelope.getType());
    	
    	String messageType =
    	        envelope.getType();

    	EngineRequest request =
    	        envelope.getData();

        if ("NEW_ORDER".equals(messageType)) {

            Order order = new Order(
                request.getOrderId(),
                request.getSymbol(),
                request.getUserId(),

                OrderSide.valueOf(
                    request.getSide()
                ),

                OrderType.valueOf(
                    request.getOrderType()
                ),

                request.getPrice(),

                request.getQuantity(),
                
                request.getQuantity(),
                
                OrderStatus.NEW,
                
                new Date()
            );

            engineManager.submitOrder(
                order,
                publisher
            );
        }

        else if ("CANCEL_ORDER".equals(messageType)) {
            engineManager.cancelOrder(
                    request.getSymbol(),
                    request.getOrderId(),
                    publisher
            );
        }
    }
}
