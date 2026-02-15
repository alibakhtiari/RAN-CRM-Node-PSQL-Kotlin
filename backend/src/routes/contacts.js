const express = require('express');
const crypto = require('crypto');
const db = require('../config/knex');
const { authenticateToken } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');
const { normalizePhoneNumber } = require('../utils/phone');
const { Parser } = require('json2csv');
const contactsRepository = require('../repositories/contactsRepository');
const validate = require('../middleware/validate');
const asyncHandler = require('../middleware/asyncHandler');
const { createContactSchema, updateContactSchema, batchStructureSchema } = require('../schemas/contactSchemas');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// Helper to check ownership
const checkOwnership = (contact, user) => {
  if (!contact) return false;
  return contact.created_by === user.id || user.is_admin;
};

// Shared handler for getting contacts
const getContacts = asyncHandler(async (req, res) => {
  const { page, limit, offset } = getPaginationParams(req);
  const { updated_since, q } = req.query;

  const search = (q && q.trim().length >= 2) ? q : null;
  const updatedSince = updated_since ? new Date(updated_since) : null;

  const total = await contactsRepository.count({ updatedSince, search });
  const contacts = await contactsRepository.findAll({ limit, offset, updatedSince, search });

  res.json(getPaginationResult(contacts, total, page, limit));
});

// GET /contacts
router.get('/', getContacts);

// GET /contacts/search - Legacy/Alias (MUST be before /:id)
router.get('/search', (req, res, next) => {
  const { q } = req.query;
  if (!q || q.trim().length < 2) {
    return res.status(400).json({ error: 'Search query must be at least 2 characters' });
  }
  return getContacts(req, res, next);
});

// GET /contacts/export - Export all contacts as CSV (MUST be before /:id)
router.get('/export', asyncHandler(async (req, res) => {
  const contacts = await db('contacts as c')
    .leftJoin('users as u', 'c.created_by', 'u.id')
    .select('c.name', 'c.phone_raw', 'u.name as creator_name', 'c.created_at')
    .whereNull('c.deleted_at')
    .orderBy('c.created_at', 'desc');

  const fields = [
    { label: 'Name', value: 'name' },
    { label: 'Phone', value: 'phone_raw' },
    { label: 'Created By', value: 'creator_name' },
    { label: 'Created At', value: (row) => new Date(row.created_at).toLocaleDateString() }
  ];

  const json2csvParser = new Parser({ fields });
  const csv = json2csvParser.parse(contacts);

  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', 'attachment; filename="contacts.csv"');
  res.send(csv);
}));

// POST /contacts
router.post('/', validate(createContactSchema), asyncHandler(async (req, res) => {
  const { name, phone_raw, phone_normalized, created_at } = req.body;

  const normalizedPhone = normalizePhoneNumber(phone_raw);

  const newContact = await db.transaction(async (trx) => {
    const existing = await trx('contacts')
      .select('id', 'name', 'phone_raw', 'phone_normalized', 'created_by', 'created_at', 'updated_at')
      .where({ phone_normalized: normalizedPhone })
      .first();

    if (existing) {
      if (existing.created_by === req.user.id) {
        const incomingDate = new Date(created_at || new Date());
        const existingDate = new Date(existing.created_at);

        if (incomingDate >= existingDate) {
          await trx('contacts')
            .where({ id: existing.id })
            .update({
              name,
              phone_raw,
              updated_at: trx.fn.now()
            });

          const updated = await trx('contacts')
            .select('id', 'name', 'phone_raw', 'phone_normalized', 'created_by', 'created_at', 'updated_at')
            .where({ id: existing.id })
            .first();
          return updated;
        } else {
          return existing;
        }
      } else {
        const error = new Error('Contact with this phone number already exists');
        error.statusCode = 409;
        error.isOperational = true;
        throw error;
      }
    }

    // Create new
    const newId = crypto.randomUUID();
    await trx('contacts')
      .insert({
        id: newId,
        name,
        phone_raw,
        phone_normalized: normalizedPhone,
        created_by: req.user.id,
        created_at: created_at || new Date()
      });

    const created = await trx('contacts')
      .select('id', 'name', 'phone_raw', 'phone_normalized', 'created_by', 'created_at', 'updated_at')
      .where({ id: newId })
      .first();

    return created;
  });

  res.status(201).json({ contact: newContact });
}));

