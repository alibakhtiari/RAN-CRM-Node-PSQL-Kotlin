import { Fragment, useState } from 'react';
import { Dialog, Transition, Switch } from '@headlessui/react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { XMarkIcon } from '@heroicons/react/24/outline';
import api from '../lib/axios';

const registerSchema = z.object({
    name: z.string().min(1, 'Name is required'),
    username: z.string().min(1, 'Username is required'), // phone
    password: z.string().min(6, 'Password must be at least 6 characters'),
    is_admin: z.boolean().default(false),
});

type RegisterInputs = z.infer<typeof registerSchema>;

interface RegisterUserModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
}

export default function RegisterUserModal({ isOpen, onClose, onSuccess }: RegisterUserModalProps) {
    const [error, setError] = useState('');

    const {
        register,
        handleSubmit,
        setValue,
        watch,
        reset,
        formState: { errors, isSubmitting },
    } = useForm<RegisterInputs>({
        resolver: zodResolver(registerSchema),
        defaultValues: {
            is_admin: false,
        },
    });

    const isAdmin = watch('is_admin');

    const onSubmit = async (data: RegisterInputs) => {
        setError('');
        try {
            await api.post('/users', data);
            reset();
            onSuccess();
            onClose();
        } catch (err: any) {
            console.error('Registration failed', err);
            setError(err.response?.data?.error || 'Failed to register user');
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
                                            Register New User
                                        </Dialog.Title>
                                        <div className="mt-2">
                                            {error && (
                                                <div className="mb-4 bg-red-50 border-l-4 border-red-400 p-4">
                                                    <p className="text-sm text-red-700">{error}</p>
                                                </div>
                                            )}

                                            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700">Full Name</label>
                                                    <input
                                                        type="text"
                                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                        {...register('name')}
                                                    />
                                                    {errors.name && <p className="text-red-500 text-xs">{errors.name.message}</p>}
                                                </div>

                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700">Username / Phone</label>
                                                    <input
                                                        type="text"
                                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                        {...register('username')}
                                                    />
                                                    {errors.username && <p className="text-red-500 text-xs">{errors.username.message}</p>}
                                                </div>

                                                <div>
                                                    <label className="block text-sm font-medium text-gray-700">Password</label>
                                                    <input
                                                        type="password"
                                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-xs focus:border-blue-500 focus:ring-blue-500 sm:text-sm border p-2"
                                                        {...register('password')}
                                                    />
                                                    {errors.password && <p className="text-red-500 text-xs">{errors.password.message}</p>}
                                                </div>

                                                <div className="flex items-center justify-between">
                                                    <span className="grow flex flex-col">
                                                        <span className="text-sm font-medium text-gray-900">Admin Privileges</span>
                                                        <span className="text-sm text-gray-500">Grant full access to the system</span>
                                                    </span>
                                                    <Switch
                                                        checked={isAdmin}
                                                        onChange={(val) => setValue('is_admin', val)}
                                                        className={`${isAdmin ? 'bg-blue-600' : 'bg-gray-200'
                                                            } relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-hidden focus:ring-2 focus:ring-blue-500 focus:ring-offset-2`}
                                                    >
                                                        <span
                                                            aria-hidden="true"
                                                            className={`${isAdmin ? 'translate-x-5' : 'translate-x-0'
                                                                } pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow ring-0 transition duration-200 ease-in-out`}
                                                        />
                                                    </Switch>
                                                </div>

                                                <div className="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
                                                    <button
                                                        type="submit"
                                                        disabled={isSubmitting}
                                                        className="inline-flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-500 sm:ml-3 sm:w-auto disabled:opacity-50"
                                                    >
                                                        {isSubmitting ? 'Registering...' : 'Register'}
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
