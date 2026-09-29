package com.example.logistics.entity;

/** What kind of row a tracking_update record is. */
public enum TrackingUpdateType {
    /** First row, written when tracking is initialized for a task. */
    INITIALIZED,
    /** Delivery status moved to a new lifecycle stage. */
    STATUS_CHANGE,
    /** Live GPS ping from the driver (no status change). */
    LOCATION,
    /** Delay flag raised. */
    DELAY_FLAGGED,
    /** Delay flag cleared. */
    DELAY_RESOLVED,
    /** Acknowledgement received from Warehouse & Inventory Service. */
    WAREHOUSE_ACK
}
