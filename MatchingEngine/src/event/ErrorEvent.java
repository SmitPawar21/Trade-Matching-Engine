package event;

public class ErrorEvent implements EngineResponse {
	private final String eventType = "ERROR";
	private final String message;
	
	public ErrorEvent(String message) {
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
