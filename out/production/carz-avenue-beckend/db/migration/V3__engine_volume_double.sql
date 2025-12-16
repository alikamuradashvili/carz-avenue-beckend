-- Align engine_volume column with entity expectation (double precision).
ALTER TABLE car_listing ALTER COLUMN engine_volume TYPE double precision USING engine_volume::double precision;
