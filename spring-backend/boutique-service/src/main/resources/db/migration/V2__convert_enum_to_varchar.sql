

-- Modify boutique.status from ENUM to VARCHAR(50)
ALTER TABLE boutique 
    MODIFY COLUMN status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';


