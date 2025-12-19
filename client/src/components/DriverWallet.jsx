import React, { useState, useEffect } from 'react';
import apiService from '../services/api';

const DriverWallet = ({ driverId }) => {
    const [earnings, setEarnings] = useState({
        totalEarnings: 0,
        pendingEarnings: 0,
        completedEarnings: 0,
        todayEarnings: 0,
        recentPayments: []
    });
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [notifications, setNotifications] = useState([]);

    useEffect(() => {
        fetchEarningsData();
        // Set up polling for real-time updates
        const interval = setInterval(fetchEarningsData, 30000); // Poll every 30 seconds
        return () => clearInterval(interval);
    }, [driverId]);

    const fetchEarningsData = async () => {
        try {
            setLoading(true);
            console.log('🚗 Fetching earnings for driver ID:', driverId);
            
            if (!driverId) {
                console.error('❌ Driver ID is missing');
                setError('Driver ID is required');
                return;
            }
            
            // Fetch driver payment history
            const historyResponse = await apiService.apiCall(
                `/payments/history/driver/${driverId}`,
                'GET'
            );

            console.log('💰 Payment history response:', historyResponse);

            if (historyResponse.success) {
                const payments = historyResponse.data;
                
                // Calculate earnings
                const totalEarnings = payments
                    .filter(p => p.paymentStatus === 'COMPLETED')
                    .reduce((sum, p) => sum + (p.driverSettlementAmount || 0), 0);
                
                const pendingEarnings = payments
                    .filter(p => p.settlementStatus === 'PENDING')
                    .reduce((sum, p) => sum + (p.driverSettlementAmount || 0), 0);
                
                const completedEarnings = payments
                    .filter(p => p.settlementStatus === 'COMPLETED')
                    .reduce((sum, p) => sum + (p.driverSettlementAmount || 0), 0);

                // Today's earnings
                const today = new Date().toDateString();
                const todayEarnings = payments
                    .filter(p => p.settlementStatus === 'COMPLETED' && 
                                new Date(p.settlementDate).toDateString() === today)
                    .reduce((sum, p) => sum + (p.driverSettlementAmount || 0), 0);

                // Recent payments (last 5)
                const recentPayments = payments
                    .filter(p => p.paymentStatus === 'COMPLETED')
                    .sort((a, b) => new Date(b.paidAt) - new Date(a.paidAt))
                    .slice(0, 5);

                setEarnings({
                    totalEarnings,
                    pendingEarnings,
                    completedEarnings,
                    todayEarnings,
                    recentPayments
                });

                // Check for new payments
                checkForNewPayments(recentPayments);
            } else {
                setError('Failed to fetch earnings data');
            }
        } catch (err) {
            console.error('Error fetching earnings:', err);
            setError('Error loading earnings data');
        } finally {
            setLoading(false);
        }
    };

    const checkForNewPayments = (newPayments) => {
        // Compare with previous payments to show notifications
        const previousPaymentIds = earnings.recentPayments.map(p => p.paymentId);
        const newPaymentNotifications = newPayments
            .filter(payment => !previousPaymentIds.includes(payment.paymentId))
            .map(payment => ({
                id: payment.paymentId,
                message: `💰 Payment received: ₹${payment.driverSettlementAmount}`,
                timestamp: new Date(),
                type: 'success'
            }));

        if (newPaymentNotifications.length > 0) {
            setNotifications(prev => [...newPaymentNotifications, ...prev].slice(0, 5));
            
            // Auto-hide notifications after 5 seconds
            setTimeout(() => {
                setNotifications(prev => 
                    prev.filter(n => !newPaymentNotifications.some(nn => nn.id === n.id))
                );
            }, 5000);
        }
    };

    const formatCurrency = (amount) => {
        return `₹${Number(amount || 0).toFixed(2)}`;
    };

    const formatDateTime = (dateTime) => {
        return new Date(dateTime).toLocaleString('en-IN', {
            dateStyle: 'short',
            timeStyle: 'short'
        });
    };

    if (loading) {
        return (
            <div className="driver-wallet bg-white rounded-lg shadow-md p-6">
                <div className="animate-pulse">
                    <div className="h-6 bg-gray-200 rounded mb-4"></div>
                    <div className="grid grid-cols-2 gap-4">
                        <div className="h-20 bg-gray-200 rounded"></div>
                        <div className="h-20 bg-gray-200 rounded"></div>
                    </div>
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="driver-wallet bg-white rounded-lg shadow-md p-6">
                <div className="text-red-600 text-center">
                    <span className="text-2xl">⚠️</span>
                    <p className="mt-2">{error}</p>
                    <button 
                        onClick={fetchEarningsData}
                        className="mt-3 px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
                    >
                        Retry
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="driver-wallet space-y-6">
            {/* Notifications */}
            {notifications.length > 0 && (
                <div className="fixed top-4 right-4 z-50 space-y-2">
                    {notifications.map((notification) => (
                        <div
                            key={notification.id}
                            className="bg-green-500 text-white px-4 py-3 rounded-lg shadow-lg flex items-center space-x-2 animate-bounce"
                        >
                            <span>{notification.message}</span>
                            <button
                                onClick={() => setNotifications(prev => 
                                    prev.filter(n => n.id !== notification.id)
                                )}
                                className="text-white hover:text-gray-200"
                            >
                                ×
                            </button>
                        </div>
                    ))}
                </div>
            )}

            {/* Earnings Summary */}
            <div className="bg-white rounded-lg shadow-md p-6">
                <h2 className="text-2xl font-bold text-gray-800 mb-6 flex items-center">
                    <span className="text-green-500 mr-2">💳</span>
                    Driver Wallet
                </h2>

                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4 mb-6">
                    {/* Today's Earnings */}
                    <div className="bg-gradient-to-r from-green-400 to-green-600 text-white p-4 rounded-lg">
                        <div className="text-sm font-medium opacity-90">Today's Earnings</div>
                        <div className="text-2xl font-bold">{formatCurrency(earnings.todayEarnings)}</div>
                    </div>

                    {/* Total Earnings */}
                    <div className="bg-gradient-to-r from-blue-400 to-blue-600 text-white p-4 rounded-lg">
                        <div className="text-sm font-medium opacity-90">Total Earnings</div>
                        <div className="text-2xl font-bold">{formatCurrency(earnings.totalEarnings)}</div>
                    </div>

                    {/* Completed Earnings */}
                    <div className="bg-gradient-to-r from-purple-400 to-purple-600 text-white p-4 rounded-lg">
                        <div className="text-sm font-medium opacity-90">Completed</div>
                        <div className="text-2xl font-bold">{formatCurrency(earnings.completedEarnings)}</div>
                    </div>

                    {/* Pending Earnings */}
                    <div className="bg-gradient-to-r from-yellow-400 to-yellow-600 text-white p-4 rounded-lg">
                        <div className="text-sm font-medium opacity-90">Pending</div>
                        <div className="text-2xl font-bold">{formatCurrency(earnings.pendingEarnings)}</div>
                    </div>
                </div>

                {/* Recent Payments */}
                <div className="bg-gray-50 rounded-lg p-4">
                    <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center">
                        <span className="text-blue-500 mr-2">📊</span>
                        Recent Payments
                    </h3>

                    {earnings.recentPayments.length > 0 ? (
                        <div className="space-y-3">
                            {earnings.recentPayments.map((payment) => (
                                <div 
                                    key={payment.paymentId}
                                    className="bg-white p-4 rounded-lg border border-gray-200 flex justify-between items-center"
                                >
                                    <div>
                                        <div className="font-semibold text-gray-800">
                                            {payment.source} → {payment.destination}
                                        </div>
                                        <div className="text-sm text-gray-600">
                                            Passenger: {payment.passengerName}
                                        </div>
                                        <div className="text-sm text-gray-500">
                                            {formatDateTime(payment.paidAt)}
                                        </div>
                                    </div>
                                    <div className="text-right">
                                        <div className="text-lg font-bold text-green-600">
                                            {formatCurrency(payment.driverSettlementAmount)}
                                        </div>
                                        <div className={`text-sm px-2 py-1 rounded ${
                                            payment.settlementStatus === 'COMPLETED' 
                                                ? 'bg-green-100 text-green-800' 
                                                : 'bg-yellow-100 text-yellow-800'
                                        }`}>
                                            {payment.settlementStatus}
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    ) : (
                        <div className="text-center text-gray-500 py-8">
                            <span className="text-4xl">💰</span>
                            <p className="mt-2">No payments yet</p>
                            <p className="text-sm">Complete rides to start earning!</p>
                        </div>
                    )}
                </div>

                {/* Refresh Button */}
                <div className="flex justify-center mt-4">
                    <button
                        onClick={fetchEarningsData}
                        className="px-6 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors flex items-center space-x-2"
                    >
                        <span>🔄</span>
                        <span>Refresh Earnings</span>
                    </button>
                </div>
            </div>
        </div>
    );
};

export default DriverWallet;