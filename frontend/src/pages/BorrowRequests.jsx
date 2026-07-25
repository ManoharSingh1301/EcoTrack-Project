import { useState, useEffect, useCallback } from 'react';
import { borrowApi, usersApi, parseApiError } from '../api/api';
import {
  Inbox, Send, Check, X, RotateCcw, Clock, AlertTriangle, PackageCheck,
} from 'lucide-react';
import Spinner from '../components/Spinner';
import EmptyState from '../components/EmptyState';
import { useToast } from '../contexts/ToastContext';

const STATUS_STYLE = {
  PENDING: 'bg-amber-100 text-amber-800 dark:bg-amber-900/50 dark:text-amber-200',
  ACCEPTED: 'bg-sky-100 text-sky-800 dark:bg-sky-900/50 dark:text-sky-200',
  REJECTED: 'bg-rose-100 text-rose-800 dark:bg-rose-900/50 dark:text-rose-200',
  RETURNED: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900/50 dark:text-emerald-200',
  CANCELLED: 'bg-gray-200 text-gray-700 dark:bg-gray-800 dark:text-gray-300',
};

function fmt(d) {
  return d ? new Date(d).toLocaleDateString([], { day: 'numeric', month: 'short', year: 'numeric' }) : '—';
}

function RequestRow({ r, role, onAction, nameOf }) {
  const person = role === 'incoming' ? nameOf(r.borrowerId) : nameOf(r.ownerId);
  return (
    <div className="surface rounded-2xl p-4 sm:p-5 space-y-3">
      <div className="flex items-start justify-between gap-3">
        <div>
          <h3 className="font-semibold text-gray-900 dark:text-white">{r.itemName || `Item #${r.itemId}`}</h3>
          <p className="text-xs text-gray-500 dark:text-gray-400">
            {role === 'incoming' ? `Requested by ${person}` : `Owner: ${person}`} · {r.borrowDays} day{r.borrowDays > 1 ? 's' : ''}
          </p>
        </div>
        <span className={`chip ${STATUS_STYLE[r.status]}`}>{r.status}</span>
      </div>

      {r.note && <p className="text-sm text-gray-600 dark:text-gray-300 italic">“{r.note}”</p>}

      <div className="flex flex-wrap gap-x-4 gap-y-1 text-xs text-gray-500 dark:text-gray-400">
        <span>Requested {fmt(r.requestDate)}</span>
        {r.dueDate && <span className={r.overdue ? 'text-rose-600 dark:text-rose-400 font-medium' : ''}>Due {fmt(r.dueDate)}{r.overdue ? ' (overdue)' : ''}</span>}
        {r.returnDate && <span>Returned {fmt(r.returnDate)}</span>}
        {r.lateFee > 0 && <span className="text-rose-600 dark:text-rose-400 font-medium">Late fee ₹{r.lateFee}</span>}
        {r.securityDeposit > 0 && <span>Deposit ₹{r.securityDeposit}</span>}
      </div>

      {/* Actions */}
      <div className="flex flex-wrap gap-2 pt-1">
        {role === 'incoming' && r.status === 'PENDING' && (
          <>
            <button onClick={() => onAction('accept', r)} className="btn-primary py-1.5 px-3 text-sm"><Check className="w-4 h-4" /> Accept</button>
            <button onClick={() => onAction('reject', r)} className="btn-danger py-1.5 px-3 text-sm"><X className="w-4 h-4" /> Reject</button>
          </>
        )}
        {role === 'incoming' && r.status === 'ACCEPTED' && (
          <button onClick={() => onAction('return', r)} className="btn-ghost py-1.5 px-3 text-sm"><PackageCheck className="w-4 h-4" /> Mark returned</button>
        )}
        {role === 'outgoing' && r.status === 'PENDING' && (
          <button onClick={() => onAction('cancel', r)} className="btn-ghost py-1.5 px-3 text-sm"><X className="w-4 h-4" /> Cancel request</button>
        )}
        {role === 'outgoing' && r.status === 'ACCEPTED' && (
          <button onClick={() => onAction('return', r)} className="btn-ghost py-1.5 px-3 text-sm"><PackageCheck className="w-4 h-4" /> Mark returned</button>
        )}
      </div>
    </div>
  );
}

