package org.refit.refitbackend.domain.master.repository;

import org.refit.refitbackend.domain.master.entity.EmailDomain;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmailDomainRepository extends JpaRepository<EmailDomain, String> {

    @Query("""
        SELECT ed FROM EmailDomain ed
        WHERE (:cursor IS NULL OR ed.domain > :cursor)
        ORDER BY ed.domain ASC
    """)
    List<EmailDomain> findByCursor(
            @Param("cursor") String cursor,
            Pageable pageable
    );
}
