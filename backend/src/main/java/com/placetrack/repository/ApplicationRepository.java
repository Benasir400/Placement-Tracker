package com.placetrack.repository;

import com.placetrack.entity.Application;
import com.placetrack.entity.ApplicationStatus;
import com.placetrack.entity.PlacementDrive;
import com.placetrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    boolean existsByStudentAndDrive(User student, PlacementDrive drive);
    List<Application> findByStudentOrderBySubmittedAtDesc(User student);
    List<Application> findByDriveOrderBySubmittedAtDesc(PlacementDrive drive);
    long countByStatus(ApplicationStatus status);
}
