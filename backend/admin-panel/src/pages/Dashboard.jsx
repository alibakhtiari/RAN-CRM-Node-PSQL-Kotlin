import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function Dashboard({ currentUser }) {
    const [users, setUsers] = useState([]);
    const [syncAudits, setSyncAudits] = useState([]);
    const [loading, setLoading] = useState(true);

    // Registration Modal State
    const [showRegisterModal, setShowRegisterModal] = useState(false);
    const [newUser, setNewUser] = useState({ username: '', name: '', password: '', is_admin: false });
    const [registerError, setRegisterError] = useState('');

    const loadData = async () => {
        setLoading(true);
        try {
            const [usersData, syncData] = await Promise.all([
                api.getUsers(),
                api.getSyncAudit()
            ]);
            setUsers(usersData.data);
            setSyncAudits(syncData.sync_audits);
        } catch (error) {
            console.error(error);
            // Handle error
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadData();
    }, []);

    const handleDeleteUser = async (id) => {
        if (!confirm('Are you sure?')) return;
        try {
            await api.deleteUser(id);
            loadData();
        } catch (error) {
            alert(error.message);
        }
    };

    const handleRegister = async (e) => {
        e.preventDefault();
        setRegisterError('');
        try {
            await api.createUser(newUser);
            setShowRegisterModal(false);
            setNewUser({ username: '', name: '', password: '', is_admin: false });
            loadData();
        } catch (error) {
            setRegisterError(error.message);
        }
    };

    // Merge data
    const mergedData = users.map(user => {
        const audit = syncAudits.find(a => a.username === user.username) || {};
        return { ...user, ...audit };
    });

    console.log('Dashboard Render. CurrentUser:', currentUser);

    if (loading) return <div className="p-8">Loading...</div>;

    return (
        <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="p-6 border-b border-gray-200 flex justify-between items-center">
                <h3 className="text-lg font-medium text-gray-900">User Management & Sync Status</h3>
                <div className="flex gap-4">
                    {currentUser?.is_admin && (
                        <button
                            onClick={() => setShowRegisterModal(true)}
                            className="bg-green-600 text-white px-4 py-2 rounded hover:bg-green-700 text-sm"
                        >
                            Register New User
                        </button>
                    )}
                    <button onClick={loadData} className="text-blue-600 hover:text-blue-800">Refresh</button>
                </div>
            </div>
            <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200">
                    <thead className="bg-gray-50">
                        <tr>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Role</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Contacts Sync</th>
                            <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Call Logs Sync</th>
                            <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                        </tr>
                    </thead>
                    <tbody className="bg-white divide-y divide-gray-200">
                        {mergedData.map(user => (
                            <tr key={user.id}>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <div className="flex items-center">
                                        <div>
                                            <div className="text-sm font-medium text-gray-900">{user.name}</div>
                                            <div className="text-sm text-gray-500">{user.username}</div>
                                        </div>
                                    </div>
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    {user.is_admin ? (
                                        <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-red-100 text-red-800">
                                            Admin
                                        </span>
                                    ) : (
                                        <span className="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-green-100 text-green-800">
                                            User
                                        </span>
                                    )}
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <SyncStatus
                                        status={user.contacts_status}
                                        lastSync={user.contacts_last_sync}
                                        count={user.synced_contacts}
                                        error={user.contacts_error}
                                    />
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap">
                                    <SyncStatus
                                        status={user.calls_status}
                                        lastSync={user.calls_last_sync}
                                        count={user.synced_calls}
                                        error={user.calls_error}
                                    />
                                </td>
                                <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                    <button
                                        onClick={() => handleDeleteUser(user.id)}
                                        className="text-red-600 hover:text-red-900"
                                    >
                                        Delete
                                    </button>
                                </td>
                            </tr>
                        ))}
                    </tbody>
                </table>
            </div>

            {/* Registration Modal */}
            {showRegisterModal && (
                <div className="fixed inset-0 bg-gray-600 bg-opacity-50 flex items-center justify-center">
                    <div className="bg-white p-6 rounded-lg shadow-xl w-96">
                        <h3 className="text-lg font-bold mb-4">Register New User</h3>
                        {registerError && <div className="bg-red-100 text-red-700 p-2 rounded mb-4 text-sm">{registerError}</div>}
                        <form onSubmit={handleRegister}>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700">Name</label>
                                <input
                                    type="text"
                                    required
                                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"
                                    value={newUser.name}
                                    onChange={e => setNewUser({ ...newUser, name: e.target.value })}
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700">Username</label>
                                <input
                                    type="text"
                                    required
                                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"
                                    value={newUser.username}
                                    onChange={e => setNewUser({ ...newUser, username: e.target.value })}
                                />
                            </div>
                            <div className="mb-4">
                                <label className="block text-sm font-medium text-gray-700">Password</label>
                                <input
                                    type="password"
                                    required
                                    className="mt-1 block w-full border border-gray-300 rounded-md shadow-sm p-2"
                                    value={newUser.password}
                                    onChange={e => setNewUser({ ...newUser, password: e.target.value })}
                                />
                            </div>
                            <div className="mb-6 flex items-center">
                                <input
                                    type="checkbox"
                                    id="isAdmin"
                                    className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                                    checked={newUser.is_admin}
                                    onChange={e => setNewUser({ ...newUser, is_admin: e.target.checked })}
                                />
                                <label htmlFor="isAdmin" className="ml-2 block text-sm text-gray-900">
                                    Is Admin?
                                </label>
                            </div>
                            <div className="flex justify-end gap-2">
                                <button
                                    type="button"
                                    onClick={() => setShowRegisterModal(false)}
                                    className="bg-gray-200 text-gray-700 px-4 py-2 rounded hover:bg-gray-300"
                                >
                                    Cancel
                                </button>
                                <button
                                    type="submit"
                                    className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                                >
                                    Register
                                </button>
                            </div>
                        </form>
                    </div>
                </div>
            )}
        </div>
    );
}

function SyncStatus({ status, lastSync, count, error }) {
    if (!status) return <span className="text-gray-400 text-sm">Never Synced</span>;

    const color = status === 'success' ? 'text-green-600' : status === 'error' ? 'text-red-600' : 'text-gray-500';
    const date = lastSync ? new Date(lastSync).toLocaleString() : '';

    return (
        <div className="text-sm">
            <div className={`font-medium ${color} uppercase flex items-center gap-1`}>
                <span className={`w-2 h-2 rounded-full ${status === 'success' ? 'bg-green-500' : status === 'error' ? 'bg-red-500' : 'bg-gray-500'}`}></span>
                {status}
            </div>
            <div className="text-gray-500 text-xs">{date}</div>
            {count !== undefined && <div className="text-gray-500 text-xs">{count} records</div>}
            {error && <div className="text-red-500 text-xs max-w-xs truncate" title={error}>{error}</div>}
        </div>
    );
}
