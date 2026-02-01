package org.refit.refitbackend.domain.master.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "email_domains")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailDomain {

    @Id
    @Column(length = 255, nullable = false)
    private String domain;

    @Column(name = "company_name", length = 100, nullable = false)
    private String companyName;
}
