package com.school.enrolment.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "applicants")
public class Applicant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false)
    private LocalDate dateOfBirth;

    @Column(length = 200)
    private String previousSchool;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id")
    private Guardian guardian;

    @Column(nullable = false)
    private Instant createdAt;

    protected Applicant() {
    }

    public Applicant(String firstName, String lastName, LocalDate dateOfBirth, String previousSchool, Guardian guardian) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.dateOfBirth = dateOfBirth;
        this.previousSchool = previousSchool;
        this.guardian = guardian;
        this.createdAt = Instant.now();
    }

    public String fullName() {
        return firstName + " " + lastName;
    }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public LocalDate getDateOfBirth() { return dateOfBirth; }
    public String getPreviousSchool() { return previousSchool; }
    public Guardian getGuardian() { return guardian; }
    public Instant getCreatedAt() { return createdAt; }
}
