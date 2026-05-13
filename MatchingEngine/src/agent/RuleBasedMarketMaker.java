package agent;

import model.AgentAction;
import model.MarketState;

public class RuleBasedMarketMaker implements MarketAgent {
	private static final double MAX_SPREAD = 5.0;

	private static final int LONG_INVENTORY = 50;

	private static final int SHORT_INVENTORY = -50;
	
	@Override
	public AgentAction decide(MarketState state) {
		// case 1: If spread too wide then tighten spread
		if (state.getSpread()
                > MAX_SPREAD) {

            return AgentAction
                    .TIGHT_SPREAD;
        }
		
		// case 2: If inventory too long then sell aggressively
		if (state.getInventory()
                > LONG_INVENTORY) {

            return AgentAction
                    .AGGRESSIVE_SELL;
        }
		
		// case 3: If inventory too short then buy aggressively
		if (state.getInventory()
                < SHORT_INVENTORY) {

            return AgentAction
                    .AGGRESSIVE_BUY;
        }
		
		return AgentAction.WIDE_SPREAD;
		
	}
}
