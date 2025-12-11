const express = require('express');
const cors = require('cors');
const path = require('path');
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const contactRoutes = require('./routes/contacts');
const callRoutes = require('./routes/calls');
const syncRoutes = require('./routes/sync');
const syncAuditRoutes = require('./routes/syncAudit');

const app = express();

// Middleware
app.use(cors());
app.use(express.json({ limit: '10mb' })); // For bulk uploads
app.use(express.urlencoded({ extended: true }));

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

const { createProxyMiddleware } = require('http-proxy-middleware');

// --- CONFIGURATION FOR ADMIN PANEL ---

// 1. In DEVELOPMENT: Proxy /admin to the Vite dev server
if (process.env.NODE_ENV !== 'production') {
  app.use('/admin', createProxyMiddleware({
    target: process.env.ADMIN_PANEL_URL || 'http://localhost:5173',
    changeOrigin: true,
    ws: true,
  }));
}

// 2. In PRODUCTION: Serve the static files built by Vite
// The build output is located in backend/public/admin
// This line serves everything inside 'public' at the root url
// So 'public/admin/index.html' becomes available at 'YOUR_DOMAIN/admin/'
app.use(express.static(path.join(__dirname, '../public')));

// -------------------------------------

// Routes
app.use('/auth', authRoutes);
app.use('/users', userRoutes);
app.use('/contacts', contactRoutes);
app.use('/calls', callRoutes);
app.use('/sync', syncRoutes);
app.use('/sync-audit', syncAuditRoutes);

// 404 handler
app.use('*', (req, res) => {
  res.status(404).json({ error: 'Route not found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('Unhandled error:', err);
  res.status(500).json({ error: 'Internal server error' });
});

module.exports = app;
