package com.placetrack.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.placetrack.entity.ApplicationMode;
import com.placetrack.entity.Company;
import com.placetrack.entity.Invitation;
import com.placetrack.entity.Notification;
import com.placetrack.entity.PlacementDrive;
import com.placetrack.entity.Role;
import com.placetrack.entity.StudentProfile;
import com.placetrack.entity.User;
import com.placetrack.repository.CompanyRepository;
import com.placetrack.repository.InvitationRepository;
import com.placetrack.repository.NotificationRepository;
import com.placetrack.repository.PlacementDriveRepository;
import com.placetrack.repository.StudentProfileRepository;
import com.placetrack.repository.UserRepository;
import com.placetrack.service.PlaceTrackService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataSeeder {
    @Bean
    CommandLineRunner seedData(
        UserRepository userRepository,
        StudentProfileRepository profileRepository,
        CompanyRepository companyRepository,
        PlacementDriveRepository driveRepository,
        InvitationRepository invitationRepository,
        NotificationRepository notificationRepository,
        PasswordEncoder passwordEncoder,
        PlaceTrackService placeTrackService
    ) {
        return args -> {
            if (userRepository.count() > 0) return;

            User coordinator = new User("coordinator@college.edu", passwordEncoder.encode("Password@123"), "Placement Coordinator", Role.PLACEMENT_COORDINATOR);
            coordinator.verifyEmail(Instant.now());
            userRepository.save(coordinator);

            User student = new User("student1@college.edu", passwordEncoder.encode("Password@123"), "Student One", Role.STUDENT);
            student.verifyEmail(Instant.now());
            userRepository.save(student);
            StudentProfile profile = new StudentProfile(student, "IT");
            profile.update("Student One", "IT", "IT2027001", 2027, BigDecimal.valueOf(8.4), 0, "9876543210", "student1.jobs@gmail.com", "https://example.com/resume.pdf");
            profileRepository.save(profile);

            Company company = companyRepository.save(new Company("ABC Technologies", "Product engineering company hiring campus talent.", "https://example.com", null, "Software"));
            PlacementDrive drive = new PlacementDrive(company, "Software Engineer Trainee", ApplicationMode.EXTERNAL);
            drive.update(company, "Software Engineer Trainee", "Build and maintain web applications with Java, React, and cloud services.", "Java,React,SQL", "Chennai", "Full-time", "5 LPA", "IT,CSE,ECE", "2027", BigDecimal.valueOf(7.0), 0, LocalDate.now().plusDays(20), LocalDate.now().plusDays(30), LocalDate.now().plusDays(40), ApplicationMode.EXTERNAL, "https://example.com/careers", null);
            drive.publish();
            driveRepository.save(drive);

            Invitation invitation = new Invitation("Student Two", "student2@college.edu", "CSE", placeTrackService.hashToken("invite-student-demo"), Instant.now().plusSeconds(72 * 3600));
            invitation.markSent();
            invitationRepository.save(invitation);

            notificationRepository.save(new Notification(student, "New placement drive", "ABC Technologies is hiring Software Engineer Trainees.", "DRIVE"));
        };
    }
}
