SET search_path TO public;

ALTER TABLE transport_request
    ADD COLUMN IF NOT EXISTS pickup_district VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_transport_request_pickup_district
    ON transport_request(pickup_district);

COMMENT ON COLUMN transport_request.pickup_district IS
'Normalized pickup district captured at transport request creation time and used for district performance reporting.';
