import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { itemsApi, borrowApi, favoritesApi, parseApiError } from '../api/api';
import {
  LayoutDashboard, Package, HandHelping, Repeat, Heart,
  Inbox, Clock, Leaf, ArrowRight, TrendingUp,
} from 'lucide-react';
import Spinner from '../components/Spinner';
import { useToast } from '../contexts/ToastContext';

// Rough sustainability estimate: each successful borrow avoids one new purchase.
const MONEY_SAVED_PER_BORROW = 500; // ₹
const WASTE_PER_BORROW_KG = 2;

const TONES = {
  emerald: 'bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 dark:text-emerald-300',
  amber: 'bg-amber-100 dark:bg-amber-900/40 text-amber-600 dark:text-amber-300',
  sky: 'bg-sky-100 dark:bg-sky-900/40 text-sky-600 dark:text-sky-300',
  violet: 'bg-violet-100 dark:bg-violet-900/40 text-violet-600 dark:text-violet-300',
  rose: 'bg-rose-100 dark:bg-rose-900/40 text-rose-600 dark:text-rose-300',
  teal: 'bg-teal-100 dark:bg-teal-900/40 text-teal-600 dark:text-teal-300',
};

function StatCard({ icon: Icon, label, value, tone = 'emerald', to }) {
  const t = TONES[tone] || TONES.emerald;
  const body = (
    <div className="surface rounded-2xl p-5 flex items-center gap-4 hover:shadow-xl transition">
      <div className={`p-3 rounded-xl ${t}`}>
        <Icon className="w-6 h-6" />
      </div>
      <div>
        <p className="text-2xl font-bold text-gray-900 dark:text-white">{value}</p>
        <p className="text-sm text-gray-500 dark:text-gray-400">{label}</p>
      </div>
    </div>
  );
  return to ? <Link to={to}>{body}</Link> : body;
}

function Dashboard({ user }) {
  const [loading, setLoading] = useState(true);
  const [stats, setStats] = useState(null);
  const toast = useToast();

  useEffect(() => {
    Promise.all([
      itemsApi.getItemsByOwner(user.userId),
      borrowApi.incoming(),
      borrowApi.outgoing(),
      favoritesApi.ids(),
    ])
      .then(([itemsRes, incomingRes, outgoingRes, favRes]) => {
        const incoming = incomingRes.data;
        const outgoing = outgoingRes.data;
        setStats({
          myItems: itemsRes.data.length,
          pendingIncoming: incoming.filter((r) => r.status === 'PENDING').length,
          activeLendings: incoming.filter((r) => r.status === 'ACCEPTED').length,
          activeBorrowings: outgoing.filter((r) => r.status === 'ACCEPTED').length,
          pendingOutgoing: outgoing.filter((r) => r.status === 'PENDING').length,
          completed: [...incoming, ...outgoing].filter((r) => r.status === 'RETURNED').length,
          lendCompleted: incoming.filter((r) => r.status === 'RETURNED').length,
          favorites: favRes.data.length,
        });
      })
      .catch((err) => toast.error(parseApiError(err, 'Could not load your dashboard.')))
      .finally(() => setLoading(false));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user.userId]);

  if (loading) return <Spinner label="Loading your dashboard…" />;
  if (!stats) return null;

  const moneySaved = stats.completed * MONEY_SAVED_PER_BORROW;
  const wasteSaved = stats.completed * WASTE_PER_BORROW_KG;

  return (
    <div className="space-y-6">
      <header className="flex items-center gap-3">
        <div className="p-2.5 rounded-2xl bg-gradient-to-br from-emerald-600 to-teal-500 shadow-lg shadow-emerald-600/20">
          <LayoutDashboard className="w-7 h-7 text-white" />
        </div>
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">
            Welcome back, <span className="brand-text">{user.fullName || user.username}</span>
          </h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">Here's what's happening in your sharing community.</p>
        </div>
      </header>

      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard icon={Package} label="My items" value={stats.myItems} to="/my-items" />
        <StatCard icon={Inbox} label="Requests to review" value={stats.pendingIncoming} tone="amber" to="/borrow-requests" />
        <StatCard icon={HandHelping} label="Currently borrowing" value={stats.activeBorrowings} tone="sky" to="/borrow-requests" />
        <StatCard icon={Repeat} label="Currently lending" value={stats.activeLendings} tone="violet" to="/borrow-requests" />
        <StatCard icon={Clock} label="Pending requests sent" value={stats.pendingOutgoing} tone="amber" to="/borrow-requests" />
        <StatCard icon={TrendingUp} label="Completed borrows" value={stats.completed} tone="emerald" />
        <StatCard icon={Heart} label="Favorites" value={stats.favorites} tone="rose" to="/favorites" />
        <StatCard icon={Repeat} label="Items lent out" value={stats.lendCompleted} tone="teal" />
      </div>

      {/* Eco impact */}
      <div className="surface rounded-2xl p-6">
        <div className="flex items-center gap-2 mb-4">
          <Leaf className="w-5 h-5 text-emerald-600 dark:text-emerald-400" />
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">Your eco impact</h2>
        </div>
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
          <div>
            <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">{stats.completed}</p>
            <p className="text-sm text-gray-500 dark:text-gray-400">Successful shares</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">₹{moneySaved.toLocaleString()}</p>
            <p className="text-sm text-gray-500 dark:text-gray-400">Estimated money saved</p>
          </div>
          <div>
            <p className="text-2xl font-bold text-emerald-600 dark:text-emerald-400">{wasteSaved} kg</p>
            <p className="text-sm text-gray-500 dark:text-gray-400">Estimated waste avoided</p>
          </div>
        </div>
      </div>

      <div className="flex flex-wrap gap-3">
        <Link to="/items" className="btn-primary"><Package className="w-4 h-4" /> Browse items</Link>
        <Link to="/borrow-requests" className="btn-ghost"><Inbox className="w-4 h-4" /> Manage requests <ArrowRight className="w-4 h-4" /></Link>
      </div>
    </div>
  );
}

export default Dashboard;
