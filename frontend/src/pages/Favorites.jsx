import { useState, useEffect } from 'react';
import { favoritesApi, borrowApi, parseApiError, BORROW_DURATIONS } from '../api/api';
import { Heart, HandHelping, Send } from 'lucide-react';
import ItemCard from '../components/ItemCard';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { useToast } from '../contexts/ToastContext';

function Favorites({ user }) {
  const me = user?.userId;
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [borrowItem, setBorrowItem] = useState(null);
  const toast = useToast();

  const load = () => {
    setLoading(true);
    favoritesApi.list()
      .then(({ data }) => setItems(data))
      .catch((err) => toast.error(parseApiError(err, 'Could not load favorites.')))
      .finally(() => setLoading(false));
  };

  useEffect(() => { load(); /* eslint-disable-next-line */ }, []);

  const remove = async (item) => {
    setItems((prev) => prev.filter((i) => i.id !== item.id));
    try { await favoritesApi.remove(item.id); }
    catch (err) { toast.error(parseApiError(err, 'Could not update favorites.')); load(); }
  };

  if (loading) return <Spinner label="Loading your wishlist…" />;

  return (
    <div className="space-y-6">
      <header className="flex items-center gap-3">
        <div className="p-2.5 rounded-2xl bg-gradient-to-br from-rose-500 to-pink-500 shadow-lg shadow-rose-500/20">
          <Heart className="w-7 h-7 text-white" />
        </div>
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">Your wishlist</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">Items you've saved to borrow later.</p>
        </div>
      </header>

      {items.length === 0 ? (
        <EmptyState icon={Heart} title="No favorites yet" message="Tap the heart on any item to save it here." />
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4 sm:gap-6">
          {items.map((item, i) => (
            <ItemCard
              key={item.id}
              item={item}
              index={i}
              isFavorite
              onToggleFavorite={remove}
              onBorrow={item.ownerId === me ? undefined : setBorrowItem}
            />
          ))}
        </div>
      )}

      {borrowItem && (
        <BorrowModal item={borrowItem} onClose={() => setBorrowItem(null)} onDone={() => setBorrowItem(null)} />
      )}
    </div>
  );
}

function BorrowModal({ item, onClose, onDone }) {
  const max = item.maxBorrowDays || 30;
  const durations = BORROW_DURATIONS.filter((d) => d <= max);
  const [days, setDays] = useState(durations[0] || 1);
  const [submitting, setSubmitting] = useState(false);
  const toast = useToast();

  const submit = async (e) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await borrowApi.create({ itemId: item.id, borrowDays: Number(days) });
      toast.success('Borrow request sent');
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
          <select value={days} onChange={(e) => setDays(e.target.value)} className="input">
            {durations.map((d) => <option key={d} value={d}>{d} day{d > 1 ? 's' : ''}</option>)}
          </select>
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

export default Favorites;
