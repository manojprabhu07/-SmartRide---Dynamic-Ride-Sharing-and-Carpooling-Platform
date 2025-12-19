import { useState, useEffect } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { Car, Phone, Lock, Eye, EyeOff, AlertCircle, CheckCircle } from 'lucide-react'
import apiService from '../services/api'

const LoginPage = ({ onLogin }) => {
  const [formData, setFormData] = useState({
    phoneNumber: '',
    password: ''
  })
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [backendStatus, setBackendStatus] = useState(null)
  const navigate = useNavigate()
  const location = useLocation()

  // Check for registration success message and pre-fill phone number
  useEffect(() => {
    if (location.state?.message) {
      setSuccessMessage(location.state.message)
      if (location.state.phoneNumber) {
        setFormData(prev => ({ ...prev, phoneNumber: location.state.phoneNumber }))
      }
      // Clear the state to prevent showing message on refresh
      window.history.replaceState({}, document.title)
    }
  }, [location])

  // Test backend connection on component mount
  useEffect(() => {
    const testBackend = async () => {
      try {
        const status = await apiService.testConnection()
        setBackendStatus(status)
      } catch (err) {
        setBackendStatus({ ok: false, message: 'Backend unavailable' })
      }
    }
    testBackend()
  }, [])

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    })
    setError('')
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    // Basic validation
    if (!formData.phoneNumber || !formData.password) {
      setError('Please fill in all fields')
      setLoading(false)
      return
    }

    // Phone number validation
    const phoneRegex = /^[6-9]\d{9}$/
    if (!phoneRegex.test(formData.phoneNumber)) {
      setError('Please enter a valid 10-digit phone number')
      setLoading(false)
      return
    }

    try {
      console.log('Attempting login with:', { phoneNumber: formData.phoneNumber })
      const response = await apiService.login(formData.phoneNumber, formData.password)
      
      console.log('Login response received:', response)
      
      // Backend returns: { accessToken, refreshToken, tokenType, user }
      if (response && response.accessToken && response.user) {
        console.log('Login successful - storing token and user data')
        
        // Store the authentication data
        apiService.setToken(response.accessToken)
        localStorage.setItem('token', response.accessToken) // Use 'token' to match App.jsx
        localStorage.setItem('refreshToken', response.refreshToken)
        localStorage.setItem('user', JSON.stringify(response.user))
        
        // Get user role and map to userType for navigation
        const userRole = response.user.role.toLowerCase()
        let userType = 'user'
        if (userRole === 'driver') {
          userType = 'driver'
        } else if (userRole === 'user') {
          userType = 'passenger'
        }
        
        localStorage.setItem('userType', userType)
        
        console.log('Stored in localStorage:', {
          token: localStorage.getItem('token'),
          user: localStorage.getItem('user'),
          userType: localStorage.getItem('userType')
        })
        
        console.log('Calling onLogin with:', { user: response.user, userType })
        console.log('Navigating to dashboard:', `/${userType}-dashboard`)
        
        onLogin(response.user, userType)
        navigate(`/${userType}-dashboard`)
      } else {
        console.log('Login failed - invalid response format:', response)
        setError(response?.message || 'Login failed - invalid response format')
      }
    } catch (err) {
      console.error('Login error:', err)
      setError(err.message || 'Login failed. Please check your credentials.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen relative flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      {/* Background Image with Overlay */}
      <div className="absolute inset-0 z-0">
        <div 
          className="w-full h-full bg-cover bg-center bg-no-repeat"
          style={{
            backgroundImage: `url('/yellow-taxi-cab-on-city-street-with-passengers.jpg')`
          }}
        ></div>
        {/* Overlay for opacity control */}
        <div className="absolute inset-0 bg-yellow-50 bg-opacity-85"></div>
      </div>
      
      {/* Content */}
      <div className="max-w-md w-full space-y-8 relative z-10">
        <div className="text-center">
          <Link to="/" className="flex items-center justify-center space-x-2 mb-4">
            <div className="bg-yellow-500 p-3 rounded-lg">
              <Car className="h-8 w-8 text-white" />
            </div>
            <span className="text-3xl font-bold text-gray-900">SmartRide</span>
          </Link>
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Welcome Back</h2>
          <p className="text-gray-600">Sign in to your account to continue</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          {/* Backend Status Indicator */}
          {backendStatus && (
            <div className={`mb-4 p-3 rounded-lg text-sm ${
              backendStatus.ok 
                ? 'bg-green-50 text-green-700 border border-green-200' 
                : 'bg-red-50 text-red-700 border border-red-200'
            }`}>
              Backend Status: {backendStatus.message} {!backendStatus.ok && '(Make sure your Spring Boot server is running on port 8080)'}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {successMessage && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-4 flex items-center space-x-2">
                <CheckCircle className="h-5 w-5 text-green-500" />
                <span className="text-green-700 text-sm">{successMessage}</span>
              </div>
            )}

            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center space-x-2">
                <AlertCircle className="h-5 w-5 text-red-500" />
                <span className="text-red-700 text-sm">{error}</span>
              </div>
            )}

            <div>
              <label htmlFor="phoneNumber" className="block text-sm font-medium text-gray-700 mb-2">
                Phone Number
              </label>
              <div className="relative">
                <Phone className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                <input
                  id="phoneNumber"
                  name="phoneNumber"
                  type="tel"
                  required
                  value={formData.phoneNumber}
                  onChange={handleChange}
                  className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                  placeholder="Enter your phone number"
                  maxLength="10"
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                Password
              </label>
              <div className="relative">
                <Lock className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                <input
                  id="password"
                  name="password"
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={formData.password}
                  onChange={handleChange}
                  className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                  placeholder="Enter your password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
                >
                  {showPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <div className="flex items-center">
                <input
                  id="remember-me"
                  name="remember-me"
                  type="checkbox"
                  className="h-4 w-4 text-yellow-600 focus:ring-yellow-500 border-gray-300 rounded"
                />
                <label htmlFor="remember-me" className="ml-2 block text-sm text-gray-700">
                  Remember me
                </label>
              </div>
              <a href="#" className="text-sm text-yellow-600 hover:text-yellow-500">
                Forgot password?
              </a>
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-yellow-500 hover:bg-yellow-600 disabled:bg-yellow-300 text-white py-3 px-4 rounded-lg font-semibold transition-colors focus:ring-4 focus:ring-yellow-200 outline-none"
            >
              {loading ? 'Signing in...' : 'Sign In'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-gray-600">
              Don't have an account?{' '}
              <Link to="/register" className="text-yellow-600 hover:text-yellow-500 font-medium">
                Sign up here
              </Link>
            </p>
          </div>

          <div className="mt-4 text-center">
            <Link
              to="/admin-login"
              className="text-sm text-gray-500 hover:text-gray-700"
            >
              Admin Login
            </Link>
          </div>
        </div>

        {/* Demo Credentials */}
        {/* <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 text-center">
          <p className="text-sm text-blue-700 font-medium mb-2">Test Credentials:</p>
          <p className="text-xs text-blue-600">Try: 7439033371 / test123</p>
          <p className="text-xs text-gray-500 mt-1">Make sure your backend is running on localhost:8080</p>
        </div> */}
      </div>
    </div>
  )
}

export default LoginPage