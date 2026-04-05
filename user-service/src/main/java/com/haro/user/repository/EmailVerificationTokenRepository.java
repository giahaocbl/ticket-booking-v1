package com.haro.user.repository;

import com.haro.user.entity.EmailVerificationToken;
import com.haro.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, UUID> {
    List<EmailVerificationToken> findByUserAndVerifiedAtIsNullOrderByCreatedAtDesc(User user);
}
