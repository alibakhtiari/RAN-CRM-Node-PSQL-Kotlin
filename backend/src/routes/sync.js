const express = require('express');
const pool = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// GET /sync
router.get('/', async (req, res) => {
  try {
    const { page, limit, offset } = getPaginationParams(req);

    const countResult = await pool.query('SELECT COUNT(*) FROM sync_audit');
    const total = parseInt(countResult.rows[0].count);

    const result = await pool.query(
      'SELECT id, user_id, synced_contacts, synced_calls, created_at FROM sync_audit ORDER BY created_at DESC LIMIT $1 OFFSET $2',
      [limit, offset]
    );

    res.json(getPaginationResult(result.rows, total, page, limit));
  } catch (error) {
    console.error('Get sync audit error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /sync (optional: to record sync operations)
router.post('/', async (req, res) => {
  try {
    const { synced_contacts = 0, synced_calls = 0 } = req.body;

    const result = await pool.query(
      'INSERT INTO sync_audit (user_id, synced_contacts, synced_calls) VALUES ($1, $2, $3) RETURNING id, user_id, synced_contacts, synced_calls, created_at',
      [req.user.id, synced_contacts, synced_calls]
    );

    res.status(201).json({ sync_record: result.rows[0] });
  } catch (error) {
    console.error('Create sync audit error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
