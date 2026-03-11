package org.refit.refitbackend.domain.agent.repository;

import org.refit.refitbackend.domain.agent.entity.AgentMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {

    List<AgentMessage> findBySessionIdAndUserIdOrderByIdAsc(String sessionId, Long userId);
}
