import { useState, useEffect, useCallback } from 'react';
import { itemsApi, borrowApi, favoritesApi, CATEGORIES, BORROW_DURATIONS, parseApiError } from '../api/api';
import { Package, Search, X, AlertTriangle, RotateCcw, HandHelping, Send } from 'lucide-react';
import ItemCard from '../components/ItemCard';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { useToast } from '../contexts/ToastContext';

function Items({ user }) {
  const me = user?.userId;
  const [items, setItems] = useState([]);
  const [favoriteIds, setFavoriteIds] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [filter, setFilter] = useState('all'); // 'all' | 'available'
  const [category, setCategory] = useState('');
  const [searchTerm, setSearchTerm] = useState('');
  const [activeQuery, setActiveQuery] = useState('');
  const [sort, setSort] = useState('newest'); // newest | popular | name
  const [borrowItem, setBorrowItem] = useState(null);
  const toast = useToast();

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

  useEffect(() => { fetchItems(); }, [fetchItems]);

  useEffect(() => {
    favoritesApi.ids().then(({ data }) => setFavoriteIds(data)).catch(() => {});
  }, []);

  const runSearch = () => setActiveQuery(searchTerm.trim());
  const clearSearch = () => { setSearchTerm(''); setActiveQuery(''); };

  const toggleFavorite = async (item) => {
    const fav = favoriteIds.includes(item.id);
    // optimistic
    setFavoriteIds((ids) => fav ? ids.filter((i) => i !== item.id) : [...ids, item.id]);
    try {
      if (fav) await favoritesApi.remove(item.id);
      else await favoritesApi.add(item.id);
    } catch (err) {
      setFavoriteIds((ids) => fav ? [...ids, item.id] : ids.filter((i) => i !== item.id));
      toast.error(parseApiError(err, 'Could not update favorites.'));
    }
  };

  const sorted = [...items].sort((a, b) => {
    if (sort === 'name') return a.name.localeCompare(b.name);
    if (sort === 'popular') return (b.borrowCount || 0) - (a.borrowCount || 0);
    return new Date(b.createdAt) - new Date(a.createdAt); // newest
  });

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
          <select value={sort} onChange={(e) => setSort(e.target.value)} className="input py-1.5 w-auto text-sm ml-auto">
            <option value="newest">Newest</option>
            <option value="popular">Most borrowed</option>
            <option value="name">Alphabetical</option>
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
      ) : sorted.length === 0 ? (
        <EmptyState
          icon={Package}
          title={activeQuery ? `No results for “${activeQuery}”` : 'Nothing here yet'}
          message={activeQuery ? 'Try a different search term.' : 'Be the first to list something for your community.'}
        />
      ) : (
        <>
          <p className="text-sm text-gray-500 dark:text-gray-400">
            {sorted.length} item{sorted.length !== 1 ? 's' : ''}
            {activeQuery && <> for “<span className="font-medium text-gray-700 dark:text-gray-300">{activeQuery}</span>”</>}
          </p>
          <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
            {sorted.map((item, i) => {
              const mine = item.ownerId === me;
              return (
                <ItemCard
                  key={item.id}
                  item={item}
                  index={i}
                  isFavorite={favoriteIds.includes(item.id)}
                  onToggleFavorite={toggleFavorite}
                  onBorrow={mine ? undefined : setBorrowItem}
                  ownerName={mine ? 'you' : undefined}
                />
              );
            })}
          </div>
        </>
      )}

      {borrowItem && (
        <BorrowModal
          item={borrowItem}
          onClose={() => setBorrowItem(null)}
          onDone={() => { setBorrowItem(null); fetchItems(); }}
        />
      )}
    </div>
  );
}

function BorrowModal({ item, onClose, onDone }) {
  const max = item.maxBorrowDays || 30;
  const durations = BORROW_DURATIONS.filter((d) => d <= max);
  const [days, setDays] = useState(durations[0] || 1);
  const [note, setNote] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  const submit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await borrowApi.create({ itemId: item.id, borrowDays: Number(days), note: note.trim() || null });
      toast.success('Borrow request sent to the owner');
      onDone();
    } catch (err) {
      toast.error(parseApiError(err, 'Could not send the request.'));
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50 backdrop-blur-sm" onClick={onClose}>
      <div className="surface rounded-2xl p-6 w-full max-w-md" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 rounded-xl bg-gradient-to-br from-emerald-600 to-teal-500">
            <HandHelping className="w-5 h-5 text-white" />
          </div>
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Request “{item.name}”</h2>
        </div>
        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Borrow duration</label>
            <select value={days} onChange={(e) => setDays(e.target.value)} className="input">
              {durations.map((d) => <option key={d} value={d}>{d} day{d > 1 ? 's' : ''}</option>)}
            </select>
          </div>
          {item.lateFeePerDay > 0 && (
            <p className="text-xs text-amber-600 dark:text-amber-400">Late returns are charged ₹{item.lateFeePerDay}/day.</p>
          )}
          {item.securityDeposit > 0 && (
            <p className="text-xs text-gray-500 dark:text-gray-400">A refundable ₹{item.securityDeposit} deposit applies.</p>
          )}
          <div>
            <label className="block text-sm font-semibold text-gray-700 dark:text-gray-300 mb-1.5">Message (optional)</label>
            <textarea value={note} onChange={(e) => setNote(e.target.value)} rows="2" maxLength={500}
                      className="input resize-none" placeholder="Tell the owner why you'd like to borrow it…" />
          </div>
          <div className="flex gap-2">
            <button type="button" onClick={onClose} className="btn-ghost flex-1">Cancel</button>
            <button type="submit" disabled={submitting} className="btn-primary flex-1">
              <Send className="w-4 h-4" /> {submitting ? 'Sending…' : 'Send request'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

export default Items;
