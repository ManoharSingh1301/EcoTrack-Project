import { useState, useEffect } from 'react';
import axios from 'axios';
import { API_URL } from '../api/api';

/**
 * Small floating indicator showing whether the backend is reachable.
 * Polls the Spring Boot actuator health endpoint every 10 seconds.
 */
function ServiceStatus() {
  const [status, setStatus] = useState('checking'); // 'checking' | 'online' | 'offline'
  const [showStatus, setShowStatus] = useState(false);

  useEffect(() => {
    const check = async () => {
      try {
        await axios.get(`${API_URL}/actuator/health`, { timeout: 3000 });
        setStatus('online');
      } catch {
        setStatus('offline');
      }
    };
    check();
    const interval = setInterval(check, 10000);
    return () => clearInterval(interval);
  }, []);

  const dot = status === 'online' ? 'bg-green-500' : status === 'offline' ? 'bg-red-500' : 'bg-yellow-500';
  const label = status === 'online' ? '✓ Online' : status === 'offline' ? '✗ Offline' : '⟳ Checking…';
  const glyph = status === 'online' ? '✓' : status === 'offline' ? '!' : '⟳';

  return (
    <div className="fixed bottom-4 right-4 z-50">
      <button
        onClick={() => setShowStatus(!showStatus)}
        className={`${dot} text-white rounded-full w-12 h-12 flex items-center justify-center shadow-lg hover:shadow-xl transition-all`}
        title="Backend status"
      >
        {glyph}
      </button>

      {showStatus && (
        <div className="absolute bottom-16 right-0 surface rounded-xl shadow-2xl p-4 w-64">
          <div className="flex justify-between items-center mb-3">
            <h3 className="font-semibold text-gray-800 dark:text-gray-100">Backend Status</h3>
            <button
              onClick={() => setShowStatus(false)}
              className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
            >
              ✕
            </button>
          </div>

          <div className="flex items-center justify-between">
            <span className="text-sm text-gray-700 dark:text-gray-300">EcoTrack API</span>
            <div className="flex items-center space-x-2">
              <span className={`w-2 h-2 rounded-full ${dot}`} />
              <span className="text-xs font-medium text-gray-600 dark:text-gray-400">{label}</span>
            </div>
          </div>

          {status === 'offline' && (
            <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
              <p className="text-xs text-red-600 dark:text-red-400">
                ⚠️ Cannot reach the backend at {API_URL}. Make sure it is running.
              </p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

export default ServiceStatus;
