package com.school.enrolment.repository;

import com.school.enrolment.domain.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditEntryRepository extends JpaRepository<AuditEntry, Long> {

    List<AuditEntry> findByEntityTypeAndEntityIdOrderByIdAsc(String entityType, Long entityId);
}
