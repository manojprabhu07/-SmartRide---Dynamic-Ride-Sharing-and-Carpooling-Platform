import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Car, Shield, Mail, Lock, Eye, EyeOff, AlertCircle } from 'lucide-react'
import apiService from '../services/api'

const AdminLogin = ({ onLogin }) => {
  const [formData, setFormData] = useState({
    email: '',
    password: ''
  })
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

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

    try {
      const response = await apiService.adminLogin(formData.email, formData.password)
      console.log('Admin login response:', response)

      if (response.status === 'SUCCESS') {
        // Store the token (it's returned as accessToken, not token)
        const token = response.data.accessToken
        console.log('Setting admin token:', token)
        apiService.setToken(token)
        
        onLogin(response.data.admin, 'admin')
        navigate('/admin-dashboard')
      } else {
        setError(response.message || 'Admin login failed')
      }
    } catch (err) {
      setError(err.message || 'Invalid admin credentials')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-gray-800 flex items-center justify-center py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-8">
        <div className="text-center">
          <Link to="/" className="flex items-center justify-center space-x-2 mb-4">
            <div className="bg-yellow-500 p-3 rounded-lg">
              <Car className="h-8 w-8 text-white" />
            </div>
            <span className="text-3xl font-bold text-white">SmartRide</span>
          </Link>
          <div className="bg-red-100 p-3 rounded-lg mb-4">
            <Shield className="h-8 w-8 text-red-600 mx-auto mb-2" />
          </div>
          <h2 className="text-3xl font-bold text-white mb-2">Admin Access</h2>
          <p className="text-gray-300">Secure administrative login portal</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center space-x-2">
                <AlertCircle className="h-5 w-5 text-red-500" />
                <span className="text-red-700 text-sm">{error}</span>
              </div>
            )}

            <div>
              <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                Admin Email
              </label>
              <div className="relative">
                <Mail className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                <input
                  id="email"
                  name="email"
                  type="email"
                  required
                  value={formData.email}
                  onChange={handleChange}
                  className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent outline-none transition-colors"
                  placeholder="Enter admin email"
                />
              </div>
            </div>

            <div>
              <label htmlFor="password" className="block text-sm font-medium text-gray-700 mb-2">
                Admin Password
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
                  className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent outline-none transition-colors"
                  placeholder="Enter admin password"
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

            <button
              type="submit"
              disabled={loading}
              className="w-full bg-red-600 hover:bg-red-700 disabled:bg-red-400 text-white py-3 px-4 rounded-lg font-semibold transition-colors focus:ring-4 focus:ring-red-200 outline-none"
            >
              {loading ? 'Authenticating...' : 'Admin Login'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <Link to="/login" className="text-gray-600 hover:text-gray-800 text-sm">
              ← Back to User Login
            </Link>
          </div>
        </div>

        {/* Demo Admin Credentials */}
        <div className="bg-gray-800 border border-gray-700 rounded-lg p-4 text-center">
          <p className="text-sm text-gray-300 font-medium mb-2">Demo Admin Credentials:</p>
          <p className="text-xs text-gray-400">Email: admin@smartride.com</p>
          <p className="text-xs text-gray-400">Password: admin123</p>
        </div>
      </div>
    </div>
  )
}

export default AdminLogin