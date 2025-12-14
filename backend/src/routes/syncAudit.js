const express = require('express');
const db = require('../config/knex'); // Use Knex
const { authenticateToken, requireAdmin } = require('../middleware/auth');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// GET /sync-audit - Get sync audit data (admin only)
router.get('/', requireAdmin, async (req, res) => {
    try {
        // Complex query rewrite to Knex raw or builders
        // Keeping it raw for simplicity but running through Knex
        const query = `
      WITH last_syncs AS (
        SELECT DISTINCT ON (user_id, sync_type)
          user_id,
          sync_type,
          status,
          error_message,
          synced_contacts,
          synced_calls,
          created_at
        FROM sync_audit
        ORDER BY user_id, sync_type, created_at DESC
      )
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
      LEFT JOIN last_syncs ls_contacts ON u.id = ls_contacts.user_id AND ls_contacts.sync_type = 'contacts'
      LEFT JOIN last_syncs ls_calls ON u.id = ls_calls.user_id AND ls_calls.sync_type = 'calls'
      ORDER BY u.username
    `;

        const result = await db.raw(query);
        res.json({ sync_audits: result.rows });
    } catch (error) {
        console.error('Get sync audit error:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

// POST /sync-audit - Record sync event
router.post('/', async (req, res) => {
    try {
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

        const [newAudit] = await db('sync_audit')
            .insert({
                user_id: req.user.id,
                sync_type,
                status,
                error_message: error_message || null,
                synced_contacts,
                synced_calls
            })
            .returning(['id', 'user_id', 'sync_type', 'status', 'created_at']);

        res.status(201).json({ sync_audit: newAudit });
    } catch (error) {
        console.error('Create sync audit error:', error);
        res.status(500).json({ error: 'Internal server error' });
    }
});

module.exports = router;
