package com.msa4lmsv2academic.domain.dashboard;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import com.msa4lmsv2academic.domain.semester.entity.Semester;
import com.msa4lmsv2academic.domain.semester.entity.SemesterTerm;
import static com.msa4lmsv2academic.domain.dashboard.AdminDashboardResponse.*;

@Repository
@RequiredArgsConstructor
public class AdminDashboardQueryRepository {
    private final NamedParameterJdbcTemplate jdbc;

    // Each branch emits only actionable pending work; no semester filter on this queue.
    private static final String PENDING_SOURCE = """
        select r.id, 'LECTURE' as type, u.name as requester_name, r.created_at
        from lecture_opening_requests r join professors p on p.id = r.professor_id
        join users u on u.id = p.user_id where r.status = 'PENDING'
        union all
        select r.id, case when r.request_type in ('GENERAL_LEAVE','MILITARY_LEAVE') then 'LEAVE' else 'RETURN' end,
               u.name, r.created_at
        from academic_requests r join students s on s.id = r.student_id join users u on u.id = s.user_id
        where r.status = 'PENDING'
        union all
        select r.id, 'WITHDRAWAL', u.name, r.created_at
        from withdrawal_requests r join students s on s.id = r.student_id join users u on u.id = s.user_id
        where r.status = 'ADVISOR_APPROVED'
        union all
        select r.id, 'DISMISSAL', u.name, r.created_at
        from dismissal_candidates r join students s on s.id = r.student_id join users u on u.id = s.user_id
        where r.status = 'PENDING'
        union all
        select r.id, case when r.request_type = 'TRANSFER_DEPARTMENT' then 'TRANSFER' else 'DOUBLE_MAJOR' end,
               u.name, r.created_at
        from academic_change_requests r join students s on s.id = r.student_id join users u on u.id = s.user_id
        where r.status = 'ADVISOR_APPROVED' and r.request_type in ('TRANSFER_DEPARTMENT','DOUBLE_MAJOR')
        """;

    public Summary summary() {
        return jdbc.queryForObject("""
            select coalesce(sum(case when type = 'LECTURE' then 1 else 0 end), 0) as lecture_pending,
                   coalesce(sum(case when type <> 'LECTURE' then 1 else 0 end), 0) as academic_pending
            from (
            """ + PENDING_SOURCE + ") pending", Map.of(),
                (rs, row) -> new Summary(rs.getLong("lecture_pending"), rs.getLong("academic_pending")));
    }

    public List<Task> tasks() {
        return jdbc.query("select * from (" + PENDING_SOURCE +
                        ") pending order by created_at, type, id limit 30", Map.of(),
                (rs, row) -> new Task(rs.getLong("id"), rs.getString("type"),
                        rs.getString("requester_name"), rs.getTimestamp("created_at").toLocalDateTime()));
    }

    public List<AcademicStat> stats(Semester semester) {
        var params = Map.of("semesterId", semester.getId(), "year", semester.getAcademicYear(),
                "term", semester.getTerm() == SemesterTerm.FIRST ? 1 : 2,
                "start", semester.getStartDate(), "end", semester.getEndDate());
        // Withdrawal/dismissal have no target-semester column. Use their effective date,
        // falling back to the requested effective date and then the submission date.
        String source = """
            select case when request_type in ('GENERAL_LEAVE','MILITARY_LEAVE') then 'LEAVE' else 'RETURN' end as type,
                   status
            from academic_requests where target_year = :year and target_semester = :term
            union all
            select 'WITHDRAWAL', status from withdrawal_requests
            where coalesce(effective_date, requested_effective_date, date(created_at)) between :start and :end
            union all
            select 'DISMISSAL', status from dismissal_candidates
            where date(coalesce(processed_at, created_at)) between :start and :end
            union all
            select case when r.request_type = 'TRANSFER_DEPARTMENT' then 'TRANSFER' else 'DOUBLE_MAJOR' end, r.status
            from academic_change_requests r left join academic_change_request_periods p on p.id = r.request_period_id
            where coalesce(r.target_semester_id, p.semester_id) = :semesterId
              and r.request_type in ('TRANSFER_DEPARTMENT','DOUBLE_MAJOR')
            """;
        return jdbc.query("""
            select type,
                   sum(case when status in ('APPROVED','APPLIED','REJECTED','ADVISOR_REJECTED','CONFIRMED') then 1 else 0 end) completed,
                   sum(case when status in ('PENDING','ADVISOR_APPROVED') then 1 else 0 end) pending
            from (
            """ + source + ") stats group by type", params,
                (rs, row) -> new AcademicStat(rs.getString("type"), rs.getLong("completed"), rs.getLong("pending")));
    }
}
