-- Create default admin user
-- Password: admin123 (hashed with bcrypt, 12 rounds)
INSERT INTO users (name, email, password_hash, is_admin)
VALUES (
  'Admin User',
  'admin@example.com',
  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj8lZQKQFQy',
  true
)
ON CONFLICT (email) DO NOTHING;
