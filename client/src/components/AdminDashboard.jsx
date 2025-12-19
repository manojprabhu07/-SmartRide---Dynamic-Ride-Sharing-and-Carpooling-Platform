import React, { useState, useEffect } from 'react'
import { 
  Users, Car, BarChart3, TrendingUp, Eye, Trash2, Check, X, 
  RefreshCw, Search, AlertTriangle, User, Phone, Mail, Calendar, Star
} from 'lucide-react'
import apiService from '../services/api'
import Loader from './Loader'

// Star Rating Component for displaying ratings
const StarRating = ({ rating, totalReviews, showCount = true, size = 'sm' }) => {
  const starSize = size === 'lg' ? 'h-5 w-5' : 'h-4 w-4'
  
  const renderStars = () => {
    const stars = []
    const fullStars = Math.floor(rating)
    const hasHalfStar = rating % 1 !== 0
    
    for (let i = 0; i < 5; i++) {
      if (i < fullStars) {
        stars.push(
          <Star key={i} className={`${starSize} text-yellow-400 fill-current`} />
        )
      } else if (i === fullStars && hasHalfStar) {
        stars.push(
          <div key={i} className="relative">
            <Star className={`${starSize} text-gray-300`} />
            <div className="absolute inset-0 overflow-hidden" style={{ width: '50%' }}>
              <Star className={`${starSize} text-yellow-400 fill-current`} />
            </div>
          </div>
        )
      } else {
        stars.push(
          <Star key={i} className={`${starSize} text-gray-300`} />
        )
      }
    }
    return stars
  }

  return (
    <div className="flex items-center space-x-1">
      <div className="flex space-x-1">
        {renderStars()}
      </div>
      <span className="text-sm font-medium text-gray-700">
        {rating.toFixed(1)}
      </span>
      {showCount && totalReviews > 0 && (
        <span className="text-xs text-gray-500">
          ({totalReviews} review{totalReviews !== 1 ? 's' : ''})
        </span>
      )}
      {totalReviews === 0 && (
        <span className="text-xs text-gray-400">No ratings yet</span>
      )}
    </div>
  )
}

