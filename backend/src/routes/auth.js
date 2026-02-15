const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const db = require('../config/knex');
const { secret, expiresIn } = require('../config/jwt');
const { authenticateToken } = require('../middleware/auth');
const validate = require('../middleware/validate');
const { loginSchema } = require('../schemas/authSchemas');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// POST /auth/login
router.post('/login', validate(loginSchema), asyncHandler(async (req, res) => {
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
}));

// GET /auth/me
router.get('/me', authenticateToken, asyncHandler(async (req, res) => {
  const user = await db('users')
    .select('id', 'username', 'name', 'is_admin', 'created_at')
    .where({ id: req.user.id })
    .first();

  if (!user) {
    return res.status(404).json({ error: 'User not found' });
  }

  res.json({ user });
}));

// POST /auth/refresh
router.post('/refresh', authenticateToken, asyncHandler(async (req, res) => {
  const freshToken = jwt.sign(
    { id: req.user.id, username: req.user.username, is_admin: req.user.is_admin },
    secret,
    { expiresIn }
  );

  res.json({ token: freshToken });
}));

module.exports = router;
