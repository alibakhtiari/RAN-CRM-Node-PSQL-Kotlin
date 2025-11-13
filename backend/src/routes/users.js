const express = require('express');
const bcrypt = require('bcryptjs');
const pool = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { requireAdmin } = require('../middleware/admin');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');

const router = express.Router();

// All routes require authentication and admin
router.use(authenticateToken, requireAdmin);

// GET /users
router.get('/', async (req, res) => {
  try {
    const { page, limit, offset } = getPaginationParams(req);

    const countResult = await pool.query('SELECT COUNT(*) FROM users');
    const total = parseInt(countResult.rows[0].count);

    const result = await pool.query(
      'SELECT id, username, name, email, is_admin, created_at FROM users ORDER BY created_at DESC LIMIT $1 OFFSET $2',
      [limit, offset]
    );

    res.json(getPaginationResult(result.rows, total, page, limit));
  } catch (error) {
    console.error('Get users error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /users
router.post('/', async (req, res) => {
  try {
    const { username, name, email, password, is_admin = false } = req.body;

    if (!username || !name || !email || !password) {
      return res.status(400).json({ error: 'Username, name, email, and password are required' });
    }

    // Check if username already exists
    const existingUsername = await pool.query('SELECT id FROM users WHERE username = $1', [username]);
    if (existingUsername.rows.length > 0) {
      return res.status(409).json({ error: 'Username already exists' });
    }

    // Check if email already exists
    const existingEmail = await pool.query('SELECT id FROM users WHERE email = $1', [email]);
    if (existingEmail.rows.length > 0) {
      return res.status(409).json({ error: 'Email already exists' });
    }

    // Hash password
    const saltRounds = 12;
    const passwordHash = await bcrypt.hash(password, saltRounds);

    const result = await pool.query(
      'INSERT INTO users (username, name, email, password_hash, is_admin) VALUES ($1, $2, $3, $4, $5) RETURNING id, username, name, email, is_admin, created_at',
      [username, name, email, passwordHash, is_admin]
    );

    res.status(201).json({ user: result.rows[0] });
  } catch (error) {
    console.error('Create user error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /users/:id
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

    // Prevent deleting self
    if (id === req.user.id) {
      return res.status(400).json({ error: 'Cannot delete your own account' });
    }

    const result = await pool.query('DELETE FROM users WHERE id = $1 RETURNING id', [id]);

    if (result.rows.length === 0) {
      return res.status(404).json({ error: 'User not found' });
    }

    res.json({ message: 'User deleted successfully' });
  } catch (error) {
    console.error('Delete user error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
