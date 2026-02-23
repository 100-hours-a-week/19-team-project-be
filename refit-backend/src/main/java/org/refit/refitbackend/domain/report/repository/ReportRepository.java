package org.refit.refitbackend.domain.report.repository;

import org.refit.refitbackend.domain.report.entity.Report;
import org.refit.refitbackend.domain.report.entity.enums.ReportStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Report> findByIdAndUserId(Long id, Long userId);

    boolean existsByChatRoomIdAndStatusIn(Long chatRoomId, List<ReportStatus> statuses);
}
