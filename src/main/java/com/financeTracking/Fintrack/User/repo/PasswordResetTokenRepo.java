package com.financeTracking.Fintrack.User.repo;

import com.financeTracking.Fintrack.AuthService.entities.User;
import com.financeTracking.Fintrack.User.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepo extends JpaRepository<PasswordResetToken,Long> {
    Optional<PasswordResetToken> findByUserId(Long id);

    void deleteByExpiryDateBefore(LocalDateTime now);

    Optional<PasswordResetToken> findByUserAndExpiryDateAfter(User user, LocalDateTime time);
}
