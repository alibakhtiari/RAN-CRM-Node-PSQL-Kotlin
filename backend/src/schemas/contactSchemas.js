const { z } = require('zod');

const contactSchema = z.object({
    name: z.string().min(1, 'Name is required'),
    phone_raw: z.string().min(3, 'Phone number is required'),
    created_at: z.string().datetime().optional().or(z.string()), // Accept ISO string
    created_by: z.string().uuid().optional()
});

const createContactSchema = z.object({
    body: contactSchema
});

const updateContactSchema = z.object({
    params: z.object({
        id: z.string().uuid('Invalid Contact ID')
    }),
    body: contactSchema
});

const batchStructureSchema = z.object({
    body: z.object({
        contacts: z.array(z.any()).min(1).max(1000),
        force_restore: z.boolean().optional()
    })
});

module.exports = {
    createContactSchema,
    updateContactSchema,
    batchStructureSchema
};
