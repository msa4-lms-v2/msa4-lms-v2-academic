package com.msa4lmsv2academic.domain.transfer.repository;

import com.msa4lmsv2academic.domain.transfer.entity.AcademicChangeRequestFile;
import com.msa4lmsv2academic.domain.transfer.entity.TransferDocumentType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AcademicChangeRequestFileRepository extends JpaRepository<AcademicChangeRequestFile, Long> {
    @Query("select f from AcademicChangeRequestFile f join fetch f.request r join fetch r.student s join fetch s.user "
            + "where r.id = :requestId and r.requestType = 'TRANSFER_DEPARTMENT' and f.documentType = :documentType")
    Optional<AcademicChangeRequestFile> findTransferFile(Long requestId, TransferDocumentType documentType);
}
