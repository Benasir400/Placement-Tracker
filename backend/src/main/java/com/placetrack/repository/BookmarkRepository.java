package com.placetrack.repository;

import com.placetrack.entity.Bookmark;
import com.placetrack.entity.PlacementDrive;
import com.placetrack.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    boolean existsByStudentAndDrive(User student, PlacementDrive drive);
    Optional<Bookmark> findByStudentAndDrive(User student, PlacementDrive drive);
    List<Bookmark> findByStudentOrderByCreatedAtDesc(User student);
}
