package com.haro.user.repository;

import com.haro.user.entity.PasswordResetToken;
import com.haro.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {
    List<PasswordResetToken> findByUserAndExpiresAtAfter(User user, OffsetDateTime time);
}
