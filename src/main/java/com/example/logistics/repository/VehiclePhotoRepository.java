package com.example.logistics.repository;

import com.example.logistics.entity.VehiclePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VehiclePhotoRepository extends JpaRepository<VehiclePhoto, UUID> {
    List<VehiclePhoto> findByVehicleVehicleIdOrderByDisplayOrderAsc(UUID vehicleId);


    Optional<VehiclePhoto> findByVehicleVehicleIdAndIsPrimaryTrue(UUID vehicleId);
}