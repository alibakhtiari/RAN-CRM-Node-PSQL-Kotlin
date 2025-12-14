import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../lib/axios';
import { useAuth } from '../context/AuthContext';
import RegisterUserModal from '../components/RegisterUserModal';
import { TrashIcon, ArrowPathIcon, PencilIcon } from '@heroicons/react/24/outline';
import EditUserModal from '../components/EditUserModal';

interface User {
    id: string;
    name: string;
    username: string;
    is_admin: boolean;
}

interface SyncAudit {
    username: string; // Linking key
    contacts_status?: string;
    contacts_last_sync?: string;
    synced_contacts?: number;
    contacts_error?: string;
    calls_status?: string;
    calls_last_sync?: string;
    synced_calls?: number;
    calls_error?: string;
}

export default function Dashboard() {
    const { user: currentUser } = useAuth();
    const queryClient = useQueryClient();
    const [isRegisterModalOpen, setIsRegisterModalOpen] = useState(false);
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [selectedUser, setSelectedUser] = useState<User | null>(null);

    // Fetch Users
    const { data: users = [], isLoading: usersLoading } = useQuery<User[]>({
        queryKey: ['users'],
        queryFn: async () => {
            const res = await api.get('/users');
            return res.data.data || [];
        },
        // Remove initialData to avoid type conflicts if queryFn return type differs,
        // but explicit default in destructuring handles it.
    });

    // Fetch Sync Audit
    const { data: syncAudits = [], isLoading: auditsLoading } = useQuery<SyncAudit[]>({
        queryKey: ['syncAudits'],
        queryFn: async () => {
            const res = await api.get('/sync-audit');
            return res.data.sync_audits || [];
        },
    });

    const deleteUserMutation = useMutation({
        mutationFn: async (id: string) => {
            await api.delete(`/users/${id}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['users'] });
        }
    });

    const handleDeleteUser = (id: string) => {
        if (window.confirm('Are you sure you want to delete this user?')) {
            deleteUserMutation.mutate(id);
        }
    };

    const handleEditUser = (user: User) => {
        setSelectedUser(user);
        setIsEditModalOpen(true);
    };

    const reloadData = () => {
        queryClient.invalidateQueries({ queryKey: ['users'] });
        queryClient.invalidateQueries({ queryKey: ['syncAudits'] });
    };

    const isLoading = usersLoading || auditsLoading;

    // Merge data
    const mergedData = users?.map(u => {
        const audit = syncAudits?.find((a: any) => a.username === u.username) || {}; // Assuming syncAudits returns structure with username
        // Or maybe syncAudits returns { sync_audits: [] }?
        // Old code said: setSyncAudits(syncData.sync_audits);
        // So fetch function needs adjustment if the backend returns { sync_audits: ... }
        return { ...u, ...audit };
    }) || [];

    return (
        <div className="space-y-6">
            <div className="md:flex md:items-center md:justify-between">
                <div className="min-w-0 flex-1">
                    <h2 className="text-2xl font-bold leading-7 text-gray-900 sm:truncate sm:text-3xl sm:tracking-tight">
                        User Management & Sync Status
                    </h2>
                </div>
                <div className="mt-4 flex md:ml-4 md:mt-0">
                    <button
                        type="button"
                        onClick={() => reloadData()}
                        className="mr-3 inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                    >
                        <ArrowPathIcon className="-ml-0.5 mr-1.5 h-5 w-5 text-gray-400" aria-hidden="true" />
                        Refresh
                    </button>
                    {currentUser?.is_admin && (
                        <button
                            type="button"
                            onClick={() => setIsRegisterModalOpen(true)}
                            className="inline-flex items-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-700 focus-visible:outline-solid focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600"
                        >
                            Register New User
                        </button>
                    )}
                </div>
            </div>

            <div className="bg-white shadow-xs ring-1 ring-gray-900/5 sm:rounded-xl md:col-span-2">
                {isLoading ? (
                    <div className="p-8 text-center text-gray-500">Loading data...</div>
                ) : (
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-300">
                            <thead>
                                <tr>
                                    <th scope="col" className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6">User</th>
                                    <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Role</th>
                                    <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Contacts Sync</th>
                                    <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Call Logs Sync</th>
                                    <th scope="col" className="relative py-3.5 pl-3 pr-4 sm:pr-6">
                                        <span className="sr-only">Actions</span>
                                    </th>
                                </tr>
                            </thead>
                            <tbody className="divide-y divide-gray-200">
                                {mergedData.map((user: any) => (
                                    <tr key={user.id}>
                                        <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6">
                                            <div>
                                                <div className="font-medium text-gray-900">{user.name}</div>
                                                <div className="text-gray-500">{user.username}</div>
                                            </div>
                                        </td>
                                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                            <span className={`inline-flex items-center rounded-md px-2 py-1 text-xs font-medium ring-1 ring-inset ${user.is_admin ? 'bg-red-50 text-red-700 ring-red-600/10' : 'bg-green-50 text-green-700 ring-green-600/10'}`}>
                                                {user.is_admin ? 'Admin' : 'User'}
                                            </span>
                                        </td>
                                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                            <SyncStatus
                                                status={user.contacts_status}
                                                lastSync={user.contacts_last_sync}
                                                count={user.synced_contacts}
                                                error={user.contacts_error}
                                            />
                                        </td>
                                        <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                            <SyncStatus
                                                status={user.calls_status}
                                                lastSync={user.calls_last_sync}
                                                count={user.synced_calls}
                                                error={user.calls_error}
                                            />
                                        </td>
                                        <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-6">
                                            <button
                                                onClick={() => handleDeleteUser(user.id)}
                                                className="text-red-600 hover:text-red-900 flex items-center justify-end gap-1 ml-auto"
                                            >
                                                <TrashIcon className="h-4 w-4" />
                                                Delete
                                            </button>
                                            <button
                                                onClick={() => handleEditUser(user)}
                                                className="text-blue-600 hover:text-blue-900 flex items-center justify-end gap-1 ml-auto mt-2"
                                            >
                                                <PencilIcon className="h-4 w-4" />
                                                Edit
                                            </button>
                                        </td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    </div>
                )}
            </div>

            <RegisterUserModal
                isOpen={isRegisterModalOpen}
                onClose={() => setIsRegisterModalOpen(false)}
                onSuccess={() => reloadData()}
            />

            <EditUserModal
                isOpen={isEditModalOpen}
                onClose={() => setIsEditModalOpen(false)}
                onSuccess={() => reloadData()}
                user={selectedUser}
            />
        </div>
    );
}

function SyncStatus({ status, lastSync, count, error }: any) {
    if (!status) return <span className="text-gray-400 text-xs italic">Never Synced</span>;

    const isSuccess = status === 'success';
    const isError = status === 'error';
    const date = lastSync ? new Date(lastSync).toLocaleString() : '';

    return (
        <div className="flex flex-col gap-0.5">
            <div className="flex items-center gap-1.5">
                <div className={`h-2 w-2 rounded-full ${isSuccess ? 'bg-green-500' : isError ? 'bg-red-500' : 'bg-yellow-500'}`} />
                <span className={`text-xs font-semibold uppercase ${isSuccess ? 'text-green-700' : isError ? 'text-red-700' : 'text-yellow-700'}`}>
                    {status}
                </span>
            </div>
            {date && <span className="text-xs text-gray-500">{date}</span>}
            {count !== undefined && <span className="text-xs text-gray-600">{count} records</span>}
            {error && <span className="text-xs text-red-500 max-w-[150px] truncate" title={error}>{error}</span>}
        </div>
    );
}
