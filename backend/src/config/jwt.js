const secret = process.env.JWT_SECRET;

if (!secret && process.env.NODE_ENV === 'production') {
  throw new Error('JWT_SECRET environment variable is required in production');
}

module.exports = {
  secret: secret || 'dev-only-jwt-secret-do-not-use-in-production',
  expiresIn: process.env.JWT_EXPIRES_IN || '30d',
};
