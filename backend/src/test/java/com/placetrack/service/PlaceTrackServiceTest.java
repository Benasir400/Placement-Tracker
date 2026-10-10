package com.placetrack.service;

import com.placetrack.dto.ApiDtos.*;
import com.placetrack.entity.*;
import com.placetrack.repository.*;
import com.placetrack.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaceTrackServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository profileRepository;
    @Mock
    private InvitationRepository invitationRepository;
    @Mock
    private CompanyRepository companyRepository;
    @Mock
    private PlacementDriveRepository driveRepository;
    @Mock
    private ApplicationRepository applicationRepository;
    @Mock
    private ApplicationEventRepository eventRepository;
    @Mock
    private BookmarkRepository bookmarkRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private InvitationEmailService invitationEmailService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    private PlaceTrackService service;

    @BeforeEach
    void setUp() {
        service = new PlaceTrackService(
                userRepository,
                profileRepository,
                invitationRepository,
                companyRepository,
                driveRepository,
                applicationRepository,
                eventRepository,
                bookmarkRepository,
                notificationRepository,
                invitationEmailService,
                passwordEncoder,
                authenticationManager,
                jwtService,
                72L,
                false,
                "http://localhost:5173"
        );
    }

    @Test
    void createInvitationsBulk_processesRowsAndSendsInvitations() {
        CreateInvitationRequest req1 = new CreateInvitationRequest("Alice", "alice@college.edu", "CSE");
        CreateInvitationRequest req2 = new CreateInvitationRequest("Bob", "bob@college.edu", "ECE");
        BulkInvitationRequest request = new BulkInvitationRequest(List.of(req1, req2));

        when(invitationRepository.existsByCollegeEmail(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(invitationRepository.save(any(Invitation.class))).thenAnswer(i -> i.getArgument(0));

        BulkInvitationResponse response = service.createInvitationsBulk(request);

        assertThat(response.totalRows()).isEqualTo(2);
        assertThat(response.sentCount()).isEqualTo(2);
        assertThat(response.skippedCount()).isEqualTo(0);
        verify(invitationEmailService, times(2)).sendInvitation(any(), any(), any());
    }

    @Test
    void updateApplicationStatus_updatesStatusAndTriggersNotificationEmail() {
        User coordinator = new User("admin@college.edu", "pass", "Admin Coordinator", Role.PLACEMENT_COORDINATOR);
        User student = new User("student@college.edu", "pass", "Jane Student", Role.STUDENT);

        Company company = new Company("TechCorp", "Desc", "https://techcorp.com", null, "IT");
        PlacementDrive drive = new PlacementDrive(company, "Software Engineer", ApplicationMode.INTERNAL);

        Application application = new Application(student, drive, "student@college.edu");

        StatusUpdateRequest request = new StatusUpdateRequest(ApplicationStatus.INTERVIEW, "Proceed to technical interview");

        when(userRepository.findByEmail("admin@college.edu")).thenReturn(Optional.of(coordinator));
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        ApplicationResponse result = service.updateApplicationStatus("admin@college.edu", 10L, request);

        assertThat(result.status()).isEqualTo(ApplicationStatus.INTERVIEW);
        verify(eventRepository, times(1)).save(any());
        verify(notificationRepository, times(1)).save(any());
        verify(invitationEmailService, times(1)).sendApplicationStatusNotification(
                eq("student@college.edu"),
                eq("Jane Student"),
                eq("TechCorp"),
                eq("Software Engineer"),
                eq("INTERVIEW"),
                eq("Proceed to technical interview")
        );
    }

    @Test
    void extractAnnouncement_parsesInformalCampusMessage() {
        String announcement = """
            *SaasAnt Infotech - Internship drive on 12th Oct Mon 9.15 am @SKCET Convention center : register with in today 3 pm:*

            *Company Name : SaasAnt Infotech*
            https://www.saasant.com/

            PFB Registration link for SaasAnt Infotech . PFA JD to know more about role.

            *<application _link>*
            https://forms.gle/QFF8YmgPc35PEQN57
            *------------------*
            *Eligibility Criteria:* *above 8 CGPA in UG only*
            *B.E CS/IT/AD/CS(AIML/IoT/cyber)/Mtech CS/CSBS/CSD/ECE only:*
            *CTC : 6 LPA , Intern :15k*
            Role : Software engineer , Location : Coimbatore
            """;

        AnnouncementExtractResponse result = service.extractAnnouncement(new AnnouncementExtractRequest(announcement));

        assertThat(result.fields().get("companyName")).isEqualTo("SaasAnt Infotech");
        assertThat(result.fields().get("jobTitle")).isEqualTo("Software engineer");
        assertThat(result.fields().get("jobLocation")).isEqualTo("Coimbatore");
        assertThat(result.fields().get("salaryDetails")).isEqualTo("6 LPA , Intern :15k");
        assertThat(result.fields().get("minimumCgpa")).isEqualTo("8");
        assertThat(result.fields().get("employmentType")).isEqualTo("Internship");
        assertThat(result.fields().get("externalApplicationUrl")).isEqualTo("https://forms.gle/QFF8YmgPc35PEQN57");
        assertThat(result.fields().get("companyWebsite")).isEqualTo("https://www.saasant.com/");
    }
}
