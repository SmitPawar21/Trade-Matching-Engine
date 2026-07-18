import React from 'react';

const EventFeed = ({ events }) => {
  const getEventColor = (type) => {
    switch (type) {
      case 'NewOrderEvent': return 'text-blue-400';
      case 'TradeExecutedEvent': return 'text-emerald-400';
      case 'CancelOrderEvent': return 'text-red-400';
      case 'OrderAcceptedEvent': return 'text-purple-400';
      case 'OrderFilledEvent': return 'text-teal-400';
      default: return 'text-gray-400';
    }
  };

  const formatEventDetails = (event) => {
    switch (event.type) {
      case 'NewOrderEvent':
        return `New ${event.order?.side} Order for ${event.order?.symbol} | ID: ${event.order?.orderId} | Qty: ${event.order?.quantity} @ $${event.order?.price}`;
      case 'TradeExecutedEvent':
        return `Trade on ${event.symbol} | Qty: ${event.tradeQty} @ $${event.tradePrice} | BuyID: ${event.buyOrderId} SellID: ${event.sellOrderId}`;
      case 'OrderAcceptedEvent':
        return `Order Accepted | ID: ${event.orderId}`;
      case 'CancelOrderEvent':
      case 'OrderCancelledEvent':
        return `Order Cancelled | ${event.symbol} | ID: ${event.orderId}`;
      case 'OrderFilledEvent':
        return `Order Filled | ID: ${event.orderId} | Filled Qty: ${event.filledQty} @ $${event.fillPrice}`;
      case 'ErrorEvent':
        return `Error: ${event.message}`;
      default:
        return JSON.stringify(event);
    }
  };

  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 flex flex-col h-full overflow-hidden shadow-lg">
      <div className="px-4 py-3 border-b border-gray-800 bg-gray-800/30 flex justify-between items-center">
        <h3 className="text-sm font-semibold text-gray-300 flex items-center gap-2">
          <svg className="w-4 h-4 text-emerald-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 10V3L4 14h7v7l9-11h-7z" />
          </svg>
          Live Event Feed
        </h3>
        <span className="text-xs text-gray-500 font-mono">{events.length} events</span>
      </div>
      
      <div className="flex-1 overflow-y-auto p-4 space-y-2 font-mono text-xs scrollbar-thin scrollbar-thumb-gray-700 scrollbar-track-transparent">
        {events.length === 0 ? (
          <div className="text-gray-500 flex items-center justify-center h-full italic">
            Waiting for engine events...
          </div>
        ) : (
          events.map((evt, idx) => (
            <div key={idx} className="bg-gray-800/40 p-2.5 rounded border border-gray-800/50 hover:bg-gray-800/60 transition-colors">
              <span className={`font-bold mr-2 ${getEventColor(evt.type)}`}>
                [{evt.type?.replace('Event', '')}]
              </span>
              <span className="text-gray-300">
                {formatEventDetails(evt)}
              </span>
            </div>
          ))
        )}
      </div>
    </div>
  );
};

export default EventFeed;
