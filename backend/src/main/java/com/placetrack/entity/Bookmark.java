package com.placetrack.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "bookmarks", uniqueConstraints = {
    @UniqueConstraint(name = "uk_bookmarks_student_drive", columnNames = {"student_id", "drive_id"})
})
public class Bookmark extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "drive_id", nullable = false)
    private PlacementDrive drive;

    protected Bookmark() {
    }

    public Bookmark(User student, PlacementDrive drive) {
        this.student = student;
        this.drive = drive;
    }

    public PlacementDrive getDrive() {
        return drive;
    }

    public User getStudent() {
        return student;
    }
}
