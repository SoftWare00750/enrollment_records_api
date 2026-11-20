package com.school.enrolment.domain;

import jakarta.persistence.*;

/** A grade level offered in a particular academic year, with its seat capacity. */
@Entity
@Table(name = "class_offerings")
public class ClassOffering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grade_level_id")
    private GradeLevel gradeLevel;

    @Column(nullable = false)
    private int capacity;

    protected ClassOffering() {
    }

    public ClassOffering(AcademicYear academicYear, GradeLevel gradeLevel, int capacity) {
        this.academicYear = academicYear;
        this.gradeLevel = gradeLevel;
        this.capacity = capacity;
    }

    public Long getId() { return id; }
    public AcademicYear getAcademicYear() { return academicYear; }
    public GradeLevel getGradeLevel() { return gradeLevel; }
    public int getCapacity() { return capacity; }
}
