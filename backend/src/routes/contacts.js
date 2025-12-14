const express = require('express');
const pool = require('../config/database');
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

// GET /contacts
router.get('/', asyncHandler(async (req, res) => {
  const { page, limit, offset } = getPaginationParams(req);
  const { updated_since, q } = req.query;

  // Search logic merged or separate? 
  // If q is present, use search logic
  const search = (q && q.trim().length >= 2) ? q : null;
  const updatedSince = updated_since ? new Date(updated_since) : null;

  if (search) {
    // Set similarity threshold if searching
    await pool.query('SET LOCAL pg_trgm.similarity_threshold = 0.3');
  }

  const total = await contactsRepository.count({ updatedSince, search });
  const contacts = await contactsRepository.findAll({ limit, offset, updatedSince, search });

  res.json(getPaginationResult(contacts, total, page, limit));
}));

// GET /contacts/search - Legacy/Alias
router.get('/search', asyncHandler(async (req, res) => {
  const { q } = req.query;
  if (!q || q.trim().length < 2) {
    return res.status(400).json({ error: 'Search query must be at least 2 characters' });
  }
  // Redirect to main GET with q param, or just reuse logic
  req.query.q = q; // Ensure q is set
  // Re-dispatch to the main GET /contacts route
  // This effectively makes /contacts/search?q=... an alias for /contacts?q=...
  return router.handle(req, res);
}));

// POST /contacts
router.post('/', validate(createContactSchema), asyncHandler(async (req, res) => {
  const { name, phone_raw, phone_normalized, created_at } = req.body;

  // name and phone_raw are required (validated by Zod)

  // Normalize phone number server-side
  const normalizedPhone = normalizePhoneNumber(phone_raw);

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    // Check if a contact with this phone number already exists
    const existingResult = await client.query(
      'SELECT id, name, phone_raw, phone_normalized, created_by, created_at, updated_at FROM contacts WHERE phone_normalized = $1',
      [normalizedPhone]
    );

    if (existingResult.rows.length > 0) {
      const existing = existingResult.rows[0];
      const incomingCreatedAt = new Date(created_at || new Date());

      // If the existing contact belongs to the same user
      if (existing.created_by === req.user.id) {
        // Update the existing contact with newer data if incoming is newer
        if (incomingCreatedAt >= new Date(existing.created_at)) {
          const updateResult = await client.query(
            'UPDATE contacts SET name = $1, phone_raw = $2, updated_at = NOW() WHERE id = $3 RETURNING id, name, phone_raw, phone_normalized, created_by, created_at, updated_at',
            [name, phone_raw, existing.id]
          );
          await client.query('COMMIT');
          return res.json({ contact: updateResult.rows[0] });
        } else {
          // Return existing contact
          await client.query('COMMIT');
          return res.json({ contact: existing });
        }
      } else {
        // Contact exists but belongs to different user
        // Return conflict error - don't allow duplicate contacts across users
        await client.query('COMMIT');
        return res.status(409).json({
          error: 'Contact with this phone number already exists',
          existing_contact: {
            id: existing.id,
            name: existing.name,
            phone_raw: existing.phone_raw,
            created_by: existing.created_by,
            created_at: existing.created_at
          }
        });
      }
    }

    // No existing contact found, create new one
    const result = await client.query(
      'INSERT INTO contacts (name, phone_raw, phone_normalized, created_by, created_at) VALUES ($1, $2, $3, $4, $5) RETURNING id, name, phone_raw, phone_normalized, created_by, created_at, updated_at',
      [name, phone_raw, normalizedPhone, req.user.id, created_at || new Date()]
    );

    await client.query('COMMIT');
    return res.status(201).json({ contact: result.rows[0] });

  } catch (error) {
    await client.query('ROLLBACK');
    throw error;
  } finally {
    client.release();
  }
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

  // 1. Prepare valid contacts
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

  // 2. Execute single Upsert query via Repository
  const rows = await contactsRepository.batchUpsert(validContacts, req.user.id, { restoreDeleted: !!force_restore });

  // 3. Process results
  const results = rows.map(row => ({
    // Note: We lose the original index mapping with this approach unless we include it in the query.
    // But for sync, the client usually just needs to know it succeeded.
    // If strict index mapping is required, we'd need to pass the index through.
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

  // Calculate summary
  const createdCount = results.filter(r => r.action === 'created').length;
  const updatedCount = results.filter(r => r.action === 'updated').length;
  // "Existing" in this context means "Ignored due to WHERE clause" (older data or different owner)
  // The RETURNING clause only returns rows that were actually inserted or updated.
  // So (Total Input - Errors - Returned Rows) = Ignored/Existing
  const ignoredCount = validContacts.length - results.length;

  res.status(200).json({
    results, // Only contains actually changed/inserted contacts
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

  // Validated by Zod

  // Check ownership
  const ownershipCheck = await pool.query(
    'SELECT created_by FROM contacts WHERE id = $1',
    [id]
  );

  if (ownershipCheck.rows.length === 0) {
    return res.status(404).json({ error: 'Contact not found' });
  }

  // Check ownership
  if (!checkOwnership({ created_by: ownershipCheck.rows[0].created_by }, req.user)) {
    return res.status(403).json({ error: 'You can only update your own contacts' });
  }

  // Normalize phone
  const normalizedPhone = normalizePhoneNumber(phone_raw);

  let query = 'UPDATE contacts SET name = $1, phone_raw = $2, phone_normalized = $3, updated_at = NOW()';
  let params = [name, phone_raw, normalizedPhone];
  let paramIndex = 4;

  // Allow admin to change creator
  if (created_by && req.user.is_admin) {
    query += `, created_by = $${paramIndex}`;
    params.push(created_by);
    paramIndex++;
  }

  query += ` WHERE id = $${paramIndex} RETURNING id, name, phone_raw, phone_normalized, created_by, created_at, updated_at`;
  params.push(id);

  const result = await pool.query(query, params);

  res.json({ contact: result.rows[0] });
}));

// DELETE /contacts/:id
router.delete('/:id', asyncHandler(async (req, res) => {
  const { id } = req.params;

  // Check ownership
  const contact = await contactsRepository.findById(id);

  if (!contact) {
    return res.status(404).json({ error: 'Contact not found' });
  }

  // Check ownership
  if (!checkOwnership(contact, req.user)) {
    return res.status(403).json({ error: 'You can only delete your own contacts' });
  }

  await contactsRepository.softDelete(id);

  res.json({ message: 'Contact deleted successfully' });
}));

// GET /contacts/export - Export all contacts as CSV
router.get('/export', asyncHandler(async (req, res) => {
  // Fetch all contacts with user information
  const result = await pool.query(`
    SELECT c.name, c.phone_raw, u.name as creator_name, c.created_at
    FROM contacts c
    LEFT JOIN users u ON c.created_by = u.id
    ORDER BY c.created_at DESC
  `);

  const fields = [
    { label: 'Name', value: 'name' },
    { label: 'Phone', value: 'phone_raw' },
    { label: 'Created By', value: 'creator_name' },
    { label: 'Created At', value: (row) => new Date(row.created_at).toLocaleDateString() }
  ];

  const json2csvParser = new Parser({ fields });
  const csv = json2csvParser.parse(result.rows);

  // Set headers for CSV download
  res.setHeader('Content-Type', 'text/csv');
  res.setHeader('Content-Disposition', 'attachment; filename="contacts.csv"');
  res.send(csv);
}));

module.exports = router;
