import { useState, useEffect } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { Car, User, Lock, Phone, Eye, EyeOff, AlertCircle, CheckCircle } from 'lucide-react'
import OtpVerification from './OtpVerification'
import apiService from '../services/api'

const RegisterPage = ({ onLogin }) => {
  const [searchParams] = useSearchParams()
  const [selectedType, setSelectedType] = useState(searchParams.get('type') || 'passenger')
  const [formData, setFormData] = useState({
    name: '',
    email: '',
    phone: '',
    password: '',
    confirmPassword: '',
    // Driver specific fields
    vehicleModel: '',
    licensePlate: '',
    capacity: '',
    licenseNumber: '',
    licenseExpiryDate: '',
    carColor: '',
    carYear: '',
    insuranceNumber: '',
    insuranceExpiryDate: ''
  })
  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [step, setStep] = useState(1) // 1: Personal Info, 2: Vehicle Details (driver only)
  const [showOtpVerification, setShowOtpVerification] = useState(false)
  const [registeredPhone, setRegisteredPhone] = useState('')
  const [isUserRegistered, setIsUserRegistered] = useState(false) // Track if user account is created
  const navigate = useNavigate()

  useEffect(() => {
    const typeFromParams = searchParams.get('type')
    if (typeFromParams && (typeFromParams === 'driver' || typeFromParams === 'passenger')) {
      setSelectedType(typeFromParams)
    }
  }, [searchParams])

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    })
    setError('')
  }

  const handleTypeChange = (type) => {
    setSelectedType(type)
    // Reset driver-specific fields when switching to passenger
    if (type === 'passenger') {
      setFormData(prev => ({
        ...prev,
        vehicleModel: '',
        licensePlate: '',
        capacity: '',
        licenseNumber: '',
        licenseExpiryDate: '',
        carColor: '',
        carYear: '',
        insuranceNumber: '',
        insuranceExpiryDate: ''
      }))
    }
  }

  const validateStep1 = () => {
    if (!formData.name || !formData.email || !formData.phone || !formData.password || !formData.confirmPassword) {
      setError('Please fill in all required fields')
      return false
    }
    // Basic email validation
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!emailRegex.test(formData.email)) {
      setError('Please enter a valid email address')
      return false
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Passwords do not match')
      return false
    }
    if (formData.password.length < 6) {
      setError('Password must be at least 6 characters')
      return false
    }
    // Validate phone number format
    const phoneRegex = /^[6-9]\d{9}$/
    if (!phoneRegex.test(formData.phone)) {
      setError('Please enter a valid 10-digit phone number')
      return false
    }
    return true
  }

  const validateStep2 = () => {
    if (selectedType === 'driver') {
      if (!formData.vehicleModel || !formData.licensePlate || !formData.capacity || 
          !formData.licenseNumber || !formData.licenseExpiryDate || !formData.carColor || 
          !formData.carYear || !formData.insuranceNumber || !formData.insuranceExpiryDate) {
        setError('Please fill in all vehicle details')
        return false
      }
    }
    return true
  }

  const handleNextStep = (e) => {
    e.preventDefault()
    if (validateStep1()) {
      if (selectedType === 'passenger') {
        handleSubmit(e)
      } else {
        setStep(2)
      }
    }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    
    // Step 1: Register user account (personal details)
    if (step === 1 && !isUserRegistered) {
      if (!validateStep1()) return
      
      setLoading(true)
      setError('')

      try {
        console.log('Registering user with data:', {
          ...formData,
          userType: selectedType
        })

        const response = await apiService.register({
          name: formData.name,
          email: formData.email,
          phone: formData.phone,
          password: formData.password,
          userType: selectedType
        })

        console.log('Registration response:', response)

        if (response && (response.message || response.phoneNumber)) {
          setRegisteredPhone(formData.phone)
          setShowOtpVerification(true)
          setError('')
        } else {
          setError('Registration failed. Please try again.')
        }
      } catch (error) {
        console.error('Registration error:', error)
        setError(error.response?.data?.message || error.message || 'Registration failed')
      } finally {
        setLoading(false)
      }
      return
    }

    // Step 2: Add driver details (after OTP verification)
    if (step === 2 && selectedType === 'driver' && isUserRegistered) {
      if (!validateStep2()) return
      
      setLoading(true)
      setError('')

      try {
        console.log('Adding driver details:', {
          vehicleModel: formData.vehicleModel,
          licensePlate: formData.licensePlate,
          capacity: parseInt(formData.capacity),
          licenseNumber: formData.licenseNumber
        })

        // First, login the user to get the token for driver details API
        const loginResponse = await apiService.login(formData.phone, formData.password)
        
        if (loginResponse && loginResponse.accessToken) {
          // Set the token for subsequent API calls
          apiService.setToken(loginResponse.accessToken)
          
          // Now add driver details
          const driverDetailsResponse = await apiService.addDriverDetails({
            licenseNumber: formData.licenseNumber,
            licenseExpiryDate: formData.licenseExpiryDate,
            carNumber: formData.licensePlate,
            carModel: formData.vehicleModel,
            carColor: formData.carColor,
            carYear: parseInt(formData.carYear),
            insuranceNumber: formData.insuranceNumber,
            insuranceExpiryDate: formData.insuranceExpiryDate
          })

          console.log('Driver details response:', driverDetailsResponse)
          
          if (driverDetailsResponse) {
            // Store login information
            localStorage.setItem('token', loginResponse.accessToken)
            localStorage.setItem('userType', 'driver')
            localStorage.setItem('user', JSON.stringify(loginResponse.user))
            
            if (onLogin) {
              onLogin(loginResponse.user, 'driver')
            }
            navigate('/driver-dashboard')
          }
        } else {
          setError('Failed to login after registration. Please try logging in manually.')
        }
      } catch (error) {
        console.error('Driver details error:', error)
        setError(error.response?.data?.message || 'Failed to save driver details')
      } finally {
        setLoading(false)
      }
      return
    }
  }

  const handleOtpVerificationSuccess = async (user, userType) => {
    console.log('OTP verification successful for:', userType)
    setIsUserRegistered(true) // Mark user as registered
    setShowOtpVerification(false)
    
    if (userType === 'driver') {
      // For drivers, proceed to step 2 (vehicle details)
      setStep(2)
      setError('')
    } else {
      // For passengers, redirect directly to login
      navigate('/login', { 
        state: { 
          message: 'Registration successful! Please login with your credentials.',
          phoneNumber: registeredPhone 
        }
      })
    }
  }

  if (showOtpVerification) {
    return (
      <OtpVerification
        phoneNumber={registeredPhone}
        userType={selectedType}
        onVerificationSuccess={handleOtpVerificationSuccess}
      />
    )
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
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Join SmartRide</h2>
          <p className="text-gray-600">Create your account to start sharing rides</p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          {/* User Type Selection */}
          {step === 1 && (
            <div className="mb-6">
              <p className="text-sm font-medium text-gray-700 mb-3">I want to:</p>
              <div className="grid grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => handleTypeChange('passenger')}
                  className={`p-4 rounded-lg border-2 transition-colors ${
                    selectedType === 'passenger'
                      ? 'border-yellow-500 bg-yellow-50 text-yellow-700'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <User className="h-6 w-6 mx-auto mb-2" />
                  <span className="text-sm font-medium">Find Rides</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleTypeChange('driver')}
                  className={`p-4 rounded-lg border-2 transition-colors ${
                    selectedType === 'driver'
                      ? 'border-yellow-500 bg-yellow-50 text-yellow-700'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  <Car className="h-6 w-6 mx-auto mb-2" />
                  <span className="text-sm font-medium">Offer Rides</span>
                </button>
              </div>
            </div>
          )}

          {/* Step Progress */}
          {selectedType === 'driver' && (
            <div className="mb-6">
              <div className="flex items-center">
                <div className={`flex items-center justify-center w-8 h-8 rounded-full ${
                  step >= 1 ? 'bg-yellow-500 text-white' : 'bg-gray-200 text-gray-600'
                }`}>
                  {step > 1 ? <CheckCircle className="h-5 w-5" /> : '1'}
                </div>
                <div className={`flex-1 h-1 mx-2 ${step > 1 ? 'bg-yellow-500' : 'bg-gray-200'}`}></div>
                <div className={`flex items-center justify-center w-8 h-8 rounded-full ${
                  step >= 2 ? 'bg-yellow-500 text-white' : 'bg-gray-200 text-gray-600'
                }`}>
                  2
                </div>
              </div>
              <div className="flex justify-between mt-2">
                <span className="text-xs text-gray-500">Personal Info</span>
                <span className="text-xs text-gray-500">Vehicle Details</span>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center space-x-2">
                <AlertCircle className="h-5 w-5 text-red-500" />
                <span className="text-red-700 text-sm">{error}</span>
              </div>
            )}

            {step === 1 && (
              <>
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 mb-2">
                    Full Name
                  </label>
                  <div className="relative">
                    <User className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <input
                      id="name"
                      name="name"
                      type="text"
                      required
                      value={formData.name}
                      onChange={handleChange}
                      className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                      placeholder="Enter your full name"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="email" className="block text-sm font-medium text-gray-700 mb-2">
                    Email Address
                  </label>
                  <div className="relative">
                    <User className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <input
                      id="email"
                      name="email"
                      type="email"
                      required
                      value={formData.email}
                      onChange={handleChange}
                      className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                      placeholder="Enter your email address"
                    />
                  </div>
                </div>

                <div>
                  <label htmlFor="phone" className="block text-sm font-medium text-gray-700 mb-2">
                    Phone Number
                  </label>
                  <div className="relative">
                    <Phone className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <input
                      id="phone"
                      name="phone"
                      type="tel"
                      required
                      value={formData.phone}
                      onChange={handleChange}
                      className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                      placeholder="Enter 10-digit phone number"
                      maxLength="10"
                    />
                  </div>
                  <p className="text-xs text-gray-500 mt-1">We'll send you an OTP to verify this number</p>
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
                      placeholder="Create a password"
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

                <div>
                  <label htmlFor="confirmPassword" className="block text-sm font-medium text-gray-700 mb-2">
                    Confirm Password
                  </label>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                    <input
                      id="confirmPassword"
                      name="confirmPassword"
                      type={showConfirmPassword ? 'text' : 'password'}
                      required
                      value={formData.confirmPassword}
                      onChange={handleChange}
                      className="w-full pl-10 pr-10 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                      placeholder="Confirm your password"
                    />
                    <button
                      type="button"
                      onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                      className="absolute right-3 top-3 text-gray-400 hover:text-gray-600"
                    >
                      {showConfirmPassword ? <EyeOff className="h-5 w-5" /> : <Eye className="h-5 w-5" />}
                    </button>
                  </div>
                </div>
              </>
            )}

            {step === 2 && selectedType === 'driver' && (
              <>
                <div className="text-center mb-6">
                  <h3 className="text-lg font-semibold text-gray-900 mb-2">Vehicle Details</h3>
                  <p className="text-sm text-gray-600">Please provide your vehicle information</p>
                </div>

                <div>
                  <label htmlFor="vehicleModel" className="block text-sm font-medium text-gray-700 mb-2">
                    Vehicle Model
                  </label>
                  <input
                    id="vehicleModel"
                    name="vehicleModel"
                    type="text"
                    required
                    value={formData.vehicleModel}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                    placeholder="e.g., Toyota Camry 2020"
                  />
                </div>

                <div>
                  <label htmlFor="licensePlate" className="block text-sm font-medium text-gray-700 mb-2">
                    License Plate
                  </label>
                  <input
                    id="licensePlate"
                    name="licensePlate"
                    type="text"
                    required
                    value={formData.licensePlate}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                    placeholder="e.g., MH12AB1234"
                  />
                </div>

                <div>
                  <label htmlFor="capacity" className="block text-sm font-medium text-gray-700 mb-2">
                    Seating Capacity (excluding driver)
                  </label>
                  <select
                    id="capacity"
                    name="capacity"
                    required
                    value={formData.capacity}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                  >
                    <option value="">Select capacity</option>
                    <option value="1">1 passenger</option>
                    <option value="2">2 passengers</option>
                    <option value="3">3 passengers</option>
                    <option value="4">4 passengers</option>
                    <option value="5">5 passengers</option>
                    <option value="6">6+ passengers</option>
                  </select>
                </div>

                <div>
                  <label htmlFor="licenseNumber" className="block text-sm font-medium text-gray-700 mb-2">
                    Driving License Number
                  </label>
                  <input
                    id="licenseNumber"
                    name="licenseNumber"
                    type="text"
                    required
                    value={formData.licenseNumber}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                    placeholder="Enter your license number"
                  />
                </div>

                <div>
                  <label htmlFor="licenseExpiryDate" className="block text-sm font-medium text-gray-700 mb-2">
                    License Expiry Date
                  </label>
                  <input
                    id="licenseExpiryDate"
                    name="licenseExpiryDate"
                    type="date"
                    required
                    value={formData.licenseExpiryDate}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                  />
                </div>

                <div>
                  <label htmlFor="carColor" className="block text-sm font-medium text-gray-700 mb-2">
                    Car Color
                  </label>
                  <input
                    id="carColor"
                    name="carColor"
                    type="text"
                    required
                    value={formData.carColor}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                    placeholder="e.g., White"
                  />
                </div>

                <div>
                  <label htmlFor="carYear" className="block text-sm font-medium text-gray-700 mb-2">
                    Car Year
                  </label>
                  <input
                    id="carYear"
                    name="carYear"
                    type="number"
                    required
                    min="1900"
                    max="2030"
                    value={formData.carYear}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                    placeholder="e.g., 2023"
                  />
                </div>

                <div>
                  <label htmlFor="insuranceNumber" className="block text-sm font-medium text-gray-700 mb-2">
                    Insurance Number
                  </label>
                  <input
                    id="insuranceNumber"
                    name="insuranceNumber"
                    type="text"
                    required
                    value={formData.insuranceNumber}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                    placeholder="Enter insurance number"
                  />
                </div>

                <div>
                  <label htmlFor="insuranceExpiryDate" className="block text-sm font-medium text-gray-700 mb-2">
                    Insurance Expiry Date
                  </label>
                  <input
                    id="insuranceExpiryDate"
                    name="insuranceExpiryDate"
                    type="date"
                    required
                    value={formData.insuranceExpiryDate}
                    onChange={handleChange}
                    className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                  />
                </div>

                <div className="flex space-x-4">
                  <button
                    type="button"
                    onClick={() => setStep(1)}
                    className="flex-1 border border-gray-300 text-gray-700 py-3 px-4 rounded-lg font-semibold hover:bg-gray-50 transition-colors"
                  >
                    Back
                  </button>
                  <button
                    type="submit"
                    disabled={loading}
                    className="flex-1 bg-yellow-500 hover:bg-yellow-600 disabled:bg-yellow-300 text-white py-3 px-4 rounded-lg font-semibold transition-colors"
                  >
                    {loading ? 'Creating Account...' : 'Create Account'}
                  </button>
                </div>
              </>
            )}

            {(selectedType === 'passenger' || step === 1) && (
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-yellow-500 hover:bg-yellow-600 disabled:bg-yellow-300 text-white py-3 px-4 rounded-lg font-semibold transition-colors focus:ring-4 focus:ring-yellow-200 outline-none"
              >
                {loading ? 'Creating Account...' : selectedType === 'driver' ? 'Continue' : 'Create Account'}
              </button>
            )}
          </form>

          <div className="mt-6 text-center">
            <p className="text-gray-600">
              Already have an account?{' '}
              <Link to="/login" className="text-yellow-600 hover:text-yellow-500 font-medium">
                Sign in here
              </Link>
            </p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default RegisterPage