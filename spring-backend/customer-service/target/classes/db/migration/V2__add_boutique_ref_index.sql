-- Add boutique_ref column if it doesn't exist, then create index

-- Add column if missing (idempotent operation)
SET @dbname = DATABASE();
SET @tablename = 'client';
SET @columnname = 'boutique_ref';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
   WHERE (TABLE_SCHEMA = @dbname)
     AND (TABLE_NAME = @tablename)
     AND (COLUMN_NAME = @columnname)
  ) > 0,
  'SELECT 1',
  CONCAT('ALTER TABLE ', @tablename, ' ADD COLUMN ', @columnname, ' VARCHAR(255) NULL AFTER customer_ref')
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Add index if it doesn't exist
SET @indexExists = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS 
                    WHERE TABLE_SCHEMA = @dbname 
                      AND TABLE_NAME = @tablename 
                      AND INDEX_NAME = 'idx_client_boutique_ref');
SET @createIndex = IF(@indexExists > 0, 
                      'SELECT 1', 
                      'CREATE INDEX idx_client_boutique_ref ON client(boutique_ref)');
PREPARE createIndexIfNotExists FROM @createIndex;
EXECUTE createIndexIfNotExists;
DEALLOCATE PREPARE createIndexIfNotExists;
