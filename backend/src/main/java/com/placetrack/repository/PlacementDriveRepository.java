package com.placetrack.repository;

import com.placetrack.entity.DriveStatus;
import com.placetrack.entity.PlacementDrive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlacementDriveRepository extends JpaRepository<PlacementDrive, Long> {
    long countByStatus(DriveStatus status);
    List<PlacementDrive> findByStatusOrderByRegistrationDeadlineAsc(DriveStatus status);
    List<PlacementDrive> findAllByOrderByCreatedAtDesc();
}
