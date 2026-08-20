package com.msa4lmsv2academic.domain.infochange.repository;

import com.msa4lmsv2academic.domain.infochange.entity.ProfessorInfoChangeRequestFile;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProfessorInfoChangeRequestFileRepository
        extends JpaRepository<ProfessorInfoChangeRequestFile, Long> {

    List<ProfessorInfoChangeRequestFile> findByRequestIdOrderByIdAsc(Long requestId);
}
