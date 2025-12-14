const express = require('express');
const db = require('../config/knex'); // Use Knex
const { authenticateToken } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// GET /sync
router.get('/', async (req, res) => {
  try {
    const { page, limit, offset } = getPaginationParams(req);

    const countResult = await db('sync_audit').count('* as count').first();
    const total = parseInt(countResult.count);

    const audits = await db('sync_audit')
      .select('id', 'user_id', 'synced_contacts', 'synced_calls', 'created_at')
      .orderBy('created_at', 'desc')
      .limit(limit)
      .offset(offset);

    res.json(getPaginationResult(audits, total, page, limit));
  } catch (error) {
    console.error('Get sync audit error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /sync (optional: to record sync operations)
router.post('/', async (req, res) => {
  try {
    const { synced_contacts = 0, synced_calls = 0 } = req.body;

    const [newRecord] = await db('sync_audit')
      .insert({
        user_id: req.user.id,
        synced_contacts,
        synced_calls
      })
      .returning(['id', 'user_id', 'synced_contacts', 'synced_calls', 'created_at']);

    res.status(201).json({ sync_record: newRecord });
  } catch (error) {
    console.error('Create sync audit error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
