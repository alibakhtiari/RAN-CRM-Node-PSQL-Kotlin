import { useState, useEffect } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useSearchParams } from 'react-router-dom';
import api from '../lib/axios';
import { useAuth } from '../context/AuthContext';
import ContactModal from '../components/ContactModal';
import { TrashIcon, PencilIcon, DocumentArrowDownIcon, DocumentArrowUpIcon, MagnifyingGlassIcon } from '@heroicons/react/24/outline';
import { useDebounce } from '../hooks/useDebounce';
import ImportContactsModal from '../components/ImportContactsModal';

interface Contact {
    id: string;
    name: string;
    phone_raw: string;
    phone_normalized: string;
    created_by: string;
    creator_name: string;
    created_at: string;
    updated_at: string;
}

export default function Contacts() {
    const { user: currentUser } = useAuth();
    const queryClient = useQueryClient();
    const [searchParams, setSearchParams] = useSearchParams();

    const page = parseInt(searchParams.get('page') || '1');
    const searchInput = searchParams.get('search') || '';
    const [search, setSearch] = useState(searchInput);
    const debouncedSearch = useDebounce(search, 500);

    const [selectedContact, setSelectedContact] = useState<Contact | null>(null);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [isImportModalOpen, setIsImportModalOpen] = useState(false);

    // Sync state with URL
    useEffect(() => {
        const params = new URLSearchParams(searchParams);
        if (debouncedSearch) {
            params.set('search', debouncedSearch);
            params.set('page', '1'); // Reset to page 1 on search
        } else {
            params.delete('search');
        }
        setSearchParams(params);
    }, [debouncedSearch]);


    // Fetch Contacts
    const { data, isLoading } = useQuery({
        queryKey: ['contacts', page, debouncedSearch],
        queryFn: async () => {
            const params = new URLSearchParams({
                page: page.toString(),
                limit: '10'
            });
            if (debouncedSearch) params.append('q', debouncedSearch);

            const endpoint = debouncedSearch ? '/contacts/search' : '/contacts';
            const res = await api.get(`${endpoint}?${params.toString()}`);
            return res.data;
        },
        placeholderData: (previousData) => previousData, // Keep previous data while fetching
    });

    // Fetch Users for Admin dropdown in Modal
    const { data: users } = useQuery({
        queryKey: ['users'],
        queryFn: async () => {
            if (!currentUser?.is_admin) return [];
            const res = await api.get('/users');
            return res.data.data || [];
        },
        enabled: !!currentUser?.is_admin
    });

    const deleteMutation = useMutation({
        mutationFn: async (id: string) => {
            await api.delete(`/contacts/${id}`);
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ['contacts'] });
        },
        onError: (error: any) => {
            alert(error.response?.data?.error || 'Failed to delete contact');
        }
    });

    // Actions
    const handleEdit = (contact: Contact) => {
        setSelectedContact(contact);
        setIsModalOpen(true);
    };

    const handleCreate = () => {
        setSelectedContact(null);
        setIsModalOpen(true);
    };

    const handleDelete = (id: string) => {
        if (window.confirm('Are you sure you want to delete this contact?')) {
            deleteMutation.mutate(id);
        }
    };

    const handleExport = async () => {
        try {
            const res = await api.get('/contacts/export', { responseType: 'blob' });
            const url = window.URL.createObjectURL(new Blob([res.data]));
            const link = document.createElement('a');
            link.href = url;
            link.setAttribute('download', 'contacts.csv');
            document.body.appendChild(link);
            link.click();
            link.remove();
        } catch (e) {
            console.error('Export failed', e);
            alert('Export failed');
        }
    };

    const handlePageChange = (newPage: number) => {
        setSearchParams(prev => {
            prev.set('page', newPage.toString());
            return prev;
        });
    };

    return (
        <div className="space-y-6">
            <div className="md:flex md:items-center md:justify-between">
                <h2 className="text-2xl font-bold text-gray-900">Contacts</h2>
                <div className="mt-4 flex gap-2 md:mt-0">
                    <button
                        onClick={handleExport}
                        className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                    >
                        <DocumentArrowDownIcon className="-ml-0.5 mr-1.5 h-5 w-5 text-gray-400" />
                        Export CSV
                    </button>
                    <button
                        onClick={() => setIsImportModalOpen(true)}
                        className="inline-flex items-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 hover:bg-gray-50"
                    >
                        <DocumentArrowUpIcon className="-ml-0.5 mr-1.5 h-5 w-5 text-gray-400" />
                        Import CSV
                    </button>
                    <button
                        onClick={handleCreate}
                        className="inline-flex items-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-700"
                    >
                        Add Contact
                    </button>
                </div>
            </div>

            {/* Search Bar */}
            <div className="relative max-w-sm">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                    <MagnifyingGlassIcon className="h-5 w-5 text-gray-400" aria-hidden="true" />
                </div>
                <input
                    type="text"
                    name="search-contacts"
                    id="search-contacts"
                    className="block w-full rounded-md border-0 py-1.5 pl-10 text-gray-900 ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                    placeholder="Search contacts..."
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                />
            </div>

            {/* Table */}
            <div className="bg-white shadow-xs ring-1 ring-gray-900/5 sm:rounded-xl">
                {isLoading ? (
                    <div className="p-8 text-center text-gray-500">Loading...</div>
                ) : (
                    <>
                        <div className="overflow-x-auto">
                            <table className="min-w-full divide-y divide-gray-300">
                                <thead>
                                    <tr>
                                        <th className="py-3.5 pl-4 pr-3 text-left text-sm font-semibold text-gray-900 sm:pl-6">Name</th>
                                        <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Phone</th>
                                        <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Created By</th>
                                        <th className="px-3 py-3.5 text-left text-sm font-semibold text-gray-900">Created At</th>
                                        <th className="relative py-3.5 pl-3 pr-4 sm:pr-6"><span className="sr-only">Actions</span></th>
                                    </tr>
                                </thead>
                                <tbody className="divide-y divide-gray-200">
                                    {data?.data?.map((contact: Contact) => (
                                        <tr key={contact.id}>
                                            <td className="whitespace-nowrap py-4 pl-4 pr-3 text-sm font-medium text-gray-900 sm:pl-6">{contact.name}</td>
                                            <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">{contact.phone_raw}</td>
                                            <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">{contact.creator_name || 'Unknown'}</td>
                                            <td className="whitespace-nowrap px-3 py-4 text-sm text-gray-500">{new Date(contact.created_at).toLocaleDateString()}</td>
                                            <td className="relative whitespace-nowrap py-4 pl-3 pr-4 text-right text-sm font-medium sm:pr-6">
                                                <button onClick={() => handleEdit(contact)} className="text-blue-600 hover:text-blue-900 mr-4">
                                                    <PencilIcon className="h-4 w-4" />
                                                    <span className="sr-only">Edit</span>
                                                </button>
                                                <button onClick={() => handleDelete(contact.id)} className="text-red-600 hover:text-red-900">
                                                    <TrashIcon className="h-4 w-4" />
                                                    <span className="sr-only">Delete</span>
                                                </button>
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>

                        {/* Pagination */}
                        <div className="flex items-center justify-between border-t border-gray-200 bg-white px-4 py-3 sm:px-6 rounded-b-xl">
                            <div className="flex flex-1 justify-between sm:hidden">
                                <button
                                    onClick={() => handlePageChange(page - 1)}
                                    disabled={page === 1}
                                    className="relative inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                                >
                                    Previous
                                </button>
                                <button
                                    onClick={() => handlePageChange(page + 1)}
                                    disabled={page >= (data?.pagination?.pages || 1)}
                                    className="relative ml-3 inline-flex items-center rounded-md border border-gray-300 bg-white px-4 py-2 text-sm font-medium text-gray-700 hover:bg-gray-50 disabled:opacity-50"
                                >
                                    Next
                                </button>
                            </div>
                            <div className="hidden sm:flex sm:flex-1 sm:items-center sm:justify-between">
                                <div>
                                    <p className="text-sm text-gray-700">
                                        Showing <span className="font-medium">{(page - 1) * 10 + 1}</span> to <span className="font-medium">{Math.min(page * 10, data?.pagination?.totalItems || 0)}</span> of{' '}
                                        <span className="font-medium">{data?.pagination?.totalItems}</span> results
                                    </p>
                                </div>
                                <div>
                                    <nav className="isolate inline-flex -space-x-px rounded-md shadow-xs" aria-label="Pagination">
                                        <button
                                            onClick={() => handlePageChange(page - 1)}
                                            disabled={page === 1}
                                            className="relative inline-flex items-center rounded-l-md px-2 py-2 text-gray-400 ring-1 ring-inset ring-gray-300 hover:bg-gray-50 focus:z-20 focus:outline-offset-0 disabled:opacity-50"
                                        >
                                            <span className="sr-only">Previous</span>
                                            Previous
                                        </button>
                                        {/* Simple Pagination for now */}
                                        <span className="relative inline-flex items-center px-4 py-2 text-sm font-semibold text-gray-700 ring-1 ring-inset ring-gray-300 focus:outline-offset-0">
                                            Page {page} of {data?.pagination?.pages || 1}
                                        </span>
                                        <button
                                            onClick={() => handlePageChange(page + 1)}
                                            disabled={page >= (data?.pagination?.pages || 1)}
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

            <ContactModal
                isOpen={isModalOpen}
                onClose={() => setIsModalOpen(false)}
                onSuccess={() => queryClient.invalidateQueries({ queryKey: ['contacts'] })}
                contact={selectedContact}
                users={users}
            />



            {isImportModalOpen && (
                <ImportContactsModal
                    onClose={() => setIsImportModalOpen(false)}
                    onImport={() => queryClient.invalidateQueries({ queryKey: ['contacts'] })}
                    users={users || []}
                />
            )}
        </div>
    );
}
