import { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import api from '../lib/axios';
import { TrashIcon, PencilIcon, MagnifyingGlassIcon } from '@heroicons/react/24/outline';
import { useDebounce } from '../hooks/useDebounce';

interface Contact {
    id: string;
    name: string;
    phone_raw: string;
    phone_normalized: string;
    created_at: string;
    updated_at: string;
}

export default function UserContactsTab({ userId }: { userId: string }) {
    const [page, setPage] = useState(1);
    const [searchQuery, setSearchQuery] = useState('');
    const debouncedQuery = useDebounce(searchQuery, 300);
    const queryClient = useQueryClient();
    const limit = 10;

    const [editingContact, setEditingContact] = useState<Contact | null>(null);
    const [editName, setEditName] = useState('');
    const [editPhone, setEditPhone] = useState('');

    const { data, isLoading, error } = useQuery({
        queryKey: ['user-contacts', userId, debouncedQuery, page],
        queryFn: async () => {
            const params = new URLSearchParams({
                page: page.toString(),
                limit: limit.toString(),
                user_id: userId,
            });
            if (debouncedQuery.length >= 2) {
                params.append('q', debouncedQuery);
            }
            const response = await api.get(`/contacts?${params.toString()}`);
            return response.data;
        },
    });

    const deleteMutation = useMutation({
        mutationFn: async (id: string) => {
            await api.delete(`/contacts/${id}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['user-contacts', userId] });
        },
        onError: (error: any) => {
            alert(error.response?.data?.error || 'Failed to delete contact');
        },
    });

    const updateMutation = useMutation({
        mutationFn: async ({ id, name, phone_raw }: { id: string; name: string; phone_raw: string }) => {
            await api.put(`/contacts/${id}`, { name, phone_raw });
        },
        onSuccess: () => {
            setEditingContact(null);
            queryClient.invalidateQueries({ queryKey: ['user-contacts', userId] });
        },
        onError: (error: any) => {
            alert(error.response?.data?.error || 'Failed to update contact');
        },
    });

    const handleDelete = (id: string) => {
        if (window.confirm('Are you sure you want to delete this contact?')) {
            deleteMutation.mutate(id);
        }
    };

    const handleEditStart = (contact: Contact) => {
        setEditingContact(contact);
        setEditName(contact.name);
        setEditPhone(contact.phone_raw);
    };

    const handleEditSave = () => {
        if (!editingContact) return;
        updateMutation.mutate({
            id: editingContact.id,
            name: editName,
            phone_raw: editPhone,
        });
    };

    return (
        <div className="mt-4">
            <div className="flex justify-between items-center mb-4">
                <div className="relative flex-1 max-w-sm">
                    <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                        <MagnifyingGlassIcon className="h-5 w-5 text-gray-400" aria-hidden="true" />
                    </div>
                    <input
                        type="text"
                        className="block w-full rounded-md border-0 py-1.5 pl-10 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                        placeholder="Search contacts..."
                        value={searchQuery}
                        onChange={(e) => {
                            setSearchQuery(e.target.value);
                            setPage(1);
                        }}
                    />
                </div>
            </div>

            {error ? (
                <div className="text-red-600 py-4 text-center">Failed to load contacts</div>
            ) : isLoading ? (
                <div className="text-center py-4 text-gray-500">Loading contacts...</div>
            ) : data?.items?.length === 0 ? (
                <div className="text-center py-8 text-gray-500 bg-gray-50 rounded-lg border border-gray-200 border-dashed">
                    No contacts found.
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
                                                <th scope="col" className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6">Name</th>
                                                <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Phone</th>
                                                <th scope="col" className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Updated At</th>
                                                <th scope="col" className="relative py-3.5 pl-3 pr-4 sm:pr-6"><span className="sr-only">Actions</span></th>
                                            </tr>
                                        </thead>
                                        <tbody className="divide-y divide-gray-200 bg-white">
                                            {data?.items.map((contact: Contact) => (
                                                <tr key={contact.id}>
                                                    <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6">
                                                        {editingContact?.id === contact.id ? (
                                                            <input type="text" value={editName} onChange={e => setEditName(e.target.value)} className="border p-1 rounded w-full" />
                                                        ) : contact.name}
                                                    </td>
                                                    <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                        {editingContact?.id === contact.id ? (
                                                            <input type="text" value={editPhone} onChange={e => setEditPhone(e.target.value)} className="border p-1 rounded w-full" />
                                                        ) : contact.phone_raw}
                                                    </td>
                                                    <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">
                                                        {new Date(contact.updated_at).toLocaleDateString()}
                                                    </td>
                                                    <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-6 whitespace-nowrap">
                                                        {editingContact?.id === contact.id ? (
                                                            <>
                                                                <button onClick={handleEditSave} className="text-green-600 hover:text-green-900 mr-4">Save</button>
                                                                <button onClick={() => setEditingContact(null)} className="text-gray-600 hover:text-gray-900">Cancel</button>
                                                            </>
                                                        ) : (
                                                            <div className="flex justify-end gap-3">
                                                                <button
                                                                    onClick={() => handleEditStart(contact)}
                                                                    className="text-blue-600 hover:text-blue-900"
                                                                    title="Edit Contact"
                                                                >
                                                                    <PencilIcon className="h-5 w-5" />
                                                                </button>
                                                                <button
                                                                    onClick={() => handleDelete(contact.id)}
                                                                    className="text-red-600 hover:text-red-900"
                                                                    disabled={deleteMutation.isPending}
                                                                    title="Delete Contact"
                                                                >
                                                                    <TrashIcon className="h-5 w-5" />
                                                                </button>
                                                            </div>
                                                        )}
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>

                    {data?.totalPages > 1 && (
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
