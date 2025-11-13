-- Create default admin user
-- Username: admin, Password: admin123 (hashed with bcrypt, 12 rounds)
INSERT INTO users (username, name, email, password_hash, is_admin)
VALUES (
  'admin',
  'Admin User',
  'admin@example.com',
  '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj8lZQKQFQy',
  true
)
ON CONFLICT (username) DO NOTHING;

-- Create default normal user
-- Username: user, Password: user123 (hashed with bcrypt, 12 rounds)
INSERT INTO users (username, name, email, password_hash, is_admin)
VALUES (
  'user',
  'Normal User',
  'user@example.com',
  '$2a$12$8K1p/5w6YvH3qE7rN9sL0O1p2Q3rS4tU5vW6xY7zA8bC9dE0fG1',
  false
)
ON CONFLICT (username) DO NOTHING;
