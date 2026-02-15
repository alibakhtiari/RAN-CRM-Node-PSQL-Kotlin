const express = require('express');
const crypto = require('crypto');
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
    .select('id', 'user_id', 'sync_type', 'status', 'synced_contacts', 'synced_calls', 'created_at')
    .orderBy('created_at', 'desc')
    .limit(limit)
    .offset(offset);

  res.json(getPaginationResult(audits, total, page, limit));
}));

// POST /sync
router.post('/', asyncHandler(async (req, res) => {
  const sync_type = req.body.sync_type || req.body.syncType || 'full';
  const status = req.body.status || 'success';
  const synced_contacts = req.body.synced_contacts || req.body.syncedContacts || 0;
  const synced_calls = req.body.synced_calls || req.body.syncedCalls || 0;

  const syncId = crypto.randomUUID();

  await db('sync_audit')
    .insert({
      id: syncId,
      user_id: req.user.id,
      sync_type,
      status,
      synced_contacts,
      synced_calls
    });

  const newRecord = await db('sync_audit')
    .where({ id: syncId })
    .first();

  res.status(201).json({ sync_record: newRecord });
}));

module.exports = router;
