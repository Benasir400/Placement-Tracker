package com.placetrack.repository;

import com.placetrack.entity.Invitation;
import com.placetrack.entity.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    boolean existsByCollegeEmail(String collegeEmail);
    long countByStatus(InvitationStatus status);
    Optional<Invitation> findByTokenHash(String tokenHash);
}
