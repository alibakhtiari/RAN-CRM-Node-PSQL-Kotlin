import { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import api from '../lib/axios';

interface CallLog {
    id: string;
    contact_name: string;
    contact_phone: string;
    phone_number: string;
    direction: 'incoming' | 'outgoing' | 'missed';
    duration_seconds: number;
    timestamp: string;
}

export default function UserCallLogsTab({ userId }: { userId: string }) {
    const [page, setPage] = useState(1);
    const limit = 10;

    const { data, isLoading, error } = useQuery({
        queryKey: ['user-calls', userId, page],
        queryFn: async () => {
            const response = await api.get(`/calls?page=${page}&limit=${limit}&user_id=${userId}`);
            return response.data;
        },
    });

    const formatDuration = (seconds: number) => {
        const h = Math.floor(seconds / 3600);
        const m = Math.floor((seconds % 3600) / 60);
        const s = seconds % 60;
        if (h > 0) return `${h}h ${m}m ${s}s`;
        if (m > 0) return `${m}m ${s}s`;
        return `${s}s`;
    };

    return (
        <div className="mt-4">
            {error ? (
                <div className="text-red-600 py-4 text-center">Failed to load call logs</div>
            ) : isLoading ? (
                <div className="text-center py-4 text-gray-500">Loading call logs...</div>
            ) : !data?.items?.length ? (
                <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-lg border border-gray-200 border-dashed">
                    No call logs found.
                </div>
            ) : (
                <>
                    <div className="mt-4 flow-root">
                        <div className="-mx-4 -my-2 overflow-x-auto sm:-mx-6 lg:-mx-8">
                            <div className="inline-block min-w-full py-2 align-middle sm:px-6 lg:px-8">
                                <div className="overflow-hidden shadow-sm ring-1 ring-black ring-opacity-5 sm:rounded-lg">
                                    <table className="min-w-full divide-y divide-gray-300">
                                        <thead className="bg-gray-50">
                                            <tr>
                                                <th scope="col" className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6">Contact / Phone</th>
                                                <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Direction</th>
                                                <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Duration</th>
                                                <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Date</th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-200 bg-white">
                                            {data?.items?.map((call: CallLog) => (
                                                <tr key={call.id}>
                                                    <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6">
                                                        {call.contact_name || 'Unknown'}
                                                        <div className="font-normal text-gray-500">{call.contact_phone || call.phone_number}</div>
                                                    </td>
                                                    <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                        <span className={`inline-flex items-center rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset ${call.direction === 'missed' ? 'bg-red-50 text-red-700 ring-red-600/10' :
                                                            call.direction === 'incoming' ? 'bg-blue-50 text-blue-700 ring-blue-600/10' :
                                                                'bg-green-50 text-green-700 ring-green-600/10'
                                                            }`}>
                                                            {call.direction.charAt(0).toUpperCase() + call.direction.slice(1)}
                                                        </span>
                                                    </td>
                                                    <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                        {formatDuration(call.duration_seconds)}
                                                    </td>
                                                    <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                        {new Date(call.timestamp).toLocaleString()}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>

                    {(data?.totalPages || 0) > 1 && (
                        <div className="mt-4 flex items-center justify-between">
                            <button
                                onClick={() => setPage((p) => Math.max(1, p - 1))}
                                disabled={page === 1}
                                className="rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50"
                            >
                                Previous
                            </button>
                            <span className="text-sm text-gray-700">
                                Page <span className="font-semibold">{page}</span> of <span className="font-semibold">{data.totalPages}</span>
                            </span>
                            <button
                                onClick={() => setPage((p) => Math.min(data.totalPages, p + 1))}
                                disabled={page === data.totalPages}
                                className="rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 disabled:opacity-50"
                            >
                                Next
                            </button>
                        </div>
                    )}
                </>
            )}
        </div>
    );
}
