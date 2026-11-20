package com.example.logistics.entity;

/**
 * Delivery lifecycle stages for a transport task, per FR-13.
 * Order of the constants below is significant: it defines the required
 * forward progression AWAITING_PICKUP -> LOADED -> IN_TRANSIT -> DELIVERED -> UNLOADED_AT_WAREHOUSE.
 * Maps to the Postgres enum type: delivery_status_enum.
 */
public enum DeliveryStatusEnum {

    AWAITING_PICKUP,
    LOADED,
    IN_TRANSIT,
    DELIVERED,
    UNLOADED_AT_WAREHOUSE;

    /**
     * True when moving from this status to {@code target} is exactly one step forward
     * in the lifecycle (no skipping, no backward movement).
     */
    public boolean isValidForwardStepTo(DeliveryStatusEnum target) {
        return target != null && target.ordinal() == this.ordinal() + 1;
    }

    public boolean isBackwardOrSameAs(DeliveryStatusEnum target) {
        return target != null && target.ordinal() <= this.ordinal();
    }

    public boolean isTerminal() {
        return this == UNLOADED_AT_WAREHOUSE;
    }
}
