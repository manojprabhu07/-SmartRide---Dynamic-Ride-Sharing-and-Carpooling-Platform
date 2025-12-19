import { Link } from 'react-router-dom'
import { ArrowRight, Shield, CreditCard, MapPin, Clock, Users, Briefcase } from 'lucide-react'
import { useEffect, useState } from 'react'
import AOS from 'aos'
import 'aos/dist/aos.css'
import { DotLottieReact } from '@lottiefiles/dotlottie-react'
import Loader from './Loader'

const LandingPage = () => {
  const [isLoading, setIsLoading] = useState(false)
  const [isPageLoading, setIsPageLoading] = useState(true)
  const [currentQuote, setCurrentQuote] = useState(0)
  
  // Ride-sharing related quotes
  const quotes = [
    "Share the journey, share the joy! 🚗✨",
    "Together we travel farther and smarter! 🌟",
    "Your next adventure is just a ride away! 🛣️",
    "Making every mile more affordable! 💰",
    "Connect, travel, and save the planet! 🌍",
    "Where every trip becomes a friendship! 👥",
    "Smart rides for smart people! 🧠",
    "Reducing carbon footprint, one ride at a time! 🌱"
  ]

  useEffect(() => {
    AOS.init({
      duration: 1000,
      once: true,
      offset: 100,
      easing: 'ease-out-cubic'
    })
  }, [])

  // Initial page loading effect
  useEffect(() => {
    const initializePage = async () => {
      // Simulate page loading time
      await new Promise(resolve => setTimeout(resolve, 2500))
      setIsPageLoading(false)
    }
    
    initializePage()
  }, [])

  // Handle quote rotation during any loading
  useEffect(() => {
    let interval = null
    if (isLoading || isPageLoading) {
      interval = setInterval(() => {
        setCurrentQuote(prev => (prev + 1) % quotes.length)
      }, 2000) // Change quote every 2 seconds
    }
    return () => {
      if (interval) clearInterval(interval)
    }
  }, [isLoading, isPageLoading, quotes.length])

  const handleSignUpClick = (type) => {
    setIsLoading(true)
    // Simulate navigation delay
    setTimeout(() => {
      window.location.href = `/register?type=${type}`
    }, 3000)
  }

  return (
    <div className="min-h-screen">
      {/* Initial Page Loading */}
      {isPageLoading && (
        <div className="fixed inset-0 bg-gradient-to-br from-yellow-50 to-orange-50 flex flex-col items-center justify-center z-50">
          <Loader 
            size={280}
            showText={false}
            className="mb-8"
          />
          <div className="text-center max-w-lg mx-auto px-4">
            <h1 className="text-4xl font-bold text-gray-900 mb-4">
              Welcome to <span className="text-yellow-500">SmartRide</span>
            </h1>
            <p className="text-xl text-yellow-600 font-me dium mb-2 animate-pulse">
              {quotes[currentQuote]}
            </p>
            <div className="flex justify-center space-x-2 mt-6">
              {quotes.map((_, index) => (
                <div
                  key={index}
                  className={`w-2 h-2 rounded-full transition-all duration-300 ${
                    index === currentQuote ? 'bg-yellow-500 scale-125' : 'bg-gray-300'
                  }`}
                />
              ))}
            </div>
            <p className="text-sm text-gray-500 mt-4">Loading your journey...</p>
          </div>
        </div>
      )}

      {/* Hero Section */}
      <section className="bg-gradient-to-br from-yellow-50 to-orange-50 py-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-12 items-center">
            {/* Left Content */}
            <div className="space-y-8" data-aos="fade-right" data-aos-delay="100">
              <h1 className="text-4xl md:text-6xl font-bold text-gray-900 leading-tight" data-aos="fade-up" data-aos-delay="200">
                Share Your Journey,{' '}
                <span className="text-yellow-500">Save Money</span>
              </h1>
              <p className="text-lg md:text-xl text-gray-600 leading-relaxed" data-aos="fade-up" data-aos-delay="300">
                Connect with fellow travelers heading in the same direction. Split costs, reduce emissions, and make new connections on every trip.
              </p>
              <div className="flex flex-col sm:flex-row gap-4" data-aos="fade-up" data-aos-delay="400">
                <button
                  onClick={() => handleSignUpClick('passenger')}
                  disabled={isLoading}
                  className="bg-yellow-500 hover:bg-yellow-600 text-white px-8 py-4 rounded-lg font-semibold text-lg transition-all duration-200 hover:shadow-lg text-center transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Find a Ride
                </button>
                <button
                  onClick={() => handleSignUpClick('driver')}
                  disabled={isLoading}
                  className="border-2 border-yellow-500 text-yellow-600 hover:bg-yellow-500 hover:text-white px-8 py-4 rounded-lg font-semibold text-lg transition-all duration-200 text-center transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  Offer a Ride
                </button>
              </div>
            </div>

            {/* Right Content - Hero Animation */}
            <div className="relative" data-aos="fade-left" data-aos-delay="200">
              <div className="rounded-2xl overflow-hidden shadow-2xl transform hover:scale-105 transition-transform duration-300 bg-yellow-500">
                <DotLottieReact
                  src="https://lottie.host/17b071fc-6b6b-4e5f-ba2e-50416292af41/OZz2D8EmxL.lottie"
                  loop
                  autoplay
                  className="w-full h-auto object-cover"
                />
              </div>
              
              {/* Floating Card */}
              {/* <div className="absolute -bottom-6 -left-6 bg-white rounded-2xl p-6 shadow-xl border-l-4 border-yellow-500">
                <div className="flex items-center space-x-4">
                  <div className="bg-yellow-100 p-3 rounded-full">
                    <Users className="h-6 w-6 text-yellow-600" />
                  </div>
                  <div>
                    <div className="font-semibold text-gray-900">Mumbai → Pune</div>
                    <div className="text-gray-500">Today, 2:00 PM</div>
                    <div className="text-right">
                      <span className="text-lg font-bold text-yellow-600">₹450</span>
                      <span className="text-gray-500 text-sm ml-1">per seat</span>
                    </div>
                  </div>
                </div>
              </div> */}
            </div>
          </div>
        </div>
      </section>

      {/* Features Section */}
      <section id="features" className="py-20 bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16" data-aos="fade-up">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4" data-aos="fade-up" data-aos-delay="100">
              Why Choose SmartRide?
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto" data-aos="fade-up" data-aos-delay="200">
              Experience the future of ride-sharing with our comprehensive platform designed for safety, convenience, and affordability.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
            {/* Feature 1 */}
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2" data-aos="fade-up" data-aos-delay="100">
              <div className="bg-yellow-100 w-16 h-16 rounded-full flex items-center justify-center mb-6 transform transition-transform duration-300 hover:scale-110">
                <Shield className="h-8 w-8 text-yellow-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Verified Drivers</h3>
              <p className="text-gray-600">
                All drivers are verified with valid licenses and vehicle documents for your safety.
              </p>
            </div>

            {/* Feature 2 */}
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2" data-aos="fade-up" data-aos-delay="200">
              <div className="bg-yellow-100 w-16 h-16 rounded-full flex items-center justify-center mb-6 transform transition-transform duration-300 hover:scale-110">
                <CreditCard className="h-8 w-8 text-yellow-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Secure Payments</h3>
              <p className="text-gray-600">
                Safe and secure payment processing with multiple payment options available.
              </p>
            </div>

            {/* Feature 3 */}
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2" data-aos="fade-up" data-aos-delay="300">
              <div className="bg-yellow-100 w-16 h-16 rounded-full flex items-center justify-center mb-6 transform transition-transform duration-300 hover:scale-110">
                <MapPin className="h-8 w-8 text-yellow-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Smart Matching</h3>
              <p className="text-gray-600">
                Advanced algorithm matches you with rides based on your route and preferences.
              </p>
            </div>

            {/* Feature 4 */}
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2" data-aos="fade-up" data-aos-delay="100">
              <div className="bg-yellow-100 w-16 h-16 rounded-full flex items-center justify-center mb-6 transform transition-transform duration-300 hover:scale-110">
                <Clock className="h-8 w-8 text-yellow-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Real-time Tracking</h3>
              <p className="text-gray-600">
                Track your ride in real-time and get updates on your journey progress.
              </p>
            </div>

            {/* Feature 5 */}
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2" data-aos="fade-up" data-aos-delay="200">
              <div className="bg-yellow-100 w-16 h-16 rounded-full flex items-center justify-center mb-6 transform transition-transform duration-300 hover:scale-110">
                <Users className="h-8 w-8 text-yellow-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-4">User Reviews</h3>
              <p className="text-gray-600">
                Rate and review your travel experience to help build a trusted community.
              </p>
            </div>

            {/* Feature 6 */}
            <div className="bg-white p-8 rounded-2xl shadow-lg border border-gray-100 hover:shadow-xl transition-all duration-300 transform hover:-translate-y-2" data-aos="fade-up" data-aos-delay="300">
              <div className="bg-yellow-100 w-16 h-16 rounded-full flex items-center justify-center mb-6 transform transition-transform duration-300 hover:scale-110">
                <Briefcase className="h-8 w-8 text-yellow-600" />
              </div>
              <h3 className="text-xl font-semibold text-gray-900 mb-4">Flexible Booking</h3>
              <p className="text-gray-600">
                Book rides instantly or schedule them in advance for your convenience.
              </p>
            </div>
          </div>
        </div>
      </section>

      {/* How It Works Section */}
      <section id="how-it-works" className="py-20 bg-gray-50">
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="text-center mb-16" data-aos="fade-up">
            <h2 className="text-3xl md:text-4xl font-bold text-gray-900 mb-4" data-aos="fade-up" data-aos-delay="100">
              How SmartRide Works
            </h2>
            <p className="text-xl text-gray-600 max-w-3xl mx-auto" data-aos="fade-up" data-aos-delay="200">
              Getting started with SmartRide is simple. Follow these easy steps to begin your ride-sharing journey.
            </p>
          </div>

          <div className="space-y-16">
            {/* Step 1 - Sign Up */}
            <div className="flex flex-col lg:flex-row items-center gap-12">
              <div className="lg:w-1/2" data-aos="fade-right" data-aos-delay="100">
                <div className="relative">
                  <img
                    src="sign.jpg"
                    alt="Person signing up on mobile phone"
                    className="w-full h-80 object-cover rounded-2xl shadow-lg transform hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute -top-4 -left-4 bg-yellow-500 text-white w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold shadow-lg animate-pulse">
                    01
                  </div>
                </div>
              </div>
              <div className="lg:w-1/2 space-y-6" data-aos="fade-left" data-aos-delay="200">
                <div className="flex items-center gap-4">
                  <div className="bg-yellow-100 p-4 rounded-full transform hover:scale-110 transition-transform duration-300">
                    <Users className="h-8 w-8 text-yellow-600" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900">Sign Up</h3>
                </div>
                <p className="text-lg text-gray-600 leading-relaxed">
                  Create your account as a driver or passenger in just a few minutes. Choose your role, verify your identity, and get ready to start sharing rides with our community.
                </p>
                <ul className="space-y-2 text-gray-600">
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Quick registration process
                  </li>
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Identity verification for safety
                  </li>
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Driver license validation
                  </li>
                </ul>
              </div>
            </div>

            {/* Connecting Line */}
            <div className="flex justify-center" data-aos="fade-up" data-aos-delay="100">
              <div className="w-1 h-16 bg-gradient-to-b from-yellow-200 to-yellow-400 rounded-full animate-pulse"></div>
            </div>

            {/* Step 2 - Find or Post */}
            <div className="flex flex-col lg:flex-row-reverse items-center gap-12">
              <div className="lg:w-1/2" data-aos="fade-left" data-aos-delay="100">
                <div className="relative">
                  <img
                    src="find.jpg"
                    alt="Person searching for rides on map"
                    className="w-full h-80 object-cover rounded-2xl shadow-lg transform hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute -top-4 -right-4 bg-yellow-500 text-white w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold shadow-lg animate-pulse">
                    02
                  </div>
                </div>
              </div>
              <div className="lg:w-1/2 space-y-6" data-aos="fade-right" data-aos-delay="200">
                <div className="flex items-center gap-4">
                  <div className="bg-yellow-100 p-4 rounded-full transform hover:scale-110 transition-transform duration-300">
                    <MapPin className="h-8 w-8 text-yellow-600" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900">Find or Post</h3>
                </div>
                <p className="text-lg text-gray-600 leading-relaxed">
                  Search for available rides or post your own trip with available seats. Use our interactive map to find the perfect match for your journey.
                </p>
                <ul className="space-y-2 text-gray-600">
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Real-time ride availability
                  </li>
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Interactive map interface
                  </li>
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Flexible pickup points
                  </li>
                </ul>
              </div>
            </div>

            {/* Connecting Line */}
            <div className="flex justify-center" data-aos="fade-up" data-aos-delay="100">
              <div className="w-1 h-16 bg-gradient-to-b from-yellow-200 to-yellow-400 rounded-full animate-pulse"></div>
            </div>

            {/* Step 3 - Connect */}
            <div className="flex flex-col lg:flex-row items-center gap-12">
              <div className="lg:w-1/2" data-aos="fade-right" data-aos-delay="100">
                <div className="relative">
                  <img
                    src="connect.jpg"
                    alt="People connecting and confirming ride booking"
                    className="w-full h-80 object-cover rounded-2xl shadow-lg transform hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute -top-4 -left-4 bg-yellow-500 text-white w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold shadow-lg animate-pulse">
                    03
                  </div>
                </div>
              </div>
              <div className="lg:w-1/2 space-y-6" data-aos="fade-left" data-aos-delay="200">
                <div className="flex items-center gap-4">
                  <div className="bg-yellow-100 p-4 rounded-full transform hover:scale-110 transition-transform duration-300">
                    <Shield className="h-8 w-8 text-yellow-600" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900">Connect</h3>
                </div>
                <p className="text-lg text-gray-600 leading-relaxed">
                  Get matched with compatible travelers and confirm your booking. Our system ensures safe connections with verified users and transparent communication.
                </p>
                <ul className="space-y-2 text-gray-600">
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Verified user profiles
                  </li>
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    In-app messaging system
                  </li>
                  <li className="flex items-center gap-2">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Instant booking confirmation
                  </li>
                </ul>
              </div>
            </div>

            {/* Connecting Line */}
            <div className="flex justify-center" data-aos="fade-up" data-aos-delay="100">
              <div className="w-1 h-16 bg-gradient-to-b from-yellow-200 to-yellow-400 rounded-full animate-pulse"></div>
            </div>

            {/* Step 4 - Travel */}
            <div className="flex flex-col lg:flex-row-reverse items-center gap-12">
              <div className="lg:w-1/2" data-aos="fade-left" data-aos-delay="100">
                <div className="relative">
                  <img
                    src="travel.jpg"
                    alt="Happy travelers in car sharing a ride"
                    className="w-full h-80 object-cover rounded-2xl shadow-lg transform hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute -top-4 -right-4 bg-yellow-500 text-white w-16 h-16 rounded-full flex items-center justify-center text-2xl font-bold shadow-lg animate-pulse">
                    04
                  </div>
                </div>
              </div>
              <div className="lg:w-1/2 space-y-6" data-aos="fade-right" data-aos-delay="200">
                <div className="flex items-center gap-4">
                  <div className="bg-yellow-100 p-4 rounded-full transform hover:scale-110 transition-transform duration-300">
                    <Briefcase className="h-8 w-8 text-yellow-600" />
                  </div>
                  <h3 className="text-2xl font-bold text-gray-900">Travel</h3>
                </div>
                <p className="text-lg text-gray-600 leading-relaxed">
                  Enjoy your shared journey with real-time tracking and secure payments. Travel comfortably knowing you're saving money and helping the environment.
                </p>
                <ul className="space-y-2 text-gray-600">
                  <li className="flex items-center gap-2" data-aos="fade-up" data-aos-delay="300">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Real-time GPS tracking
                  </li>
                  <li className="flex items-center gap-2" data-aos="fade-up" data-aos-delay="400">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    Secure payment processing
                  </li>
                  <li className="flex items-center gap-2" data-aos="fade-up" data-aos-delay="500">
                    <div className="w-2 h-2 bg-yellow-500 rounded-full"></div>
                    24/7 customer support
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-20 bg-yellow-500">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-4" data-aos="fade-up" data-aos-delay="100">
            Ready to Start Your Journey?
          </h2>
          <p className="text-xl text-yellow-100 mb-8 max-w-2xl mx-auto" data-aos="fade-up" data-aos-delay="200">
            Join thousands of travelers who are already saving money and reducing their carbon footprint.
          </p>
          <div className="flex flex-col sm:flex-row gap-4 justify-center" data-aos="fade-up" data-aos-delay="300">
            <button
              onClick={() => handleSignUpClick('passenger')}
              disabled={isLoading}
              className="bg-white text-yellow-600 hover:bg-gray-50 px-8 py-4 rounded-lg font-semibold text-lg transition-all duration-200 hover:shadow-lg transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Sign Up as Passenger
            </button>
            <button
              onClick={() => handleSignUpClick('driver')}
              disabled={isLoading}
              className="border-2 border-white text-white hover:bg-white hover:text-yellow-600 px-8 py-4 rounded-lg font-semibold text-lg transition-all duration-200 transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Sign Up as Driver
            </button>
          </div>
        </div>
      </section>

      {/* Loading Overlay with Quotes */}
      {isLoading && (
        <div className="fixed inset-0 bg-white bg-opacity-95 flex flex-col items-center justify-center z-50">
          <Loader 
            size={250}
            showText={false}
            className="mb-8"
          />
          <div className="text-center max-w-lg mx-auto px-4">
            <h3 className="text-2xl font-bold text-gray-900 mb-4">
              Getting Ready for Your Journey...
            </h3>
            <p className="text-xl text-yellow-600 font-medium mb-2 animate-pulse">
              {quotes[currentQuote]}
            </p>
            <div className="flex justify-center space-x-2 mt-6">
              {quotes.map((_, index) => (
                <div
                  key={index}
                  className={`w-2 h-2 rounded-full transition-all duration-300 ${
                    index === currentQuote ? 'bg-yellow-500 scale-125' : 'bg-gray-300'
                  }`}
                />
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default LandingPage