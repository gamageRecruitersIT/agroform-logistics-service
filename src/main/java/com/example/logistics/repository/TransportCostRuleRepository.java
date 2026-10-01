package com.example.logistics.repository;

import com.example.logistics.entity.TransportCostRule;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransportCostRuleRepository extends JpaRepository<TransportCostRule, UUID> {
    Optional<TransportCostRule> findByTransporterIdAndIsActiveTrue(UUID transporterId);
}