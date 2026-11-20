package com.school.enrolment.repository;

import com.school.enrolment.domain.GradeLevel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeLevelRepository extends JpaRepository<GradeLevel, Long> {
}
