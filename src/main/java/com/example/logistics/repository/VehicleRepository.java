package com.example.logistics.repository;

import com.example.logistics.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByVehicleNumberPlate(String vehicleNumberPlate);
    boolean existsByEngineNumber(String engineNumber);
    List<Vehicle> findByTransporterId(UUID transporterId);
}