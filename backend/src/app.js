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

// Serve static files from public directory
app.use(express.static(path.join(__dirname, '../public')));

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Serve React App static files
app.use('/admin', express.static(path.join(__dirname, '../public/admin')));

// Handle SPA routing for admin panel
app.get('/admin/*', (req, res) => {
  res.sendFile(path.join(__dirname, '../public/admin/index.html'));
});

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