function BorrowRequests() {
  const [tab, setTab] = useState('incoming');
  const [incoming, setIncoming] = useState([]);
  const [outgoing, setOutgoing] = useState([]);
  const [names, setNames] = useState({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const toast = useToast();

  const nameOf = (id) => names[id] || `user #${id}`;

  const load = useCallback(async () => {
    setLoading(true);
    setError(false);
    try {
      const [inc, out, usersRes] = await Promise.all([
        borrowApi.incoming(), borrowApi.outgoing(), usersApi.getAllUsers().catch(() => ({ data: [] })),
      ]);
      setIncoming(inc.data);
      setOutgoing(out.data);
      const map = {};
      (usersRes.data || []).forEach((u) => { map[u.id] = u.fullName || u.username; });
      setNames(map);
    } catch (err) {
      setError(true);
      toast.error(parseApiError(err, 'Could not load borrow requests.'));
    } finally {
      setLoading(false);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => { load(); }, [load]);

  const onAction = async (action, r) => {
    try {
      const fn = { accept: borrowApi.accept, reject: borrowApi.reject, cancel: borrowApi.cancel, return: borrowApi.markReturned }[action];
      const { data } = await fn(r.id);
      const msg = {
        accept: 'Request accepted', reject: 'Request rejected',
        cancel: 'Request cancelled', return: data.lateFee > 0 ? `Returned — late fee ₹${data.lateFee}` : 'Marked as returned',
      }[action];
      toast.success(msg);
      load();
    } catch (err) {
      toast.error(parseApiError(err, 'Action failed.'));
    }
  };

  const pendingIncoming = incoming.filter((r) => r.status === 'PENDING').length;
  const list = tab === 'incoming' ? incoming : outgoing;

  return (
    <div className="space-y-6">
      <header className="flex items-center gap-3">
        <div className="p-2.5 rounded-2xl bg-gradient-to-br from-emerald-600 to-teal-500 shadow-lg shadow-emerald-600/20">
          <Inbox className="w-7 h-7 text-white" />
        </div>
        <div className="flex-1">
          <h1 className="text-2xl sm:text-3xl font-bold text-gray-900 dark:text-white">Borrow requests</h1>
          <p className="text-sm text-gray-500 dark:text-gray-400">Approve requests for your items and track what you've borrowed.</p>
        </div>
        <button onClick={load} className="btn-ghost" aria-label="Refresh"><RotateCcw className="w-4 h-4" /></button>
      </header>

      <div className="flex gap-2">
        <button onClick={() => setTab('incoming')}
                className={`chip px-4 py-2 ${tab === 'incoming' ? 'bg-emerald-600 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300'}`}>
          <Inbox className="w-4 h-4" /> Requests for my items
          {pendingIncoming > 0 && <span className="ml-1 px-1.5 py-0.5 rounded-full bg-white/25 text-xs">{pendingIncoming}</span>}
        </button>
        <button onClick={() => setTab('outgoing')}
                className={`chip px-4 py-2 ${tab === 'outgoing' ? 'bg-emerald-600 text-white' : 'bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-300'}`}>
          <Send className="w-4 h-4" /> My borrow requests
        </button>
      </div>

      {loading ? (
        <Spinner label="Loading requests…" />
      ) : error ? (
        <EmptyState icon={AlertTriangle} title="Couldn't load requests" message="Please try again."
                    action={<button onClick={load} className="btn-primary"><RotateCcw className="w-4 h-4" /> Retry</button>} />
      ) : list.length === 0 ? (
        <EmptyState
          icon={tab === 'incoming' ? Inbox : Clock}
          title={tab === 'incoming' ? 'No requests yet' : 'No borrow requests sent'}
          message={tab === 'incoming' ? 'When someone requests one of your items, it shows up here.' : 'Browse the community shelf and request something to borrow.'}
        />
      ) : (
        <div className="grid sm:grid-cols-2 gap-4">
          {list.map((r) => <RequestRow key={r.id} r={r} role={tab} onAction={onAction} nameOf={nameOf} />)}
        </div>
      )}
    </div>
  );
}

export default BorrowRequests;
