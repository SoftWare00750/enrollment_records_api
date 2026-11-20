package com.school.enrolment.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "academic_years")
public class AcademicYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String label;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private boolean applicationsOpen;

    protected AcademicYear() {
    }

    public AcademicYear(String label, LocalDate startDate, LocalDate endDate, boolean applicationsOpen) {
        this.label = label;
        this.startDate = startDate;
        this.endDate = endDate;
        this.applicationsOpen = applicationsOpen;
    }

    public Long getId() { return id; }
    public String getLabel() { return label; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public boolean isApplicationsOpen() { return applicationsOpen; }
}
