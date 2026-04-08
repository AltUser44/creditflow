CREATE DATABASE users_db;
CREATE DATABASE transactions_db;
CREATE DATABASE credit_db;

GRANT ALL PRIVILEGES ON DATABASE users_db TO creditflow;
GRANT ALL PRIVILEGES ON DATABASE transactions_db TO creditflow;
GRANT ALL PRIVILEGES ON DATABASE credit_db TO creditflow;
