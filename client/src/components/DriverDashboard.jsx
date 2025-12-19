import { useState, useEffect } from 'react'
import { Car, Plus, MapPin, Clock, Users, IndianRupee, Calendar, Eye } from 'lucide-react'
import apiService from '../services/api'
import DriverWallet from './DriverWallet'
import Loader from './Loader'

const DriverDashboard = ({ user }) => {
  const [activeTab, setActiveTab] = useState('overview')
  const [isDashboardLoading, setIsDashboardLoading] = useState(true)
  const [rides, setRides] = useState([])
  const [bookings, setBookings] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [postRideForm, setPostRideForm] = useState({
    from: '',
    to: '',
    date: '',
    time: '',
    seats: '',
    price: '',
    notes: ''
  })
  const [fareCalculation, setFareCalculation] = useState(null)
  const [calculatingFare, setCalculatingFare] = useState(false)

  useEffect(() => {
    const initializeDashboard = async () => {
      // Simulate initial data loading
      await new Promise(resolve => setTimeout(resolve, 2500))
      setIsDashboardLoading(false)
      
      // Load data after initial loading
      fetchData()
    }
    
    initializeDashboard()
  }, [])

  // Handle tab switching - no more full page loading
  const handleTabSwitch = (newTab) => {
    if (newTab === activeTab) return // Don't switch if already on the same tab
    setActiveTab(newTab)
  }

  const fetchData = async () => {
    setLoading(true)
    try {
      const [ridesResponse, bookingsResponse] = await Promise.all([
        apiService.getMyRides(),
        apiService.getDriverBookings()
      ])
      
      if (ridesResponse.status === 'SUCCESS') {
        setRides(ridesResponse.data || [])
      }
      
      if (bookingsResponse.status === 'SUCCESS') {
        setBookings(bookingsResponse.data || [])
      }
    } catch (err) {
      setError('Failed to load data: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  const handlePostRideSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError('')

    // Frontend validation
    try {
      // Validate required fields
      if (!postRideForm.from.trim()) {
        throw new Error('Please enter departure location')
      }
      if (!postRideForm.to.trim()) {
        throw new Error('Please enter destination location')
      }
      if (!postRideForm.date) {
        throw new Error('Please select departure date')
      }
      if (!postRideForm.time) {
        throw new Error('Please select departure time')
      }
      if (!postRideForm.seats || parseInt(postRideForm.seats) < 1) {
        throw new Error('Please select number of available seats')
      }
      if (!postRideForm.price || parseFloat(postRideForm.price) < 1) {
        throw new Error('Please enter a valid price per seat')
      }

      // Validate date is not in the past
      const selectedDateTime = new Date(`${postRideForm.date}T${postRideForm.time}`)
      const now = new Date()
      if (selectedDateTime <= now) {
        throw new Error('Departure date and time must be in the future')
      }

      // Validate price range
      const price = parseFloat(postRideForm.price)
      if (price < 10) {
        throw new Error('Price per seat must be at least ₹10')
      }
      if (price > 10000) {
        throw new Error('Price per seat cannot exceed ₹10,000')
      }

      // Validate seat count
      const seats = parseInt(postRideForm.seats)
      if (seats < 1 || seats > 8) {
        throw new Error('Available seats must be between 1 and 8')
      }

      const response = await apiService.postRide(postRideForm)
      
      if (response.status === 'SUCCESS') {
        alert('Ride posted successfully!')
        setPostRideForm({
          from: '',
          to: '',
          date: '',
          time: '',
          seats: '',
          price: '',
          notes: ''
        })
        setFareCalculation(null) // Reset fare calculation
        fetchData() // Refresh rides list
        await handleTabSwitch('my-rides') // Switch to My Rides tab to see the posted ride
      } else {
        setError(response.message || 'Failed to post ride')
      }
    } catch (err) {
      console.error('Error posting ride:', err)
      setError(err.message || 'Failed to post ride')
    } finally {
      setLoading(false)
    }
  }

  const handlePostRideChange = (e) => {
    setPostRideForm({
      ...postRideForm,
      [e.target.name]: e.target.value
    })
  }

  // Function to calculate fare based on distance
  const calculateFare = async () => {
    if (!postRideForm.from || !postRideForm.to) {
      setError('Please enter both source and destination to calculate fare')
      return
    }

    setCalculatingFare(true)
    setError('')
    
    try {
      const response = await apiService.calculateFare(postRideForm.from, postRideForm.to)
      
      if (response.status === 'SUCCESS') {
        setFareCalculation(response.data)
        // Auto-fill the calculated fare in the price field
        setPostRideForm({
          ...postRideForm,
          price: response.data.calculatedFare.toString()
        })
      } else {
        setError('Failed to calculate fare: ' + response.message)
      }
    } catch (err) {
      setError('Error calculating fare: ' + err.message)
    } finally {
      setCalculatingFare(false)
    }
  }

  // Ride management functions
  const handleRideStatusChange = async (rideId, action) => {
    try {
      setLoading(true)
      let response
      
      switch (action) {
        case 'cancel':
          response = await apiService.cancelRide(rideId)
          break
        case 'complete':
          response = await apiService.completeRide(rideId)
          break
        case 'activate':
          response = await apiService.activateRide(rideId)
          break
        default:
          throw new Error('Invalid action')
      }

      if (response.status === 'SUCCESS') {
        alert(`Ride ${action}d successfully!`)
        fetchData() // Refresh data
      } else {
        alert(response.message || `Failed to ${action} ride`)
      }
    } catch (err) {
      alert(err.message || `Failed to ${action} ride`)
    } finally {
      setLoading(false)
    }
  }

  // Booking management functions
  const handleBookingAction = async (rideId, bookingId, action) => {
    try {
      setLoading(true)
      let response
      
      if (action === 'confirm') {
        response = await apiService.confirmBooking(rideId, bookingId)
      } else if (action === 'cancel') {
        response = await apiService.cancelBookingByDriver(rideId, bookingId)
      }

      if (response.status === 'SUCCESS') {
        alert(`Booking ${action}ed successfully!`)
        fetchData() // Refresh data
      } else {
        alert(response.message || `Failed to ${action} booking`)
      }
    } catch (err) {
      alert(err.message || `Failed to ${action} booking`)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Initial Dashboard Loading */}
      {isDashboardLoading && (
        <div className="fixed inset-0 bg-white bg-opacity-98 flex flex-col items-center justify-center z-50">
          <Loader 
            size={250}
            showText={true}
            text="Loading your driver dashboard..."
            className="mb-8"
          />
          <div className="text-center max-w-lg mx-auto px-4">
            <h3 className="text-2xl font-bold text-gray-900 mb-4">
              Ready to Hit the Road! 🚗💨
            </h3>
            <p className="text-lg text-gray-600 mb-2">
              Setting up your driving experience...
            </p>
            <div className="flex justify-center items-center space-x-2 mt-6">
              <div className="w-3 h-3 bg-yellow-500 rounded-full animate-ping"></div>
              <div className="w-3 h-3 bg-yellow-500 rounded-full animate-ping" style={{ animationDelay: '0.2s' }}></div>
              <div className="w-3 h-3 bg-yellow-500 rounded-full animate-ping" style={{ animationDelay: '0.4s' }}></div>
            </div>
          </div>
        </div>
      )}
      
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="mb-8">
          <h1 className="text-3xl font-bold text-gray-900">Driver Dashboard</h1>
          <p className="text-gray-600 mt-2">Welcome back, {user.name}!</p>
        </div>

        {/* Stats Cards */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-8">
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center">
              <Car className="h-8 w-8 text-yellow-500" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500">Total Rides</p>
                <p className="text-2xl font-bold text-gray-900">{rides.length}</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center">
              <Users className="h-8 w-8 text-green-500" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500">Passengers</p>
                <p className="text-2xl font-bold text-gray-900">{bookings.length}</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center">
              <IndianRupee className="h-8 w-8 text-blue-500" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500">Earnings</p>
                <p className="text-2xl font-bold text-gray-900">₹{rides.reduce((sum, ride) => sum + (ride.pricePerSeat * ride.bookedSeats || 0), 0)}</p>
              </div>
            </div>
          </div>
          <div className="bg-white rounded-lg shadow-sm p-6">
            <div className="flex items-center">
              <Calendar className="h-8 w-8 text-purple-500" />
              <div className="ml-4">
                <p className="text-sm font-medium text-gray-500">Active Rides</p>
                <p className="text-2xl font-bold text-gray-900">{rides.filter(ride => ride.status === 'ACTIVE').length}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Tab Navigation */}
        <div className="bg-white rounded-lg shadow-sm mb-8">
          <div className="border-b border-gray-200">
            <nav className="flex space-x-8 px-6">
              <button
                onClick={() => handleTabSwitch('overview')}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'overview'
                    ? 'border-yellow-500 text-yellow-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                Overview
              </button>
              <button
                onClick={() => handleTabSwitch('post-ride')}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'post-ride'
                    ? 'border-yellow-500 text-yellow-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                Post New Ride
              </button>
              <button
                onClick={() => handleTabSwitch('my-rides')}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'my-rides'
                    ? 'border-yellow-500 text-yellow-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                My Rides
              </button>
              <button
                onClick={() => handleTabSwitch('bookings')}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'bookings'
                    ? 'border-yellow-500 text-yellow-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                Bookings
              </button>
              <button
                onClick={() => handleTabSwitch('wallet')}
                className={`py-4 px-1 border-b-2 font-medium text-sm ${
                  activeTab === 'wallet'
                    ? 'border-yellow-500 text-yellow-600'
                    : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                }`}
              >
                💳 Wallet
              </button>
            </nav>
          </div>

          {/* Main Content */}
            {loading && (
              <Loader 
                overlay={true} 
                text="Loading..." 
                size={180}
                className="bg-white bg-opacity-95 rounded-lg"
              />
            )}

          <div className="p-6">
            {activeTab === 'overview' && (
              <div>
                <h3 className="text-lg font-medium text-gray-900 mb-4">Recent Activity</h3>
                <div className="space-y-4">
                  <div className="flex items-center justify-between p-4 bg-green-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="bg-green-100 p-2 rounded-full">
                        <Users className="h-5 w-5 text-green-600" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">New booking received</p>
                        <p className="text-sm text-gray-500">Mumbai → Pune ride on Jan 15</p>
                      </div>
                    </div>
                    <span className="text-sm text-gray-500">2 hours ago</span>
                  </div>
                  <div className="flex items-center justify-between p-4 bg-blue-50 rounded-lg">
                    <div className="flex items-center space-x-3">
                      <div className="bg-blue-100 p-2 rounded-full">
                        <IndianRupee className="h-5 w-5 text-blue-600" />
                      </div>
                      <div>
                        <p className="font-medium text-gray-900">Payment received</p>
                        <p className="text-sm text-gray-500">₹2,400 for Delhi → Jaipur ride</p>
                      </div>
                    </div>
                    <span className="text-sm text-gray-500">Yesterday</span>
                  </div>
                </div>
              </div>
            )}

            {activeTab === 'post-ride' && (
              <div>
                <h3 className="text-lg font-medium text-gray-900 mb-6">Post a New Ride</h3>
                {error && (
                  <div className="mb-4 bg-red-50 border border-red-200 rounded-lg p-4 text-red-700 text-sm">
                    {error}
                  </div>
                )}
                <form onSubmit={handlePostRideSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">From</label>
                    <div className="relative">
                      <MapPin className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                      <input
                        type="text"
                        name="from"
                        value={postRideForm.from}
                        onChange={handlePostRideChange}
                        required
                        className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                        placeholder="Departure city"
                      />
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">To</label>
                    <div className="relative">
                      <MapPin className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                      <input
                        type="text"
                        name="to"
                        value={postRideForm.to}
                        onChange={handlePostRideChange}
                        required
                        className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                        placeholder="Destination city"
                      />
                    </div>
                  </div>
                  
                  {/* Dynamic Fare Calculation Section */}
                  <div className="md:col-span-2">
                    <div className="bg-blue-50 border border-blue-200 rounded-lg p-4">
                      <div className="flex items-center justify-between mb-3">
                        <h4 className="text-sm font-medium text-blue-900">🚗 Dynamic Fare Calculator</h4>
                        <button
                          type="button"
                          onClick={calculateFare}
                          disabled={calculatingFare || !postRideForm.from || !postRideForm.to}
                          className="bg-blue-600 hover:bg-blue-700 disabled:bg-blue-300 text-white px-4 py-2 rounded-lg text-sm font-medium transition-colors"
                        >
                          {calculatingFare ? 'Calculating...' : 'Calculate Fare'}
                        </button>
                      </div>
                      
                      {fareCalculation && (
                        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 text-sm">
                          <div className="bg-white rounded-lg p-3 border border-blue-100">
                            <p className="text-blue-600 font-medium">Distance</p>
                            <p className="text-gray-900 font-semibold">{fareCalculation.distanceText}</p>
                          </div>
                          <div className="bg-white rounded-lg p-3 border border-blue-100">
                            <p className="text-blue-600 font-medium">Duration</p>
                            <p className="text-gray-900 font-semibold">{fareCalculation.durationText}</p>
                          </div>
                          <div className="bg-white rounded-lg p-3 border border-blue-100">
                            <p className="text-blue-600 font-medium">Calculated Fare</p>
                            <p className="text-green-600 font-bold text-lg">₹{fareCalculation.calculatedFare}</p>
                          </div>
                        </div>
                      )}
                      
                      {!fareCalculation && (
                        <p className="text-blue-700 text-sm">
                          💡 <strong>FREE Smart Pricing:</strong> Our system calculates fare based on actual distance.
                          Formula: Base Fare (₹50) + Distance × Rate per KM (₹3/km).
                          Max cost:₹5000 Only.
                        </p>
                      )}
                    </div>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Date</label>
                    <input
                      type="date"
                      name="date"
                      value={postRideForm.date}
                      onChange={handlePostRideChange}
                      required
                      className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Time</label>
                    <input
                      type="time"
                      name="time"
                      value={postRideForm.time}
                      onChange={handlePostRideChange}
                      required
                      className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">Available Seats</label>
                    <select 
                      name="seats"
                      value={postRideForm.seats}
                      onChange={handlePostRideChange}
                      required
                      className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                    >
                      <option value="">Select seats</option>
                      <option value="1">1</option>
                      <option value="2">2</option>
                      <option value="3">3</option>
                      <option value="4">4</option>
                      <option value="5">5</option>
                      <option value="6">6</option>
                      <option value="7">7</option>
                      <option value="8">8</option>
                    </select>
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Price per Seat 
                      {fareCalculation && <span className="text-green-600 text-xs ml-2">(Auto-calculated)</span>}
                    </label>
                    <div className="relative">
                      <IndianRupee className="absolute left-3 top-3 h-5 w-5 text-gray-400" />
                      <input
                        type="number"
                        name="price"
                        value={postRideForm.price}
                        onChange={handlePostRideChange}
                        required
                        min="0"
                        className="w-full pl-10 pr-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                        placeholder="0"
                      />
                    </div>
                    {fareCalculation && (
                      <p className="text-xs text-gray-500 mt-1">
                        Calculated based on {fareCalculation.distanceKm?.toFixed(1)} km distance
                      </p>
                    )}
                  </div>
                  <div className="md:col-span-2">
                    <label className="block text-sm font-medium text-gray-700 mb-2">Additional Notes</label>
                    <textarea
                      rows="3"
                      name="notes"
                      value={postRideForm.notes}
                      onChange={handlePostRideChange}
                      className="w-full px-3 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-yellow-500 focus:border-transparent"
                      placeholder="Any additional information for passengers..."
                    ></textarea>
                  </div>
                  <div className="md:col-span-2">
                    <button
                      type="submit"
                      disabled={loading}
                      className="w-full bg-yellow-500 hover:bg-yellow-600 disabled:bg-yellow-300 text-white py-3 px-4 rounded-lg font-semibold transition-colors flex items-center justify-center space-x-2"
                    >
                      <Plus className="h-5 w-5" />
                      <span>{loading ? 'Posting...' : 'Post Ride'}</span>
                    </button>
                  </div>
                </form>
              </div>
            )}

            {activeTab === 'my-rides' && (
              <div>
                <h3 className="text-lg font-medium text-gray-900 mb-6">My Posted Rides</h3>
                <div className="space-y-4">
                  {rides.map((ride) => (
                    <div key={ride.id} className="border border-gray-200 rounded-lg p-6">
                      <div className="flex items-center justify-between mb-4">
                        <div className="flex items-center space-x-4">
                          <h4 className="text-lg font-medium text-gray-900">
                            {ride.source} → {ride.destination}
                          </h4>
                          <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                            ride.status === 'ACTIVE' 
                              ? 'bg-blue-100 text-blue-800' 
                              : ride.status === 'COMPLETED'
                              ? 'bg-green-100 text-green-800'
                              : 'bg-gray-100 text-gray-800'
                          }`}>
                            {ride.status?.toLowerCase() || 'unknown'}
                          </span>
                        </div>
                        <button className="text-yellow-600 hover:text-yellow-700">
                          <Eye className="h-5 w-5" />
                        </button>
                      </div>
                      <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                        <div className="flex items-center space-x-2">
                          <Calendar className="h-4 w-4 text-gray-400" />
                          <span>{ride.departureDate}</span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <Clock className="h-4 w-4 text-gray-400" />
                          <span>{ride.departureTime}</span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <Users className="h-4 w-4 text-gray-400" />
                          <span>{ride.bookedSeats || 0}/{ride.availableSeats} seats</span>
                        </div>
                        <div className="flex items-center space-x-2">
                          <IndianRupee className="h-4 w-4 text-gray-400" />
                          <span>₹{ride.pricePerSeat}</span>
                        </div>
                      </div>
                      {ride.notes && (
                        <div className="mt-3 text-sm text-gray-600">
                          <strong>Notes:</strong> {ride.notes}
                        </div>
                      )}
                      
                      {/* Ride Management Buttons */}
                      <div className="mt-4 flex flex-wrap gap-2">
                        {ride.status === 'ACTIVE' && (
                          <>
                            <button
                              onClick={() => handleRideStatusChange(ride.id, 'complete')}
                              disabled={loading}
                              className="bg-green-500 hover:bg-green-600 text-white px-3 py-1 rounded text-sm disabled:opacity-50"
                            >
                              Mark Complete
                            </button>
                            <button
                              onClick={() => handleRideStatusChange(ride.id, 'cancel')}
                              disabled={loading}
                              className="bg-red-500 hover:bg-red-600 text-white px-3 py-1 rounded text-sm disabled:opacity-50"
                            >
                              Cancel Ride
                            </button>
                          </>
                        )}
                        {ride.status === 'CANCELLED' && (
                          <button
                            onClick={() => handleRideStatusChange(ride.id, 'activate')}
                            disabled={loading}
                            className="bg-blue-500 hover:bg-blue-600 text-white px-3 py-1 rounded text-sm disabled:opacity-50"
                          >
                            Reactivate
                          </button>
                        )}
                        <button
                          onClick={() => window.open(`#/ride/${ride.id}/bookings`, '_blank')}
                          className="bg-yellow-500 hover:bg-yellow-600 text-white px-3 py-1 rounded text-sm"
                        >
                          View Bookings ({ride.bookedSeats || 0})
                        </button>
                      </div>
                    </div>
                  ))}
                  {rides.length === 0 && (
                    <div className="text-center py-8 text-gray-500">
                      No rides posted yet. Create your first ride to get started!
                    </div>
                  )}
                </div>
              </div>
            )}

            {activeTab === 'bookings' && (
              <div>
                <h3 className="text-lg font-medium text-gray-900 mb-6">Booking Requests</h3>
                {bookings.length === 0 ? (
                  <div className="bg-gray-50 rounded-lg p-8 text-center">
                    <Users className="h-12 w-12 text-gray-400 mx-auto mb-4" />
                    <p className="text-gray-500">No booking requests at the moment</p>
                    <p className="text-sm text-gray-400 mt-2">When passengers book your rides, they'll appear here</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {bookings.map((booking) => (
                      <div key={booking.id} className="border border-gray-200 rounded-lg p-6">
                        <div className="flex items-center justify-between mb-4">
                          <div>
                            <h5 className="font-semibold text-gray-900">
                              {booking.passengerName} - {booking.passengerPhone}
                            </h5>
                            <p className="text-sm text-gray-600">
                              {booking.source} → {booking.destination}
                            </p>
                            <p className="text-xs text-gray-500">
                              Pickup: {booking.pickupPoint}
                            </p>
                          </div>
                          <div className="text-right">
                            <span className={`px-3 py-1 rounded-full text-sm font-medium ${
                              booking.status === 'CONFIRMED' ? 'bg-green-100 text-green-800' :
                              booking.status === 'PENDING' ? 'bg-yellow-100 text-yellow-800' :
                              booking.status === 'CANCELLED' ? 'bg-red-100 text-red-800' :
                              'bg-gray-100 text-gray-800'
                            }`}>
                              {booking.status}
                            </span>
                            <p className="text-lg font-bold text-gray-900 mt-1">
                              ₹{booking.totalAmount}
                            </p>
                          </div>
                        </div>
                        
                        <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mb-4 text-sm">
                          <div>
                            <p className="text-gray-500">Departure</p>
                            <p className="font-medium">
                              {new Date(booking.departureDate).toLocaleDateString()}
                            </p>
                          </div>
                          <div>
                            <p className="text-gray-500">Seats Booked</p>
                            <p className="font-medium">{booking.seatsBooked}</p>
                          </div>
                          <div>
                            <p className="text-gray-500">Booking Date</p>
                            <p className="font-medium">
                              {new Date(booking.bookingDate).toLocaleDateString()}
                            </p>
                          </div>
                          <div>
                            <p className="text-gray-500">Vehicle</p>
                            <p className="font-medium">{booking.vehicleModel}</p>
                          </div>
                        </div>

                        {/* Booking Management Buttons */}
                        {booking.status === 'PENDING' && (
                          <div className="flex space-x-2 mt-4">
                            <button
                              onClick={() => handleBookingAction(booking.rideId, booking.id, 'confirm')}
                              disabled={loading}
                              className="bg-green-500 hover:bg-green-600 text-white px-4 py-2 rounded font-medium transition-colors disabled:opacity-50"
                            >
                              Confirm Booking
                            </button>
                            <button
                              onClick={() => handleBookingAction(booking.rideId, booking.id, 'cancel')}
                              disabled={loading}
                              className="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded font-medium transition-colors disabled:opacity-50"
                            >
                              Reject Booking
                            </button>
                          </div>
                        )}
                        
                        {booking.status === 'CONFIRMED' && (
                          <div className="flex space-x-2 mt-4">
                            <div className="bg-green-50 text-green-700 px-4 py-2 rounded font-medium">
                              ✓ Booking Confirmed
                            </div>
                            <button
                              onClick={() => handleBookingAction(booking.rideId, booking.id, 'cancel')}
                              disabled={loading}
                              className="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded font-medium transition-colors disabled:opacity-50"
                            >
                              Cancel Booking
                            </button>
                          </div>
                        )}
                        
                        {booking.status === 'CANCELLED' && (
                          <div className="bg-red-50 text-red-700 px-4 py-2 rounded font-medium mt-4">
                            ✗ Booking Cancelled
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            )}

            {/* Wallet Tab */}
            {activeTab === 'wallet' && (
              <DriverWallet driverId={user.id} />
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default DriverDashboard