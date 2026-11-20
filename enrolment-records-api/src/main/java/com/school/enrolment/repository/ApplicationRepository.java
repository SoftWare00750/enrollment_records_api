package com.school.enrolment.repository;

import com.school.enrolment.domain.Application;
import com.school.enrolment.domain.ApplicationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.Optional;

public interface ApplicationRepository extends JpaRepository<Application, Long>, JpaSpecificationExecutor<Application> {

    @EntityGraph(attributePaths = {"applicant", "applicant.guardian", "offering", "offering.academicYear",
            "offering.gradeLevel", "documents"})
    Optional<Application> findWithDetailsById(Long id);

    @Override
    @EntityGraph(attributePaths = {"applicant", "offering", "offering.academicYear", "offering.gradeLevel"})
    Page<Application> findAll(Specification<Application> spec, Pageable pageable);

    long countByOfferingIdAndStatus(Long offeringId, ApplicationStatus status);

    boolean existsByApplicantIdAndOfferingAcademicYearIdAndStatusNotIn(
            Long applicantId, Long academicYearId, Collection<ApplicationStatus> statuses);

    @Query(value = "select nextval('application_reference_seq')", nativeQuery = true)
    long nextReferenceNumber();
}
