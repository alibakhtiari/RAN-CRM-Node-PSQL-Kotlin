const express = require('express');
const pool = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');
const { normalizePhoneNumber } = require('../utils/phone');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// GET /calls
router.get('/', async (req, res) => {
  try {
    const { page, limit, offset } = getPaginationParams(req);
    const { updated_since } = req.query;

    let whereClause = '';
    let params = [limit, offset];
    let paramIndex = 3;

    // Delta sync support
    if (updated_since) {
      whereClause = 'WHERE cl.timestamp > $3';
      params = [limit, offset, new Date(updated_since)];
    }

    const countQuery = `SELECT COUNT(*) FROM call_logs cl ${whereClause}`;
    const countResult = await pool.query(countQuery, updated_since ? [new Date(updated_since)] : []);
    const total = parseInt(countResult.rows[0].count);

    const dataQuery = `SELECT cl.id, cl.user_id, cl.contact_id, cl.direction, cl.duration_seconds, cl.timestamp,
              c.name as contact_name, c.phone_raw as contact_phone
       FROM call_logs cl
       LEFT JOIN contacts c ON cl.contact_id = c.id
       ${whereClause}
       ORDER BY cl.timestamp DESC LIMIT $${paramIndex - 2} OFFSET $${paramIndex - 1}`;
    const result = await pool.query(dataQuery, params);

    res.json(getPaginationResult(result.rows, total, page, limit));
  } catch (error) {
    console.error('Get calls error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /calls/:contact_id
router.get('/:contact_id', async (req, res) => {
  try {
    const { contact_id } = req.params;
    const { page, limit, offset } = getPaginationParams(req);

    // Verify contact exists and user has access
    const contactCheck = await pool.query(
      'SELECT id FROM contacts WHERE id = $1 AND created_by = $2',
      [contact_id, req.user.id]
    );

    if (contactCheck.rows.length === 0) {
      return res.status(404).json({ error: 'Contact not found or access denied' });
    }

    const countResult = await pool.query(
      'SELECT COUNT(*) FROM call_logs WHERE contact_id = $1',
      [contact_id]
    );
    const total = parseInt(countResult.rows[0].count);

    const result = await pool.query(
      `SELECT cl.id, cl.user_id, cl.contact_id, cl.direction, cl.duration_seconds, cl.timestamp,
              c.name as contact_name, c.phone_raw as contact_phone
       FROM call_logs cl
       LEFT JOIN contacts c ON cl.contact_id = c.id
       WHERE cl.contact_id = $1
       ORDER BY cl.timestamp DESC LIMIT $2 OFFSET $3`,
      [contact_id, limit, offset]
    );

    res.json(getPaginationResult(result.rows, total, page, limit));
  } catch (error) {
    console.error('Get contact calls error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /calls
router.post('/', async (req, res) => {
  try {
    const { calls } = req.body;

    if (!Array.isArray(calls) || calls.length === 0) {
      return res.status(400).json({ error: 'Calls array is required and must not be empty' });
    }

    if (calls.length > 1000) {
      return res.status(400).json({ error: 'Maximum 1000 calls per request' });
    }

    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      const insertedCalls = [];

      for (const call of calls) {
        const { contact_id, direction, duration_seconds, timestamp, phone_normalized } = call;

        if (!direction || !['incoming', 'outgoing', 'missed'].includes(direction)) {
          throw new Error(`Invalid direction: ${direction}`);
        }

        if (duration_seconds < 0) {
          throw new Error('Duration must be non-negative');
        }

        let finalContactId = contact_id;

        // If no contact_id but phone_normalized provided, try to find matching contact
        if (!finalContactId && phone_normalized) {
          const normalizedPhone = normalizePhoneNumber(phone_normalized);
          const contactResult = await client.query(
            'SELECT id FROM contacts WHERE phone_normalized = $1 AND created_by = $2',
            [normalizedPhone, req.user.id]
          );
          if (contactResult.rows.length > 0) {
            finalContactId = contactResult.rows[0].id;
          }
        }

        const result = await client.query(
          'INSERT INTO call_logs (user_id, contact_id, direction, duration_seconds, timestamp) VALUES ($1, $2, $3, $4, $5) RETURNING id, user_id, contact_id, direction, duration_seconds, timestamp',
          [req.user.id, finalContactId, direction, duration_seconds, timestamp || new Date()]
        );

        insertedCalls.push(result.rows[0]);
      }

      await client.query('COMMIT');
      res.status(201).json({ calls: insertedCalls, count: insertedCalls.length });
    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  } catch (error) {
    console.error('Bulk upload calls error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
