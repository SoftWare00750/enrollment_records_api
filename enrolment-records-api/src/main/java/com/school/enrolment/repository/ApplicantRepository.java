package com.school.enrolment.repository;

import com.school.enrolment.domain.Applicant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface ApplicantRepository extends JpaRepository<Applicant, Long> {

    Optional<Applicant> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDateOfBirthAndGuardianId(
            String firstName, String lastName, LocalDate dateOfBirth, Long guardianId);
}
