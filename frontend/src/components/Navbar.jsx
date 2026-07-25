import { Link } from 'react-router-dom';
import { Leaf, Package, User, LogOut, LogIn, UserPlus, Moon, Sun, Menu, X, List, MessageCircle, LayoutDashboard, Inbox, Heart } from 'lucide-react';
import { useTheme } from '../contexts/ThemeContext';
import { useState } from 'react';

function Navbar({ user, onLogout }) {
  const { theme, toggleTheme } = useTheme();
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  return (
    <nav className="backdrop-blur-xl bg-white/80 dark:bg-gray-900/80 border-b border-gray-200/50 dark:border-gray-700/50 shadow-lg transition-colors duration-500 sticky top-0 z-50">
      <div className="container mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center py-3 sm:py-4">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2 text-xl sm:text-2xl font-bold text-green-600 dark:text-green-400 hover:text-green-700 dark:hover:text-green-300 transition">
            <Leaf className="w-6 h-6 sm:w-8 sm:h-8" />
            <span className="hidden xs:inline">EcoTrack</span>
          </Link>
          
          {/* Desktop Navigation */}
          <div className="hidden md:flex items-center space-x-4 lg:space-x-6">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-lg bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 transition-all duration-300"
              aria-label="Toggle theme"
            >
              {theme === 'light' ? (
                <Moon className="w-5 h-5 text-gray-700 dark:text-gray-300" />
              ) : (
                <Sun className="w-5 h-5 text-gray-700 dark:text-gray-300" />
              )}
            </button>
            
            {user ? (
              <>
                <div className="hidden lg:flex items-center space-x-2 text-gray-700 dark:text-gray-300">
                  <User className="w-5 h-5" />
                  <span className="font-medium">{user.username}</span>
                </div>
                <button
                  onClick={onLogout}
                  className="flex items-center space-x-2 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 text-white px-3 lg:px-4 py-2 rounded-lg transition shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                >
                  <LogOut className="w-5 h-5" />
                  <span className="hidden sm:inline">Logout</span>
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 transition">
                  <LogIn className="w-5 h-5" />
                  <span className="hidden lg:inline">Login</span>
                </Link>
                <Link
                  to="/register"
                  className="flex items-center space-x-2 bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-700 hover:to-green-700 text-white px-3 lg:px-4 py-2 rounded-lg transition shadow-lg hover:shadow-xl transform hover:-translate-y-0.5"
                >
                  <UserPlus className="w-5 h-5" />
                  <span className="hidden sm:inline">Register</span>
                </Link>
              </>
            )}
          </div>

          {/* Mobile Menu Button */}
          <div className="flex md:hidden items-center space-x-2">
            <button
              onClick={toggleTheme}
              className="p-2 rounded-lg bg-gray-100 dark:bg-gray-800 hover:bg-gray-200 dark:hover:bg-gray-700 transition-all duration-300"
              aria-label="Toggle theme"
            >
              {theme === 'light' ? (
                <Moon className="w-5 h-5 text-gray-700 dark:text-gray-300" />
              ) : (
                <Sun className="w-5 h-5 text-gray-700 dark:text-gray-300" />
              )}
            </button>
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              className="p-2 rounded-lg text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-800 transition"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? <X className="w-6 h-6" /> : <Menu className="w-6 h-6" />}
            </button>
          </div>
        </div>

        {/* Mobile Menu */}
        {mobileMenuOpen && (
          <div className="md:hidden py-4 space-y-3 border-t border-gray-200 dark:border-gray-700">
            {user ? (
              <>
                <div className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 px-2">
                  <User className="w-5 h-5" />
                  <span className="font-medium">{user.username}</span>
                </div>
                <Link
                  to="/dashboard"
                  className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <LayoutDashboard className="w-5 h-5" />
                  <span>Dashboard</span>
                </Link>
                <Link
                  to="/items"
                  className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Package className="w-5 h-5" />
                  <span>Browse Items</span>
                </Link>
                <Link
                  to="/borrow-requests"
                  className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Inbox className="w-5 h-5" />
                  <span>Borrow Requests</span>
                </Link>
                <Link
                  to="/favorites"
                  className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <Heart className="w-5 h-5" />
                  <span>Favorites</span>
                </Link>
                <Link
                  to="/my-items"
                  className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <List className="w-5 h-5" />
                  <span>My Items</span>
                </Link>
                <Link
                  to="/chat"
                  className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <MessageCircle className="w-5 h-5" />
                  <span>Messages</span>
                </Link>
                <Link
                  to="/profile"
                  className="flex items-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-2 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <User className="w-5 h-5" />
                  <span>Profile</span>
                </Link>
                <button
                  onClick={() => {
                    onLogout();
                    setMobileMenuOpen(false);
                  }}
                  className="w-full flex items-center justify-center space-x-2 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 text-white px-4 py-2 rounded-lg transition shadow-lg"
                >
                  <LogOut className="w-5 h-5" />
                  <span>Logout</span>
                </button>
              </>
            ) : (
              <>
                <Link
                  to="/login"
                  className="flex items-center justify-center space-x-2 text-gray-700 dark:text-gray-300 hover:text-green-600 dark:hover:text-green-400 px-4 py-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <LogIn className="w-5 h-5" />
                  <span>Login</span>
                </Link>
                <Link
                  to="/register"
                  className="flex items-center justify-center space-x-2 bg-gradient-to-r from-emerald-600 to-green-600 hover:from-emerald-700 hover:to-green-700 text-white px-4 py-2 rounded-lg transition shadow-lg"
                  onClick={() => setMobileMenuOpen(false)}
                >
                  <UserPlus className="w-5 h-5" />
                  <span>Register</span>
                </Link>
              </>
            )}
          </div>
        )}
      </div>
    </nav>
  );
}

export default Navbar;
