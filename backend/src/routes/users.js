const express = require('express');
const bcrypt = require('bcryptjs');
const db = require('../config/knex');
const { authenticateToken, requireAdmin } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// All routes require authentication and admin
router.use(authenticateToken, requireAdmin);

// GET /users
router.get('/', asyncHandler(async (req, res) => {
  const { page, limit, offset } = getPaginationParams(req);

  const countResult = await db('users').count('id as count').first();
  const total = parseInt(countResult.count);

  const users = await db('users')
    .select('id', 'username', 'name', 'is_admin', 'created_at')
    .orderBy('created_at', 'desc')
    .limit(limit)
    .offset(offset);

  res.json(getPaginationResult(users, total, page, limit));
}));

// POST /users
router.post('/', asyncHandler(async (req, res) => {
  const { username, name, password, is_admin = false } = req.body;

  if (!username || !name || !password) {
    return res.status(400).json({ error: 'Username, name, and password are required' });
  }

  // Check if username already exists
  const existingUser = await db('users').where({ username }).first();
  if (existingUser) {
    return res.status(409).json({ error: 'Username already exists' });
  }

  // Hash password
  const saltRounds = 12;
  const passwordHash = await bcrypt.hash(password, saltRounds);

  const id = db.raw('UUID()');

  await db('users')
    .insert({
      id,
      username,
      name,
      password_hash: passwordHash,
      is_admin
    });

  // Fetch the created user
  const newUser = await db('users')
    .select('id', 'username', 'name', 'is_admin', 'created_at')
    .where({ username })
    .first();

  res.status(201).json({ user: newUser });
}));

// PATCH /users/:id
router.patch('/:id', asyncHandler(async (req, res) => {
  const { id } = req.params;
  const { name, is_admin, password } = req.body;

  const updates = {};
  if (name) updates.name = name;
  if (is_admin !== undefined) updates.is_admin = is_admin;
  if (password) {
    const saltRounds = 12;
    updates.password_hash = await bcrypt.hash(password, saltRounds);
  }

  if (Object.keys(updates).length === 0) {
    return res.status(400).json({ error: 'No fields to update' });
  }

  updates.updated_at = db.fn.now();

  const rowsUpdated = await db('users')
    .where({ id })
    .update(updates);

  if (rowsUpdated === 0) {
    return res.status(404).json({ error: 'User not found' });
  }

  const updatedUser = await db('users')
    .select('id', 'username', 'name', 'is_admin', 'created_at')
    .where({ id })
    .first();

  res.json({ user: updatedUser });
}));

// DELETE /users/:id
router.delete('/:id', asyncHandler(async (req, res) => {
  const { id } = req.params;

  // Prevent deleting self
  if (id === req.user.id) {
    return res.status(400).json({ error: 'Cannot delete your own account' });
  }

  const rowsDeleted = await db('users').where({ id }).delete();

  if (rowsDeleted === 0) {
    return res.status(404).json({ error: 'User not found' });
  }

  res.json({ message: 'User deleted successfully' });
}));

module.exports = router;
