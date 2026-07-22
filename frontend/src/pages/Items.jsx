import { useState, useEffect, useCallback } from 'react';
import { itemsApi, CATEGORIES } from '../api/api';
import { Package, Search, X, AlertTriangle, RotateCcw } from 'lucide-react';
import ItemCard from '../components/ItemCard';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';

function Items() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [filter, setFilter] = useState('all'); // 'all' | 'available'
  const [category, setCategory] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [activeQuery, setActiveQuery] = useState('');

  const fetchItems = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      let res;
      if (activeQuery) {
        res = await itemsApi.searchItems(activeQuery);
      } else if (category) {
        res = await itemsApi.getItemsByCategory(category);
      } else if (filter === 'available') {
        res = await itemsApi.getAvailableItems();
      } else {
        res = await itemsApi.getAllItems();
      }
      setItems(res.data);
    } catch (e) {
      setError(true);
    } finally {
      setLoading(false);
    }
  }, [filter, category, activeQuery]);

  useEffect(() => {
    fetchItems();
  }, [fetchItems]);

  const runSearch = () => setActiveQuery(searchTerm.trim());
  const clearSearch = () => {
    setSearchTerm('');
    setActiveQuery('');
  };

  return (
    <div className="space-y-6">
      <header className="flex items-center gap-3">
        <div className="p-2.5 rounded-2xl bg-gradient-to-br from-emerald-600 to-teal-500 shadow-lg shadow-emerald-600/20">
          <Package className="w-7 h-7 text-white" />
        </div>
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">
            Browse the <span className="brand-text">community shelf</span>
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">Borrow what you need — share what you don't.</p>
        </div>
      </header>

      {/* Controls */}
      <div className="surface rounded-2xl p-4 sm:p-5 space-y-4">
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search items by name…"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && runSearch()}
              className="input pl-10 pr-10"
            />
            {(searchTerm || activeQuery) && (
              <button onClick={clearSearch} aria-label="Clear search"
                      className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-200">
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
          <button onClick={runSearch} className="btn-primary">
            <Search className="w-4 h-4" /> Search
          </button>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={() => { setFilter('all'); setCategory(''); }}
            className={`chip px-3 py-1.5 ${filter === 'all' && !category ? 'bg-emerald-600 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300'}`}>
            All
          </button>
          <button
            onClick={() => { setFilter('available'); setCategory(''); }}
            className={`chip px-3 py-1.5 ${filter === 'available' && !category ? 'bg-emerald-600 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300'}`}>
            Available only
          </button>
          <select
            value={category}
            onChange={(e) => { setCategory(e.target.value); setFilter('all'); }}
            className="input py-1.5 w-auto text-sm">
            <option value="">All categories</option>
            {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
      </div>

      {/* Results */}
      {loading ? (
        <Spinner label="Loading items…" />
      ) : error ? (
        <EmptyState
          icon={AlertTriangle}
          title="Couldn't load items"
          message="The item service may be starting up or unreachable."
          action={<button onClick={fetchItems} className="btn-primary"><RotateCcw className="w-4 h-4" /> Retry</button>}
        />
      ) : items.length === 0 ? (
        <EmptyState
          icon={Package}
          title={activeQuery ? `No results for “${activeQuery}”` : 'Nothing here yet'}
          message={activeQuery ? 'Try a different search term.' : 'Be the first to list something for your community.'}
        />
      ) : (
        <>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {items.length} item{items.length !== 1 ? 's' : ''}
            {activeQuery && <> for “<span className="font-medium text-gray-700 dark:text-gray-300">{activeQuery}</span>”</>}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
            {items.map((item, i) => <ItemCard key={item.id} item={item} index={i} />)}
          </div>
        </>
      )}
    </div>
  );
}

export default Items;
