-- Align database id columns with entity definitions (BIGINT).

-- Users
ALTER TABLE users ALTER COLUMN id TYPE BIGINT;
ALTER SEQUENCE users_id_seq AS BIGINT;

-- Car listings
ALTER TABLE car_listing ALTER COLUMN id TYPE BIGINT;
ALTER TABLE car_listing ALTER COLUMN owner_id TYPE BIGINT;
ALTER SEQUENCE car_listing_id_seq AS BIGINT;

-- Car photos
ALTER TABLE car_photos ALTER COLUMN car_id TYPE BIGINT;

-- Refresh tokens
ALTER TABLE refresh_token ALTER COLUMN id TYPE BIGINT;
ALTER TABLE refresh_token ALTER COLUMN user_id TYPE BIGINT;
ALTER SEQUENCE refresh_token_id_seq AS BIGINT;
