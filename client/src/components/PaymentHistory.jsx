import { useState, useEffect } from 'react'
import { Calendar, Download, Filter, IndianRupee, CheckCircle, XCircle, Clock, RefreshCw } from 'lucide-react'
import apiService from '../services/api'

const PaymentHistory = ({ user, userType = 'passenger' }) => {
  const [payments, setPayments] = useState([])
  const [filteredPayments, setFilteredPayments] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [totalAmount, setTotalAmount] = useState(0)
  const [filters, setFilters] = useState({
    status: 'all',
    dateRange: 'all',
    customStartDate: '',
    customEndDate: ''
  })

  useEffect(() => {
    fetchPaymentHistory()
    fetchTotalAmount()
  }, [user.id, userType])

  useEffect(() => {
    applyFilters()
  }, [payments, filters])

  const fetchPaymentHistory = async () => {
    try {
      setLoading(true)
      setError('')
      
      let response
      if (userType === 'driver') {
        response = await apiService.getDriverPaymentHistory(user.id)
      } else {
        response = await apiService.getPassengerPaymentHistory(user.id)
      }

      if (response.success) {
        setPayments(response.data || [])
      } else {
        setError(response.message || 'Failed to fetch payment history')
      }
    } catch (error) {
      console.error('Error fetching payment history:', error)
      setError('Failed to fetch payment history')
    } finally {
      setLoading(false)
    }
  }

  const fetchTotalAmount = async () => {
    try {
      let response
      if (userType === 'driver') {
        response = await apiService.getDriverTotalEarnings(user.id)
      } else {
        response = await apiService.getPassengerTotalSpending(user.id)
      }

      if (response.success) {
        setTotalAmount(response.totalEarnings || response.totalSpending || 0)
      }
    } catch (error) {
      console.error('Error fetching total amount:', error)
    }
  }

  const applyFilters = () => {
    let filtered = [...payments]

    // Filter by status
    if (filters.status !== 'all') {
      filtered = filtered.filter(payment => 
        payment.paymentStatus.toLowerCase() === filters.status.toLowerCase()
      )
    }

    // Filter by date range
    if (filters.dateRange !== 'all') {
      const now = new Date()
      let startDate

      switch (filters.dateRange) {
        case 'today':
          startDate = new Date(now.setHours(0, 0, 0, 0))
          break
        case 'week':
          startDate = new Date(now.setDate(now.getDate() - 7))
          break
        case 'month':
          startDate = new Date(now.setMonth(now.getMonth() - 1))
          break
        case 'custom':
          if (filters.customStartDate && filters.customEndDate) {
            startDate = new Date(filters.customStartDate)
            const endDate = new Date(filters.customEndDate)
            endDate.setHours(23, 59, 59, 999)
            
            filtered = filtered.filter(payment => {
              const paymentDate = new Date(payment.createdAt)
              return paymentDate >= startDate && paymentDate <= endDate
            })
          }
          break
        default:
          break
      }

      if (filters.dateRange !== 'custom' && startDate) {
        filtered = filtered.filter(payment => 
          new Date(payment.createdAt) >= startDate
        )
      }
    }

    setFilteredPayments(filtered)
  }

  const getStatusIcon = (status) => {
    switch (status.toUpperCase()) {
      case 'COMPLETED':
        return <CheckCircle className="h-4 w-4 text-green-500" />
      case 'FAILED':
      case 'CANCELLED':
        return <XCircle className="h-4 w-4 text-red-500" />
      case 'PENDING':
      case 'CREATED':
        return <Clock className="h-4 w-4 text-yellow-500" />
      default:
        return <Clock className="h-4 w-4 text-gray-500" />
    }
  }

  const getStatusColor = (status) => {
    switch (status.toUpperCase()) {
      case 'COMPLETED':
        return 'text-green-600 bg-green-50'
      case 'FAILED':
      case 'CANCELLED':
        return 'text-red-600 bg-red-50'
      case 'PENDING':
      case 'CREATED':
        return 'text-yellow-600 bg-yellow-50'
      default:
        return 'text-gray-600 bg-gray-50'
    }
  }

  const formatDate = (dateString) => {
    return new Date(dateString).toLocaleDateString('en-IN', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    })
  }

  const exportToCSV = () => {
    const headers = userType === 'driver' 
      ? ['Date', 'Booking ID', 'Route', 'Amount', 'Earnings', 'Commission', 'Status']
      : ['Date', 'Booking ID', 'Route', 'Amount', 'Status', 'Payment Method']

    const csvData = filteredPayments.map(payment => {
      const baseData = [
        formatDate(payment.createdAt),
        payment.bookingId,
        `${payment.source} → ${payment.destination}`,
        `₹${payment.amount}`
      ]

      if (userType === 'driver') {
        return [
          ...baseData,
          `₹${payment.driverSettlementAmount || 0}`,
          `₹${payment.platformCommission || 0}`,
          payment.paymentStatus
        ]
      } else {
        return [
          ...baseData,
          payment.paymentStatus,
          payment.paymentMethod || 'N/A'
        ]
      }
    })

    const csvContent = [headers, ...csvData]
      .map(row => row.join(','))
      .join('\n')

    const blob = new Blob([csvContent], { type: 'text/csv' })
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `payment-history-${userType}-${new Date().toISOString().split('T')[0]}.csv`
    a.click()
    window.URL.revokeObjectURL(url)
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <RefreshCw className="h-6 w-6 animate-spin mr-2" />
        <span>Loading payment history...</span>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      {/* Summary Cards */}
      <div className=" grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-yellow-100 p-4 rounded-lg shadow border">
          <div className="flex items-center">
            <IndianRupee className="h-8 w-8 text-green-500 mr-3" />
            <div>
              <p className="text-sm text-gray-600">
                Total {userType === 'driver' ? 'Earnings' : 'Spending'}
              </p>
              <p className="text-2xl font-bold text-green-600">₹{totalAmount}</p>
            </div>
          </div>
        </div>
        
        <div className="bg-yellow-100 p-4 rounded-lg shadow border">
          <div className="flex items-center">
            <CheckCircle className="h-8 w-8 text-yellow-600 mr-3" />
            <div>
              <p className="text-sm text-gray-600">Total Transactions</p>
              <p className="text-2xl font-bold text-yellow-600">{payments.length}</p>
            </div>
          </div>
        </div>
        
        <div className="bg-yellow-100 p-4 rounded-lg shadow border">
          <div className="flex items-center">
            <CheckCircle className="h-8 w-8 text-green-500 mr-3" />
            <div>
              <p className="text-sm text-gray-600">Successful Payments</p>
              <p className="text-2xl font-bold text-green-600">
                {payments.filter(p => p.paymentStatus === 'COMPLETED').length}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Filters */}
      <div className="bg-yellow-100 p-4 rounded-lg shadow border">
        <div className="flex flex-wrap gap-4 items-center">
          <div className="flex items-center gap-2">
            <Filter className="h-4 w-4" />
            <span className="font-medium">Filters:</span>
          </div>
          
          <select
            value={filters.status}
            onChange={(e) => setFilters(prev => ({ ...prev, status: e.target.value }))}
            className="bg-yellow-50 border rounded px-3 py-1"
          >
            <option value="all">All Status</option>
            <option value="completed">Completed</option>
            <option value="pending">Pending</option>
            <option value="failed">Failed</option>
          </select>
          
          <select
            value={filters.dateRange}
            onChange={(e) => setFilters(prev => ({ ...prev, dateRange: e.target.value }))}
            className="bg-yellow-50 border rounded px-3 py-1"
          >
            <option value="all">All Time</option>
            <option value="today">Today</option>
            <option value="week">Last 7 Days</option>
            <option value="month">Last 30 Days</option>
            <option value="custom">Custom Range</option>
          </select>
          
          {filters.dateRange === 'custom' && (
            <>
              <input
                type="date"
                value={filters.customStartDate}
                onChange={(e) => setFilters(prev => ({ ...prev, customStartDate: e.target.value }))}
                className="border rounded px-3 py-1"
              />
              <input
                type="date"
                value={filters.customEndDate}
                onChange={(e) => setFilters(prev => ({ ...prev, customEndDate: e.target.value }))}
                className="border rounded px-3 py-1"
              />
            </>
          )}
          
          <button
            onClick={exportToCSV}
            className="ml-auto flex items-center gap-2 bg-yellow-500 text-white px-3 py-1 rounded hover:yellow-blue-700"
          >
            <Download className="h-4 w-4" />
            Export CSV
          </button>
        </div>
      </div>

      {/* Payment History Table */}
      <div className="bg-yellow-100 rounded-lg shadow border overflow-hidden">
        <div className="px-4 py-3 border-b bg-yellow-500">
          <h3 className="font-semibold">
            Payment History ({filteredPayments.length} transactions)
          </h3>
        </div>
        
        {error && (
          <div className="p-4 bg-red-50 border-l-4 border-red-400">
            <p className="text-red-700">{error}</p>
          </div>
        )}
        
        {filteredPayments.length === 0 ? (
          <div className="p-8 text-center text-gray-500">
            <Calendar className="h-12 w-12 mx-auto mb-3 opacity-50" />
            <p>No payment history found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-yellow-200">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">Date</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">Route</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">Amount</th>
                  {userType === 'driver' && (
                    <>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">Earnings</th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">Commission</th>
                    </>
                  )}
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">Status</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-700 uppercase">Method</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-yellow-700">
                {filteredPayments.map((payment) => (
                  <tr key={payment.paymentId} className="bg-yellow-100 hover:bg-yellow-400">
                    <td className="px-4 py-3 text-sm">
                      {formatDate(payment.createdAt)}
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <div>
                        <p className="font-medium">{payment.source} → {payment.destination}</p>
                        <p className="text-gray-500 text-xs">Booking #{payment.bookingId}</p>
                      </div>
                    </td>
                    <td className="px-4 py-3 text-sm font-medium">
                      ₹{payment.amount}
                    </td>
                    {userType === 'driver' && (
                      <>
                        <td className="px-4 py-3 text-sm font-medium text-green-600">
                          ₹{payment.driverSettlementAmount || 0}
                        </td>
                        <td className="px-4 py-3 text-sm text-gray-600">
                          ₹{payment.platformCommission || 0}
                        </td>
                      </>
                    )}
                    <td className="px-4 py-3 text-sm">
                      <span className={`inline-flex items-center gap-1 px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(payment.paymentStatus)}`}>
                        {getStatusIcon(payment.paymentStatus)}
                        {payment.paymentStatus}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">
                      {payment.paymentMethod || 'N/A'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

export default PaymentHistory