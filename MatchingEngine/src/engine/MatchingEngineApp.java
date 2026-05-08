package engine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.Order;
import model.OrderSide;
import model.OrderStatus;
import model.OrderType;
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

            // 3. Start symbol engines
            manager.startAll();

            System.out.println(
                    "Matching Engine Started..."
            );

            // 4. Start TCP socket server
            EngineSocketServer server =
                    new EngineSocketServer(
                            manager
                    );

            server.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
