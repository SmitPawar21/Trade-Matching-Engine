package model;

public class MarketState {
	private double bestBid;
    private double bestAsk;

    private double spread;

    private int bidVolume;
    private int askVolume;

    private double lastTradePrice;

    private double midPrice;
    
    private int inventory;          // how many coins agent holds
    private double unrealizedPnL;   // mark-to-market profit
    private int recentTrades;       // trades in last N ticks
    private double imbalance;       // (bidVol-askVol)/(bidVol+askVol)

	public MarketState(double bestBid, double bestAsk, double spread, int bidVolume, int askVolume,
			double lastTradePrice, double midPrice, int inventory, double unrealizedPnL, int recentTrades,
			double imbalance) {
		super();
		this.bestBid = bestBid;
		this.bestAsk = bestAsk;
		this.spread = spread;
		this.bidVolume = bidVolume;
		this.askVolume = askVolume;
		this.lastTradePrice = lastTradePrice;
		this.midPrice = midPrice;
		this.inventory = inventory;
		this.unrealizedPnL = unrealizedPnL;
		this.recentTrades = recentTrades;
		this.imbalance = imbalance;
	}

	public int getInventory() {
		return inventory;
	}

	public void setInventory(int inventory) {
		this.inventory = inventory;
	}

	public double getUnrealizedPnL() {
		return unrealizedPnL;
	}

	public void setUnrealizedPnL(double unrealizedPnL) {
		this.unrealizedPnL = unrealizedPnL;
	}

	public int getRecentTrades() {
		return recentTrades;
	}

	public void setRecentTrades(int recentTrades) {
		this.recentTrades = recentTrades;
	}

	public double getImbalance() {
		return imbalance;
	}

	public void setImbalance(double imbalance) {
		this.imbalance = imbalance;
	}

	public double getBestBid() {
		return bestBid;
	}

	public void setBestBid(double bestBid) {
		this.bestBid = bestBid;
	}

	public double getBestAsk() {
		return bestAsk;
	}

	public void setBestAsk(double bestAsk) {
		this.bestAsk = bestAsk;
	}

	public double getSpread() {
		return spread;
	}

	public void setSpread(double spread) {
		this.spread = spread;
	}

	public int getBidVolume() {
		return bidVolume;
	}

	public void setBidVolume(int bidVolume) {
		this.bidVolume = bidVolume;
	}

	public int getAskVolume() {
		return askVolume;
	}

	public void setAskVolume(int askVolume) {
		this.askVolume = askVolume;
	}

	public double getLastTradePrice() {
		return lastTradePrice;
	}

	public void setLastTradePrice(double lastTradePrice) {
		this.lastTradePrice = lastTradePrice;
	}

	public double getMidPrice() {
		return midPrice;
	}

	public void setMidPrice(double midPrice) {
		this.midPrice = midPrice;
	}
    
    
}
