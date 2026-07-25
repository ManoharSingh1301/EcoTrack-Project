import { useTheme } from '../contexts/ThemeContext';
import { Package, User, List, MessageCircle, LayoutDashboard, Inbox, Heart } from 'lucide-react';
import { Link, useLocation } from 'react-router-dom';

export default function GlassSidebar({ user }) {
  const { theme } = useTheme();
  const location = useLocation();

  if (!user) return null;

  const menuItems = [
    { path: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
    { path: '/items', icon: Package, label: 'Browse Items' },
    { path: '/my-items', icon: List, label: 'My Items' },
    { path: '/borrow-requests', icon: Inbox, label: 'Borrow Requests' },
    { path: '/favorites', icon: Heart, label: 'Favorites' },
    { path: '/chat', icon: MessageCircle, label: 'Messages' },
    { path: '/profile', icon: User, label: 'Profile' },
  ];

  return (
    <aside className="hidden lg:block fixed left-0 top-16 sm:top-20 h-[calc(100vh-4rem)] sm:h-[calc(100vh-5rem)] w-56 xl:w-64 p-3 xl:p-4 z-10">
      <div className="h-full rounded-2xl backdrop-blur-xl bg-white/70 dark:bg-gray-900/70 border border-white/20 dark:border-gray-700/50 shadow-2xl p-4 xl:p-6">
        <nav className="space-y-2">
          {menuItems.map((item) => {
            const Icon = item.icon;
            const isActive = location.pathname === item.path;
            
            return (
              <Link
                key={item.path}
                to={item.path}
                className={`flex items-center gap-3 px-3 xl:px-4 py-2.5 xl:py-3 rounded-xl transition-all duration-300 ${
                  isActive
                    ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white shadow-lg'
                    : 'hover:bg-green-50 dark:hover:bg-gray-800 text-gray-700 dark:text-gray-300'
                }`}
              >
                <Icon className="w-5 h-5 flex-shrink-0" />
                <span className="font-medium text-sm xl:text-base">{item.label}</span>
              </Link>
            );
          })}
        </nav>
      </div>
    </aside>
  );
}
