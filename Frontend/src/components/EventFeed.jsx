import React from 'react';

const EventFeed = ({ events }) => {
  const getEventStyle = (eventType) => {
    switch (eventType) {
      case 'TRADE_EXECUTED':   return { color: 'text-emerald-400', badge: 'bg-emerald-500/15 border-emerald-500/30', icon: '⚡' };
      case 'ORDER_ACCEPTED':   return { color: 'text-blue-400',    badge: 'bg-blue-500/15 border-blue-500/30',    icon: '✓' };
      case 'ORDER_FILLED':     return { color: 'text-teal-400',    badge: 'bg-teal-500/15 border-teal-500/30',    icon: '●' };
      case 'ORDER_CANCELLED':  return { color: 'text-amber-400',   badge: 'bg-amber-500/15 border-amber-500/30',  icon: '✕' };
      case 'ORDER_REJECTED':   return { color: 'text-red-400',     badge: 'bg-red-500/15 border-red-500/30',      icon: '⊘' };
      case 'CANCEL_REJECTED':  return { color: 'text-orange-400',  badge: 'bg-orange-500/15 border-orange-500/30', icon: '!' };
      default:                 return { color: 'text-gray-400',    badge: 'bg-gray-700/30 border-gray-600/30',    icon: '·' };
    }
  };

  const formatEvent = (evt) => {
    switch (evt.eventType) {
      case 'TRADE_EXECUTED':
        return `${evt.symbol} | Qty: ${evt.tradeQty} @ $${evt.tradePrice?.toLocaleString()} | Buy#${evt.buyOrderId} ↔ Sell#${evt.sellOrderId}`;
      case 'ORDER_ACCEPTED':
        return `${evt.symbol} | Order #${evt.orderId} accepted`;
      case 'ORDER_FILLED':
        return `${evt.symbol} | Order #${evt.orderId} fully filled`;
      case 'ORDER_CANCELLED':
        return `${evt.symbol} | Order #${evt.orderId} cancelled`;
      case 'ORDER_REJECTED':
        return `${evt.symbol} | Order #${evt.orderId} rejected — ${evt.reason || ''}`;
      case 'PARTIALLY_FILLED':
        return `${evt.symbol} | Order #${evt.orderId} partial fill, remaining: ${evt.remainingQuantity}`;
      default:
        return JSON.stringify(evt);
    }
  };

  return (
    <div className="bg-gray-900 rounded-xl border border-gray-800 flex flex-col h-[400px] overflow-hidden shadow-lg">
      <div className="px-4 py-3 border-b border-gray-800 bg-gray-800/30 flex justify-between items-center shrink-0">
        <h3 className="text-sm font-semibold text-gray-300 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          Live Engine Events
        </h3>
        <span className="text-xs text-gray-500 font-mono">{events.length} events</span>
      </div>
      
      <div className="flex-1 overflow-y-auto p-3 space-y-1.5 font-mono text-xs">
        {events.length === 0 ? (
          <div className="text-gray-500 flex items-center justify-center h-full italic">
            Waiting for engine events...
          </div>
        ) : (
          events.map((evt, idx) => {
            const style = getEventStyle(evt.eventType);
            return (
              <div key={idx} className={`px-3 py-2 rounded-lg border ${style.badge} flex items-start gap-2 transition-all`}>
                <span className="shrink-0 mt-0.5">{style.icon}</span>
                <div className="flex-1 min-w-0">
                  <span className={`font-bold mr-2 ${style.color}`}>
                    {evt.eventType?.replace(/_/g, ' ')}
                  </span>
                  <span className="text-gray-300">{formatEvent(evt)}</span>
                </div>
                <span className="text-gray-600 shrink-0 text-[10px]">{evt._timestamp}</span>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};

export default EventFeed;
