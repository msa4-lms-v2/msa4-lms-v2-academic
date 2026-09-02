package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.StudentInfoChangeRequestFile;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentInfoChangeRequestFileRepository extends JpaRepository<StudentInfoChangeRequestFile, Long> {

    List<StudentInfoChangeRequestFile> findByRequestIdOrderByIdAsc(Long requestId);

    List<StudentInfoChangeRequestFile> findByRequestIdIn(Collection<Long> requestIds);
}
