import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Car, Phone, Clock, RotateCcw, CheckCircle, AlertCircle } from 'lucide-react'
import apiService from '../services/api'

const OtpVerification = ({ phoneNumber, userType, onVerificationSuccess }) => {
  const [otp, setOtp] = useState(['', '', '', '', '', ''])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [resendLoading, setResendLoading] = useState(false)
  const [countdown, setCountdown] = useState(60)
  const [canResend, setCanResend] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000)
      return () => clearTimeout(timer)
    } else {
      setCanResend(true)
    }
  }, [countdown])

  const handleOtpChange = (index, value) => {
    if (value.length > 1) return

    const newOtp = [...otp]
    newOtp[index] = value
    setOtp(newOtp)
    setError('')

    // Auto focus next input
    if (value && index < 5) {
      const nextInput = document.querySelector(`input[name="otp-${index + 1}"]`)
      if (nextInput) nextInput.focus()
    }
  }

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !otp[index] && index > 0) {
      const prevInput = document.querySelector(`input[name="otp-${index - 1}"]`)
      if (prevInput) prevInput.focus()
    }
  }

  const handleVerifyOtp = async (e) => {
    e.preventDefault()
    const otpValue = otp.join('')
    
    if (otpValue.length !== 6) {
      setError('Please enter complete 6-digit OTP')
      return
    }

    setLoading(true)
    setError('')

    try {
      console.log('Verifying OTP:', { phoneNumber, otp: otpValue })
      const response = await apiService.verifyOtp(phoneNumber, otpValue)
      
      console.log('OTP verification response:', response)
      
      // Backend returns: { message, accessToken, tokenType, user }
      if (response && response.accessToken && response.user) {
        setSuccess('Phone number verified successfully!')
        
        // Store the authentication data
        apiService.setToken(response.accessToken)
        localStorage.setItem('authToken', response.accessToken)
        localStorage.setItem('user', JSON.stringify(response.user))
        localStorage.setItem('userType', userType)
        
        setTimeout(() => {
          onVerificationSuccess(response.user, userType)
        }, 1000)
      } else if (response && response.message && response.message.includes('verified successfully')) {
        // Handle case where backend returns success message but different format
        setSuccess('Phone number verified successfully!')
        setTimeout(() => {
          onVerificationSuccess({ phoneNumber }, userType)
        }, 1000)
      } else {
        setError(response?.message || 'OTP verification failed')
      }
    } catch (err) {
      setError(err.message || 'OTP verification failed')
    } finally {
      setLoading(false)
    }
  }

  const handleResendOtp = async () => {
    if (!canResend) return

    setResendLoading(true)
    setError('')

    try {
      console.log('Resending OTP for:', phoneNumber)
      const response = await apiService.resendOtp(phoneNumber)
      
      console.log('Resend OTP response:', response)
      
      // Backend returns: { message, phoneNumber }
      if (response && (response.message?.includes('sent successfully') || response.phoneNumber)) {
        setSuccess('OTP sent successfully!')
        setCountdown(60)
        setCanResend(false)
        setOtp(['', '', '', '', '', ''])
      } else {
        setError(response?.message || 'Failed to resend OTP')
      }
    } catch (err) {
      setError(err.message || 'Failed to resend OTP')
    } finally {
      setResendLoading(false)
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
          <h2 className="text-3xl font-bold text-gray-900 mb-2">Verify Your Phone</h2>
          <p className="text-gray-600">
            We've sent a 6-digit code to{' '}
            <span className="font-semibold text-gray-900">{phoneNumber}</span>
          </p>
        </div>

        <div className="bg-white rounded-2xl shadow-xl p-8">
          <form onSubmit={handleVerifyOtp} className="space-y-6">
            {error && (
              <div className="bg-red-50 border border-red-200 rounded-lg p-4 flex items-center space-x-2">
                <AlertCircle className="h-5 w-5 text-red-500" />
                <span className="text-red-700 text-sm">{error}</span>
              </div>
            )}

            {success && (
              <div className="bg-green-50 border border-green-200 rounded-lg p-4 flex items-center space-x-2">
                <CheckCircle className="h-5 w-5 text-green-500" />
                <span className="text-green-700 text-sm">{success}</span>
              </div>
            )}

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-4 text-center">
                Enter 6-digit OTP
              </label>
              <div className="flex justify-center space-x-3">
                {otp.map((digit, index) => (
                  <input
                    key={index}
                    type="text"
                    name={`otp-${index}`}
                    value={digit}
                    onChange={(e) => handleOtpChange(index, e.target.value)}
                    onKeyDown={(e) => handleKeyDown(index, e)}
                    className="w-12 h-12 text-center text-lg font-semibold border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent outline-none transition-colors"
                    maxLength="1"
                    inputMode="numeric"
                    pattern="[0-9]"
                  />
                ))}
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || otp.join('').length !== 6}
              className="w-full bg-yellow-500 hover:bg-yellow-600 disabled:bg-yellow-300 text-white py-3 px-4 rounded-lg font-semibold transition-colors focus:ring-4 focus:ring-yellow-200 outline-none"
            >
              {loading ? 'Verifying...' : 'Verify OTP'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <div className="flex items-center justify-center space-x-2 text-sm text-gray-600 mb-4">
              <Clock className="h-4 w-4" />
              <span>
                {canResend ? 'You can resend OTP now' : `Resend OTP in ${countdown}s`}
              </span>
            </div>
            <button
              onClick={handleResendOtp}
              disabled={!canResend || resendLoading}
              className="flex items-center space-x-2 text-yellow-600 hover:text-yellow-500 disabled:text-gray-400 disabled:cursor-not-allowed font-medium mx-auto"
            >
              <RotateCcw className={`h-4 w-4 ${resendLoading ? 'animate-spin' : ''}`} />
              <span>{resendLoading ? 'Sending...' : 'Resend OTP'}</span>
            </button>
          </div>

          <div className="mt-6 text-center">
            <Link to="/register" className="text-gray-500 hover:text-gray-700 text-sm">
              ← Back to Registration
            </Link>
          </div>
        </div>

        <div className="text-center">
          <p className="text-sm text-gray-600">
            Didn't receive the code? Check your SMS or{' '}
            <button
              onClick={handleResendOtp}
              disabled={!canResend}
              className="text-yellow-600 hover:text-yellow-500 disabled:text-gray-400 font-medium"
            >
              try again
            </button>
          </p>
        </div>
      </div>
    </div>
  )
}

export default OtpVerification