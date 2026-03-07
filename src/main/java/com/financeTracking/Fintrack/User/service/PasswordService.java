package com.financeTracking.Fintrack.User.service;

import com.financeTracking.Fintrack.AuthService.Repository.UserRepository;
import com.financeTracking.Fintrack.User.entity.PasswordResetToken;
import com.financeTracking.Fintrack.AuthService.entities.User;
import com.financeTracking.Fintrack.ExceptionHandler.ResourceNotFoundException;
import com.financeTracking.Fintrack.User.repo.PasswordResetTokenRepo;
import com.financeTracking.Fintrack.email.EmailService;
import jakarta.mail.MessagingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordService.class);


    private final UserRepository userRepository;
    private final PasswordResetTokenRepo tokenRepo;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public PasswordService(UserRepository userRepository, PasswordResetTokenRepo tokenRepo, PasswordEncoder passwordEncoder, EmailService emailService) {
        this.userRepository = userRepository;
        this.tokenRepo = tokenRepo;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void createTokenAndSendMail(String mail) throws MessagingException {
        User user = userRepository.findByEmail(mail).orElseThrow(()->new ResourceNotFoundException("User not found"));

        SecureRandom random = new SecureRandom();
        String token = String.valueOf(10000000 + random.nextInt(90000000));
        Optional<PasswordResetToken> existing =
                tokenRepo.findByUserAndExpiryDateAfter(user, LocalDateTime.now());

        if (existing.isPresent()) {
            throw new RuntimeException("Password reset already requested. Try after 24 hours.");
        }

        PasswordResetToken passwordResetToken = new PasswordResetToken();
        passwordResetToken.setToken(passwordEncoder.encode(token));
        passwordResetToken.setUser(user);
        passwordResetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));
        passwordResetToken.setCreatedAt(LocalDateTime.now());

        try {

            tokenRepo.save(passwordResetToken);

            emailService.sendResetLink(mail, token);
        }catch (Exception e){
            throw new RuntimeException("You might have been updated your password is last 24 hours, " +
                    "So wait for reset password for 24 hours after your last password reset");
        }

    }

    public void resetPassword(String mail, String token, String newPassword) {
        User user = userRepository.findByEmail(mail).orElseThrow(()->new ResourceNotFoundException("User not found"));

        PasswordResetToken resetToken =
                tokenRepo.findByUserId(user.getId())
                        .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (!passwordEncoder.matches(token, resetToken.getToken())) {
            throw new RuntimeException("Invalid token");
        }

        if (resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        user.setPasswordResetToken(null);
        userRepository.save(user);

        tokenRepo.deleteById(resetToken.getId());
    }

    @Scheduled(cron = "0 0 * * * *")  // every hour
    @Transactional
    public void deleteExpiredTokens() {
        tokenRepo.deleteByExpiryDateBefore(LocalDateTime.now());
        logger.info("Deleted the old tokens");
    }
}
