const express = require('express');
const crypto = require('crypto');
const db = require('../config/knex');
const { authenticateToken, requireAdmin } = require('../middleware/auth');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// GET /sync-audit - Get sync audit data (admin only)
router.get('/', requireAdmin, asyncHandler(async (req, res) => {
  // Rewritten from PostgreSQL DISTINCT ON to MariaDB-compatible ROW_NUMBER
  const query = `
      SELECT 
        u.id as user_id,
        u.username,
        u.name as user_name,
        ls_contacts.status as contacts_status,
        ls_contacts.created_at as contacts_last_sync,
        ls_contacts.synced_contacts,
        ls_contacts.error_message as contacts_error,
        ls_calls.status as calls_status,
        ls_calls.created_at as calls_last_sync,
        ls_calls.synced_calls,
        ls_calls.error_message as calls_error
      FROM users u
      LEFT JOIN (
        SELECT sa.* FROM sync_audit sa
        INNER JOIN (
          SELECT user_id, MAX(created_at) as max_created
          FROM sync_audit
          WHERE sync_type = 'contacts'
          GROUP BY user_id
        ) latest ON sa.user_id = latest.user_id AND sa.created_at = latest.max_created AND sa.sync_type = 'contacts'
      ) ls_contacts ON u.id = ls_contacts.user_id
      LEFT JOIN (
        SELECT sa.* FROM sync_audit sa
        INNER JOIN (
          SELECT user_id, MAX(created_at) as max_created
          FROM sync_audit
          WHERE sync_type = 'calls'
          GROUP BY user_id
        ) latest ON sa.user_id = latest.user_id AND sa.created_at = latest.max_created AND sa.sync_type = 'calls'
      ) ls_calls ON u.id = ls_calls.user_id
      ORDER BY u.username
    `;

  const [rows] = await db.raw(query);
  res.json({ sync_audits: rows });
}));

// POST /sync-audit - Record sync event
router.post('/', asyncHandler(async (req, res) => {
  // Support both camelCase (from Android) and snake_case
  const sync_type = req.body.sync_type || req.body.syncType;
  const status = req.body.status;
  const error_message = req.body.error_message || req.body.errorMessage;
  const synced_contacts = req.body.synced_contacts || req.body.syncedContacts || 0;
  const synced_calls = req.body.synced_calls || req.body.syncedCalls || 0;

  if (!sync_type || !['contacts', 'calls', 'full'].includes(sync_type)) {
    return res.status(400).json({ error: 'Invalid sync_type' });
  }

  if (!status || !['success', 'error'].includes(status)) {
    return res.status(400).json({ error: 'Invalid status' });
  }

  const auditId = crypto.randomUUID();

  await db('sync_audit')
    .insert({
      id: auditId,
      user_id: req.user.id,
      sync_type,
      status,
      error_message: error_message || null,
      synced_contacts,
      synced_calls
    });

  const newAudit = await db('sync_audit')
    .select('id', 'user_id', 'sync_type', 'status', 'created_at')
    .where({ id: auditId })
    .first();

  res.status(201).json({ sync_audit: newAudit });
}));

module.exports = router;
