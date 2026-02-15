const express = require('express');
const db = require('../config/knex');
const { authenticateToken } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// GET /sync
router.get('/', asyncHandler(async (req, res) => {
  const { page, limit, offset } = getPaginationParams(req);

  const countResult = await db('sync_audit').count('* as count').first();
  const total = parseInt(countResult.count);

  const audits = await db('sync_audit')
    .select('id', 'user_id', 'synced_contacts', 'synced_calls', 'created_at')
    .orderBy('created_at', 'desc')
    .limit(limit)
    .offset(offset);

  res.json(getPaginationResult(audits, total, page, limit));
}));

// POST /sync (optional: to record sync operations)
router.post('/', asyncHandler(async (req, res) => {
  const { synced_contacts = 0, synced_calls = 0 } = req.body;

  const id = db.raw('UUID()');

  await db('sync_audit')
    .insert({
      id,
      user_id: req.user.id,
      synced_contacts,
      synced_calls
    });

  // Fetch the inserted record
  const newRecord = await db('sync_audit')
    .where({ user_id: req.user.id })
    .orderBy('created_at', 'desc')
    .first();

  res.status(201).json({ sync_record: newRecord });
}));

module.exports = router;
