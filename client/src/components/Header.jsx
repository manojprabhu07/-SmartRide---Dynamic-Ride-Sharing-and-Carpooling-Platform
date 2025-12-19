import { Link } from 'react-router-dom'
import { Car, User, LogOut, Menu, X } from 'lucide-react'
import { useState } from 'react'

const Header = ({ user, userType, onLogout }) => {
  const [isMenuOpen, setIsMenuOpen] = useState(false)

  const toggleMenu = () => {
    setIsMenuOpen(!isMenuOpen)
  }

  return (
    <header className="bg-white shadow-md sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center py-4">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-2">
            <div className="bg-yellow-500 p-2 rounded-lg">
              <Car className="h-6 w-6 text-white" />
            </div>
            <span className="text-2xl font-bold text-gray-900">SmartRide</span>
          </Link>

          {/* Desktop Navigation */}
          <nav className="hidden md:flex items-center space-x-8">
            <Link to="/#features" className="text-gray-600 hover:text-yellow-600 transition-colors">
              Features
            </Link>
            <Link to="/#how-it-works" className="text-gray-600 hover:text-yellow-600 transition-colors">
              How It Works
            </Link>
            <Link to="/#about" className="text-gray-600 hover:text-yellow-600 transition-colors">
              About
            </Link>
          </nav>

          {/* Desktop Auth Buttons */}
          <div className="hidden md:flex items-center space-x-4">
            {user ? (
              <div className="flex items-center space-x-4">
                <Link
                  to={
                    userType === 'driver' ? '/driver-dashboard' : 
                    userType === 'passenger' ? '/passenger-dashboard' : 
                    '/admin-dashboard'
                  }
                  className="flex items-center space-x-2 text-gray-700 hover:text-yellow-600 transition-colors"
                >
                  <User className="h-5 w-5" />
                  <span>{user.name || user.email}</span>
                </Link>
                <button
                  onClick={onLogout}
                  className="flex items-center space-x-2 text-gray-600 hover:text-red-600 transition-colors"
                >
                  <LogOut className="h-5 w-5" />
                  <span>Logout</span>
                </button>
              </div>
            ) : (
              <>
                <Link
                  to="/login"
                  className="text-gray-600 hover:text-yellow-600 transition-colors"
                >
                  Sign In
                </Link>
                <Link
                  to="/register"
                  className="bg-yellow-500 hover:bg-yellow-600 text-white px-6 py-2 rounded-lg font-medium transition-colors"
                >
                  Get Started
                </Link>
              </>
            )}
          </div>

          {/* Mobile Menu Button */}
          <button
            className="md:hidden"
            onClick={toggleMenu}
          >
            {isMenuOpen ? (
              <X className="h-6 w-6 text-gray-600" />
            ) : (
              <Menu className="h-6 w-6 text-gray-600" />
            )}
          </button>
        </div>

        {/* Mobile Menu */}
        {isMenuOpen && (
          <div className="md:hidden py-4 border-t border-gray-200">
            <nav className="flex flex-col space-y-4">
              <Link
                to="/#features"
                className="text-gray-600 hover:text-yellow-600 transition-colors"
                onClick={() => setIsMenuOpen(false)}
              >
                Features
              </Link>
              <Link
                to="/#how-it-works"
                className="text-gray-600 hover:text-yellow-600 transition-colors"
                onClick={() => setIsMenuOpen(false)}
              >
                How It Works
              </Link>
              <Link
                to="/#about"
                className="text-gray-600 hover:text-yellow-600 transition-colors"
                onClick={() => setIsMenuOpen(false)}
              >
                About
              </Link>
              
              {user ? (
                <div className="flex flex-col space-y-4 pt-4 border-t border-gray-200">
                  <Link
                    to={
                      userType === 'driver' ? '/driver-dashboard' : 
                      userType === 'passenger' ? '/passenger-dashboard' : 
                      '/admin-dashboard'
                    }
                    className="flex items-center space-x-2 text-gray-700"
                    onClick={() => setIsMenuOpen(false)}
                  >
                    <User className="h-5 w-5" />
                    <span>{user.name || user.email}</span>
                  </Link>
                  <button
                    onClick={() => {
                      onLogout()
                      setIsMenuOpen(false)
                    }}
                    className="flex items-center space-x-2 text-red-600"
                  >
                    <LogOut className="h-5 w-5" />
                    <span>Logout</span>
                  </button>
                </div>
              ) : (
                <div className="flex flex-col space-y-4 pt-4 border-t border-gray-200">
                  <Link
                    to="/login"
                    className="text-gray-600 hover:text-yellow-600 transition-colors"
                    onClick={() => setIsMenuOpen(false)}
                  >
                    Sign In
                  </Link>
                  <Link
                    to="/register"
                    className="bg-yellow-500 hover:bg-yellow-600 text-white px-6 py-2 rounded-lg font-medium transition-colors text-center"
                    onClick={() => setIsMenuOpen(false)}
                  >
                    Get Started
                  </Link>
                </div>
              )}
            </nav>
          </div>
        )}
      </div>
    </header>
  )
}

export default Header