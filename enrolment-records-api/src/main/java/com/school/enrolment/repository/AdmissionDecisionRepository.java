package com.school.enrolment.repository;

import com.school.enrolment.domain.AdmissionDecision;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdmissionDecisionRepository extends JpaRepository<AdmissionDecision, Long> {

    List<AdmissionDecision> findByApplicationIdOrderByIdAsc(Long applicationId);
}
