package com.school.enrolment.web;

import com.school.enrolment.service.ReferenceDataService;
import com.school.enrolment.web.ApiDtos.AcademicYearView;
import com.school.enrolment.web.ApiDtos.GradeLevelView;
import com.school.enrolment.web.ApiDtos.OfferingView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ReferenceDataController {

    private final ReferenceDataService service;

    public ReferenceDataController(ReferenceDataService service) {
        this.service = service;
    }

    @GetMapping("/academic-years")
    public List<AcademicYearView> academicYears() {
        return service.academicYears();
    }

    @GetMapping("/grade-levels")
    public List<GradeLevelView> gradeLevels() {
        return service.gradeLevels();
    }

    /** Grades offered per year, with live seat counts. */
    @GetMapping("/offerings")
    public List<OfferingView> offerings() {
        return service.offerings();
    }
}
