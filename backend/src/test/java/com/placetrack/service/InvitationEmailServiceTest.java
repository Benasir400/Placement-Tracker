package com.placetrack.service;

import com.placetrack.entity.Invitation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvitationEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Test
    void sendInvitation_devModeFalse_sendsEmail() {
        InvitationEmailService service = new InvitationEmailService(mailSender, false, "no-reply@placetrack.local");

        Invitation invitation = new Invitation("John Student", "john@college.edu", "CSE", "hash123", null);

        service.sendInvitation(invitation, "http://localhost:5173/register?code=abc", "abc");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("john@college.edu");
        assertThat(sent.getSubject()).contains("PlaceTrack registration invitation");
        assertThat(sent.getText()).contains("Hello John Student");
        assertThat(sent.getText()).contains("Invitation code:\nabc");
    }

    @Test
    void sendInvitation_devModeTrue_skipsEmail() {
        InvitationEmailService service = new InvitationEmailService(mailSender, true, "no-reply@placetrack.local");

        Invitation invitation = new Invitation("John Student", "john@college.edu", "CSE", "hash123", null);

        service.sendInvitation(invitation, "http://localhost:5173/register?code=abc", "abc");

        verifyNoInteractions(mailSender);
    }

    @Test
    void sendApplicationStatusNotification_devModeFalse_sendsEmail() {
        InvitationEmailService service = new InvitationEmailService(mailSender, false, "no-reply@placetrack.local");

        service.sendApplicationStatusNotification(
                "john@college.edu",
                "John Student",
                "Acme Corp",
                "Software Engineer",
                "SHORTLISTED",
                "Interview scheduled on Monday"
        );

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sent = captor.getValue();
        assertThat(sent.getTo()).containsExactly("john@college.edu");
        assertThat(sent.getSubject()).contains("Acme Corp");
        assertThat(sent.getText()).contains("updated to: SHORTLISTED");
        assertThat(sent.getText()).contains("Interview scheduled on Monday");
    }
}
