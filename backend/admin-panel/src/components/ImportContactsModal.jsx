import React, { useState } from 'react';
import { api } from '../services/api';

export default function ImportContactsModal({ onClose, onImport }) {
    const [file, setFile] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [result, setResult] = useState(null);

    const handleFileChange = (e) => {
        const selectedFile = e.target.files[0];
        if (selectedFile && selectedFile.type === 'text/csv') {
            setFile(selectedFile);
            setError('');
        } else {
            setError('Please select a valid CSV file');
            setFile(null);
        }
    };

    const parseCSV = (text) => {
        const lines = text.split('\n').filter(line => line.trim());
        if (lines.length < 2) {
            throw new Error('CSV file is empty or invalid');
        }

        const headers = lines[0].split(',').map(h => h.trim());
        const nameIndex = headers.findIndex(h => h.toLowerCase() === 'name');
        const phoneIndex = headers.findIndex(h => h.toLowerCase() === 'phone');

        if (nameIndex === -1 || phoneIndex === -1) {
            throw new Error('CSV must have "Name" and "Phone" columns');
        }

        const contacts = [];
        for (let i = 1; i < lines.length; i++) {
            const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''));
            if (values[nameIndex] && values[phoneIndex]) {
                contacts.push({
                    name: values[nameIndex],
                    phone_raw: values[phoneIndex],
                });
            }
        }

        return contacts;
    };

    const handleImport = async () => {
        if (!file) {
            setError('Please select a file');
            return;
        }

        setLoading(true);
        setError('');
        setResult(null);

        try {
            const text = await file.text();
            const contacts = parseCSV(text);

            if (contacts.length === 0) {
                throw new Error('No valid contacts found in CSV');
            }

            const response = await api.importContacts(contacts);
            setResult(response.summary);
            setTimeout(() => {
                onImport();
                onClose();
            }, 2000);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg shadow-lg w-full max-w-md">
                <div className="p-4 border-b border-gray-200 flex justify-between items-center bg-gray-50">
                    <h3 className="text-lg font-medium text-gray-900">Import Contacts</h3>
                    <button onClick={onClose} className="text-gray-500 hover:text-gray-700 text-xl">&times;</button>
                </div>

                <div className="p-6">
                    <div className="mb-4">
                        <label className="block text-gray-700 text-sm font-bold mb-2">
                            Select CSV File
                        </label>
                        <input
                            type="file"
                            accept=".csv"
                            onChange={handleFileChange}
                            className="w-full p-2 border rounded focus:outline-none focus:ring-2 focus:ring-blue-500"
                        />
                        <p className="text-xs text-gray-500 mt-2">
                            CSV format: Name,Phone (first row must be headers)
                        </p>
                    </div>

                    {error && (
                        <div className="mb-4 p-3 bg-red-100 border border-red-400 text-red-700 rounded">
                            {error}
                        </div>
                    )}

                    {result && (
                        <div className="mb-4 p-3 bg-green-100 border border-green-400 text-green-700 rounded">
                            <p>Total: {result.total}</p>
                            <p>Created: {result.created}</p>
                            <p>Updated: {result.updated}</p>
                            <p>Errors: {result.errors}</p>
                        </div>
                    )}

                    <div className="flex justify-end gap-2">
                        <button
                            type="button"
                            onClick={onClose}
                            className="px-4 py-2 border rounded hover:bg-gray-50"
                            disabled={loading}
                        >
                            Cancel
                        </button>
                        <button
                            onClick={handleImport}
                            disabled={!file || loading}
                            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50"
                        >
                            {loading ? 'Importing...' : 'Import'}
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}
