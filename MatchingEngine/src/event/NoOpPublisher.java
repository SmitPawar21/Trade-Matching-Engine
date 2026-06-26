package event;

/**
 * A silent publisher that discards all engine response events.
 * Used for seed orders (startup) and internal RL agent orders
 * that do not need to send responses over a socket connection.
 */
public class NoOpPublisher implements EngineResponsePublisher {

    public static final NoOpPublisher INSTANCE = new NoOpPublisher();

    private NoOpPublisher() {}

    @Override
    public void publish(EngineResponse response) {
        // Silently discard — no socket to write to
    }
}
