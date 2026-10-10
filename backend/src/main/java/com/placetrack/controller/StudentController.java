package com.placetrack.controller;

import static com.placetrack.dto.ApiDtos.*;

import java.util.List;

import com.placetrack.service.PlaceTrackService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
public class StudentController {
    private final PlaceTrackService service;

    public StudentController(PlaceTrackService service) {
        this.service = service;
    }

    @GetMapping("/profile")
    public StudentProfileResponse profile(Authentication authentication) {
        return service.profile(authentication.getName());
    }

    @PutMapping("/profile")
    public StudentProfileResponse updateProfile(Authentication authentication, @Valid @RequestBody StudentProfileRequest request) {
        return service.updateProfile(authentication.getName(), request);
    }

    @GetMapping("/drives")
    public List<DriveResponse> drives() {
        return service.studentDrives();
    }

    @GetMapping("/drives/{id}")
    public DriveResponse drive(@PathVariable Long id) {
        return service.drive(id);
    }

    @GetMapping("/drives/{id}/eligibility")
    public EligibilityResponse eligibility(Authentication authentication, @PathVariable Long id) {
        return service.eligibility(authentication.getName(), id);
    }

    @PostMapping("/drives/{id}/applications")
    public ApplicationResponse apply(Authentication authentication, @PathVariable Long id, @RequestBody ApplyRequest request) {
        return service.apply(authentication.getName(), id, request);
    }

    @GetMapping("/applications")
    public List<ApplicationResponse> applications(Authentication authentication) {
        return service.myApplications(authentication.getName());
    }

    @PostMapping("/applications/{id}/mark-submitted")
    public ApplicationResponse markSubmitted(Authentication authentication, @PathVariable Long id) {
        return service.markExternalSubmitted(authentication.getName(), id);
    }

    @PostMapping("/bookmarks/{driveId}")
    public void toggleBookmark(Authentication authentication, @PathVariable Long driveId) {
        service.toggleBookmark(authentication.getName(), driveId);
    }

    @GetMapping("/bookmarks")
    public List<DriveResponse> bookmarks(Authentication authentication) {
        return service.bookmarks(authentication.getName());
    }

    @GetMapping("/notifications")
    public List<NotificationResponse> notifications(Authentication authentication) {
        return service.notifications(authentication.getName());
    }
}
