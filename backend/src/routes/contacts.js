const express = require('express');
const pool = require('../config/database');
const { authenticateToken } = require('../middleware/auth');
const { getPaginationParams, getPaginationResult } = require('../utils/pagination');
const { normalizePhoneNumber } = require('../utils/phone');
const { Parser } = require('json2csv');

const router = express.Router();

// All routes require authentication
router.use(authenticateToken);

// Helper to check ownership
const checkOwnership = (contact, user) => {
  if (!contact) return false;
  return contact.created_by === user.id || user.is_admin;
};

// GET /contacts
router.get('/', async (req, res) => {
  try {
    const { page, limit, offset } = getPaginationParams(req);
    const { updated_since } = req.query;

    let whereClause = '';
    let countParams = [];
    let dataParams = [];

    // Delta sync support
    if (updated_since) {
      whereClause = 'WHERE c.updated_at > $1';
      countParams = [new Date(updated_since)];
      dataParams = [new Date(updated_since), limit, offset];
    } else {
      dataParams = [limit, offset];
    }

    const countQuery = `SELECT COUNT(*) FROM contacts c ${whereClause}`;
    const countResult = await pool.query(countQuery, countParams);
    const total = parseInt(countResult.rows[0].count);

    const dataQuery = updated_since
      ? `SELECT c.id, c.name, c.phone_raw, c.phone_normalized, c.created_by, c.created_at, c.updated_at, u.name as creator_name 
         FROM contacts c 
         LEFT JOIN users u ON c.created_by = u.id 
         ${whereClause} 
         ORDER BY c.updated_at DESC LIMIT $2 OFFSET $3`
      : `SELECT c.id, c.name, c.phone_raw, c.phone_normalized, c.created_by, c.created_at, c.updated_at, u.name as creator_name 
         FROM contacts c 
         LEFT JOIN users u ON c.created_by = u.id 
         ORDER BY c.updated_at DESC LIMIT $1 OFFSET $2`;

    const result = await pool.query(dataQuery, dataParams);

    res.json(getPaginationResult(result.rows, total, page, limit));
  } catch (error) {
    console.error('Get contacts error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /contacts/search?q=
router.get('/search', async (req, res) => {
  try {
    const { q } = req.query;
    if (!q || q.trim().length < 2) {
      return res.status(400).json({ error: 'Search query must be at least 2 characters' });
    }

    const { page, limit, offset } = getPaginationParams(req);

    // Use % operator for trigram index efficiency
    const searchQuery = `%${q}%`;

    const countResult = await pool.query(
      'SELECT COUNT(*) FROM contacts WHERE name ILIKE $1 OR phone_raw ILIKE $1',
      [searchQuery]
    );
    const total = parseInt(countResult.rows[0].count);

    const result = await pool.query(
      `SELECT c.id, c.name, c.phone_raw, c.phone_normalized, c.created_by, c.created_at, c.updated_at, u.name as creator_name 
       FROM contacts c 
       LEFT JOIN users u ON c.created_by = u.id 
       WHERE c.name ILIKE $1 OR c.phone_raw ILIKE $1
       ORDER BY c.created_at DESC LIMIT $2 OFFSET $3`,
      [searchQuery, limit, offset]
    );

    res.json(getPaginationResult(result.rows, total, page, limit));
  } catch (error) {
    console.error('Search contacts error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /contacts
router.post('/', async (req, res) => {
  try {
    const { name, phone_raw, phone_normalized, created_at } = req.body;

    if (!name || !phone_raw) {
      return res.status(400).json({ error: 'Name and phone_raw are required' });
    }

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
  } catch (error) {
    console.error('Create contact error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// POST /contacts/batch
router.post('/batch', async (req, res) => {
  try {
    const { contacts } = req.body;

    if (!Array.isArray(contacts) || contacts.length === 0) {
      return res.status(400).json({ error: 'Contacts array is required and must not be empty' });
    }

    if (contacts.length > 1000) { // Increased limit for efficiency
      return res.status(400).json({ error: 'Maximum 1000 contacts per batch request' });
    }

    const client = await pool.connect();
    try {
      await client.query('BEGIN');

      const results = [];
      const errors = [];

      // Prepare data for bulk upsert
      // We process in chunks to avoid query parameter limits if necessary, but for 100-1000 items, loop is okay if optimized.
      // However, true bulk upsert in Postgres is best done with UNNEST or VALUES list.
      // Given the complexity of per-row logic (ownership check), a pure single SQL statement is complex.
      // But we can optimize by fetching all existing phones first.

      // 1. Normalize all phones
      const validContacts = [];
      const phonesToCheck = [];

      for (let i = 0; i < contacts.length; i++) {
        const contact = contacts[i];
        if (!contact.name || !contact.phone_raw) {
          errors.push({ index: i, error: 'Name and phone_raw are required', contact });
          continue;
        }
        try {
          const normalized = normalizePhoneNumber(contact.phone_raw);
          contact.phone_normalized = normalized;
          validContacts.push({ ...contact, originalIndex: i });
          phonesToCheck.push(normalized);
        } catch (e) {
          errors.push({ index: i, error: e.message, contact });
        }
      }

      if (validContacts.length === 0) {
        await client.query('ROLLBACK');
        return res.status(200).json({ results: [], errors, summary: { total: contacts.length, created: 0, updated: 0, existing: 0, errors: errors.length } });
      }

      // 2. Fetch existing contacts in one go
      const existingResult = await client.query(
        'SELECT * FROM contacts WHERE phone_normalized = ANY($1)',
        [phonesToCheck]
      );

      const existingMap = new Map();
      existingResult.rows.forEach(row => existingMap.set(row.phone_normalized, row));

      // 3. Process each contact
      const toInsert = [];
      const toUpdate = [];

      for (const contact of validContacts) {
        const existing = existingMap.get(contact.phone_normalized);
        const incomingCreatedAt = new Date(contact.created_at || new Date());

        if (existing) {
          if (existing.created_by === req.user.id) {
            if (incomingCreatedAt >= new Date(existing.created_at)) {
              toUpdate.push(contact);
            } else {
              results.push({ index: contact.originalIndex, action: 'existing', contact: existing });
            }
          } else {
            errors.push({
              index: contact.originalIndex,
              error: 'Contact with this phone number already exists',
              existing_contact: { id: existing.id, name: existing.name, phone_raw: existing.phone_raw, created_by: existing.created_by }
            });
          }
        } else {
          toInsert.push(contact);
        }
      }

      // 4. Bulk Insert
      if (toInsert.length > 0) {
        // Construct bulk insert query
        // INSERT INTO contacts (...) VALUES (...), (...) RETURNING ...
        const values = [];
        const placeholders = [];
        let paramIdx = 1;

        toInsert.forEach(c => {
          placeholders.push(`($${paramIdx}, $${paramIdx + 1}, $${paramIdx + 2}, $${paramIdx + 3}, $${paramIdx + 4})`);
          values.push(c.name, c.phone_raw, c.phone_normalized, req.user.id, c.created_at || new Date());
          paramIdx += 5;
        });

        const insertQuery = `
            INSERT INTO contacts (name, phone_raw, phone_normalized, created_by, created_at)
            VALUES ${placeholders.join(', ')}
            RETURNING id, name, phone_raw, phone_normalized, created_by, created_at, updated_at
          `;

        const insertResult = await client.query(insertQuery, values);

        insertResult.rows.forEach((row, idx) => {
          // We need to map back to original index. 
          // Since we iterated toInsert in order, we can match by index if we kept track.
          // But simpler is just to push success.
          // Ideally we'd map back, but for now let's just add to results.
          // To be precise, we should have kept the original contact object reference.
          const originalContact = toInsert[idx];
          results.push({ index: originalContact.originalIndex, action: 'created', contact: row });
        });
      }

      // 5. Bulk Update (or individual updates if bulk is too complex for UPDATE FROM VALUES)
      // For simplicity and since updates might be fewer, let's do individual updates or a CASE statement.
      // Individual updates in a transaction are still better than N+1 round trips if we pipeline, 
      // but `await` in loop is slow.
      // Let's use a single query with a VALUES clause for updates if possible, or just loop for now as it's already much better than full N+1 select+insert.
      // Actually, let's use the Upsert pattern for the updates to be safe/fast.

      for (const contact of toUpdate) {
        const updateResult = await client.query(
          'UPDATE contacts SET name = $1, phone_raw = $2, updated_at = NOW() WHERE phone_normalized = $3 AND created_by = $4 RETURNING id, name, phone_raw, phone_normalized, created_by, created_at, updated_at',
          [contact.name, contact.phone_raw, contact.phone_normalized, req.user.id]
        );
        if (updateResult.rows.length > 0) {
          results.push({ index: contact.originalIndex, action: 'updated', contact: updateResult.rows[0] });
        }
      }

      await client.query('COMMIT');

      res.status(200).json({
        results,
        errors,
        summary: {
          total: contacts.length,
          created: results.filter(r => r.action === 'created').length,
          updated: results.filter(r => r.action === 'updated').length,
          existing: results.filter(r => r.action === 'existing').length,
          errors: errors.length
        }
      });

    } catch (error) {
      await client.query('ROLLBACK');
      throw error;
    } finally {
      client.release();
    }
  } catch (error) {
    console.error('Batch create contacts error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// PUT /contacts/:id
router.put('/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const { name, phone_raw, created_by } = req.body;

    if (!name || !phone_raw) {
      return res.status(400).json({ error: 'Name and phone_raw are required' });
    }

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
  } catch (error) {
    console.error('Update contact error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// DELETE /contacts/:id
router.delete('/:id', async (req, res) => {
  try {
    const { id } = req.params;

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
      return res.status(403).json({ error: 'You can only delete your own contacts' });
    }

    const result = await pool.query('DELETE FROM contacts WHERE id = $1 RETURNING id', [id]);

    res.json({ message: 'Contact deleted successfully' });
  } catch (error) {
    console.error('Delete contact error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

// GET /contacts/export - Export all contacts as CSV
router.get('/export', async (req, res) => {
  try {
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
  } catch (error) {
    console.error('Export contacts error:', error);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
