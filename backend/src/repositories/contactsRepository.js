const crypto = require('crypto');
const db = require('../config/knex');

/**
 * Escape special LIKE metacharacters (%, _) in user input
 */
const escapeLike = (str) => str.replace(/[%_\\]/g, '\\$&');

class ContactsRepository {
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
      .whereNull('c.deleted_at');

    if (updatedSince) {
      query = query.where('c.updated_at', '>', updatedSince);
    }

    if (search) {
      const escaped = escapeLike(search);
      query = query.where(qb => {
        qb.where('c.name', 'like', `%${escaped}%`)
          .orWhere('c.phone_raw', 'like', `%${escaped}%`);
      });
      // Relevance ordering: prefix matches first
      query = query.orderByRaw('CASE WHEN c.name LIKE ? THEN 0 ELSE 1 END ASC', [`${escaped}%`]);
    }

    // Default ordering + pagination AFTER search ordering
    query = query.orderBy('c.updated_at', 'desc')
      .limit(limit)
      .offset(offset);

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
      const escaped = escapeLike(search);
      query = query.where(qb => {
        qb.where('c.name', 'like', `%${escaped}%`)
          .orWhere('c.phone_raw', 'like', `%${escaped}%`);
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

    const restoreDeleted = options.restoreDeleted || false;

    return db.transaction(async (trx) => {
      const results = [];

      for (const contact of contacts) {
        const createdBy = contact.created_by || userId;
        const createdAt = contact.created_at ? new Date(contact.created_at) : new Date();

        // Check if contact exists by phone_normalized
        const existing = await trx('contacts')
          .where({ phone_normalized: contact.phone_normalized })
          .first();

        if (existing) {
          // Only update if owned by same user AND incoming is newer
          if (existing.created_by === createdBy) {
            const incomingDate = new Date(createdAt);
            const existingDate = new Date(existing.created_at);

            if (incomingDate > existingDate) {
              const updateData = {
                name: contact.name,
                phone_raw: contact.phone_raw,
                updated_at: trx.fn.now()
              };

              if (restoreDeleted && existing.deleted_at) {
                updateData.deleted_at = null;
              }

              await trx('contacts')
                .where({ id: existing.id })
                .update(updateData);

              const updated = await trx('contacts')
                .where({ id: existing.id })
                .first();
              updated.is_insert = false;
              results.push(updated);
            } else {
              // existing is newer, skip update but return in results
              existing.is_existing = true;
              results.push(existing);
            }
          } else {
            // different user owns this contact, skip update but return in results
            existing.is_existing = true;
            results.push(existing);
          }
          // Insert new contact
          const newId = crypto.randomUUID();
          await trx('contacts')
            .insert({
              id: newId,
              name: contact.name,
              phone_raw: contact.phone_raw,
              phone_normalized: contact.phone_normalized,
              created_by: createdBy,
              created_at: createdAt
            });

          const inserted = await trx('contacts')
            .where({ id: newId })
            .first();
          inserted.is_insert = true;
          results.push(inserted);
        }
      }

      return results;
    });
  }
}

module.exports = new ContactsRepository();
