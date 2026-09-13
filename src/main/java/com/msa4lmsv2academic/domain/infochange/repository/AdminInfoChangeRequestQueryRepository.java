package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequesterType;
import com.msa4lmsv2academic.domain.infochange.entity.InfoChangeRequestStatus;
import com.msa4lmsv2academic.domain.infochange.request.AdminInfoChangeRequestSearchRequestDTO;
import com.msa4lmsv2academic.domain.infochange.response.AdminInfoChangeRequestSummaryResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AdminInfoChangeRequestQueryRepository {

    private final EntityManager entityManager;

    public AdminInfoChangeRequestSearchResult search(
            AdminInfoChangeRequestSearchRequestDTO request,
            long offset,
            int limit
    ) {
        String unionQuery = unionQuery(request.requesterType());
        long totalCount = count(unionQuery, request);
        List<AdminInfoChangeRequestSummaryResponseDTO> items = fetch(unionQuery, request, offset, limit);
        return new AdminInfoChangeRequestSearchResult(items, totalCount);
    }

    private long count(String unionQuery, AdminInfoChangeRequestSearchRequestDTO request) {
        Query query = entityManager.createNativeQuery("SELECT COUNT(*) FROM (" + unionQuery + ") requests");
        bindSearchParameters(query, request);
        Number count = (Number) query.getSingleResult();
        return count.longValue();
    }

    @SuppressWarnings("unchecked")
    private List<AdminInfoChangeRequestSummaryResponseDTO> fetch(
            String unionQuery,
            AdminInfoChangeRequestSearchRequestDTO request,
            long offset,
            int limit
    ) {
        Query query = entityManager.createNativeQuery(
                "SELECT * FROM (" + unionQuery + ") requests "
                        + "ORDER BY created_at DESC, requester_type ASC, request_id DESC"
        );
        bindSearchParameters(query, request);
        query.setFirstResult(Math.toIntExact(offset));
        query.setMaxResults(limit);

        return ((List<Object[]>) query.getResultList()).stream()
                .map(this::toSummary)
                .toList();
    }

    private String unionQuery(InfoChangeRequesterType requesterType) {
        List<String> selects = new ArrayList<>();
        if (requesterType == null || requesterType == InfoChangeRequesterType.STUDENT) {
            selects.add(studentQuery());
        }
        if (requesterType == null || requesterType == InfoChangeRequesterType.PROFESSOR) {
            selects.add(professorQuery());
        }
        return String.join(" UNION ALL ", selects);
    }

    private String studentQuery() {
        return """
                SELECT 'STUDENT' AS requester_type,
                       request.id AS request_id,
                       user.name AS requester_name,
                       department.id AS department_id,
                       department.name AS department_name,
                       request.status AS request_status,
                       request.created_at AS created_at
                  FROM student_info_change_requests request
                  JOIN students student ON student.id = request.student_id
                  JOIN users user ON user.id = student.user_id
             LEFT JOIN departments department ON department.id = student.department_id
                 WHERE (:keyword IS NULL OR LOWER(user.name) LIKE CONCAT('%', LOWER(:keyword), '%'))
                   AND (:status IS NULL OR request.status = :status)
                   AND (:requestedFrom IS NULL OR request.created_at >= :requestedFrom)
                   AND (:requestedUntil IS NULL OR request.created_at < :requestedUntil)
                """;
    }

    private String professorQuery() {
        return """
                SELECT 'PROFESSOR' AS requester_type,
                       request.id AS request_id,
                       user.name AS requester_name,
                       department.id AS department_id,
                       department.name AS department_name,
                       request.status AS request_status,
                       request.created_at AS created_at
                  FROM professor_info_change_requests request
                  JOIN professors professor ON professor.id = request.professor_id
                  JOIN users user ON user.id = professor.user_id
             LEFT JOIN departments department ON department.id = professor.department_id
                 WHERE (:keyword IS NULL OR LOWER(user.name) LIKE CONCAT('%', LOWER(:keyword), '%'))
                   AND (:status IS NULL OR request.status = :status)
                   AND (:requestedFrom IS NULL OR request.created_at >= :requestedFrom)
                   AND (:requestedUntil IS NULL OR request.created_at < :requestedUntil)
                """;
    }

    private void bindSearchParameters(Query query, AdminInfoChangeRequestSearchRequestDTO request) {
        query.setParameter("keyword", request.normalizedKeyword());
        query.setParameter("status", request.status() == null ? null : request.status().name());
        query.setParameter("requestedFrom", request.requestedFrom() == null ? null : request.requestedFrom().atStartOfDay());
        query.setParameter(
                "requestedUntil",
                request.requestedTo() == null ? null : request.requestedTo().plusDays(1).atStartOfDay()
        );
    }

    private AdminInfoChangeRequestSummaryResponseDTO toSummary(Object[] row) {
        return new AdminInfoChangeRequestSummaryResponseDTO(
                InfoChangeRequesterType.valueOf((String) row[0]),
                ((Number) row[1]).longValue(),
                (String) row[2],
                row[3] == null ? null : ((Number) row[3]).longValue(),
                (String) row[4],
                InfoChangeRequestStatus.valueOf((String) row[5]),
                toLocalDateTime(row[6])
        );
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime();
        }
        return (LocalDateTime) value;
    }
}
