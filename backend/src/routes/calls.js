const express = require('express');
const db = require('../config/knex');
const { authenticateToken } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');
const { normalizePhoneNumber } = require('../utils/phone');
const asyncHandler = require('../middleware/asyncHandler');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// GET /calls
router.get('/', asyncHandler(async (req, res) => {
  const { page, limit, offset } = getPaginationParams(req);
  const { updated_since } = req.query;

  let query = db('call_logs as cl')
    .leftJoin('contacts as c', 'cl.contact_id', 'c.id')
    .leftJoin('users as u', 'cl.user_id', 'u.id')
    .select(
      'cl.id', 'cl.user_id', 'cl.contact_id', 'cl.direction', 'cl.duration_seconds', 'cl.timestamp', 'cl.phone_number',
      'c.name as contact_name', 'c.phone_raw as contact_phone',
      'u.username', 'u.name as user_name'
    );

  let countQuery = db('call_logs as cl').count('* as count');

  if (!req.user.is_admin) {
    query.where('cl.user_id', req.user.id);
    countQuery.where('cl.user_id', req.user.id);
  }

  if (updated_since) {
    const since = new Date(updated_since);
    query.where('cl.timestamp', '>', since);
    countQuery.where('cl.timestamp', '>', since);
  }

  const countResult = await countQuery.first();
  const total = parseInt(countResult.count);

  const calls = await query
    .orderBy('cl.timestamp', 'desc')
    .limit(limit)
    .offset(offset);

  res.json(getPaginationResult(calls, total, page, limit));
}));

// GET /calls/:contact_id
router.get('/:contact_id', asyncHandler(async (req, res) => {
  const { contact_id } = req.params;
  const { page, limit, offset } = getPaginationParams(req);

  // Verify contact exists and user has access
  const contactCheck = db('contacts').select('id').where({ id: contact_id });
  if (!req.user.is_admin) {
    contactCheck.where({ created_by: req.user.id });
  }
  const contact = await contactCheck.first();

  if (!contact) {
    return res.status(404).json({ error: 'Contact not found or access denied' });
  }

  const countResult = await db('call_logs').where({ contact_id }).count('* as count').first();
  const total = parseInt(countResult.count);

  const calls = await db('call_logs as cl')
    .leftJoin('contacts as c', 'cl.contact_id', 'c.id')
    .leftJoin('users as u', 'cl.user_id', 'u.id')
    .select(
      'cl.id', 'cl.user_id', 'cl.contact_id', 'cl.direction', 'cl.duration_seconds', 'cl.timestamp', 'cl.phone_number',
      'c.name as contact_name', 'c.phone_raw as contact_phone',
      'u.name as user_name'
    )
    .where('cl.contact_id', contact_id)
    .orderBy('cl.timestamp', 'desc')
    .limit(limit)
    .offset(offset);

  res.json(getPaginationResult(calls, total, page, limit));
}));

// POST /calls (Bulk Upload)
router.post('/', asyncHandler(async (req, res) => {
  const { calls } = req.body;

  if (!Array.isArray(calls) || calls.length === 0) {
    return res.status(400).json({ error: 'Calls array is required and must not be empty' });
  }

  if (calls.length > 1000) {
    return res.status(400).json({ error: 'Maximum 1000 calls per request' });
  }

  const insertedCalls = await db.transaction(async (trx) => {
    const results = [];
    const userContacts = await trx('contacts')
      .select('id', 'phone_normalized')
      .where({ created_by: req.user.id });

    const contactMap = new Map(userContacts.map(c => [c.phone_normalized, c.id]));

    for (const call of calls) {
      const { contact_id, direction, duration_seconds, timestamp, phone_normalized } = call;

      if (!direction || !['incoming', 'outgoing', 'missed'].includes(direction)) {
        throw new Error(`Invalid direction: ${direction}`);
      }
      if (duration_seconds < 0) {
        throw new Error('Duration must be non-negative');
      }

      let finalContactId = contact_id;
      if (!finalContactId && phone_normalized) {
        const normalized = normalizePhoneNumber(phone_normalized);
        finalContactId = contactMap.get(normalized) || null;
      }

      // Duplicate Check
      const callTime = new Date(timestamp || Date.now());
      const startWindow = new Date(callTime.getTime() - 1000);
      const endWindow = new Date(callTime.getTime() + 1000);

      const existingCall = await trx('call_logs')
        .select('id')
        .where({
          user_id: req.user.id,
          direction,
          duration_seconds
        })
        .andWhere('timestamp', '>=', startWindow)
        .andWhere('timestamp', '<=', endWindow)
        .andWhere(function () {
          if (finalContactId) {
            this.where('contact_id', finalContactId);
          } else {
            this.whereNull('contact_id');
          }
        })
        .first();

      if (existingCall) continue;

      const [insertId] = await trx('call_logs')
        .insert({
          id: trx.raw('UUID()'),
          user_id: req.user.id,
          contact_id: finalContactId,
          phone_number: phone_normalized,
          direction,
          duration_seconds,
          timestamp: callTime
        });

      const newCall = await trx('call_logs')
        .where('id', trx.raw('LAST_INSERT_ID()'))
        .orWhere({ user_id: req.user.id, timestamp: callTime, direction, duration_seconds })
        .first();

      if (newCall) results.push(newCall);
    }
    return results;
  });

  res.status(201).json({ calls: insertedCalls, count: insertedCalls.length });
}));

// DELETE /calls/:id
router.delete('/:id', asyncHandler(async (req, res) => {
  if (!req.user.is_admin) {
    return res.status(403).json({ error: 'Admin access required' });
  }

  const { id } = req.params;
  const rowsDeleted = await db('call_logs').where({ id }).delete();

  if (rowsDeleted === 0) {
    return res.status(404).json({ error: 'Call log not found' });
  }

  res.json({ message: 'Call log deleted successfully' });
}));

// DELETE /calls (Clear all)
router.delete('/', asyncHandler(async (req, res) => {
  if (!req.user.is_admin) {
    return res.status(403).json({ error: 'Admin access required' });
  }

  const rowsDeleted = await db('call_logs').delete();
  res.json({
    message: 'All call logs cleared successfully',
    deleted_count: rowsDeleted
  });
}));

module.exports = router;
