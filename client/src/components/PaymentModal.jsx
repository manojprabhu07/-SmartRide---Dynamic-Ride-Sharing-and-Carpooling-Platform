import { useState } from 'react'
import { X, CreditCard, AlertCircle, CheckCircle, Loader2 } from 'lucide-react'
import apiService from '../services/api'

const PaymentModal = ({ isOpen, onClose, booking, onPaymentSuccess }) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [paymentStatus, setPaymentStatus] = useState(null) // null, 'processing', 'success', 'failed'

  if (!isOpen || !booking) return null

  const handlePayment = async () => {
    try {
      setLoading(true)
      setError('')
      setPaymentStatus('processing')

      console.log('🚀 Starting payment process for booking:', booking.id)

      // Step 1: Create payment order
      const orderResponse = await apiService.createPaymentOrder({
        bookingId: booking.id
      })

      console.log('📋 Order response:', orderResponse)

      if (!orderResponse.success) {
        throw new Error(orderResponse.message || 'Failed to create payment order')
      }

      const orderData = orderResponse.data

      // Step 2: Initialize Razorpay
      const options = {
        key: orderData.keyId,
        amount: orderData.amount * 100, // Convert to paise
        currency: orderData.currency,
        name: orderData.companyName,
        description: orderData.description,
        order_id: orderData.orderId,
        prefill: {
          email: orderData.contactEmail,
          contact: orderData.contactPhone
        },
        theme: {
          color: '#3B82F6'
        },
        handler: async function (response) {
          try {
            // Step 3: Verify payment
            const verificationResponse = await apiService.verifyPayment({
              razorpay_order_id: response.razorpay_order_id,
              razorpay_payment_id: response.razorpay_payment_id,
              razorpay_signature: response.razorpay_signature,
              paymentMethod: response.method || 'unknown'
            })

            if (verificationResponse.success && verificationResponse.verified) {
              setPaymentStatus('success')
              setTimeout(() => {
                onPaymentSuccess(booking.id)
                onClose()
              }, 2000)
            } else {
              throw new Error('Payment verification failed')
            }
          } catch (verifyError) {
            console.error('Payment verification error:', verifyError)
            setPaymentStatus('failed')
            setError('Payment verification failed. Please contact support.')
          }
        },
        modal: {
          ondismiss: function () {
            if (paymentStatus === 'processing') {
              setPaymentStatus('failed')
              setError('Payment was cancelled by user')
            }
          }
        }
      }

      // Check if Razorpay is loaded
      if (typeof window.Razorpay === 'undefined') {
        throw new Error('Razorpay SDK not loaded. Please refresh the page.')
      }

      const rzp = new window.Razorpay(options)
      rzp.on('payment.failed', function (response) {
        console.error('Payment failed:', response.error)
        setPaymentStatus('failed')
        setError(`Payment failed: ${response.error.description}`)
        
        // Notify backend about payment failure
        apiService.handlePaymentFailure({
          razorpay_order_id: orderData.orderId,
          reason: response.error.description
        }).catch(console.error)
      })

      rzp.open()

    } catch (error) {
      console.error('Payment initiation error:', error)
      setPaymentStatus('failed')
      setError(error.message || 'Failed to initiate payment')
    } finally {
      setLoading(false)
    }
  }

  const getStatusIcon = () => {
    switch (paymentStatus) {
      case 'processing':
        return <Loader2 className="h-8 w-8 text-blue-500 animate-spin" />
      case 'success':
        return <CheckCircle className="h-8 w-8 text-green-500" />
      case 'failed':
        return <AlertCircle className="h-8 w-8 text-red-500" />
      default:
        return <CreditCard className="h-8 w-8 text-blue-500" />
    }
  }

  const getStatusMessage = () => {
    switch (paymentStatus) {
      case 'processing':
        return 'Processing payment...'
      case 'success':
        return 'Payment successful! Redirecting...'
      case 'failed':
        return 'Payment failed!'
      default:
        return 'Ready to pay'
    }
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg p-6 w-full max-w-md mx-4">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-semibold">Payment</h3>
          <button
            onClick={onClose}
            className="text-gray-400 hover:text-gray-600"
            disabled={paymentStatus === 'processing'}
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Booking Details */}
        <div className="mb-6 p-4 bg-gray-50 rounded-lg">
          <h4 className="font-medium mb-2">Booking Details</h4>
          <div className="space-y-1 text-sm text-gray-600">
            <p><strong>Route:</strong> {booking.ride?.source} → {booking.ride?.destination}</p>
            <p><strong>Date:</strong> {new Date(booking.ride?.departureDate).toLocaleDateString()}</p>
            <p><strong>Seats:</strong> {booking.seatsBooked}</p>
            <p><strong>Amount:</strong> ₹{booking.totalAmount}</p>
          </div>
        </div>

        {/* Payment Status */}
        <div className="mb-6 text-center">
          <div className="mb-3">
            {getStatusIcon()}
          </div>
          <p className="text-lg font-medium">{getStatusMessage()}</p>
          {error && (
            <p className="text-red-500 text-sm mt-2">{error}</p>
          )}
        </div>

        {/* Payment Actions */}
        {paymentStatus !== 'success' && paymentStatus !== 'processing' && (
          <div className="space-y-3">
            <button
              onClick={handlePayment}
              disabled={loading}
              className="w-full bg-blue-600 text-white py-2 px-4 rounded-lg hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {loading ? (
                <>
                  <Loader2 className="h-4 w-4 animate-spin" />
                  Processing...
                </>
              ) : (
                <>
                  <CreditCard className="h-4 w-4" />
                  Pay ₹{booking.totalAmount}
                </>
              )}
            </button>
            
            <button
              onClick={onClose}
              disabled={loading}
              className="w-full bg-gray-200 text-gray-800 py-2 px-4 rounded-lg hover:bg-gray-300 disabled:opacity-50"
            >
              Cancel
            </button>
          </div>
        )}

        {/* Payment Methods Info */}
        {paymentStatus === null && (
          <div className="mt-4 p-3 bg-blue-50 rounded-lg">
            <p className="text-xs text-blue-600">
              Secure payment powered by Razorpay. Supports UPI, Cards, Net Banking, and Wallets.
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

export default PaymentModal