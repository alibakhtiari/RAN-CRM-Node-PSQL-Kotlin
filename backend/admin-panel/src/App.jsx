import React, { useState, useEffect } from 'react';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Contacts from './pages/Contacts';
import CallLogs from './pages/CallLogs';

import { api } from './services/api';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentUser, setCurrentUser] = useState(null);

  // Initialize view from URL or default to 'dashboard'
  const getInitialView = () => {
    const params = new URLSearchParams(window.location.search);
    return params.get('view') || 'dashboard';
  };

  const [currentView, setCurrentView] = useState(getInitialView);

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      setIsAuthenticated(true);
      fetchCurrentUser();
    }
  }, []);

  // Update URL when view changes
  useEffect(() => {
    if (isAuthenticated) {
      const params = new URLSearchParams(window.location.search);
      if (params.get('view') !== currentView) {
        params.set('view', currentView);
        const newUrl = `${window.location.pathname}?${params.toString()}`;
        window.history.pushState({ path: newUrl }, '', newUrl);
      }
    }
  }, [currentView, isAuthenticated]);

  // Handle browser back/forward buttons
  useEffect(() => {
    const handlePopState = () => {
      const params = new URLSearchParams(window.location.search);
      const view = params.get('view') || 'dashboard';
      setCurrentView(view);
    };

    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);

  const fetchCurrentUser = async () => {
    try {
      const data = await api.getMe();
      setCurrentUser(data.user);
    } catch (error) {
      console.error('Failed to fetch user:', error);
      // If token is invalid, logout
      handleLogout();
    }
  };

  const handleLogin = () => {
    setIsAuthenticated(true);
    fetchCurrentUser();
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
    setCurrentUser(null);
    setCurrentView('dashboard');
    // Clear URL params on logout
    window.history.pushState({}, '', window.location.pathname);
  };

  if (!isAuthenticated) {
    return <Login onLogin={handleLogin} />;
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <nav className="bg-blue-600 text-white p-4 flex justify-between items-center">
        <div className="flex items-center gap-8">
          <h1 className="text-xl font-bold">RAN CRM Admin</h1>
          <div className="flex gap-4">
            <button
              onClick={() => setCurrentView('dashboard')}
              className={`px-3 py-1 rounded ${currentView === 'dashboard' ? 'bg-blue-800' : 'hover:bg-blue-700'}`}
            >
              Dashboard
            </button>
            <button
              onClick={() => setCurrentView('contacts')}
              className={`px-3 py-1 rounded ${currentView === 'contacts' ? 'bg-blue-800' : 'hover:bg-blue-700'}`}
            >
              Contacts
            </button>
            <button
              onClick={() => setCurrentView('call_logs')}
              className={`px-3 py-1 rounded ${currentView === 'call_logs' ? 'bg-blue-800' : 'hover:bg-blue-700'}`}
            >
              Call Logs
            </button>
          </div>
        </div>
        <div className="flex items-center gap-4">
          {currentUser && <span className="text-sm opacity-90">Hello, {currentUser.name}</span>}
          <button onClick={handleLogout} className="bg-red-500 px-4 py-2 rounded hover:bg-red-600">
            Logout
          </button>
        </div>
      </nav>
      <main className="p-8">
        {currentView === 'dashboard' && <Dashboard currentUser={currentUser} />}
        {currentView === 'contacts' && <Contacts />}
        {currentView === 'call_logs' && <CallLogs />}
      </main>
    </div>
  );
}

export default App;
