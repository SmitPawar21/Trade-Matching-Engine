package agent;

import model.MarketState;

public interface MarketStateProvider {
	MarketState getState(String symbol);
}
