package org.refit.refitbackend.domain.agent.repository;

import org.refit.refitbackend.domain.agent.entity.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AgentSessionRepository extends JpaRepository<AgentSession, String> {

    Optional<AgentSession> findBySessionIdAndUserId(String sessionId, Long userId);

    List<AgentSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
}
