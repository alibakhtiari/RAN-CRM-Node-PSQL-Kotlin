const { z } = require('zod');
const AppError = require('../utils/AppError');

const validate = (schema) => async (req, res, next) => {
    try {
        if (!schema) {
            console.error('Validate middleware called with undefined schema');
            return next(new Error('Validation schema is missing'));
        }
        console.log('Validating request against schema:', {
            bodyKeys: Object.keys(req.body || {}),
            schemaKeys: schema.shape ? Object.keys(schema.shape) : 'unknown'
        });

        await schema.parseAsync({
            body: req.body,
            query: req.query,
            params: req.params,
        });
        next();
    } catch (error) {
        console.error('Validation Error Details:', error);
        if (error instanceof z.ZodError) {
            const messages = error.errors.map(e => {
                const path = e.path ? e.path.join('.') : 'unknown';
                return `${path} - ${e.message}`;
            }).join('; ');
            console.error('Formatted Validation Errors:', messages);
            return next(new AppError(`Validation Error: ${messages}`, 400));
        }
        next(error);
    }
};

module.exports = validate;
