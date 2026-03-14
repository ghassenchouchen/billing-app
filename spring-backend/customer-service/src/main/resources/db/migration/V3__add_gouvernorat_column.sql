-- Add gouvernorat column to client table
ALTER TABLE client ADD COLUMN gouvernorat VARCHAR(100) DEFAULT NULL AFTER code_postal;
