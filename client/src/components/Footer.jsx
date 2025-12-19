import { Link } from 'react-router-dom'
import { Car, Mail, Phone, MapPin, Facebook, Twitter, Instagram, Linkedin } from 'lucide-react'

const Footer = () => {
  return (
    <footer className="bg-gray-900 text-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
          {/* Brand Section */}
          <div className="space-y-4">
            <Link to="/" className="flex items-center space-x-2">
              <div className="bg-yellow-500 p-2 rounded-lg">
                <Car className="h-6 w-6 text-white" />
              </div>
              <span className="text-2xl font-bold">SmartRide</span>
            </Link>
            <p className="text-gray-400">
              Connecting travelers, reducing costs, and building a sustainable future through smart ride-sharing.
            </p>
            <div className="flex space-x-4">
              <a href="#" className="text-gray-400 hover:text-yellow-500 transition-colors">
                <Facebook className="h-5 w-5" />
              </a>
              <a href="#" className="text-gray-400 hover:text-yellow-500 transition-colors">
                <Twitter className="h-5 w-5" />
              </a>
              <a href="#" className="text-gray-400 hover:text-yellow-500 transition-colors">
                <Instagram className="h-5 w-5" />
              </a>
              <a href="#" className="text-gray-400 hover:text-yellow-500 transition-colors">
                <Linkedin className="h-5 w-5" />
              </a>
            </div>
          </div>

          {/* Quick Links */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Quick Links</h3>
            <ul className="space-y-3">
              <li>
                <Link to="/#features" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  Features
                </Link>
              </li>
              <li>
                <Link to="/#how-it-works" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  How It Works
                </Link>
              </li>
              <li>
                <Link to="/register" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  Sign Up
                </Link>
              </li>
              <li>
                <Link to="/login" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  Login
                </Link>
              </li>
            </ul>
          </div>

          {/* Services */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Services</h3>
            <ul className="space-y-3">
              <li>
                <Link to="/register?type=passenger" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  Find a Ride
                </Link>
              </li>
              <li>
                <Link to="/register?type=driver" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  Offer a Ride
                </Link>
              </li>
              <li>
                <a href="#" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  Safety Guidelines
                </a>
              </li>
              <li>
                <a href="#" className="text-gray-400 hover:text-yellow-500 transition-colors">
                  Support
                </a>
              </li>
            </ul>
          </div>

          {/* Contact Info */}
          <div>
            <h3 className="text-lg font-semibold mb-4">Contact Us</h3>
            <ul className="space-y-3">
              <li className="flex items-center space-x-3">
                <Mail className="h-5 w-5 text-yellow-500" />
                <span className="text-gray-400">support@smartride.com</span>
              </li>
              <li className="flex items-center space-x-3">
                <Phone className="h-5 w-5 text-yellow-500" />
                <span className="text-gray-400">+91 9876543210</span>
              </li>
              <li className="flex items-center space-x-3">
                <MapPin className="h-5 w-5 text-yellow-500" />
                <span className="text-gray-400">Mumbai, Maharashtra, India</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Bottom Section */}
        <div className="border-t border-gray-800 mt-12 pt-8">
          <div className="flex flex-col md:flex-row justify-between items-center">
            <p className="text-gray-400 text-sm">
              © 2024 SmartRide. All rights reserved.
            </p>
            <div className="flex space-x-6 mt-4 md:mt-0">
              <a href="#" className="text-gray-400 hover:text-yellow-500 text-sm transition-colors">
                Privacy Policy
              </a>
              <a href="#" className="text-gray-400 hover:text-yellow-500 text-sm transition-colors">
                Terms of Service
              </a>
              <a href="#" className="text-gray-400 hover:text-yellow-500 text-sm transition-colors">
                Cookie Policy
              </a>
            </div>
          </div>
        </div>
      </div>
    </footer>
  )
}

export default Footer