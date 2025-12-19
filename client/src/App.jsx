import { useState, useEffect } from "react"
import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from "react-router-dom"
import LandingPage from "./components/LandingPage"
import LoginPage from "./components/LoginPage"
import RegisterPage from "./components/RegisterPage"
import AdminLogin from "./components/AdminLogin"
import DriverDashboard from "./components/DriverDashboard"
import PassengerDashboard from "./components/PassengerDashboard"
import AdminDashboard from "./components/AdminDashboard"
import OtpVerification from "./components/OtpVerification"
import Header from "./components/Header"
import Footer from "./components/Footer"
import apiService from "./services/api"

// Component to conditionally render Header and Footer
const Layout = ({ children, user, userType, onLogout }) => {
  const location = useLocation()
  
  // Show header on all pages, but footer only on non-dashboard pages
  const isDashboardPage = ['/driver-dashboard', '/passenger-dashboard', '/admin-dashboard'].includes(location.pathname)
  
  return (
    <>
      <Header user={user} userType={userType} onLogout={onLogout} />
      <main className={isDashboardPage ? "min-h-screen" : "min-h-screen"}>
        {children}
      </main>
      {!isDashboardPage && <Footer />}
    </>
  )
}

function App() {
  const [user, setUser] = useState(null)
  const [userType, setUserType] = useState(null)

  const handleLogin = (userData, type) => {
    setUser(userData)
    setUserType(type)
    localStorage.setItem('user', JSON.stringify(userData))
    localStorage.setItem('userType', type)
  }

  const handleLogout = () => {
    setUser(null)
    setUserType(null)
    localStorage.removeItem('user')
    localStorage.removeItem('userType')
    localStorage.removeItem('token')
    apiService.setToken(null)
  }

  // Check for existing session on app load
  useEffect(() => {
    const savedUser = localStorage.getItem('user')
    const savedUserType = localStorage.getItem('userType')
    const savedToken = localStorage.getItem('token')
    
    if (savedUser && savedUserType && savedToken) {
      setUser(JSON.parse(savedUser))
      setUserType(savedUserType)
      apiService.setToken(savedToken)
    }
  }, [])

  return (
    <Router>
      <div className="min-h-screen bg-gradient-to-br from-yellow-50 to-orange-50">
        <Layout user={user} userType={userType} onLogout={handleLogout}>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route
              path="/login"
              element={
                user ? (
                  <Navigate to={
                    userType === "driver" ? "/driver-dashboard" : 
                    userType === "passenger" ? "/passenger-dashboard" : 
                    "/admin-dashboard"
                  } />
                ) : (
                  <LoginPage onLogin={handleLogin} />
                )
              }
            />
            <Route path="/register" element={<RegisterPage onLogin={handleLogin} />} />
            <Route path="/admin-login" element={<AdminLogin onLogin={handleLogin} />} />
            <Route
              path="/driver-dashboard"
              element={user && userType === "driver" ? <DriverDashboard user={user} /> : <Navigate to="/login" />}
            />
            <Route
              path="/passenger-dashboard"
              element={user && userType === "passenger" ? <PassengerDashboard user={user} /> : <Navigate to="/login" />}
            />
            <Route
              path="/admin-dashboard"
              element={user && userType === "admin" ? <AdminDashboard user={user} /> : <Navigate to="/admin-login" />}
            />
          </Routes>
        </Layout>
      </div>
    </Router>
  )
}

export default App
