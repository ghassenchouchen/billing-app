-- Create all databases for the microservices
CREATE DATABASE IF NOT EXISTS db_customer;
CREATE DATABASE IF NOT EXISTS db_catalog;
CREATE DATABASE IF NOT EXISTS db_subscription;
CREATE DATABASE IF NOT EXISTS db_usage;
CREATE DATABASE IF NOT EXISTS db_billing;
CREATE DATABASE IF NOT EXISTS db_payment;
CREATE DATABASE IF NOT EXISTS db_authentication;
CREATE DATABASE IF NOT EXISTS db_boutique;

-- Grant privileges (root@localhost already has all privileges,
-- so we only need this for Docker/remote setups)
-- Wrapped in a safe way that won't fail if user doesn't exist
GRANT ALL PRIVILEGES ON db_customer.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON db_catalog.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON db_subscription.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON db_usage.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON db_billing.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON db_payment.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON db_authentication.* TO 'root'@'localhost';
GRANT ALL PRIVILEGES ON db_boutique.* TO 'root'@'localhost';

FLUSH PRIVILEGES;
