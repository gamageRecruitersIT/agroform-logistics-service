-- =========================================
-- V1__create_logistics_service_tables.sql
-- Logistics Service
-- Responsibilities: Vehicle Registration & Management, Driver-Vehicle Assignment,
--                   Transport Request Handling, Transport Cost Calculation,
--                   Delivery Status Tracking, Route Estimation,
--                   Logistics Audit Events
--                   Base API Path: /api/v1/logistics
-- =========================================

SET search_path TO public;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- =========================================
-- ENUMS
-- Plain CREATE TYPE is used here. Flyway versioned migrations run exactly once,
-- so duplicate type errors will not occur under normal operation.
-- =========================================



CREATE TYPE vehicle_type_enum AS ENUM (
            'TRUCK',
            'LORRY',
            'VAN',
            'PICKUP',
            'MOTORCYCLE',
            'OTHER'
        );



CREATE TYPE vehicle_availability_enum AS ENUM (
            'AVAILABLE',
            'ASSIGNED',
            'UNDER_MAINTENANCE',
            'INACTIVE'
        );



CREATE TYPE transport_request_status_enum AS ENUM (
            'PENDING',
            'ACCEPTED',
            'REJECTED',
            'CANCELLED',
            'COMPLETED'
        );



CREATE TYPE transport_task_status_enum AS ENUM (
            'ASSIGNED',
            'IN_PROGRESS',
            'COMPLETED',
            'CANCELLED'
        );



CREATE TYPE delivery_status_enum AS ENUM (
            'AWAITING_PICKUP',
            'LOADED',
            'IN_TRANSIT',
            'DELIVERED',
            'UNLOADED_AT_WAREHOUSE'
        );



CREATE TYPE logistics_event_type_enum AS ENUM (
            'VEHICLE_REGISTERED',
            'VEHICLE_UPDATED',
            'DRIVER_ASSIGNED_TO_VEHICLE',
            'TRANSPORT_REQUEST_CREATED',
            'TRANSPORT_REQUEST_ACCEPTED',
            'TRANSPORT_REQUEST_REJECTED',
            'TRANSPORT_REQUEST_CANCELLED',
            'DRIVER_ASSIGNED_TO_TASK',
            'DELIVERY_STATUS_UPDATED',
            'DELIVERY_COMPLETED',
            'TASK_CANCELLED'
        );



-- =========================================
-- TABLES
-- =========================================

-- =====================
-- VEHICLE
-- Registered transport vehicle owned and managed by a transporter.
-- transporter_id is a plain UUID cross-service reference to Identity Service — no FK constraint.
-- vehicle_code is the UI-friendly code shown to frontend and mobile users instead of the UUID.
-- vehicle_number_plate and engine_number are enforced as unique because no two real vehicles
-- share these identifiers — duplicates would indicate a data entry error.
-- capacity_kg stores vehicle load capacity in kilograms; used in transport cost estimation.
-- operation_area is a free-text description of the geographic area the vehicle covers.
-- =====================
CREATE TABLE IF NOT EXISTS vehicle (
    vehicle_id           UUID                      NOT NULL DEFAULT gen_random_uuid(),
    vehicle_code         VARCHAR(20)               NOT NULL DEFAULT ('VHC-' || UPPER(encode(gen_random_bytes(4), 'hex'))),

    transporter_id       UUID                      NOT NULL,

    vehicle_number_plate VARCHAR(20)               NOT NULL,
    engine_number        VARCHAR(50)               NOT NULL,
    vehicle_type         vehicle_type_enum         NOT NULL,
    capacity_kg          NUMERIC(10,2)             NOT NULL,
    operation_area       VARCHAR(255)              NOT NULL,

    availability_status  vehicle_availability_enum NOT NULL DEFAULT 'AVAILABLE',
    is_active            BOOLEAN                   NOT NULL DEFAULT TRUE,

    created_at           TIMESTAMPTZ               NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ               NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_vehicle
    PRIMARY KEY (vehicle_id),

    CONSTRAINT uq_vehicle_code
    UNIQUE (vehicle_code),

    CONSTRAINT uq_vehicle_number_plate
    UNIQUE (vehicle_number_plate),

    CONSTRAINT uq_vehicle_engine_number
    UNIQUE (engine_number),

    CONSTRAINT chk_vehicle_code_not_blank
    CHECK (LENGTH(TRIM(vehicle_code)) > 0),

    CONSTRAINT chk_vehicle_number_plate_not_blank
    CHECK (LENGTH(TRIM(vehicle_number_plate)) > 0),

    CONSTRAINT chk_vehicle_engine_number_not_blank
    CHECK (LENGTH(TRIM(engine_number)) > 0),

    CONSTRAINT chk_vehicle_capacity_positive
    CHECK (capacity_kg > 0),

    CONSTRAINT chk_vehicle_operation_area_not_blank
    CHECK (LENGTH(TRIM(operation_area)) > 0)
    );

