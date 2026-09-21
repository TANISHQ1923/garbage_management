const { ZodError } = require('zod');

/**
 * Middleware factory validating req.body, req.query, or req.params against a Zod schema.
 * @param {import('zod').ZodSchema} schema - Zod validation schema
 * @param {'body' | 'query' | 'params'} source - Request property to validate (default 'body')
 */
const validateRequest = (schema, source = 'body') => {
  return (req, res, next) => {
    try {
      const validated = schema.parse(req[source]);
      req[source] = validated;
      next();
    } catch (error) {
      if (error instanceof ZodError) {
        const errors = error.errors.map((err) => ({
          field: err.path.join('.'),
          message: err.message
        }));
        return res.status(400).json({
          success: false,
          message: 'Validation failed',
          errors
        });
      }
      next(error);
    }
  };
};

module.exports = { validateRequest };