// POST /contacts/batch
router.post('/batch', validate(batchStructureSchema), asyncHandler(async (req, res) => {
  const { contacts, force_restore } = req.body;

  if (!Array.isArray(contacts) || contacts.length === 0) {
    return res.status(400).json({ error: 'Contacts array is required and must not be empty' });
  }

  if (contacts.length > 1000) {
    return res.status(400).json({ error: 'Maximum 1000 contacts per batch request' });
  }

  const validContacts = [];
  const errors = [];

  for (let i = 0; i < contacts.length; i++) {
    const contact = contacts[i];
    if (!contact.name || !contact.phone_raw) {
      errors.push({ index: i, error: 'Name and phone_raw are required', contact });
      continue;
    }
    try {
      const normalized = normalizePhoneNumber(contact.phone_raw);
      validContacts.push({
        ...contact,
        phone_normalized: normalized,
        created_at: contact.created_at || new Date().toISOString()
      });
    } catch (e) {
      errors.push({ index: i, error: e.message, contact });
    }
  }

  if (validContacts.length === 0) {
    return res.status(200).json({
      results: [],
      errors,
      summary: { total: contacts.length, created: 0, updated: 0, existing: 0, errors: errors.length }
    });
  }

  const rows = await contactsRepository.batchUpsert(validContacts, req.user.id, { restoreDeleted: !!force_restore });

  const results = rows.map(row => ({
    action: row.is_insert ? 'created' : 'updated',
    contact: {
      id: row.id,
      name: row.name,
      phone_raw: row.phone_raw,
      phone_normalized: row.phone_normalized,
      created_by: row.created_by,
      created_at: row.created_at,
      updated_at: row.updated_at
    }
  }));

  const createdCount = results.filter(r => r.action === 'created').length;
  const updatedCount = results.filter(r => r.action === 'updated').length;
  const ignoredCount = validContacts.length - results.length;

  res.status(200).json({
    results,
    errors,
    summary: {
      total: contacts.length,
      created: createdCount,
      updated: updatedCount,
      existing: ignoredCount,
      errors: errors.length
    }
  });
}));

// PUT /contacts/:id
router.put('/:id', validate(updateContactSchema), asyncHandler(async (req, res) => {
  const { id } = req.params;
  const { name, phone_raw, created_by } = req.body;

  const ownershipCheck = await db('contacts').select('created_by').where({ id }).first();

  if (!ownershipCheck) {
    return res.status(404).json({ error: 'Contact not found' });
  }

  if (!checkOwnership({ created_by: ownershipCheck.created_by }, req.user)) {
    return res.status(403).json({ error: 'You can only update your own contacts' });
  }

  const normalizedPhone = normalizePhoneNumber(phone_raw);

  const updates = {
    name,
    phone_raw,
    phone_normalized: normalizedPhone,
    updated_at: db.fn.now()
  };

  if (created_by && req.user.is_admin) {
    updates.created_by = created_by;
  }

  await db('contacts')
    .where({ id })
    .update(updates);

  const updatedContact = await db('contacts')
    .select('id', 'name', 'phone_raw', 'phone_normalized', 'created_by', 'created_at', 'updated_at')
    .where({ id })
    .first();

  res.json({ contact: updatedContact });
}));

// DELETE /contacts/:id
router.delete('/:id', asyncHandler(async (req, res) => {
  const { id } = req.params;

  const contact = await contactsRepository.findById(id);

  if (!contact) {
    return res.status(404).json({ error: 'Contact not found' });
  }

  if (!checkOwnership(contact, req.user)) {
    return res.status(403).json({ error: 'You can only delete your own contacts' });
  }

  await contactsRepository.softDelete(id);

  res.json({ message: 'Contact deleted successfully' });
}));

module.exports = router;
