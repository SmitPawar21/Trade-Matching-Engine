import React from 'react';

const TradeHistory = ({ trades }) => {
  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden shadow-lg">
      <div className="px-4 py-3 border-b border-gray-800 bg-gray-800/30 flex justify-between items-center">
        <h3 className="text-sm font-semibold text-gray-300 flex items-center gap-2">
          <svg className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2" />
          </svg>
          Matched Trades
        </h3>
        <span className="text-xs text-gray-500 font-mono">{trades.length} trades</span>
      </div>

      {/* Table Header */}
      <div className="grid grid-cols-6 gap-2 px-4 py-2 text-[10px] font-medium text-gray-500 uppercase tracking-wider border-b border-gray-800 bg-gray-900/50">
        <div>Time</div>
        <div>Symbol</div>
        <div className="text-right">Price</div>
        <div className="text-right">Qty</div>
        <div className="text-right">Buy ID</div>
        <div className="text-right">Sell ID</div>
      </div>

      {/* Table Body */}
      <div className="max-h-[280px] overflow-y-auto">
        {trades.length === 0 ? (
          <div className="text-gray-500 text-xs italic text-center py-8">
            No trades executed yet...
          </div>
        ) : (
          trades.map((trade, idx) => (
            <div 
              key={idx} 
              className={`grid grid-cols-6 gap-2 px-4 py-1.5 text-xs font-mono border-b border-gray-800/50 hover:bg-gray-800/30 transition-colors ${
                idx === 0 ? 'bg-emerald-500/5' : ''
              }`}
            >
              <div className="text-gray-500">{trade._timestamp}</div>
              <div className="text-gray-300 font-semibold">{trade.symbol}</div>
              <div className="text-right text-emerald-400">${trade.tradePrice?.toLocaleString()}</div>
              <div className="text-right text-gray-300">{trade.tradeQty}</div>
              <div className="text-right text-blue-400">#{trade.buyOrderId}</div>
              <div className="text-right text-red-400">#{trade.sellOrderId}</div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default TradeHistory;
