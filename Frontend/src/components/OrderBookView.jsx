import React, { useMemo } from 'react';

const OrderBookView = ({ symbol, midPrice = 0 }) => {
  // Generate mock order book data based on mid price just for visual layout
  const bookData = useMemo(() => {
    const bids = [];
    const asks = [];
    const baseQty = symbol === 'BTC' ? 0.5 : 5.0;
    
    if (midPrice > 0) {
      for (let i = 0; i < 10; i++) {
        // Asks (higher than mid)
        asks.push({
          price: midPrice + (i + 1) * 10,
          qty: (Math.random() * baseQty + baseQty).toFixed(2),
          total: (Math.random() * baseQty * 3).toFixed(2)
        });
        // Bids (lower than mid)
        bids.push({
          price: midPrice - (i + 1) * 10,
          qty: (Math.random() * baseQty + baseQty).toFixed(2),
          total: (Math.random() * baseQty * 3).toFixed(2)
        });
      }
    }
    return { asks: asks.reverse(), bids }; // Reverse asks so lowest is at bottom
  }, [midPrice, symbol]);

  return (
    <div className="bg-gray-800/80 backdrop-blur-sm rounded-xl border border-gray-700 overflow-hidden flex flex-col h-[500px] shadow-lg">
      <div className="px-4 py-3 border-b border-gray-700 bg-gray-900/50 flex justify-between items-center">
        <h3 className="font-bold text-gray-200">{symbol} Order Book</h3>
        <span className="text-xs px-2 py-1 bg-gray-700 text-gray-300 rounded uppercase tracking-wide">Mocked Data</span>
      </div>
      
      <div className="grid grid-cols-3 gap-4 px-4 py-2 text-xs font-medium text-gray-500 uppercase tracking-wider border-b border-gray-800 bg-gray-900/20">
        <div className="text-left">Price</div>
        <div className="text-right">Size</div>
        <div className="text-right">Total</div>
      </div>

      <div className="flex-1 overflow-y-auto font-mono text-sm scrollbar-thin scrollbar-thumb-gray-700">
        {midPrice === 0 ? (
          <div className="h-full flex items-center justify-center text-gray-500 italic">
            Waiting for price data...
          </div>
        ) : (
          <div className="flex flex-col">
            {/* Asks (Sell Orders) */}
            <div className="flex flex-col">
              {bookData.asks.map((ask, i) => (
                <div key={`ask-${i}`} className="grid grid-cols-3 gap-4 px-4 py-1 hover:bg-gray-700/30 cursor-pointer relative group">
                  <div className="absolute inset-y-0 right-0 bg-red-500/10 z-0" style={{ width: `${Math.min(100, ask.total * 5)}%` }}></div>
                  <div className="text-red-400 z-10">{ask.price.toLocaleString(undefined, {minimumFractionDigits: 2})}</div>
                  <div className="text-right text-gray-300 z-10">{ask.qty}</div>
                  <div className="text-right text-gray-400 z-10">{ask.total}</div>
                </div>
              ))}
            </div>

            {/* Mid Price Spread Indicator */}
            <div className="py-2 my-1 border-y border-gray-700/50 bg-gray-900/50 flex justify-center items-center gap-4">
              <span className="text-lg font-bold text-gray-200">${midPrice.toLocaleString(undefined, {minimumFractionDigits: 2})}</span>
              <svg className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 10l7-7m0 0l7 7m-7-7v18" />
              </svg>
            </div>

            {/* Bids (Buy Orders) */}
            <div className="flex flex-col">
              {bookData.bids.map((bid, i) => (
                <div key={`bid-${i}`} className="grid grid-cols-3 gap-4 px-4 py-1 hover:bg-gray-700/30 cursor-pointer relative group">
                  <div className="absolute inset-y-0 right-0 bg-emerald-500/10 z-0" style={{ width: `${Math.min(100, bid.total * 5)}%` }}></div>
                  <div className="text-emerald-400 z-10">{bid.price.toLocaleString(undefined, {minimumFractionDigits: 2})}</div>
                  <div className="text-right text-gray-300 z-10">{bid.qty}</div>
                  <div className="text-right text-gray-400 z-10">{bid.total}</div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default OrderBookView;
