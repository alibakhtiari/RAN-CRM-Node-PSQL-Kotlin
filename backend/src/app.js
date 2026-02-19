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
const morgan = require('morgan');

const app = express();

// Middleware — HTTP request logging
// In production: only log errors (4xx/5xx), skip static assets and successful requests
// In development: log everything for debugging
const morganSkip = process.env.NODE_ENV === 'production'
  ? (req, res) => res.statusCode < 400
  : false;
app.use(morgan(':method :url :status :res[content-length] - :response-time ms', { skip: morganSkip }));
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
  max: 500, // Limit each IP to 500 requests per 15 minutes (Android sync needs headroom)
  standardHeaders: true,
  legacyHeaders: false,
});
app.use(limiter);

// Health check
app.get('/health', (req, res) => {
  res.json({ status: 'OK', timestamp: new Date().toISOString() });
});

// Redirect root to admin panel
app.get('/', (req, res) => {
  res.redirect('/admin');
});

// --- CONFIGURATION FOR ADMIN PANEL ---

// 1. In DEVELOPMENT: Proxy /admin to the Vite dev server
if (process.env.NODE_ENV !== 'production') {
  try {
    const { createProxyMiddleware } = require('http-proxy-middleware');
    app.use('/admin', createProxyMiddleware({
      target: process.env.ADMIN_PANEL_URL || 'http://localhost:5173',
      changeOrigin: true,
      ws: true,
    }));
  } catch (e) {
    // http-proxy-middleware is a devDependency, may not be installed in production
  }
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
