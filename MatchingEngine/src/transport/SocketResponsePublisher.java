package transport;

import java.io.PrintWriter;

import com.fasterxml.jackson.databind.ObjectMapper;

import event.EngineResponse;
import event.EngineResponsePublisher;

public class SocketResponsePublisher implements EngineResponsePublisher {
	private final PrintWriter writer;

    private final ObjectMapper mapper =
            new ObjectMapper();
    
    public SocketResponsePublisher(
            PrintWriter writer) {

        this.writer = writer;
    }
    
    @Override
    public void publish(EngineResponse response) {
    	try {
    		String json = mapper.writeValueAsString(response);
    		writer.println(json);
    		writer.flush();
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}
