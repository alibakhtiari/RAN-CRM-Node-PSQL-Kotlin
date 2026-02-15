-- Create default admin user
-- Username: admin, Password: admin123 (hashed with bcrypt, 12 rounds)
INSERT IGNORE INTO users (username, name, password_hash, is_admin)
VALUES (
    'admin',
    'Admin User',
    '$2a$12$CXhuc9o2xuY2xQIJqDwd1.0D3zc.jk6Vrh6GQ6b4xeSVfgxOqBMKO',
    TRUE
  );
-- Create default normal user
-- Username: user, Password: user123 (hashed with bcrypt, 12 rounds)
INSERT IGNORE INTO users (username, name, password_hash, is_admin)
VALUES (
    'user',
    'Normal User',
    '$2a$12$nXvyTzQ/c4qVhz80hfYOkuw0ZjFSbLX6ptbAFUN3ck/SCCxEyScqO',
    FALSE
  );