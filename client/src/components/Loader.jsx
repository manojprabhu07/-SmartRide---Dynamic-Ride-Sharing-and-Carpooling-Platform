import React from 'react';
import { DotLottieReact } from '@lottiefiles/dotlottie-react';

const Loader = ({ 
  size = 200, 
  showText = true,
  text = "Loading...",
  className = "",
  overlay = false 
}) => {
  const containerStyles = overlay 
    ? "fixed inset-0 bg-white bg-opacity-95 flex flex-col items-center justify-center z-50"
    : "flex flex-col items-center justify-center";

  return (
    <div className={`${containerStyles} ${className}`}>
      <div className="relative flex items-center justify-center" style={{ width: size, height: size }}>
        {/* Primary Lottie Animation */}
        <div className="relative z-10 lottie-container">
          <DotLottieReact
            src="https://lottie.host/73967f37-44c1-4592-b5c2-57e84de96053/1UB0bZNir7.lottie"
            loop={true}
            autoplay={true}
            style={{ 
              width: size, 
              height: size,
              maxWidth: '100%',
              maxHeight: '100%'
            }}
            onLoadError={(error) => {
              console.error('Lottie loading error:', error);
              // Hide Lottie container and show fallback
              const lottieContainer = document.querySelector('.lottie-container');
              const fallbackElement = document.querySelector('.lottie-fallback');
              if (lottieContainer) lottieContainer.style.display = 'none';
              if (fallbackElement) fallbackElement.style.display = 'flex';
            }}
            onLoad={() => {
              console.log('Lottie loaded successfully');
              // Ensure Lottie is visible and fallback is hidden
              const lottieContainer = document.querySelector('.lottie-container');
              const fallbackElement = document.querySelector('.lottie-fallback');
              if (lottieContainer) lottieContainer.style.display = 'block';
              if (fallbackElement) fallbackElement.style.display = 'none';
            }}
          />
        </div>

        {/* Fallback Custom CSS Car Animation (shown only if Lottie fails) */}
        <div className="lottie-fallback absolute inset-0 flex items-center justify-center" style={{ display: 'none' }}>
          <div className="flex items-center justify-center">
            <div className="w-16 h-10 bg-yellow-500 rounded-lg relative shadow-lg animate-bounce" style={{ animationDuration: '1.5s' }}>
              {/* Simple car fallback */}
              <div className="absolute -top-2 left-2 right-2 h-4 bg-yellow-600 rounded-t-lg"></div>
              <div className="absolute top-1 left-3 w-3 h-2 bg-blue-200 rounded-sm opacity-80"></div>
              <div className="absolute top-1 right-3 w-3 h-2 bg-blue-200 rounded-sm opacity-80"></div>
              <div className="absolute -bottom-1 left-1 w-4 h-4 bg-gray-800 rounded-full animate-spin" style={{ animationDuration: '0.8s' }}>
                <div className="absolute inset-1 bg-gray-300 rounded-full"></div>
              </div>
              <div className="absolute -bottom-1 right-1 w-4 h-4 bg-gray-800 rounded-full animate-spin" style={{ animationDuration: '0.8s' }}>
                <div className="absolute inset-1 bg-gray-300 rounded-full"></div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
        {showText && (
          <p className="mt-6 text-lg font-medium text-gray-700 animate-pulse">
            {text}
          </p>
        )}
      </div>
      
    );
  };
  
  export default Loader;