exports.up = function (knex) {
    return knex.schema.alterTable('contacts', function (table) {
        table.timestamp('deleted_at').nullable();
        table.index(['created_by']);
    });
};

exports.down = function (knex) {
    return knex.schema.alterTable('contacts', function (table) {
        table.dropColumn('deleted_at');
        table.dropIndex(['created_by']);
    });
};
