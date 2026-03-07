package com.financeTracking.Fintrack.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender,
                        SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Value("${spring.mail.username}")
    private String fromMailId;

    public void sendResetLink(String mail, String token) throws MessagingException {

        Context context = new Context();
        context.setVariable("appName", "Finance Tracking App");
        context.setVariable("name", token);
        context.setVariable("message", "Refer the above OTP to reset password, it valid up to 10min.");
        context.setVariable("buttonLink", "https://daily-finance-tracking.netlify.app/");
        context.setVariable("buttonText", "Click here");
        context.setVariable("expires", LocalDateTime.now().plusMinutes(10));

        String htmlContent = templateEngine.process("email-content", context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(mail);
        helper.setSubject("Finance Tracking App Password/Username reset");
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }
}
