package com.example.logistics.repository;

import com.example.logistics.entity.RouteEstimate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;


public interface RouteEstimateRepository extends JpaRepository<RouteEstimate, UUID> {

}