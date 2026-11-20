package com.school.enrolment.service;

import com.school.enrolment.domain.AuditEntry;
import com.school.enrolment.repository.AuditEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditEntryRepository repository;

    public AuditService(AuditEntryRepository repository) {
        this.repository = repository;
    }

    /**
     * MANDATORY: an audit row is only ever written inside the same transaction as the change
     * it describes, so a change can never be committed without its audit entry (or vice versa).
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String entityType, Long entityId, String action, String from, String to,
                       String actor, String details) {
        repository.save(new AuditEntry(entityType, entityId, action, from, to, actor, details));
    }
}
