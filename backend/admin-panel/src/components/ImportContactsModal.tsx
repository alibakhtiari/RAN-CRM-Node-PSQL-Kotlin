import { Fragment, useState, useRef } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { XMarkIcon, DocumentArrowUpIcon } from '@heroicons/react/24/outline';
import api from '../lib/axios';

interface ImportContactsModalProps {
    onClose: () => void;
    onImport: () => void;
    users: any[];
}

interface ExternalContact {
    name: string;
    phone_raw: string;
    [key: string]: any;
}

export default function ImportContactsModal({ onClose, onImport }: ImportContactsModalProps) {
    const [inputMode, setInputMode] = useState<'paste' | 'file'>('file');
    const [pasteInput, setPasteInput] = useState('');
    const [file, setFile] = useState<File | null>(null);
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const [importResult, setImportResult] = useState<any>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const parseCSV = (text: string): ExternalContact[] => {
        const lines = text.split(/\r?\n/).filter(line => line.trim() !== '');
        if (lines.length < 2) throw new Error('CSV must have a header row and at least one data row.');

        const headers = lines[0].split(',').map(h => h.trim().toLowerCase().replace(/^"|"$/g, ''));
        const nameIndex = headers.findIndex(h => h.includes('name'));
        const phoneIndex = headers.findIndex(h => h.includes('phone') || h.includes('mobile'));

        if (nameIndex === -1 && phoneIndex === -1) {
            throw new Error('Could not detect "name" or "phone" headers.');
        }

        const result: ExternalContact[] = [];

        for (let i = 1; i < lines.length; i++) {
            // Simple comma split (doesn't handle commas inside quotes perfectly but good enough for simple contact lists)
            const values = lines[i].split(',').map(v => v.trim().replace(/^"|"$/g, ''));

            if (values.length < headers.length) continue;

            const contact: any = {};
            if (nameIndex !== -1) contact.name = values[nameIndex];
            if (phoneIndex !== -1) contact.phone_raw = values[phoneIndex];

            // If we only found one column, use it for both if sensible, or validation will catch it later
            if (contact.name || contact.phone_raw) {
                result.push(contact);
            }
        }
        return result;
    };

    const handleImport = async () => {
        setError('');
        setImportResult(null);
        setLoading(true);

        try {
            let contacts: ExternalContact[] = [];
            let contentToParse = '';

            if (inputMode === 'file') {
                if (!file) throw new Error('Please select a CSV file.');
                contentToParse = await file.text();
            } else {
                contentToParse = pasteInput;
            }

            if (!contentToParse.trim()) throw new Error('Input is empty.');

            // Attempt to parse as JSON first (legacy support), then CSV
            try {
                if (contentToParse.trim().startsWith('[')) {
                    contacts = JSON.parse(contentToParse);
                } else {
                    contacts = parseCSV(contentToParse);
                }
            } catch (e: any) {
                throw new Error('Parsing failed: ' + e.message);
            }

            if (!Array.isArray(contacts) || contacts.length === 0) {
                throw new Error('No valid contacts found to import.');
            }

            const res = await api.post('/contacts/batch', { contacts, force_restore: true });
            setImportResult(res.data);
            onImport();

        } catch (err: any) {
            console.error('Import failed', err);
            setError(err.message || err.response?.data?.error || 'Import failed');
        } finally {
            setLoading(false);
        }
    };

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files.length > 0) {
            setFile(e.target.files[0]);
            setError('');
        }
    };

    return (
        <Transition.Root show={true} as={Fragment}>
            <Dialog as="div" className="relative z-50" onClose={onClose}>
                <Transition.Child
                    as={Fragment}
                    enter="ease-out duration-300"
                    enterFrom="opacity-0"
                    enterTo="opacity-100"
                    leave="ease-in duration-200"
                    leaveFrom="opacity-100"
                    leaveTo="opacity-0"
                >
                    <div className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity" />
                </Transition.Child>

                <div className="fixed inset-0 z-10 overflow-y-auto">
                    <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
                        <Transition.Child
                            as={Fragment}
                            enter="ease-out duration-300"
                            enterFrom="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                            enterTo="opacity-100 translate-y-0 sm:scale-100"
                            leave="ease-in duration-200"
                            leaveFrom="opacity-100 translate-y-0 sm:scale-100"
                            leaveTo="opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                        >
                            <Dialog.Panel className="relative transform overflow-hidden rounded-lg bg-white px-4 pb-4 pt-5 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg sm:p-6">
                                <div className="absolute right-0 top-0 hidden pr-4 pt-4 sm:block">
                                    <button
                                        type="button"
                                        className="rounded-md bg-white text-gray-400 hover:text-gray-500 focus:outline-hidden focus:ring-2 focus:ring-blue-500 focus:ring-offset-2"
                                        onClick={onClose}
                                    >
                                        <span className="sr-only">Close</span>
                                        <XMarkIcon className="h-6 w-6" aria-hidden="true" />
                                    </button>
                                </div>

                                <div className="sm:flex sm:items-start">
                                    <div className="mx-auto flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-green-100 sm:mx-0 sm:h-10 sm:w-10">
                                        <DocumentArrowUpIcon className="h-6 w-6 text-green-600" aria-hidden="true" />
                                    </div>
                                    <div className="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left w-full">
                                        <Dialog.Title as="h3" className="text-base font-semibold leading-6 text-gray-900">
                                            Import Contacts
                                        </Dialog.Title>

                                        <div className="mt-4">
                                            {/* Tabs */}
                                            <div className="border-b border-gray-200 mb-4">
                                                <nav className="-mb-px flex space-x-8" aria-label="Tabs">
                                                    <button
                                                        onClick={() => setInputMode('file')}
                                                        className={`${inputMode === 'file'
                                                            ? 'border-blue-500 text-blue-600'
                                                            : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                                                            } whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium`}
                                                    >
                                                        Upload CSV File
                                                    </button>
                                                    <button
                                                        onClick={() => setInputMode('paste')}
                                                        className={`${inputMode === 'paste'
                                                            ? 'border-blue-500 text-blue-600'
                                                            : 'border-transparent text-gray-500 hover:border-gray-300 hover:text-gray-700'
                                                            } whitespace-nowrap border-b-2 py-4 px-1 text-sm font-medium`}
                                                    >
                                                        Paste Text
                                                    </button>
                                                </nav>
                                            </div>

                                            {error && (
                                                <div className="mb-4 bg-red-50 border-l-4 border-red-400 p-4">
                                                    <p className="text-sm text-red-700">{error}</p>
                                                </div>
                                            )}

                                            {!importResult ? (
                                                <div className="mt-2 text-sm text-gray-500">
                                                    {inputMode === 'file' ? (
                                                        <div className="mt-2 flex justify-center rounded-lg border border-dashed border-gray-900/25 px-6 py-10">
                                                            <div className="text-center">
                                                                <DocumentArrowUpIcon className="mx-auto h-12 w-12 text-gray-300" aria-hidden="true" />
                                                                <div className="mt-4 flex text-sm leading-6 text-gray-600 justify-center">
                                                                    <label
                                                                        htmlFor="file-upload"
                                                                        className="relative cursor-pointer rounded-md bg-white font-semibold text-blue-600 focus-within:outline-hidden focus-within:ring-2 focus-within:ring-blue-600 focus-within:ring-offset-2 hover:text-blue-500"
                                                                    >
                                                                        <span>Upload a file</span>
                                                                        <input
                                                                            id="file-upload"
                                                                            name="file-upload"
                                                                            type="file"
                                                                            accept=".csv,.txt"
                                                                            className="sr-only"
                                                                            ref={fileInputRef}
                                                                            onChange={handleFileChange}
                                                                        />
                                                                    </label>
                                                                    <p className="pl-1">or drag and drop</p>
                                                                </div>
                                                                <p className="text-xs leading-5 text-gray-600">CSV up to 10MB</p>
                                                                {file && <p className="mt-2 text-sm font-semibold text-blue-600">{file.name}</p>}
                                                            </div>
                                                        </div>
                                                    ) : (
                                                        <>
                                                            <p className="mb-2">Paste CSV content (header row required):</p>
                                                            <textarea
                                                                id="paste-csv"
                                                                name="paste-csv"
                                                                rows={10}
                                                                className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6 font-mono"
                                                                placeholder={`name,phone\nJohn Doe,+1234567890`}
                                                                value={pasteInput}
                                                                onChange={(e) => setPasteInput(e.target.value)}
                                                            />
                                                        </>
                                                    )}
                                                </div>
                                            ) : (
                                                <div className="bg-green-50 p-4 rounded-md">
                                                    <h4 className="text-sm font-medium text-green-800">Import Complete</h4>
                                                    <ul className="mt-2 text-sm text-green-700 list-disc list-inside">
                                                        <li>Total: {importResult.summary.total}</li>
                                                        <li>Created: {importResult.summary.created}</li>
                                                        <li>Updated: {importResult.summary.updated}</li>
                                                        <li>Errors: {importResult.summary.errors}</li>
                                                    </ul>
                                                    {importResult.errors?.length > 0 && (
                                                        <div className="mt-4">
                                                            <h5 className="text-xs font-bold text-red-800">Failed Items:</h5>
                                                            <div className="mt-1 max-h-32 overflow-y-auto text-xs text-red-700 bg-red-50 p-2 rounded-sm">
                                                                {importResult.errors.map((e: any, i: number) => (
                                                                    <div key={i}>Index {e.index}: {e.error}</div>
                                                                ))}
                                                            </div>
                                                        </div>
                                                    )}
                                                </div>
                                            )}
                                        </div>
                                    </div>
                                </div>

                                <div className="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
                                    {!importResult ? (
                                        <button
                                            type="button"
                                            disabled={loading || (inputMode === 'file' && !file) || (inputMode === 'paste' && !pasteInput)}
                                            className="inline-flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-500 sm:ml-3 sm:w-auto disabled:opacity-50"
                                            onClick={handleImport}
                                        >
                                            {loading ? 'Importing...' : 'Import'}
                                        </button>
                                    ) : (
                                        <button
                                            type="button"
                                            className="inline-flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-500 sm:ml-3 sm:w-auto"
                                            onClick={onClose}
                                        >
                                            Done
                                        </button>
                                    )}
                                    <button
                                        type="button"
                                        className="mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto"
                                        onClick={onClose}
                                    >
                                        Cancel
                                    </button>
                                </div>
                            </Dialog.Panel>
                        </Transition.Child>
                    </div>
                </div>
            </Dialog>
        </Transition.Root>
    );
}
