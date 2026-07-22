import { useState, useEffect } from 'react';
import axios from 'axios';

function ServiceStatus() {
  const [services, setServices] = useState({
    gateway: { status: 'checking', name: 'API Gateway', url: 'http://localhost:8080' },
    eureka: { status: 'checking', name: 'Discovery Server', url: 'http://localhost:8761' },
  });
  const [showStatus, setShowStatus] = useState(false);

  useEffect(() => {
    checkServices();
    const interval = setInterval(checkServices, 10000); // Check every 10 seconds
    return () => clearInterval(interval);
  }, []);

  const checkServices = async () => {
    const newStatus = { ...services };

    // Check API Gateway
    try {
      await axios.get('http://localhost:8080/actuator/health', { timeout: 3000 });
      newStatus.gateway.status = 'online';
    } catch (error) {
      newStatus.gateway.status = 'offline';
    }

    // Check Eureka
    try {
      await axios.get('http://localhost:8761/actuator/health', { timeout: 3000 });
      newStatus.eureka.status = 'online';
    } catch (error) {
      newStatus.eureka.status = 'offline';
    }

    setServices(newStatus);
  };

  const getStatusColor = (status) => {
    switch (status) {
      case 'online':
        return 'bg-green-500';
      case 'offline':
        return 'bg-red-500';
      default:
        return 'bg-yellow-500';
    }
  };

  const getStatusText = (status) => {
    switch (status) {
      case 'online':
        return '✓ Online';
      case 'offline':
        return '✗ Offline';
      default:
        return '⟳ Checking...';
    }
  };

  const allOnline = Object.values(services).every(s => s.status === 'online');
  const anyOffline = Object.values(services).some(s => s.status === 'offline');

  return (
    <div className="fixed bottom-4 right-4 z-50">
      {/* Status Indicator Dot */}
      <button
        onClick={() => setShowStatus(!showStatus)}
        className={`${
          allOnline ? 'bg-green-500' : anyOffline ? 'bg-red-500' : 'bg-yellow-500'
        } text-white rounded-full w-12 h-12 flex items-center justify-center shadow-lg hover:shadow-xl transition-all`}
        title="Service Status"
      >
        {allOnline ? '✓' : anyOffline ? '!' : '⟳'}
      </button>

      {/* Status Panel */}
      {showStatus && (
        <div className="absolute bottom-16 right-0 surface rounded-xl shadow-2xl p-4 w-64">
          <div className="flex justify-between items-center mb-3">
            <h3 className="font-semibold text-gray-800 dark:text-gray-100">Service Status</h3>
            <button
              onClick={() => setShowStatus(false)}
              className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-200"
            >
              ✕
            </button>
          </div>

          <div className="space-y-2">
            {Object.entries(services).map(([key, service]) => (
              <div key={key} className="flex items-center justify-between">
                <span className="text-sm text-gray-700 dark:text-gray-300">{service.name}</span>
                <div className="flex items-center space-x-2">
                  <span className={`w-2 h-2 rounded-full ${getStatusColor(service.status)}`} />
                  <span className="text-xs font-medium text-gray-600 dark:text-gray-400">{getStatusText(service.status)}</span>
                </div>
              </div>
            ))}
          </div>

          {anyOffline && (
            <div className="mt-3 pt-3 border-t border-gray-200 dark:border-gray-700">
              <p className="text-xs text-red-600 dark:text-red-400 mb-1">
                ⚠️ Some services are offline.
              </p>
              <p className="text-xs text-gray-500 dark:text-gray-400">
                Check the Eureka dashboard at localhost:8761.
              </p>
            </div>
          )}

          <button
            onClick={checkServices}
            className="mt-3 w-full text-xs bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 text-gray-700 dark:text-gray-300 py-2 rounded-lg"
          >
            Refresh Status
          </button>
        </div>
      )}
    </div>
  );
}

export default ServiceStatus;
