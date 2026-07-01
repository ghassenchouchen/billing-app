-- V3: Add email, set_password_token, and transition status to VARCHAR to support PENDING_PASSWORD
ALTER TABLE users ADD COLUMN email VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN set_password_token VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN set_password_token_expires_at DATETIME NULL;

-- Update existing default users
UPDATE users SET email = 'admin@telecom-billing.tn' WHERE username = 'admin';
UPDATE users SET email = 'agent@telecom-billing.tn' WHERE username = 'agent';

-- Make status column VARCHAR to avoid tight enum restrictions in database
ALTER TABLE users MODIFY COLUMN status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE';
