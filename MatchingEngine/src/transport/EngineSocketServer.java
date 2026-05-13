package transport;

import java.net.ServerSocket;
import java.net.Socket;

import engine.EngineManager;

public class EngineSocketServer {
	private final EngineManager engineManager;
	
	public EngineSocketServer(EngineManager engineManager) {
		this.engineManager = engineManager;
	}
	
	public void start() throws Exception {
        ServerSocket server = new ServerSocket(9090);

        System.out.println(
                "Java Engine listening on 9090"
        );

        while (true) {
            Socket socket = server.accept();
            System.out.println(
                    "Node connected: "
                    + socket.getRemoteSocketAddress()
            );
            new Thread(
        		new ClientHandler(socket, engineManager)
            ).start();
        }
    }
}
