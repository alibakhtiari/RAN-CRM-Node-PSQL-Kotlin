import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../lib/axios';
import { TrashIcon, ArrowPathIcon, PhoneIcon, PhoneArrowUpRightIcon, PhoneArrowDownLeftIcon, PhoneXMarkIcon } from '@heroicons/react/24/outline';
import { useDebounce } from '../hooks/useDebounce';

interface CallLog {
    id: string;
    direction: 'incoming' | 'outgoing' | 'missed' | 'rejected' | 'blocked' | 'voicemail';
    contact_name: string;
    contact_phone: string;
    phone_number: string;
    user_name: string;
    duration_seconds: number;
    timestamp: string;
}

export default function CallLogs() {
    const queryClient = useQueryClient();
    const [page, setPage] = useState(1);
    const [filter, setFilter] = useState('');
    const debouncedFilter = useDebounce(filter, 300);

    // Fetch Call Logs
    const { data, isLoading, isError, refetch } = useQuery({
        queryKey: ['callLogs', page], // Note: filter is client-side for now as per legacy code
        queryFn: async () => {
            const res = await api.get(`/calls?page=${page}&limit=20`);
            return res.data;
        },
        placeholderData: (previousData) => previousData,
    });

    const deleteMutation = useMutation({
        mutationFn: async (id: string) => {
            await api.delete(`/calls/${id}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['callLogs'] });
        }
    });

    const handleDelete = (id: string) => {
        if (window.confirm('Are you sure you want to delete this call log?')) {
            deleteMutation.mutate(id);
        }
    };

    const formatDuration = (seconds: number) => {
        const mins = Math.floor(seconds / 60);
        const secs = seconds % 60;
        return `${mins}:${secs.toString().padStart(2, '0')}`;
    };

    const getDirectionIcon = (direction: string) => {
        switch (direction) {
            case 'incoming': return <PhoneArrowDownLeftIcon className="h-4 w-4 text-green-600" />;
            case 'outgoing': return <PhoneArrowUpRightIcon className="h-4 w-4 text-blue-600" />;
            case 'missed': return <PhoneXMarkIcon className="h-4 w-4 text-red-600" />;
            default: return <PhoneIcon className="h-4 w-4 text-gray-500" />;
        }
    };

    const getDirectionLabel = (direction: string) => {
        switch (direction) {
            case 'incoming': return 'Incoming';
            case 'outgoing': return 'Outgoing';
            case 'missed': return 'Missed';
            default: return direction;
        }
    };

    // Client-side filtering to match legacy behavior
    // Ideally this should be server-side
    const filteredLogs = data?.data?.filter((log: CallLog) => {
        if (!debouncedFilter) return true;
        const search = debouncedFilter.toLowerCase();
        return (
            (log.contact_name && log.contact_name.toLowerCase().includes(search)) ||
            (log.contact_phone && log.contact_phone.includes(search)) ||
            (log.phone_number && log.phone_number.includes(search)) ||
            (log.user_name && log.user_name.toLowerCase().includes(search))
        );
    }) || [];

    return (
        <div className="space-y-6">
            <div className="md:flex md:items-center md:justify-between">
                <h2 className="text-2xl font-bold text-gray-900">Call Logs</h2>
                <div className="mt-4 flex gap-4 md:mt-0">
                    <input
                        type="text"
                        name="call-log-filter"
                        id="call-log-filter"
                        placeholder="Filter by name or phone..."
                        value={filter}
                        onChange={(e) => setFilter(e.target.value)}
                        className="block w-64 rounded-md border-0 py-1.5 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                    />
                    <button
                        onClick={() => refetch()}
                        className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                    >
                        <ArrowPathIcon className="-ml-0.5 mr-1.5 h-5 w-5 text-gray-400" />
                        Refresh
                    </button>
                </div>
            </div>

            <div className="bg-white shadow-sm ring-1 ring-gray-900/5 sm:rounded-xl">
                {isLoading ? (
                    <div className="p-8 text-center text-gray-500">Loading call logs...</div>
                ) : isError ? (
                    <div className="p-8 text-center text-red-500">Failed to load call logs.</div>
                ) : (
                    <>
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-300">
                                <thead>
                                    <tr>
                                        <th className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6">Direction</th>
                                        <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Contact</th>
                                        <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">User</th>
                                        <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Duration</th>
                                        <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Time</th>
                                        <th className="relative py-3.5 pl-3 pr-4 sm:pr-6"><span className="sr-only">Actions</span></th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-200">
                                    {filteredLogs.map((log: CallLog) => (
                                        <tr key={log.id}>
                                            <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6">
                                                <div className="flex items-center gap-2">
                                                    {getDirectionIcon(log.direction)}
                                                    <span className="capitalize">{getDirectionLabel(log.direction)}</span>
                                                </div>
                                            </td>
                                            <td className="whitespace-nowrap px-3 py-4 text-sm">
                                                <div className="font-medium text-gray-900">{log.contact_name || 'Unknown'}</div>
                                                <div className="text-gray-500">{log.phone_number || log.contact_phone}</div>
                                            </td>
                                            <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                {log.user_name || 'Unknown'}
                                            </td>
                                            <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                {formatDuration(log.duration_seconds)}
                                            </td>
                                            <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                {new Date(log.timestamp).toLocaleString()}
                                            </td>
                                            <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-6">
                                                <button
                                                    onClick={() => handleDelete(log.id)}
                                                    className="text-red-600 hover:text-red-900"
                                                >
                                                    <TrashIcon className="h-4 w-4" />
                                                    <span className="sr-only">Delete</span>
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                    {filteredLogs.length === 0 && (
                                        <tr>
                                            <td colSpan={6} className="px-6 py-12 text-center text-gray-500">
                                                No call logs found.
                                            </td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        </div>

                        <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6 rounded-b-xl">
                            <div className="flex flex-1 justify-between sm:hidden">
                                <button
                                    onClick={() => setPage(p => Math.max(1, p - 1))}
                                    disabled={page === 1}
                                    className="relative inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                                >
                                    Previous
                                </button>
                                <button
                                    onClick={() => setPage(p => p + 1)}
                                    disabled={page >= (data?.pagination?.totalPages || 1)}
                                    className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                                >
                                    Next
                                </button>
                            </div>
                            <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                                <div>
                                    <p className="text-sm text-gray-700">
                                        Page <span className="font-medium">{page}</span> of <span className="font-medium">{data?.pagination?.totalPages || 1}</span>
                                    </p>
                                </div>
                                <div>
                                    <nav className="isolate inline-flex -space-x-px rounded-md shadow-sm" aria-label="Pagination">
                                        <button
                                            onClick={() => setPage(p => Math.max(1, p - 1))}
                                            disabled={page === 1}
                                            className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50"
                                        >
                                            <span className="sr-only">Previous</span>
                                            Previous
                                        </button>
                                        <button
                                            onClick={() => setPage(p => p + 1)}
                                            disabled={page >= (data?.pagination?.totalPages || 1)}
                                            className="relative inline-flex items-center rounded-r-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50"
                                        >
                                            <span className="sr-only">Next</span>
                                            Next
                                        </button>
                                    </nav>
                                </div>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}
