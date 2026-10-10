package com.placetrack.service;

import com.placetrack.entity.Invitation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class InvitationEmailService {
    private final JavaMailSender mailSender;
    private final boolean developmentMode;
    private final String fromAddress;

    public InvitationEmailService(
        JavaMailSender mailSender,
        @Value("${placetrack.email.development-mode}") boolean developmentMode,
        @Value("${SMTP_FROM:bena26875@gmail.com}") String fromAddress
    ) {
        this.mailSender = mailSender;
        this.developmentMode = developmentMode;
        this.fromAddress = fromAddress;
    }

    public void sendInvitation(Invitation invitation, String registrationUrl, String invitationCode) {
        if (developmentMode) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(invitation.getCollegeEmail());
        message.setSubject("PlaceTrack registration invitation");
        message.setText("""
            Hello %s,

            You have been invited to register for the college placement portal.

            College email:
            %s

            Invitation code:
            %s

            Register here:
            %s

            This secure link expires automatically. If you did not request this invitation, contact your placement coordinator.
            """.formatted(invitation.getStudentName(), invitation.getCollegeEmail(), invitationCode, registrationUrl));
        mailSender.send(message);
    }

    public void sendApplicationStatusNotification(String studentEmail, String studentName, String companyName, String jobTitle, String status, String note) {
        if (developmentMode) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(studentEmail);
        message.setSubject("PlaceTrack - Application Status Update: " + companyName + " (" + jobTitle + ")");
        
        String noteDetails = (note != null && !note.isBlank()) ? "\nCoordinator Note:\n" + note + "\n" : "";

        message.setText("""
            Hello %s,

            Your application for %s - %s has been updated to: %s.
            %s
            Log in to PlaceTrack to view full details.

            Best regards,
            PlaceTrack Placement Cell
            """.formatted(studentName, companyName, jobTitle, status, noteDetails));
        mailSender.send(message);
    }
}

