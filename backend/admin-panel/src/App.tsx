import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Routes, Route, Navigate, Outlet } from 'react-router-dom';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Contacts from './pages/Contacts';
import CallLogs from './pages/CallLogs'; // Ensuring checking existance
import Layout from './components/Layout'; // We will create this
import { AuthProvider, useAuth } from './context/AuthContext'; // We will create this

const queryClient = new QueryClient();

function ProtectedRoute() {
    const { isAuthenticated, isLoading } = useAuth();

    if (isLoading) return <div className="flex h-screen items-center justify-center">Loading...</div>;

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
}

export default function App() {
    return (
        <QueryClientProvider client={queryClient}>
            <AuthProvider>
                <BrowserRouter basename="/admin">
                    <Routes>
                        <Route path="/login" element={<Login />} />

                        <Route element={<ProtectedRoute />}>
                            <Route element={<Layout />}>
                                <Route path="/" element={<Navigate to="/dashboard" replace />} />
                                <Route path="/dashboard" element={<Dashboard />} />
                                <Route path="/contacts" element={<Contacts />} />
                                <Route path="/call-logs" element={<CallLogs />} />
                            </Route>
                        </Route>

                        <Route path="*" element={<Navigate to="/dashboard" replace />} />
                    </Routes>
                </BrowserRouter>
            </AuthProvider>
        </QueryClientProvider>
    );
}
