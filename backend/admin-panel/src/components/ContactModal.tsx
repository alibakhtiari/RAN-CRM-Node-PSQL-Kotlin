import { Fragment, useEffect } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { XMarkIcon } from '@heroicons/react/24/outline';
import api from '../lib/axios';
import { useAuth } from '../context/AuthContext';

const contactSchema = z.object({
    name: z.string().min(1, 'Name is required'),
    phone_raw: z.string().min(1, 'Phone number is required'),
    created_by: z.string().optional(), // Admin can change creator
});

type ContactInputs = z.infer<typeof contactSchema>;

interface ContactModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    contact?: any; // If provided, we are editing
    users?: any[]; // For admin to select creator
}

export default function ContactModal({ isOpen, onClose, onSuccess, contact, users }: ContactModalProps) {
    const { user: currentUser } = useAuth();

    const {
        register,
        handleSubmit,
        reset,
        setValue,
        formState: { errors, isSubmitting },
    } = useForm<ContactInputs>({
        resolver: zodResolver(contactSchema),
    });

    useEffect(() => {
        if (contact) {
            setValue('name', contact.name);
            setValue('phone_raw', contact.phone_raw);
            setValue('created_by', contact.created_by);
        } else {
            reset({
                name: '',
                phone_raw: '',
                created_by: currentUser?.id,
            });
        }
    }, [contact, setValue, reset, currentUser]);

    const onSubmit = async (data: ContactInputs) => {
        try {
            if (contact) {
                await api.put(`/contacts/${contact.id}`, data);
            } else {
                await api.post('/contacts', { ...data, created_at: new Date().toISOString() });
            }
            reset();
            onSuccess();
            onClose();
        } catch (err: any) {
            console.error('Failed to save contact', err);
            alert(err.response?.data?.error || 'Failed to save contact');
        }
    };

    return (
        <Transition.Root show={isOpen} as={Fragment}>
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
                                    <div className="mt-3 text-center sm:ml-4 sm:mt-0 sm:text-left w-full">
                                        <Dialog.Title as="h3" className="text-base font-semibold leading-6 text-gray-900">
                                            {contact ? 'Edit Contact' : 'Create New Contact'}
                                        </Dialog.Title>
                                        <div className="mt-2">
                                            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700">Name</label>
                                                    <input
                                                        type="text"
                                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                        {...register('name')}
                                                    />
                                                    {errors.name && <p className="text-red-500 text-xs">{errors.name.message}</p>}
                                                </div>

                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700">Phone</label>
                                                    <input
                                                        type="text"
                                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                        {...register('phone_raw')}
                                                    />
                                                    {errors.phone_raw && <p className="text-red-500 text-xs">{errors.phone_raw.message}</p>}
                                                </div>

                                                {currentUser?.is_admin && users && (
                                                    <div>
                                                        <label className="block text-sm font-medium text-gray-700">Assign To</label>
                                                        <select
                                                            className="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                            {...register('created_by')}
                                                        >
                                                            {users.map(u => (
                                                                <option key={u.id} value={u.id}>{u.name} ({u.username})</option>
                                                            ))}
                                                        </select>
                                                    </div>
                                                )}

                                                <div className="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
                                                    <button
                                                        type="submit"
                                                        disabled={isSubmitting}
                                                        className="inline-flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-500 sm:ml-3 sm:w-auto disabled:opacity-50"
                                                    >
                                                        {isSubmitting ? 'Saving...' : 'Save'}
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="mt-3 inline-flex w-full justify-center rounded-md bg-white px-3 py-2 text-sm font-semibold text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 hover:bg-gray-50 sm:mt-0 sm:w-auto"
                                                        onClick={onClose}
                                                    >
                                                        Cancel
                                                    </button>
                                                </div>
                                            </form>
                                        </div>
                                    </div>
                                </div>
                            </Dialog.Panel>
                        </Transition.Child>
                    </div>
                </div>
            </Dialog>
        </Transition.Root>
    );
}
