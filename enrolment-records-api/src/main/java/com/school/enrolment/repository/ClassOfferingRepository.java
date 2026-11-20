package com.school.enrolment.repository;

import com.school.enrolment.domain.ClassOffering;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassOfferingRepository extends JpaRepository<ClassOffering, Long> {

    Optional<ClassOffering> findByAcademicYearIdAndGradeLevelId(Long academicYearId, Long gradeLevelId);

    /**
     * Takes a row lock (SELECT ... FOR UPDATE) so two admissions into the same class are
     * serialised and the seat count cannot be oversold.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ClassOffering o where o.id = :id")
    Optional<ClassOffering> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = {"academicYear", "gradeLevel"})
    List<ClassOffering> findAllByOrderByIdAsc();
}
