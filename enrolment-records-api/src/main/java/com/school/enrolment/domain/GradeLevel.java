package com.school.enrolment.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "grade_levels")
public class GradeLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private int minAge;

    @Column(nullable = false)
    private int maxAge;

    @Column(nullable = false)
    private boolean requiresPreviousReport;

    protected GradeLevel() {
    }

    public GradeLevel(String code, String name, int minAge, int maxAge, boolean requiresPreviousReport) {
        this.code = code;
        this.name = name;
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.requiresPreviousReport = requiresPreviousReport;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getMinAge() { return minAge; }
    public int getMaxAge() { return maxAge; }
    public boolean isRequiresPreviousReport() { return requiresPreviousReport; }
}
