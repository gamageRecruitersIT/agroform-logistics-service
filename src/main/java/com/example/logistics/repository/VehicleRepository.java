package com.example.logistics.repository;

import com.example.logistics.entity.Vehicle;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    boolean existsByVehicleNumberPlate(String vehicleNumberPlate);
    boolean existsByEngineNumber(String engineNumber);
    List<Vehicle> findByTransporterId(UUID transporterId);

    Optional<Vehicle> findByVehicleCode(String vehicleCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM Vehicle v WHERE v.vehicleCode = :vehicleCode")
    Optional<Vehicle> findByVehicleCodeForUpdate(@Param("vehicleCode") String vehicleCode);
}