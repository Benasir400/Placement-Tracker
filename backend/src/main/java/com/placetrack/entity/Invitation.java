package com.placetrack.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "invitations")
public class Invitation extends BaseEntity {
    @Column(nullable = false, length = 120)
    private String studentName;

    @Column(nullable = false, length = 180)
    private String collegeEmail;

    @Column(nullable = false, length = 40)
    private String branch;

    @Column(nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvitationStatus status = InvitationStatus.PENDING;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant sentAt;
    private Instant acceptedAt;

    @Column(length = 2000)
    private String lastDeliveryError;

    protected Invitation() {
    }

    public Invitation(String studentName, String collegeEmail, String branch, String tokenHash, Instant expiresAt) {
        this.studentName = studentName;
        this.collegeEmail = collegeEmail;
        this.branch = branch;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getCollegeEmail() {
        return collegeEmail;
    }

    public String getBranch() {
        return branch;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public Instant getAcceptedAt() {
        return acceptedAt;
    }

    public String getLastDeliveryError() {
        return lastDeliveryError;
    }

    public void markSent() {
        this.status = InvitationStatus.SENT;
        this.sentAt = Instant.now();
        this.lastDeliveryError = null;
    }

    public void rotateToken(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.acceptedAt = null;
    }

    public void markAccepted() {
        this.status = InvitationStatus.ACCEPTED;
        this.acceptedAt = Instant.now();
    }

    public void revoke() {
        this.status = InvitationStatus.REVOKED;
    }

    public void fail(String error) {
        this.status = InvitationStatus.FAILED;
        if (error != null && error.length() > 1900) {
            this.lastDeliveryError = error.substring(0, 1900) + "...";
        } else {
            this.lastDeliveryError = error;
        }
    }
}
