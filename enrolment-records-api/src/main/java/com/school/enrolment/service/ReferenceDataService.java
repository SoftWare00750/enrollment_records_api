package com.school.enrolment.service;

import com.school.enrolment.domain.ApplicationStatus;
import com.school.enrolment.repository.AcademicYearRepository;
import com.school.enrolment.repository.ApplicationRepository;
import com.school.enrolment.repository.ClassOfferingRepository;
import com.school.enrolment.repository.GradeLevelRepository;
import com.school.enrolment.web.ApiDtos.AcademicYearView;
import com.school.enrolment.web.ApiDtos.GradeLevelView;
import com.school.enrolment.web.ApiDtos.OfferingView;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read-only lookups that clients need before they can register an application. */
@Service
@Transactional(readOnly = true)
public class ReferenceDataService {

    private final AcademicYearRepository yearRepo;
    private final GradeLevelRepository gradeRepo;
    private final ClassOfferingRepository offeringRepo;
    private final ApplicationRepository applicationRepo;

    public ReferenceDataService(AcademicYearRepository yearRepo, GradeLevelRepository gradeRepo,
                                ClassOfferingRepository offeringRepo, ApplicationRepository applicationRepo) {
        this.yearRepo = yearRepo;
        this.gradeRepo = gradeRepo;
        this.offeringRepo = offeringRepo;
        this.applicationRepo = applicationRepo;
    }

    public List<AcademicYearView> academicYears() {
        return yearRepo.findAll(Sort.by(Sort.Direction.DESC, "startDate")).stream()
                .map(y -> new AcademicYearView(y.getId(), y.getLabel(), y.getStartDate(), y.getEndDate(),
                        y.isApplicationsOpen()))
                .toList();
    }

    public List<GradeLevelView> gradeLevels() {
        return gradeRepo.findAll(Sort.by("id")).stream()
                .map(g -> new GradeLevelView(g.getId(), g.getCode(), g.getName(), g.getMinAge(), g.getMaxAge(),
                        g.isRequiresPreviousReport()))
                .toList();
    }

    public List<OfferingView> offerings() {
        return offeringRepo.findAllByOrderByIdAsc().stream()
                .map(o -> {
                    long admitted = applicationRepo.countByOfferingIdAndStatus(o.getId(), ApplicationStatus.ADMITTED);
                    return new OfferingView(o.getId(), o.getAcademicYear().getLabel(), o.getGradeLevel().getCode(),
                            o.getGradeLevel().getName(), o.getCapacity(), admitted,
                            Math.max(0, o.getCapacity() - admitted), o.getAcademicYear().isApplicationsOpen());
                })
                .toList();
    }
}
