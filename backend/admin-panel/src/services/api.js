const API_BASE = ''; // Relative path, handled by proxy

export const getAuthHeader = () => {
    const token = localStorage.getItem('token');
    return token ? { Authorization: `Bearer ${token}` } : {};
};

export const api = {
    login: async (username, password) => {
        const response = await fetch(`${API_BASE}/auth/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Login failed');
        }
        return response.json();
    },
    getMe: async () => {
        const response = await fetch(`${API_BASE}/auth/me`, {
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to load user info');
        return response.json();
    },
    createUser: async (data) => {
        const response = await fetch(`${API_BASE}/users`, {
            method: 'POST',
            headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to create user');
        }
        return response.json();
    },
    getUsers: async () => {
        const response = await fetch(`${API_BASE}/users?limit=100`, {
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to load users');
        return response.json();
    },
    getSyncAudit: async () => {
        const response = await fetch(`${API_BASE}/sync-audit`, {
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to load sync audit');
        return response.json();
    },
    deleteUser: async (id) => {
        const response = await fetch(`${API_BASE}/users/${id}`, {
            method: 'DELETE',
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to delete user');
        return response.json();
    },
    getContacts: async (page = 1, limit = 10, search = '') => {
        const query = new URLSearchParams({ page, limit });
        if (search) query.append('q', search);

        const endpoint = search ? '/contacts/search' : '/contacts';
        const response = await fetch(`${API_BASE}${endpoint}?${query.toString()}`, {
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to load contacts');
        return response.json();
    },
    deleteContact: async (id) => {
        const response = await fetch(`${API_BASE}/contacts/${id}`, {
            method: 'DELETE',
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to delete contact');
        return response.json();
    },
    updateContact: async (id, data) => {
        const response = await fetch(`${API_BASE}/contacts/${id}`, {
            method: 'PUT',
            headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to update contact');
        }
        return response.json();
    },
    getCallLogs: async (contactId, page = 1, limit = 10) => {
        const query = new URLSearchParams({ page, limit });
        const response = await fetch(`${API_BASE}/calls/${contactId}?${query.toString()}`, {
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to load call logs');
        return response.json();
    },
    createContact: async (data) => {
        const response = await fetch(`${API_BASE}/contacts`, {
            method: 'POST',
            headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
            body: JSON.stringify(data),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to create contact');
        }
        return response.json();
    },
    exportContacts: async () => {
        const response = await fetch(`${API_BASE}/contacts/export`, {
            headers: getAuthHeader(),
        });
        if (!response.ok) throw new Error('Failed to export contacts');
        return response.blob();
    },
    importContacts: async (contacts) => {
        const response = await fetch(`${API_BASE}/contacts/batch`, {
            method: 'POST',
            headers: { ...getAuthHeader(), 'Content-Type': 'application/json' },
            body: JSON.stringify({ contacts }),
        });
        if (!response.ok) {
            const error = await response.json();
            throw new Error(error.error || 'Failed to import contacts');
        }
        return response.json();
    },
};
