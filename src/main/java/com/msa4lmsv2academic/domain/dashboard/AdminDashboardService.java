package com.msa4lmsv2academic.domain.dashboard;

import com.msa4lmsv2academic.domain.semester.repository.SemesterRepository;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.msa4lmsv2academic.domain.dashboard.AdminDashboardResponse.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {
    private static final List<String> TYPES = List.of("LEAVE", "RETURN", "WITHDRAWAL", "DISMISSAL", "TRANSFER", "DOUBLE_MAJOR");
    private final SemesterRepository semesterRepository;
    private final AdminDashboardQueryRepository queries;

    public AdminDashboardResponse getDashboard() {
        var semester = semesterRepository.findFirstByCurrentTrue().orElse(null);
        var summary = queries.summary();
        var tasks = queries.tasks();
        if (semester == null) return new AdminDashboardResponse(null, summary, tasks, null);
        var counts = queries.stats(semester).stream().collect(Collectors.toMap(AcademicStat::type, Function.identity()));
        var stats = TYPES.stream().map(type -> counts.getOrDefault(type, new AcademicStat(type, 0, 0))).toList();
        return new AdminDashboardResponse(CurrentSemester.from(semester), summary, tasks, stats);
    }
}
