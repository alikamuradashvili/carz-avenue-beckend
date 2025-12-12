-- Run this in psql (e.g. \i db/create_database.sql)
-- Adjust owner/password as needed before running.

-- 1) Create database (skip if it already exists)
CREATE DATABASE carz_avenue;

-- 2) Connect to it
\connect carz_avenue;

-- 3) Schema (matches Flyway V1__init.sql)
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone_number VARCHAR(50),
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS car_listing (
    id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL REFERENCES users(id),
    title VARCHAR(255) NOT NULL,
    make VARCHAR(100) NOT NULL,
    model VARCHAR(100) NOT NULL,
    year INTEGER,
    mileage INTEGER,
    fuel_type VARCHAR(50),
    transmission VARCHAR(50),
    body_type VARCHAR(50),
    engine_volume NUMERIC(10,2),
    color VARCHAR(50),
    price NUMERIC(12,2),
    description TEXT,
    location VARCHAR(255),
    photos TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    is_vip BOOLEAN NOT NULL DEFAULT FALSE,
    vip_expires_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS car_photos (
    car_id INTEGER NOT NULL REFERENCES car_listing(id) ON DELETE CASCADE,
    url TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS refresh_token (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id),
    token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Optional: seed an admin user (replace password hash)
-- INSERT INTO users (email, password_hash, name, role) VALUES
-- ('admin@example.com', '$2a$10$replace_with_bcrypt_hash', 'Admin', 'ADMIN');
