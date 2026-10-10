package com.placetrack.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.placetrack.entity.ApplicationMode;
import com.placetrack.entity.ApplicationStatus;
import com.placetrack.entity.DriveStatus;
import com.placetrack.entity.InvitationStatus;
import com.placetrack.entity.Role;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public final class ApiDtos {
    private ApiDtos() {
    }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {
    }

    public record RegisterRequest(@NotBlank String fullName, @Email @NotBlank String email, @NotBlank @Size(min = 8) String password, @NotBlank String branch) {
    }

    public record AuthResponse(String token, UserResponse user) {
    }

    public record UserResponse(Long id, String email, String fullName, Role role, boolean active) {
    }

    public record AcceptInvitationRequest(String token, String code, @Email String collegeEmail, @NotBlank @Size(min = 8) String password) {
    }

    public record InvitationPreviewResponse(String studentName, String collegeEmail, String branch, InvitationStatus status, Instant expiresAt) {
    }

    public record StudentProfileRequest(
        @NotBlank String fullName,
        @NotBlank String branch,
        String registerNumber,
        @Min(2020) Integer graduationYear,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal cgpa,
        @Min(0) Integer activeBacklogs,
        String phoneNumber,
        @Email String preferredApplicationEmail,
        String resumeUrl
    ) {
    }

    public record StudentProfileResponse(
        Long id,
        String fullName,
        String collegeEmail,
        String branch,
        String registerNumber,
        Integer graduationYear,
        BigDecimal cgpa,
        Integer activeBacklogs,
        String phoneNumber,
        String preferredApplicationEmail,
        String resumeUrl,
        boolean profileComplete
    ) {
    }

    public record InvitationResponse(
        Long id,
        String studentName,
        String collegeEmail,
        String branch,
        InvitationStatus status,
        Instant expiresAt,
        Instant sentAt,
        Instant acceptedAt,
        String lastDeliveryError,
        String developmentToken,
        String registrationUrl
    ) {
    }

    public record CreateInvitationRequest(@NotBlank String name, @Email @NotBlank String collegeEmail, @NotBlank String branch) {
    }

    public record BulkInvitationRequest(@NotNull List<CreateInvitationRequest> students) {
    }

    public record BulkInvitationResult(
        int rowNumber,
        String name,
        String collegeEmail,
        String branch,
        String status,
        String message,
        String registrationUrl
    ) {
    }

    public record BulkInvitationResponse(int totalRows, int sentCount, int skippedCount, List<BulkInvitationResult> results) {
    }

    public record CompanyRequest(@NotBlank String name, String description, String website, String logoUrl, String industry) {
    }

    public record CompanyResponse(Long id, String name, String description, String website, String logoUrl, String industry, boolean archived) {
    }

    public record DriveRequest(
        @NotNull Long companyId,
        @NotBlank String jobTitle,
        String jobDescription,
        String requiredSkills,
        String jobLocation,
        String employmentType,
        String salaryDetails,
        @NotBlank String eligibleBranches,
        @NotBlank String eligibleGraduationYears,
        @DecimalMin("0.0") @DecimalMax("10.0") BigDecimal minimumCgpa,
        @Min(0) Integer maximumActiveBacklogs,
        LocalDate registrationDeadline,
        LocalDate assessmentDate,
        LocalDate interviewDate,
        @NotNull ApplicationMode applicationMode,
        String externalApplicationUrl,
        String internalApplicationInstructions
    ) {
    }

    public record DriveResponse(
        Long id,
        CompanyResponse company,
        String jobTitle,
        String jobDescription,
        String requiredSkills,
        String jobLocation,
        String employmentType,
        String salaryDetails,
        String eligibleBranches,
        String eligibleGraduationYears,
        BigDecimal minimumCgpa,
        Integer maximumActiveBacklogs,
        LocalDate registrationDeadline,
        LocalDate assessmentDate,
        LocalDate interviewDate,
        ApplicationMode applicationMode,
        String externalApplicationUrl,
        String internalApplicationInstructions,
        DriveStatus status,
        Instant publishedAt
    ) {
    }

    public record EligibilityResponse(boolean eligible, List<String> reasons) {
    }

    public record ApplyRequest(@Email String selectedApplicationEmail) {
    }

    public record ApplicationResponse(
        Long id,
        Long driveId,
        String companyName,
        String jobTitle,
        ApplicationStatus status,
        Instant submittedAt,
        String selectedApplicationEmail,
        Instant externalSubmittedAt
    ) {
    }

    public record StatusUpdateRequest(@NotNull ApplicationStatus status, String note) {
    }

    public record NotificationResponse(Long id, String title, String message, String type, boolean read, Instant createdAt) {
    }

    public record AnnouncementExtractRequest(@NotBlank String text) {
    }

    public record AnnouncementExtractResponse(Map<String, Object> fields, List<String> missingFields, String extractionMode) {
    }

    public record DashboardResponse(
        long invitedStudents,
        long registeredStudents,
        long pendingInvitations,
        long failedInvitations,
        long totalCompanies,
        long publishedDrives,
        long totalApplications,
        long selectedStudents,
        Map<String, Long> applicationDistribution
    ) {
    }
}
