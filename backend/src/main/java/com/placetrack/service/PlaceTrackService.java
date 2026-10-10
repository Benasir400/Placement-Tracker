package com.placetrack.service;

import static com.placetrack.dto.ApiDtos.*;

import java.math.BigDecimal;
import java.net.URI;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import com.placetrack.entity.Application;
import com.placetrack.entity.ApplicationMode;
import com.placetrack.entity.ApplicationStatus;
import com.placetrack.entity.Bookmark;
import com.placetrack.entity.Company;
import com.placetrack.entity.DriveStatus;
import com.placetrack.entity.Invitation;
import com.placetrack.entity.InvitationStatus;
import com.placetrack.entity.Notification;
import com.placetrack.entity.PlacementDrive;
import com.placetrack.entity.Role;
import com.placetrack.entity.StudentProfile;
import com.placetrack.entity.User;
import com.placetrack.repository.ApplicationEventRepository;
import com.placetrack.repository.ApplicationRepository;
import com.placetrack.repository.BookmarkRepository;
import com.placetrack.repository.CompanyRepository;
import com.placetrack.repository.InvitationRepository;
import com.placetrack.repository.NotificationRepository;
import com.placetrack.repository.PlacementDriveRepository;
import com.placetrack.repository.StudentProfileRepository;
import com.placetrack.repository.UserRepository;
import com.placetrack.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PlaceTrackService {
    private final UserRepository userRepository;
    private final StudentProfileRepository profileRepository;
    private final InvitationRepository invitationRepository;
    private final CompanyRepository companyRepository;
    private final PlacementDriveRepository driveRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationEventRepository eventRepository;
    private final BookmarkRepository bookmarkRepository;
    private final NotificationRepository notificationRepository;
    private final InvitationEmailService invitationEmailService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long invitationExpiryHours;
    private final boolean emailDevelopmentMode;
    private final String frontendBaseUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PlaceTrackService(
        UserRepository userRepository,
        StudentProfileRepository profileRepository,
        InvitationRepository invitationRepository,
        CompanyRepository companyRepository,
        PlacementDriveRepository driveRepository,
        ApplicationRepository applicationRepository,
        ApplicationEventRepository eventRepository,
        BookmarkRepository bookmarkRepository,
        NotificationRepository notificationRepository,
        InvitationEmailService invitationEmailService,
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        @Value("${placetrack.invitations.expiry-hours}") long invitationExpiryHours,
        @Value("${placetrack.email.development-mode}") boolean emailDevelopmentMode,
        @Value("${placetrack.frontend.base-url:http://127.0.0.1:5173}") String frontendBaseUrl
    ) {
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.invitationRepository = invitationRepository;
        this.companyRepository = companyRepository;
        this.driveRepository = driveRepository;
        this.applicationRepository = applicationRepository;
        this.eventRepository = eventRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.notificationRepository = notificationRepository;
        this.invitationEmailService = invitationEmailService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.invitationExpiryHours = invitationExpiryHours;
        this.emailDevelopmentMode = emailDevelopmentMode;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email).orElseThrow();
        return new AuthResponse(jwtService.generate(user), toUser(user));
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) throw badRequest("An account already exists for " + email);
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(), Role.STUDENT);
        user.verifyEmail(Instant.now());
        userRepository.save(user);
        StudentProfile profile = new StudentProfile(user, request.branch().trim().toUpperCase(Locale.ROOT));
        profile.update(request.fullName().trim(), request.branch().trim().toUpperCase(Locale.ROOT), null, null, null, 0, null, email, null);
        profileRepository.save(profile);
        notificationRepository.save(new Notification(user, "Welcome to PlaceTrack", "Complete your profile and review active placement drives.", "REGISTRATION"));
        return new AuthResponse(jwtService.generate(user), toUser(user));
    }

    @Transactional
    public AuthResponse acceptInvitation(AcceptInvitationRequest request) {
        Invitation invitation = invitationByCode(invitationCode(request));
        if (invitation.getStatus() == InvitationStatus.ACCEPTED) throw badRequest("Invitation already used");
        if (invitation.getStatus() == InvitationStatus.REVOKED) throw badRequest("Invitation was revoked");
        if (invitation.getExpiresAt().isBefore(Instant.now())) throw badRequest("Invitation expired");
        String email = normalizeEmail(invitation.getCollegeEmail());
        if (request.collegeEmail() != null && !request.collegeEmail().isBlank() && !email.equals(normalizeEmail(request.collegeEmail()))) {
            throw badRequest("College email does not match this invitation code");
        }
        if (userRepository.existsByEmail(email)) throw badRequest("Student account already exists");
        User user = new User(email, passwordEncoder.encode(request.password()), invitation.getStudentName(), Role.STUDENT);
        user.verifyEmail(Instant.now());
        userRepository.save(user);
        StudentProfile profile = new StudentProfile(user, invitation.getBranch());
        profile.update(invitation.getStudentName(), invitation.getBranch(), null, null, null, 0, null, email, null);
        profileRepository.save(profile);
        invitation.markAccepted();
        notificationRepository.save(new Notification(user, "Welcome to PlaceTrack", "Complete your profile and review active placement drives.", "REGISTRATION"));
        return new AuthResponse(jwtService.generate(user), toUser(user));
    }

    @Transactional(readOnly = true)
    public InvitationPreviewResponse invitationPreview(String code) {
        Invitation invitation = invitationByCode(code);
        return new InvitationPreviewResponse(invitation.getStudentName(), invitation.getCollegeEmail(), invitation.getBranch(), invitation.getStatus(), invitation.getExpiresAt());
    }

    public User currentUser(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public UserResponse me(String email) {
        return toUser(currentUser(email));
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        Map<String, Long> distribution = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            distribution.put(status.name(), applicationRepository.countByStatus(status));
        }
        return new DashboardResponse(
            invitationRepository.count(),
            userRepository.countByRole(Role.STUDENT),
            invitationRepository.countByStatus(InvitationStatus.PENDING) + invitationRepository.countByStatus(InvitationStatus.SENT),
            invitationRepository.countByStatus(InvitationStatus.FAILED),
            companyRepository.count(),
            driveRepository.countByStatus(DriveStatus.PUBLISHED),
            applicationRepository.count(),
            applicationRepository.countByStatus(ApplicationStatus.SELECTED),
            distribution
        );
    }

    @Transactional(readOnly = true)
    public List<InvitationResponse> invitations() {
        return invitationRepository.findAll().stream().map(this::toInvitation).toList();
    }

    @Transactional
    public InvitationResponse createInvitation(CreateInvitationRequest request) {
        String email = normalizeEmail(request.collegeEmail());
        if (invitationRepository.existsByCollegeEmail(email) || userRepository.existsByEmail(email)) {
            throw badRequest("A student or invitation already exists for " + email);
        }
        String token = newToken();
        Invitation invitation = new Invitation(request.name().trim(), email, request.branch().trim().toUpperCase(Locale.ROOT), hashToken(token), Instant.now().plusSeconds(invitationExpiryHours * 3600));
        deliverInvitation(invitation, token);
        Invitation saved = invitationRepository.save(invitation);
        return toInvitation(saved, token);
    }

    @Transactional
    public BulkInvitationResponse createInvitationsBulk(BulkInvitationRequest request) {
        if (request.students() == null || request.students().isEmpty()) {
            throw badRequest("Upload at least one student row");
        }
        List<BulkInvitationResult> results = new ArrayList<>();
        int sent = 0;
        int skipped = 0;
        for (int index = 0; index < request.students().size(); index++) {
            CreateInvitationRequest row = request.students().get(index);
            int rowNumber = index + 1;
            try {
                if (row.name() == null || row.name().isBlank() || row.collegeEmail() == null || row.collegeEmail().isBlank() || row.branch() == null || row.branch().isBlank()) {
                    skipped++;
                    results.add(new BulkInvitationResult(rowNumber, valueOrBlank(row.name()), valueOrBlank(row.collegeEmail()), valueOrBlank(row.branch()), "SKIPPED", "Name, college email, and branch are required", null));
                    continue;
                }
                String email = normalizeEmail(row.collegeEmail());
                if (!email.contains("@")) {
                    skipped++;
                    results.add(new BulkInvitationResult(rowNumber, row.name().trim(), email, row.branch().trim(), "SKIPPED", "College email is not valid", null));
                    continue;
                }
                if (invitationRepository.existsByCollegeEmail(email) || userRepository.existsByEmail(email)) {
                    skipped++;
                    results.add(new BulkInvitationResult(rowNumber, row.name().trim(), email, row.branch().trim(), "SKIPPED", "Student already invited or registered", null));
                    continue;
                }
                InvitationResponse invitation = createInvitation(row);
                sent++;
                results.add(new BulkInvitationResult(rowNumber, invitation.studentName(), invitation.collegeEmail(), invitation.branch(), "SENT", "Invitation link generated", invitation.registrationUrl()));
            } catch (RuntimeException ex) {
                skipped++;
                results.add(new BulkInvitationResult(rowNumber, valueOrBlank(row.name()), valueOrBlank(row.collegeEmail()), valueOrBlank(row.branch()), "SKIPPED", ex.getMessage(), null));
            }
        }
        return new BulkInvitationResponse(request.students().size(), sent, skipped, results);
    }

    @Transactional
    public InvitationResponse resendInvitation(Long id) {
        Invitation invitation = invitationRepository.findById(id).orElseThrow(() -> notFound("Invitation not found"));
        if (invitation.getStatus() == InvitationStatus.ACCEPTED || invitation.getStatus() == InvitationStatus.REVOKED) {
            throw badRequest("Invitation cannot be resent in its current status");
        }
        String token = newToken();
        invitation.rotateToken(hashToken(token), Instant.now().plusSeconds(invitationExpiryHours * 3600));
        deliverInvitation(invitation, token);
        return toInvitation(invitation, token);
    }

    @Transactional
    public InvitationResponse revokeInvitation(Long id) {
        Invitation invitation = invitationRepository.findById(id).orElseThrow(() -> notFound("Invitation not found"));
        invitation.revoke();
        return toInvitation(invitation);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> students() {
        return userRepository.findAll().stream().filter(user -> user.getRole() == Role.STUDENT).map(this::toUser).toList();
    }

    @Transactional(readOnly = true)
    public List<CompanyResponse> companies() {
        return companyRepository.findAll().stream().map(this::toCompany).toList();
    }

    @Transactional
    public CompanyResponse createCompany(CompanyRequest request) {
        if (companyRepository.existsByNameIgnoreCase(request.name())) throw badRequest("Company already exists");
        return toCompany(companyRepository.save(new Company(request.name().trim(), request.description(), safeUrlOrNull(request.website()), safeUrlOrNull(request.logoUrl()), request.industry())));
    }

    @Transactional(readOnly = true)
    public List<DriveResponse> adminDrives() {
        return driveRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toDrive).toList();
    }

    @Transactional(readOnly = true)
    public List<DriveResponse> studentDrives() {
        return driveRepository.findByStatusOrderByRegistrationDeadlineAsc(DriveStatus.PUBLISHED).stream().map(this::toDrive).toList();
    }

    @Transactional
    public DriveResponse createDrive(DriveRequest request) {
        Company company = companyRepository.findById(request.companyId()).orElseThrow(() -> notFound("Company not found"));
        validateDriveRequest(request);
        PlacementDrive drive = new PlacementDrive(company, request.jobTitle().trim(), request.applicationMode());
        drive.update(company, request.jobTitle(), request.jobDescription(), request.requiredSkills(), request.jobLocation(), request.employmentType(),
            request.salaryDetails(), normalizeCsv(request.eligibleBranches()), normalizeCsv(request.eligibleGraduationYears()), request.minimumCgpa(),
            request.maximumActiveBacklogs(), request.registrationDeadline(), request.assessmentDate(), request.interviewDate(), request.applicationMode(),
            safeUrlOrNull(request.externalApplicationUrl()), request.internalApplicationInstructions());
        return toDrive(driveRepository.save(drive));
    }

    @Transactional
    public DriveResponse publishDrive(Long id) {
        PlacementDrive drive = driveRepository.findById(id).orElseThrow(() -> notFound("Drive not found"));
        drive.publish();
        List<User> students = userRepository.findAll().stream().filter(user -> user.getRole() == Role.STUDENT).toList();
        for (User student : students) {
            notificationRepository.save(new Notification(student, "New placement drive", drive.getCompany().getName() + " is hiring for " + drive.getJobTitle(), "DRIVE"));
        }
        return toDrive(drive);
    }

    @Transactional(readOnly = true)
    public DriveResponse drive(Long id) {
        return toDrive(driveRepository.findById(id).orElseThrow(() -> notFound("Drive not found")));
    }

    @Transactional(readOnly = true)
    public StudentProfileResponse profile(String email) {
        User user = currentUser(email);
        StudentProfile profile = profileRepository.findByUser(user).orElseThrow(() -> notFound("Profile not found"));
        return toProfile(profile);
    }

    @Transactional
    public StudentProfileResponse updateProfile(String email, StudentProfileRequest request) {
        User user = currentUser(email);
        StudentProfile profile = profileRepository.findByUser(user).orElseGet(() -> new StudentProfile(user, request.branch()));
        profile.update(request.fullName(), request.branch().trim().toUpperCase(Locale.ROOT), request.registerNumber(), request.graduationYear(), request.cgpa(),
            request.activeBacklogs(), request.phoneNumber(), request.preferredApplicationEmail(), request.resumeUrl());
        return toProfile(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    public EligibilityResponse eligibility(String email, Long driveId) {
        User user = currentUser(email);
        PlacementDrive drive = driveRepository.findById(driveId).orElseThrow(() -> notFound("Drive not found"));
        return checkEligibility(user, drive);
    }

    @Transactional
    public ApplicationResponse apply(String email, Long driveId, ApplyRequest request) {
        User user = currentUser(email);
        PlacementDrive drive = driveRepository.findById(driveId).orElseThrow(() -> notFound("Drive not found"));
        EligibilityResponse eligibility = checkEligibility(user, drive);
        if (!eligibility.eligible()) throw badRequest(String.join(", ", eligibility.reasons()));
        String selectedEmail = request.selectedApplicationEmail() == null || request.selectedApplicationEmail().isBlank()
            ? user.getEmail() : normalizeEmail(request.selectedApplicationEmail());
        Application application = applicationRepository.save(new Application(user, drive, selectedEmail));
        notificationRepository.save(new Notification(user, "Application submitted", "Your application for " + drive.getJobTitle() + " was recorded.", "APPLICATION"));
        return toApplication(application);
    }

    @Transactional
    public ApplicationResponse markExternalSubmitted(String email, Long applicationId) {
        User user = currentUser(email);
        Application application = applicationRepository.findById(applicationId).orElseThrow(() -> notFound("Application not found"));
        if (!application.getStudent().getId().equals(user.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your application");
        application.markExternalSubmitted();
        return toApplication(application);
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> myApplications(String email) {
        return applicationRepository.findByStudentOrderBySubmittedAtDesc(currentUser(email)).stream().map(this::toApplication).toList();
    }

    @Transactional(readOnly = true)
    public List<ApplicationResponse> driveApplications(Long driveId) {
        PlacementDrive drive = driveRepository.findById(driveId).orElseThrow(() -> notFound("Drive not found"));
        return applicationRepository.findByDriveOrderBySubmittedAtDesc(drive).stream().map(this::toApplication).toList();
    }

    @Transactional
    public ApplicationResponse updateApplicationStatus(String actorEmail, Long applicationId, StatusUpdateRequest request) {
        User actor = currentUser(actorEmail);
        Application application = applicationRepository.findById(applicationId).orElseThrow(() -> notFound("Application not found"));
        ApplicationStatus previous = application.getStatus();
        application.changeStatus(request.status());
        eventRepository.save(new com.placetrack.entity.ApplicationEvent(application, previous, request.status(), actor, request.note()));
        notificationRepository.save(new Notification(application.getStudent(), "Application status updated", application.getDrive().getJobTitle() + " moved to " + request.status(), "APPLICATION"));
        try {
            invitationEmailService.sendApplicationStatusNotification(
                application.getStudent().getEmail(),
                application.getStudent().getFullName(),
                application.getDrive().getCompany().getName(),
                application.getDrive().getJobTitle(),
                request.status().name(),
                request.note()
            );
        } catch (Exception ex) {
            // Log delivery failure gracefully without failing the transaction
        }
        return toApplication(application);
    }

    @Transactional
    public void toggleBookmark(String email, Long driveId) {
        User user = currentUser(email);
        PlacementDrive drive = driveRepository.findById(driveId).orElseThrow(() -> notFound("Drive not found"));
        bookmarkRepository.findByStudentAndDrive(user, drive).ifPresentOrElse(bookmarkRepository::delete, () -> bookmarkRepository.save(new Bookmark(user, drive)));
    }

    @Transactional(readOnly = true)
    public List<DriveResponse> bookmarks(String email) {
        return bookmarkRepository.findByStudentOrderByCreatedAtDesc(currentUser(email)).stream().map(bookmark -> toDrive(bookmark.getDrive())).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> notifications(String email) {
        return notificationRepository.findByUserOrderByCreatedAtDesc(currentUser(email)).stream().map(this::toNotification).toList();
    }

    public AnnouncementExtractResponse extractAnnouncement(AnnouncementExtractRequest request) {
        String rawText = request.text();
        if (rawText == null || rawText.isBlank()) {
            return new AnnouncementExtractResponse(Map.of(), List.of("text"), "rule-based");
        }

        String cleanText = rawText.replaceAll("[*_~]+", " ").trim();
        Map<String, Object> fields = new LinkedHashMap<>();

        // Company Name
        String companyName = firstMatch(cleanText, "(?i)(?:company name|company|organization|employer)\\s*:?\\s*([A-Za-z0-9 &.]+?)(?:\\r?\\n|\\.|,|;|$)", 1);
        if (companyName == null) companyName = firstMatch(cleanText, "^\\s*([A-Z0-9][A-Za-z0-9 &.]+?)\\s*-\\s*(?:Internship|Placement|Drive|Hiring|Recruitment)", 1);
        if (companyName == null) companyName = firstMatch(cleanText, "^\\s*([A-Z0-9][A-Za-z0-9 &.]+?)\\s+(?:is hiring|hiring|invites|recruitment)", 1);
        fields.put("companyName", companyName);

        // Job Title
        String jobTitle = firstMatch(cleanText, "(?i)(?:job title|role|position|profile)\\s*:?\\s*([A-Za-z0-9 /&+-]+?)(?:\\r?\\n|\\.|,|;|$)", 1);
        if (jobTitle == null) jobTitle = firstMatch(cleanText, "(?i)(?:hiring|for)\\s+([A-Za-z ]*(?:Engineer|Developer|Analyst|Trainee|Intern|Associate|Consultant)[A-Za-z ]*)", 1);
        if (jobTitle == null && cleanText.toLowerCase(Locale.ROOT).contains("intern")) jobTitle = "Software Engineer Trainee / Intern";
        fields.put("jobTitle", jobTitle);

        // Required Skills
        fields.put("requiredSkills", firstMatch(cleanText, "(?i)(?:skills|required skills|tech stack)\\s*:?\\s*([A-Za-z0-9, /.+#-]+?)(?:\\r?\\n|\\.|$)", 1));

        // Employment Type
        String employmentType = firstMatch(cleanText, "(?i)(?:employment type|job type|type)\\s*:?\\s*([A-Za-z -]+?)(?:\\r?\\n|\\.|,|$)", 1);
        if (employmentType == null) {
            String lower = cleanText.toLowerCase(Locale.ROOT);
            if (lower.contains("intern")) employmentType = "Internship";
            else if (lower.contains("full time") || lower.contains("fulltime")) employmentType = "Full Time";
        }
        fields.put("employmentType", employmentType);

        // Industry
        fields.put("industry", firstMatch(cleanText, "(?i)industry\\s*:?\\s*([A-Za-z &-]+?)(?:\\r?\\n|\\.|,|$)", 1));

        // Eligible Branches
        String eligibleBranches = firstMatch(cleanText, "(?i)(?:eligible branches?|branches|streams)\\s*:?\\s*([A-Z0-9, /()+-]+?)(?:\\r?\\n|\\.|$)", 1);
        if (eligibleBranches == null) {
            eligibleBranches = extractBranchList(cleanText);
        }
        fields.put("eligibleBranches", eligibleBranches);

        // Eligible Graduation Years
        fields.put("eligibleGraduationYears", firstMatch(cleanText, "(?i)(?:graduation year|batch|passing year)\\s*:?\\s*(\\d{4}(?:\\s*[,/]\\s*\\d{4})*)", 1));

        // Minimum CGPA
        String minCgpa = firstMatch(cleanText, "(?i)(?:minimum|min|above|>=|>)\\s*(?:cgpa|gpa|percentage)?\\s*:?\\s*(\\d+(?:\\.\\d+)?)", 1);
        if (minCgpa == null) minCgpa = firstMatch(cleanText, "(\\d+(?:\\.\\d+)?)\\s*(?:cgpa|gpa)", 1);
        fields.put("minimumCgpa", minCgpa);

        // Maximum Active Backlogs
        fields.put("maximumActiveBacklogs", cleanText.toLowerCase(Locale.ROOT).contains("no active backlog") ? 0 : firstMatch(cleanText, "(?i)(?:maximum|max|up to)\\s*(?:active )?backlogs?\\s*:?\\s*(\\d+)", 1));

        // Salary Details
        String salary = firstMatch(cleanText, "(?i)(?:ctc|salary|package|stipend)\\s*:?\\s*([^\r\n;]+?)(?:\\r?\\n|\\.|$)", 1);
        if (salary == null) salary = firstMatch(cleanText, "([0-9.]+\\s*(?:lpa|lakhs?|pa|k|per month|month))", 1);
        fields.put("salaryDetails", salary);

        // Job Location
        String location = firstMatch(cleanText, "(?i)(?:location|work location|job location)\\s*:?\\s*([A-Za-z0-9 ,/-]+?)(?:\\r?\\n|\\.|;|$)", 1);
        fields.put("jobLocation", location);

        // Registration Deadline
        String deadline = firstMatch(cleanText, "(?i)(?:registration closes on|closes on|apply by|deadline|last date|register with in|register within|register by)\\s*:?\\s*([^\r\n:]+?)(?:\\r?\\n|\\.|$)", 1);
        fields.put("registrationDeadline", deadline);

        // URLs
        String externalUrl = extractBestApplicationUrl(rawText);
        String companyWebsite = extractCompanyWebsiteUrl(rawText, externalUrl);
        fields.put("companyWebsite", companyWebsite);
        fields.put("externalApplicationUrl", externalUrl);
        fields.put("applicationMode", externalUrl == null ? "INTERNAL" : "EXTERNAL");

        List<String> missing = fields.entrySet().stream().filter(entry -> entry.getValue() == null).map(Map.Entry::getKey).toList();
        return new AnnouncementExtractResponse(fields, missing, "rule-based");
    }

    private String extractBranchList(String text) {
        List<String> found = new ArrayList<>();
        String upper = text.toUpperCase(Locale.ROOT);
        String[] possibleBranches = {"CSE", "CS", "IT", "ECE", "EEE", "MECH", "CIVIL", "AD", "AIDS", "AIML", "CSBS", "CSD"};
        for (String branch : possibleBranches) {
            if (Pattern.compile("\\b" + branch + "\\b").matcher(upper).find() && !found.contains(branch)) {
                found.add(branch);
            }
        }
        return found.isEmpty() ? null : String.join(",", found);
    }

    private String extractBestApplicationUrl(String text) {
        var matcher = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE).matcher(text);
        List<String> urls = new ArrayList<>();
        while (matcher.find()) {
            urls.add(matcher.group().replaceAll("[).,*]+$", ""));
        }
        if (urls.isEmpty()) return null;
        for (String url : urls) {
            String lower = url.toLowerCase(Locale.ROOT);
            if (lower.contains("forms.gle") || lower.contains("google.com/forms") || lower.contains("forms.office") || lower.contains("apply") || lower.contains("register") || lower.contains("bit.ly") || lower.contains("tinyurl")) {
                return url;
            }
        }
        return urls.size() > 1 ? urls.get(1) : urls.get(0);
    }

    private String extractCompanyWebsiteUrl(String text, String applicationUrl) {
        var matcher = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE).matcher(text);
        while (matcher.find()) {
            String url = matcher.group().replaceAll("[).,*]+$", "");
            if (!url.equalsIgnoreCase(applicationUrl)) {
                return url;
            }
        }
        return null;
    }

    private EligibilityResponse checkEligibility(User user, PlacementDrive drive) {
        List<String> reasons = new ArrayList<>();
        if (!user.isActive()) reasons.add("Student account is inactive");
        StudentProfile profile = profileRepository.findByUser(user).orElse(null);
        if (profile == null || !profile.isProfileComplete()) reasons.add("Complete your student profile first");
        if (drive.getStatus() != DriveStatus.PUBLISHED) reasons.add("Drive is not published");
        if (drive.getRegistrationDeadline() != null && drive.getRegistrationDeadline().isBefore(LocalDate.now(ZoneId.of("Asia/Kolkata")))) reasons.add("Registration deadline has passed");
        if (applicationRepository.existsByStudentAndDrive(user, drive)) reasons.add("You have already applied");
        if (profile != null) {
            if (!containsCsv(drive.getEligibleBranches(), profile.getBranch())) reasons.add("Your branch is not eligible");
            if (profile.getGraduationYear() == null || !containsCsv(drive.getEligibleGraduationYears(), String.valueOf(profile.getGraduationYear()))) reasons.add("Your graduation year is not eligible");
            if (drive.getMinimumCgpa() != null && (profile.getCgpa() == null || profile.getCgpa().compareTo(drive.getMinimumCgpa()) < 0)) reasons.add("Minimum CGPA is " + drive.getMinimumCgpa());
            if (drive.getMaximumActiveBacklogs() != null && profile.getActiveBacklogs() != null && profile.getActiveBacklogs() > drive.getMaximumActiveBacklogs()) reasons.add("Active backlogs must be at most " + drive.getMaximumActiveBacklogs());
        }
        return new EligibilityResponse(reasons.isEmpty(), reasons);
    }

    private void validateDriveRequest(DriveRequest request) {
        if (request.applicationMode() == ApplicationMode.EXTERNAL && safeUrlOrNull(request.externalApplicationUrl()) == null) {
            throw badRequest("A safe external URL is required for external applications");
        }
    }

    private String safeUrlOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        URI uri;
        try {
            uri = URI.create(value.trim());
        } catch (IllegalArgumentException ex) {
            throw badRequest("URL is not valid");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) throw badRequest("Only http/https URLs are allowed");
        if (uri.getHost() == null || uri.getHost().isBlank()) throw badRequest("URL must include a host");
        return uri.toString();
    }

    private boolean containsCsv(String csv, String value) {
        if (csv == null || value == null) return false;
        String target = value.trim().toUpperCase(Locale.ROOT);
        for (String item : csv.split(",")) {
            if (item.trim().toUpperCase(Locale.ROOT).equals(target)) return true;
        }
        return false;
    }

    private String normalizeCsv(String csv) {
        return csv == null ? "" : String.join(",", Pattern.compile("[,/]").splitAsStream(csv).map(String::trim).filter(s -> !s.isBlank()).map(s -> s.toUpperCase(Locale.ROOT)).toList());
    }

    private String firstMatch(String text, String regex, int group) {
        var matcher = Pattern.compile(regex, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE).matcher(text);
        return matcher.find() ? matcher.group(group).replaceAll("[.,]+$", "").trim() : null;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String invitationCode(AcceptInvitationRequest request) {
        if (request.code() != null && !request.code().isBlank()) return request.code().trim();
        if (request.token() != null && !request.token().isBlank()) return request.token().trim();
        throw badRequest("Invitation code is required");
    }

    private Invitation invitationByCode(String code) {
        if (code == null || code.isBlank()) throw badRequest("Invitation code is required");
        String trimmed = code.trim();
        return invitationRepository.findByTokenHash(hashToken(trimmed))
            .or(() -> invitationRepository.findByTokenHash(trimmed))
            .orElseThrow(() -> badRequest("Invalid or expired invitation code. Please ask your coordinator to send a new invitation."));
    }

    private String valueOrBlank(String value) {
        return value == null ? "" : value.trim();
    }

    public String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes()));
        } catch (Exception ex) {
            throw new IllegalStateException("Could not hash token", ex);
        }
    }

    public String newToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private UserResponse toUser(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole(), user.isActive());
    }

    private InvitationResponse toInvitation(Invitation invitation) {
        return toInvitation(invitation, null);
    }

    private InvitationResponse toInvitation(Invitation invitation, String developmentToken) {
        String token = emailDevelopmentMode ? developmentToken : null;
        String registrationUrl = token == null ? null : registrationUrl(token);
        return new InvitationResponse(invitation.getId(), invitation.getStudentName(), invitation.getCollegeEmail(), invitation.getBranch(), invitation.getStatus(), invitation.getExpiresAt(), invitation.getSentAt(), invitation.getAcceptedAt(), invitation.getLastDeliveryError(), token, registrationUrl);
    }

    private void deliverInvitation(Invitation invitation, String token) {
        try {
            invitationEmailService.sendInvitation(invitation, registrationUrl(token), token);
            invitation.markSent();
        } catch (RuntimeException ex) {
            invitation.fail(ex.getMessage());
        }
    }

    private String registrationUrl(String token) {
        return frontendBaseUrl.replaceAll("/+$", "") + "/register?code=" + token;
    }

    private CompanyResponse toCompany(Company company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getDescription(), company.getWebsite(), company.getLogoUrl(), company.getIndustry(), company.isArchived());
    }

    private DriveResponse toDrive(PlacementDrive drive) {
        return new DriveResponse(drive.getId(), toCompany(drive.getCompany()), drive.getJobTitle(), drive.getJobDescription(), drive.getRequiredSkills(), drive.getJobLocation(), drive.getEmploymentType(), drive.getSalaryDetails(), drive.getEligibleBranches(), drive.getEligibleGraduationYears(), drive.getMinimumCgpa(), drive.getMaximumActiveBacklogs(), drive.getRegistrationDeadline(), drive.getAssessmentDate(), drive.getInterviewDate(), drive.getApplicationMode(), drive.getExternalApplicationUrl(), drive.getInternalApplicationInstructions(), drive.getStatus(), drive.getPublishedAt());
    }

    private StudentProfileResponse toProfile(StudentProfile profile) {
        return new StudentProfileResponse(profile.getId(), profile.getUser().getFullName(), profile.getUser().getEmail(), profile.getBranch(), profile.getRegisterNumber(), profile.getGraduationYear(), profile.getCgpa(), profile.getActiveBacklogs(), profile.getPhoneNumber(), profile.getPreferredApplicationEmail(), profile.getResumeUrl(), profile.isProfileComplete());
    }

    private ApplicationResponse toApplication(Application application) {
        PlacementDrive drive = application.getDrive();
        return new ApplicationResponse(application.getId(), drive.getId(), drive.getCompany().getName(), drive.getJobTitle(), application.getStatus(), application.getSubmittedAt(), application.getSelectedApplicationEmail(), application.getExternalSubmittedAt());
    }

    private NotificationResponse toNotification(Notification notification) {
        return new NotificationResponse(notification.getId(), notification.getTitle(), notification.getMessage(), notification.getType(), notification.getReadAt() != null, notification.getCreatedAt());
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
