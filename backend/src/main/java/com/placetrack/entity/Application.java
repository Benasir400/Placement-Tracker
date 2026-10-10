package com.placetrack.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "applications", uniqueConstraints = {
    @UniqueConstraint(name = "uk_applications_student_drive", columnNames = {"student_id", "drive_id"})
})
public class Application extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drive_id", nullable = false)
    private PlacementDrive drive;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(nullable = false)
    private Instant submittedAt;

    @Column(length = 180)
    private String selectedApplicationEmail;

    private Instant externalSubmittedAt;

    protected Application() {
    }

    public Application(User student, PlacementDrive drive, String selectedApplicationEmail) {
        this.student = student;
        this.drive = drive;
        this.selectedApplicationEmail = selectedApplicationEmail;
        this.submittedAt = Instant.now();
    }

    public User getStudent() {
        return student;
    }

    public PlacementDrive getDrive() {
        return drive;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public String getSelectedApplicationEmail() {
        return selectedApplicationEmail;
    }

    public Instant getExternalSubmittedAt() {
        return externalSubmittedAt;
    }

    public void changeStatus(ApplicationStatus status) {
        this.status = status;
    }

    public void markExternalSubmitted() {
        this.externalSubmittedAt = Instant.now();
    }
}
