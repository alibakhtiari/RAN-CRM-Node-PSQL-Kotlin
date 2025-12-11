import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

export default function ContactModal({ contact, onClose, onUpdate }) {
    const [formData, setFormData] = useState({
        name: contact.name,
        phone_raw: contact.phone_raw,
        created_by: contact.created_by
    });
    const [callLogs, setCallLogs] = useState([]);
    const [loadingCalls, setLoadingCalls] = useState(false);
    const [users, setUsers] = useState([]);

    useEffect(() => {
        loadCalls();
        loadUsers();
    }, []);

    const loadCalls = async () => {
        setLoadingCalls(true);
        try {
            const data = await api.getCallLogs(contact.id);
            setCallLogs(data.data);
        } catch (error) {
            console.error(error);
        } finally {
            setLoadingCalls(false);
        }
    };

    const loadUsers = async () => {
        try {
            const data = await api.getUsers();
            setUsers(data.data);
        } catch (error) {
            console.error(error);
        }
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        try {
            await api.updateContact(contact.id, formData);
            onUpdate();
            onClose();
        } catch (error) {
            alert(error.message);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-lg w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
                <div className="p-4 border-b border-gray-200 flex justify-between items-center bg-gray-50">
                    <h3 className="text-lg font-medium text-gray-900">Contact Details</h3>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700 text-xl">&times;</button>
                </div>

                <div className="p-6 overflow-y-auto flex-1">
                    {/* Edit Form Section */}
                    <div className="mb-8">
                        <h4 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">Edit Information</h4>
                        <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-gray-700 text-sm font-bold mb-2">Name</label>
                                <input
                                    type="text"
                                    value={formData.name}
                                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                                    className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>
                            <div>
                                <label className="block text-gray-700 text-sm font-bold mb-2">Phone</label>
                                <input
                                    type="text"
                                    value={formData.phone_raw}
                                    onChange={(e) => setFormData({ ...formData, phone_raw: e.target.value })}
                                    className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                />
                            </div>
                            <div className="md:col-span-2">
                                <label className="block text-gray-700 text-sm font-bold mb-2">Created By (Owner)</label>
                                <select
                                    value={formData.created_by}
                                    onChange={(e) => setFormData({ ...formData, created_by: e.target.value })}
                                    className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                                >
                                    {users.map(user => (
                                        <option key={user.id} value={user.id}>{user.name} ({user.username})</option>
                                    ))}
                                </select>
                            </div>
                            <div className="md:col-span-2 flex justify-end gap-2 mt-2">
                                <button type="button" onClick={onClose} className="px-4 py-2 border rounded hover:bg-gray-50">Cancel</button>
                                <button type="submit" className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">Save Changes</button>
                            </div>
                        </form>
                    </div>

                    {/* Call Logs Section */}
                    <div>
                        <h4 className="text-sm font-semibold text-gray-500 uppercase tracking-wider mb-4">Call Logs</h4>
                        <div className="border rounded-lg overflow-hidden">
                            <div className="max-h-64 overflow-y-auto bg-gray-50">
                                {loadingCalls ? (
                                    <div className="text-center py-8 text-gray-500">Loading call logs...</div>
                                ) : callLogs.length === 0 ? (
                                    <div className="text-center py-8 text-gray-500">No call logs found</div>
                                ) : (
                                    <table className="min-w-full divide-y divide-gray-200">
                                        <thead className="bg-gray-100 sticky top-0">
                                            <tr>
                                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Direction</th>
                                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">User</th>
                                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Duration</th>
                                                <th className="px-4 py-2 text-left text-xs font-medium text-gray-500 uppercase">Time</th>
                                            </tr>
                                        </thead>
                                        <tbody className="bg-white divide-y divide-gray-200">
                                            {callLogs.map(call => (
                                                <tr key={call.id}>
                                                    <td className="px-4 py-2 whitespace-nowrap">
                                                        <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                              ${call.direction === 'missed' ? 'bg-red-100 text-red-800' :
                                                                call.direction === 'incoming' ? 'bg-green-100 text-green-800' :
                                                                    'bg-blue-100 text-blue-800'}`}>
                                                            {call.direction}
                                                        </span>
                                                    </td>
                                                    <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-900">
                                                        {call.user_name || 'Unknown'}
                                                    </td>
                                                    <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-500">
                                                        {call.duration_seconds}s
                                                    </td>
                                                    <td className="px-4 py-2 whitespace-nowrap text-sm text-gray-500">
                                                        {new Date(call.timestamp).toLocaleString()}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                )}
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
