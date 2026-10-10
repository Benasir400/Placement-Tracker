package com.placetrack.controller;

import static com.placetrack.dto.ApiDtos.*;

import com.placetrack.service.PlaceTrackService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final PlaceTrackService placeTrackService;

    public AuthController(PlaceTrackService placeTrackService) {
        this.placeTrackService = placeTrackService;
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return placeTrackService.login(request);
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return placeTrackService.register(request);
    }

    @PostMapping("/accept-invitation")
    public AuthResponse acceptInvitation(@Valid @RequestBody AcceptInvitationRequest request) {
        return placeTrackService.acceptInvitation(request);
    }

    @GetMapping("/invitation")
    public InvitationPreviewResponse invitation(@RequestParam String code) {
        return placeTrackService.invitationPreview(code);
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        return placeTrackService.me(authentication.getName());
    }
}
