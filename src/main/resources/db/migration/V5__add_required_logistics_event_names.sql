SET search_path TO public;

ALTER TYPE logistics_event_type_enum
    ADD VALUE IF NOT EXISTS 'VEHICLE_ASSIGNED';

ALTER TYPE logistics_event_type_enum
    ADD VALUE IF NOT EXISTS 'DRIVER_ASSIGNED';

ALTER TYPE logistics_event_type_enum
    ADD VALUE IF NOT EXISTS 'PICKUP_STARTED';

ALTER TYPE logistics_event_type_enum
    ADD VALUE IF NOT EXISTS 'IN_TRANSIT';

ALTER TYPE logistics_event_type_enum
    ADD VALUE IF NOT EXISTS 'DELIVERED';

ALTER TYPE logistics_event_type_enum
    ADD VALUE IF NOT EXISTS 'UNLOADED_AT_WAREHOUSE';

ALTER TYPE logistics_event_type_enum
    ADD VALUE IF NOT EXISTS 'TRANSPORT_CANCELLED';
