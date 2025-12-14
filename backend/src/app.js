const express = require('express');
const cors = require('cors');
const path = require('path');
const rateLimit = require('express-rate-limit');
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const contactRoutes = require('./routes/contacts');
const callRoutes = require('./routes/calls');
const syncRoutes = require('./routes/sync');
const syncAuditRoutes = require('./routes/syncAudit');
const errorHandler = require('./middleware/errorHandler');
const AppError = require('./utils/AppError');

const helmet = require('helmet');

const app = express();

// Middleware
app.use(helmet({
  contentSecurityPolicy: {
    directives: {
      defaultSrc: ["'self'"],
      scriptSrc: ["'self'", "'unsafe-inline'", "'unsafe-eval'"],
      styleSrc: ["'self'", "'unsafe-inline'"],
      imgSrc: ["'self'", "data:", "https:"],
      connectSrc: ["'self'"],
    },
  },
}));
app.use(cors());
app.use(express.json({ limit: '10mb' })); // For bulk uploads
app.use(express.urlencoded({ extended: true }));

// Global Rate Limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100, // Limit each IP to 100 requests per `window` (here, per 15 minutes)
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

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

// SPA Fallback for Admin Panel (Production)
// This must come before API routes if you want strict separation, 
// or after if you want to ensure API takes precedence. 
// Since API routes don't start with /admin, strictly speaking it's safe here 
// or before the 404 handler. 
// Placing it here to capture /admin/* requests immediately after static files check.
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
app.use('*', (req, res, next) => {
  next(new AppError(`Can't find ${req.originalUrl} on this server!`, 404));
});

// Error handler
app.use(errorHandler);

module.exports = app;
