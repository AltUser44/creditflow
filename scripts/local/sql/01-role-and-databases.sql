-- Run as PostgreSQL superuser (e.g. postgres) against database "postgres".
-- Creates application role only. Databases are created by setup-postgres.ps1 / .sh (idempotent).

DO $$
BEGIN
  IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'creditflow') THEN
    CREATE ROLE creditflow LOGIN PASSWORD 'creditflow';
  END IF;
END$$;
