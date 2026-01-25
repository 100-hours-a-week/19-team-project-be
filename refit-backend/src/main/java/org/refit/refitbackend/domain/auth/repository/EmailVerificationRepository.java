package org.refit.refitbackend.domain.auth.repository;

import org.refit.refitbackend.domain.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {
    Optional<EmailVerification> findTopByUserIdAndEmailOrderByIdDesc(Long userId, String email);

    Optional<EmailVerification> findTopByEmailOrderByIdDesc(String email);
}
