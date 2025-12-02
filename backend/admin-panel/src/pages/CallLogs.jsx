import React, { useState, useEffect } from 'react';
import { api } from '../services/api';

function CallLogs() {
    const [logs, setLogs] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [filter, setFilter] = useState('');
    const [deletingId, setDeletingId] = useState(null);

    const fetchLogs = async () => {
        try {
            setLoading(true);
            const data = await api.getAllCallLogs(page, 20); // Fetch 20 per page
            setLogs(data.data);
            setTotalPages(data.pagination.pages);
            setError(null);
        } catch (err) {
            setError('Failed to load call logs');
            console.error(err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchLogs();
    }, [page]);

    const handleDelete = async (id) => {
        if (!window.confirm('Are you sure you want to delete this call log?')) return;

        try {
            setDeletingId(id);
            await api.deleteCallLog(id);
            setLogs(logs.filter(log => log.id !== id));
        } catch (err) {
            alert('Failed to delete call log');
            console.error(err);
        } finally {
            setDeletingId(null);
        }
    };

    const filteredLogs = logs.filter(log => {
        const search = filter.toLowerCase();
        return (
            (log.contact_name && log.contact_name.toLowerCase().includes(search)) ||
            (log.contact_phone && log.contact_phone.includes(search)) ||
            (log.user_name && log.user_name.toLowerCase().includes(search))
        );
    });

    const formatDuration = (seconds) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const formatDirection = (direction) => {
        switch (direction) {
            case 'incoming': return '📥 Incoming';
            case 'outgoing': return '📤 Outgoing';
            case 'missed': return '📞 Missed';
            default: return direction;
        }
    };

    return (
        <div className="bg-white rounded-lg shadow p-6">
            <div className="flex justify-between items-center mb-6">
                <h2 className="text-2xl font-bold">Call Logs</h2>
                <div className="flex gap-4">
                    <input
                        type="text"
                        placeholder="Filter by name or phone..."
                        value={filter}
                        onChange={(e) => setFilter(e.target.value)}
                        className="border rounded px-3 py-2 w-64"
                    />
                    <button
                        onClick={fetchLogs}
                        className="bg-gray-100 px-4 py-2 rounded hover:bg-gray-200"
                    >
                        Refresh
                    </button>
                </div>
            </div>

            {error && (
                <div className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded mb-4">
                    {error}
                </div>
            )}

            {loading ? (
                <div className="text-center py-8">Loading...</div>
            ) : (
                <>
                    <div className="overflow-x-auto">
                        <table className="min-w-full table-auto">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Direction</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Contact</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">User</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Duration</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Time</th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {filteredLogs.map((log) => (
                                    <tr key={log.id} className="hover:bg-gray-50">
                                        <td className="px-6 py-4 whitespace-nowrap text-sm">
                                            <span className={`px-2 inline-flex text-xs leading-5 font-semibold rounded-full 
                        ${log.direction === 'missed' ? 'bg-red-100 text-red-800' :
                                                    log.direction === 'incoming' ? 'bg-green-100 text-green-800' :
                                                        'bg-blue-100 text-blue-800'}`}>
                                                {formatDirection(log.direction)}
                                            </span>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap">
                                            <div className="text-sm font-medium text-gray-900">{log.contact_name || 'Unknown'}</div>
                                            <div className="text-sm text-gray-500">{log.contact_phone}</div>
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {log.user_name || 'Unknown'}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {formatDuration(log.duration_seconds)}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-sm text-gray-500">
                                            {new Date(log.timestamp).toLocaleString()}
                                        </td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                            <button
                                                onClick={() => handleDelete(log.id)}
                                                disabled={deletingId === log.id}
                                                className="text-red-600 hover:text-red-900 disabled:opacity-50"
                                            >
                                                {deletingId === log.id ? 'Deleting...' : 'Delete'}
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>

                    {/* Pagination */}
                    <div className="flex justify-between items-center mt-4">
                        <div className="text-sm text-gray-700">
                            Page {page} of {totalPages}
                        </div>
                        <div className="flex gap-2">
                            <button
                                onClick={() => setPage(p => Math.max(1, p - 1))}
                                disabled={page === 1}
                                className="px-3 py-1 border rounded disabled:opacity-50"
                            >
                                Previous
                            </button>
                            <button
                                onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                                disabled={page === totalPages}
                                className="px-3 py-1 border rounded disabled:opacity-50"
                            >
                                Next
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

export default CallLogs;
