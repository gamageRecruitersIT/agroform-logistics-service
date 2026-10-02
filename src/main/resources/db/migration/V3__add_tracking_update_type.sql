-- =========================================
-- V3__add_tracking_update_type.sql
-- Logistics Service - Delivery Status & Tracking (Dilum)
-- Adds update_type to tracking_update so one table can hold status changes,
-- live GPS pings, delay flag/resolve events and warehouse acknowledgements.
-- =========================================

SET search_path TO public;

ALTER TABLE tracking_update
    ADD COLUMN IF NOT EXISTS update_type VARCHAR(30) NOT NULL DEFAULT 'STATUS_CHANGE';

-- Back-fill rows written before this migration
UPDATE tracking_update SET update_type = 'INITIALIZED' WHERE previous_status IS NULL;
UPDATE tracking_update
   SET update_type = CASE WHEN is_delayed THEN 'DELAY_FLAGGED' ELSE 'DELAY_RESOLVED' END
 WHERE previous_status IS NOT NULL AND previous_status = new_status;

ALTER TABLE tracking_update
    ADD CONSTRAINT chk_tracking_update_type
    CHECK (update_type IN ('INITIALIZED','STATUS_CHANGE','LOCATION','DELAY_FLAGGED','DELAY_RESOLVED','WAREHOUSE_ACK'));

CREATE INDEX IF NOT EXISTS idx_tracking_update_task_type_recorded_at
    ON tracking_update(transport_task_id, update_type, recorded_at DESC);

COMMENT ON COLUMN tracking_update.update_type IS
'INITIALIZED, STATUS_CHANGE, LOCATION (live GPS ping), DELAY_FLAGGED, DELAY_RESOLVED or WAREHOUSE_ACK. LOCATION rows have previous_status = new_status = the current status.';
