import { createContext, useContext, useState, useCallback } from 'react';
import { CheckCircle, XCircle, Info, X } from 'lucide-react';

const ToastContext = createContext(null);
let idSeq = 0;

export function ToastProvider({ children }) {
  const [toasts, setToasts] = useState([]);

  const remove = useCallback((id) => {
    setToasts((list) => list.filter((t) => t.id !== id));
  }, []);

  const push = useCallback((message, type) => {
    const id = ++idSeq;
    setToasts((list) => [...list, { id, message, type }]);
    setTimeout(() => remove(id), 4000);
  }, [remove]);

  const toast = {
    success: (m) => push(m, 'success'),
    error: (m) => push(m, 'error'),
    info: (m) => push(m, 'info'),
  };

  return (
    <ToastContext.Provider value={toast}>
      {children}
      <div className="fixed top-4 right-4 z-[100] flex flex-col gap-2 w-[calc(100%-2rem)] max-w-sm pointer-events-none">
        {toasts.map((t) => (
          <ToastItem key={t.id} {...t} onClose={() => remove(t.id)} />
        ))}
      </div>
    </ToastContext.Provider>
  );
}

function ToastItem({ message, type, onClose }) {
  const config = {
    success: { Icon: CheckCircle, color: 'text-emerald-600 dark:text-emerald-400' },
    error: { Icon: XCircle, color: 'text-red-600 dark:text-red-400' },
    info: { Icon: Info, color: 'text-sky-600 dark:text-sky-400' },
  }[type] || { Icon: Info, color: 'text-gray-500' };
  const { Icon, color } = config;

  return (
    <div className="surface pointer-events-auto rounded-xl p-3 pr-2 flex items-start gap-3"
         style={{ animation: 'fadeInUp 0.25s ease-out both' }}>
      <Icon className={`w-5 h-5 flex-shrink-0 mt-0.5 ${color}`} />
      <p className="text-sm text-gray-800 dark:text-gray-100 flex-1">{message}</p>
      <button onClick={onClose} aria-label="Dismiss notification"
              className="text-gray-400 hover:text-gray-700 dark:hover:text-gray-200 flex-shrink-0">
        <X className="w-4 h-4" />
      </button>
    </div>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
}
