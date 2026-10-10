package com.placetrack.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "student_profiles")
public class StudentProfile extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, length = 40)
    private String branch;

    @Column(length = 60)
    private String registerNumber;

    private Integer graduationYear;

    @Column(precision = 4, scale = 2)
    private BigDecimal cgpa;

    @Column(nullable = false)
    private Integer activeBacklogs = 0;

    @Column(length = 30)
    private String phoneNumber;

    @Column(length = 180)
    private String preferredApplicationEmail;

    @Column(length = 500)
    private String resumeUrl;

    @Column(nullable = false)
    private boolean profileComplete = false;

    protected StudentProfile() {
    }

    public StudentProfile(User user, String branch) {
        this.user = user;
        this.branch = branch;
    }

    public User getUser() {
        return user;
    }

    public String getBranch() {
        return branch;
    }

    public String getRegisterNumber() {
        return registerNumber;
    }

    public Integer getGraduationYear() {
        return graduationYear;
    }

    public BigDecimal getCgpa() {
        return cgpa;
    }

    public Integer getActiveBacklogs() {
        return activeBacklogs;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getPreferredApplicationEmail() {
        return preferredApplicationEmail;
    }

    public String getResumeUrl() {
        return resumeUrl;
    }

    public boolean isProfileComplete() {
        return profileComplete;
    }

    public void update(String fullName, String branch, String registerNumber, Integer graduationYear, BigDecimal cgpa,
                       Integer activeBacklogs, String phoneNumber, String preferredApplicationEmail, String resumeUrl) {
        this.user.setFullName(fullName);
        this.branch = branch;
        this.registerNumber = registerNumber;
        this.graduationYear = graduationYear;
        this.cgpa = cgpa;
        this.activeBacklogs = activeBacklogs == null ? 0 : activeBacklogs;
        this.phoneNumber = phoneNumber;
        this.preferredApplicationEmail = preferredApplicationEmail;
        this.resumeUrl = resumeUrl;
        this.profileComplete = graduationYear != null && cgpa != null;
    }
}
