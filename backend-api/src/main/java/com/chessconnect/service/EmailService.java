package com.chessconnect.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username:support@mychess.fr}")
    private String fromEmail;

    @Value("${app.name:Mychess}")
    private String appName;

    @Value("${app.logo-url:https://mychess.fr/assets/logo.png}")
    private String logoUrl;

    @Value("${spring.mail.enabled:false}")
    private boolean emailEnabled;

    public EmailService(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!emailEnabled) {
            log.info("Email sending disabled. Would have sent '{}' to {} using template '{}'",
                    subject, to, templateName);
            return;
        }

        try {
            Context context = new Context();
            context.setVariables(variables);
            context.setVariable("appName", appName);
            context.setVariable("logoUrl", logoUrl);

            String htmlContent = templateEngine.process("email/" + templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, appName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {} with subject '{}'", to, subject);

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String firstName, String resetLink) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "resetLink", resetLink
        );
        sendEmail(to, "Reinitialisation de votre mot de passe - Mychess",
                "password-reset", variables);
    }

    @Async
    public void sendLessonReminderEmail(String to, String firstName, String teacherName,
                                         String lessonDate, String lessonTime, String meetingLink) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "teacherName", teacherName,
                "lessonDate", lessonDate,
                "lessonTime", lessonTime,
                "meetingLink", meetingLink
        );
        sendEmail(to, "Rappel: Votre cours d'echecs dans 1 heure - Mychess",
                "lesson-reminder", variables);
    }

    @Async
    public void sendNewAvailabilityNotification(String to, String firstName, String teacherName,
                                                  String availabilityInfo, String bookingLink) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "teacherName", teacherName,
                "availabilityInfo", availabilityInfo,
                "bookingLink", bookingLink
        );
        sendEmail(to, teacherName + " a publie de nouveaux creneaux - Mychess",
                "new-availability-notification", variables);
    }

    @Async
    public void sendContactAdminEmail(String adminEmail, String senderName, String senderEmail,
                                       String subject, String messageContent) {
        Map<String, Object> variables = Map.of(
                "senderName", senderName,
                "senderEmail", senderEmail,
                "subject", subject,
                "messageContent", messageContent
        );
        sendEmail(adminEmail, "[Mychess] " + subject, "contact-admin", variables);
    }

    @Async
    public void sendEmailVerificationEmail(String to, String firstName, String verificationLink) {
        Map<String, Object> variables = Map.of(
                "firstName", firstName,
                "verificationLink", verificationLink
        );
        sendEmail(to, "Confirmez votre adresse email - Mychess",
                "email-verification", variables);
    }
}
