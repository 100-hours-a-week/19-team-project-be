package org.refit.refitbackend.domain.notification.repository;

import org.refit.refitbackend.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {

    Optional<FcmToken> findByToken(String token);

    List<FcmToken> findAllByUserId(Long userId);

    long deleteByUserIdAndToken(Long userId, String token);
}
