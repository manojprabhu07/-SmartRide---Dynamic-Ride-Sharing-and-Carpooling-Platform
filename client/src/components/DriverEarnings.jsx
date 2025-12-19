import { useState, useEffect } from 'react'
import { DollarSign, TrendingUp, Wallet, ArrowDownToLine, CheckCircle, Clock, AlertCircle } from 'lucide-react'
import apiService from '../services/api'

const DriverEarnings = ({ driverId }) => {
  const [earnings, setEarnings] = useState({
    currentBalance: 0,
    totalEarnings: 0,
    monthlyEarnings: 0,
    totalRides: 0
  })
  const [recentPayments, setRecentPayments] = useState([])
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    fetchEarnings()
    fetchRecentPayments()
    // Set up polling for real-time updates when rides are completed
    const interval = setInterval(fetchEarnings, 30000) // Check every 30 seconds
    return () => clearInterval(interval)
  }, [driverId])

  const fetchEarnings = async () => {
    try {
      const response = await apiService.getDriverTotalEarnings(driverId)
      if (response.success) {
        setEarnings(prev => ({
          ...prev,
          totalEarnings: response.totalEarnings
        }))
      }
    } catch (error) {
      console.error('Error fetching earnings:', error)
    }
  }

  const fetchRecentPayments = async () => {
    try {
      setLoading(true)
      const response = await apiService.getDriverPaymentHistory(driverId)
      if (response.success) {
        // Get only recent payments from last 24 hours
        const recent = response.data.filter(payment => {
          const paymentDate = new Date(payment.createdAt)
          const oneDayAgo = new Date(Date.now() - 24 * 60 * 60 * 1000)
          return paymentDate > oneDayAgo
        }).slice(0, 5)
        setRecentPayments(recent)
      }
    } catch (error) {
      console.error('Error fetching recent payments:', error)
    } finally {
      setLoading(false)
    }
  }

  const getPaymentIcon = (status) => {
    switch (status.toUpperCase()) {
      case 'COMPLETED':
        return <CheckCircle className="h-4 w-4 text-green-500" />
      case 'PENDING':
        return <Clock className="h-4 w-4 text-yellow-500" />
      default:
        return <AlertCircle className="h-4 w-4 text-gray-500" />
    }
  }

  const formatTime = (dateString) => {
    const date = new Date(dateString)
    const now = new Date()
    const diffMinutes = Math.floor((now - date) / (1000 * 60))
    
    if (diffMinutes < 1) return 'Just now'
    if (diffMinutes < 60) return `${diffMinutes}m ago`
    if (diffMinutes < 1440) return `${Math.floor(diffMinutes / 60)}h ago`
    return date.toLocaleDateString()
  }

  return (
    <div className="space-y-6">
      {/* Earnings Overview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gradient-to-r from-green-500 to-green-600 text-white p-6 rounded-lg shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-green-100 text-sm">Available Balance</p>
              <p className="text-3xl font-bold">₹{earnings.currentBalance || 0}</p>
              <p className="text-green-100 text-xs mt-1">Ready for withdrawal</p>
            </div>
            <Wallet className="h-12 w-12 text-green-100" />
          </div>
        </div>
        
        <div className="bg-gradient-to-r from-blue-500 to-blue-600 text-white p-6 rounded-lg shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-blue-100 text-sm">Total Earnings</p>
              <p className="text-3xl font-bold">₹{earnings.totalEarnings || 0}</p>
              <p className="text-blue-100 text-xs mt-1">Lifetime earnings</p>
            </div>
            <TrendingUp className="h-12 w-12 text-blue-100" />
          </div>
        </div>
        
        <div className="bg-gradient-to-r from-purple-500 to-purple-600 text-white p-6 rounded-lg shadow-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="text-purple-100 text-sm">This Month</p>
              <p className="text-3xl font-bold">₹{earnings.monthlyEarnings || 0}</p>
              <p className="text-purple-100 text-xs mt-1">{earnings.totalRides || 0} rides completed</p>
            </div>
            <DollarSign className="h-12 w-12 text-purple-100" />
          </div>
        </div>
      </div>

      {/* Instant Payment Feature */}
      <div className="bg-white rounded-lg shadow-lg p-6 border-l-4 border-green-500">
        <div className="flex items-center mb-4">
          <CheckCircle className="h-6 w-6 text-green-500 mr-3" />
          <h3 className="text-lg font-semibold text-gray-900">Instant Payment System</h3>
        </div>
        <div className="bg-green-50 p-4 rounded-lg">
          <h4 className="font-medium text-green-800 mb-2">🚗💰 How it works:</h4>
          <ol className="text-green-700 text-sm space-y-1">
            <li>1. Complete your ride by clicking "Mark as Complete"</li>
            <li>2. Payment is automatically calculated (after 10% platform fee)</li>
            <li>3. Money is instantly added to your wallet balance</li>
            <li>4. Withdraw anytime with UPI, Bank Transfer, or Digital Wallet</li>
          </ol>
          <div className="mt-3 p-3 bg-white rounded border border-green-200">
            <p className="text-xs text-green-600">
              ✅ <strong>Example:</strong> Passenger pays ₹2000 → You get ₹1800 instantly (₹200 platform fee)
            </p>
          </div>
        </div>
      </div>

      {/* Recent Payments */}
      <div className="bg-white rounded-lg shadow border">
        <div className="px-6 py-4 border-b bg-gray-50">
          <div className="flex items-center justify-between">
            <h3 className="text-lg font-semibold">Recent Payments (Last 24 hours)</h3>
            <div className="flex items-center text-sm text-gray-500">
              <div className="w-2 h-2 bg-green-500 rounded-full mr-2 animate-pulse"></div>
              Live updates
            </div>
          </div>
        </div>
        
        <div className="p-6">
          {loading ? (
            <div className="text-center py-4">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-500 mx-auto"></div>
              <p className="text-gray-500 mt-2">Loading recent payments...</p>
            </div>
          ) : recentPayments.length === 0 ? (
            <div className="text-center py-8 text-gray-500">
              <ArrowDownToLine className="h-12 w-12 mx-auto mb-3 opacity-50" />
              <p>No recent payments</p>
              <p className="text-sm">Complete rides to see instant payments here!</p>
            </div>
          ) : (
            <div className="space-y-3">
              {recentPayments.map((payment, index) => (
                <div key={payment.paymentId} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                  <div className="flex items-center">
                    {getPaymentIcon(payment.paymentStatus)}
                    <div className="ml-3">
                      <p className="font-medium text-sm">{payment.source} → {payment.destination}</p>
                      <p className="text-xs text-gray-500">
                        Booking #{payment.bookingId} • {formatTime(payment.createdAt)}
                      </p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-green-600">+₹{payment.driverSettlementAmount || payment.amount * 0.9}</p>
                    <p className="text-xs text-gray-500">
                      {payment.settlementStatus === 'COMPLETED' ? 'Paid' : 'Processing'}
                    </p>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        <button className="bg-blue-600 text-white p-4 rounded-lg hover:bg-blue-700 transition-colors">
          <ArrowDownToLine className="h-6 w-6 mx-auto mb-2" />
          <span className="font-medium">Withdraw Earnings</span>
          <p className="text-xs text-blue-100 mt-1">UPI • Bank • Wallet</p>
        </button>
        
        <button className="bg-gray-600 text-white p-4 rounded-lg hover:bg-gray-700 transition-colors">
          <TrendingUp className="h-6 w-6 mx-auto mb-2" />
          <span className="font-medium">View Full History</span>
          <p className="text-xs text-gray-100 mt-1">Detailed earnings report</p>
        </button>
      </div>
    </div>
  )
}

export default DriverEarnings