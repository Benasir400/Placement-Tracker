package com.placetrack.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "placement_drives")
public class PlacementDrive extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, length = 180)
    private String jobTitle;

    @Column(length = 5000)
    private String jobDescription;

    @Column(length = 1000)
    private String requiredSkills;

    @Column(length = 160)
    private String jobLocation;

    @Column(length = 80)
    private String employmentType;

    @Column(length = 160)
    private String salaryDetails;

    @Column(nullable = false, length = 500)
    private String eligibleBranches;

    @Column(nullable = false, length = 160)
    private String eligibleGraduationYears;

    @Column(precision = 4, scale = 2)
    private BigDecimal minimumCgpa;

    private Integer maximumActiveBacklogs;

    private LocalDate registrationDeadline;
    private LocalDate assessmentDate;
    private LocalDate interviewDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApplicationMode applicationMode = ApplicationMode.INTERNAL;

    @Column(length = 500)
    private String externalApplicationUrl;

    @Column(length = 2000)
    private String internalApplicationInstructions;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DriveStatus status = DriveStatus.DRAFT;

    private Instant publishedAt;
    private Instant closedAt;

    protected PlacementDrive() {
    }

    public PlacementDrive(Company company, String jobTitle, ApplicationMode applicationMode) {
        this.company = company;
        this.jobTitle = jobTitle;
        this.applicationMode = applicationMode;
    }

    public Company getCompany() {
        return company;
    }

    public String getJobTitle() {
        return jobTitle;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public String getRequiredSkills() {
        return requiredSkills;
    }

    public String getJobLocation() {
        return jobLocation;
    }

    public String getEmploymentType() {
        return employmentType;
    }

    public String getSalaryDetails() {
        return salaryDetails;
    }

    public String getEligibleBranches() {
        return eligibleBranches;
    }

    public String getEligibleGraduationYears() {
        return eligibleGraduationYears;
    }

    public BigDecimal getMinimumCgpa() {
        return minimumCgpa;
    }

    public Integer getMaximumActiveBacklogs() {
        return maximumActiveBacklogs;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public LocalDate getAssessmentDate() {
        return assessmentDate;
    }

    public LocalDate getInterviewDate() {
        return interviewDate;
    }

    public ApplicationMode getApplicationMode() {
        return applicationMode;
    }

    public String getExternalApplicationUrl() {
        return externalApplicationUrl;
    }

    public String getInternalApplicationInstructions() {
        return internalApplicationInstructions;
    }

    public DriveStatus getStatus() {
        return status;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void update(Company company, String jobTitle, String jobDescription, String requiredSkills, String jobLocation,
                       String employmentType, String salaryDetails, String eligibleBranches, String eligibleGraduationYears,
                       BigDecimal minimumCgpa, Integer maximumActiveBacklogs, LocalDate registrationDeadline,
                       LocalDate assessmentDate, LocalDate interviewDate, ApplicationMode applicationMode,
                       String externalApplicationUrl, String internalApplicationInstructions) {
        this.company = company;
        this.jobTitle = jobTitle;
        this.jobDescription = jobDescription;
        this.requiredSkills = requiredSkills;
        this.jobLocation = jobLocation;
        this.employmentType = employmentType;
        this.salaryDetails = salaryDetails;
        this.eligibleBranches = eligibleBranches;
        this.eligibleGraduationYears = eligibleGraduationYears;
        this.minimumCgpa = minimumCgpa;
        this.maximumActiveBacklogs = maximumActiveBacklogs;
        this.registrationDeadline = registrationDeadline;
        this.assessmentDate = assessmentDate;
        this.interviewDate = interviewDate;
        this.applicationMode = applicationMode;
        this.externalApplicationUrl = externalApplicationUrl;
        this.internalApplicationInstructions = internalApplicationInstructions;
    }

    public void publish() {
        this.status = DriveStatus.PUBLISHED;
        this.publishedAt = Instant.now();
        this.closedAt = null;
    }

    public void close() {
        this.status = DriveStatus.CLOSED;
        this.closedAt = Instant.now();
    }

    public void archive() {
        this.status = DriveStatus.ARCHIVED;
    }
}
