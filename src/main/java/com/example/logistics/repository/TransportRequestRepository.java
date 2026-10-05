package com.example.logistics.repository;

import com.example.logistics.entity.TransportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransportRequestRepository extends JpaRepository<TransportRequest, UUID> {

    /**
     * Check if a transport request already exists for a given order ID.
     * Used to prevent duplicate requests (Idempotency check).
     *
     * @param orderId The UUID of the order.
     * @return true if a request exists, false otherwise.
     */
    boolean existsByOrderId(UUID orderId);

    /**
     * Find a transport request by its public, UI-friendly request code.
     *
     * @param transportRequestCode The unique request code (e.g., TRQ-ABCD).
     * @return Optional containing the TransportRequest if found.
     */
    Optional<TransportRequest> findByTransportRequestCode(String transportRequestCode);

    /**
     * Check if a transport request already exists for a given auction reference code.
     *
     * @param auctionRefCode The auction reference code.
     * @return true if a request exists, false otherwise.
     */
    boolean existsByAuctionRefCode(String auctionRefCode);
}
