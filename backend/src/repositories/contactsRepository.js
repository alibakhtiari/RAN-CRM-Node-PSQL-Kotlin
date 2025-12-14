const db = require('../config/knex');

class ContactsRepository {
  /**
   * Batch upsert contacts.
   * @param {Array} contacts - Array of contact objects { name, phone_raw, phone_normalized, created_at }
   * @param {string} userId - ID of the user creating the contacts
   * @returns {Promise<Array>} - Array of upserted rows
   */
  /**
   * Find contacts with pagination and filtering
   */
  async findAll({ limit, offset, updatedSince, search }) {
    let query = db('contacts as c')
      .leftJoin('users as u', 'c.created_by', 'u.id')
      .select(
        'c.id',
        'c.name',
        'c.phone_raw',
        'c.phone_normalized',
        'c.created_by',
        'c.created_at',
        'c.updated_at',
        'u.name as creator_name'
      )
      .whereNull('c.deleted_at')
      .orderBy('c.updated_at', 'desc')
      .limit(limit)
      .offset(offset);

    if (updatedSince) {
      query = query.where('c.updated_at', '>', updatedSince);
    }

    if (search) {
      // Use raw for trigram similarity or ILIKE
      query = query.where(qb => {
        qb.whereRaw('c.name % ?', [search])
          .orWhere('c.phone_raw', 'ilike', `%${search}%`);
      });
      // Adjust order for search
      query = query.orderByRaw('c.name <-> ? ASC', [search]);
    }

    return query;
  }

  /**
   * Count contacts matching filter
   */
  async count({ updatedSince, search }) {
    let query = db('contacts as c').count('* as count').whereNull('c.deleted_at');

    if (updatedSince) {
      query = query.where('c.updated_at', '>', updatedSince);
    }

    if (search) {
      query = query.where(qb => {
        qb.whereRaw('c.name % ?', [search])
          .orWhere('c.phone_raw', 'ilike', `%${search}%`);
      });
    }

    const result = await query.first();
    return parseInt(result.count);
  }

  async findById(id) {
    return db('contacts').where({ id }).first();
  }

  async softDelete(id) {
    return db('contacts')
      .where({ id })
      .update({ deleted_at: db.fn.now() });
  }

  async batchUpsert(contacts, userId, options = {}) {
    if (!contacts.length) return [];

    const names = contacts.map(c => c.name);
    const phonesRaw = contacts.map(c => c.phone_raw);
    const phonesNormalized = contacts.map(c => c.phone_normalized);
    const createdAts = contacts.map(c => c.created_at || new Date().toISOString());
    // Use provided created_by (UUID) if valid, otherwise fallback to the acting user's ID
    const userIds = contacts.map(c => c.created_by || userId);

    const restoreClause = options.restoreDeleted ? 'deleted_at = NULL,' : '';

    // Using raw SQL for precise UNNEST and Conditional ON CONFLICT support
    const query = `
      INSERT INTO contacts (name, phone_raw, phone_normalized, created_by, created_at)
      SELECT * FROM UNNEST(?::text[], ?::text[], ?::text[], ?::uuid[], ?::timestamptz[])
      ON CONFLICT (phone_normalized) 
      DO UPDATE SET 
        name = EXCLUDED.name,
        phone_raw = EXCLUDED.phone_raw,
        ${restoreClause}
        updated_at = NOW()
        -- deleted_at = NULL -- Prevent reviving soft-deleted contacts during sync for now
      WHERE contacts.created_by = EXCLUDED.created_by -- Only update if owned by same user
        AND EXCLUDED.created_at > contacts.created_at -- Only update if incoming is newer
      RETURNING id, name, phone_raw, phone_normalized, created_by, created_at, updated_at, (xmax = 0) AS is_insert;
    `;

    const result = await db.raw(query, [
      names,
      phonesRaw,
      phonesNormalized,
      userIds,
      createdAts
    ]);

    return result.rows;
  }
}

module.exports = new ContactsRepository();
