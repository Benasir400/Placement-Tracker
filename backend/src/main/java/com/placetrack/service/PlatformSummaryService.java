package com.placetrack.service;

import com.placetrack.dto.PlatformSummaryResponse;
import com.placetrack.entity.DriveStatus;
import com.placetrack.entity.InvitationStatus;
import com.placetrack.entity.Role;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.CompanyRepository;
import com.placetrack.repository.InvitationRepository;
import com.placetrack.repository.PlacementDriveRepository;
import com.placetrack.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformSummaryService {
    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final PlacementDriveRepository placementDriveRepository;
    private final ApplicationRepository applicationRepository;

    public PlatformSummaryService(
        InvitationRepository invitationRepository,
        UserRepository userRepository,
        CompanyRepository companyRepository,
        PlacementDriveRepository placementDriveRepository,
        ApplicationRepository applicationRepository
    ) {
        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.placementDriveRepository = placementDriveRepository;
        this.applicationRepository = applicationRepository;
    }

    @Transactional(readOnly = true)
    public PlatformSummaryResponse getSummary() {
        return new PlatformSummaryResponse(
            invitationRepository.count(),
            userRepository.countByRole(Role.STUDENT),
            invitationRepository.countByStatus(InvitationStatus.PENDING) + invitationRepository.countByStatus(InvitationStatus.SENT),
            invitationRepository.countByStatus(InvitationStatus.FAILED),
            companyRepository.count(),
            placementDriveRepository.countByStatus(DriveStatus.PUBLISHED),
            applicationRepository.count()
        );
    }
}
