package com.placetrack.controller;

import static com.placetrack.dto.ApiDtos.*;

import java.util.List;

import com.placetrack.service.PlaceTrackService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final PlaceTrackService service;

    public AdminController(PlaceTrackService service) {
        this.service = service;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return service.dashboard();
    }

    @GetMapping("/students")
    public List<UserResponse> students() {
        return service.students();
    }

    @GetMapping("/invitations")
    public List<InvitationResponse> invitations() {
        return service.invitations();
    }

    @PostMapping("/invitations")
    public InvitationResponse createInvitation(@Valid @RequestBody CreateInvitationRequest request) {
        return service.createInvitation(request);
    }

    @PostMapping("/invitations/bulk")
    public BulkInvitationResponse createInvitationsBulk(@Valid @RequestBody BulkInvitationRequest request) {
        return service.createInvitationsBulk(request);
    }

    @PostMapping("/invitations/{id}/resend")
    public InvitationResponse resendInvitation(@PathVariable Long id) {
        return service.resendInvitation(id);
    }

    @PostMapping("/invitations/{id}/revoke")
    public InvitationResponse revokeInvitation(@PathVariable Long id) {
        return service.revokeInvitation(id);
    }

    @GetMapping("/companies")
    public List<CompanyResponse> companies() {
        return service.companies();
    }

    @PostMapping("/companies")
    public CompanyResponse createCompany(@Valid @RequestBody CompanyRequest request) {
        return service.createCompany(request);
    }

    @GetMapping("/drives")
    public List<DriveResponse> drives() {
        return service.adminDrives();
    }

    @PostMapping("/drives")
    public DriveResponse createDrive(@Valid @RequestBody DriveRequest request) {
        return service.createDrive(request);
    }

    @PostMapping("/drives/{id}/publish")
    public DriveResponse publish(@PathVariable Long id) {
        return service.publishDrive(id);
    }

    @GetMapping("/drives/{id}/applications")
    public List<ApplicationResponse> driveApplications(@PathVariable Long id) {
        return service.driveApplications(id);
    }

    @PatchMapping("/applications/{id}/status")
    public ApplicationResponse updateStatus(Authentication authentication, @PathVariable Long id, @Valid @RequestBody StatusUpdateRequest request) {
        return service.updateApplicationStatus(authentication.getName(), id, request);
    }

    @PostMapping("/announcements/extract")
    public AnnouncementExtractResponse extract(@Valid @RequestBody AnnouncementExtractRequest request) {
        return service.extractAnnouncement(request);
    }
}
