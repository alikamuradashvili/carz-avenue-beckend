-- Align price column with entity expectation (double precision).
ALTER TABLE car_listing ALTER COLUMN price TYPE double precision USING price::double precision;