-- =====================
-- VEHICLE PHOTO
-- Photos attached to a registered vehicle.
-- A vehicle can have multiple photos; only one can be marked as primary,
-- enforced by a partial unique index on (vehicle_id) WHERE is_primary = TRUE.
-- display_order controls the rendering sequence in the UI.
-- =====================
CREATE TABLE IF NOT EXISTS vehicle_photo (
    photo_id      UUID         NOT NULL DEFAULT gen_random_uuid(),
    vehicle_id    UUID         NOT NULL,

    photo_url     VARCHAR(500) NOT NULL,
    is_primary    BOOLEAN      NOT NULL DEFAULT FALSE,
    display_order INTEGER      NOT NULL DEFAULT 0,

    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_vehicle_photo
    PRIMARY KEY (photo_id),

    CONSTRAINT fk_vehicle_photo_vehicle
    FOREIGN KEY (vehicle_id)
    REFERENCES vehicle(vehicle_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_vehicle_photo_url_not_blank
    CHECK (LENGTH(TRIM(photo_url)) > 0),

    CONSTRAINT chk_vehicle_photo_display_order_non_negative
    CHECK (display_order >= 0)
    );

-- =====================
-- TRANSPORT COST RULE
-- Defines the per-kilometre cost rate set by a transporter.
-- transporter_id is a plain UUID cross-service reference to Identity Service — no FK constraint.
-- Only the active rule (is_active = TRUE) is used for cost estimation at request time.
-- A transporter may update their rate by deactivating the old rule and creating a new one,
-- preserving the rate history without deleting records that past estimates may reference.
-- effective_from records when this rate came into effect for audit and reporting purposes.
-- =====================
CREATE TABLE IF NOT EXISTS transport_cost_rule (
    cost_rule_id   UUID          NOT NULL DEFAULT gen_random_uuid(),
    transporter_id UUID          NOT NULL,

    cost_per_km    NUMERIC(10,2) NOT NULL,
    cost_per_kg    NUMERIC(10,2),
    is_active      BOOLEAN       NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    created_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_transport_cost_rule
    PRIMARY KEY (cost_rule_id),

    CONSTRAINT chk_transport_cost_rule_cost_per_km_positive
    CHECK (cost_per_km > 0),

    CONSTRAINT chk_transport_cost_rule_cost_per_kg_non_negative
    CHECK (cost_per_kg IS NULL OR cost_per_kg >= 0)
    );

-- =====================
-- TRANSPORT REQUEST
-- Created by a farmer after their auction ends and the winning order is confirmed.
-- transport_request_code is the UI-friendly code shown to farmers, transporters, and drivers.
-- farmer_id is a plain UUID cross-service reference to Identity Service — no FK constraint.
-- order_id is a plain UUID cross-service reference to Order & Payment Service — no FK constraint.
-- auction_ref_code is a plain VARCHAR reference to auction_id_record.auction_ref_code
--   in Marketplace Service — no FK constraint. Stored so Logistics Service can verify the
--   related order with Order & Payment Service via Feign Client without re-fetching from Marketplace.
-- product_name, quantity, and quantity_unit are denormalized from the order payload
--   to avoid Feign calls back to Order & Payment Service on every request read.
-- pickup_latitude and pickup_longitude capture the farmer's farm location at the time of request;
--   sourced from the farmer's registered farm field in Identity Service.
-- warehouse_id is a plain UUID cross-service reference to Warehouse & Inventory Service — no FK constraint.
-- warehouse_latitude and warehouse_longitude are denormalized from the target warehouse
--   to avoid Feign calls during route calculation.
-- estimated_distance_km is calculated at request creation time using the GPS coordinates
--   of the farm and the target warehouse via an external mapping API.
-- =====================
CREATE TABLE IF NOT EXISTS transport_request (
    transport_request_id   UUID                          NOT NULL DEFAULT gen_random_uuid(),
    transport_request_code VARCHAR(20)                   NOT NULL DEFAULT ('TRQ-' || UPPER(encode(gen_random_bytes(4), 'hex'))),

    farmer_id              UUID                          NOT NULL,
    order_id               UUID                          NOT NULL,
    auction_ref_code       VARCHAR(30)                   NOT NULL,

    product_name           VARCHAR(150)                  NOT NULL,
    quantity               NUMERIC(10,2)                 NOT NULL,
    quantity_unit          VARCHAR(20)                   NOT NULL DEFAULT 'KG',

    pickup_latitude        NUMERIC(10,7),
    pickup_longitude       NUMERIC(10,7),
    pickup_address         TEXT,

    warehouse_id           UUID,
    warehouse_latitude     NUMERIC(10,7),
    warehouse_longitude    NUMERIC(10,7),

    estimated_distance_km  NUMERIC(10,2),

    request_status         transport_request_status_enum NOT NULL DEFAULT 'PENDING',
    notes                  TEXT,

    created_at             TIMESTAMPTZ                   NOT NULL DEFAULT NOW(),
    updated_at             TIMESTAMPTZ                   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_transport_request
    PRIMARY KEY (transport_request_id),

    CONSTRAINT uq_transport_request_code
    UNIQUE (transport_request_code),

    CONSTRAINT uq_transport_request_order_id
    UNIQUE (order_id),

    CONSTRAINT chk_transport_request_code_not_blank
    CHECK (LENGTH(TRIM(transport_request_code)) > 0),

    CONSTRAINT chk_transport_request_auction_ref_code_not_blank
    CHECK (LENGTH(TRIM(auction_ref_code)) > 0),

    CONSTRAINT chk_transport_request_product_name_not_blank
    CHECK (LENGTH(TRIM(product_name)) > 0),

    CONSTRAINT chk_transport_request_quantity_unit_not_blank
    CHECK (LENGTH(TRIM(quantity_unit)) > 0),

    CONSTRAINT chk_transport_request_quantity_positive
    CHECK (quantity > 0),

    CONSTRAINT chk_transport_request_pickup_latitude_range
    CHECK (pickup_latitude IS NULL OR pickup_latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_transport_request_pickup_longitude_range
    CHECK (pickup_longitude IS NULL OR pickup_longitude BETWEEN -180 AND 180),

    CONSTRAINT chk_transport_request_warehouse_latitude_range
    CHECK (warehouse_latitude IS NULL OR warehouse_latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_transport_request_warehouse_longitude_range
    CHECK (warehouse_longitude IS NULL OR warehouse_longitude BETWEEN -180 AND 180),

    CONSTRAINT chk_transport_request_distance_positive_when_present
    CHECK (estimated_distance_km IS NULL OR estimated_distance_km > 0)
    );

-- =====================
-- ROUTE ESTIMATE
-- Stores the computed cost estimation result for a transport request.
-- One estimate per transport request — enforced by unique constraint on transport_request_id.
-- cost_per_km_used captures the transporter's cost rule rate at the time of estimation,
--   creating an immutable snapshot in case the rate changes later.
-- weight_used is the product quantity in KG used as the weight factor in cost calculation.
-- estimated_cost is the final computed total: cost_per_km_used * estimated_distance_km,
--   adjusted by the transporter's weight/quantity pricing logic.
-- Stored separately from transport_request to keep the request table clean
--   and to allow re-estimation without modifying the original request record.
-- =====================
CREATE TABLE IF NOT EXISTS route_estimate (
    route_estimate_id     UUID          NOT NULL DEFAULT gen_random_uuid(),
    transport_request_id  UUID          NOT NULL,

    estimated_distance_km NUMERIC(10,2) NOT NULL,
    estimated_cost        NUMERIC(14,2) NOT NULL,
    cost_per_km_used      NUMERIC(10,2) NOT NULL,
    cost_per_kg_used      NUMERIC(10,2),
    weight_used           NUMERIC(10,2),

    estimated_at          TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    created_at            TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_route_estimate
    PRIMARY KEY (route_estimate_id),

    CONSTRAINT uq_route_estimate_transport_request_id
    UNIQUE (transport_request_id),

    CONSTRAINT fk_route_estimate_transport_request
    FOREIGN KEY (transport_request_id)
    REFERENCES transport_request(transport_request_id)
    ON DELETE CASCADE,

    CONSTRAINT chk_route_estimate_distance_positive
    CHECK (estimated_distance_km > 0),

    CONSTRAINT chk_route_estimate_cost_positive
    CHECK (estimated_cost > 0),

    CONSTRAINT chk_route_estimate_cost_per_km_positive
    CHECK (cost_per_km_used > 0),

    CONSTRAINT chk_route_estimate_cost_per_kg_non_negative_when_present
    CHECK (cost_per_kg_used IS NULL OR cost_per_kg_used >= 0),

    CONSTRAINT chk_route_estimate_weight_positive_when_present
    CHECK (weight_used IS NULL OR weight_used > 0)
    );

-- =====================
-- TRANSPORT TASK
-- Created by the transporter when they accept a transport request and assign a vehicle and driver.
-- transport_task_code is the UI-friendly code shown to transporter, driver, and farmer.
-- transporter_id is a plain UUID cross-service reference to Identity Service — no FK constraint.
-- driver_id is a plain UUID cross-service reference to Identity Service driver profile — no FK constraint.
-- vehicle_id has a FK constraint because vehicle ownership is fully within Logistics Service.
-- estimated_cost is sourced from route_estimate at task creation; actual_cost is updated
--   after delivery when the final billable amount is confirmed.
-- One active task per transport request — enforced by unique constraint on transport_request_id.
-- =====================
CREATE TABLE IF NOT EXISTS transport_task (
    transport_task_id    UUID                       NOT NULL DEFAULT gen_random_uuid(),
    transport_task_code  VARCHAR(20)                NOT NULL DEFAULT ('TTK-' || UPPER(encode(gen_random_bytes(4), 'hex'))),

    transport_request_id UUID                       NOT NULL,
    transporter_id       UUID                       NOT NULL,
    vehicle_id           UUID                       NOT NULL,
    driver_id            UUID                       NOT NULL,

    assigned_at          TIMESTAMPTZ                NOT NULL DEFAULT NOW(),
    accepted_at          TIMESTAMPTZ,

    estimated_cost       NUMERIC(14,2),
    actual_cost          NUMERIC(14,2),

    task_status          transport_task_status_enum NOT NULL DEFAULT 'ASSIGNED',

    created_at           TIMESTAMPTZ                NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ                NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_transport_task
    PRIMARY KEY (transport_task_id),

    CONSTRAINT uq_transport_task_code
    UNIQUE (transport_task_code),

    CONSTRAINT uq_transport_task_transport_request_id
    UNIQUE (transport_request_id),

    CONSTRAINT fk_transport_task_transport_request
    FOREIGN KEY (transport_request_id)
    REFERENCES transport_request(transport_request_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_transport_task_vehicle
    FOREIGN KEY (vehicle_id)
    REFERENCES vehicle(vehicle_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_transport_task_code_not_blank
    CHECK (LENGTH(TRIM(transport_task_code)) > 0),

    CONSTRAINT chk_transport_task_estimated_cost_positive_when_present
    CHECK (estimated_cost IS NULL OR estimated_cost > 0),

    CONSTRAINT chk_transport_task_actual_cost_positive_when_present
    CHECK (actual_cost IS NULL OR actual_cost > 0)
    );

-- =====================
-- DRIVER ASSIGNMENT
-- Records the history of every driver-to-vehicle and driver-to-task assignment
-- made by a transporter. One active assignment per driver at any time is enforced
-- by a partial unique index on driver_id WHERE is_active = TRUE.
-- transporter_id is a plain UUID cross-service reference to Identity Service — no FK constraint.
-- driver_id is a plain UUID cross-service reference to Identity Service driver profile — no FK constraint.
-- released_at is set when a driver completes or is removed from an active assignment.
-- =====================
CREATE TABLE IF NOT EXISTS driver_assignment (
    assignment_id      UUID        NOT NULL DEFAULT gen_random_uuid(),

    driver_id          UUID        NOT NULL,
    transporter_id     UUID        NOT NULL,
    vehicle_id         UUID        NOT NULL,
    transport_task_id  UUID        NOT NULL,

    assigned_at        TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    released_at        TIMESTAMPTZ,
    is_active          BOOLEAN     NOT NULL DEFAULT TRUE,

    created_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_driver_assignment
    PRIMARY KEY (assignment_id),

    CONSTRAINT fk_driver_assignment_vehicle
    FOREIGN KEY (vehicle_id)
    REFERENCES vehicle(vehicle_id)
    ON DELETE RESTRICT,

    CONSTRAINT fk_driver_assignment_transport_task
    FOREIGN KEY (transport_task_id)
    REFERENCES transport_task(transport_task_id)
    ON DELETE RESTRICT
    );

-- =====================
-- DELIVERY STATUS
-- Stores the current delivery status for a transport task.
-- One record per transport task — enforced by unique constraint on transport_task_id.
-- The full history of all status transitions is recorded separately in tracking_update.
-- updated_by is a plain UUID cross-service reference to Identity Service — no FK constraint.
--   This is the user_id of the driver or transporter who performed the status update.
-- current_status starts at AWAITING_PICKUP and progresses through:
--   LOADED → IN_TRANSIT → DELIVERED → UNLOADED_AT_WAREHOUSE (FR-13).
-- =====================
CREATE TABLE IF NOT EXISTS delivery_status (
    delivery_status_id UUID                 NOT NULL DEFAULT gen_random_uuid(),
    transport_task_id  UUID                 NOT NULL,

    current_status     delivery_status_enum NOT NULL DEFAULT 'AWAITING_PICKUP',
    updated_by         UUID,
    notes              TEXT,

    updated_at         TIMESTAMPTZ          NOT NULL DEFAULT NOW(),
    created_at         TIMESTAMPTZ          NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_delivery_status
    PRIMARY KEY (delivery_status_id),

    CONSTRAINT uq_delivery_status_transport_task_id
    UNIQUE (transport_task_id),

    CONSTRAINT fk_delivery_status_transport_task
    FOREIGN KEY (transport_task_id)
    REFERENCES transport_task(transport_task_id)
    ON DELETE RESTRICT
    );

-- =====================
-- TRACKING UPDATE
-- Immutable audit log of every delivery status transition for a transport task.
-- Mirrors the payment_audit_record pattern from Order & Payment Service.
-- previous_status is null when recording the initial AWAITING_PICKUP status on task creation.
-- latitude and longitude capture the GPS coordinates of the driver at the time of the update,
--   sourced from the mobile app's device location (FR-13, GPS tracking interface).
-- updated_by is a plain UUID cross-service reference to Identity Service — no FK constraint.
--   Represents the driver or transporter who triggered the status change.
-- These records must not be deleted; they provide the complete delivery audit trail.
-- =====================
CREATE TABLE IF NOT EXISTS tracking_update (
    tracking_update_id UUID                 NOT NULL DEFAULT gen_random_uuid(),
    transport_task_id  UUID                 NOT NULL,

    previous_status    delivery_status_enum,
    new_status         delivery_status_enum NOT NULL,

    latitude           NUMERIC(10,7),
    longitude          NUMERIC(10,7),

    updated_by         UUID,
    notes              TEXT,

    recorded_at        TIMESTAMPTZ          NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_tracking_update
    PRIMARY KEY (tracking_update_id),

    CONSTRAINT fk_tracking_update_transport_task
    FOREIGN KEY (transport_task_id)
    REFERENCES transport_task(transport_task_id)
    ON DELETE RESTRICT,

    CONSTRAINT chk_tracking_update_latitude_range
    CHECK (latitude IS NULL OR latitude BETWEEN -90 AND 90),

    CONSTRAINT chk_tracking_update_longitude_range
    CHECK (longitude IS NULL OR longitude BETWEEN -180 AND 180)
    );

-- =====================
-- LOGISTICS EVENT
-- Audit log of important logistics lifecycle events.
-- Mirrors the marketplace_event pattern from Marketplace Service
-- and the order_event pattern from Order & Payment Service.
-- transport_request_id, transport_task_id, and vehicle_id are plain UUID references
--   with no FK constraints — events may be recorded at the moment entities are being committed.
-- All three context columns are nullable; at least one must be non-null per CHECK constraint.
-- created_by is the user_id of the actor who triggered the event.
--   NULL is allowed for system-generated events such as automatic status transitions.
-- =====================
CREATE TABLE IF NOT EXISTS logistics_event (
    event_id             UUID                      PRIMARY KEY DEFAULT gen_random_uuid(),

    transport_request_id UUID,
    transport_task_id    UUID,
    vehicle_id           UUID,

    event_type           logistics_event_type_enum NOT NULL,
    description          TEXT,
    created_by           UUID,

    created_at           TIMESTAMPTZ               NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_logistics_event_has_context
    CHECK (
        transport_request_id IS NOT NULL OR
        transport_task_id    IS NOT NULL OR
        vehicle_id           IS NOT NULL
    )
    );



-- =========================================
-- TRIGGERS
-- =========================================

CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_vehicle_set_updated_at
BEFORE UPDATE ON vehicle
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_transport_cost_rule_set_updated_at
BEFORE UPDATE ON transport_cost_rule
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_transport_request_set_updated_at
BEFORE UPDATE ON transport_request
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_transport_task_set_updated_at
BEFORE UPDATE ON transport_task
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();

CREATE TRIGGER trg_delivery_status_set_updated_at
BEFORE UPDATE ON delivery_status
FOR EACH ROW
EXECUTE FUNCTION public.set_updated_at();



-- =========================================
-- INDEXES
-- =========================================

-- Vehicle
CREATE INDEX IF NOT EXISTS idx_vehicle_code
    ON vehicle(vehicle_code);

CREATE INDEX IF NOT EXISTS idx_vehicle_transporter_id
    ON vehicle(transporter_id);

CREATE INDEX IF NOT EXISTS idx_vehicle_number_plate
    ON vehicle(vehicle_number_plate);

CREATE INDEX IF NOT EXISTS idx_vehicle_engine_number
    ON vehicle(engine_number);

CREATE INDEX IF NOT EXISTS idx_vehicle_type
    ON vehicle(vehicle_type);

CREATE INDEX IF NOT EXISTS idx_vehicle_availability_status
    ON vehicle(availability_status);

CREATE INDEX IF NOT EXISTS idx_vehicle_is_active
    ON vehicle(is_active);

-- Composite: used to find available vehicles of a specific type for a transporter
CREATE INDEX IF NOT EXISTS idx_vehicle_transporter_availability
    ON vehicle(transporter_id, availability_status, is_active);

-- Vehicle Photo
CREATE INDEX IF NOT EXISTS idx_vehicle_photo_vehicle_id
    ON vehicle_photo(vehicle_id);

CREATE INDEX IF NOT EXISTS idx_vehicle_photo_is_primary
    ON vehicle_photo(is_primary);

-- Only one primary photo allowed per vehicle
CREATE UNIQUE INDEX IF NOT EXISTS uq_vehicle_primary_photo
    ON vehicle_photo(vehicle_id)
    WHERE is_primary = TRUE;

-- Transport Cost Rule
CREATE INDEX IF NOT EXISTS idx_transport_cost_rule_transporter_id
    ON transport_cost_rule(transporter_id);

CREATE INDEX IF NOT EXISTS idx_transport_cost_rule_is_active
    ON transport_cost_rule(is_active);

-- Composite: used to fetch the active cost rule for a transporter during cost estimation
CREATE INDEX IF NOT EXISTS idx_transport_cost_rule_transporter_active
    ON transport_cost_rule(transporter_id, is_active);

-- Enforces one active cost rule per transporter at DB level.
-- Prevents multiple is_active = TRUE rows for the same transporter,
-- which would cause cost estimation queries to return multiple rows.
CREATE UNIQUE INDEX IF NOT EXISTS uq_transporter_active_cost_rule
    ON transport_cost_rule(transporter_id)
    WHERE is_active = TRUE;

-- Transport Request
CREATE INDEX IF NOT EXISTS idx_transport_request_code
    ON transport_request(transport_request_code);

CREATE INDEX IF NOT EXISTS idx_transport_request_farmer_id
    ON transport_request(farmer_id);

CREATE INDEX IF NOT EXISTS idx_transport_request_order_id
    ON transport_request(order_id);

CREATE INDEX IF NOT EXISTS idx_transport_request_auction_ref_code
    ON transport_request(auction_ref_code);

CREATE INDEX IF NOT EXISTS idx_transport_request_warehouse_id
    ON transport_request(warehouse_id);

CREATE INDEX IF NOT EXISTS idx_transport_request_status
    ON transport_request(request_status);

CREATE INDEX IF NOT EXISTS idx_transport_request_created_at
    ON transport_request(created_at);

-- Composite: used by Logistics Service to find pending transport requests
-- eligible for transporter acceptance
CREATE INDEX IF NOT EXISTS idx_transport_request_status_created_at
    ON transport_request(request_status, created_at);

-- Route Estimate
CREATE INDEX IF NOT EXISTS idx_route_estimate_transport_request_id
    ON route_estimate(transport_request_id);

CREATE INDEX IF NOT EXISTS idx_route_estimate_estimated_at
    ON route_estimate(estimated_at);

-- Transport Task
CREATE INDEX IF NOT EXISTS idx_transport_task_code
    ON transport_task(transport_task_code);

CREATE INDEX IF NOT EXISTS idx_transport_task_transport_request_id
    ON transport_task(transport_request_id);

CREATE INDEX IF NOT EXISTS idx_transport_task_transporter_id
    ON transport_task(transporter_id);

CREATE INDEX IF NOT EXISTS idx_transport_task_vehicle_id
    ON transport_task(vehicle_id);

CREATE INDEX IF NOT EXISTS idx_transport_task_driver_id
    ON transport_task(driver_id);

CREATE INDEX IF NOT EXISTS idx_transport_task_status
    ON transport_task(task_status);

CREATE INDEX IF NOT EXISTS idx_transport_task_assigned_at
    ON transport_task(assigned_at);

-- Composite: used by the driver mobile app to fetch active tasks assigned to a specific driver
CREATE INDEX IF NOT EXISTS idx_transport_task_driver_status
    ON transport_task(driver_id, task_status);

-- Driver Assignment
CREATE INDEX IF NOT EXISTS idx_driver_assignment_driver_id
    ON driver_assignment(driver_id);

CREATE INDEX IF NOT EXISTS idx_driver_assignment_transporter_id
    ON driver_assignment(transporter_id);

CREATE INDEX IF NOT EXISTS idx_driver_assignment_vehicle_id
    ON driver_assignment(vehicle_id);

CREATE INDEX IF NOT EXISTS idx_driver_assignment_transport_task_id
    ON driver_assignment(transport_task_id);

CREATE INDEX IF NOT EXISTS idx_driver_assignment_is_active
    ON driver_assignment(is_active);

CREATE INDEX IF NOT EXISTS idx_driver_assignment_assigned_at
    ON driver_assignment(assigned_at);

-- Only one active assignment per driver at any time
CREATE UNIQUE INDEX IF NOT EXISTS uq_driver_active_assignment
    ON driver_assignment(driver_id)
    WHERE is_active = TRUE;

-- Delivery Status
CREATE INDEX IF NOT EXISTS idx_delivery_status_transport_task_id
    ON delivery_status(transport_task_id);

CREATE INDEX IF NOT EXISTS idx_delivery_status_current_status
    ON delivery_status(current_status);

CREATE INDEX IF NOT EXISTS idx_delivery_status_updated_by
    ON delivery_status(updated_by);

CREATE INDEX IF NOT EXISTS idx_delivery_status_updated_at
    ON delivery_status(updated_at);

-- Tracking Update
CREATE INDEX IF NOT EXISTS idx_tracking_update_transport_task_id
    ON tracking_update(transport_task_id);

CREATE INDEX IF NOT EXISTS idx_tracking_update_new_status
    ON tracking_update(new_status);

CREATE INDEX IF NOT EXISTS idx_tracking_update_updated_by
    ON tracking_update(updated_by);

CREATE INDEX IF NOT EXISTS idx_tracking_update_recorded_at
    ON tracking_update(recorded_at);

-- Composite: used to fetch the full ordered status history of a specific task
CREATE INDEX IF NOT EXISTS idx_tracking_update_task_recorded_at
    ON tracking_update(transport_task_id, recorded_at DESC);

-- Logistics Event
CREATE INDEX IF NOT EXISTS idx_logistics_event_transport_request_id
    ON logistics_event(transport_request_id);

CREATE INDEX IF NOT EXISTS idx_logistics_event_transport_task_id
    ON logistics_event(transport_task_id);

CREATE INDEX IF NOT EXISTS idx_logistics_event_vehicle_id
    ON logistics_event(vehicle_id);

CREATE INDEX IF NOT EXISTS idx_logistics_event_type
    ON logistics_event(event_type);

CREATE INDEX IF NOT EXISTS idx_logistics_event_created_by
    ON logistics_event(created_by);

CREATE INDEX IF NOT EXISTS idx_logistics_event_created_at
    ON logistics_event(created_at);



-- =========================================
-- COMMENTS
-- =========================================

COMMENT ON TABLE vehicle IS
'Registered transport vehicle managed by a transporter. transporter_id is a cross-service UUID reference to Identity Service with no FK constraint. vehicle_code is shown in UI instead of vehicle_id UUID.';

COMMENT ON TABLE vehicle_photo IS
'Photos attached to a registered vehicle. Multiple photos allowed per vehicle. Only one primary photo is enforced by a partial unique index on vehicle_id WHERE is_primary = TRUE.';

COMMENT ON TABLE transport_cost_rule IS
'Per-kilometre cost rate defined by a transporter. Only the active rule (is_active = TRUE) is used during cost estimation. Rate history is preserved by deactivating old rules rather than deleting them.';

COMMENT ON TABLE transport_request IS
'Transport request created by a farmer after their auction ends and the winning order is confirmed by Order & Payment Service. order_id and auction_ref_code are cross-service references stored as plain UUID and VARCHAR — no FK constraints.';

COMMENT ON TABLE route_estimate IS
'Computed transport cost estimation result for a transport request. Stores a snapshot of the distance, cost rate, and weight used at the time of calculation. One estimate per transport request.';

COMMENT ON TABLE transport_task IS
'Task created by the transporter after accepting a transport request, assigning a vehicle and driver. One active task per transport request enforced by unique constraint. driver_id and transporter_id are cross-service UUID references to Identity Service — no FK constraints.';

COMMENT ON TABLE driver_assignment IS
'Immutable history of every driver-to-vehicle and driver-to-task assignment. Only one active assignment per driver at a time is enforced by a partial unique index on driver_id WHERE is_active = TRUE.';

COMMENT ON TABLE delivery_status IS
'Current delivery status for a transport task. One record per task. Full status change history is maintained separately in tracking_update. Status progresses from AWAITING_PICKUP through UNLOADED_AT_WAREHOUSE per FR-13.';

COMMENT ON TABLE tracking_update IS
'Immutable audit log of every delivery status transition. Records GPS coordinates at the time of each update for real-time tracking support. Must not be deleted — required for logistics traceability.';

COMMENT ON TABLE logistics_event IS
'Audit log of important logistics lifecycle events. Mirrors the marketplace_event and order_event patterns from other services. At least one of transport_request_id, transport_task_id, or vehicle_id must be non-null.';

-- Vehicle Columns
COMMENT ON COLUMN vehicle.vehicle_code IS
'UI-friendly vehicle code such as VHC-AB12CD34. Use this in frontend and mobile instead of vehicle_id UUID.';

COMMENT ON COLUMN vehicle.transporter_id IS
'Cross-service reference to Identity Service transporter profile. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN vehicle.vehicle_number_plate IS
'Official vehicle registration number plate. Must be unique across all registered vehicles.';

COMMENT ON COLUMN vehicle.engine_number IS
'Vehicle engine serial number. Must be unique across all registered vehicles.';

COMMENT ON COLUMN vehicle.capacity_kg IS
'Maximum load capacity of the vehicle in kilograms. Used in transport cost estimation per FR-12.';

COMMENT ON COLUMN vehicle.operation_area IS
'Free-text description of the geographic area this vehicle operates in. Example: Colombo District, Western Province.';

COMMENT ON COLUMN vehicle.availability_status IS
'AVAILABLE: vehicle is free to be assigned. ASSIGNED: vehicle is currently on an active transport task. UNDER_MAINTENANCE: vehicle is not operational. INACTIVE: vehicle has been deregistered or retired.';

-- Vehicle Photo Columns
COMMENT ON COLUMN vehicle_photo.is_primary IS
'TRUE when this is the main display photo for the vehicle. Only one primary photo is allowed per vehicle, enforced by partial unique index.';

COMMENT ON COLUMN vehicle_photo.display_order IS
'Controls the rendering sequence of photos in the UI. Lower values appear first.';

-- Transport Cost Rule Columns
COMMENT ON COLUMN transport_cost_rule.transporter_id IS
'Cross-service reference to Identity Service transporter profile. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN transport_cost_rule.cost_per_km IS
'Rate charged by the transporter per kilometre. Applied during route cost estimation per FR-12.';

COMMENT ON COLUMN transport_cost_rule.cost_per_kg IS
'Optional weight-based rate charged per kilogram of product. Nullable — when set, this is combined with cost_per_km during cost estimation to account for the product weight/quantity factor per FR-12. When null, only the per-km rate applies.';

COMMENT ON COLUMN transport_cost_rule.is_active IS
'TRUE when this is the currently active cost rule for the transporter. Only one active rule is used at estimation time.';

COMMENT ON COLUMN transport_cost_rule.effective_from IS
'Timestamp when this cost rule became effective. Preserved for historical audit and reporting purposes.';

-- Transport Request Columns
COMMENT ON COLUMN transport_request.transport_request_code IS
'UI-friendly request code such as TRQ-AB12CD34. Use this in frontend and mobile instead of transport_request_id UUID.';

COMMENT ON COLUMN transport_request.farmer_id IS
'Cross-service reference to Identity Service farmer profile. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN transport_request.order_id IS
'Cross-service reference to Order & Payment Service order record. Stored as plain UUID — no FK constraint. One transport request per order enforced by unique constraint.';

COMMENT ON COLUMN transport_request.auction_ref_code IS
'Cross-service reference to auction_id_record.auction_ref_code in Marketplace Service. Stored as plain VARCHAR — no FK constraint. Used when verifying order details with Order & Payment Service via Feign Client.';

COMMENT ON COLUMN transport_request.product_name IS
'Denormalized product name from the order payload. Stored to avoid Feign calls back to Order & Payment Service on every transport request read.';

COMMENT ON COLUMN transport_request.quantity IS
'Product quantity from the order record. Used as the weight factor in transport cost estimation per FR-12.';

COMMENT ON COLUMN transport_request.quantity_unit IS
'Unit of measurement for product quantity. Example values: KG, METRIC_TON, BAGS.';

COMMENT ON COLUMN transport_request.pickup_latitude IS
'GPS latitude of the farmer farm field at the time of request creation. Sourced from Identity Service farmer_field data via Feign or request payload.';

COMMENT ON COLUMN transport_request.pickup_longitude IS
'GPS longitude of the farmer farm field at the time of request creation.';

COMMENT ON COLUMN transport_request.pickup_address IS
'Human-readable pickup address for display in transporter and driver mobile apps.';

COMMENT ON COLUMN transport_request.warehouse_id IS
'Cross-service reference to Warehouse & Inventory Service warehouse record. Stored as plain UUID — no FK constraint. Identifies the destination warehouse for this transport.';

COMMENT ON COLUMN transport_request.warehouse_latitude IS
'GPS latitude of the destination warehouse. Denormalized to avoid Feign calls during distance and cost calculation.';

COMMENT ON COLUMN transport_request.warehouse_longitude IS
'GPS longitude of the destination warehouse. Denormalized to avoid Feign calls during distance and cost calculation.';

COMMENT ON COLUMN transport_request.estimated_distance_km IS
'Road distance in kilometres between the pickup location and the destination warehouse. Calculated via external mapping API at request creation time.';

COMMENT ON COLUMN transport_request.request_status IS
'PENDING: awaiting transporter acceptance. ACCEPTED: transporter has created a transport task. REJECTED: transporter declined. CANCELLED: farmer or admin cancelled before acceptance. COMPLETED: delivery reached UNLOADED_AT_WAREHOUSE status.';

-- Route Estimate Columns
COMMENT ON COLUMN route_estimate.estimated_distance_km IS
'Road distance in kilometres used for this cost estimate. Sourced from transport_request.estimated_distance_km or recalculated if coordinates changed.';

COMMENT ON COLUMN route_estimate.estimated_cost IS
'Total estimated transport cost. Calculated from cost_per_km_used multiplied by estimated_distance_km, adjusted by any weight or quantity pricing factor.';

COMMENT ON COLUMN route_estimate.cost_per_km_used IS
'Snapshot of the transporter per-km cost rule rate at the time of estimation. Preserved here so future rate changes do not retroactively alter past estimates.';

COMMENT ON COLUMN route_estimate.cost_per_kg_used IS
'Snapshot of the transporter per-kg cost rule rate at the time of estimation. Null when the transporter had no per-kg rate set. Preserved alongside cost_per_km_used to make this estimate fully self-contained per FR-12.';

COMMENT ON COLUMN route_estimate.weight_used IS
'Product quantity in KG used as the weight factor in cost calculation. Sourced from transport_request.quantity at the time of estimation.';

-- Transport Task Columns
COMMENT ON COLUMN transport_task.transport_task_code IS
'UI-friendly task code such as TTK-AB12CD34. Use this in frontend and mobile instead of transport_task_id UUID.';

COMMENT ON COLUMN transport_task.transporter_id IS
'Cross-service reference to Identity Service transporter profile who accepted the request and created this task. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN transport_task.driver_id IS
'Cross-service reference to Identity Service driver profile assigned to execute this delivery. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN transport_task.assigned_at IS
'Timestamp when the transporter created and assigned this task to a driver.';

COMMENT ON COLUMN transport_task.accepted_at IS
'Timestamp when the assigned driver acknowledged and accepted the task in the mobile app.';

COMMENT ON COLUMN transport_task.estimated_cost IS
'Cost estimate at task creation time, sourced from route_estimate. Displayed to farmer, buyer, and transporter.';

COMMENT ON COLUMN transport_task.actual_cost IS
'Final billable cost confirmed after delivery completion. May differ from estimated_cost if distance or weight adjustments are applied.';

COMMENT ON COLUMN transport_task.task_status IS
'ASSIGNED: task created and driver assigned. IN_PROGRESS: driver has updated status to at least LOADED. COMPLETED: delivery reached UNLOADED_AT_WAREHOUSE and Warehouse & Inventory Service was notified. CANCELLED: task cancelled before completion.';

-- Driver Assignment Columns
COMMENT ON COLUMN driver_assignment.driver_id IS
'Cross-service reference to Identity Service driver profile. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN driver_assignment.transporter_id IS
'Cross-service reference to Identity Service transporter profile who made this assignment. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN driver_assignment.released_at IS
'Timestamp when the driver was released from this assignment. Set when the task is completed or cancelled.';

COMMENT ON COLUMN driver_assignment.is_active IS
'TRUE when this assignment is currently active. Only one active assignment per driver is enforced by a partial unique index.';

-- Delivery Status Columns
COMMENT ON COLUMN delivery_status.current_status IS
'Current stage in the delivery lifecycle: AWAITING_PICKUP → LOADED → IN_TRANSIT → DELIVERED → UNLOADED_AT_WAREHOUSE per FR-13.';

COMMENT ON COLUMN delivery_status.updated_by IS
'Cross-service reference to Identity Service user who last updated the status. Typically the driver or transporter. Stored as plain UUID — no FK constraint.';

-- Tracking Update Columns
COMMENT ON COLUMN tracking_update.previous_status IS
'Delivery status before this transition. Null when recording the initial AWAITING_PICKUP status on task creation.';

COMMENT ON COLUMN tracking_update.new_status IS
'Delivery status after this transition. Progresses through the full lifecycle defined in delivery_status_enum per FR-13.';

COMMENT ON COLUMN tracking_update.latitude IS
'GPS latitude of the driver at the time of this status update. Sourced from the driver mobile app device location.';

COMMENT ON COLUMN tracking_update.longitude IS
'GPS longitude of the driver at the time of this status update. Sourced from the driver mobile app device location.';

COMMENT ON COLUMN tracking_update.updated_by IS
'Cross-service reference to Identity Service user who performed this status update. Typically the driver or transporter. Stored as plain UUID — no FK constraint.';

COMMENT ON COLUMN tracking_update.recorded_at IS
'Exact timestamp when this status transition was recorded by the system.';

-- Logistics Event Columns
COMMENT ON COLUMN logistics_event.event_type IS
'VEHICLE_REGISTERED: new vehicle added by transporter. VEHICLE_UPDATED: vehicle details changed. DRIVER_ASSIGNED_TO_VEHICLE: driver linked to a vehicle. TRANSPORT_REQUEST_CREATED: farmer submitted a transport request. TRANSPORT_REQUEST_ACCEPTED: transporter accepted the request and created a task. TRANSPORT_REQUEST_REJECTED: transporter declined the request. TRANSPORT_REQUEST_CANCELLED: request cancelled before acceptance. DRIVER_ASSIGNED_TO_TASK: transporter assigned a driver to the transport task. DELIVERY_STATUS_UPDATED: driver or transporter changed the delivery status. DELIVERY_COMPLETED: status reached UNLOADED_AT_WAREHOUSE and Warehouse & Inventory Service was notified. TASK_CANCELLED: active transport task was cancelled.';

COMMENT ON COLUMN logistics_event.created_by IS
'User ID of the actor who triggered the event. Null is allowed for system-generated events. Cross-service reference to Identity Service — no FK constraint.';
