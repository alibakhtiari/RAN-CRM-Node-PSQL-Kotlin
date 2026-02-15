const { z } = require('zod');
const AppError = require('../utils/AppError');

const validate = (schema) => async (req, res, next) => {
    try {
        if (!schema) {
            return next(new Error('Validation schema is missing'));
        }

        await schema.parseAsync({
            body: req.body,
            query: req.query,
            params: req.params,
        });
        next();
    } catch (error) {
        if (error instanceof z.ZodError) {
            const messages = error.errors.map(e => {
                const path = e.path ? e.path.join('.') : 'unknown';
                return `${path} - ${e.message}`;
            }).join('; ');
            return next(new AppError(`Validation Error: ${messages}`, 400));
        }
        next(error);
    }
};

module.exports = validate;
