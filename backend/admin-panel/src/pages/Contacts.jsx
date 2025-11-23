import React, { useState, useEffect } from 'react';
import { api } from '../services/api';
import ContactModal from '../components/ContactModal';
import CreateContactModal from '../components/CreateContactModal';

export default function Contacts() {
    const [contacts, setContacts] = useState([]);
    const [loading, setLoading] = useState(true);
    const [page, setPage] = useState(1);
    const [totalPages, setTotalPages] = useState(1);
    const [search, setSearch] = useState('');
    const [debouncedSearch, setDebouncedSearch] = useState('');
    const [selectedContact, setSelectedContact] = useState(null);
    const [showCreateModal, setShowCreateModal] = useState(false);

    useEffect(() => {
        const timer = setTimeout(() => {
            setDebouncedSearch(search);
            setPage(1); // Reset to page 1 on search
        }, 500);
        return () => clearTimeout(timer);
    }, [search]);

    useEffect(() => {
        loadContacts();
    }, [page, debouncedSearch]);

    const loadContacts = async () => {
        setLoading(true);
        try {
            const data = await api.getContacts(page, 10, debouncedSearch);
            setContacts(data.data);
            setTotalPages(data.pagination.pages);
        } catch (error) {
            console.error(error);
        } finally {
            setLoading(false);
        }
    };

    const handleDelete = async (id) => {
        if (!confirm('Are you sure?')) return;
        try {
            await api.deleteContact(id);
            loadContacts();
        } catch (error) {
            alert(error.message);
        }
    };

    return (
        <div className="bg-white rounded-lg shadow overflow-hidden">
            <div className="p-6 border-b border-gray-200 flex justify-between items-center">
                <h3 className="text-lg font-medium text-gray-900">Contacts</h3>
                <div className="flex gap-4">
                    <input
                        type="text"
                        placeholder="Search contacts..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        className="p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                    <button
                        onClick={() => setShowCreateModal(true)}
                        className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700"
                    >
                        Create Contact
                    </button>
                </div>
            </div>

            {loading ? (
                <div className="p-8 text-center">Loading...</div>
            ) : (
                <>
                    <div className="overflow-x-auto">
                        <table className="min-w-full divide-y divide-gray-200">
                            <thead className="bg-gray-50">
                                <tr>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Name</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Phone</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Created By</th>
                                    <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Created At</th>
                                    <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 uppercase tracking-wider">Actions</th>
                                </tr>
                            </thead>
                            <tbody className="bg-white divide-y divide-gray-200">
                                {contacts.map(contact => (
                                    <tr
                                        key={contact.id}
                                        className="hover:bg-gray-50 cursor-pointer"
                                        onClick={() => setSelectedContact(contact)}
                                    >
                                        <td className="px-6 py-4 whitespace-nowrap font-medium text-gray-900">{contact.name}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-gray-500">{contact.phone_raw}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-gray-500">{contact.creator_name || 'Unknown'}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-gray-500">{new Date(contact.created_at).toLocaleDateString()}</td>
                                        <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                                            <button
                                                onClick={(e) => { e.stopPropagation(); handleDelete(contact.id); }}
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

                    <div className="p-4 border-t border-gray-200 flex justify-between items-center">
                        <button
                            disabled={page === 1}
                            onClick={() => setPage(p => p - 1)}
                            className="px-4 py-2 border rounded disabled:opacity-50"
                        >
                            Previous
                        </button>
                        <span>Page {page} of {totalPages}</span>
                        <button
                            disabled={page === totalPages}
                            onClick={() => setPage(p => p + 1)}
                            className="px-4 py-2 border rounded disabled:opacity-50"
                        >
                            Next
                        </button>
                    </div>
                </>
            )}

            {selectedContact && (
                <ContactModal
                    contact={selectedContact}
                    onClose={() => setSelectedContact(null)}
                    onUpdate={loadContacts}
                />
            )}

            {showCreateModal && (
                <CreateContactModal
                    onClose={() => setShowCreateModal(false)}
                    onCreate={loadContacts}
                />
            )}
        </div>
    );
}
