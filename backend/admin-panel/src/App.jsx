import React, { useState, useEffect } from 'react';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Contacts from './pages/Contacts';

function App() {
  const [isAuthenticated, setIsAuthenticated] = useState(false);
  const [currentView, setCurrentView] = useState('dashboard');

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (token) {
      setIsAuthenticated(true);
    }
  }, []);

  const handleLogin = () => {
    setIsAuthenticated(true);
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    setIsAuthenticated(false);
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
          </div>
        </div>
        <button onClick={handleLogout} className="bg-red-500 px-4 py-2 rounded hover:bg-red-600">
          Logout
        </button>
      </nav>
      <main className="p-8">
        {currentView === 'dashboard' ? <Dashboard /> : <Contacts />}
      </main>
    </div>
  );
}

export default App;
