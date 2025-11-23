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
    let countParams = [];
    let dataParams = [];

    // Filter by user if not admin
    const userFilter = req.user.is_admin ? '' : 'cl.user_id = $1';

    // Delta sync support
    if (updated_since) {
      const timeFilter = 'cl.timestamp > $' + (req.user.is_admin ? '1' : '2');
      whereClause = userFilter
        ? `WHERE ${userFilter} AND ${timeFilter}`
        : `WHERE ${timeFilter}`;

      if (req.user.is_admin) {
        countParams = [new Date(updated_since)];
        dataParams = [new Date(updated_since), limit, offset];
      } else {
        countParams = [req.user.id, new Date(updated_since)];
        dataParams = [req.user.id, new Date(updated_since), limit, offset];
      }
    } else {
      whereClause = userFilter ? `WHERE ${userFilter}` : '';
      if (req.user.is_admin) {
        dataParams = [limit, offset];
      } else {
        countParams = [req.user.id];
        dataParams = [req.user.id, limit, offset];
      }
    }

    const countQuery = `SELECT COUNT(*) FROM call_logs cl ${whereClause}`;
    const countResult = await pool.query(countQuery, countParams);
    const total = parseInt(countResult.rows[0].count);

    const dataQuery = `SELECT cl.id, cl.user_id, cl.contact_id, cl.direction, cl.duration_seconds, cl.timestamp,
            c.name as contact_name, c.phone_raw as contact_phone,
            u.username, u.name as user_name
     FROM call_logs cl
     LEFT JOIN contacts c ON cl.contact_id = c.id
     LEFT JOIN users u ON cl.user_id = u.id
     ${whereClause}
     ORDER BY cl.timestamp DESC LIMIT $${req.user.is_admin ? (updated_since ? '2' : '1') : (updated_since ? '3' : '2')} OFFSET $${req.user.is_admin ? (updated_since ? '3' : '2') : (updated_since ? '4' : '3')}`;

    const result = await pool.query(dataQuery, dataParams);

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

        // Check for duplicates before inserting
        const duplicateCheck = await client.query(
          `SELECT id FROM call_logs 
           WHERE user_id = $1 
           AND direction = $2 
           AND duration_seconds = $3 
           AND timestamp >= $4::timestamp - interval '1 second'
           AND timestamp <= $4::timestamp + interval '1 second'
           AND (
             (contact_id IS NOT NULL AND contact_id = $5) OR 
             (contact_id IS NULL AND $5 IS NULL)
           )`,
          [req.user.id, direction, duration_seconds, timestamp || new Date(), finalContactId]
        );

        if (duplicateCheck.rows.length > 0) {
          // Skip duplicate
          continue;
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

// DELETE /calls - Admin only, clear all call logs
router.delete('/', async (req, res) => {
  try {
    // Check if user is admin (requireAdmin middleware will be added in router registration)
    if (!req.user.is_admin) {
      return res.status(403).json({ error: 'Admin access required' });
    }

    const result = await pool.query('DELETE FROM call_logs');
    res.json({
      message: 'All call logs cleared successfully',
      deleted_count: result.rowCount
    });
  } catch (error) {
    console.error('Delete all calls error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