const AdminDashboard = ({ user }) => {
  const [activeTab, setActiveTab] = useState('overview')
  const [isDashboardLoading, setIsDashboardLoading] = useState(true)
  
  // State for different data types
  const [allUsers, setAllUsers] = useState([])
  const [allDrivers, setAllDrivers] = useState([])
  const [loading, setLoading] = useState(false)
  const [driversLoading, setDriversLoading] = useState(false)
  const [usersLoading, setUsersLoading] = useState(false)
  const [overviewLoading, setOverviewLoading] = useState(false)
  const [error, setError] = useState('')
  
  // Filter states
  const [userSearch, setUserSearch] = useState('')
  const [userTypeFilter, setUserTypeFilter] = useState('all')
  const [driverStatusFilter, setDriverStatusFilter] = useState('all')

  // Pagination states
  const [userCurrentPage, setUserCurrentPage] = useState(1)
  const [driverCurrentPage, setDriverCurrentPage] = useState(1)
  const itemsPerPage = 10

  // Stats state
  const [stats, setStats] = useState({
    totalUsers: 0,
    totalDrivers: 0,
    pendingVerifications: 0,
    verifiedDrivers: 0
  })

  useEffect(() => {
    // Initial dashboard loading
    const initializeDashboard = async () => {
      await fetchDashboardData()
      // Simulate initial loading time
      setTimeout(() => {
        setIsDashboardLoading(false)
      }, 2000)
    }
    
    initializeDashboard()
  }, [])

  useEffect(() => {
    switch (activeTab) {
      case 'overview':
        fetchDashboardData()
        break
      case 'users':
        fetchAllUsers()
        break
      case 'drivers':
        fetchAllDrivers()
        break
    }
  }, [activeTab])

  // Handle tab switching - no more full page loading
  const handleTabSwitch = (newTab) => {
    if (newTab === activeTab) return // Don't switch if already on the same tab
    setActiveTab(newTab)
  }

  const fetchDashboardData = async () => {
    try {
      setOverviewLoading(true)
      setError('')
      
      // Increased loading time for partial loader
      await new Promise(resolve => setTimeout(resolve, 1500))
      
      const [usersResponse, driversResponse] = await Promise.all([
        apiService.getAllUsers(),
        apiService.getAllDriversWithRatings() // Use the new API with ratings
      ])
      
      if (usersResponse.status === 'SUCCESS') {
        setAllUsers(usersResponse.data || [])
      }
      
      if (driversResponse.status === 'SUCCESS') {
        console.log('Raw drivers data:', driversResponse.data)
        setAllDrivers(driversResponse.data || [])
        // Log first driver for debugging
        if (driversResponse.data && driversResponse.data.length > 0) {
          const firstDriver = driversResponse.data[0]
          console.log('=== DRIVER DEBUG INFO ===')
          console.log('Full driver object:', firstDriver)
          console.log('All driver properties:', Object.keys(firstDriver))
          console.log('Verification fields check:', {
            verified: firstDriver.verified,
            verifiedType: typeof firstDriver.verified,
            isVerified: firstDriver.isVerified,
            isVerifiedType: typeof firstDriver.isVerified,
            verification: firstDriver.verification,
            verificationType: typeof firstDriver.verification
          })
          
          // Check the last driver (should be the pending one)
          const lastDriver = driversResponse.data[driversResponse.data.length - 1]
          console.log('=== LAST DRIVER (PENDING) DEBUG ===')
          console.log('Last driver object:', lastDriver)
          console.log('Last driver verification fields:', {
            verified: lastDriver.verified,
            verifiedType: typeof lastDriver.verified,
            isVerified: lastDriver.isVerified,
            isVerifiedType: typeof lastDriver.isVerified
          })
          console.log('=== END DEBUG INFO ===')
        }
      }
      
      updateStats(usersResponse.data || [], driversResponse.data || [])
    } catch (err) {
      console.error('Error fetching dashboard data:', err)
      setError('Failed to fetch dashboard data: ' + err.message)
    } finally {
      setOverviewLoading(false)
    }
  }

  // User Management Functions
  const fetchAllUsers = async () => {
    try {
      setUsersLoading(true)
      setError('')
      
      // Increased loading time for partial loader
      await new Promise(resolve => setTimeout(resolve, 1500))
      
      const response = await apiService.getAllUsers()
      console.log('Users API Response:', response)
      
      if (response.status === 'SUCCESS') {
        setAllUsers(response.data || [])
      } else {
        setError('Failed to fetch users: ' + response.message)
      }
    } catch (err) {
      console.error('Error fetching users:', err)
      setError('Failed to fetch users: ' + err.message)
    } finally {
      setUsersLoading(false)
    }
  }

  const deleteUser = async (userId) => {
    if (!window.confirm('Are you sure you want to delete this user? This action cannot be undone.')) {
      return
    }
    
    try {
      setLoading(true)
      console.log('Attempting to delete user with ID:', userId)
      console.log('Current token:', localStorage.getItem('token'))
      
      const response = await apiService.deleteUser(userId)
      console.log('Delete user response:', response)
      
      if (response.status === 'SUCCESS') {
        alert('User deleted successfully')
        fetchAllUsers() // Refresh the list
      } else {
        console.error('Delete failed with response:', response)
        alert('Failed to delete user: ' + response.message)
      }
    } catch (err) {
      console.error('Error deleting user:', err)
      alert('Failed to delete user: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  // Driver Management Functions
  const fetchAllDrivers = async () => {
    try {
      setDriversLoading(true)
      setError('')
      
      // Increased loading time for partial loader
      await new Promise(resolve => setTimeout(resolve, 1500))
      
      const response = await apiService.getAllDriversWithRatings() // Use the new API with ratings
      console.log('Drivers with Ratings API Response:', response)
      
      if (response.status === 'SUCCESS') {
        console.log('Full drivers API response:', response.data)
        // Check each driver's verification status in detail
        response.data?.forEach((driver, index) => {
          console.log(`Driver ${index + 1} (ID: ${driver.id}):`, {
            verified: driver.verified,
            verifiedType: typeof driver.verified,
            verifiedValue: driver.verified === null ? 'null' : driver.verified === undefined ? 'undefined' : driver.verified,
            licenseNumber: driver.licenseNumber,
            userName: `${driver.user?.firstName} ${driver.user?.lastName}`,
            allFields: Object.keys(driver)
          })
        })
        setAllDrivers(response.data || [])
      } else {
        setError('Failed to fetch drivers: ' + response.message)
      }
    } catch (err) {
      console.error('Error fetching drivers:', err)
      setError('Failed to fetch drivers: ' + err.message)
    } finally {
      setDriversLoading(false)
    }
  }

  const verifyDriver = async (driverDetailId, isVerify = true) => {
    try {
      setDriversLoading(true)
      console.log(`${isVerify ? 'Verifying' : 'Rejecting'} driver with ID:`, driverDetailId)
      
      let response
      
      if (isVerify) {
        response = await apiService.adminVerifyDriver(driverDetailId)
      } else {
        response = await apiService.adminRejectDriver(driverDetailId)
      }
      
      console.log('Verification response:', response)
      
      if (response.status === 'SUCCESS') {
        alert(`Driver ${isVerify ? 'verified' : 'rejected'} successfully`)
        fetchAllDrivers() // Refresh the list
      } else {
        console.error('Verification failed:', response)
        alert(`Failed to ${isVerify ? 'verify' : 'reject'} driver: ` + response.message)
      }
    } catch (err) {
      console.error(`Error ${isVerify ? 'verifying' : 'rejecting'} driver:`, err)
      alert(`Failed to ${isVerify ? 'verify' : 'reject'} driver: ` + err.message)
    } finally {
      setDriversLoading(false)
    }
  }

  // Helper function to get verification status
  const getVerificationStatus = (driver) => {
    // The backend uses 'isVerified' field (Boolean)
    const driverId = driver.userId || driver.driverDetailId || driver.id
    console.log('Checking verification for driver:', driverId, {
      isVerified: driver.isVerified,
      isVerifiedType: typeof driver.isVerified,
      verified: driver.verified,
      verifiedType: typeof driver.verified
    })
    
    let verificationValue = null
    
    // Primary field is 'isVerified' from DriverDetail entity
    if (driver.isVerified !== undefined) {
      verificationValue = driver.isVerified
    } else if (driver.verified !== undefined) {
      verificationValue = driver.verified  // fallback
    }
    
    // Convert boolean to number for consistency
    if (verificationValue === true) {
      verificationValue = 1      // Verified
    } else if (verificationValue === false) {
      verificationValue = 0      // Rejected
    }
    // null/undefined stays as null for Pending
    
    console.log('Final verification value for driver', driver.id, ':', verificationValue, typeof verificationValue)
    return verificationValue
  }

  // Update stats based on fetched data
  const updateStats = (users = allUsers, drivers = allDrivers) => {
    const pendingDrivers = drivers.filter(driver => {
      const status = getVerificationStatus(driver)
      return status !== 1 && status !== 0 // null, undefined, or any other value means pending
    })
    const verifiedDrivers = drivers.filter(driver => getVerificationStatus(driver) === 1)
    
    setStats({
      totalUsers: users.length,
      totalDrivers: drivers.length,
      pendingVerifications: pendingDrivers.length,
      verifiedDrivers: verifiedDrivers.length
    })
  }

  // Filter functions
  const getFilteredUsers = () => {
    return allUsers.filter(user => {
      const matchesSearch = user.firstName?.toLowerCase().includes(userSearch.toLowerCase()) ||
                           user.lastName?.toLowerCase().includes(userSearch.toLowerCase()) ||
                           user.email?.toLowerCase().includes(userSearch.toLowerCase()) ||
                           user.phoneNumber?.includes(userSearch)
      
      const matchesType = userTypeFilter === 'all' || 
                         user.role?.toLowerCase() === userTypeFilter.toLowerCase()
      
      return matchesSearch && matchesType
    })
  }

  const getFilteredDrivers = () => {
    return allDrivers.filter(driver => {
      const status = getVerificationStatus(driver)
      const matchesStatus = driverStatusFilter === 'all' ||
                           (driverStatusFilter === 'pending' && status !== 1 && status !== 0) || // null/undefined = pending
                           (driverStatusFilter === 'verified' && status === 1) ||
                           (driverStatusFilter === 'rejected' && status === 0)
      
      return matchesStatus
    })
  }

  // Pagination helper functions
  const getPaginatedUsers = () => {
    const filteredUsers = getFilteredUsers()
    const startIndex = (userCurrentPage - 1) * itemsPerPage
    const endIndex = startIndex + itemsPerPage
    return filteredUsers.slice(startIndex, endIndex)
  }

  const getPaginatedDrivers = () => {
    const filteredDrivers = getFilteredDrivers()
    const startIndex = (driverCurrentPage - 1) * itemsPerPage
    const endIndex = startIndex + itemsPerPage
    return filteredDrivers.slice(startIndex, endIndex)
  }

  const getUserTotalPages = () => {
    return Math.ceil(getFilteredUsers().length / itemsPerPage)
  }

  const getDriverTotalPages = () => {
    return Math.ceil(getFilteredDrivers().length / itemsPerPage)
  }

  // Reset pagination when filters change
  useEffect(() => {
    setUserCurrentPage(1)
  }, [userSearch, userTypeFilter])

  useEffect(() => {
    setDriverCurrentPage(1)
  }, [driverStatusFilter])

  const renderOverviewTab = () => (
    <div className="space-y-6">
      {overviewLoading ? (
        <div className="relative min-h-[400px]">
          {/* Tab Loader Overlay for Overview Section */}
          <div className="absolute inset-0 bg-white bg-opacity-95 flex flex-col items-center justify-center z-10 rounded-lg">
            <Loader 
              size={250}
              showText={true}
              text="Loading overview data..."
              className="mb-4"
            />
            <div className="text-center">
              <h4 className="text-lg font-medium text-gray-900 mb-2">
                Preparing Dashboard Overview
              </h4>
              <p className="text-sm text-gray-600">
                Gathering system statistics and metrics...
              </p>
            </div>
          </div>
        </div>
      ) : (
        <>
          {/* Stats Cards */}
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Users className="h-6 w-6 text-blue-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total Users</p>
              <p className="text-2xl font-semibold text-gray-900">{stats.totalUsers}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center">
            <div className="p-2 bg-green-100 rounded-lg">
              <Car className="h-6 w-6 text-green-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Total Drivers</p>
              <p className="text-2xl font-semibold text-gray-900">{stats.totalDrivers}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center">
            <div className="p-2 bg-yellow-100 rounded-lg">
              <AlertTriangle className="h-6 w-6 text-yellow-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Pending Verifications</p>
              <p className="text-2xl font-semibold text-gray-900">{stats.pendingVerifications}</p>
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm p-6">
          <div className="flex items-center">
            <div className="p-2 bg-purple-100 rounded-lg">
              <Check className="h-6 w-6 text-purple-600" />
            </div>
            <div className="ml-4">
              <p className="text-sm font-medium text-gray-600">Verified Drivers</p>
              <p className="text-2xl font-semibold text-gray-900">{stats.verifiedDrivers}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        <div className="bg-white rounded-lg shadow-sm">
          <div className="px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900">Quick Actions</h3>
          </div>
          <div className="p-6">
            <div className="space-y-3">
              <button
                onClick={() => setActiveTab('users')}
                className="w-full text-left px-4 py-3 bg-blue-50 border border-blue-200 rounded-lg hover:bg-blue-100 transition-colors"
              >
                <div className="flex items-center space-x-3">
                  <Users className="h-5 w-5 text-blue-600" />
                  <span className="font-medium text-blue-800">Manage Users ({stats.totalUsers})</span>
                </div>
              </button>
              <button
                onClick={() => setActiveTab('drivers')}
                className="w-full text-left px-4 py-3 bg-green-50 border border-green-200 rounded-lg hover:bg-green-100 transition-colors"
              >
                <div className="flex items-center space-x-3">
                  <Car className="h-5 w-5 text-green-600" />
                  <span className="font-medium text-green-800">Manage Drivers ({stats.totalDrivers})</span>
                </div>
              </button>
              {stats.pendingVerifications > 0 && (
                <button
                  onClick={() => {
                    handleTabSwitch('drivers')
                    setDriverStatusFilter('pending')
                  }}
                  className="w-full text-left px-4 py-3 bg-yellow-50 border border-yellow-200 rounded-lg hover:bg-yellow-100 transition-colors"
                >
                  <div className="flex items-center space-x-3">
                    <AlertTriangle className="h-5 w-5 text-yellow-600" />
                    <span className="font-medium text-yellow-800">Review Pending ({stats.pendingVerifications})</span>
                  </div>
                </button>
              )}
            </div>
          </div>
        </div>

        <div className="bg-white rounded-lg shadow-sm">
          <div className="px-6 py-4 border-b border-gray-200">
            <h3 className="text-lg font-semibold text-gray-900">Recent Activity</h3>
          </div>
          <div className="p-6">
            <div className="space-y-4">
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 bg-green-500 rounded-full"></div>
                <p className="text-sm text-gray-600">{stats.verifiedDrivers} drivers verified</p>
              </div>
              <div className="flex items-center space-x-3">
                <div className="w-2 h-2 bg-blue-500 rounded-full"></div>
                <p className="text-sm text-gray-600">{stats.totalUsers} total users registered</p>
              </div>
              {stats.pendingVerifications > 0 && (
                <div className="flex items-center space-x-3">
                  <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                  <p className="text-sm text-gray-600">{stats.pendingVerifications} drivers pending verification</p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
        </>
      )}
    </div>
  )

  const renderUsersTab = () => (
    <div className="bg-white rounded-lg shadow-sm">
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex justify-between items-center">
          <h3 className="text-lg font-semibold text-gray-900">User Management</h3>
          <button
            onClick={fetchAllUsers}
            disabled={usersLoading}
            className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 text-sm font-medium flex items-center space-x-2"
          >
            <RefreshCw className={`h-4 w-4 ${usersLoading ? 'animate-spin' : ''}`} />
            <span>{usersLoading ? 'Loading...' : 'Refresh'}</span>
          </button>
        </div>
        
        {/* Search and Filter */}
        <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          <div className="relative">
            <Search className="h-4 w-4 absolute left-3 top-3 text-gray-400" />
            <input
              type="text"
              placeholder="Search users..."
              value={userSearch}
              onChange={(e) => setUserSearch(e.target.value)}
              className="pl-10 pr-4 py-2 border border-gray-300 rounded-lg w-full focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>
          <select
            value={userTypeFilter}
            onChange={(e) => setUserTypeFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="all">All Types</option>
            <option value="user">Passengers</option>
            <option value="driver">Drivers</option>
            <option value="admin">Admins</option>
          </select>
          <div className="text-sm text-gray-600 flex items-center">
            Total: {getFilteredUsers().length} users
            {getUserTotalPages() > 1 && (
              <span className="ml-2 text-blue-600">
                (Page {userCurrentPage} of {getUserTotalPages()})
              </span>
            )}
          </div>
        </div>
      </div>
      
      <div className="p-6">
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-700">{error}</p>
          </div>
        )}
        
        {usersLoading ? (
          <div className="relative min-h-[400px]">
            {/* Tab Loader Overlay for Users Section */}
            <div className="absolute inset-0 bg-white bg-opacity-95 flex flex-col items-center justify-center z-10 rounded-lg">
              <Loader 
                size={250}
                showText={true}
                text="Loading user data..."
                className="mb-4"
              />
              <div className="text-center">
                <h4 className="text-lg font-medium text-gray-900 mb-2">
                  Fetching User Information
                </h4>
                <p className="text-sm text-gray-600">
                  Please wait while we load all user accounts...
                </p>
              </div>
            </div>
          </div>
        ) : getPaginatedUsers().length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <Users className="h-12 w-12 mx-auto mb-4 text-gray-300" />
            <p>No users found</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-gray-50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">ID</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Name</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Email</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Phone</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Role</th>
                  <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 uppercase">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-200">
                {getPaginatedUsers().map((user) => (
                  <tr key={user.id} className="hover:bg-gray-50">
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">#{user.id}</td>
                    <td className="px-4 py-3 text-sm font-medium text-gray-900">
                      {user.firstName} {user.lastName}
                    </td>
                    <td className="px-4 py-3 text-sm text-gray-600">{user.email}</td>
                    <td className="px-4 py-3 text-sm text-gray-600">{user.phoneNumber}</td>
                    <td className="px-4 py-3 text-sm">
                      <span className={`px-2 py-1 text-xs rounded-full ${
                        user.role === 'DRIVER' 
                          ? 'bg-green-100 text-green-800'
                          : user.role === 'ADMIN'
                          ? 'bg-purple-100 text-purple-800' 
                          : 'bg-blue-100 text-blue-800'
                      }`}>
                        {user.role}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-sm">
                      <div className="flex space-x-2">
                        <button
                          onClick={() => {
                            console.log('Delete button clicked for user:', user)
                            deleteUser(user.id)
                          }}
                          className="text-red-600 hover:text-red-800"
                          title="Delete User"
                          disabled={loading}
                        >
                          <Trash2 className="h-4 w-4" />
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        
        {/* Users Pagination */}
        {getPaginatedUsers().length > 0 && (
          <PaginationComponent
            currentPage={userCurrentPage}
            totalPages={getUserTotalPages()}
            onPageChange={setUserCurrentPage}
            totalItems={getFilteredUsers().length}
            itemsPerPage={itemsPerPage}
          />
        )}
      </div>
    </div>
  )

  const renderDriversTab = () => (
    <div className="bg-white rounded-lg shadow-sm">
      <div className="px-6 py-4 border-b border-gray-200">
        <div className="flex justify-between items-center">
          <h3 className="text-lg font-semibold text-gray-900">Driver Management</h3>
          <button
            onClick={fetchAllDrivers}
            disabled={driversLoading}
            className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 disabled:opacity-50 text-sm font-medium flex items-center space-x-2"
          >
            <RefreshCw className={`h-4 w-4 ${driversLoading ? 'animate-spin' : ''}`} />
            <span>{driversLoading ? 'Loading...' : 'Refresh'}</span>
          </button>
        </div>
        
        {/* Filter */}
        <div className="mt-4 grid grid-cols-1 md:grid-cols-3 gap-4">
          <select
            value={driverStatusFilter}
            onChange={(e) => setDriverStatusFilter(e.target.value)}
            className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
          >
            <option value="all">All Drivers</option>
            <option value="pending">Pending Verification</option>
            <option value="verified">Verified</option>
            <option value="rejected">Rejected</option>
          </select>
          <div className="text-sm text-gray-600 flex items-center">
            Total: {getFilteredDrivers().length} drivers
            {getDriverTotalPages() > 1 && (
              <span className="ml-2 text-blue-600">
                (Page {driverCurrentPage} of {getDriverTotalPages()})
              </span>
            )}
          </div>
          <div className="text-sm text-gray-600 flex items-center">
            Pending: {allDrivers.filter(d => {
              const status = getVerificationStatus(d)
              return status !== 1 && status !== 0 // null/undefined = pending
            }).length}
          </div>
        </div>
      </div>
      
      <div className="p-6">
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-4">
            <p className="text-red-700">{error}</p>
          </div>
        )}
        
        {driversLoading ? (
          <div className="relative min-h-[400px]">
            {/* Tab Loader Overlay for Drivers Section */}
            <div className="absolute inset-0 bg-white bg-opacity-95 flex flex-col items-center justify-center z-10 rounded-lg">
              <Loader 
                size={250}
                showText={true}
                text="Loading driver details..."
                className="mb-4"
              />
              <div className="text-center">
                <h4 className="text-lg font-medium text-gray-900 mb-2">
                  Fetching Driver Information
                </h4>
                <p className="text-sm text-gray-600">
                  Please wait while we load all driver details and ratings...
                </p>
              </div>
            </div>
          </div>
        ) : getPaginatedDrivers().length === 0 ? (
          <div className="text-center py-8 text-gray-500">
            <Car className="h-12 w-12 mx-auto mb-4 text-gray-300" />
            <p>No drivers found</p>
          </div>
        ) : (
          <div className="space-y-4">
            {getPaginatedDrivers().map((driver) => (
              <div key={driver.id || driver.driverDetailId} className="border border-gray-200 rounded-lg p-6">
                <div className="flex justify-between items-start">
                  <div className="grid grid-cols-1 md:grid-cols-4 gap-6 flex-1">
                    {/* Driver Info */}
                    <div>
                      <h4 className="font-medium text-gray-900 mb-2">Driver Information</h4>
                      <div className="space-y-2 text-sm">
                        <div className="flex items-center space-x-2">
                          <User className="h-4 w-4 text-gray-400" />
                          <span>{driver.firstName || driver.user?.firstName} {driver.lastName || driver.user?.lastName}</span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <Phone className="h-4 w-4 text-gray-400" />
                          <span>{driver.phoneNumber || driver.user?.phoneNumber}</span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <Mail className="h-4 w-4 text-gray-400" />
                          <span>{driver.email || driver.user?.email}</span>
                        </div>
                      </div>
                    </div>

                    {/* License Info */}
                    <div>
                      <h4 className="font-medium text-gray-900 mb-2">License Details</h4>
                      <div className="space-y-1 text-sm text-gray-600">
                        <p><strong>License:</strong> {driver.licenseNumber}</p>
                        <p><strong>Expiry:</strong> {driver.licenseExpiry || 'N/A'}</p>
                        <p><strong>Insurance:</strong> {driver.insuranceNumber || 'N/A'}</p>
                      </div>
                    </div>

                    {/* Vehicle Info */}
                    <div>
                      <h4 className="font-medium text-gray-900 mb-2">Vehicle Details</h4>
                      <div className="space-y-1 text-sm text-gray-600">
                        <p><strong>Model:</strong> {driver.vehicleModel || driver.carModel}</p>
                        <p><strong>Number:</strong> {driver.vehiclePlateNumber || driver.carNumber}</p>
                        <p><strong>Year:</strong> {driver.vehicleYear || driver.carYear}</p>
                        <p><strong>Color:</strong> {driver.vehicleColor || driver.carColor}</p>
                      </div>
                    </div>

                    {/* Rating Info */}
                    <div>
                      <h4 className="font-medium text-gray-900 mb-2">Driver Rating</h4>
                      <div className="space-y-2">
                        <StarRating 
                          rating={driver.averageRating || 0} 
                          totalReviews={driver.totalRatings || 0}
                          size="sm"
                        />
                        {driver.totalRatings > 0 && (
                          <div className="text-xs text-gray-500 space-y-1">
                            <div className="flex justify-between">
                              <span>5★:</span>
                              <span>{driver.fiveStarCount || 0}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>4★:</span>
                              <span>{driver.fourStarCount || 0}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>3★:</span>
                              <span>{driver.threeStarCount || 0}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>2★:</span>
                              <span>{driver.twoStarCount || 0}</span>
                            </div>
                            <div className="flex justify-between">
                              <span>1★:</span>
                              <span>{driver.oneStarCount || 0}</span>
                            </div>
                          </div>
                        )}
                      </div>
                    </div>
                  </div>

                  {/* Status and Actions */}
                  <div className="ml-6 text-right">
                    <div className="mb-4">
                      <span className={`px-3 py-1 text-sm rounded-full ${
                        getVerificationStatus(driver) === 1
                          ? 'bg-green-100 text-green-800'
                          : getVerificationStatus(driver) === 0
                          ? 'bg-red-100 text-red-800'
                          : 'bg-yellow-100 text-yellow-800'
                      }`}>
                        {getVerificationStatus(driver) === 1 ? 'Verified' : getVerificationStatus(driver) === 0 ? 'Rejected' : 'Pending'}
                      </span>
                    </div>

                    {/* Action Buttons */}
                    {getVerificationStatus(driver) !== 1 && (
                      <div className="space-y-2">
                        <button
                          onClick={() => verifyDriver(driver.driverDetailId || driver.id, true)}
                          disabled={driversLoading}
                          className="w-full px-4 py-2 bg-green-500 text-white rounded-lg hover:bg-green-600 disabled:opacity-50 text-sm font-medium flex items-center justify-center space-x-2"
                        >
                          <Check className="h-4 w-4" />
                          <span>Verify</span>
                        </button>
                        <button
                          onClick={() => verifyDriver(driver.driverDetailId || driver.id, false)}
                          disabled={driversLoading}
                          className="w-full px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 disabled:opacity-50 text-sm font-medium flex items-center justify-center space-x-2"
                        >
                          <X className="h-4 w-4" />
                          <span>Reject</span>
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
        
        {/* Drivers Pagination */}
        {getPaginatedDrivers().length > 0 && (
          <PaginationComponent
            currentPage={driverCurrentPage}
            totalPages={getDriverTotalPages()}
            onPageChange={setDriverCurrentPage}
            totalItems={getFilteredDrivers().length}
            itemsPerPage={itemsPerPage}
          />
        )}
      </div>
    </div>
  )

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <div className="bg-white shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between items-center py-6">
            <div>
              <h1 className="text-2xl font-bold text-gray-900">Admin Dashboard</h1>
              <p className="text-sm text-gray-500">Welcome back, {user?.firstName || 'Admin'}</p>
            </div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div className="bg-white border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <nav className="flex space-x-8">
            <button
              onClick={() => handleTabSwitch('overview')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'overview'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center space-x-2">
                <BarChart3 className="h-4 w-4" />
                <span>Overview</span>
              </div>
            </button>

            <button
              onClick={() => handleTabSwitch('users')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'users'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center space-x-2">
                <Users className="h-4 w-4" />
                <span>All Users ({stats.totalUsers})</span>
              </div>
            </button>

            <button
              onClick={() => handleTabSwitch('drivers')}
              className={`py-4 px-1 border-b-2 font-medium text-sm ${
                activeTab === 'drivers'
                  ? 'border-blue-500 text-blue-600'
                  : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
              }`}
            >
              <div className="flex items-center space-x-2">
                <Car className="h-4 w-4" />
                <span>All Drivers ({stats.totalDrivers})</span>
                {stats.pendingVerifications > 0 && (
                  <span className="bg-red-500 text-white text-xs rounded-full px-2 py-1 ml-1">
                    {stats.pendingVerifications}
                  </span>
                )}
              </div>
            </button>
          </nav>
        </div>
      </div>

      {/* Initial Dashboard Loading Overlay */}
      {isDashboardLoading && (
        <Loader 
          overlay={true} 
          text="Initializing Admin Dashboard..." 
          size={250}
          className="bg-white bg-opacity-98 z-50"
        />
      )}

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeTab === 'overview' && renderOverviewTab()}
        {activeTab === 'users' && renderUsersTab()}
        {activeTab === 'drivers' && renderDriversTab()}
      </div>
    </div>
  )
}

// Pagination Component
const PaginationComponent = ({ currentPage, totalPages, onPageChange, totalItems, itemsPerPage }) => {
  const startItem = (currentPage - 1) * itemsPerPage + 1
  const endItem = Math.min(currentPage * itemsPerPage, totalItems)

  const getPageNumbers = () => {
    const pages = []
    const showPages = 5
    let start = Math.max(1, currentPage - Math.floor(showPages / 2))
    let end = Math.min(totalPages, start + showPages - 1)

    if (end - start + 1 < showPages) {
      start = Math.max(1, end - showPages + 1)
    }

    for (let i = start; i <= end; i++) {
      pages.push(i)
    }
    return pages
  }

  if (totalPages <= 1) return null

  return (
    <div className="flex items-center justify-between px-6 py-3 bg-white border-t border-gray-200">
      <div className="flex-1 flex justify-between sm:hidden">
        <button
          onClick={() => onPageChange(currentPage - 1)}
          disabled={currentPage === 1}
          className="relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Previous
        </button>
        <button
          onClick={() => onPageChange(currentPage + 1)}
          disabled={currentPage === totalPages}
          className="ml-3 relative inline-flex items-center px-4 py-2 border border-gray-300 text-sm font-medium rounded-md text-gray-700 bg-white hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
        >
          Next
        </button>
      </div>
      <div className="hidden sm:flex-1 sm:flex sm:items-center sm:justify-between">
        <div>
          <p className="text-sm text-gray-700">
            Showing <span className="font-medium">{startItem}</span> to{' '}
            <span className="font-medium">{endItem}</span> of{' '}
            <span className="font-medium">{totalItems}</span> results
          </p>
        </div>
        <div>
          <nav className="relative z-0 inline-flex rounded-md shadow-sm -space-x-px" aria-label="Pagination">
            <button
              onClick={() => onPageChange(currentPage - 1)}
              disabled={currentPage === 1}
              className="relative inline-flex items-center px-2 py-2 rounded-l-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Previous
            </button>
            {getPageNumbers().map((page) => (
              <button
                key={page}
                onClick={() => onPageChange(page)}
                className={`relative inline-flex items-center px-4 py-2 border text-sm font-medium ${
                  page === currentPage
                    ? 'z-10 bg-blue-50 border-blue-500 text-blue-600'
                    : 'bg-white border-gray-300 text-gray-500 hover:bg-gray-50'
                }`}
              >
                {page}
              </button>
            ))}
            <button
              onClick={() => onPageChange(currentPage + 1)}
              disabled={currentPage === totalPages}
              className="relative inline-flex items-center px-2 py-2 rounded-r-md border border-gray-300 bg-white text-sm font-medium text-gray-500 hover:bg-gray-50 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Next
            </button>
          </nav>
        </div>
      </div>
    </div>
  )
}

export default AdminDashboard