const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/knex'); // Use Knex
const { secret, expiresIn } = require('../config/jwt');
const { authenticateToken } = require('../middleware/auth');
const validate = require('../middleware/validate');
const { loginSchema } = require('../schemas/authSchemas');

const router = express.Router();

// POST /auth/login
router.post('/login', (req, res, next) => {
  console.log('Login request body:', req.body);
  console.log('Login Schema:', loginSchema);
  next();
}, validate(loginSchema), async (req, res) => {
  try {
    const { username, password } = req.body;

    const user = await db('users')
      .select('id', 'username', 'name', 'password_hash', 'is_admin')
      .where({ username })
      .first();

    if (!user) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const isValidPassword = await bcrypt.compare(password, user.password_hash);

    if (!isValidPassword) {
      return res.status(401).json({ error: 'Invalid credentials' });
    }

    const token = jwt.sign(
      { id: user.id, username: user.username, is_admin: user.is_admin },
      secret,
      { expiresIn }
    );

    res.json({
      token,
      user: {
        id: user.id,
        username: user.username,
        name: user.name,
        is_admin: user.is_admin,
      },
    });
  } catch (error) {
    console.error('Login error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /auth/me
router.get('/me', authenticateToken, async (req, res) => {
  try {
    const user = await db('users')
      .select('id', 'username', 'name', 'is_admin', 'created_at')
      .where({ id: req.user.id })
      .first();

    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ user });
  } catch (error) {
    console.error('Get user error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /auth/refresh
router.post('/refresh', authenticateToken, async (req, res) => {
  try {
    // The authenticateToken middleware already verified the token
    // and attached req.user. Issue a fresh token with a new expiry.
    const freshToken = jwt.sign(
      { id: req.user.id, username: req.user.username, is_admin: req.user.is_admin },
      secret,
      { expiresIn }
    );

    res.json({ token: freshToken });
  } catch (error) {
    console.error('Token refresh error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
