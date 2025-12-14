const { z } = require('zod');
const AppError = require('../utils/AppError');

const validate = (schema) => (req, res, next) => {
    try {
        schema.parse({
            body: req.body,
            query: req.query,
            params: req.params,
        });
        next();
    } catch (error) {
        if (error instanceof z.ZodError) {
            const messages = error.errors.map(e => `${e.path.join('.')} - ${e.message}`).join('; ');
            return next(new AppError(`Validation Error: ${messages}`, 400));
        }
        next(error);
    }
};

module.exports = validate;
