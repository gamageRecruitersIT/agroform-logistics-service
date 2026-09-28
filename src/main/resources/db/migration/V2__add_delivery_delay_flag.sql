-- =========================================
-- V2__add_delivery_delay_flag.sql
-- Logistics Service
-- Adds DELAYED-flag support to delivery_status and tracking_update,
-- per Dilum's Delivery Status & Tracking module (FR-13 delay handling).
--
-- DELAYED is modeled as a boolean flag rather than a new value in
-- delivery_status_enum: a task can be delayed while still sitting at any
-- lifecycle status (e.g. IN_TRANSIT + delayed), so a flag composes with the
-- existing status instead of interrupting the forward progression.
-- =========================================

SET search_path TO public;

ALTER TABLE delivery_status
    ADD COLUMN IF NOT EXISTS is_delayed  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS delay_reason TEXT,
    ADD COLUMN IF NOT EXISTS delayed_at   TIMESTAMPTZ;

ALTER TABLE tracking_update
    ADD COLUMN IF NOT EXISTS is_delayed   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS delay_reason TEXT;

CREATE INDEX IF NOT EXISTS idx_delivery_status_is_delayed
    ON delivery_status(is_delayed);

CREATE INDEX IF NOT EXISTS idx_tracking_update_is_delayed
    ON tracking_update(is_delayed);

COMMENT ON COLUMN delivery_status.is_delayed IS
'TRUE when the task is currently flagged as delayed. Independent of current_status; enables downstream delay notifications (Communication & Support Service) and Kafka delay events without adding a DELAYED value to delivery_status_enum.';

COMMENT ON COLUMN delivery_status.delay_reason IS
'Free-text reason supplied when the delay flag was raised. Cleared when the delay is resolved.';

COMMENT ON COLUMN delivery_status.delayed_at IS
'Timestamp when the delay flag was last raised. Null when not currently delayed.';

COMMENT ON COLUMN tracking_update.is_delayed IS
'Snapshot of the delay flag at the time this history row was recorded. A row with previous_status = new_status represents a pure delay flag/resolve event.';

COMMENT ON COLUMN tracking_update.delay_reason IS
'Snapshot of the delay reason at the time this history row was recorded.';
