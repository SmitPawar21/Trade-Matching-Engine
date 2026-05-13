package agent;

import model.AgentAction;
import model.MarketState;

public interface MarketAgent {
	AgentAction decide(
            MarketState state
    );
}
