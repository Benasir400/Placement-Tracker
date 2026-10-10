package com.placetrack.dto;

public record PlatformSummaryResponse(
    long invitedStudents,
    long registeredStudents,
    long pendingInvitations,
    long failedInvitations,
    long totalCompanies,
    long publishedDrives,
    long totalApplications
) {
}
