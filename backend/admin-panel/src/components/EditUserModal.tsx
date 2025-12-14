import { Fragment, useEffect } from 'react';
import { Dialog, Transition } from '@headlessui/react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import api from '../lib/axios';
import { useMutation } from '@tanstack/react-query';
import { XMarkIcon } from '@heroicons/react/24/outline';

const editUserSchema = z.object({
    name: z.string().min(1, 'Name is required'),
    password: z.string().optional(),
    is_admin: z.boolean(),
});

type EditUserFormData = z.infer<typeof editUserSchema>;

interface User {
    id: string;
    username: string;
    name: string;
    is_admin: boolean;
}

interface EditUserModalProps {
    isOpen: boolean;
    onClose: () => void;
    onSuccess: () => void;
    user: User | null;
}

export default function EditUserModal({ isOpen, onClose, onSuccess, user }: EditUserModalProps) {
    const {
        register,
        handleSubmit,
        reset,
        setValue,
        formState: { errors },
    } = useForm<EditUserFormData>({
        resolver: zodResolver(editUserSchema),
        defaultValues: {
            name: '',
            password: '',
            is_admin: false,
        },
    });

    useEffect(() => {
        if (user) {
            setValue('name', user.name);
            setValue('is_admin', user.is_admin);
            setValue('password', ''); // Always reset password field
        }
    }, [user, setValue]);

    const mutation = useMutation({
        mutationFn: async (data: EditUserFormData) => {
            if (!user) return;
            // Only send password if it's not empty
            const payload: any = {
                name: data.name,
                is_admin: data.is_admin,
            };
            if (data.password) {
                payload.password = data.password;
            }
            await api.patch(`/users/${user.id}`, payload);
        },
        onSuccess: () => {
            reset();
            onSuccess();
            onClose();
        },
        onError: (error: any) => {
            alert(error.response?.data?.error || 'Failed to update user');
        },
    });

    const onSubmit = (data: EditUserFormData) => {
        mutation.mutate(data);
    };

    return (
        <Transition.Root show={isOpen} as={Fragment}>
            <Dialog as="div" className="relative z-10" onClose={onClose}>
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

                <div className="fixed inset-0 z-10 w-screen overflow-y-auto">
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
                                            Edit User: {user?.username}
                                        </Dialog.Title>
                                        <div className="mt-4">
                                            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
                                                <div>
                                                    <label htmlFor="name" className="block text-sm font-medium leading-6 text-gray-900">
                                                        Name
                                                    </label>
                                                    <div className="mt-2">
                                                        <input
                                                            id="name"
                                                            type="text"
                                                            autoComplete="name"
                                                            {...register('name')}
                                                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                                                        />
                                                        {errors.name && <p className="mt-1 text-sm text-red-600">{errors.name.message}</p>}
                                                    </div>
                                                </div>

                                                <div>
                                                    <label htmlFor="password" className="block text-sm font-medium leading-6 text-gray-900">
                                                        New Password (Optional)
                                                    </label>
                                                    <div className="mt-2">
                                                        <input
                                                            id="password"
                                                            type="password"
                                                            autoComplete="new-password"
                                                            {...register('password')}
                                                            placeholder="Leave blank to keep current password"
                                                            className="block w-full rounded-md border-0 py-1.5 text-gray-900 shadow-xs ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-blue-600 sm:text-sm sm:leading-6"
                                                        />
                                                        {errors.password && <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>}
                                                    </div>
                                                </div>

                                                <div className="relative flex gap-x-3 items-center">
                                                    <div className="flex h-6 items-center">
                                                        <input
                                                            id="is_admin"
                                                            type="checkbox"
                                                            {...register('is_admin')}
                                                            className="h-4 w-4 rounded-sm border-gray-300 text-blue-600 focus:ring-blue-600"
                                                        />
                                                    </div>
                                                    <div className="text-sm leading-6">
                                                        <label htmlFor="is_admin" className="font-medium text-gray-900">
                                                            Administrator Privileges
                                                        </label>
                                                    </div>
                                                </div>

                                                <div className="mt-5 sm:mt-4 sm:flex sm:flex-row-reverse">
                                                    <button
                                                        type="submit"
                                                        disabled={mutation.isPending}
                                                        className="inline-flex w-full justify-center rounded-md bg-blue-600 px-3 py-2 text-sm font-semibold text-white shadow-xs hover:bg-blue-500 sm:ml-3 sm:w-auto disabled:opacity-50"
                                                    >
                                                        {mutation.isPending ? 'Saving...' : 'Save Changes'}
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
